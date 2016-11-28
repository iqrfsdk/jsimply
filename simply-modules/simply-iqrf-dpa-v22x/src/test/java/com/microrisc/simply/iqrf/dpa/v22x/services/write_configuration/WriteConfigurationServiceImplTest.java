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
package com.microrisc.simply.iqrf.dpa.v22x.services.write_configuration;

import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationServiceParameters;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteResult;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationProcessingInfo;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationServiceImpl;
import com.microrisc.simply.BaseNode;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.v22x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_CollectedBits;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_Data;
import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.ServiceResult;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests of Write Configurtion Service default implementation.
 * 
 * @author Michal Konopa
 */
public class WriteConfigurationServiceImplTest {
    
    // FRC Acknowledged broadcast test result
    private static class FrcTestResult implements FRC_CollectedBits {
        private final byte bit0;
        private final byte bit1;
        
        public FrcTestResult(byte bit0, byte bit1) {
            this.bit0 = bit0;
            this.bit1 = bit1;
        }
        
        @Override
        public byte getBit0() {
            return bit0;
        }

        @Override
        public byte getBit1() {
            return bit1;
        }
        
    }
    
    // complete returned value of FRC ACK broadcast 
    private static class FRC_AckBroadcastReturnValue {
        private final FRC_Data sendDataRetValue;
        private final short[] extraResultRetValue;
        
        public FRC_AckBroadcastReturnValue(FRC_Data sendDataRetValue, short[] extraResultRetValue) {
            this.sendDataRetValue = sendDataRetValue;
            this.extraResultRetValue = extraResultRetValue;
        }
    }
    
    // length of total FRC output data
    private static final int FRC_DATA_LEN_TOTAL = 64;
    
    // length of FRC output data returned from FRC send* methods - for one response
    private static final int FRC_DATA_LEN_RESPONSE = 55;
    
    
    // creates return value for FRC send methods
    private FRC_AckBroadcastReturnValue createFrcAckBroadcastReturnValue(
            Map<String, FrcTestResult> expResultsMap
    ) {
        final int FIRST_BIT_START_INDEX = 0;
        final int SECOND_BIT_START_INDEX = 32;
        
        // FRC acknowledged broadcast responses from nodes
        short[] allData = new short[FRC_DATA_LEN_TOTAL];
        for ( Map.Entry<String, FrcTestResult> testResultEntry : expResultsMap.entrySet() ) {
            int nodeId = Integer.decode(testResultEntry.getKey());
            FrcTestResult testResult = testResultEntry.getValue();
            
            if ( testResult.bit0 == 1 ) {
                int dataByteIndex = FIRST_BIT_START_INDEX + nodeId / 8;
                int dataBitIndex = nodeId % 8;
                allData[dataByteIndex] |= (short)Math.pow(2, dataBitIndex);
            }
            
            if ( testResult.bit1 == 1 ) {
                int dataByteIndex = SECOND_BIT_START_INDEX + nodeId / 8;
                int dataBitIndex = nodeId % 8;
                allData[dataByteIndex] |= (short)Math.pow(2, dataBitIndex);
            }
        }
        
        // separation logic
        short[] sendSelectiveDataBuffer = new short[FRC_DATA_LEN_RESPONSE];
        System.arraycopy(allData, 0, sendSelectiveDataBuffer, 0, sendSelectiveDataBuffer.length);
        FRC_Data sendSelectiveData = new FRC_Data(0, sendSelectiveDataBuffer);
        
        // constructing return data for extraResult method 
        short[] extraResultDataBuffer = new short[FRC_DATA_LEN_TOTAL - FRC_DATA_LEN_RESPONSE];
        System.arraycopy(allData, FRC_DATA_LEN_RESPONSE, 
                extraResultDataBuffer, 0, extraResultDataBuffer.length
        );
        
        return new FRC_AckBroadcastReturnValue(sendSelectiveData, extraResultDataBuffer);
    }
    
    
    public WriteConfigurationServiceImplTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    // test bytes to write map
    private static void testBytesToWriteMap(
            Map<Integer, HWP_ConfigurationByte> bytesToWriteMap
    ) {
        assertEquals(bytesToWriteMap.size(), 12);
        
        HWP_ConfigurationByte configByte = bytesToWriteMap.get(1);
        assertEquals(configByte.getAddress(), 1);
        assertEquals(configByte.getValue(), 0b11111000);
        assertEquals(configByte.getMask(), 0b11111000);
        
        configByte = bytesToWriteMap.get(2);
        assertEquals(configByte.getAddress(), 2);
        assertEquals(configByte.getValue(), 0b00100110);
        assertEquals(configByte.getMask(), 0b00111111);
        
        configByte = bytesToWriteMap.get(5);
        assertEquals(configByte.getAddress(), 5);
        assertEquals(configByte.getValue(), 0b00000011);
        assertEquals(configByte.getMask(), 0b00111111);
        
        configByte = bytesToWriteMap.get(6);
        assertEquals(configByte.getAddress(), 6);
        assertEquals(configByte.getValue(), 42);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(8);
        assertEquals(configByte.getAddress(), 8);
        assertEquals(configByte.getValue(), 1);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(9);
        assertEquals(configByte.getAddress(), 9);
        assertEquals(configByte.getValue(), 5);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(10);
        assertEquals(configByte.getAddress(), 10);
        assertEquals(configByte.getValue(), 6);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(11);
        assertEquals(configByte.getAddress(), 11);
        assertEquals(configByte.getValue(), 3);
        assertEquals(configByte.getMask(), 0b00000111);
        
        configByte = bytesToWriteMap.get(12);
        assertEquals(configByte.getAddress(), 12);
        assertEquals(configByte.getValue(), 0);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(17);
        assertEquals(configByte.getAddress(), 17);
        assertEquals(configByte.getValue(), 52);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(18);
        assertEquals(configByte.getAddress(), 18);
        assertEquals(configByte.getValue(), 5);
        assertEquals(configByte.getMask(), 0xFF);
        
        configByte = bytesToWriteMap.get(0x20);
        assertEquals(configByte.getAddress(), 0x20);
        assertEquals(configByte.getValue(), 0b11010101);
        assertEquals(configByte.getMask(), 0b11010101);
    }
    
    
    /**
     * Unicast ok.
     */
    @Test
    public void unicastOk() {
        //OS mockedOs = mock(SimpleOS.class);
        /*
        OS mockedOs = mock(SimpleOS.class);
        when(mockedOs.writeHWPConfigurationByte(argThat( new ArrayOfHwpConfigBytes())))
                .thenReturn( new VoidType());
        //when(mockedOs.setRequestHwProfile(1)).
        when(((DeviceObject)mockedOs).getNodeId()).thenReturn("1");
        
        */
        TestingOs os = new TestingOs("1", "1");
        os.setWriteHWPConfigurationByteReturnValue(new VoidType());
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(OS.class, (DeviceObject)os);
        WriteConfigurationServiceImpl writeConfigService 
                = new WriteConfigurationServiceImpl( new BaseNode("1", "1", deviceObjects));
        
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        WriteConfigurationServiceParameters params 
                = new WriteConfigurationServiceParameters(defFileName, userSettingsFileName);
        
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(params);
        
        assertEquals(serviceResult.getStatus(), ServiceResult.Status.SUCCESSFULLY_COMPLETED);
        assertNull(serviceResult.getProcessingInfo().getError());
        
        WriteResult writeResult = serviceResult.getResult();
        WriteResult.NodeWriteResult nodeResult = writeResult.getNodeResult("1");
        assertNotNull(nodeResult);
        
        testBytesToWriteMap(nodeResult.getBytesToWrite());
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytesMap = nodeResult.getWritingFailedBytes();
        assertEquals(writingFailedBytesMap.size(), 0);
    }
    
    /**
     * Unicast failed - null result from setWriteHWPConfigurationByte method.
     */
    @Test
    public void unicastFailed_nullResult() {
        TestingOs os = new TestingOs("1", "1");
        os.setWriteHWPConfigurationByteReturnValue(null);
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(OS.class, (DeviceObject)os);
        WriteConfigurationServiceImpl writeConfigService 
                = new WriteConfigurationServiceImpl(new BaseNode("1", "1", deviceObjects));
        
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        WriteConfigurationServiceParameters params 
                = new WriteConfigurationServiceParameters(defFileName, userSettingsFileName);
        
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(params);
        
        assertEquals(serviceResult.getStatus(), ServiceResult.Status.ERROR);
        assertNull(serviceResult.getProcessingInfo().getError());
        
        WriteResult writeResult = serviceResult.getResult();
        WriteResult.NodeWriteResult nodeResult = writeResult.getNodeResult("1");
        assertNotNull(nodeResult);
        
        testBytesToWriteMap(nodeResult.getBytesToWrite());
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytesMap = nodeResult.getWritingFailedBytes();
        assertEquals(writingFailedBytesMap.size(), 12);
    }
    
    /**
     * Broadcast ok.
     */
    @Test
    public void broadcastOk() {
        TestingFrc frc = new TestingFrc("1", "1");
        
        // returned results from FRC send
        Map<String, FrcTestResult> retResults = new HashMap<>();
        retResults.put("1", new FrcTestResult((byte)1, (byte)1));
        
        FRC_AckBroadcastReturnValue frcRetValue = createFrcAckBroadcastReturnValue(retResults);
        frc.setSendSelectiveReturnValue(frcRetValue.sendDataRetValue);
        frc.setExtraResultReturnValue(frcRetValue.extraResultRetValue);
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(FRC.class, (DeviceObject)frc);
        WriteConfigurationServiceImpl writeConfigService 
                = new WriteConfigurationServiceImpl(new BaseNode("1", "1", deviceObjects));
        
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        Node node1 = new BaseNode("1", "1", deviceObjects);
        Collection<Node> nodes = new LinkedList<>();
        nodes.add(node1);
        
        WriteConfigurationServiceParameters params 
                = new WriteConfigurationServiceParameters(defFileName, userSettingsFileName, nodes);
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(params);
        
        assertEquals(serviceResult.getStatus(), ServiceResult.Status.SUCCESSFULLY_COMPLETED);
        assertNull(serviceResult.getProcessingInfo().getError());
        
        WriteResult writeResult = serviceResult.getResult();
        WriteResult.NodeWriteResult nodeResult = writeResult.getNodeResult("1");
        assertNotNull(nodeResult);
        
        testBytesToWriteMap(nodeResult.getBytesToWrite());
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytesMap = nodeResult.getWritingFailedBytes();
        assertEquals(writingFailedBytesMap.size(), 0);
    }
    
    /**
     * Broadcast failed - null result from sendSelective method.
     */
    @Test
    public void broadcastFailed_nullResult() {
        TestingFrc frc = new TestingFrc("1", "1");
        frc.setSendSelectiveReturnValue(null);
        frc.setExtraResultReturnValue(null);
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(FRC.class, (DeviceObject)frc);
        WriteConfigurationServiceImpl writeConfigService 
                = new WriteConfigurationServiceImpl(new BaseNode("1", "1", deviceObjects));
        
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        Node node1 = new BaseNode("1", "1", deviceObjects);
        Collection<Node> nodes = new LinkedList<>();
        nodes.add(node1);
        
        WriteConfigurationServiceParameters params 
                = new WriteConfigurationServiceParameters(defFileName, userSettingsFileName, nodes);
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(params);
        
        assertEquals(serviceResult.getStatus(), ServiceResult.Status.ERROR);
        assertNull(serviceResult.getProcessingInfo().getError());
        
        WriteResult writeResult = serviceResult.getResult();
        WriteResult.NodeWriteResult nodeResult = writeResult.getNodeResult("1");
        assertNotNull(nodeResult);
        
        testBytesToWriteMap(nodeResult.getBytesToWrite());
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytesMap = nodeResult.getWritingFailedBytes();
        assertEquals(writingFailedBytesMap.size(), 12);
    }
    
    /**
     * Broadcast failed - HWP not match
     */
    @Test
    public void broadcastFailed_hwpNotMatch() {
        TestingFrc frc = new TestingFrc("1", "1");
        
        Map<String, FrcTestResult> retResults = new HashMap<>();
        
        // node didn't respond - first bit = 0
        retResults.put("1", new FrcTestResult((byte)0, (byte)1));
        FRC_AckBroadcastReturnValue frcRetValue = createFrcAckBroadcastReturnValue(retResults);
        frc.setSendSelectiveReturnValue(frcRetValue.sendDataRetValue);
        frc.setExtraResultReturnValue(frcRetValue.extraResultRetValue);
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(FRC.class, (DeviceObject)frc);
        WriteConfigurationServiceImpl writeConfigService 
                = new WriteConfigurationServiceImpl(new BaseNode("1", "1", deviceObjects));
        
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        Node node1 = new BaseNode("1", "1", deviceObjects);
        Collection<Node> nodes = new LinkedList<>();
        nodes.add(node1);
        
        WriteConfigurationServiceParameters params 
                = new WriteConfigurationServiceParameters(defFileName, userSettingsFileName, nodes);
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(params);
        
        assertEquals(serviceResult.getStatus(), ServiceResult.Status.ERROR);
        assertNull(serviceResult.getProcessingInfo().getError());
        
        WriteResult writeResult = serviceResult.getResult();
        WriteResult.NodeWriteResult nodeResult = writeResult.getNodeResult("1");
        assertNotNull(nodeResult);
        
        testBytesToWriteMap(nodeResult.getBytesToWrite());
        
        Map<Integer, HWP_ConfigurationByte> writingFailedBytesMap = nodeResult.getWritingFailedBytes();
        assertEquals(writingFailedBytesMap.size(), 12);
    }
}
