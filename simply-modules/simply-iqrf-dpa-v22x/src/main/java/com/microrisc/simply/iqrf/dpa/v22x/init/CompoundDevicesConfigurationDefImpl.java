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

import java.util.LinkedList;
import java.util.List;

/**
 * Default implementation of {@link CompoundDevicesConfiguration} interface.
 * 
 * @author Michal Konopa
 */
public final class CompoundDevicesConfigurationDefImpl implements CompoundDevicesConfiguration {
    
    // list of config objects for each compound device
    private final List<CompoundDeviceConfiguration> configurations;
    
    
    private List<CompoundDeviceConfiguration> checkConfigurations(
            List<CompoundDeviceConfiguration> configurations
    ) {
        if ( configurations == null ) {
            throw new IllegalArgumentException("Configurations cannot be null.");
        }
        return configurations;
    }
    
    
    /**
     * Creates new object of {@code CompoundDevicesConfigurationDefImpl} according
     * to specified list of source configuration objects for each compound device.
     * 
     * @param configurations list of source configuration objects for compound devices
     */
    public CompoundDevicesConfigurationDefImpl(List<CompoundDeviceConfiguration> configurations) {
        this.configurations = checkConfigurations(configurations);
    }
    
    /**
     * Returns list of configuration objects for all compound devices belonging
     * to node (target node) as specified by its ID and network ID.
     * @param networkId ID of network to which belongs the target node
     * @param nodeId ID of the target node
     * @return list of configuration objects for all compound devices belonging
     *         to node as specified by its ID and network ID.
     */
    @Override
    public List<CompoundDeviceConfiguration> getDevicesConfigurations(
        String networkId, String nodeId
    ) {
        List<CompoundDeviceConfiguration> listToReturn = new LinkedList<>();
        for ( CompoundDeviceConfiguration compoundDevConfig : configurations ) {
            if ( compoundDevConfig.getNetworkId().equals(networkId)
                && compoundDevConfig.getNodeId().equals(nodeId) ) {
                listToReturn.add(compoundDevConfig);
            }
        }
        
        return listToReturn;
    }
    
    /**
     * Returns list of configuration objects for all compound devices.
     * @return list of configuration objects for all compound devices.
     */
    @Override
    public List<CompoundDeviceConfiguration> getAllDevicesConfigurations() {
        return new LinkedList<>(configurations);
    }
}
