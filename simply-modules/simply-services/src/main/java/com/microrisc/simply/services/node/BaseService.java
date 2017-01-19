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
import com.microrisc.simply.services.ServiceParameters;

/**
 * Base implementation for Services on Nodes.
 * 
 * @author Michal Konopa
 */
public class BaseService implements Service {
    
    // node in context
    protected final Node contextNode;
    
    
    /**
     * Creates new base service object with specified node in context.
     * 
     * @param contextNode node in context
     */
    public BaseService(Node contextNode) {
        this.contextNode = contextNode;
    }
    
    /**
     * Does nothing.
     * 
     * @param params 
     */
    @Override
    public void setServiceParameters(ServiceParameters params) { }
}
