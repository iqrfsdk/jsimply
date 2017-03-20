
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

import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.LoadCodeError;

/**
 * Information about processing of code load.
 * 
 * @author Martin Strouhal
 * @author Michal Konopa
 */
public final class LoadCodeProcessingInfo {
    
    // error
    private final LoadCodeError error;
    
    
    /**
     * Creates new object of Load Code Processing Info with no error.
     */
    public LoadCodeProcessingInfo() {
        this.error = null;
    }
    
    /**
     * Creates new object of Load Code Processing Info with specified error.
     * 
     * @param error error object to store in newly created object
     */
    public LoadCodeProcessingInfo(LoadCodeError error) {
        this.error = error;
    }
    
    /**
     * Returns information abour error. 
     * 
     * @return information a error
     *         {@code null}, if no error has been encountered during processing
     */
    public LoadCodeError getError() {
        return error;
    }
    
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append("   error: " + error + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
}
