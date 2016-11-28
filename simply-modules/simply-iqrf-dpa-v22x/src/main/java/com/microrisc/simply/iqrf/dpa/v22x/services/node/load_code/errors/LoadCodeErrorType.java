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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors;

/**
 * Types of errors, which encounter during run of Write Configuration Service.
 * 
 * @author Michal Konopa
 */
public enum LoadCodeErrorType {
    
    /** Errors related to (processing of) a loading content. */
    LOADING_CONTENT,
    
    /** Missing peripheral. */
    MISSING_PERIPHERAL,
    
    /** Processing of request failed. */
    REQUEST_PROCESSING
}
