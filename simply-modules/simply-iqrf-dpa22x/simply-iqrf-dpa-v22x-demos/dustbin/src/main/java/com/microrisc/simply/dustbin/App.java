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

package com.microrisc.simply.dustbin;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.dustbin.mqtt.MqttCommunicator;
import com.microrisc.simply.dustbin.mqtt.MqttConfiguration;
import com.microrisc.simply.dustbin.mqtt.MqttFormatter;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.FRC;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_Data;
import com.microrisc.simply.iqrf.dpa.v22x.types.FRC_UART_SPI_data;
import com.microrisc.simply.iqrf.dpa.v22x.types.SleepInfo;
import com.microrisc.simply.iqrf.types.VoidType;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Dustbins.
 * 
 * @author Michal Konopa
 */
public final class App {
   
    // Simply object
    private static DPA_Simply simply = null;
    
    // network
    private static Network network = null;
    
    // FRC on coordinator
    private static FRC frcCoord = null;
    
    // UARTs on nodes in the network
    private static Map<String, UART> uarts = null;
    
    // OSes on nodes in the network
    private static Map<String, OS> oses = null;
    
    
    // interval of read data from devices
    private static final int READ_INTERVAL = 10000;
    
    // interval of reading data from dustbin UART [in ms]
    private static final int READ_FROM_DUSTBIN_INTERVAL = 10;
    
    // node sleeping time
    private static final int NODE_SLEEPING_TIME = (int)Math.round((10 * 60) / 2.097);
            
    // MQTT related elements
    // configuration
    private static MqttConfiguration mqttConfiguration = null;
    
    // communicator
    private static MqttCommunicator mqttCommunicator = null;
    
    // parameters for publishing data 
    private static String PUBLISH_TOPIC = "/std/dustbin/data";
    private static int PUBLISH_QOS = 2;
    
    
    public static void main(String[] args) {
        init();
        
        // main loop
        while ( true ) {
            Map<String, FRC_UART_SPI_data.Result> dataAvailability = getDataAvailability();
            if ( dataAvailability != null ) {
                Map<String, short[]> data = readDataFromDustbin(dataAvailability);
                setDustbinIntoSleepingMode(data.keySet());
                sendDataToMqtt(data);
            }
           
            try {
                Thread.sleep(READ_INTERVAL);
            } catch ( InterruptedException ex ) {
                printMessageAndExit("Read interval interrupted");
            }
        }
    }
    
    // returns UARTs on nodes
    private static Map<String, UART> getUarts() {
        Map<String, UART> uarts = new HashMap<>();
        
        for ( Node node : network.getNodesMap().values() ) {
            UART uart = node.getDeviceObject(UART.class);
            if ( uart != null ) {
                uarts.put(node.getId(), uart);
            }
        }
        
        return uarts;
    }
    
    // returns OSes on nodes
    private static Map<String, OS> getOses() {
        Map<String, OS> oses = new HashMap<>();
        
        for ( Node node : network.getNodesMap().values() ) {
            OS os = node.getDeviceObject(OS.class);
            if ( os != null ) {
                oses.put(node.getId(), os);
            }
        }
        
        return oses;
    }
    
    // loads mqtt params from file
    private static MqttConfiguration loadMqttConfiguration(String configFile)
            throws IOException, ParseException 
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(
                new FileReader("config" + File.separator + "mqtt" + File.separator + configFile)
        );

        JSONObject jsonObject = (JSONObject) obj;

        return new MqttConfiguration(
                (String) jsonObject.get("protocol"),
                (String) jsonObject.get("broker"),
                (long) jsonObject.get("port"),
                (String) jsonObject.get("clientid"),
                (String) jsonObject.get("gwid"),
                (boolean) jsonObject.get("cleansession"),
                (boolean) jsonObject.get("quitemode"),
                (boolean) jsonObject.get("ssl"),
                (String) jsonObject.get("certfile"),
                (String) jsonObject.get("username"),
                (String) jsonObject.get("password"),
                (String) jsonObject.get("roottopic")
        );
    }
    
    // inits MQTT related elements
    private static void initMqttElements() {
        // loading MQTT configuration
        try {
            mqttConfiguration = loadMqttConfiguration("Mqtt.json");
        } catch ( Exception ex ) {
            printMessageAndExit("Error in loading MQTT configuration: " + ex);
        }
        
        // creating MQTT communicator
        try {
            mqttCommunicator = new MqttCommunicator(mqttConfiguration);
        } catch ( MqttException ex ) {
            printMessageAndExit("Error while creating MQTT commnicator: " + ex);
        }
    }
    
    // main initialization method
    private static void init() {
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "simply" + File.separator + "Simply.properties");
        } catch ( Exception ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }
        
        network = simply.getNetwork("1", Network.class);
        if ( network == null ) {
            printMessageAndExit("DPA Network doesn't exist");
        }
        
        Node coordNode = network.getNode("0");
        if ( coordNode == null ) {
            printMessageAndExit("Coordinator node doesn't exist in the network.");
        }
        
        frcCoord = coordNode.getDeviceObject(FRC.class);
        if ( frcCoord == null ) {
            printMessageAndExit("Coordinator node doesn't support FRC.");
        }
        
        uarts = getUarts();
        oses = getOses();
        
        initMqttElements();
    }
    
    // checks result status of FRC command
    private static boolean isFrcSuccessfull(int status) {
        if ( (status > 0x00) && (status <= 0xEF) ) {
            return true;
        }
        
        if ( (status >= 0xF0) && (status <= 0xFC) ) {
            throw new IllegalStateException("Reserved status used: " + status);
        }
        
        return false;
    }
    
    // get availiability of data
    private static Map<String, FRC_UART_SPI_data.Result> getDataAvailability() {
        FRC_Data frcData = frcCoord.send( new FRC_UART_SPI_data() );
        if ( frcData == null ) {
            CallRequestProcessingState procState = frcCoord.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                System.err.println(
                        "Getting data availability failed. Error: " 
                        + frcCoord.getCallRequestProcessingErrorOfLastCall() 
                );
            } else {
                System.err.println("Getting data availability failed. State: " + procState);
            }
            return null;
        }
        
        if ( !isFrcSuccessfull(frcData.getStatus()) ) {
            System.err.println("FRC unsucessful. Status = " + frcData.getStatus());
            return null;
        }
        
        try {
            return FRC_UART_SPI_data.parse(frcData.getData());
        } catch ( Exception ex ) {
            System.err.println("Parsing of FRC result data failed: " + ex);
            return null;
        }
    }
    
    // reads data - according to its availability from dustbin
    private static Map<String, short[]> readDataFromDustbin(
            Map<String, FRC_UART_SPI_data.Result> dataAvailability
    ) {
        
        Map<String, short[]> readData = new HashMap<>();
                
        for ( Map.Entry<String, FRC_UART_SPI_data.Result> entry : dataAvailability.entrySet() ) {
            FRC_UART_SPI_data.Result avalResult = entry.getValue();
            
            // skip over nodes whose data is not available
            if ( !avalResult.isAccessible() ) {
                continue;
            }
            
            UART uart = uarts.get(entry.getKey());
            if ( uart == null ) {
                System.err.println("UART not found on node " + entry.getKey());
                continue;
            }
            
            short[] uartData = uart.writeAndRead(READ_FROM_DUSTBIN_INTERVAL, new short[] {});
            if ( uartData == null ) {
                CallRequestProcessingState procState = uart.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    System.err.println(
                            "Getting data from" + entry.getKey() + " failed. Error: " 
                            + uart.getCallRequestProcessingErrorOfLastCall() 
                    );
                } else {
                    System.err.println("Getting data from " + entry.getKey() + "failed. State: " + procState);
                }
                continue;
            }
            
            readData.put(entry.getKey(), uartData);
        }
        
        return readData;
    }
    
    // sets device on specified nodes into sleeping mode
    private static void setDustbinIntoSleepingMode(Set<String> nodeIds) {
        for ( String nodeId : nodeIds ) {
            OS os = oses.get(nodeId);
            if ( os == null ) {
                System.err.println("OS not found on node " + nodeId);
                continue;
            }
            
            VoidType sleepResult = os.sleep( new SleepInfo(NODE_SLEEPING_TIME, 0));
            if ( sleepResult == null ) {
                CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    System.err.println(
                            "Putting node" + nodeId + " into sleeping mode failed. Error: " 
                            + os.getCallRequestProcessingErrorOfLastCall() 
                    );
                } else {
                    System.err.println("Putting node" + nodeId + " into sleeping mode failed. State: " + procState);
                }
            }
        }
    }
    
    // converts specified dustbin data and sends them to web using mqtt
    private static void sendDataToMqtt(Map<String, short[]> data) {
        for ( Map.Entry<String, short[]> entry : data.entrySet() ) {
            String responseData = MqttFormatter
                    .formatDustbinData(PUBLISH_TOPIC, entry.getKey(), entry.getValue());

            // send result's data to mqtt
            try {
                mqttCommunicator.publish(
                        mqttConfiguration.getGwId() + PUBLISH_TOPIC, PUBLISH_QOS, responseData.getBytes()
                );
            } catch ( MqttException ex ) {
                System.err.println("Error while publishing data: " + ex);
            }
        }
    }
    
    // releases used resources
    private static void releaseUsedResources() {
        if ( simply != null ) {
            simply.destroy();
        }
    }
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        releaseUsedResources();
        System.exit(1);
    }
}
