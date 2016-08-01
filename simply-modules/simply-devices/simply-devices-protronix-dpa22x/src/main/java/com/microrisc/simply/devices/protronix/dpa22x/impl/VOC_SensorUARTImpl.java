
package com.microrisc.simply.devices.protronix.dpa22x.impl;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.protronix.devices.VOC_Sensor;
import com.microrisc.simply.iqrf.dpa.v22x.protronix.types.VOC_SensorData;
import java.util.UUID;

/**
 * Implementation of {@link com.microrisc.simply.iqrf.dpa.v22x.protronix.devices.VOC_Sensor}
 * using UART peripheral.
 * 
 * @author Michal Konopa
 */
public class VOC_SensorUARTImpl 
extends CompoundDeviceObject implements VOC_Sensor {
    
    // used UART
    private final UART uart;
    
    // UART read timeout
    private static final short READ_TIMEOUT = 0xFE;
    
    // used data to send to UART
    private static final short[] DATA 
        = { 0x01, 0x42, 0x00, 0x03, 0x75, 0x31, 0x75, 0x33, 0x75, 0x32, 0x00, 0x00 }; 
    
    
    
    // parses data from UART to VOC Sensor Data 
    private static class UART_DataParser {
        
        private static final int VOC_HIGH_BYTE_POS = 6;
        private static final int VOC_LOW_BYTE_POS = 7;
        
        private static final int TEMPERATURE_HIGH_BYTE_POS = 10;
        private static final int TEMPERATURE_LOW_BYTE_POS = 11;
        
        private static final int HUMIDITY_HIGH_BYTE_POS = 14;
        private static final int HUMIDITY_LOW_BYTE_POS = 15;
        
        
        public static VOC_SensorData parse(short[] uartData) {
            int voc = (uartData[VOC_HIGH_BYTE_POS] << 8) + uartData[VOC_LOW_BYTE_POS];
            float temperature = ((uartData[TEMPERATURE_HIGH_BYTE_POS] << 8) + uartData[TEMPERATURE_LOW_BYTE_POS])
                    / (float) 10;
            float humidity = ((uartData[HUMIDITY_HIGH_BYTE_POS] << 8) + uartData[HUMIDITY_LOW_BYTE_POS])
                    / (float) 10;
            
            return new VOC_SensorData(voc, temperature, humidity);
        }
    }
            
    
    private static UART checkUartDeviceObject(DeviceObject uartDeviceObject) {
        if ( !(uartDeviceObject.getImplementedDeviceInterface() == UART.class) ) {
            throw new IllegalArgumentException(
                "Device Object doesn't implement UART Device Interface. "
                + "Implemented Device Interface: " + uartDeviceObject.getImplementedDeviceInterface());
        }
        return (UART)uartDeviceObject;
    }
    
    
    /**
     * Creates a new object representing the CO2 sensor.
     * @param networkId identifier of network, which this sensor belongs to.
     * @param nodeId identifier of node, which this sensor belongs to.
     * @param uartDeviceObject UART, which will be used for communication
     * @throws IllegalArgumentException if {@code uartDeviceObject} doesn't implement
     *         the {@link UART} Device Interface
     */
    public VOC_SensorUARTImpl(String networkId, String nodeId, DeviceObject uartDeviceObject) {
        super(networkId, nodeId, uartDeviceObject);
        this.uart = checkUartDeviceObject(uartDeviceObject);
    }

    @Override
    public UUID async_get() {
        return uart.async_writeAndRead(READ_TIMEOUT, DATA);
    }

    @Override
    public VOC_SensorData get() {
        short[] readData = uart.writeAndRead(READ_TIMEOUT, DATA);
        if ( readData == null ) {
            return null;
        }
        
        return UART_DataParser.parse(readData);
    }

    @Override
    public <T> T getCallResult(UUID callId, Class<T> resultClass, long timeout) {
        return uart.getCallResult(callId, resultClass, timeout);
    }

    @Override
    public <T> T getCallResultInDefaultWaitingTimeout(UUID callId, Class<T> resultClass) {
        return uart.getCallResultInDefaultWaitingTimeout(callId, resultClass);
    }

    @Override
    public <T> T getCallResultImmediately(UUID callId, Class<T> resultClass) {
        return uart.getCallResultImmediately(callId, resultClass);
    }

    @Override
    public <T> T getCallResultInUnlimitedWaitingTimeout(UUID callId, Class<T> resultClass) {
        return uart.getCallResultInUnlimitedWaitingTimeout(callId, resultClass);
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
    public UUID getIdOfLastExexutedCallRequest() {
        return uart.getIdOfLastExexutedCallRequest();
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingState(UUID callId) {
        return uart.getCallRequestProcessingState(callId);
    }

    @Override
    public void cancelCallRequest(UUID callId) {
        uart.cancelCallRequest(callId);
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingStateOfLastCall() {
        return uart.getCallRequestProcessingStateOfLastCall();
    }

    @Override
    public void cancelCallRequestOfLastCall() {
        uart.cancelCallRequestOfLastCall();
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingError(UUID callId) {
        return uart.getCallRequestProcessingError(callId);
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingErrorOfLastCall() {
        return uart.getCallRequestProcessingErrorOfLastCall();
    }

    @Override
    public Object getCallResultAdditionalInfo(UUID callId) {
        return uart.getCallResultAdditionalInfo(callId);
    }

    @Override
    public Object getCallResultAdditionalInfoOfLastCall() {
        return uart.getCallResultAdditionalInfoOfLastCall();
    }

    @Override
    public UUID call(Object methodId, Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String transform(Object methodId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
