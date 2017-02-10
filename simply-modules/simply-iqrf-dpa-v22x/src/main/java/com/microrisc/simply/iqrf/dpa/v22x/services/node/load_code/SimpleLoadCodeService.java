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

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.devices.EEEPROM;
import com.microrisc.simply.iqrf.dpa.v22x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.RequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.di_services.DPA_StandardServices;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.LoadingContentError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.MissingPeripheralError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.ParamsError;
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
    
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> 
            createRequestProcessingError(DPA_StandardServices dpaPer, String errMessage) 
    {
        CallRequestProcessingState reqState = dpaPer.getCallRequestProcessingStateOfLastCall();
        if ( reqState == CallRequestProcessingState.ERROR ) {
            return new BaseServiceResult<>(
                ServiceResult.Status.ERROR, 
                null, 
                new LoadCodeProcessingInfo( 
                        new RequestProcessingError(dpaPer.getCallRequestProcessingErrorOfLastCall())
                )
            );
        }

        return new BaseServiceResult<>(
                ServiceResult.Status.ERROR, 
                null, 
                new LoadCodeProcessingInfo( 
                        new RequestProcessingError(errMessage + " State: " + reqState)
                )
        );
                
    }
    
    // writes data into EEEPROM of the context node
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemoryOfThisNode(
            int startAddress, short[][] data
    ) {
        EEEPROM eeeprom = this.contextNode.getDeviceObject(EEEPROM.class);
        if ( eeeprom == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(EEEPROM.class))
            );
        }
        
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(OS.class))
            );
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

                    VoidType result = os.batch( new DPA_Request[] { firstReq, secondReq } );
                    if ( result == null ) {
                        return createRequestProcessingError(os, "Result of write data to EEEPROM not found.");
                    }

                    index += 2;
                } else {
                    VoidType result = eeeprom.extendedWrite(actualAddress, data[index]);
                    if ( result == null ) {
                        return createRequestProcessingError(os, "Result of write data to EEEPROM not found.");
                    }
                    actualAddress += data[index].length;
                    index++;
                }
            } else {
                VoidType result = eeeprom.extendedWrite(actualAddress, data[index]);
                if ( result == null ) {
                    return createRequestProcessingError(os, "Result of write data to EEEPROM not found.");
                }
                actualAddress += data[index].length;
                index++;
            }
        }
        
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put(this.contextNode.getId(), true);
        
        return new BaseServiceResult<>(
                ServiceResult.Status.SUCCESSFULLY_COMPLETED,
                new LoadCodeResult(resultMap),
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
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(FRC.class))
            );
        }
        
        // returns map of valid nodes in respect to start address
        Map<String, Node> validNodesMap = getStartAddressValidNodesMap(startAddress, targetNodes);
        if ( validNodesMap.isEmpty() ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( 
                            new LoadingContentError("No start address valid nodes to load code in.")
                    )
            );
        }
        
        Collection<Node> nodesToWriteInto = new LinkedList<>(validNodesMap.values());
        
        // final map of results
        Map<String, Boolean> finalResultsMap = new HashMap<>();
        
        // add all start address invalid nodes into final result map
        for ( Node node : targetNodes ) {
            if ( !validNodesMap.containsKey(node.getId()) ) {
                finalResultsMap.put(node.getId(), false);
            }
        }
        
        // indicator, if all writes were OK
        boolean allWritesOk = true;
        
        int actualAddress = startAddress;
        int index = 0;
        while ( index < data.length ) {
            short[] dpaRequestData = createWriteDataToMemoryRequestData(frc, actualAddress, data[index]);
            
            // excludes nodes with failed write - it is useless to write next byte chunks into them
            excludeFailedNodes(nodesToWriteInto, finalResultsMap);  
            
            FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                    dpaRequestData, nodesToWriteInto.toArray( new Node[] {}))
            );

            if ( result == null ) {
                return createRequestProcessingError(frc, "Returning FRC result failed. ");
            } 

            short[] extraResult = frc.extraResult();
            if ( extraResult == null ) {
                return createRequestProcessingError(frc, "Returning FRC extra result failed. ");
            }
        
            // putting both parts of result together
            short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);

            // parsing result
            Map<String, FRC_AcknowledgedBroadcastBits.Result> parsedResultMap = null;
            try {
                parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
            } catch ( Exception ex ) {
                return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( 
                            new RequestProcessingError("Parsing of result failed")
                    )
                );
            }
        
            Map<String, Boolean> resultsMap = new HashMap<>();

            // creating final results of 1 data chunk
            for ( Node node : nodesToWriteInto ) {
                Result parsedNodeResult = parsedResultMap.get(node.getId());

                DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
                if ( (devProcResult == DeviceProcResult.NOT_RESPOND) 
                    || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
                ) {
                    resultsMap.put(node.getId(), false);
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
                new LoadCodeResult(finalResultsMap),
                new LoadCodeProcessingInfo()
        );
    }
    
    // writes specified data into EEEPROM beginning from specified address    
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemory(
            int startAddress, short[][] data, Collection<Node> targetNodes
    ) {
        if ( logger.isDebugEnabled() ){
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
        
        if ( (targetNodes == null) || (targetNodes.isEmpty()) ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeResult 
                = writeDataToMemoryOfThisNode(startAddress, data);
            
            logger.debug("writeDataToMemory - end: {}", writeResult);
            return writeResult;
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeResult 
            = writeDataToMemoryUsingBroadcast(startAddress, data, targetNodes);
        
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
        
        // get access to OS peripheral
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult 
                = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(OS.class))
            );
            
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
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                = createRequestProcessingError(os, "Load code result not found.");
            
            logger.debug("loadCodeUnicast - end: {}", servResult);
            return servResult;
        }
        
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put(this.contextNode.getId(), result.getResult());
        
        ServiceResult.Status status = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
        LoadCodeProcessingInfo loadCodeProcessingInfo = new LoadCodeProcessingInfo();
        
        if ( result.getResult() == false ) {
            status = ServiceResult.Status.ERROR;
            loadCodeProcessingInfo = new LoadCodeProcessingInfo( 
                    new LoadingContentError("Checksum does not match.")
            );
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
            = new BaseServiceResult<>(
                status,
                new LoadCodeResult(resultMap),
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
        
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult 
                = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(FRC.class))
                );
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        }
        
        short[] dpaRequestData = createLoadCodeRequestData(frc, params, length, dataChecksum);
        
        FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                dpaRequestData, targetNodes.toArray( new Node[] {}))
        );
        
        if ( result == null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                = createRequestProcessingError(frc, "Returning FRC result failed. ");
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        } 
        
        short[] extraResult = frc.extraResult();
        if ( extraResult == null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                    = createRequestProcessingError(frc, "Returning FRC extra result failed. ");
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        }
        
        // putting both parts of result together
        short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);

        // parsing result
        Map<String, FRC_AcknowledgedBroadcastBits.Result> parsedResultMap = null;
        try {
            parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
        } catch ( Exception ex ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( 
                            new RequestProcessingError("Parsing of result failed")
                    )
            );
            
            logger.debug("loadCodeBroadcast - end: {}", servResult);
            return servResult;
        }
        
        Map<String, Boolean> resultsMap = new HashMap<>();
        boolean loadFailed = false;
        
        // creating final results
        for ( Node node : targetNodes ) {
            Result parsedNodeResult = parsedResultMap.get(node.getId());

            DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
            if ( (devProcResult == DeviceProcResult.NOT_RESPOND) 
                || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
            ) {
                resultsMap.put(node.getId(), false);
                loadFailed = true;
            } else {
                resultsMap.put(node.getId(), true);
            }
        }
        
        ServiceResult.Status serviceStatus = ( loadFailed == false )?
                                              ServiceResult.Status.SUCCESSFULLY_COMPLETED
                                              : ServiceResult.Status.ERROR;
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
            = new BaseServiceResult<>(
                serviceStatus, 
                new LoadCodeResult(resultsMap),
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
        
        LoadingCodeProperties.LoadingContent loadingContent = params.getLoadingContent();
        if ( loadingContent == null ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                = new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( new LoadingContentError("Unspecified loading content."))
                );
            
            logger.debug("loadCode - end: {}", servResult);
            return servResult;
        }
        
        // indicates, whether data for writing into EEEPROM will be prepared for broadcast 
        boolean prepareDataForBroadcast = true;
        Collection<Node> targetNodes = params.getTargetNodes();
        
        if ( targetNodes == null || targetNodes.isEmpty() ) {
            if ( !isStartAddressValid(this.contextNode.getId(), params.getStartAddress()) ) {
                ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                    =  new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                    new LoadingContentError("Invalid start address.")
                            )
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
            }
            prepareDataForBroadcast = false;
        } else {
            // checking availability of target nodes
            if ( !areNodesAvalailable(targetNodes) ) {
                ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                    = new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( 
                                new ParamsError("Some nodes not available.")
                        )
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
            }
        }
        
        short[][] dataToWrite = null;
        final int length, dataChecksum;
        
        switch ( loadingContent ) {
            case Hex:
                // prepare with allocated size and after parse data
                IntelHex file = new IntelHex(0xFFFFFF);
                try {
                    file.parseIntelHex(params.getFileName());
                } catch ( IOException ex ) {
                    ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                        = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( new LoadingContentError(ex.getMessage()))
                    );
                    
                    logger.debug("loadCode - end: {}", servResult);
                    return servResult;
                }  
                
                // separating code block with custom DPA handler block
                CodeBlock handlerBlock = findHandlerBlock(file);
                if (  handlerBlock == null ) {
                    ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                        = new BaseServiceResult<>(
                            ServiceResult.Status.ERROR,
                            null,
                            new LoadCodeProcessingInfo( 
                                new LoadingContentError(
                                    "Selected .hex files does not include Custom DPA "
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
                logger.debug(" Length of data is: " + Integer.toHexString(length));
                
                // calculating checksum with initial value 1 (defined for DPA handler)
                dataChecksum = calculateChecksum(file, handlerBlock, length);
                logger.debug(" Checksum of data is: " + Integer.toHexString(dataChecksum));
                
                // prepare data to blocks for writing into EEEPROM
                file.getData().position(0);
                if ( prepareDataForBroadcast ) {
                    DataPreparer dataPreparer = new DataPreparer(handlerBlock, file);
                    dataToWrite = dataPreparer.prepareAs16BytesBlocks();
                    //dataToWrite = new DataPreparer(handlerBlock, file).prepareAs16BytesBlocks();
                } else {
                    dataToWrite = new DataPreparer(handlerBlock, file).prepare();
                }
                break;
            case IQRF_Plugin:
                // parse iqrf file
                IQRFParser parser = new IQRFParser(params.getFileName());
                byte[] parsedData = parser.parse();
                
                length = parsedData.length;
                logger.debug(" Length of data is: " + Integer.toHexString(length));
                
                dataChecksum = calculateChecksum(parsedData, length);
                logger.debug(" Checksum of data is: " + Integer.toHexString(dataChecksum));
                
                // prepare data to blocks for writing into EEEPROM
                if ( prepareDataForBroadcast ) {
                    dataToWrite = new DataPreparer(parsedData).prepareAs16BytesBlocks();
                } else {
                    dataToWrite = new DataPreparer(parsedData).prepare();
                }
                break;
            default:
                ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                    = new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( new LoadingContentError("Unsupported loading content."))
                );
                
                logger.debug("loadCode - end: {}", servResult);
                return servResult;
        }
        
        // writing data to memory
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataResult 
            = writeDataToMemory(
                    params.getStartAddress(), dataToWrite, params.getTargetNodes()
        );
        
        // if there was some fundamental error during data writing, it is useless to load code
        if ( writeDataResult.getProcessingInfo().getError() != null ) {
            logger.debug("loadCode - end: {}", writeDataResult);
            return writeDataResult;
        }
        
        // results of data write
        Map<String, Boolean> writeDataResultsMap = writeDataResult.getResult().getAllNodeResultsMap();
        
        // final results map
        Map<String, Boolean> finalResultsMap = new HashMap<>();
        
        // nodes, which has been written data successfully into
        List<Node> nodesToLoad = new LinkedList<>();
        
        // find out successfull nodes and store the unsucessful ones into final results map
        if ( params.getTargetNodes() != null ) {
            for ( Node node : params.getTargetNodes() ) {
                Boolean nodeResult = writeDataResultsMap.get(node.getId());
                if ( nodeResult == null || nodeResult == false ) {
                    finalResultsMap.put(node.getId(), false);
                    continue;
                }

                nodesToLoad.add(node);
            }
        }
        
        // possibly add context node into the target nodes
        if ( (params.getTargetNodes() == null) || (params.getTargetNodes().isEmpty()) ) {
            Boolean contextNodeResult = writeDataResultsMap.get(this.contextNode.getId());
            if ( (contextNodeResult != null) && (contextNodeResult == true) ) {
                nodesToLoad.add(contextNode);
            } else {
                finalResultsMap.put(this.contextNode.getId(), false);
            }
        }
        
        // there must be at least error in writing data into context node, which this
        // service resides on
        if ( nodesToLoad.isEmpty() ) {
            ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
                = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR,
                    new LoadCodeResult(finalResultsMap), 
                    new LoadCodeProcessingInfo()
            );
            
            logger.debug("loadCode - end: {}", servResult);
            return servResult;
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadResult = null;
        
        // if a node to load code into is the context node, use unicast else use broadcast
        if ( nodesToLoad.size() == 1 ) {
            Node nodeToLoadInto = nodesToLoad.get(0);
            if ( this.contextNode.getId().equals(nodeToLoadInto.getId()) ) {
                loadResult = loadCodeUnicast(params, length, dataChecksum);
            } else {
                loadResult = loadCodeBroadcast(params, length, dataChecksum, nodesToLoad);
            }
        } else {
            loadResult = loadCodeBroadcast(params, length, dataChecksum, nodesToLoad);
        }
        
        // constructing of final result
        if ( loadResult.getProcessingInfo().getError() != null ) {
            logger.debug("loadCode - end: {}", loadResult);
            return loadResult;
        }
        
        // put load results into final results map
        if ( loadResult.getResult() != null ) {
            finalResultsMap.putAll(loadResult.getResult().getAllNodeResultsMap());
        }
        
        ServiceResult.Status status = loadResult.getStatus();
        if ( writeDataResult.getStatus() == ServiceResult.Status.ERROR ) {
            status = ServiceResult.Status.ERROR;
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> servResult
            = new BaseServiceResult<>(
                status,
                new LoadCodeResult(finalResultsMap),
                new LoadCodeProcessingInfo()
        );
        
        logger.debug("loadCode - end: {}", servResult);
        return servResult;
    }

    @Override
    public void setServiceParameters(ServiceParameters params) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}