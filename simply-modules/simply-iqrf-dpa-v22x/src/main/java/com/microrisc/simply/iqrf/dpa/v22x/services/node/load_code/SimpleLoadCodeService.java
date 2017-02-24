/*
 * Copyright 2016 MICRORISC s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code;

import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.devices.EEEPROM;
import com.microrisc.simply.iqrf.dpa.v22x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.LoadCodeError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.LoadError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.PreprocessingError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.WriteError;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_Request;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits.Result;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits.Result.DeviceProcResult;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_Data;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadResult;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.BaseServiceResult;
import com.microrisc.simply.services.ServiceParameters;
import com.microrisc.simply.services.ServiceResult;
import com.microrisc.simply.services.node.BaseService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Load Code Service implementation.
 * 
 * @author Martin Strouhal
 * @author Michal Konopa
 */
public final class SimpleLoadCodeService 
extends BaseService implements LoadCodeService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoadCodeService.class);

    private static final int CRC_INIT_VALUE_HEX = 0x01;
    private static final int CRC_INIT_VALUE_IQRF = 0x03;
    
    private static final int FOURSOME_LEN = 1 
            + DPA_ProtocolProperties.PNUM_LENGTH
            + DPA_ProtocolProperties.PCMD_LENGTH
            + DPA_ProtocolProperties.HW_PROFILE_LENGTH
            ;
   
    // total timeout[in ms] after code load - includes mainly reset operation
    private static final int TIMEOUT_AFTER_LOAD = 5 + 100 + 300 + 300;
    
    
    // is message printing enabled or not
    private boolean isPrintingMessagesEnabled = false;
    
    // prints info message
    private void printMessage(String message) {
        if ( isPrintingMessagesEnabled ) {
            System.out.println(message);
        }
    }
    
    // converts specified bytes into hexa string
    private String toHexString(short[] arr) {
        StringBuilder sb = new StringBuilder();
        
        sb.append('[');
        for( short b : arr ) {
           sb.append(String.format("%02x", b));
           sb.append(", ");
        }
        
        if ( arr.length > 0 ) {
            sb.delete(sb.length() - 2, sb.length());
        }
        
        sb.append(']');
        
        return sb.toString();
    }
    
    
    // checks, if the start address is valid in relation to node ID
    private static boolean isStartAddressValid(String nodeId, int startAddress) {
        int nodeAddr = Integer.parseInt(nodeId);
        if ( nodeAddr == DPA_ProtocolProperties.NADR_Properties.IQMESH_COORDINATOR_ADDRESS ) {
            return (startAddress >= EEEPROM.COORD_ADDRESS_MIN && startAddress <= EEEPROM.COORD_ADDRESS_MAX);
        }
        
        return (startAddress >= EEEPROM.NODE_ADDRESS_MIN && startAddress <= EEEPROM.NODE_ADDRESS_MAX);
    }
    
    
    private CodeBlock findHandlerBlock(IntelHex file) {
        Collections.sort(file.getCodeBlocks());

        List<CodeBlock> blocks = file.getCodeBlocks();

        for (CodeBlock block : blocks) {
            if (block != null && (file.getData().getShort((int) block.
                    getAddressStart()) == 0x6400)) {
               return block;
            }
        }

        return null;
    }

    private int calculateChecksum(IntelHex file, CodeBlock handlerBlock, int length) {
        return calculateChecksum(file.getData(), handlerBlock, CRC_INIT_VALUE_HEX, length);
    }

    private int calculateChecksum(byte[] data, int length){
        CodeBlock block = new CodeBlock(0, data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return calculateChecksum(buffer, block, CRC_INIT_VALUE_IQRF, length);
    }
   
    private int calculateChecksum(
            ByteBuffer buffer, CodeBlock handlerBlock, int checkSumInitialValue, int length
    ) {
        logger.debug(
                "calculateChecksum - start: buffer={}, handlerBlock={}, "
                + "checkSumInitialValue={}, length={}",
                buffer, handlerBlock, checkSumInitialValue, length
        );
        
        int dataChecksum = checkSumInitialValue;
        // checksum for data
        for (
            long address = handlerBlock.getAddressStart();
            address < handlerBlock.getAddressStart() + length;
            address++
        ) {
            int oneByte = buffer.get((int) address) & 0xFF;
            if (handlerBlock.getAddressEnd() - address < 0) {
               oneByte = 0x34FF;
            }

            // One’s Complement Fletcher Checksum
            int tempL = dataChecksum & 0xff;
            tempL += oneByte;
            if ((tempL & 0x100) != 0) {
               tempL++;
            }

            int tempH = dataChecksum >> 8;
            tempH += tempL & 0xff;
            if ((tempH & 0x100) != 0) {
               tempH++;
            }

            dataChecksum = (tempL & 0xff) | (tempH & 0xff) << 8;
        }
      
        logger.debug("calculateChecksum - end: {}", dataChecksum);
        return dataChecksum;
    }    
    
    // updates results and errors map according to failed nodes
    private static void updateNodeResultsMaps(
            Map<String, Boolean> resultsMap, 
            Map<String, LoadCodeError> errorsMap,
            LoadCodeError error,
            Collection<Node> failedNodes
    ) {
        for ( Node node : failedNodes ) {
            resultsMap.put(node.getId(), false);
            errorsMap.put(node.getId(), error);
        }
    }
    
    // creates error result for specified error, failed nodes and results map
    private static ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> 
            createErrorResult(
                    LoadCodeError error, 
                    Collection<Node> failedNodes,
                    Map<String, Boolean> resultsMap,
                    Map<String, LoadCodeError> errorsMap
    ) {
        updateNodeResultsMaps(resultsMap, errorsMap, error, failedNodes);
        
        return new BaseServiceResult<>(
                ServiceResult.Status.ERROR, 
                new LoadCodeResult(resultsMap, errorsMap), 
                new LoadCodeProcessingInfo(error)
        );
    } 
            
    // creates error result for specified error and failed nodes
    private static ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> 
            createErrorResult(LoadCodeError error, Collection<Node> failedNodes) 
    {
        return createErrorResult(
                error, 
                failedNodes, 
                new HashMap<String, Boolean>(),
                new HashMap<String, LoadCodeError>()
        );
    }
    
    
    // writes data into EEEPROM of the context node
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemoryOfThisNode(
            int startAddress, short[][] data
    ) {
        // preparing for hypotetical case when error occurs on context node
        Collection<Node> contextNodeList = new LinkedList<>();
        contextNodeList.add(contextNode);
            
        EEEPROM eeeprom = this.contextNode.getDeviceObject(EEEPROM.class);
        if ( eeeprom == null ) {
            return createErrorResult( new WriteError("Missing EEEPROM"), contextNodeList);
        }
        
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            return createErrorResult( new WriteError("Missing OS"), contextNodeList);
        }
      
        int actualAddress = startAddress;
        int index = 0;
        while ( index < data.length ) {
            if ( (index + 1) < data.length ) {
                if  ( (data[index].length == 16) && (data[index+1].length == 16) ) {
                    DPA_Request firstReq = new DPA_Request(
                            EEEPROM.class,
                            EEEPROM.MethodID.EXTENDED_WRITE,
                            new Object[]{actualAddress, data[index]},
                            DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK
                    );

                    actualAddress += data[index].length;

                    DPA_Request secondReq = new DPA_Request(
                            EEEPROM.class,
                            EEEPROM.MethodID.EXTENDED_WRITE,
                            new Object[]{actualAddress, data[index+1]},
                            DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK
                    );

                    actualAddress += data[index+1].length;
                    
                    printMessage(
                            "\nData to write: " + toHexString(data[index]) 
                            + " " + toHexString(data[index+1])
                    );
                    
                    VoidType result = os.batch( new DPA_Request[] { firstReq, secondReq } );
                    if ( result == null ) {
                        return createErrorResult(
                                new WriteError("Result of write data to EEEPROM not found."), 
                                contextNodeList
                        );
                    }
                    
                    printMessage("Completed.");
                    
                    index += 2;
                } else {
                    printMessage("\nData to write: " + toHexString(data[index]));
                    
                    VoidType result = eeeprom.extendedWrite(actualAddress, data[index]);
                    if ( result == null ) {
                        return createErrorResult(
                                new WriteError("Result of write data to EEEPROM not found."), 
                                contextNodeList
                        );
                    }
                    
                    printMessage("Completed.");
                    
                    actualAddress += data[index].length;
                    index++;
                }
            } else {
                printMessage("\nData to write: " + toHexString(data[index]));
                
                VoidType result = eeeprom.extendedWrite(actualAddress, data[index]);
                if ( result == null ) {
                    return createErrorResult(
                            new WriteError("Result of write data to EEEPROM not found."), 
                            contextNodeList
                    );
                }
                
                printMessage("Completed.");
                
                actualAddress += data[index].length;
                index++;
            }
        }
        
        Map<String, Boolean> nodeResultsMap = new HashMap<>();
        Map<String, LoadCodeError> nodeErrorsMap = new HashMap<>();
        
        nodeResultsMap.put(this.contextNode.getId(), true);
        
        return new BaseServiceResult<>(
                ServiceResult.Status.SUCCESSFULLY_COMPLETED,
                new LoadCodeResult(nodeResultsMap, nodeErrorsMap),
                new LoadCodeProcessingInfo()
        );
    }    
    
    // returns map of start address valid nodes
    private Map<String, Node> getStartAddressValidNodesMap(int startAddress, Collection<Node> nodes) {
        Map<String, Node> validNodesMap = new HashMap<>();
        
        for ( Node node : nodes ) {
            if ( isStartAddressValid(node.getId(), startAddress) ) {
                validNodesMap.put(node.getId(), node);
            }
        }
        
        return validNodesMap;
    }
    
    private short[] createWriteDataToMemoryRequestData(FRC frc, int address, short[] data) {
        int hwpId = frc.getRequestHwProfile();
        
        short[] writeDataParams = new  short[2 + data.length];
        writeDataParams[0] = (short)(address & 0xFF);
        writeDataParams[1] = (short)((address >> 8) & 0xFF);
        System.arraycopy(data, 0, writeDataParams, 2, data.length);
        
        short[] foursome = new short[FOURSOME_LEN];
        foursome[0] = (short)(FOURSOME_LEN + writeDataParams.length);
        foursome[1] = DPA_ProtocolProperties.PNUM_Properties.EEEPROM; 
        foursome[2] = 0x03;
        foursome[3] = (short)(hwpId & 0xFF);
        foursome[4] = (short)((hwpId >> 8) & 0xFF);

        short[] dpaRequestData = new short[foursome.length + writeDataParams.length];
        System.arraycopy(foursome, 0, dpaRequestData, 0, foursome.length);
        System.arraycopy(writeDataParams, 0, dpaRequestData, foursome.length, writeDataParams.length);
        
        return dpaRequestData;
    }
    
    // excludes nodes with false values according to specified map
    private void excludeFailedNodes(Collection<Node> nodes, Map<String, Boolean> resultsMap) {
        if ( resultsMap.isEmpty() ) {
            return;
        }
        
        Iterator<Node> nodesIter = nodes.iterator();
        
        while ( nodesIter.hasNext() ) {
            Node node = nodesIter.next();
            Boolean writeResult = resultsMap.get(node.getId());
            if ( writeResult == null ) {
                continue;
            }
            
            // exclude node
            if ( writeResult == false ) {
                nodesIter.remove();
            }
        }
    }
    
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemoryUsingBroadcast(
            int startAddress, short[][] data, Collection<Node> targetNodes
    ) {
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            return createErrorResult(
                    new WriteError("Missing FRC."),
                    targetNodes
            );
        }
        
        // returns map of valid nodes in respect to start address
        Map<String, Node> validNodesMap = getStartAddressValidNodesMap(startAddress, targetNodes);
        if ( validNodesMap.isEmpty() ) {
            return createErrorResult(
                    new WriteError("No start address valid nodes to load code in."),
                    targetNodes
            );
        }
        
        // nodes with correct start address
        Collection<Node> nodesToWriteInto = new LinkedList<>(validNodesMap.values());
         
        // final maps of results
        Map<String, Boolean> finalResultsMap = new HashMap<>();
        Map<String, LoadCodeError> errorsMap = new HashMap<>();
        
        // add all start address invalid nodes into final result map
        WriteError invalidStartAddrError = new WriteError("Invalid start address.");
        for ( Node node : targetNodes ) {
            if ( !validNodesMap.containsKey(node.getId()) ) {
                finalResultsMap.put(node.getId(), false);
                errorsMap.put(node.getId(), invalidStartAddrError);
            }
        }
        
        // for usage in FRC results
        WriteError writeError = new WriteError("Not response or HWP not match.");
        
        // indicator, if all writes were OK
        boolean allWritesOk = true;
        
        int actualAddress = startAddress;
        int index = 0;
        while ( index < data.length ) {
            short[] dpaRequestData = createWriteDataToMemoryRequestData(frc, actualAddress, data[index]);
            
            // excludes nodes with failed write - it is useless to write next byte chunks into them
            excludeFailedNodes(nodesToWriteInto, finalResultsMap);  
            
            printMessage("\nData to write: " + toHexString(data[index]));
            
            FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                    dpaRequestData, nodesToWriteInto.toArray( new Node[] {}))
            );
            
            if ( result == null ) {
                return createErrorResult(
                        new WriteError("Returning of FRC result failed."), 
                        nodesToWriteInto,
                        finalResultsMap,
                        errorsMap
                );
            }

            short[] extraResult = frc.extraResult();
            if ( extraResult == null ) {
                return createErrorResult(
                        new WriteError("Returning of FRC extra result failed."), 
                        nodesToWriteInto,
                        finalResultsMap,
                        errorsMap
                );
            }
            
            printMessage("Completed.");
            
            // putting both parts of result together
            short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);

            // parsing result
            Map<String, FRC_AcknowledgedBroadcastBits.Result> parsedResultMap = null;
            try {
                parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
            } catch ( Exception ex ) {
                return createErrorResult(
                        new WriteError("Parsing of FRC result failed."), 
                        nodesToWriteInto,
                        finalResultsMap,
                        errorsMap
                );
            }
            
            // FRC results
            Map<String, Boolean> resultsMap = new HashMap<>();

            // creating final results of 1 data chunk
            for ( Node node : nodesToWriteInto ) {
                Result parsedNodeResult = parsedResultMap.get(node.getId());

                DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
                if ( (devProcResult == DeviceProcResult.NOT_RESPOND) 
                    || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
                ) {
                    resultsMap.put(node.getId(), false);
                    errorsMap.put(node.getId(), writeError);
                    allWritesOk = false;
                } else {
                    resultsMap.put(node.getId(), true);
                }
            }
            
            // copy results map of 1 data chunk into final results map
            finalResultsMap.putAll(resultsMap);
            
            // increase target address
            actualAddress += data[index].length;
            
            index++;
        }
        
        ServiceResult.Status serviceStatus = ( allWritesOk == true )?
                                              ServiceResult.Status.SUCCESSFULLY_COMPLETED
                                              : ServiceResult.Status.ERROR;
        
        return new BaseServiceResult<>(
                serviceStatus,
                new LoadCodeResult(finalResultsMap, errorsMap),
                new LoadCodeProcessingInfo()
        );
    }
    
    // returns coordinator node from specified nodes
    private static Node getCoordNode(Collection<Node> nodes) {
        for ( Node node : nodes ) {
            if ( node.getId().equals("0") ) {
                return node;
            }
        }
        return null;
    }
    
    // writes specified data into EEEPROM beginning from specified address    
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemory(
            int startAddress, short[][] data, Collection<Node> targetNodes
    ) {
        if ( logger.isDebugEnabled() ) {
            StringBuilder debugData = new StringBuilder();
            
            debugData.append("{\n");
            for (short[] data1 : data) {
                debugData.append("(");
                debugData.append("length: ");
                debugData.append(data1.length);
                debugData.append(") > ");
                debugData.append(Arrays.toString(data1));
                debugData.append("\n");
            }
            debugData.append("\n}");
            
            logger.debug(
                    "writeDataToMemory - start: startAddress={}, data={}", 
                    startAddress, debugData.toString()
            );
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeResult = null;
        
        if ( (targetNodes == null) || (targetNodes.isEmpty()) ) {
            writeResult = writeDataToMemoryOfThisNode(startAddress, data);
            
            logger.debug("writeDataToMemory - end: {}", writeResult);
            return writeResult;
        }
        
        // coordinator requires unicast - does not function with FRC
        Node coordNode = getCoordNode(targetNodes);
        if ( coordNode != null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> coordWriteResult 
                = writeDataToMemoryOfThisNode(startAddress, data);
            
            // for removing coordinator from FRC write
            targetNodes.remove(coordNode);
            
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> nodesWriteResult 
                = writeDataToMemoryUsingBroadcast(startAddress, data, targetNodes);
            
            // put coordinator node back into target nodes
            targetNodes.add(coordNode);
            
            // merging results together
            Map<String, Boolean> mergedResultsMap = new HashMap<>();
            mergedResultsMap.putAll(coordWriteResult.getResult().getResultsMap());
            mergedResultsMap.putAll(nodesWriteResult.getResult().getResultsMap());
            
            // merging errors together
            Map<String, LoadCodeError> mergedErrorsMap = new HashMap<>();
            mergedErrorsMap.putAll(coordWriteResult.getResult().getErrorsMap());
            mergedErrorsMap.putAll(nodesWriteResult.getResult().getErrorsMap());
            
            ServiceResult.Status mergedResult = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
            if ( 
                coordWriteResult.getStatus() == ServiceResult.Status.ERROR
                || nodesWriteResult.getStatus() == ServiceResult.Status.ERROR 
            ) {
                mergedResult = ServiceResult.Status.ERROR;
            }

            writeResult = new BaseServiceResult<>(
                mergedResult,
                new LoadCodeResult(mergedResultsMap, mergedErrorsMap),
                new LoadCodeProcessingInfo()
            );
            
            logger.debug("writeDataToMemory - end: {}", writeResult);
            return writeResult;
        }
        
        writeResult = writeDataToMemoryUsingBroadcast(startAddress, data, targetNodes);
        
        logger.debug("writeDataToMemory - end: {}", writeResult);
        return writeResult;
    }
   
    
    // loads code into context contextNode using unicast
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadCodeUnicast(
        LoadCodeServiceParameters params, int length, int dataChecksum
    ) { 
        logger.debug(
                "loadCodeUnicast - begin: params={}, length={}, dataChecksum={}",
                params, length, dataChecksum
        );
        
        // for hypotetical error during code load
        Collection<Node> contextNodeList = new LinkedList<>();
        contextNodeList.add(contextNode);
        
        // service result
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult = null;
        
        // get access to OS peripheral
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            servResult = createErrorResult( new LoadError("Missing OS"), contextNodeList);
            logger.debug("loadCodeUnicast - end: {}", servResult);
            return servResult;
        }

        // set longer waiting timeout
        os.setDefaultWaitingTimeout(30000);            
      
        // loading code
        LoadResult result = os.loadCode( 
                new LoadingCodeProperties(
                    params.getLoadingAction(), params.getLoadingContent(),
                    params.getStartAddress(), length, dataChecksum
                )
        );
        
        if ( result == null ) {
            servResult = createErrorResult( 
                    new LoadError("Load code result not found."),
                    contextNodeList
            );
            
            logger.debug("loadCodeUnicast - end: {}", servResult);
            return servResult;
        }
        
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put(this.contextNode.getId(), result.getResult());
        
        Map<String, LoadCodeError> errorsMap = new HashMap<>();
        
        ServiceResult.Status status = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
        LoadCodeProcessingInfo loadCodeProcessingInfo = new LoadCodeProcessingInfo();
        
        if ( result.getResult() == false ) {
            status = ServiceResult.Status.ERROR;
            LoadError error = new LoadError("Checksum does not match.");
            loadCodeProcessingInfo = new LoadCodeProcessingInfo(error);
            errorsMap.put(this.contextNode.getId(), error);
        }
        
        servResult = new BaseServiceResult<>(
                status,
                new LoadCodeResult(resultMap, errorsMap),
                loadCodeProcessingInfo
        );
        
        logger.debug("loadCodeUnicast - end: {}", servResult);
        return servResult;
    }
    
    // creates and returns load code request params data 
    private static short[] createLoadCodeRequestParams(
            LoadingCodeProperties.LoadingAction action,
            LoadingCodeProperties.LoadingContent content,
            int startAddress, int length, int checksum
    ) {
        short[] request = new short[7];
        
        // flags
        request[0] = 0;
        if ( action == LoadingCodeProperties.LoadingAction.ComputeAndMatchChecksumWithCodeLoading ) {
            request[0] |= 0b00000001; 
        }
        
        if ( content == LoadingCodeProperties.LoadingContent.IQRF_Plugin ) {
            request[0] |= 0b00000010;
        }
        
        // address
        request[1] = (short)(startAddress & 0xFF);
        request[2] = (short)((startAddress >> 8) & 0xFF);
        
        // length
        request[3] = (short)(length & 0xFF);
        request[4] = (short)((length >> 8) & 0xFF);
        
        // checksum
        request[5] = (short)(checksum & 0xFF);
        request[6] = (short)((checksum >> 8) & 0xFF);
        
        return request;
    }
    
    // creates load code request data
    private short[] createLoadCodeRequestData(
            FRC frc, LoadCodeServiceParameters params, int length, int dataChecksum
    ) {
        short[] loadCodeRequestParams = createLoadCodeRequestParams(
                params.getLoadingAction(),
                params.getLoadingContent(),
                params.getStartAddress(), 
                length, 
                dataChecksum
        );
        
        int hwpId = frc.getRequestHwProfile();
        
        short[] foursome = new short[FOURSOME_LEN];
        foursome[0] = (short)(FOURSOME_LEN + loadCodeRequestParams.length);
        foursome[1] = DPA_ProtocolProperties.PNUM_Properties.OS; 
        foursome[2] = 0x0A;
        foursome[3] = (short)(hwpId & 0xFF);
        foursome[4] = (short)((hwpId >> 8) & 0xFF);

        short[] dpaRequestData = new short[foursome.length + loadCodeRequestParams.length];
        System.arraycopy(foursome, 0, dpaRequestData, 0, foursome.length);
        System.arraycopy(loadCodeRequestParams, 0, dpaRequestData, foursome.length, loadCodeRequestParams.length);
        
        return dpaRequestData;
    }
    
    // puts both parts of FRC result together a returns the result
    private short[] getCompleteFrcResult(short[] firstPart, short[] extraResult) {
        short[] completeResult = new short[firstPart.length + extraResult.length];
        System.arraycopy(firstPart, 0, completeResult, 0, firstPart.length);
        System.arraycopy(extraResult, 0, completeResult, firstPart.length, extraResult.length);
        return completeResult;
    }
    
    // loads code to specified nodes using FRC acknowledged broadcast
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadCodeBroadcast(
        LoadCodeServiceParameters params, int length, int dataChecksum, Collection<Node> targetNodes
    ) { 
        logger.debug(
                "loadCodeBroadcast - begin: params={}, length={}, dataChecksum={}, "
                + "targetNodes={}",
                params, length, dataChecksum, Arrays.toString(targetNodes.toArray())
        );
        
        // service result
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult = null;
        
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            servResult = createErrorResult( new LoadError("Missing FRC"), targetNodes);
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        }
        
        short[] dpaRequestData = createLoadCodeRequestData(frc, params, length, dataChecksum);
        
        FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                dpaRequestData, targetNodes.toArray( new Node[] {}))
        );
        
        if ( result == null ) {
            servResult = createErrorResult( 
                    new LoadError("Returning of FRC result failed."), 
                    targetNodes
            );
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        } 
        
        short[] extraResult = frc.extraResult();
        if ( extraResult == null ) {
            servResult = createErrorResult( 
                    new LoadError("Returning of FRC extra result failed."), 
                    targetNodes
            );
        }
        
        // putting both parts of result together
        short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);

        // parsing result
        Map<String, FRC_AcknowledgedBroadcastBits.Result> parsedResultMap = null;
        try {
            parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
        } catch ( Exception ex ) {
            servResult = createErrorResult( 
                    new LoadError("Parsing of FRC result failed."), 
                    targetNodes
            );
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        }
        
        Map<String, Boolean> resultsMap = new HashMap<>();
        Map<String, LoadCodeError> errorsMap = new HashMap<>();
        
        boolean loadFailed = false;
        
        // for usage in FRC results
        LoadError notResponseOrHwpError = new LoadError("Not response or HWP not match.");
        
        // creating final results
        for ( Node node : targetNodes ) {
            Result parsedNodeResult = parsedResultMap.get(node.getId());

            DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
            if ( (devProcResult == DeviceProcResult.NOT_RESPOND) 
                || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
            ) {
                resultsMap.put(node.getId(), false);
                errorsMap.put(node.getId(), notResponseOrHwpError);
                loadFailed = true;
            } else {
                resultsMap.put(node.getId(), true);
            }
        }
        
        ServiceResult.Status serviceStatus = ( loadFailed == false )?
                                              ServiceResult.Status.SUCCESSFULLY_COMPLETED
                                              : ServiceResult.Status.ERROR;
        
        servResult = new BaseServiceResult<>(
                serviceStatus, 
                new LoadCodeResult(resultsMap, errorsMap),
                new LoadCodeProcessingInfo()
        );
        
        logger.debug("loadCodeBroadcast - end: {}", servResult);
        return servResult;
    }
    
    // indicates, whether all nodes from specified collection are available
    private static boolean areNodesAvalailable(Collection<Node> nodes) {
        for ( Node node : nodes ) {
            if ( node == null ) {
                return false;
            }
        }
        return true;
    }
    
    // returns the number of successfully loaded nodes
    private static int getNumOfSuccessfullyLoadedNodes(LoadCodeResult loadCodeResult) {
        int succNodesNum = 0;
        
        Map<String, Boolean> resultsMap = loadCodeResult.getResultsMap();
        if ( resultsMap == null ) {
            return 0;
        }
        
        for ( Map.Entry<String, Boolean> entry : resultsMap.entrySet() ) {
            if ( entry.getValue() == true ) {
                succNodesNum++;
            }
        }
        
        return succNodesNum;
    }
    
    
            
    /**
     * Creates new Load Code Service object.
     * 
     * @param node Device Objects to use
     * @throws IllegalArgumentException if {@code contextNode} is empty
     */
    public SimpleLoadCodeService(Node node) {
        super(node);
        if ( node == null ) {
            throw new IllegalArgumentException("Node in context cannot be null.");
        }
    }
    
    @Override
    public ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> 
        loadCode(LoadCodeServiceParameters params)
    {
        logger.debug("loadCode - start: params={}", params);
        
        // service result
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult = null;
        
        LoadingCodeProperties.LoadingContent loadingContent = params.getLoadingContent();
        if ( loadingContent == null ) {
            servResult = new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( new PreprocessingError("Unspecified loading content."))
            );
            
            logger.debug("loadCode - end: {}", servResult);
            return servResult;
        }
        
        // indicates, whether data for writing into EEEPROM will be prepared for broadcast 
        boolean prepareDataForBroadcast = true;
        Collection<Node> targetNodes = params.getTargetNodes();
        
        // if target nodes are not specified, load code target node is the context node
        if ( targetNodes == null || targetNodes.isEmpty() ) {
            if ( !isStartAddressValid(this.contextNode.getId(), params.getStartAddress()) ) {
                servResult = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                    new PreprocessingError("Invalid start address.")
                            )
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
            }
            prepareDataForBroadcast = false;
        } else {
            // checking availability of target nodes
            if ( !areNodesAvalailable(targetNodes) ) {
                servResult = new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( 
                                new PreprocessingError("Some nodes not available.")
                        )
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
            }
            
            // checking if target nodes contain coordinator node and if so then if
            // the coordinator node is equal to the context node
            if ( getCoordNode(targetNodes) != null ) {
                if ( !contextNode.getId().equals("0") ) {
                    servResult = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                    new PreprocessingError(
                                        "Coordinator target node can be used ONLY on"
                                        + " coordinator context node."
                                )
                            )
                        );

                    logger.debug("loadCode - end: {}", servResult);
                    return servResult;
                }
            }
        }
        
        isPrintingMessagesEnabled = params.isPrintingMessagesEnabled();
        
        short[][] dataToWrite = null;
        final int length, dataChecksum;
        
        switch ( loadingContent ) {
            case Hex:
                // prepare with allocated size and after parse data
                IntelHex file = new IntelHex(0xFFFFFF);
                try {
                    file.parseIntelHex(params.getFileName());
                } catch ( IOException ex ) {
                    servResult = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo(new PreprocessingError(ex))
                    );
                    
                    logger.debug("loadCode - end: {}", servResult);
                    return servResult;
                }  
                
                // separating code block with custom DPA handler block
                CodeBlock handlerBlock = findHandlerBlock(file);
                if (  handlerBlock == null ) {
                    servResult = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                new PreprocessingError(
                                    "Selected .hex file does not include Custom DPA "
                                    + "handler section or the code does not start with"
                                    + "clrwdt() marker."
                                )
                            )
                    );
                    
                    logger.debug("loadCode - end: {}", servResult);
                    return servResult;
                }
                
                logger.debug(
                        " Handler block starts at " + handlerBlock.getAddressStart()
                        + " and ends at " + handlerBlock.getAddressEnd()
                ); 
                
                // calculating rounded length of handler in memory
                length = (int) ((handlerBlock.getLength() + (64 - 1)) & ~(64 - 1));
                String msg = "Length of data is: " + Integer.toHexString(length);
                logger.debug(msg);
                printMessage(msg);
                
                // calculating checksum with initial value 1 (defined for DPA handler)
                dataChecksum = calculateChecksum(file, handlerBlock, length);
                msg = "Checksum of data is: " + Integer.toHexString(dataChecksum);
                logger.debug(msg);
                printMessage(msg);
                
                // splitting data into blocks for writing into EEEPROM
                file.getData().position(0);
                if ( prepareDataForBroadcast ) {
                    dataToWrite = new DataPreparer(handlerBlock, file).prepareAs16BytesBlocks();
                } else {
                    dataToWrite = new DataPreparer(handlerBlock, file).prepare();
                }
                break;
            case IQRF_Plugin:
                // parse iqrf file
                IQRFParser parser = new IQRFParser(params.getFileName());
                byte[] parsedData = parser.parse();
                
                length = parsedData.length;
                msg = "Length of data is: " + Integer.toHexString(length);
                logger.debug(msg);
                printMessage(msg);
                
                dataChecksum = calculateChecksum(parsedData, length);
                msg = "Checksum of data is: " + Integer.toHexString(dataChecksum);
                logger.debug(msg);
                printMessage(msg);
                
                // splitting data into blocks for writing into EEEPROM
                if ( prepareDataForBroadcast ) {
                    dataToWrite = new DataPreparer(parsedData).prepareAs16BytesBlocks();
                } else {
                    dataToWrite = new DataPreparer(parsedData).prepare();
                }
                break;
            default:
                servResult = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                    new PreprocessingError("Unsupported loading content.")
                            )
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
        }
        
        // writing data to memory
        printMessage("\nWriting code into external EEPROM - begin");
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataResult 
            = writeDataToMemory(
                    params.getStartAddress(), dataToWrite, params.getTargetNodes()
        );
        printMessage("Writing code into external EEPROM - end");
        
        
        // if there is no result, it is useless to load code
        if ( writeDataResult.getResult() == null ) {
            logger.debug("loadCode - end: {}", writeDataResult);
            return writeDataResult;
        }
        
        // final results map
        Map<String, Boolean> finalResultsMap = new HashMap<>();
        
        // final errors map
        Map<String, LoadCodeError> finalErrorsMap = new HashMap<>();
        
        // nodes, which has been written data successfully into
        List<Node> nodesToLoad = new LinkedList<>();
        
        // results of data write
        Map<String, Boolean> writeDataResultsMap = writeDataResult.getResult().getResultsMap();
        
        // errors of data write
        Map<String, LoadCodeError> writeErrorsMap = writeDataResult.getResult().getErrorsMap();
        
        // add all errors from write into final errors
        finalErrorsMap.putAll(writeErrorsMap);
        
        // find out successfull nodes and store the unsuccessful ones into final results map
        if ( params.getTargetNodes() != null ) {
            WriteError resultNotFound = new WriteError("Result not found");
            
            for ( Node node : params.getTargetNodes() ) {
                Boolean nodeResult = writeDataResultsMap.get(node.getId());
                if ( nodeResult == null ) {
                    finalResultsMap.put(node.getId(), false);
                    finalErrorsMap.put(node.getId(), resultNotFound);
                    logger.error("Write result not found for node: " + node.getId());
                    continue;
                }
                
                if ( nodeResult == false ) {
                    finalResultsMap.put(node.getId(), false);
                    if ( !writeErrorsMap.containsKey(node.getId()) ) {
                        finalErrorsMap.put(node.getId(), resultNotFound);
                        logger.error("Write error not found for node: " + node.getId());
                    } else {
                        finalErrorsMap.put(node.getId(), writeErrorsMap.get(node.getId()));
                    }
                    continue;
                }
                
                // successfully written node
                nodesToLoad.add(node);
            }
        }
        
        // possibly add context node into the target nodes
        if ( (params.getTargetNodes() == null) || (params.getTargetNodes().isEmpty()) ) {
            if ( !writeDataResultsMap.containsKey(this.contextNode.getId()) ) {
                finalResultsMap.put(this.contextNode.getId(), false);
                finalErrorsMap.put(this.contextNode.getId(), new WriteError("Result not found"));
                
                servResult = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR,
                    new LoadCodeResult(finalResultsMap, writeErrorsMap),
                    new LoadCodeProcessingInfo( new WriteError("No result from context node."))
                );
            
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
            }
            
            Boolean contextNodeResult = writeDataResultsMap.get(this.contextNode.getId());
            if ( contextNodeResult == true ) {
                nodesToLoad.add(contextNode);
            } else {
                finalResultsMap.put(this.contextNode.getId(), false);
                finalErrorsMap.put(
                        this.contextNode.getId(), 
                        writeErrorsMap.get(this.contextNode.getId())
                );
            }
        }
        
        // there must be at least error in writing data into context node, which this
        // service resides on
        if ( nodesToLoad.isEmpty() ) {
            servResult = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR,
                    new LoadCodeResult(finalResultsMap, finalErrorsMap), 
                    new LoadCodeProcessingInfo()
            );
            
            logger.debug("loadCode - end: {}", servResult);
            return servResult;
        }
        
        // loading code into FLASH
        printMessage("\nLoading code into FLASH - begin");
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadResult = null;
        
        // indicates, whether it has been waited after code load into coordinator
        boolean waitedAfterCoordCodeLoad = false;
        
        // if a node to load code into is the context node, use unicast else use broadcast
        if ( nodesToLoad.size() == 1 ) {
            Node nodeToLoadInto = nodesToLoad.get(0);
            if ( this.contextNode.getId().equals(nodeToLoadInto.getId()) ) {
                loadResult = loadCodeUnicast(params, length, dataChecksum);
            } else {
                loadResult = loadCodeBroadcast(params, length, dataChecksum, nodesToLoad);
            }
        } else {
            // if to load into coordinator, use unicast
            Node coordNode = getCoordNode(nodesToLoad);
            if ( coordNode != null ) {
                ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> coordLoadResult 
                    = loadCodeUnicast(params, length, dataChecksum);
                
                // waiting some time for TR module(s) reset
                if ( getNumOfSuccessfullyLoadedNodes(coordLoadResult.getResult()) > 0 ) {
                    logger.debug("Waiting after coordinator code load.");
                    try {
                        Thread.sleep(TIMEOUT_AFTER_LOAD);
                        waitedAfterCoordCodeLoad = true;
                    } catch ( InterruptedException ex ) {
                        logger.error("Waiting after coordinator code load interrupted.");
                    }
                }
                
                // put away coordinator from nodes to load into
                nodesToLoad.remove(coordNode);
                ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> nodesLoadResult 
                    = loadCodeBroadcast(params, length, dataChecksum, nodesToLoad);
                
                // put back coordinator
                nodesToLoad.add(coordNode);
                
                // merging results together
                Map<String, Boolean> mergedResultsMap = new HashMap<>();
                mergedResultsMap.putAll(coordLoadResult.getResult().getResultsMap());
                mergedResultsMap.putAll(nodesLoadResult.getResult().getResultsMap());
                
                // merging errors together
                Map<String, LoadCodeError> mergedErrorsMap = new HashMap<>();
                mergedErrorsMap.putAll(coordLoadResult.getResult().getErrorsMap());
                mergedErrorsMap.putAll(nodesLoadResult.getResult().getErrorsMap());
                
                ServiceResult.Status mergedStatus = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
                if ( 
                    coordLoadResult.getStatus() == ServiceResult.Status.ERROR
                    || nodesLoadResult.getStatus() == ServiceResult.Status.ERROR
                ) {
                    mergedStatus = ServiceResult.Status.ERROR;
                }
                
                loadResult = new BaseServiceResult<>(
                    mergedStatus,
                    new LoadCodeResult(mergedResultsMap, mergedErrorsMap),
                    new LoadCodeProcessingInfo()
                );
                
            } else {
                loadResult = loadCodeBroadcast(params, length, dataChecksum, nodesToLoad);
            }   
        }
        printMessage("Loading code into FLASH - end");
        
        // put load results and errors into final maps
        finalResultsMap.putAll(loadResult.getResult().getResultsMap());
        finalErrorsMap.putAll(loadResult.getResult().getErrorsMap());
        
        
        // it is neccessary to wait some time for TR module(s) reset
        int successfullyLoadedNodesNum = getNumOfSuccessfullyLoadedNodes(loadResult.getResult());
        if ( !(successfullyLoadedNodesNum == 1 && waitedAfterCoordCodeLoad) ) {
            try {
                Thread.sleep(TIMEOUT_AFTER_LOAD);
            } catch ( InterruptedException ex ) {
                logger.error("Waiting after code load interrupted.");
            }
        }
        
        ServiceResult.Status status = loadResult.getStatus();
        if ( writeDataResult.getStatus() == ServiceResult.Status.ERROR ) {
            status = ServiceResult.Status.ERROR;
        }
        
        servResult = new BaseServiceResult<>(
                status,
                new LoadCodeResult(finalResultsMap, finalErrorsMap),
                new LoadCodeProcessingInfo()
        );
        
        logger.debug("loadCode - end: {}", servResult);
        return servResult;
    }
    
    /**
     * Does nothning.
     * @param params service parameters to set
     */
    @Override
    public void setServiceParameters(ServiceParameters params) {}
}