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
package com.microrisc.simply.services.node;

import com.microrisc.simply.Node;
import com.microrisc.simply.services.Service;

/**
 * Factory for creation of services in the context of nodes.
 * 
 * @author Michal Konopa
 */
public interface ServiceFactory {
    
    /**
     * Creates new object of node-context service.
     * 
     * @param node context node
     * @param args arguments for factory
     * @return service object
     * @throws java.lang.Exception if some error occured during creation of 
     *         a service object
     */
    Service create(Node node, Object[] args) throws Exception;
}