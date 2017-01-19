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
package com.microrisc.simply.iqrf.dpa.v22x.examples.services;

import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.iqrf.dpa.DPA_Network;
import com.microrisc.simply.iqrf.dpa.DPA_Node;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.LoadCodeProcessingInfo;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.LoadCodeResult;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.LoadCodeService;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.LoadCodeServiceParameters;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code.errors.LoadCodeError;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import com.microrisc.simply.services.ServiceResult;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * Using broadcast in Load Code Service.
 * 
 * @author Michal Konopa
 */
public final class LoadCodeServiceExample_Broadcast {
    private static DPA_Simply simply = null;
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        if ( simply != null) {
            simply.destroy();
        }
        System.exit(1);
    }
    
    public static void main(String[] args) {
        // creating Simply instance
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "simply" + File.separator +  "Simply.properties");
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }

        // getting network 1
        DPA_Network network1 = simply.getNetwork("1", DPA_Network.class);
        if ( network1 == null ) {
            printMessageAndExit("Network 1 doesn't exist");
        }
        
        // getting node 0 - coordinator
        // probably supports FRC peripheral - needed for performing a broadcast
        DPA_Node node0 = network1.getNode("0");
        if ( node0 == null ) {
            printMessageAndExit("Node 0 doesn't exist.");
        }
        
        // target nodes to load code into
        String[] nodeIds = new String[] { "1", "2", "3" };
        Collection<Node> targetNodes = getNodes(network1, nodeIds);
        
        // getting Load Code Service on node 0
        LoadCodeService loadCodeService = node0.getService(LoadCodeService.class);
        if ( loadCodeService == null ) {
            printMessageAndExit("Node 0 doesn't support Load Code Service.");
        }
        
        // loading code
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> serviceResult 
            = loadCodeService.loadCode( 
                    new LoadCodeServiceParameters(
                        "config" + File.separator + "custom-dpa-handlers" + File.separator + "CustomDpaHandler-LED-Green-On-7xD-V228-160912.hex",
                        0x0800,
                        LoadingCodeProperties.LoadingAction.ComputeAndMatchChecksumWithCodeLoading,
                        LoadingCodeProperties.LoadingContent.Hex,
                        targetNodes
                    )
            );
        
        // getting results
        if ( serviceResult.getStatus() == ServiceResult.Status.SUCCESSFULLY_COMPLETED ) {
            System.out.println("Code successfully loaded.");
        } else {
            System.out.println("Code load was NOT successful.");
            
            // find out details about errors
            // is there a principal error?
            LoadCodeProcessingInfo procInfo = serviceResult.getProcessingInfo();
            LoadCodeError error = procInfo.getError();
            if ( error != null ) {
                System.out.println("Error: " + error);
            } else {
                // if there is no principal error, find out, which nodes failed
                // to load code into
                LoadCodeResult loadCodeResult = serviceResult.getResult();
                if ( loadCodeResult != null ) {
                    System.out.println("Loading code failed at nodes: ");
                    printFailedNodes(loadCodeResult);
                }
            }
        }
        
        simply.destroy();
    }
    
    
    // returns collection of nodes from specified network corresponding to specified node IDs
    private static Collection<Node> getNodes(DPA_Network network, String[] nodeIds) {
        Collection<Node> nodes = new LinkedList<>();
        for ( String nodeId : nodeIds ) {
            DPA_Node node = network.getNode(nodeId);
            if ( node == null ) {
                printMessageAndExit("Node " + nodeId + " doesn't exist.");
            }
            nodes.add(node);
        }
        
        return nodes;
    }
    
    // prints nodes, which failed to load code into
    private static void printFailedNodes(LoadCodeResult loadCodeResult) {
        for ( Map.Entry<String, Boolean> entry : loadCodeResult.getAllNodeResultsMap().entrySet() ) {
            if ( entry.getValue() == false ) {
                System.out.println(entry.getKey());
            }
        }
    }
}
