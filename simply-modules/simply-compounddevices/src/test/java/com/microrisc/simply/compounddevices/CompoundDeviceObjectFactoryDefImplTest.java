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

import com.microrisc.simply.BaseDeviceObject;
import com.microrisc.simply.DeviceInterface;
import com.microrisc.simply.DeviceObject;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple test of {@link CompoundDeviceObjectFactoryDefImpl}
 * 
 * @author Michal Konopa
 */
public class CompoundDeviceObjectFactoryDefImplTest {
    
    @DeviceInterface
    public static interface TestingInternalDeviceInterface {
    }
    
    public static class TestingInternalDevice 
    extends BaseDeviceObject implements TestingInternalDeviceInterface {
        
        public TestingInternalDevice(String networkId, String nodeId) {
            super(networkId, nodeId);
        }
        
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
    
    // testing compound device with one internal
    public static class TestingCompoundDeviceWithOneInternal 
    extends CompoundDeviceObject implements TestingDeviceInterface {
        
        public TestingCompoundDeviceWithOneInternal(
                String networkId, String nodeId, DeviceObject internalDevice
        ) {
            super(networkId, nodeId, internalDevice);
        }
        
        
        @Override
        public Object doSomething() {
            return new Object();
        }
        
    }
    
    
    public CompoundDeviceObjectFactoryDefImplTest() {
    }
    

    /**
     * Test of getCompoundDeviceObject method.
     */
    @Test
    public void testGetCompoundDeviceObject() {
        CompoundDeviceObjectFactoryDefImpl factory = new CompoundDeviceObjectFactoryDefImpl();
        assertNotNull(factory);
        
        String networkId = "1";
        String nodeId = "1";
        Class implClass = TestingCompoundDevice.class;
        
        List<DeviceObject> internalDevices = new LinkedList<>();
        DeviceObject internalDevice = new TestingInternalDevice(networkId, nodeId);
        internalDevices.add(internalDevice);
        
        Configuration configuration = null;
        
        CompoundDeviceObject result = factory
                .getCompoundDeviceObject(networkId, nodeId, implClass, internalDevices, configuration);
        
        assertNotNull(result);
        assertEquals(networkId, result.getNetworkId());
        assertEquals(nodeId, result.getNodeId());
        assertEquals(TestingDeviceInterface.class, result.getImplementedDeviceInterface());
        
        assertEquals(1, result.internalDevices.length);
        assertEquals(internalDevice, result.internalDevices[0]);
    }
    
    /**
     * Test of getCompoundDeviceObject method.
     * Testing of creation of Compound Device Object with one internal device.
     */
    @Test
    public void testGetCompoundDeviceObject_2() {
        CompoundDeviceObjectFactoryDefImpl factory = new CompoundDeviceObjectFactoryDefImpl();
        assertNotNull(factory);
        
        String networkId = "1";
        String nodeId = "1";
        Class implClass = TestingCompoundDeviceWithOneInternal.class;
        
        List<DeviceObject> internalDevices = new LinkedList<>();
        DeviceObject internalDevice = new TestingInternalDevice(networkId, nodeId);
        internalDevices.add(internalDevice);
        
        Configuration configuration = null;
        
        CompoundDeviceObject result = factory
                .getCompoundDeviceObject(networkId, nodeId, implClass, internalDevices, configuration);
        
        assertNotNull(result);
        assertEquals(networkId, result.getNetworkId());
        assertEquals(nodeId, result.getNodeId());
        assertEquals(TestingDeviceInterface.class, result.getImplementedDeviceInterface());
        
        assertEquals(1, result.internalDevices.length);
        assertEquals(internalDevice, result.internalDevices[0]);
    }
    
}
