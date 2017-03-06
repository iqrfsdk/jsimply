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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.load_code;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v30x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v30x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_Command;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_Configuration;
import com.microrisc.simply.iqrf.dpa.v30x.types.FRC_Data;
import java.util.UUID;

/**
 * Implementation of FRC for testing purposes.
 * 
 * @author Michal Konopa
 */
public final class TestingFrc implements DeviceObject, FRC {
    private static final String DEFAULT_NODE_ID = "1";
    private static final String DEFAULT_NETWORK_ID = "1";
    
    private final String nodeId;
    private final String networkId;
    
    // returns values for tested methods
    private FRC_Data sendSelectiveReturnValue = null;
    private short[] extraResultReturnValue = null;
    
    
    public TestingFrc() {
        nodeId = DEFAULT_NODE_ID;
        networkId = DEFAULT_NETWORK_ID;
    }
    
    public TestingFrc(String nodeId, String networkId) {
        this.nodeId = nodeId;
        this.networkId = networkId;
    }
    
    
    
    // sets return value of sendSelective method
    public void setSendSelectiveReturnValue(FRC_Data sendSelectiveReturnValue) {
        this.sendSelectiveReturnValue = sendSelectiveReturnValue;
    }
    
    // sets return value of extraResult method
    public void setExtraResultReturnValue(short[] extraResultReturnValue) {
        this.extraResultReturnValue = extraResultReturnValue;
    }
    
    
    @Override
    public UUID async_send(FRC_Command frcCmd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_extraResult() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public UUID async_sendSelective(FRC_Command frcCmd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_setFRCParams(FRC_Configuration config) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FRC_Data send(FRC_Command frcCmd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short[] extraResult() {
        return extraResultReturnValue;
    }

    @Override
    public FRC_Data sendSelective(FRC_Command frcCmd) {
        return sendSelectiveReturnValue;
    }

    @Override
    public FRC_Configuration setFRCParams(FRC_Configuration config) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID call(Object methodId, Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String transform(Object methodId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNetworkId() {
        return networkId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Class getImplementedDeviceInterface() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getCallResult(UUID callId, Class<T> resultClass, long timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getCallResultInDefaultWaitingTimeout(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getCallResultImmediately(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getCallResultInUnlimitedWaitingTimeout(UUID callId, Class<T> resultClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getDefaultWaitingTimeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultWaitingTimeout(long timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID getIdOfLastExexutedCallRequest() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingState(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void cancelCallRequest(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallRequestProcessingState getCallRequestProcessingStateOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void cancelCallRequestOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingError(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CallRequestProcessingError getCallRequestProcessingErrorOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getCallResultAdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getCallResultAdditionalInfoOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfo(UUID callId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfoOfLastCall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setRequestHwProfile(int hwProfile) {
    }

    @Override
    public int getRequestHwProfile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
