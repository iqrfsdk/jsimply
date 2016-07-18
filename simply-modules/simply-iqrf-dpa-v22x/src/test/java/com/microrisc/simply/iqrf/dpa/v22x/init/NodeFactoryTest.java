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
package com.microrisc.simply.iqrf.dpa.v22x.init;

import com.microrisc.simply.DeviceInterface;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.NetworkData;
import com.microrisc.simply.NetworkLayerListener;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.config.ConfigurationReader;
import com.microrisc.simply.iqrf.dpa.DPA_Node;
import com.microrisc.simply.iqrf.dpa.v22x.CompoundDeviceObject;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.network.AbstractNetworkLayer;
import com.microrisc.simply.network.AbstractNetworkLayerFactory;
import com.microrisc.simply.network.NetworkConnectionStorage;
import com.microrisc.simply.network.NetworkLayer;
import com.microrisc.simply.network.NetworkLayerException;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests of {@link NodeFactory}. 
 * 
 * @author Michal Konopa
 */
public class NodeFactoryTest {
    
    /**
     * Testing network layer.
     * 
     * @author Michal Konopa
     */
    public static final class TestingNetworkLayer extends AbstractNetworkLayer {
    
        public TestingNetworkLayer(NetworkConnectionStorage connectionStorage) {
            super(connectionStorage);
        }
    
        @Override
        public void start() throws SimplyException {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void registerListener(NetworkLayerListener listener) {
        }

        @Override
        public void unregisterListener() {
        }

        @Override
        public void sendData(NetworkData data) throws NetworkLayerException {
        }
    
    }
    
    /**
     * Testing network layer factory.
     * 
     * @author Michal Konopa
     */
    public static final class TestingNetworkLayerFactory 
    extends AbstractNetworkLayerFactory<Configuration, NetworkLayer> {
        
        public TestingNetworkLayerFactory() {}
        
        @Override
        public NetworkLayer getNetworkLayer(
                NetworkConnectionStorage connectionStorage, Configuration configuration
        ) throws Exception {
            return new TestingNetworkLayer(connectionStorage);
        }

   }
    
    @DeviceInterface
    public static interface TestingDeviceInterface {
        VoidType doSomething();
    }
    
    // testing compound device
    public static class TestingCompoundDevice 
    extends CompoundDeviceObject implements TestingDeviceInterface {
        
        public TestingCompoundDevice(String networkId, String nodeId, DeviceObject... internalDevices) {
            super(networkId, nodeId, internalDevices);
        }

        @Override
        public VoidType doSomething() {
            return new VoidType();
        }
        
    }
    
    private static DPA_InitializerConfiguration dpaInitConfig;
    
    
    public NodeFactoryTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        Configuration configuration = ConfigurationReader.fromFile(
                "compound_devices" + File.separator + "Simply_NodeFactoryTest.properties"
        );
        
        SimpleDPA_InitObjects initObjects = (new DPA_InitObjectsFactory()).getInitObjects(configuration);
        NodeFactory.init(initObjects);
        
        dpaInitConfig = DPA_InitializerConfigurationFactory
                .getDPA_InitializerConfiguration(initObjects.getConfigSettings().getGeneralSettings()
        );
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


    @Test
    public void testCreateNode() throws Exception {
        String networkId = "1";
        String nodeId = "1";
        Set<Integer> perNumbers = new HashSet<>();
        List<CompoundDeviceConfiguration> compoundDevicesConfigList 
                = dpaInitConfig.getCompoundDevicesConfiguration().getDevicesConfigurations(networkId, nodeId);
        
        DPA_Node result = NodeFactory.createNode(networkId, nodeId, perNumbers, compoundDevicesConfigList);
        
        assertEquals(networkId, result.getNetworkId());
        assertEquals(nodeId, result.getId());
        
        TestingDeviceInterface compoundDevice = result.getDeviceObject(TestingDeviceInterface.class);
        assertNotNull(compoundDevice);
        
        TestingCompoundDevice testingCompoundDevice = (TestingCompoundDevice) compoundDevice;
        assertEquals(networkId, testingCompoundDevice.getNetworkId());
        assertEquals(nodeId, testingCompoundDevice.getNodeId());
        assertEquals(TestingDeviceInterface.class, testingCompoundDevice.getImplementedDeviceInterface());
    }
    
}
