/*
 * Copyright 2017 MICRORISC s.r.o.
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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.load_code.errors;

/**
 * Errors related to starting phase before writing data into external EEPROM and 
 * loading code. 
 * 
 * @author Michal Konopa
 */
public final class PreprocessingError extends AbstractLoadCodeError {
    
    private final LoadCodeErrorType errorType = LoadCodeErrorType.PREPROCESS;
    
    
    public PreprocessingError(String message) {}
    
    public PreprocessingError(String message, Throwable cause) {}
    
    public PreprocessingError(Throwable cause) {}
    
    @Override
    public LoadCodeErrorType getErrorType() {
        return errorType;
    }
    
}
