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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration;

import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of configuration bytes writing.
 * 
 * @author Michal Konopa
 */
public final class WriteResult {
    
    /**
     * Provides access to results of writing configuration bytes into specified 
     * node. 
     */
    public static interface NodeWriteResult {
        
        /**
         * Returns all bytes which were intended to write.
         * 
         * @return all bytes to write
         */
        Map<Integer, HWP_ConfigurationByte> getBytesToWrite();
        
        /**
         * Returns map of configuration bytes, which failed to write into node.
         * Map is indexed by configuration byte addresses.
         * 
         * @return map of configuration bytes, which failed to write into node
         */
        Map<Integer, HWP_ConfigurationByte> getWritingFailedBytes();
    }
    
    /**
     * Writing results on concrete node.
     */
    static final class NodeWriteResultImpl implements NodeWriteResult {
        
        // all config bytes which were intended to write
        private final Map<Integer, HWP_ConfigurationByte> bytesToWrite;
        
        // config bytes which failed to write
        private final Map<Integer, HWP_ConfigurationByte> writingFailedBytes;
        
        /**
         * Creates new instance of node write result.
         * 
         * @param bytesToWrite all bytes, which were intended to write
         * @param failedWritingBytes bytes, which failed to write
         *        A subset of {@code bytesToWrite}
         */
        public NodeWriteResultImpl(
                Map<Integer, HWP_ConfigurationByte> bytesToWrite,
                Map<Integer, HWP_ConfigurationByte> failedWritingBytes
        ) {
            this.bytesToWrite = new HashMap<>(bytesToWrite);
            this.writingFailedBytes = new HashMap<>(failedWritingBytes);
        }
        
        @Override
        public Map<Integer, HWP_ConfigurationByte> getBytesToWrite() {
            return bytesToWrite;
        }
        
        @Override
        public Map<Integer, HWP_ConfigurationByte> getWritingFailedBytes() {
            return writingFailedBytes;
        }
        
        @Override
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            String NEW_LINE = System.getProperty("line.separator");

            strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
            strBuilder.append("   bytes to write: " + bytesToWrite + NEW_LINE);
            strBuilder.append("   writing failed bytes: " + writingFailedBytes + NEW_LINE);
            strBuilder.append("}");

            return strBuilder.toString();
        }
        
    }
    
    // map of results of writing configuration bytes into each node
    // map is indexed by node IDs
    private final Map<String, NodeWriteResult> nodeResultsMap;
    
    
    
    /**
     * Creates and returns new object of Writing Result.
     * 
     * @param nodeResultsMap map of results of writing configuration bytes into 
     *                       each node. Indexed by node IDs 
     */
    public WriteResult(Map<String, NodeWriteResult> nodeResultsMap) {
        this.nodeResultsMap = new HashMap<>(nodeResultsMap);
    }
    
    /**
     * Returns writing result for node specified by its ID.
     * 
     * @param nodeId ID of node, whose writing result to return
     * @return writing result for specified node
     */
    public NodeWriteResult getNodeResult(String nodeId) {
        return nodeResultsMap.get(nodeId);
    }
    
    /**
     * Returns writing result map for all nodes.
     * Map is indexed by node IDs.
     * 
     * @return writing result map for all nodes.
     */
    public Map<String, NodeWriteResult> getAllNodeResultsMap() {
        return new HashMap<>(nodeResultsMap);
    }
   
}
