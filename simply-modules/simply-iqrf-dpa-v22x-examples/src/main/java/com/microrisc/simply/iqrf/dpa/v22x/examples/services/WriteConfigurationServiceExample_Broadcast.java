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
import com.microrisc.simply.iqrf.dpa.v22x.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationProcessingInfo;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationService;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteConfigurationServiceParameters;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.WriteResult;
import com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.errors.WriteConfigurationError;
import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import com.microrisc.simply.services.ServiceResult;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Using broadcast in Write Configuration Service.
 * 
 * @author Michal Konopa
 */
public final class WriteConfigurationServiceExample_Broadcast {
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
        // probably supports needed peripheral for broadcast - FRC
        DPA_Node node0 = network1.getNode("0");
        if ( node0 == null ) {
            printMessageAndExit("Node 0 doesn't exist.");
        }
        
        // target nodes to write configuration into
        String[] nodeIds = new String[] { "1", "2", "3" };
        Collection<Node> targetNodes = getNodes(network1, nodeIds);
        
        // getting Write Configuration Service on node 1
        WriteConfigurationService writeConfigService = node0.getService(WriteConfigurationService.class);
        if ( writeConfigService == null ) {
            printMessageAndExit("Node 1 doesn't support Write Configuration Service.");
        }
        
        // setting service parameters
        WriteConfigurationServiceParameters serviceParams 
                = new WriteConfigurationServiceParameters(
                                "config" + File.separator + "dctr-configs" + File.separator + "dpa-2xx" + File.separator + "TR_config_2_00.xml",
                                "config" + File.separator + "dctr-configs" + File.separator + "node.xml",
                                targetNodes
                );
        serviceParams.setHwpId(DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK);
        
        // writing configuration - only for node, which this service resides on
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(serviceParams);
        
        // getting results
        if ( serviceResult.getStatus() == ServiceResult.Status.SUCCESSFULLY_COMPLETED ) {
            System.out.println("Configuration successfully written.");
        } else {
            System.out.println("Configuration write was NOT successful.");
            
            // find out details about errors
            // are there any principal error?
            WriteConfigurationProcessingInfo procInfo = serviceResult.getProcessingInfo();
            WriteConfigurationError error = procInfo.getError();
            if ( error != null ) {
                System.out.println("Error: " + error);
            } else {
                // if there is no principal error, find out, which config bytes 
                // at which nodes failed to write
                WriteResult writeResult = serviceResult.getResult();
                if ( writeResult != null ) {
                    printWritingFailedBytes(writeResult, targetNodes);
                }
            }
        }
        
        simply.destroy();
    }
    
    
    // returns collection of nodes from specified network corresponding to specified node IDs
    private static Collection<Node> getNodes(DPA_Network network, String[] nodeIds) {
        Collection<Node> nodes = new LinkedList<>();
        for ( String nodeId : nodeIds ) {
            // getting node
            DPA_Node node = network.getNode(nodeId);
            if ( node == null ) {
                printMessageAndExit("Node " + nodeId + " doesn't exist.");
            }
            nodes.add(node);
        }
        
        return nodes;
    }
    
    // prints bytes, which failed to write
    private static void printWritingFailedBytes(
            WriteResult writeResult, Collection<Node> targetNodes
    ) {
        for ( Node node : targetNodes ) {
            WriteResult.NodeWriteResult nodeWriteResult = writeResult.getNodeResult(node.getId());
            System.out.println("Node: " + node.getId());
            for ( HWP_ConfigurationByte configByte : nodeWriteResult.getWritingFailedBytes().values() ) {
                System.out.println(configByte);
            }
            System.out.println();
        }
    }
}
