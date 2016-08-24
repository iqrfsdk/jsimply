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
package com.microrisc.opengateway.dpa;

import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;

/**
 * Holds complete DPA result information. 
 * Operation result, DPA additional value and possible error
 * 
 * @author Michal Konopa
 */
public final class DPA_CompleteResult {
    
    // operational result
    private final short[] opResult;
    
    // potential error
    private final CallRequestProcessingError error;
    
    // additional information
    private final DPA_AdditionalInfo dpaAddInfo;
    
    
    /**
     * Creates object of complete result.
     * 
     * @param opResult result of DPA operation
     * @param error potential error
     * @param dpaAddInfo DPA additional info
     */
    public DPA_CompleteResult(
        short[] opResult, CallRequestProcessingError error, DPA_AdditionalInfo dpaAddInfo
    ) {
        this.opResult = opResult;
        this.error = error;
        this.dpaAddInfo = dpaAddInfo;
    }

    /**
     * @return result of DPA operation
     */
    public short[] getOpResult() {
        return opResult;
    }

    /**
     * @return the error
     */
    public CallRequestProcessingError getError() {
        return error;
    }

    /**
     * @return the DPA additional info
     */
    public DPA_AdditionalInfo getDpaAddInfo() {
        return dpaAddInfo;
    }
    
}
