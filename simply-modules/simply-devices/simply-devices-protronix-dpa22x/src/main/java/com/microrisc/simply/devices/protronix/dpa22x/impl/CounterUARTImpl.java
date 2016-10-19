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
package com.microrisc.simply.devices.protronix.dpa22x.impl;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.devices.protronix.dpa22x.Counter;
import com.microrisc.simply.devices.protronix.dpa22x.errors.BadResponseDataError;
import com.microrisc.simply.devices.protronix.dpa22x.utils.ModbusCRCSetter;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import java.util.UUID;

/**
 * Implementation of {@link com.microrisc.simply.iqrf.dpa.v22x.protronix.devices.Counter}
 * using UART peripheral.
 * 
 * @author Michal Konopa
 */
public final class CounterUARTImpl 
extends CompoundDeviceObject implements Counter {
    
    // UART to use for communication
    private final UART uart;
    
    // UART read timeout
    private static final short READ_TIMEOUT = 0xFE;
    
    // Protronix HWPID
    private static final int HWPID = 0xFFFF;
    
    // used data to send to UART
    private static final short[] REQUEST = { 0x01, 0x04, 0x75, 0x37, 0x00, 0x01, 0x00, 0x00 }; 
    
    // reponse length
    private static final int RESPONSE_LENGTH = 2;
    
    
    // last response data error
    private BadResponseDataError lastResponseDataError = null; 
    
    private static UART checkUartDeviceObject(DeviceObject uartDeviceObject) {
        if ( !(uartDeviceObject.getImplementedDeviceInterface() == UART.class) ) {
            throw new IllegalArgumentException(
                "Device Object doesn't implement UART Device Interface. "
                + "Implemented Device Interface: " + uartDeviceObject.getImplementedDeviceInterface());
        }
        return (UART)uartDeviceObject;
    }
    
    
    /**
     * Creates a new object representing the Counter sensor.
     * 
     * @param networkId identifier of network, which this sensor belongs to.
     * @param nodeId identifier of node, which this sensor belongs to.
     * @param uartDeviceObject UART, which will be used for communication
     * @throws IllegalArgumentException if {@code uartDeviceObject} doesn't implement
     *         the {@link UART} Device Interface
     */
    public CounterUARTImpl(String networkId, String nodeId, DeviceObject uartDeviceObject) {
        super(networkId, nodeId, uartDeviceObject);
        this.uart = checkUartDeviceObject(uartDeviceObject);
        this.uart.setRequestHwProfile(HWPID);
    }

    @Override
    public Integer count() {
        lastResponseDataError = null;
        
        short[] readData = uart.writeAndRead(READ_TIMEOUT, ModbusCRCSetter.set(REQUEST));
        if ( readData == null ) {
            return null;
        }
        
        if ( readData.length == 0 ) {
            lastResponseDataError = new BadResponseDataError("No received data from UART.");
            return null;
        }
                
        if ( readData.length != RESPONSE_LENGTH ) {
            lastResponseDataError = new BadResponseDataError(
                    "Bad lenght of data received from UART. Get data length: " + readData.length 
                    + "Expected: " + RESPONSE_LENGTH
            );
            return null;
        }
        
        return readData[0] + (readData[1] << 8);
    }

    @Override
    public long getDefaultWaitingTimeout() {
        return uart.getDefaultWaitingTimeout();
    }

    @Override
    public void setDefaultWaitingTimeout(long timeout) {
        uart.setDefaultWaitingTimeout(timeout);
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingStateOfLastCall() {
        if ( lastResponseDataError != null ) {
            return CallRequestProcessingState.ERROR;
        }
        return uart.getCallRequestProcessingStateOfLastCall();
    }

    @Override
    public void cancelCallRequestOfLastCall() {
        uart.cancelCallRequestOfLastCall();
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingErrorOfLastCall() {
        if ( lastResponseDataError != null ) {
            return lastResponseDataError;
        }
        return uart.getCallRequestProcessingErrorOfLastCall();
    }
    
    /**
     * Not supported.
     * 
     * @param callId
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfoOfLastCall() {
        return uart.getDPA_AdditionalInfoOfLastCall();
    } 
    
}
