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

/**
 * No configuration data error.
 * 
 * @author Michal Konopa
 */
public final class NoConfigurationDataError extends AbstractWriteConfigurationError {
    
    private final WriteConfigurationErrorType errorType 
            = WriteConfigurationErrorType.NO_CONFIGURATION_DATA;
    
    
    public NoConfigurationDataError() {}
    
    public NoConfigurationDataError(String message) {
        super(message);
    }
    
    public NoConfigurationDataError(Throwable cause) {
        super(cause);
    }
    
    @Override
    public WriteConfigurationErrorType getErrorType() {
        return errorType;
    }
}
