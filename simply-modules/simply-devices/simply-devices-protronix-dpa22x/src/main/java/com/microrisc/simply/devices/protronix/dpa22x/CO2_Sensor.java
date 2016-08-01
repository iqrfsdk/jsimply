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
package com.microrisc.simply.devices.protronix.dpa22x;

import com.microrisc.simply.DeviceInterface;
import com.microrisc.simply.DeviceInterfaceMethodId;
import com.microrisc.simply.di_services.GenericAsyncCallable;
import com.microrisc.simply.di_services.MethodIdTransformer;
import com.microrisc.simply.di_services.StandardServices;
import com.microrisc.simply.iqrf.dpa.v22x.protronix.types.CO2_SensorData;
import java.util.UUID;

/**
 * CO2 Sensor Device Interface.
 * <p>
 * IMPORTANT NOTE: <br>
 * Every method returns {@code NULL}, if an error has occurred during processing
 * of this method.
 * 
 * @author Michal Konopa
 */
@DeviceInterface
public interface CO2_Sensor
extends StandardServices, GenericAsyncCallable, MethodIdTransformer {
    
    /**
     * Identifiers of this device interface's methods.
     */
    enum MethodID implements DeviceInterfaceMethodId {
        GET
    }
    
    
    // ASYNCHRONOUS METHODS
    
    /**
     * Sends method call request for getting data from the sensor.
     * @return unique identifier of sent request
     */
    UUID async_get();
    
    
    // SYNCHRONOUS WRAPPERS
    
    /**
     * Synchronous wrapper for {@link #async_get() async_get} method.
     * @return sensor data <br>
     *         {@code null}, if an error has occurred during processing
     */
    CO2_SensorData get();
}
