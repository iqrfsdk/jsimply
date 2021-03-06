/*
 * Copyright 2014 MICRORISC s.r.o.
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

package com.microrisc.simply.iqrf.dpa.v30x.init;

import com.microrisc.simply.compounddevices.CompoundDeviceConfiguration;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimpleDeviceObjectFactory;
import com.microrisc.simply.init.InitConfigSettings;
import com.microrisc.simply.iqrf.dpa.DPA_Node;
import com.microrisc.simply.iqrf.dpa.DPA_NodeImpl;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.compounddevices.CompoundDeviceObjectFactory;
import com.microrisc.simply.iqrf.dpa.v30x.devices.PeripheralInfoGetter;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.load_code.LoadCodeService;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.load_code.LoadCodeServiceFactory;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteConfigurationService;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteConfigurationServiceFactory;
import com.microrisc.simply.services.node.ServiceCreationInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating nodes.
 * 
 * @author Michal Konopa
 */
public final class NodeFactory {
    
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(NodeFactory.class);
    
    private static DPA_InitObjects<InitConfigSettings<Configuration, Map<String, Configuration>>>
            _initObjects = null;
    
    private static SimpleDeviceObjectFactory _devObjectFactory = null;
    
    /**
     * Creates and returns peripheral information object for specified node.
     */
    private static PeripheralInfoGetter createPerInfoObject(String networkId, String nodeId) 
            throws Exception {
        Class baseImplClass = _initObjects.getImplClassMapper().getImplClass(
                PeripheralInfoGetter.class
        );
        
        if ( _devObjectFactory == null ) {
            _devObjectFactory = new SimpleDeviceObjectFactory();
        }
        
        return (PeripheralInfoGetter)_devObjectFactory.getDeviceObject(
                networkId, nodeId, _initObjects.getConnectionStack().getConnector(),
                baseImplClass, _initObjects.getConfigSettings().getGeneralSettings()
        );
    }
    
    
    private static DPA_InitObjects<InitConfigSettings<Configuration, Map<String, Configuration>>>
    checkInitObjects(
            DPA_InitObjects<InitConfigSettings<Configuration, Map<String, Configuration>>> initObjects
    ) {
        if ( initObjects == null ) {
            throw new IllegalArgumentException("DPA initialization objects cannot be null");
        }
        return initObjects;
    }
    
    /**
     * Initializes the Node Factory.
     * @param initObjects DPA initialization objects
     */
    public static void init(
            DPA_InitObjects<InitConfigSettings<Configuration, Map<String, Configuration>>> initObjects
    ) {
        _initObjects = checkInitObjects(initObjects);
    }
    
    private static Map<Class, ServiceCreationInfo> createServCreationInfoMap() {
        Map<Class, ServiceCreationInfo> servCreationInfoMap = new HashMap<>();
        
        servCreationInfoMap.put(
                LoadCodeService.class, 
                new ServiceCreationInfo( new LoadCodeServiceFactory(), null)
        );
        
        servCreationInfoMap.put(
                WriteConfigurationService.class, 
                new ServiceCreationInfo( new WriteConfigurationServiceFactory(), null)
        );
        
        return servCreationInfoMap;
    }
    
    // creates devices corresponding to specified DPA periherals number and
    // add them into specified map
    private static void createAndAddPeripherals(
        String networkId, String nodeId, Set<Integer> perNumbers, Map<Class, DeviceObject> devices
    ) throws Exception {
        
        for ( int perId : perNumbers ) {
            Class devIface = _initObjects.getPeripheralToDevIfaceMapper().getDeviceInterface(perId);
            
            // IMPORTANT: if no device interface for specified peripheral number is found,
            // continue, not throw an exception
            if ( devIface == null ) {
                logger.warn("Interface not found for peripheral: {}", perId);
                continue;
            }
            
            Class implClass = _initObjects.getImplClassMapper().getImplClass(devIface);
            if ( implClass == null ) {
                throw new RuntimeException("Implementation for " + devIface.getName() + " not found");
            }
            
            if ( _devObjectFactory == null ) {
                _devObjectFactory = new SimpleDeviceObjectFactory();
            }
            
            // creating new device object for implementation class
            DeviceObject newDeviceObj = _devObjectFactory.getDeviceObject(
                    networkId, nodeId, _initObjects.getConnectionStack().getConnector(), 
                    implClass, _initObjects.getConfigSettings().getGeneralSettings()
            );
            
            // put object into service's map
            devices.put(devIface, newDeviceObj);
        }
    }
    
    // creates devices corresponding to specified compound devices and add them into the specified map
    private static void createAndAddCompoundDevices(
        List<CompoundDeviceConfiguration> compoundDevicesConfigList, Map<Class, DeviceObject> devices
    ) {
        if ( compoundDevicesConfigList == null ) {
            logger.warn(
                "List of compound's devices configuration object is null."
                + "No compound devices will be created."
            );
            return;
        }
        
        if ( compoundDevicesConfigList.isEmpty() ) {
            logger.warn(
                "List of compound's devices configuration object is empty."
                + "No compound devices will be created."
            );
            return;
        }
        
        for ( CompoundDeviceConfiguration compDevConfig : compoundDevicesConfigList ) {
            List<DeviceObject> internalDevices = new LinkedList<>();
            List<Class> devIfacesOfInternalDevices = compDevConfig.getDevIfacesOfInternalDevices();
            boolean internalDeviceNotFound = false;
            
            // getting the list of internal devices
            for ( Class devIface : devIfacesOfInternalDevices ) {
                DeviceObject devObject = devices.get(devIface);
                if ( devIface == null ) {
                    logger.error("Device object for internal device: {} not found", devIface);
                    internalDeviceNotFound = true;
                    break;
                }
                internalDevices.add(devObject);
            }
            
            // if some internal device has not been found, continue with next Compound Device
            if ( internalDeviceNotFound ) {
                internalDeviceNotFound = false;
                continue;
            }
            
            CompoundDeviceObjectFactory factory = compDevConfig.getFactory();
            if ( factory == null ) {
                logger.error(
                    "Factory for Compound Device Object: {} not found", compDevConfig.getImplClass()
                );
                continue;
            }
            
            CompoundDeviceObject compoundDeviceObject 
                = factory.getCompoundDeviceObject(
                        compDevConfig.getNetworkId(), compDevConfig.getNodeId(),
                        compDevConfig.getImplClass(), internalDevices, 
                        compDevConfig.getOtherSettings()
                );
            
            devices.put(compoundDeviceObject.getImplementedDeviceInterface(), compoundDeviceObject);
        }
    }
    
    /**
     * Creates and returns new node with specified peripherals.
     * @param networkId ID of network the node belongs to
     * @param nodeId ID of node to create
     * @param perNumbers peripheral numbers to create node services for
     * @param compoundDevicesConfigList list of configuration objects of compound
     *        devices related to node to create. If {@code null}, no compound devices
     *        will be created
     * @return new node
     * @throws java.lang.Exception if an error has occured during node creation
     */
    public static DPA_Node createNode(
        String networkId, String nodeId, Set<Integer> perNumbers, 
        List<CompoundDeviceConfiguration> compoundDevicesConfigList
    ) throws Exception 
    {
        logger.debug("createNode - start: networkId={}, nodeId={}, perNumbers={}",
                networkId, nodeId, Arrays.toString(perNumbers.toArray( new Integer[0] ))
        );
        
        // node devices
        Map<Class, DeviceObject> devices = new HashMap<>();
        
        // creating Peripheral Information object
        PeripheralInfoGetter perInfoObject = createPerInfoObject(networkId, nodeId);
        
        // put info object into service's map
        devices.put(PeripheralInfoGetter.class, (DeviceObject)perInfoObject);
        
        // creating devices corresponding to DPA peripherals
        createAndAddPeripherals(networkId, nodeId, perNumbers, devices);
        
        // creating compound devices
        createAndAddCompoundDevices(compoundDevicesConfigList, devices);
        
        // creating services info map
        Map<Class, ServiceCreationInfo> servCreationInfoMap = createServCreationInfoMap();
        
        DPA_Node node = new DPA_NodeImpl(networkId, nodeId, devices, servCreationInfoMap);
        
        logger.debug("createNode - end: {}", node);
        return node;
    }
    
    /**
     * Creates and returns new node with services for all peripherals.
     * @param networkId ID of network the node belongs to
     * @param nodeId ID of node to create
     * @param compoundDevicesConfigList list of configuration objects of compound
     *        devices related to node to create. If {@code null}, no compound devices
     *        will be created
     * @return new node
     * @throws java.lang.Exception if an error has occured during node creation
     */
    public static Node createNodeWithAllPeripherals(
        String networkId, String nodeId, List<CompoundDeviceConfiguration> compoundDevicesConfigList
    ) throws Exception 
    {
        Set<Integer> peripherals = _initObjects.getPeripheralToDevIfaceMapper().getMappedPeripherals();
        return createNode(networkId, nodeId, peripherals, compoundDevicesConfigList);
    }
}
