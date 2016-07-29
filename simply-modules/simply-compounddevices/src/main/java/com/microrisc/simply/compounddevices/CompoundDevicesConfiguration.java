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

import java.util.List;

/**
 * Interface for access to configurations of Compound Devices. 
 * 
 * @author Michal Konopa
 */
public interface CompoundDevicesConfiguration {
    
    /**
     * Returns list of configuration objects for all compound devices belonging
     * to node (target node) as specified by its ID and network ID.
     * @param networkId ID of network to which belongs the target node
     * @param nodeId ID of the target node
     * @return list of configuration objects for all compound devices belonging
     *         to node as specified by its ID and network ID.
     */
    List<CompoundDeviceConfiguration> getDevicesConfigurations(String networkId, String nodeId);
    
    /**
     * Returns list of configuration objects for all compound devices.
     * @return list of configuration objects for all compound devices.
     */
    List<CompoundDeviceConfiguration> getAllDevicesConfigurations();

}
