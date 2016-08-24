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
package com.microrisc.opengateway.web;

import com.microrisc.opengateway.dpa.DPA_Request;

/**
 * Web requests parser.
 * 
 * @author Michal Konopa
 */
public final class WebRequestParser {
    
    /**
     * Parses specified web request and returns its corresponding DPA request
     * representation.
     * @param webRequest web request to parse
     * @return DPA request corresponding to {@code webRequest}
     * @throws com.microrisc.opengateway.web.WebRequestParserException if some error
     *         occurs during parsing
     */
    public static DPA_Request parse(WebRequest webRequest) 
            throws WebRequestParserException 
    {
        throw new UnsupportedOperationException();
    }
    
}
