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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration;

import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.v30x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v30x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v30x.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteResult.NodeWriteResultImpl;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.errors.ConfigFileParsingError;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.errors.MissingPeripheralError;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.errors.NoConfigurationDataError;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.errors.RequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_AcknowledgedBroadcastBits;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_AcknowledgedBroadcastBits.Result;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_AcknowledgedBroadcastBits.Result.DeviceProcResult;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_Data;
import com.microrisc.simply.iqrf.dpa.v30x.types.HWP_ConfigurationByte;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.BaseServiceResult;
import com.microrisc.simply.services.ServiceResult;
import com.microrisc.simply.services.node.BaseService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of Write Configuration Service. 
 * 
 * @author Michal Konopa
 */
public final class WriteConfigurationServiceImpl 
extends BaseService implements WriteConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WriteConfigurationServiceImpl.class);
    
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
    
    // write security attributes for specified node using unicast
    private WriteResult.SecurityResult setSecurityUsingUnicast(ConfigSettings.Security security) {
        OS os = this.contextNode.getDeviceObject(OS.class);
       
        boolean passwordToWrite = false;
        boolean passwordWriteResult = false;

        if ( security.getPassword() != null ) {
            passwordToWrite = true;
            if ( os.setSecurity(0, security.getPassword()) != null ) {
                passwordWriteResult = true;
            }
        }

        boolean keyToWrite = false;
        boolean keyWriteResult = false;

        if ( security.getKey() != null ) {
            keyToWrite = true;
            if ( os.setSecurity(1, security.getKey()) != null ) {
                keyWriteResult = true;
            }
        }

        return new WriteResult.SecurityResultImpl(
                passwordToWrite, passwordWriteResult, keyToWrite, keyWriteResult
        );
    }
    
    // indicates, whether setting of security attibutes was successful or not
    private boolean settingSecuritySuccessful(WriteResult.SecurityResult result) {
        if ( result.isPasswordToWrite() && result.getPaswordWriteResult() == false ) {
            return false;
        }
        
        if ( result.isKeyToWrite() && result.getKeyWriteResult() == false ) {
            return false;
        }
        
        return true;
    }
    
    // indicates, whether setting of security attibutes was successful or not
    private boolean settingSecuritySuccessful(
            Map<String, WriteResult.SecurityResult> results
    ) {
        for ( WriteResult.SecurityResult result : results.values() ) {
            if ( !settingSecuritySuccessful(result) ) {
                return false;
            }
        }
        
        return true;
    }
    
    // writes configuration to this contextNode
    private ServiceResult<WriteResult, WriteConfigurationProcessingInfo>  
        writeConfigurationToThisNode(ConfigSettings configSettings, int hwpId) 
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
        
        
        // 1.PART - SETTING SECURITY
        WriteResult.SecurityResult securityResult = setSecurityUsingUnicast(configSettings.getSecurity());
        
        
        
        // 2.PART - WRITING CONFIGURATION BYTES
        HWP_ConfigurationByte[] hwpConfigBytes = configSettings.getHwpConfigBytes();
        
        // no bytes to write
        if ( (hwpConfigBytes == null) || (hwpConfigBytes.length == 0) ) {
            logger.warn("Configuration bytes are missing");
            
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new NoConfigurationDataError() )
            );
        }
        
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
                = new WriteResult.NodeWriteResultImpl(
                        toMap(hwpConfigBytes), 
                        writingFailedBytes,
                        securityResult
                );
        Map<String, WriteResult.NodeWriteResult> nodeResultsMap = new HashMap<>();
        nodeResultsMap.put(((DeviceObject)os).getNodeId(), nodeWriteResult);
        
        ServiceResult.Status serviceStatus = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
        if ( !writingFailedBytes.isEmpty() || !settingSecuritySuccessful(securityResult) ) {
            serviceStatus = ServiceResult.Status.ERROR;
        } 
        
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
            WriteResult.SecurityResult securityResult,
            String nodeId
    ) {
        NodeWriteResultImpl nodeResult = (WriteResult.NodeWriteResultImpl)nodeResultsMap.get(nodeId);
        if ( nodeResult == null ) {
            nodeResultsMap.put(
                nodeId, 
                new WriteResult.NodeWriteResultImpl(bytesToWrite, writingFailedBytes, securityResult)
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
            Map<String, WriteResult.SecurityResult> securityResults,
            Collection<Node> nodes
    ) {
        for ( Node node : nodes ) {
            NodeWriteResultImpl nodeResult = (WriteResult.NodeWriteResultImpl)nodeResultsMap.get(node.getId());
            if ( nodeResult == null ) {
                nodeResultsMap.put(
                    node.getId(), 
                    new WriteResult.NodeWriteResultImpl(
                            bytesToWrite, writingFailedBytes, securityResults.get(node.getId())
                    )
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
    
    // returns map with all values set to false
    private Map<String, Boolean> getFalseMap(Collection<Node> nodes) {
        Map<String, Boolean> falseMap = new HashMap<>();
        
        for ( Node node : nodes ) {
            falseMap.put(node.getId(), Boolean.FALSE);
        }
        
        return falseMap;
    }
    
    // type of security datas 
    private static enum SecurityDataType {
        PASSWORD, 
        KEY
    }
    
    // sets security data into nodes
    private Map<String, Boolean> setSecurityDataUsingBroadcast(
            short[] data,
            Collection<Node> targetNodes,
            int hwpId,
            SecurityDataType secDataType
    ) {
        short[] foursome = new short[FOURSOME_LEN];
        foursome[0] = (short)(FOURSOME_LEN + data.length + 1);
        foursome[1] = DPA_ProtocolProperties.PNUM_Properties.OS; 
        foursome[2] = 0x07;
        foursome[3] = (short)(hwpId & 0xFF);
        foursome[4] = (short)((hwpId >> 8) & 0xFF);
        
        short[] dpaRequestData = new short[foursome[0]];
        System.arraycopy(foursome, 0, dpaRequestData, 0, foursome.length);
        
        switch ( secDataType ) {
            case PASSWORD:
                dpaRequestData[foursome.length] = 0;
                break;
            case KEY:
                dpaRequestData[foursome.length] = 1;
                break;
            default:
                throw new IllegalStateException("Unsupported security data type: " + secDataType);
        }
        
        System.arraycopy(data, 0, dpaRequestData, foursome.length+1, data.length);
        
        FRC frc = this.contextNode.getDeviceObject(FRC.class);
        
        FRC_Data result = frc.sendSelective( 
                new FRC_AcknowledgedBroadcastBits(
                    dpaRequestData, targetNodes.toArray( new Node[] {})
                )
        );
        
        // results map
        Map<String, Boolean> resultsMap = new HashMap<>();
        
        // getting the first part of the result
        if ( result == null ) {
            return getFalseMap(targetNodes);
        }

        // getting the remainder of the result
        short[] extraResult = frc.extraResult();
        if ( extraResult == null ) {
            return getFalseMap(targetNodes);
        }

        // put both parts together
        short[] completeResult = getCompleteFrcResult(result.getData(), extraResult);

        // parsing result
        Map<String, Result> parsedResultMap = null;
        try {
            parsedResultMap = FRC_AcknowledgedBroadcastBits.parse(completeResult);
        } catch ( Exception ex ) {
            return getFalseMap(targetNodes);
        }

        // adding config bytes, which failed to write
        for ( Node node : targetNodes ) {
            Result parsedNodeResult = parsedResultMap.get(node.getId());

            DeviceProcResult devProcResult = parsedNodeResult.getDeviceProcResult();
            if ( 
                ( devProcResult == DeviceProcResult.NOT_RESPOND ) 
                || ( devProcResult == DeviceProcResult.HWPID_NOT_MATCH )
            ) {
                resultsMap.put(node.getId(), Boolean.FALSE);
            } else {
                resultsMap.put(node.getId(), Boolean.TRUE);
            }
        }
        
        return resultsMap;
    }
    
    // sets security settings to nodes using broadcast
    Map<String, WriteResult.SecurityResult> setSecurityUsingBroadcast(
            ConfigSettings.Security settings, 
            Collection<Node> targetNodes,
            int hwpId
    ) {
        if ( settings == null ) {
            return null;
        }
        
        Map<String, Boolean> passwordResultsMap = null;
        
        short[] password = settings.getPassword();
        if ( password != null ) {
            passwordResultsMap = setSecurityDataUsingBroadcast(
                password, targetNodes, hwpId, SecurityDataType.PASSWORD
            );
        }
        
        Map<String, Boolean> keyResultsMap = null;
        
        short[] key = settings.getKey();
        if ( key != null ) {
            keyResultsMap = setSecurityDataUsingBroadcast(
                key, targetNodes, hwpId, SecurityDataType.KEY
            );
        }
       
        // putting result together
        Map<String, WriteResult.SecurityResult> resultsMap = new HashMap<>();
        for ( Node node : targetNodes ) {
            boolean passwordToWrite = false;
            boolean passwordWriteResult = false;
            boolean keyToWrite = false;
            boolean keyWriteResult = false;
            
            if ( password != null ) {
                passwordToWrite = true;
                if ( 
                    passwordResultsMap.get(node.getId()) != null 
                    && passwordResultsMap.get(node.getId()) == true     
                ) {
                    passwordWriteResult = true;
                }
            }
            
            if ( key != null ) {
                keyToWrite = true;
                if ( 
                    keyResultsMap.get(node.getId()) != null 
                    && keyResultsMap.get(node.getId()) == true     
                ) {
                    keyWriteResult = true;
                }
            }
            
            resultsMap.put(
                    node.getId(), 
                    new WriteResult.SecurityResultImpl(
                            passwordToWrite, 
                            passwordWriteResult, 
                            keyToWrite, 
                            keyWriteResult
                    )
            );
        }
        
        return resultsMap;
    }
    
    // writes configuration to nodes using broadcast
    private ServiceResult<WriteResult, WriteConfigurationProcessingInfo> 
        writeConfigurationUsingBroadcast(
                ConfigSettings configSettings, 
                Collection<Node> targetNodes,
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
        
        // 1.part - set security attributes using broadcast
        Map<String, WriteResult.SecurityResult> securityResults
            = setSecurityUsingBroadcast(
                configSettings.getSecurity(),
                targetNodes,
                hwpId
        ); 
        
        
        
        // 2.part - writing configuration bytes
        HWP_ConfigurationByte[] hwpConfigBytes = configSettings.getHwpConfigBytes();
        
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
                        securityResults,
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
                        securityResults,
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
                if ( 
                    (devProcResult == DeviceProcResult.NOT_RESPOND) 
                    || (devProcResult == DeviceProcResult.HWPID_NOT_MATCH)
                ) {
                    Map<Integer, HWP_ConfigurationByte> writingFailedBytes
                        = getWritingFailedBytes(configBytePos, chunkLen, hwpConfigBytes);
                    addConfigBytesIntoMap(
                            bytesToWriteMap, 
                            writingFailedBytes, 
                            nodeResultsMap,
                            securityResults.get(node.getId()),
                            node.getId()
                    );
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
                            new HashMap<Integer, HWP_ConfigurationByte>(),
                            securityResults.get(node.getId())
                    );
                nodeResultsMap.put(node.getId(), nodeResult);
            }
        }
        
        ServiceResult.Status serviceStatus = ServiceResult.Status.SUCCESSFULLY_COMPLETED;
        if ( writeFailed || !settingSecuritySuccessful(securityResults) ) {
            serviceStatus = ServiceResult.Status.ERROR;
        }
        
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
        logger.debug("writeConfiguration - start: params={}", params);
        
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> result = null;
        
        ConfigSettings configSettings = null;
        try {
            configSettings = XmlConfigurationParser.parse(
                    params.getDefFileName(), 
                    params.getUserSettingsFileName()
            );
        } catch ( XmlConfigurationParserException ex ) {
            result = new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new ConfigFileParsingError(ex) )
            );
            
            logger.debug("writeConfiguration - end: {}", result);
            return result;
        }
        
        // basic checking of security attributes
        if ( configSettings.getSecurity() == null ) {
            logger.warn("Security settings are missing");
            
            return new BaseServiceResult<>(
                    ServiceResult.Status.ERROR, 
                    null, 
                    new WriteConfigurationProcessingInfo( new NoConfigurationDataError() )
            );
        }
        
        Collection<Node> targetNodes = params.getTargetNodes();
        if ( (targetNodes == null) || (targetNodes.isEmpty()) ) {
            result = writeConfigurationToThisNode(configSettings, params.getHwpId());
            
            logger.debug("writeConfiguration - end: {}", result);
            return result;
        }
        
        result = writeConfigurationUsingBroadcast(configSettings, targetNodes, params.getHwpId());
        
        logger.debug("writeConfiguration - end: {}", result);
        return result;
    }
    
}
