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
package com.microrisc.opengateway.async;

import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;

/**
 * Data from DPA asynchronous message combined with some other data for use in MQTT.
 * 
 * @author Michal Konopa
 */
public final class AsyncDataForMqtt {
    
    // module state
    public static enum ModuleState {
        UNKNOWN,
        FREE,
        OCCUPIED;
    }
    
    // module state
    private final ModuleState moduleState;
    
    // ID of source module
    private final String moduleId;
    
    // ID of source node
    private final String nodeId;
    
    // source peripheral ID
    private final int pnum;
    
    // HW profile ID
    private final int hwpid;
    
    // response code
    private final DPA_ResponseCode responseCode;
    
    // DPA value
    private final int dpaValue;
    
    
    
    /**
     * Creates new DPA asynchronous message data for MQTT.
     * 
     * @param moduleId ID of source module
     * @param moduleState module state
     * @param nodeId source node ID
     * @param pnum source peripheral ID
     * @param hwpid HW profile ID
     * @param responseCode response code
     * @param dpaValue DPA value
     */
    public AsyncDataForMqtt(
            String moduleId, ModuleState moduleState, String nodeId, int pnum, 
            int hwpid, DPA_ResponseCode responseCode, int dpaValue 
    ) {
        this.moduleId = moduleId;
        this.moduleState = moduleState;
        this.nodeId = nodeId;
        this.pnum = pnum;
        this.hwpid = hwpid;
        this.responseCode = responseCode;
        this.dpaValue = dpaValue;
    }
    
    /**
     * @return the module ID
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * @return the module state
     */
    public ModuleState getModuleState() {
        return moduleState;
    }
    
    /**
     * @return the source node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return the source peripheral number
     */
    public int getPnum() {
        return pnum;
    }

    /**
     * @return the HW profile ID
     */
    public int getHwpid() {
        return hwpid;
    }

    /**
     * @return the response code
     */
    public DPA_ResponseCode getResponseCode() {
        return responseCode;
    }

    /**
     * @return the DPA value
     */
    public int getDpaValue() {
        return dpaValue;
    }

}
