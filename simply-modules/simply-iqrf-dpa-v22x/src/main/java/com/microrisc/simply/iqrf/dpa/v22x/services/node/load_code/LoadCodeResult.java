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

import java.util.HashMap;
import java.util.Map;

/**
 * Result of loading code on some specified collection of nodes.
 * 
 * @author Michal Konopa
 */
public final class LoadCodeResult {
    
    // map of results of loading code into each node
    // map is indexed by node IDs
    private final Map<String, Boolean> nodeResultsMap;
    
    
    /**
     * Creates and returns new object of load code result.
     * 
     * @param nodeResultsMap map of results of loading code into 
     *                       each node. Indexed by node IDs 
     */
    public LoadCodeResult(Map<String, Boolean> nodeResultsMap) {
        this.nodeResultsMap = new HashMap<>(nodeResultsMap);
    }
    
    /**
     * Returns writing result for node specified by its ID.
     * 
     * @param nodeId ID of node, whose writing result to return
     * @return writing result for specified node
     */
    public boolean getNodeResult(String nodeId) {
        return nodeResultsMap.get(nodeId);
    }
    
    /**
     * Returns writing result map for all nodes.
     * Map is indexed by node IDs.
     * 
     * @return writing result map for all nodes.
     */
    public Map<String, Boolean> getAllNodeResultsMap() {
        return new HashMap<>(nodeResultsMap);
    }
}
