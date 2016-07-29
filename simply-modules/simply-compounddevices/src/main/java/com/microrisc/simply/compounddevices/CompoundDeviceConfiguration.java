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

import com.microrisc.simply.compounddevices.CompoundDeviceObjectFactory;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.configuration.Configuration;

/**
 * Configuration of Compound Device.
 * 
 * @author Michal Konopa
 */
public final class CompoundDeviceConfiguration {
    
    // ID of network
    private final String networkId;
    
    // ID of node
    private final String nodeId;
    
    // class implementing the Device Interface of Coumpound Device Object
    private final Class implClass;
    
    // Device Interfaces of internal devices
    private final List<Class> devIfacesOfInternalDevices;
    
    // other configuration settings
    private final Configuration otherSettings;
    
    // factory for creation
    private final CompoundDeviceObjectFactory factory;
    
    
    private static String checkNetworkId(String networkId) {
        if ( networkId == null ) {
            throw new IllegalArgumentException("Network ID cannot be null.");
        }
        
        if ( networkId.isEmpty() ) {
            throw new IllegalArgumentException("Network ID cannot be empty string.");
        }
        
        return networkId;
    }
    
    private static String checkNodeId(String nodeId) {
        if ( nodeId == null ) {
            throw new IllegalArgumentException("Node ID cannot be null.");
        }
        
        if ( nodeId.isEmpty() ) {
            throw new IllegalArgumentException("Node ID cannot be empty string.");
        }
        
        return nodeId;
    }
    
    private static Class checkImplClass(Class implClass) {
        if ( implClass == null ) {
            throw new IllegalArgumentException("Implementing class cannot be null.");
        }
        return implClass;
    }
    
    private static List<Class> checkDevIfacesOfInternalDevices(
            List<Class> devIfacesOfInternalDevices
    ) {
        if ( devIfacesOfInternalDevices == null ) {
            throw new IllegalArgumentException(
                    "Device Interfaces list of internal devices cannot be null"
            );
        }
        return devIfacesOfInternalDevices;
    }
    
    private static CompoundDeviceObjectFactory checkFactory(CompoundDeviceObjectFactory factory) {
        if ( factory == null ) {
            throw new IllegalArgumentException("Factory cannot be null.");
        }
        return factory;
    }
    
    
    /**
     * Creates new configuration object for Compound Device Object.
     * 
     * @param networkId network ID
     * @param nodeId node ID
     * @param implClass class implementing Device Interface
     * @param devIfacesOfInternalDevices Device Interfaces of internal devices
     * @param otherSettings other configuration settings
     * @param factory factory
     * @throws IllegalArgumentException if: <br>
     *         - any of the argument, except for {@code otherSettings} is {@code null} <br>
     *         - {@code networkId} or {@code nodeId} is empty string
     */
    public CompoundDeviceConfiguration(
        String networkId, String nodeId, Class implClass, List<Class> devIfacesOfInternalDevices,
        Configuration otherSettings, CompoundDeviceObjectFactory factory
    ) {
        this.networkId = checkNetworkId(networkId);
        this.nodeId = checkNodeId(nodeId);
        this.implClass = checkImplClass(implClass);
        this.devIfacesOfInternalDevices = checkDevIfacesOfInternalDevices(devIfacesOfInternalDevices);
        this.otherSettings = otherSettings;
        this.factory = checkFactory(factory);
    }
    
    /**
     * @return the network Id
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @return the node Id
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return the class implementing the Device Interface
     */
    public Class getImplClass() {
        return implClass;
    }

    /**
     * @return the Device Interfaces of internal devices
     */
    public List<Class> getDevIfacesOfInternalDevices() {
        return new LinkedList<>(this.devIfacesOfInternalDevices);
    }

    /**
     * @return the other settings
     */
    public Configuration getOtherSettings() {
        return otherSettings;
    }

    /**
     * @return the factory
     */
    public CompoundDeviceObjectFactory getFactory() {
        return factory;
    }
    
}
