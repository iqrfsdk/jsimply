/*
 * Copyright 2016 MICRORISC s.r.o..
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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration;

/**
 * Exceptions of XML Configuration Parser.
 * 
 * @author Michal Konopa
 */
public final class XmlConfigurationParserException extends Exception {
    
    public XmlConfigurationParserException(String message) {
        super(message);
    }
    
    public XmlConfigurationParserException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public XmlConfigurationParserException(Throwable cause) {
        super(cause);
    }
    
}
