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
import com.microrisc.simply.DeviceObject;

/**
 * Base class for so called <b>'Compound Devices'</b>.
 * <p>
 * Compound Device is device, which <b>internally</b> uses other device(s), e.g.
 * peripheral in typical case, for its functioning.
 * 
 * @author Michal Konopa
 */
public abstract class CompoundDeviceObject extends BaseDeviceObject {
    
    /** Internal devices. */
    protected DeviceObject[] internalDevices;
    
    private static DeviceObject[] checkInternalDevices(DeviceObject[] internalDevices) {
        if ( internalDevices == null ) {
            throw new IllegalArgumentException("Internal devices cannot be null");
        }
        return internalDevices;
    }
    
    
    /**
     * Creates new Compound Device Object, which will be using specified devices.
     * 
     * @param networkId identifier of network, which this device belongs to.
     * @param nodeId identifier of node, which this device belongs to.
     * @param internalDevices internal devices to use
     * @throws IllegalArgumentException if {@code networkId} or {@code nodeId} 
     *         or {@code internalDevices} is {@code null}
     * @throws IllegalStateException if this device object doesn't implement no
     *         device interface
     */
    public CompoundDeviceObject(String networkId, String nodeId, DeviceObject... internalDevices) {
        super(networkId, nodeId);
        this.internalDevices = checkInternalDevices(internalDevices);
    }
    
}
