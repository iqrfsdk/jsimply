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

import com.microrisc.simply.DeviceObject;
import java.util.List;
import org.apache.commons.configuration.Configuration;

/**
 * Factory for creation of {@link CompoundDeviceObject} objects.
 * 
 * @author Michal Konopa
 */
public interface CompoundDeviceObjectFactory {
    
    /**
     * Returns Compound Device Object according to specified parameters.
     * 
     * @param networkId ID of network, which returned Compound Device Object will be belonging to
     * @param nodeId ID of node, which returned Compound Device Object will be belonging to
     * @param implClass class implementing Device Interface of the returned Device Object
     * @param internalDevices devices, which the returned Compound Device Object will be using 
     * @param configuration configuration settings 
     * @return Compound Device Object
     */
    CompoundDeviceObject getCompoundDeviceObject(
            String networkId, String nodeId, Class implClass, 
            List<DeviceObject> internalDevices, Configuration configuration
    );
}
