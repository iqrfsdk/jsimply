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
package com.microrisc.simply.iqrf.dpa.v30x.examples.services;

import com.microrisc.simply.SimplyException;
import com.microrisc.simply.iqrf.dpa.DPA_Network;
import com.microrisc.simply.iqrf.dpa.DPA_Node;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v30x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v30x.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteConfigurationProcessingInfo;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteConfigurationService;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteConfigurationServiceParameters;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.WriteResult;
import com.microrisc.simply.iqrf.dpa.v30x.types.HWP_ConfigurationByte;
import com.microrisc.simply.services.ServiceResult;
import java.io.File;

/**
 * Usage of Write Configuration Service.
 * 
 * @author Michal Konopa
 */
public final class WriteConfigurationServiceExample {
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

        // getting node 1
        DPA_Node node1 = network1.getNode("1");
        if ( node1 == null ) {
            printMessageAndExit("Node 1 doesn't exist.");
        }
        
        // getting Write Configuration Service on node 1
        WriteConfigurationService writeConfigService = node1.getService(WriteConfigurationService.class);
        if ( writeConfigService == null ) {
            printMessageAndExit("Node 1 doesn't support Write Configuration Service.");
        }
        
        // setting service parameters
        WriteConfigurationServiceParameters serviceParams 
                = new WriteConfigurationServiceParameters(
                        "config" + File.separator + "dctr-configs" + File.separator + "dpa-3xx" + File.separator + "TR_config_3_00.xml",
//                      "config" + File.separator + "dctr-configs" + File.separator + "coordinator.xml"
                        "config" + File.separator + "dctr-configs" + File.separator + "node.xml"
        );
        
        // do not check HWP ID
        serviceParams.setHwpId(DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK);
        
        // writing configuration - only for node, which this service resides on
        ServiceResult<WriteResult, WriteConfigurationProcessingInfo> serviceResult 
                = writeConfigService.writeConfiguration(serviceParams);
        
        // getting results
        if ( serviceResult.getStatus() == ServiceResult.Status.SUCCESSFULLY_COMPLETED ) {
            System.out.println("Configuration successfully written.");
        } else {
            System.out.println("Configuration write was NOT successful.");
            
            // find out details about errors for nodes, which failed to write configuration into
            WriteResult writeResult = serviceResult.getResult();
            if ( writeResult != null ) {
                printNodesResults(writeResult);
            } else {
                WriteConfigurationProcessingInfo procInfo = serviceResult.getProcessingInfo();
                System.out.println(procInfo);
            }
        }
        
        simply.destroy();
    }
    
    // prints results of writing for each node
    private static void printNodesResults(WriteResult writeResult) {
        for ( String nodeId : writeResult.getAllNodeResultsMap().keySet() ) {
            WriteResult.NodeWriteResult nodeWriteResult = writeResult.getNodeResult(nodeId);
            System.out.println("Node: " + nodeId);
            
            System.out.println("Failed configuration bytes: ");
            for ( HWP_ConfigurationByte configByte : nodeWriteResult.getWritingFailedBytes().values() ) {
                System.out.println(configByte);
            }
            System.out.println();
            
            System.out.println("Failed security: ");
            WriteResult.SecurityResult securityResult = nodeWriteResult.getSecurityResult();
            
            if ( securityResult.isPasswordToWrite() && !securityResult.getPaswordWriteResult() ) {
                System.out.println("Password");
            }
            
            if ( securityResult.isKeyToWrite() && !securityResult.getKeyWriteResult() ) {
                System.out.println("Key");
            }
            
            System.out.println();
        }
    }
}
