/* 
 * Copyright 2014 MICRORISC s.r.o.
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

package com.microrisc.simply.iqrf.dpa.v22x.examples.std_per.io;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.Simply;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_Command;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_DirectionSettings;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_OutputValueSettings;
import com.microrisc.simply.iqrf.types.VoidType;
import java.io.File;

/**
 * Example of using IO Peripheral - synchronous version.
 * 
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public class SetAllPinsOutHigh {
    // reference to Simply
    private static Simply simply = null;
    
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
        Network network1 = simply.getNetwork("1", Network.class);
        if ( network1 == null ) {
            printMessageAndExit("Network 1 doesn't exist");
        }
        
        // getting node 1
        Node node1 = network1.getNode("1");
        if ( node1 == null ) {
            printMessageAndExit("Node 1 doesn't exist");
        }
        
        IO io = node1.getDeviceObject(IO.class);
        if ( io == null ) {
            printMessageAndExit("IO not present or enabled at node 1");
        }
        
        // set all pins OUT
        IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[] {
            new IO_DirectionSettings(0x00, 0x21, 0x00),
            new IO_DirectionSettings(0x01, 0x10, 0x00),
            new IO_DirectionSettings(0x02, 0xFC, 0x00)
        };

        VoidType result = io.setDirection(dirSettings);
        if ( result == null ) {
            CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
            printMessageAndExit("Setting IO direction failed: " + error);
        }
        
        // set all pins HIGH
        IO_Command[] iocs = new IO_OutputValueSettings[] {
            new IO_OutputValueSettings(0x00, 0x21, 0x21),
            new IO_OutputValueSettings(0x01, 0x10, 0x10),
            new IO_OutputValueSettings(0x02, 0xFC, 0xFC)
        };

        result = io.setOutputState(iocs);
        if ( result == null ) {
            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Setting IO output state failed: " + error);
            } else {
                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState);
            } 
        }
        
        // end of working with Simply
        simply.destroy();
    }
}
