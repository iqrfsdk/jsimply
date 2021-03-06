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

import com.microrisc.simply.Node;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Parameters of Load Code Service.
 * 
 * @author Michal Konopa
 * @author Martin Strouhal
 */
public final class LoadCodeServiceParameters {
    
    // source file full name
    private String fileName;
    
    // start address on which will be saved data into memory
    private int startAddress;
    
    // loading action
    private LoadingCodeProperties.LoadingAction loadingAction;
    
    // loading content
    private LoadingCodeProperties.LoadingContent loadingContent;
    
    // target nodes
    private Collection<Node> targetNodes;
    
    // indicates, whether to print messages about service's progress
    private boolean printMessages = false;
    
    
    private String checkFileName(String fileName) {
        if ( fileName == null ) {
            throw new IllegalArgumentException("File name cannot be null.");
        }
        
        if ( fileName.isEmpty() ) {
            throw new IllegalArgumentException("File name cannot be empty.");
        }
        
        File f = new File(fileName);
        if(!f.exists()) {
           throw new IllegalArgumentException("File doesn't exist!");
        }
        
        return fileName;
    }
        
    private int checkStartAddress(int startAddress){
       if(startAddress < 0){
          throw new IllegalArgumentException("Start address must be between ");
       }
       return startAddress;
    }
    
    private LoadingCodeProperties.LoadingAction checkLoadingAction(
            LoadingCodeProperties.LoadingAction loadingAction
    ) {
        if ( loadingAction == null ) {
            throw new IllegalArgumentException("Loading action cannot be null.");
        }
        return loadingAction;
    }
    
    private LoadingCodeProperties.LoadingContent checkLoadingContent(
            LoadingCodeProperties.LoadingContent loadingContent
    ) {
        if ( loadingContent == null ) {
            throw new IllegalArgumentException("Type of content to load cannot be null.");
        }
        return loadingContent;
    }
    
    
    /**
     * Creates object of parameters for Load Code Service.
     * 
     * @param fileName full source file name including path  
     * @param startAddress start address on which will be saved data into memory
     * @param loadingAction type of action to do
     * @param loadingContent type of content to load
     * @throws IllegalArgumentException if: <br>
     *          - {@code fileName} is not valid file name or file does not exist <br>
     *          - {@code codeSize} is negative <br>
     *          - {@code startAddress} is negative or greater than ...<br>
     *          - {@code loadingAction} is {@code null} <br>
     *          - {@code loadingContent} is {@code null}
     */
    public LoadCodeServiceParameters( 
            String fileName, 
            int startAddress,
            LoadingCodeProperties.LoadingAction loadingAction,
            LoadingCodeProperties.LoadingContent loadingContent
    ) {
        this.fileName = checkFileName(fileName);
        this.startAddress = checkStartAddress(startAddress);
        this.loadingAction = checkLoadingAction(loadingAction);
        this.loadingContent = checkLoadingContent(loadingContent);
        this.targetNodes = new LinkedList<>();
    }
    
    /**
     * Creates object of parameters for Load Code Service.
     * 
     * @param fileName full source file name including path  
     * @param startAddress start address on which will be saved data into memory
     * @param loadingAction type of action to do
     * @param loadingContent type of content to load
     * @param targetNodes target nodes
     * @throws IllegalArgumentException if: <br>
     *          - {@code fileName} is not valid file name or file does not exist <br>
     *          - {@code codeSize} is negative <br>
     *          - {@code startAddress} is negative or greater than ...<br>
     *          - {@code loadingAction} is {@code null} <br>
     *          - {@code loadingContent} is {@code null}
     */
    public LoadCodeServiceParameters( 
            String fileName, 
            int startAddress,
            LoadingCodeProperties.LoadingAction loadingAction,
            LoadingCodeProperties.LoadingContent loadingContent,
            Collection<Node> targetNodes
    ) {
        this.fileName = checkFileName(fileName);
        this.startAddress = checkStartAddress(startAddress);
        this.loadingAction = checkLoadingAction(loadingAction);
        this.loadingContent = checkLoadingContent(loadingContent);
        this.targetNodes = new LinkedList<>(targetNodes);
    }

    /**
     * @return the source file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName source file name
     */
    public void setFileName(String fileName) {
        this.fileName = checkFileName(fileName);
    }

    /**
     * @return start address on which will be saved data into memory
     */
   public int getStartAddress() {
      return startAddress;
   }

   /**
    * @param startAddress start address on which will be saved data into memory
    */
   public void setStartAddress(int startAddress) {
      this.startAddress = startAddress;
   }

    /**
     * @return the loading action
     */
    public LoadingCodeProperties.LoadingAction getLoadingAction() {
        return loadingAction;
    }

    /**
     * @param loadingAction loading action to set
     */
    public void setLoadingAction(LoadingCodeProperties.LoadingAction loadingAction) {
        this.loadingAction = checkLoadingAction(loadingAction);
    }

    /**
     * @return the type of loading content
     */
    public LoadingCodeProperties.LoadingContent getLoadingContent() {
        return loadingContent;
    }

    /**
     * @param loadingContent type of loading content to set 
     */
    public void setLoadingContent(LoadingCodeProperties.LoadingContent loadingContent) {
        this.loadingContent = checkLoadingContent(loadingContent);
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
     * Indicates, whether to print messages about service's progress on standard
     * output or not.
     * 
     * @return {@code true} messages will be printed <br>
     *         {@code false}, otherwise
     */
    public boolean isPrintingMessagesEnabled() {
        return this.printMessages;
    }
    
    /**
     * Sets, whether to print information messages about service's progress on
     * standard output or not.
     * 
     * @param printMsg {@code true} to print messages <br>
     *                 {@code false}, otherwise
     */
    public void enablePrintingMessages(boolean printMsg) {
        this.printMessages = printMsg;
    }
    
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append("   file name: " + fileName + NEW_LINE);
        strBuilder.append("   start address: " + startAddress + NEW_LINE);
        strBuilder.append("   loading action: " + loadingAction + NEW_LINE);
        strBuilder.append("   loading content: " + loadingContent + NEW_LINE);
        strBuilder.append("   target nodes: " + targetNodes + NEW_LINE);
        strBuilder.append("   print messages: " + printMessages + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
}