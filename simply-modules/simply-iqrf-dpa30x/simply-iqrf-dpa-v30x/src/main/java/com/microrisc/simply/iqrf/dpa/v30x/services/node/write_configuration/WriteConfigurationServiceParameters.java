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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration;

import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.v30x.protocol.DPA_ProtocolProperties;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Parameters of Write Configuration Service.
 * 
 * @author Michal Konopa
 */
public final class WriteConfigurationServiceParameters {
    
    // definition source file full name
    private String defFileName;
    
    // user settings file name including path
    private String userSettingsFileName;
    
    // target nodes
    private Collection<Node> targetNodes;
    
    // HW profile ID
    private int hwpId = DPA_ProtocolProperties.HWPID_Properties.DEFAULT;
    
    
    private static String checkFileName(String fileName) {
        if ( fileName == null ) {
            throw new IllegalArgumentException("File name cannot be null.");
        }
        
        if ( fileName.isEmpty() ) {
            throw new IllegalArgumentException("File name cannot be empty.");
        }
        
        return fileName;
    }
    
    
    /**
     * Creates object of parameters for Write Configuration Service.
     * 
     * @param defFileName config definition source file name including path
     * @param userSettingsFileName user settings file name including path
     * @throws IllegalArgumentException if
     *         {@code defFileName} is not valid file name or the file does not exist
     *         {@code userSettingsFileName} is not valid file name or the file does not exist
     */
    public WriteConfigurationServiceParameters(String defFileName, String userSettingsFileName) {
        this.defFileName = checkFileName(defFileName);
        this.userSettingsFileName = checkFileName(userSettingsFileName);
        this.targetNodes = new LinkedList<>();
    }
    
    /**
     * Creates object of parameters for Write Configuration Service.
     * 
     * @param defFileName config definition source file name including path
     * @param userSettingsFileName user settings file name including path
     * @param targetNodes target nodes
     * @throws IllegalArgumentException if
     *         {@code defFileName} is not valid file name or the file does not exist
     *         {@code userSettingsFileName} is not valid file name or the file does not exist
     */
    public WriteConfigurationServiceParameters(
            String defFileName, String userSettingsFileName, Collection<Node> targetNodes
    ) {
        this.defFileName = checkFileName(defFileName);
        this.userSettingsFileName = checkFileName(userSettingsFileName);
        this.targetNodes = new LinkedList<>(targetNodes);
    }
    
    /**
     * @return the source file name
     */
    public String getDefFileName() {
        return defFileName;
    }

    /**
     * @param defFileName source file name
     * @throws IllegalArgumentException if
     *         {@code defFileName} is not valid file name or the file does not exist
     */
    public void setDefFileName(String defFileName) {
        this.defFileName = checkFileName(defFileName);
    }

    /**
     * @return the user settings file name
     */
    public String getUserSettingsFileName() {
        return userSettingsFileName;
    }

    /**
     * @param userSettingsFileName the user settings file name to set
     * @throws IllegalArgumentException if
     *         {@code userSettingsFileName} is not valid file name or the file does not exist
     */
    public void setUserSettingsFileName(String userSettingsFileName) {
        this.userSettingsFileName = checkFileName(userSettingsFileName);
    }

    /**
     * @return target nodes
     */
    public Collection<Node> getTargetNodes() {
        return targetNodes;
    }

    /**
     * @param targetNodes target nodes
     */
    public void setTargetNodes(Collection<Node> targetNodes) {
        this.targetNodes = targetNodes;
    }

    /**
     * @return the HW profile ID
     */
    public int getHwpId() {
        return hwpId;
    }

    /**
     * @param hwpId HW Profile ID to set
     */
    public void setHwpId(int hwpId) {
        this.hwpId = hwpId;
    }
    
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append("   definition file: " + defFileName + NEW_LINE);
        strBuilder.append("   user settings file: " + userSettingsFileName + NEW_LINE);
        strBuilder.append("   target nodes: " + targetNodes + NEW_LINE);
        strBuilder.append("   hwp id: " + hwpId + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
    
}
