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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.errors;

import com.microrisc.simply.errors.CallRequestProcessingError;

/**
 * Request processing error.
 * Carries info about associated Call Request Processing Error.
 * 
 * @author Michal Konopa
 */
public final class RequestProcessingError extends AbstractWriteConfigurationError {
    
    private final WriteConfigurationErrorType errorType 
            = WriteConfigurationErrorType.REQUEST_PROCESSING;
    
    private final CallRequestProcessingError callRequestProcessingError;
    
    
    public RequestProcessingError(String message) {
        this.callRequestProcessingError = null;
    }
    
    public RequestProcessingError(CallRequestProcessingError callRequestProcessingError) {
        this.callRequestProcessingError = callRequestProcessingError;
    }
    
    @Override
    public WriteConfigurationErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Returns info about associated call request processing error.
     * 
     * @return associated call request processing error
     */
    public CallRequestProcessingError getCallRequestProcessingError() {
        return callRequestProcessingError;
    }
}
