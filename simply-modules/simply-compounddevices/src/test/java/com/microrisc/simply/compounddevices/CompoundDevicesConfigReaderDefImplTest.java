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
package com.microrisc.simply.compounddevices;

import com.microrisc.simply.DeviceInterface;
import com.microrisc.simply.DeviceObject;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple test of {@link CompoundDevicesConfigReaderDefImpl}
 * 
 * @author Michal Konopa
 */
public class CompoundDevicesConfigReaderDefImplTest {
    
    @DeviceInterface
    public static interface TestingInternalDeviceInterface {
        Object doSomething();
    }
    
    @DeviceInterface
    public static interface TestingDeviceInterface {
        Object doSomething();
    }
    
    // testing compound device
    public static class TestingCompoundDevice 
    extends CompoundDeviceObject implements TestingDeviceInterface {
        
        public TestingCompoundDevice(String networkId, String nodeId, DeviceObject... internalDevices) {
            super(networkId, nodeId, internalDevices);
        }

        @Override
        public Object doSomething() {
            return new Object();
        }
        
    }
    
    
    public CompoundDevicesConfigReaderDefImplTest() {
    }

    /**
     * Test of read method.
     */
    @Test
    public void testRead() throws ConfigurationException {
        Configuration configuration 
            = new PropertiesConfiguration("Simply_ConfigReaderDefImplTest.properties");
        
        CompoundDevicesConfigReaderDefImpl instance = new CompoundDevicesConfigReaderDefImpl();
        CompoundDevicesConfiguration result = instance.read(configuration);
        
        // getDevicesConfigurations test
        List<CompoundDeviceConfiguration> devConfigList = result.getDevicesConfigurations("1", "1");
        assertEquals(1, devConfigList.size());
        
        CompoundDeviceConfiguration devConfig = devConfigList.get(0);
        assertNotNull(devConfig);
        
        assertEquals("1", devConfig.getNetworkId());
        assertEquals("1", devConfig.getNodeId());
        assertEquals(TestingCompoundDevice.class, devConfig.getImplClass());
        
        assertEquals(1, devConfig.getDevIfacesOfInternalDevices().size());
        assertArrayEquals( new Class[] {TestingInternalDeviceInterface.class} , devConfig.getDevIfacesOfInternalDevices().toArray());
        
        assertNotNull(devConfig.getOtherSettings());
        assertEquals(CompoundDeviceObjectFactoryDefImpl.class, devConfig.getFactory().getClass());
        
        
        // getAllDevicesConfigurations test
        List<CompoundDeviceConfiguration> allDevConfigList = result.getAllDevicesConfigurations();
        assertEquals(1, devConfigList.size());
        
        devConfig = allDevConfigList.get(0);
        assertNotNull(devConfig);
        
        assertEquals("1", devConfig.getNetworkId());
        assertEquals("1", devConfig.getNodeId());
        assertEquals(TestingCompoundDevice.class, devConfig.getImplClass());
        
        assertEquals(1, devConfig.getDevIfacesOfInternalDevices().size());
        assertArrayEquals( new Class[] {TestingInternalDeviceInterface.class} , devConfig.getDevIfacesOfInternalDevices().toArray());
        
        assertNotNull(devConfig.getOtherSettings());
        assertEquals(CompoundDeviceObjectFactoryDefImpl.class, devConfig.getFactory().getClass());
    }
    
}
