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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration;

import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.v22x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteResult.NodeWriteResultImpl;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.errors.ConfigFileParsingError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.errors.MissingPeripheralError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.errors.NoConfigurationDataError;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.errors.RequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits.Result;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_AcknowledgedBroadcastBits.Result.DeviceProcResult;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_Data;
import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.BaseServiceResult;
import com.microrisc.simply.services.ServiceResult;
import com.microrisc.simply.services.node.BaseService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of Write Configuration Service. 
 * 
 * @author Michal Konopa
 */
public final class WriteConfigurationServiceImpl 
extends BaseService implements WriteConfigurationService {
    
    // length [in number of bytes] of one configuration item in HWP_ConfigurationByte array
    private static final int CONFIG_ITEM_LEN = 3;
    
    private static final int FRC_COMMAND_LEN = 1;
    private static final int SELECTED_NODES_LEN = 30;
    
    private static final int FOURSOME_LEN = 1 
            + DPA_ProtocolProperties.PNUM_LENGTH
            + DPA_ProtocolProperties.PCMD_LENGTH
            + DPA_ProtocolProperties.HW_PROFILE_LENGTH
            ;
    
    // length [in number of bytes] of payload usable for config items 
    // in acknowledged broadcast
    private static final int BROADCAST_PAYLOAD_LEN 
            = DPA_ProtocolProperties.PDATA_MAX_LENGTH
            - FRC_COMMAND_LEN
            - SELECTED_NODES_LEN
            - FOURSOME_LEN
            ;
    
    
    // returns maximal length of config bytes data chunk for unicast
    // [in number of HWP_ConfigurationByte]
    private static int getMaxChunkLengthForUnicast() {
        return (DPA_ProtocolProperties.PDATA_MAX_LENGTH / CONFIG_ITEM_LEN);
    }
    
    // returns maximal length of config bytes data chunk for broadcast
    // [in number of HWP_ConfigurationByte]
    private static int getMaxChunkLengthForBroadcast() {
        return (BROADCAST_PAYLOAD_LEN / CONFIG_ITEM_LEN);
    }
    
    // converts specified array into map indexed by config byte addresses
    private static Map<Integer, HWP_ConfigurationByte> toMap(
            HWP_ConfigurationByte[] hwpConfigBytes
    ) {
        Map<Integer, HWP_ConfigurationByte> resultMap = new HashMap<>();
        for ( HWP_ConfigurationByte configByte : hwpConfigBytes ) {
            resultMap.put(configByte.getAddress(), configByte);
        }
        
        return resultMap;
    }
    
    // writes configuration to this contextNode
    private ServiceResult<WriteResult, WriteConfigurationProcessingInfo>  
        writeConfigurationToThisNode(HWP_ConfigurationByte[] hwpConfigBytes, int hwpId) 
    {
        OS os = this.contextNode.getDeviceObject(OS.class);
        if ( os == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new MissingPeripheralError(OS.class))
            );
        }
        
        os.setRequestHwProfile(hwpId);
        
        int configBytePos = 0;
        int maxChunkLen = getMaxChunkLengthForUnicast();
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytes = new HashMap<>();
        
        while ( configBytePos < hwpConfigBytes.length ) {
            int chunkLen = maxChunkLen;
            if ( (configBytePos + chunkLen) > hwpConfigBytes.length ) {
                chunkLen = hwpConfigBytes.length - configBytePos;
            }
            
            HWP_ConfigurationByte[] configBytesChunk = new HWP_ConfigurationByte[chunkLen];
            
            System.arraycopy(
                    hwpConfigBytes, configBytePos, configBytesChunk, 0, configBytesChunk.length
            );

            VoidType result = os.writeHWPConfigurationByte(configBytesChunk);
            if ( result == null ) {
                // add all config bytes from chunk into failed writing map
                for ( HWP_ConfigurationByte configByte : configBytesChunk ) {
                    writingFailedBytes.put(configByte.getAddress(), configByte);
                }
            }
            
            configBytePos += chunkLen;
        }
        
        WriteResult.NodeWriteResult nodeWriteResult 
                = new WriteResult.NodeWriteResultImpl(toMap(hwpConfigBytes), writingFailedBytes);
        Map<String, WriteResult.NodeWriteResult> nodeResultsMap = new HashMap<>();
        nodeResultsMap.put(((DeviceObject)os).getNodeId(), nodeWriteResult);
        
        ServiceResult.Status serviceStatus = ( writingFailedBytes.isEmpty() )?
                                              ServiceResult.Status.SUCCESSFULLY_COMPLETED
                                              : ServiceResult.Status.ERROR;
        
        return new BaseServiceResult<>(
                serviceStatus, 
                new WriteResult(nodeResultsMap),
                new WriteConfigurationProcessingInfo()
        );
    }
    
        
    // returns map of config bytes, which failed to write
    private Map<Integer, HWP_ConfigurationByte> getWritingFailedBytes(
        int configBytePos, int chunkLen, HWP_ConfigurationByte[] hwpConfigBytes 
    ) {
        Map<Integer, HWP_ConfigurationByte> writingFailedBytes = new HashMap<>();

        // add all config bytes from chunk into failed writing map
        for ( int i = (configBytePos-1); i >= (configBytePos - chunkLen); i-- ) {
            writingFailedBytes.put(hwpConfigBytes[i].getAddress(), hwpConfigBytes[i]);
        }
        
        return writingFailedBytes;
    }
    
    // adds configuration bytes into contextNode results map for specified contextNode
    private void addConfigBytesIntoMap(
            Map<Integer, HWP_ConfigurationByte> bytesToWrite,
            Map<Integer, HWP_ConfigurationByte> writingFailedBytes, 
            Map<String, WriteResult.NodeWriteResult> nodeResultsMap,
            String nodeId
    ) {
        NodeWriteResultImpl nodeResult = (WriteResult.NodeWriteResultImpl)nodeResultsMap.get(nodeId);
        if ( nodeResult == null ) {
            nodeResultsMap.put(
                nodeId, new WriteResult.NodeWriteResultImpl(bytesToWrite, writingFailedBytes)
            );
        } else {
            nodeResult.getWritingFailedBytes().putAll(writingFailedBytes);
        }
    } 
    
    // adds configuration bytes into contextNode results map for
    // each contextNode in the specified collection
    private void addConfigBytesIntoNodeResultsMap(
            Map<Integer, HWP_ConfigurationByte> bytesToWrite,
            Map<Integer, HWP_ConfigurationByte> writingFailedBytes, 
            Map<String, WriteResult.NodeWriteResult> nodeResultsMap,
            Collection<Node> nodes
    ) {
        for ( Node node : nodes ) {
            NodeWriteResultImpl nodeResult = (WriteResult.NodeWriteResultImpl)nodeResultsMap.get(node.getId());
            if ( nodeResult == null ) {
                nodeResultsMap.put(
                    node.getId(), 
                    new WriteResult.NodeWriteResultImpl(bytesToWrite, writingFailedBytes)
                );
            } else {
                nodeResult.getWritingFailedBytes().putAll(writingFailedBytes);
            }
        }
    }      
    
    // puts both parts of FRC result together a returns the result
    private short[] getCompleteFrcResult(short[] firstPart, short[] extraResult) {
        short[] completeResult = new short[firstPart.length + extraResult.length];
        System.arraycopy(firstPart, 0, completeResult, 0, firstPart.length);
        System.arraycopy(extraResult, 0, completeResult, firstPart.length, extraResult.length);
        return completeResult;
    }
    
    // writes configuration to nodes using broadcast
    private ServiceResult<WriteResult, WriteConfigurationProcessingInfo> 
        writeConfigurationUsingBroadcast(
                HWP_ConfigurationByte[] hwpConfigBytes, Collection<Node> targetNodes,
                int hwpId
    ) {
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        if ( frc == null ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new MissingPeripheralError(FRC.class))
            );
        }
        
        frc.setRequestHwProfile(hwpId);
        
        int configBytePos = 0;
        int maxChunkLen = getMaxChunkLengthForBroadcast();
        
        Map<Integer, HWP_ConfigurationByte> bytesToWriteMap = toMap(hwpConfigBytes);
        Map<String, WriteResult.NodeWriteResult> nodeResultsMap = new HashMap<>();
        boolean writeFailed = false;
        
        while ( configBytePos < hwpConfigBytes.length ) {
            int chunkLen = maxChunkLen;
            if ( (configBytePos + chunkLen) > hwpConfigBytes.length ) {
                chunkLen = hwpConfigBytes.length - configBytePos;
            }
            
            short[] foursome = new short[FOURSOME_LEN];
            foursome[0] = (short)(FOURSOME_LEN + chunkLen * CONFIG_ITEM_LEN);
            foursome[1] = DPA_ProtocolProperties.PNUM_Properties.OS; 
            foursome[2] = 0x09;
            foursome[3] = (short)(hwpId & 0xFF);
            foursome[4] = (short)((hwpId >> 8) & 0xFF);
            
            short[] configBytesChunk = new short[chunkLen * CONFIG_ITEM_LEN];
            
            int byteId = 0;
            while ( (byteId-1) < (configBytesChunk.length - CONFIG_ITEM_LEN) ) {
                configBytesChunk[byteId++] = (short)hwpConfigBytes[configBytePos].getAddress();
                configBytesChunk[byteId++] = (short)hwpConfigBytes[configBytePos].getValue();
                configBytesChunk[byteId++] = (short)hwpConfigBytes[configBytePos].getMask();
                
                configBytePos++;
            }
            
            short[] dpaRequestData = new short[foursome.length + configBytesChunk.length];
            System.arraycopy(foursome, 0, dpaRequestData, 0, foursome.length);
            System.arraycopy(configBytesChunk, 0, dpaRequestData, foursome.length, configBytesChunk.length);
            
            FRC_Data result = frc.sendSelective( new FRC_AcknowledgedBroadcastBits(
                    dpaRequestData, targetNodes.toArray( new Node[] {}))
            );
            
            // getting the first part of the result
            if ( result == null ) {
                addConfigBytesIntoNodeResultsMap(
                        bytesToWriteMap,
                        getWritingFailedBytes(configBytePos, chunkLen, hwpConfigBytes),
                        nodeResultsMap, 
                        targetNodes
                );
                
                writeFailed = true;
                continue;
            }
            
            // getting the remainder of the result
            short[] extraResult = frc.extraResult();
            if ( extraResult == null ) {
                addConfigBytesIntoNodeResultsMap(
                        bytesToWriteMap,
                        getWritingFailedBytes(configBytePos, chunkLen, hwpConfigBytes),
                        nodeResultsMap, 
                        targetNodes
                );
                
                writeFailed = true;
                continue;
            }
            
            // put both parts together
            short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);
            
            // parsing result
            Map<String, Result> parsedResultMap = null;
            try {
                parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
            } catch ( Exception ex ) {
                return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    new WriteResult(nodeResultsMap), 
                    new WriteConfigurationProcessingInfo( 
                            new RequestProcessingError("Parsing of result failed")
                    )
                );
            }
            
            // adding config bytes, which failed to write
            for ( Node node : targetNodes ) {
                Result parsedNodeResult = parsedResultMap.get(node.getId());
                
                DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
                if ( (devProcResult == DeviceProcResult.NOT_RESPOND) 
                    || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
                ) {
                    Map<Integer, HWP_ConfigurationByte> writingFailedBytes
                        = getWritingFailedBytes(configBytePos, chunkLen, hwpConfigBytes);
                    addConfigBytesIntoMap(bytesToWriteMap, writingFailedBytes, nodeResultsMap, node.getId());
                    writeFailed = true;
                }
            }
        }
        
        // adding bytes to write to NodeWrite results for nodes without write error
        for ( Node node : targetNodes ) {
            if ( !nodeResultsMap.containsKey(node.getId()) ) {
                WriteResult.NodeWriteResultImpl nodeResult 
                    = new NodeWriteResultImpl(
                            bytesToWriteMap, 
                            new HashMap<Integer, HWP_ConfigurationByte>()
                    );
                nodeResultsMap.put(node.getId(), nodeResult);
            }
        }
        
        ServiceResult.Status serviceStatus = ( writeFailed == false )?
                                              ServiceResult.Status.SUCCESSFULLY_COMPLETED
                                              : ServiceResult.Status.ERROR;
        
        return new BaseServiceResult<>(
                serviceStatus, 
                new WriteResult(nodeResultsMap),
                new WriteConfigurationProcessingInfo()
        );
    }    
        
        
    /**
     * Creates new object of Write Configuration Service.
     * 
     * @param node contextNode in context
     * @throws IllegalArgumentException if {@code contextNode} is empty
     */
    public WriteConfigurationServiceImpl(Node node) {
        super(node);
        if ( node == null ) {
            throw new IllegalArgumentException("Node in context cannot be null.");
        }
    }
    
    @Override
    public ServiceResult<WriteResult, WriteConfigurationProcessingInfo> 
        writeConfiguration(WriteConfigurationServiceParameters params) 
    {
        HWP_ConfigurationByte[] hwpConfigBytes = null;
        try {
            hwpConfigBytes = XmlConfigurationParser.parse(
                    params.getDefFileName(), params.getUserSettingsFileName()
            );
        } catch ( XmlConfigurationParserException ex ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new ConfigFileParsingError(ex) )
            );
        }
        
        // no bytes to write
        if ( (hwpConfigBytes == null) || (hwpConfigBytes.length == 0) ) {
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new NoConfigurationDataError() )
            );
        }
        
        Collection<Node> targetNodes = params.getTargetNodes();
        if ( (targetNodes == null) || (targetNodes.isEmpty()) ) {
            return writeConfigurationToThisNode(hwpConfigBytes, params.getHwpId());
        }
        
        return writeConfigurationUsingBroadcast(hwpConfigBytes, targetNodes, params.getHwpId());
    }
    
}
