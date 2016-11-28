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
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
      return calculateChecksum(file.getData().asShortBuffer(), handlerBlock, 
              CRC_INIT_VALUE_HEX, length);
   }

   private int calculateChecksum(short[] data, int length){
      CodeBlock block = new CodeBlock(0, data.length);
      ShortBuffer buffer = ShortBuffer.wrap(data);
      return calculateChecksum(buffer, block, CRC_INIT_VALUE_IQRF, length);
   }
   
   private int calculateChecksum(ShortBuffer buffer, CodeBlock handlerBlock,
           int checkSumInitialValue, int length) {
      logger.debug("calculateChecksum - start: buffer={}, handlerBlock={}, checkSumInitialValue={}, length={}",
              buffer, handlerBlock, checkSumInitialValue, length);
      int dataChecksum = checkSumInitialValue;
      // checksum for data
      for (long address = handlerBlock.getAddressStart();
              address < handlerBlock.getAddressStart() + length;
              address++) {
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
        }
        
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put(this.contextNode.getId(), true);
        
        return new BaseServiceResult<>(
                ServiceResult.Status.SUCCESSFULLY_COMPLETED,
                new LoadCodeResult(resultMap),
                new LoadCodeProcessingInfo()
        );
    }    
    
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataToMemoryUsingBroadcast(
            int startAddress, short[][] data, Collection<Node> targetNodes
    ) {
        /*
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(FRC.class))
            );
        }
        */
        //frc.setRequestHwProfile(hwpId);
        return new BaseServiceResult<>(
                ServiceResult.Status.ERROR,
                null,
                new LoadCodeProcessingInfo(  new RequestProcessingError("Currenlty not implemented."))
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
        
        logger.debug("writeDataToMemory - end");
        
        if ( (targetNodes == null) || (targetNodes.isEmpty()) ) {
            return writeDataToMemoryOfThisNode(startAddress, data);
        }
        
        return writeDataToMemoryUsingBroadcast(startAddress, data, targetNodes);
    }
   
    
    // loads code into context contextNode using unicast
    private ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadCodeUnicast(
        LoadCodeServiceParameters params, int length, int dataChecksum
    ) { 
        // get access to OS peripheral
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(OS.class))
            );
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

        logger.debug("loadCode - end");
        
        if ( result == null ) {
            return createRequestProcessingError(os, "Load code result not found.");
        }
        
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put(this.contextNode.getId(), result.getResult());
        
        ServiceResult.Status status = (result.getResult() == true)? 
                ServiceResult.Status.SUCCESSFULLY_COMPLETED : ServiceResult.Status.ERROR;  
        
        return new BaseServiceResult<>(
                status,
                new LoadCodeResult(resultMap),
                new LoadCodeProcessingInfo()
        );
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
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new LoadCodeProcessingInfo( new MissingPeripheralError(FRC.class))
            );
        }
        
        short[] dpaRequestData = createLoadCodeRequestData(frc, params, length, dataChecksum);
        
        FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                dpaRequestData, targetNodes.toArray( new Node[] {}))
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
        
        return new BaseServiceResult<>(
                serviceStatus, 
                new LoadCodeResult(resultsMap),
                new LoadCodeProcessingInfo()
        );
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
        short[][] dataToWrite = null;
        final int length, dataChecksum;
        
        LoadingCodeProperties.LoadingContent loadingContent = params.getLoadingContent();
        if ( loadingContent == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR,
                    null,
                    new LoadCodeProcessingInfo( new LoadingContentError("Unspecified loading content."))
            );
        }
        
        switch ( loadingContent ) {
            case Hex:
                // prepare with allocated size and after parse data
                IntelHex file = new IntelHex(0xFFFFFF);
                try {
                    file.parseIntelHex(params.getFileName());
                } catch ( IOException ex ) {
                    return new BaseServiceResult<>(
                        ServiceResult.Status.ERROR,
                        null,
                        new LoadCodeProcessingInfo( new LoadingContentError(ex.getMessage()))
                    );
                }  
                
                // separating code block with custom DPA handler block
                CodeBlock handlerBlock = findHandlerBlock(file);
                if (  handlerBlock == null ) {
                    return new BaseServiceResult<>(
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
                }
                
                logger.debug(
                        " Handler block starts at " + handlerBlock.getAddressStart()
                                + " and ends at " + handlerBlock.getAddressEnd()
                ); 
                
                // calcualting rounded length of handler in memory
                length = (int) ((handlerBlock.getLength() + (64 - 1)) & ~(64 - 1));
                
                // calculating checksum with initial value 1 (defined for DPA handler)
                dataChecksum = calculateChecksum(file, handlerBlock, length);
                logger.debug(" Checksum of data is: " + Integer.toHexString(dataChecksum));
                
                // prepare data to 48 and 16 long blocks for writing
                dataToWrite = new DataPreparer(handlerBlock, file).prepare();
                break;
            case IQRF_Plugin:
                // parse iqrf file
                IQRFParser parser = new IQRFParser(params.getFileName());
                short[] parsedData = parser.parse();
                length = parsedData.length;
                logger.debug(" Length of data is: " + Integer.toHexString(length));
                dataChecksum = calculateChecksum(parsedData, length);
                logger.debug(" Checksum of data is: " + Integer.toHexString(dataChecksum));
                
                // prepare data to 48 and 16 long blocks for writing
                dataToWrite = new DataPreparer(parsedData).prepare();
                break;
            default:
                logger.debug("loadCode - end");
                return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR,
                    null,
                    new LoadCodeProcessingInfo( new LoadingContentError("Unsupported loading content."))
                );
        }
        
        // writing data to memory
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> writeDataResult 
            = writeDataToMemory(
                    params.getStartAddress(), dataToWrite, params.getTargetNodes()
        );
        
        // if there was some fundamental error during data writing it is useless to load code
        if ( writeDataResult.getProcessingInfo().getError() != null ) {
            return writeDataResult;
        }
        
        // write results
        Map<String, Boolean> writeDataResultsMap = writeDataResult.getResult().getAllNodeResultsMap();
        
        // final results map
        Map<String, Boolean> finalResultsMap = new HashMap<>();
        
        // nodes, which has been written data successfully into
        List<Node> nodesToLoad = new LinkedList<>();
        
        // find out successfull nodes and store the unsucessful ones into final results map 
        for ( Node node : params.getTargetNodes() ) {
            Boolean nodeResult = writeDataResultsMap.get(node.getId());
            if ( nodeResult == null || nodeResult == false ) {
                finalResultsMap.put(node.getId(), false);
                continue;
            }
            
            nodesToLoad.add(node);
        }
        
        // there must be at least one error in writing data into contextNode, which this
        // service resides on
        if ( nodesToLoad.isEmpty() ) {
            return new BaseServiceResult<>(
                ServiceResult.Status.ERROR,
                new LoadCodeResult(finalResultsMap), 
                new LoadCodeProcessingInfo()
            );
        }
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> loadResult = null;
        
        // if a node to load code into is the context node, use unicast
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
        
        return new BaseServiceResult<>(
                status,
                new LoadCodeResult(finalResultsMap),
                new LoadCodeProcessingInfo()
        );
    }

    @Override
    public void setServiceParameters(ServiceParameters params) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}