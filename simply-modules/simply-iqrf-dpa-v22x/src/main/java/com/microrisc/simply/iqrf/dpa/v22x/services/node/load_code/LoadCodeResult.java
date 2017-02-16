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
    
    // errors related to specified nodes
    // indexed by node IDs
    private final Map<String, LoadCodeError> nodeErrorsMap;
    
    
    private static void checkNodeResultsMap(Map<String, Boolean> nodeResultsMap) {
        if ( nodeResultsMap == null ) {
            throw new IllegalArgumentException("Node results map cannot be null.");
        }
    }
    
    // checks consistency of node results map and node errors map
    private static void checkConsistency(
            Map<String, Boolean> nodeResultsMap,
            Map<String, LoadCodeError> nodeErrorsMap
    ) {
        if ( nodeResultsMap == null ) {
            throw new IllegalArgumentException("Node results map cannot be null.");
        }
        
        if ( nodeErrorsMap == null ) {
            throw new IllegalArgumentException("Node errors map cannot be null.");
        }
        
        for ( Map.Entry<String, Boolean> resultEntry : nodeResultsMap.entrySet() ) {
            LoadCodeError error = nodeErrorsMap.get(resultEntry.getKey());
            
            Boolean result = resultEntry.getValue();
            if ( result == null ) {
                throw new IllegalArgumentException(
                            "Missing result for node: " + resultEntry.getKey()
                );
            }
            
            if ( result == true ) {
                if ( error != null ) {
                    throw new IllegalArgumentException(
                            "Unexpected error for node: " + resultEntry.getKey()
                    );
                }
            } else {
                if ( error == null ) {
                    throw new IllegalArgumentException(
                            "Missing error for node: " + resultEntry.getKey()
                    );
                }
            }
        }
    }
    
    // creates node errors map according to specified results map
    private static Map<String, LoadCodeError> createErrorsMap(
            Map<String, Boolean> nodeResultsMap
    ) {
        Map<String, LoadCodeError> errorsMap = new HashMap<>();
        
        for ( Map.Entry<String, Boolean> resultEntry : nodeResultsMap.entrySet() ) {
            if ( resultEntry.getValue() == false ) {
                throw new IllegalArgumentException(
                        "False values not permitted in creation of default errors map."
                        + " Node ID: " + resultEntry.getKey()
                );
            }
            
            errorsMap.put(resultEntry.getKey(), null);
        }
        
        return errorsMap;
    }
    
    
    
    /**
     * Creates and returns new object of load code result.
     * 
     * @param nodeResultsMap Map of results of loading code into 
     *                       each node. Indexed by node IDs. 
     * @param nodeErrorsMap Errors related to nodes. Indexed by node IDs.
     *                      If load code was allright, value at that node's ID is
     *                      {@code null}.
     * @throws IllegalArgumentException if: <br>
     *         - {@code nodeResultsMap} is {@code null} <br>
     *         - {@code nodeErrorsMap} is {@code null} <br>
     *         - {@code nodeResultsMap} contains some {@code false} values
     */
    public LoadCodeResult(
            Map<String, Boolean> nodeResultsMap,
            Map<String, LoadCodeError> nodeErrorsMap
    ) {
        checkConsistency(nodeResultsMap, nodeErrorsMap);
        this.nodeResultsMap = new HashMap<>(nodeResultsMap);
        this.nodeErrorsMap = new HashMap<>(nodeErrorsMap);
    }
    
    /**
     * Creates and returns new object of load code result.
     * 
     * Errors map will be created based on data in node results map. If node results
     * map contain some nodes with {@code false} values, exception is thrown because
     * the error is unobtainable.
     * 
     * @param nodeResultsMap Map of results of loading code into 
     *                       each node. Indexed by node IDs. 
     * @throws IllegalArgumentException if: <br>
     *         - {@code nodeResultsMap} is {@code null} <br>
     *         - {@code nodeResultsMap} contains some {@code false} values
     */
    public LoadCodeResult(Map<String, Boolean> nodeResultsMap) {
        checkNodeResultsMap(nodeResultsMap);
        this.nodeResultsMap = new HashMap<>(nodeResultsMap);
        this.nodeErrorsMap = createErrorsMap(nodeResultsMap);
    }
    
    /**
     * Returns loading result map for all nodes.
     * Map is indexed by node IDs.
     * 
     * @return loading result map for all nodes.
     */
    public Map<String, Boolean> getResultsMap() {
        return new HashMap<>(nodeResultsMap);
    }
    
    /**
     * Returns error map for failed nodes.
     * Map is indexed by node IDs.
     * 
     * @return errors map for failed nodes.
     */
    public Map<String, LoadCodeError> getErrorsMap() {
        return new HashMap<>(nodeErrorsMap);
    }
    
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append("   results: " +  nodeResultsMap + NEW_LINE);
        strBuilder.append("   errors: " +  nodeErrorsMap + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
}
