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
import com.microrisc.simply.di_services.CallErrorsService_sync;
import com.microrisc.simply.di_services.CallRequestProcessingService_sync;
import com.microrisc.simply.di_services.WaitingTimeoutService;
import com.microrisc.simply.iqrf.dpa.v22x.di_services.DPA_AdditionalInfoService;
import com.microrisc.simply.devices.protronix.dpa22x.types.VOCSensorData;

/**
 * VoC Sensor Device Interface.
 * <p>
 * IMPORTANT NOTE: <br>
 * Every method returns {@code NULL}, if an error has occurred during processing
 * of this method.
 * 
 * @author Michal Konopa
 */
@DeviceInterface
public interface VOCSensor
extends WaitingTimeoutService, CallRequestProcessingService_sync, 
        CallErrorsService_sync, DPA_AdditionalInfoService 
{
    
    /**
     * Returns data from the sensor.
     * @return sensor data <br>
     *         {@code null}, if an error has occurred during processing
     */
    VOCSensorData get();
}
