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

package com.microrisc.opengateway.app;

import java.util.List;

/**
 * Holding APP params from JSON config file
 * 
 * @author Rostislav Spinar
 */
public class APPConfig {
    private long pollingPeriod;
    private long numberOfDevices;
    private List<Device> devices;

    /**
     * @return the numberOfSensors
     */
    public long getNumberOfDevices() {
        return numberOfDevices;
    }

    /**
     * @param numberOfDevices the numberOfDevices to set
     */
    public void setNumberOfDevices(long numberOfDevices) {
        this.numberOfDevices = numberOfDevices;
    }

    /**
     * @return the pullingPeriod
     */
    public long getPollingPeriod() {
        return pollingPeriod;
    }

    /**
     * @param pullingPeriod the pullingPeriod to set
     */
    public void setPollingPeriod(long pullingPeriod) {
        this.pollingPeriod = pullingPeriod;
    }

    /**
     * @return the devices
     */
    public List<Device> getDevices() {
        return devices;
    }

    /**
     * @param devices the devices to set
     */
    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }
    
}
