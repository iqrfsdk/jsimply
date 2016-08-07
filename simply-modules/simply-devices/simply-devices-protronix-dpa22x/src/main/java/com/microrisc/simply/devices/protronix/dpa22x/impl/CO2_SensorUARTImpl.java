
package com.microrisc.simply.devices.protronix.dpa22x.impl;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.devices.protronix.dpa22x.CO2_Sensor;
import com.microrisc.simply.devices.protronix.dpa22x.errors.BadResponseDataError;
import com.microrisc.simply.devices.protronix.dpa22x.utils.Modbus_CRCSetter;
import com.microrisc.simply.iqrf.dpa.v22x.protronix.types.CO2_SensorData;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import java.util.UUID;

/**
 * Implementation of {@link com.microrisc.simply.iqrf.dpa.v22x.protronix.devices.CO2_Sensor}
 * using UART peripheral.
 * 
 * @author Michal Konopa
 */
public class CO2_SensorUARTImpl 
extends CompoundDeviceObject implements CO2_Sensor {
    
    // used UART
    private final UART uart;
    
    // UART read timeout
    private static final short READ_TIMEOUT = 0xFE;
    
    // used data to send to UART
    private static final short[] DATA 
        = { 0x01, 0x42, 0x00, 0x03, 0x75, 0x31, 0x75, 0x33, 0x75, 0x32, 0x00, 0x00 }; 
    
    // reponse length
    private static final int MODBUS_RESPONSE_LENGTH = 18;
    
    
    // parses data from UART to CO2 Sensor Data 
    private static class UART_DataParser {
        
        private static final int CO2_HIGH_BYTE_POS = 6;
        private static final int CO2_LOW_BYTE_POS = 7;
        
        private static final int TEMPERATURE_HIGH_BYTE_POS = 10;
        private static final int TEMPERATURE_LOW_BYTE_POS = 11;
        
        private static final int HUMIDITY_HIGH_BYTE_POS = 14;
        private static final int HUMIDITY_LOW_BYTE_POS = 15;
        
        
        public static CO2_SensorData parse(short[] uartData) {
            int co2 = (uartData[CO2_HIGH_BYTE_POS] << 8) + uartData[CO2_LOW_BYTE_POS];
            float temperature = ((uartData[TEMPERATURE_HIGH_BYTE_POS] << 8) + uartData[TEMPERATURE_LOW_BYTE_POS])
                    / (float) 10;
            float humidity = ((uartData[HUMIDITY_HIGH_BYTE_POS] << 8) + uartData[HUMIDITY_LOW_BYTE_POS])
                    / (float) 10;
            
            return new CO2_SensorData(co2, temperature, humidity);
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
    
    // last response data error
    private BadResponseDataError lastResponseDataError = null; 
    
    
    /**
     * Creates a new object representing the CO2 sensor.
     * @param networkId identifier of network, which this sensor belongs to.
     * @param nodeId identifier of node, which this sensor belongs to.
     * @param uartDeviceObject UART, which will be used for communication
     * @throws IllegalArgumentException if {@code uartDeviceObject} doesn't implement
     *         the {@link UART} Device Interface
     */
    public CO2_SensorUARTImpl(String networkId, String nodeId, DeviceObject uartDeviceObject) {
        super(networkId, nodeId, uartDeviceObject);
        this.uart = checkUartDeviceObject(uartDeviceObject);
    }

    @Override
    public CO2_SensorData get() {
        lastResponseDataError = null;
        
        short[] readData = uart.writeAndRead(READ_TIMEOUT, Modbus_CRCSetter.set(DATA));
        if ( readData == null ) {
            return null;
        }
        
        if ( readData.length == 0 ) {
            lastResponseDataError = new BadResponseDataError("No received data from UART.");
            return null;
        }
                
        if ( readData.length != MODBUS_RESPONSE_LENGTH ) {
            lastResponseDataError = new BadResponseDataError(
                    "Bad lenght of data received from UART. Get data length: " + readData.length 
                    + "Expected: " + MODBUS_RESPONSE_LENGTH
            );
            return null;
        }
        
        return UART_DataParser.parse(readData);
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
    
    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfoOfLastCall() {
        return uart.getDPA_AdditionalInfoOfLastCall();
    }
    
}
