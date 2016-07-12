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

import com.microrisc.simply.iqrf.dpa.v22x.CompoundDeviceObjectFactory;
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

    
    /**
     * Creates new otherSettings for Compound Device Object.
     * 
     * @param networkId network ID
     * @param nodeId node ID
     * @param implClass class implementing Device Interface
     * @param devIfacesOfInternalDevices Device Interfaces of internal devices
     * @param otherSettings other configuration settings
     * @param factory factory
     */
    public CompoundDeviceConfiguration(
        String networkId, String nodeId, Class implClass, List<Class> devIfacesOfInternalDevices,
        Configuration otherSettings, CompoundDeviceObjectFactory factory
    ) {
        this.networkId = networkId;
        this.nodeId = nodeId;
        this.implClass = implClass;
        this.devIfacesOfInternalDevices = devIfacesOfInternalDevices;
        this.otherSettings = otherSettings;
        this.factory = factory;
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
