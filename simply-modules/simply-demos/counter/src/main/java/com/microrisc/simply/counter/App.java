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
package com.microrisc.simply.counter;

import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.devices.protronix.dpa22x.Counter;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import com.microrisc.simply.counter.config.ApplicationConfiguration;
import com.microrisc.simply.counter.config.DeviceInfo;
import com.microrisc.simply.counter.mqtt.MqttCommunicator;
import com.microrisc.simply.counter.mqtt.MqttConfiguration;
import com.microrisc.simply.counter.mqtt.MqttFormatter;
import com.microrisc.simply.counter.mqtt.MqttTopics;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Sending number of counted objects over MQTT in JSON SENML format.
 *
 * @author Michal Konopa
 */
public final class App {

    // references for DPA
    private static DPA_Simply simply = null;
    
    // map of counters
    private static Map<String, CompoundDeviceObject> deviceMap = null;
    
    // OS info map
    private static Map<String, OsInfo> osInfoMap = null;
    
    
    // MQTT elements
    // references for MQTT configuration
    private static MqttConfiguration mqttConfiguration = null;
    
    // references for MQTT communication
    private static MqttCommunicator mqttCommunicator = null;
    
    // topics
    private static MqttTopics mqttTopics = null;
    
    
    // application related references
    private static ApplicationConfiguration appConfiguration = null;
    
    // not used so far
    private static int pid = 0;
    
    
    
    // MAIN PROCESSING
    public static void main(String[] args) {
        init();
        
        // main application loop
        while ( true ) {
            // getting data from devices
            Map<String, Object> dataFromDevices = getDataFromDevices();

            // converting data to MQTT form
            Map<String, List<String>> mqttDataFromDevices = toMqttForm(dataFromDevices);

            // publishing converted data
            mqttSendAndPublish(mqttDataFromDevices);
            
            try {
                Thread.sleep(appConfiguration.getPollingPeriod() * 1000);
            } catch ( InterruptedException ex ) {
                printMessageAndExit("Application interrupted");
            }
        }
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
        
        // topics initialization
        mqttTopics =  new MqttTopics.Builder().gwId(mqttConfiguration.getRootTopic())
                .stdSensorsProtronix("/iqrf/iaq/protronix")
                .stdSensorsProtronixErrors("/iqrf/iaq/protronix/errors/")
                .build();
    }
    
    // main initialization method
    private static void init() {
        // application exit hook
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {

            @Override
            public void run() {
                System.out.println("End via shutdown hook.");
                releaseUsedResources();
            }
        }));

        // loading application configuration
        try {
            appConfiguration = loadApplicationConfiguration("App.json");
        } catch ( Exception ex ) {
            printMessageAndExit("Error in loading application configuration: " + ex);
        }
        
        // Simply initialization
        String simplyConfigFile = "";
        
        if ( appConfiguration.getCommunicationInterface().equalsIgnoreCase("cdc") ) {
            simplyConfigFile = "Simply-CDC.properties";
        } else if ( appConfiguration.getCommunicationInterface().equalsIgnoreCase("spi") ) {
            simplyConfigFile = "Simply-SPI.properties";
        } else {
            printMessageAndExit("Not supported communication interface: " + appConfiguration.getCommunicationInterface());
        }
        
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "simply" + File.separator + simplyConfigFile);
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }
        
        // getting reference to IQRF DPA network to use
        Network dpaNetwork = simply.getNetwork("1", Network.class);
        if ( dpaNetwork == null ) {
           printMessageAndExit("DPA Network doesn't exist");
        }
        
        // reference to map of all nodes in the network
        Map<String, Node> nodesMap = dpaNetwork.getNodesMap();
        
        // reference to OS Info
        osInfoMap = getOsInfoFromNodes(nodesMap);
        
        // printing MIDs of nodes in the network
        printMIDs(osInfoMap);
        
        deviceMap = getDevicesMap(nodesMap);
        
        initMqttElements();
    }
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        releaseUsedResources();
        System.exit(1);
    }
    
    // tests, if specified node ID is in valid interval
    private static boolean isNodeIdInValidInterval(long nodeId) {
        return ( nodeId > 0 && nodeId <= appConfiguration.getNumberOfDevices() );
    }
    
    // returns reference to map of OS info objects for specified nodes map
    private static Map<String, OsInfo> getOsInfoFromNodes(Map<String, Node> nodesMap) {
        Map<String, OsInfo> osInfoMap = new LinkedHashMap<>();
        
        for ( Map.Entry<String, Node> entry : nodesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
                
            System.out.println("Getting OS info on the node: " + entry.getKey());

            // OS peripheral
            OS os = entry.getValue().getDeviceObject(OS.class);
            
            if ( os != null ) {
                // get OS info about module
                OsInfo osInfo = os.read();
                if ( osInfo != null ) {
                    osInfoMap.put(entry.getKey(), osInfo);
                } else {
                    CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
                    if ( procState == ERROR ) { 
                        CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                        System.err.println("Getting OS info failed: " + error);
                    } else {
                        System.err.println("Getting OS info hasn't been processed yet: " + procState);
                    }
                }
            } else {
                System.err.println("OS doesn't exist on node");
            }
        }
        
        return osInfoMap;
    }
    
    // prints MIDs of specified nodes in the map
    private static void printMIDs(Map<String, OsInfo> osInfoMap) {
        for ( Map.Entry<String, OsInfo> entry : osInfoMap.entrySet() ) {
            System.out.println("Node: " + entry.getKey() + " MID: " + entry.getValue().getPrettyFormatedModuleId() );
        }
    }
    
    // returns map devices from specified map of nodes
    private static Map<String, CompoundDeviceObject> getDevicesMap(Map<String, Node> nodesMap) {
        Map<String, CompoundDeviceObject> devicesMap = new LinkedHashMap<>();
        
        for ( Map.Entry<String, Node> entry : nodesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            System.out.println("Getting device: " + entry.getKey());
            DeviceInfo sensorInfo = appConfiguration.getDevicesInfoMap().get(nodeId);

            switch ( sensorInfo.getType() ) {
                case "counter":
                    Counter counter = entry.getValue().getDeviceObject(Counter.class);
                    if ( counter != null ) {
                        devicesMap.put(entry.getKey(), (CompoundDeviceObject) counter);
                        System.out.println("Device type: " + sensorInfo.getType());
                    } else {
                        System.err.println("Counter sensor not found on node: " + nodeId);
                    }
                break;

                default:
                    printMessageAndExit("Device type not supported:" + sensorInfo.getType());
                break;
            }
        }
        
        return devicesMap;
    }
    
    // returns data from devices indexed by node ID
    private static Map<String, Object> getDataFromDevices() {
        Map<String, Object> dataFromDevices = new HashMap<>();
        
        for ( Map.Entry<String, CompoundDeviceObject> entry : deviceMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            DeviceInfo sensorInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Getting data from device: " + entry.getKey());
            
            // we are interested only in Counter DI
            String sensorType = sensorInfo.getType();
            if ( !sensorType.equalsIgnoreCase("counter") ) {
                continue;
            }
            
            CompoundDeviceObject compDevObject = entry.getValue();
            if ( compDevObject == null ) {
                System.err.println("Device not found. Id: " + entry.getKey());
                continue;
            }
            
            if ( !(compDevObject instanceof Counter) ) {
                System.err.println("Bad type of device. Got: " + compDevObject.getClass() 
                    + ", expected: " + Counter.class
                );
                continue;
            }
                    
            Counter counter = (Counter)compDevObject;
            Integer countedObjectsNum = counter.count();
            if ( countedObjectsNum != null ) {
                dataFromDevices.put(entry.getKey(), countedObjectsNum);
            } else {
                CallRequestProcessingState requestState = counter.getCallRequestProcessingStateOfLastCall();
                if ( requestState == ERROR ) {                        
                    CallRequestProcessingError error = counter.getCallRequestProcessingErrorOfLastCall();
                    System.err.println("Error while getting data from counter: " + error);

                    String mqttError = MqttFormatter.formatError( String.valueOf(error) );
                    mqttPublishErrors(nodeId, mqttTopics, mqttError);
                } else {
                    System.err.println(
                        "Could not get data from counter. State of the request: " + requestState
                    );
                }
            } 
        }
        
        return dataFromDevices;
    }
    
    // for specified devices data returns their equivalent MQTT form
    private static Map<String, List<String>> toMqttForm(
            Map<String, Object> dataFromDevices
    ) {
        Map<String, List<String>> mqttAllData = new LinkedHashMap<>();
        
        // for each sensor's data
        for ( Map.Entry<String, Object> entry : dataFromDevices.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            // mqtt data for 1 device
            List<String> deviceData = new LinkedList<>();
            
            DeviceInfo devicesInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Preparing MQTT message for node: " + entry.getKey());
            
            switch ( devicesInfo.getType().toLowerCase() ) {
                case "counter":
                    Integer countedValue = (Integer)entry.getValue();
                    if ( countedValue == null ) {
                        System.out.println(
                            "No data received from device, check log for details "
                            + "about protronix counter data"
                        );
                        mqttAllData.put(entry.getKey(), null);
                        break;
                    }
                    
                    // packet id
                    //pid++;
                    
                    //String moduleId = getModuleId(entry.getKey(), osInfoMap);
                    String clientId = mqttConfiguration.getClientId();
                    
                    String mqttDataProtronix = MqttFormatter
                            .formatCountedObjectsNum(
                                    "people", 
                                    String.valueOf(countedValue), 
                                    clientId
                            );

                    deviceData.add(mqttDataProtronix);
                    mqttAllData.put(entry.getKey(), deviceData);
                break;

                default:
                    printMessageAndExit("Device type not supported:" + devicesInfo.getType());
                break;
            }                      
        }
        
        return mqttAllData;
    }
    
    // sends and publishes prepared json messages with data from sensors to 
    // specified MQTT topics
    private static void mqttSendAndPublish(Map<String, List<String>> dataFromsSensorsMqtt) { 
        for ( Map.Entry<String, List<String>> entry : dataFromsSensorsMqtt.entrySet() ) {        
            int nodeId = Integer.parseInt(entry.getKey());
            
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            if ( entry.getValue() != null ) {
                System.out.println("Sending parsed data for node: " + entry.getKey());

                for ( String mqttData : entry.getValue() ) {
                    try {
                       mqttCommunicator.publish(mqttTopics.getStdSensorsProtronix(), 2, mqttData.getBytes());
                    } catch ( MqttException ex ) {
                        System.err.println("Error while publishing sync dpa message: " + ex);
                    }
                }
            } else {
                System.err.println("No data found for sensor: " + entry.getKey());
            }
        }
    }
    
    // publish error messages to specified MQTT topics
    private static void mqttPublishErrors(int nodeId, MqttTopics mqttTopics, String errorMessage) {
        try {
            mqttCommunicator.publish(mqttTopics.getStdSensorsProtronixErrors() + nodeId, 2, errorMessage.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing error message: " + ex);
        }
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
    
    // loads app configuration from file
    private static ApplicationConfiguration loadApplicationConfiguration(String configFile) 
            throws IOException, ParseException 
    {
        JSONObject appJsonObjects = (JSONObject) JSONValue
            .parseWithException( new FileReader(
                    "config" + File.separator + "app" + File.separator + configFile
            )
        );

        // get the devices
        JSONArray devicesArray = (JSONArray) appJsonObjects.get("devices");

        Map<Integer, DeviceInfo> devicesInfos = new HashMap<>();
        for ( int i = 0; i < devicesArray.size(); i++ ) {
            JSONObject deviceObjects = (JSONObject) devicesArray.get(i);

            DeviceInfo deviceInfo = new DeviceInfo(
                    (long) deviceObjects.get("device"),
                    (String) deviceObjects.get("manufacturer"),
                    (String) deviceObjects.get("type")
            );

            devicesInfos.put((int)deviceInfo.getId(), deviceInfo);
        }
        
        return new ApplicationConfiguration(
                (long) appJsonObjects.get("pollingPeriod"),
                (String) appJsonObjects.get("communicationInterface"),
                devicesInfos
        );
    }
    
    // releases used resources
    private static void releaseUsedResources() {
        if ( simply != null ) {
            simply.destroy();
        }
    }
}
