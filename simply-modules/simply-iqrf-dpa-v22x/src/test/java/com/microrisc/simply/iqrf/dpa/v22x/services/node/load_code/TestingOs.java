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
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_Request;
import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_Configuration;
import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadResult;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.SleepInfo;
import com.microrisc.simply.iqrf.types.VoidType;
import java.util.UUID;

/**
 * Implementation of OS for testing purposes.
 * 
 * @author Michal Konopa
 */
public final class TestingOs implements DeviceObject, OS {
    
    private static final String DEFAULT_NODE_ID = "1";
    private static final String DEFAULT_NETWORK_ID = "1";
    
    private final String nodeId;
    private final String networkId;
    
    // return values of methods called by tests
    private VoidType batchReturnValue;
    private LoadResult loadCodeReturnValue;
    
    
    public TestingOs() {
        nodeId = DEFAULT_NODE_ID;
        networkId = DEFAULT_NETWORK_ID;
    }
    
    public TestingOs(String nodeId, String networkId) {
        this.nodeId = nodeId;
        this.networkId = networkId;
    }
    
    // sets return value of batch method
    public void setBatchReturnValue(VoidType batchReturnValue) {
        this.batchReturnValue = batchReturnValue;
    }
    
    // sets return value of load code method
    public void setLoadCodeReturnValue(LoadResult loadCodeReturnValue) {
        this.loadCodeReturnValue = loadCodeReturnValue;
    }
    
    @Override
    public UUID async_read() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_reset() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public UUID async_readHWPConfiguration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_runRFPGM() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_sleep(SleepInfo sleepInfo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_batch(DPA_Request[] requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_setUSEC(int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_setMID(short[] key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_restart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_writeHWPConfiguration(HWP_Configuration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_writeHWPConfigurationByte(HWP_ConfigurationByte[] configByte) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UUID async_loadCode(LoadingCodeProperties properties) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OsInfo read() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HWP_Configuration readHWPConfiguration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType runRFPGM() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType sleep(SleepInfo sleepInfo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType batch(DPA_Request[] requests) {
        return batchReturnValue;
    }

    @Override
    public VoidType setUSEC(int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType setMID(short[] key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType restart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType writeHWPConfiguration(HWP_Configuration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoidType writeHWPConfigurationByte(HWP_ConfigurationByte[] configBytes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LoadResult loadCode(LoadingCodeProperties properties) {
        return loadCodeReturnValue;
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
