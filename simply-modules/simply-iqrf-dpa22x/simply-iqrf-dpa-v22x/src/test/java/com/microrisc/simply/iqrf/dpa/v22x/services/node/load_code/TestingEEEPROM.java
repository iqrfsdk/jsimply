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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.devices.EEEPROM;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.types.VoidType;
import java.util.UUID;

/**
 * Implementation of OS for testing purposes.
 * 
 * @author Michal Konopa
 */
public final class TestingEEEPROM implements DeviceObject, EEEPROM {
    
    private static final String DEFAULT_NODE_ID = "1";
    private static final String DEFAULT_NETWORK_ID = "1";
    
    private final String nodeId;
    private final String networkId;
    
    // return values of methods called by tests
    private VoidType extendedWriteReturnValue;
    
    
    public TestingEEEPROM() {
        nodeId = DEFAULT_NODE_ID;
        networkId = DEFAULT_NETWORK_ID;
    }
    
    public TestingEEEPROM(String nodeId, String networkId) {
        this.nodeId = nodeId;
        this.networkId = networkId;
    }
    
    // sets return value of batch method
    public void setExtendedWriteReturnValue(VoidType extendedWriteReturnValue) {
        this.extendedWriteReturnValue = extendedWriteReturnValue;
    }
    
    
    @Override
    public String getNetworkId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNodeId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class getImplementedDeviceInterface() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID async_read(int blockNumber, int length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID async_write(int blockNumber, short[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID async_extendedRead(int address, int length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID async_extendedWrite(int address, short[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short[] read(int blockNumber, int length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VoidType write(int blockNumber, short[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short[] extendedRead(int address, int length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VoidType extendedWrite(int address, short[] data) {
        return extendedWriteReturnValue;
    }

    @Override
    public <T> T getCallResult(UUID callId, Class<T> resultClass, long timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T getCallResultInDefaultWaitingTimeout(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T getCallResultImmediately(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T getCallResultInUnlimitedWaitingTimeout(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getDefaultWaitingTimeout() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDefaultWaitingTimeout(long timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID getIdOfLastExexutedCallRequest() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingState(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancelCallRequest(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingStateOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancelCallRequestOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingError(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingErrorOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getCallResultAdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getCallResultAdditionalInfoOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfoOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setRequestHwProfile(int hwProfile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getRequestHwProfile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID call(Object methodId, Object[] args) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String transform(Object methodId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
