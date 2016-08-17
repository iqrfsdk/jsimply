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

import com.microrisc.opengateway.async.AsyncDataForMqtt.ModuleState;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;

/**
 * Creator of {@link AsyncDataForMqtt} objects.
 * 
 * @author Michal Konopa
 */
public final class AsyncDataForMqttCreator {
    
    // returns module state
    private static ModuleState getModuleState(short[] mainMessageData) {
        if ( mainMessageData[0] == 0 ) {
            return ModuleState.FREE;
        }
        
        if ( mainMessageData[0] == 1 ) {
            return ModuleState.OCCUPIED;
        }
        
        return ModuleState.UNKNOWN;
    }
    
    // return module ID
    private static String getModuleId(OsInfo osInfo) {
        if ( osInfo != null ) {
            return osInfo.getPrettyFormatedModuleId();
        }
        return "uknown";
    }
    
    
    /**
     * From specified DPA asynchronous message creates corresponding 
     * {@link AsyncDataForMqtt} object.
     * 
     * @param dpaAsyncMessage source DPA asynchronous message
     * @param osInfo OS info relating to source node
     * @return AsyncDataForMqtt corresponding to source DPA asynchronous message
     * @throws com.microrisc.opengateway.async.AsyncDataForMqttCreatorException
     *         if some error occurs during process of creation
     */
    public static AsyncDataForMqtt create(
            DPA_AsynchronousMessage dpaAsyncMessage, OsInfo osInfo
    )
        throws AsyncDataForMqttCreatorException 
    {
        if ( dpaAsyncMessage.getMessageSource() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message source not known.");
        }
        
        if ( dpaAsyncMessage.getMessageSource().getNodeId() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message source node not known.");
        }
        
        if ( dpaAsyncMessage.getMainData() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message main data not known.");
        }
        
        if ( dpaAsyncMessage.getAdditionalData()== null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message additional data not known.");
        }
        
        if ( !(dpaAsyncMessage.getMainData() instanceof short[]) ) {
            throw new AsyncDataForMqttCreatorException(
                "Asynchronous message bad type of the main data."
                + "Got: " + dpaAsyncMessage.getMainData().getClass()
                + ", expected: " + short[].class    
            );
        }
        
        short[] mainData = (short[])dpaAsyncMessage.getMainData();
        if ( mainData.length == 0 ) {
            throw new AsyncDataForMqttCreatorException(
                "No asynchronously received data from the node: "
                + dpaAsyncMessage.getMessageSource().getNodeId()
            );
        }
        
        if ( !(dpaAsyncMessage.getAdditionalData() instanceof DPA_AdditionalInfo) ) {
            throw new AsyncDataForMqttCreatorException(
                "Asynchronous message bad type of the additional data."
                + "Got: " + dpaAsyncMessage.getAdditionalData().getClass()
                + ", expected: " + DPA_AdditionalInfo.class    
            );
        }
        
        DPA_AdditionalInfo additionalData = (DPA_AdditionalInfo)dpaAsyncMessage.getAdditionalData();
        
        return new AsyncDataForMqtt(
                getModuleId(osInfo), 
                getModuleState(mainData), 
                dpaAsyncMessage.getMessageSource().getNodeId(), 
                dpaAsyncMessage.getMessageSource().getPeripheralNumber(),
                additionalData.getHwProfile(), 
                additionalData.getResponseCode(), 
                additionalData.getDPA_Value()
        );
    }
}
