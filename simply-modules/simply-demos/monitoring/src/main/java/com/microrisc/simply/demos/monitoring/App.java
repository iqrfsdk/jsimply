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
package com.microrisc.simply.demos.monitoring;

import com.microrisc.simply.demos.config.ApplicationConfiguration;
import com.microrisc.simply.demos.config.DeviceInfo;
import com.microrisc.simply.demos.mqtt.MqttConfiguration;
import com.microrisc.simply.demos.mqtt.MqttTopics;
import com.microrisc.simply.demos.mqtt.MqttCommunicator;
import com.microrisc.simply.demos.mqtt.MqttFormatter;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.compounddevices.CompoundDeviceObject;
import com.microrisc.simply.devices.protronix.dpa22x.CO2Sensor;
import com.microrisc.simply.devices.protronix.dpa22x.VOCSensor;
import com.microrisc.simply.devices.protronix.dpa22x.types.CO2SensorData;
import com.microrisc.simply.devices.protronix.dpa22x.types.VOCSensorData;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.errors.CallRequestProcessingErrorType;
import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Coordinator;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_Parameter;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
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
 * Sending Protronix values over MQTT in JSON SENML format .
 *
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public final class App {
    
    // data to publish
    private static class DataToPublish {
        Object sensorData;
        Integer rssi;
        
        public DataToPublish(Object sensorData, Integer rssi) {
            this.sensorData = sensorData;
            this.rssi = rssi;
        }
    }
    
    // RSSI is not avalaible
    private static final int RSSI_NOT_AVAILABLE = 0;
    
    
    // references for DPA
    private static DPA_Simply dpaSimply = null;
    
    // references for MQTT
    private static MqttCommunicator mqttCommunicator = null;
    
    // application related references
    private static ApplicationConfiguration appConfiguration = null;
    
    // MQTT topics
    private static MqttTopics mqttTopics = null;
    
    // OS's info map
    private static Map<String, OsInfo> osInfoMap = null;
    
    // sensor's map
    private static Map<String, CompoundDeviceObject> sensorsMap = null;
    
    // not used so far
    private static int pid = 0;
    
    
    // number of attempts to get data from sensors (VOC and CO2)
    private static final int GET_SENSOR_DATA_ATTEMPTS_NUM = 3;
    
    
    // MAIN PROCESSING
    public static void main(String[] args) {
        // initialization
        init();
        
        // main application loop
        while ( true ) {
            getAndPublishSensorData();
            try {
                Thread.sleep(appConfiguration.getPollingPeriod() * 1000);
            } catch ( InterruptedException ex ) {
                printMessageAndExit(
                        "Application interrupted while present in polling period: " 
                        + ex.getMessage()
                );
            }
        }
    }
    
    // initializes application
    private static void init() {
        // application exit hook
        Runtime.getRuntime().addShutdownHook( new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("End via shutdown hook.");
                releaseResources();
            }
        }));
        
        // loading application configuration
        try {
            appConfiguration = loadApplicationConfiguration("App.json");
        } catch ( Exception ex ) {
            printMessageAndExit("Error in loading application configuration: " + ex);
        }
        
        // Simply initialization
        if ( appConfiguration.getCommunicationInterface().equalsIgnoreCase("cdc")) {
            dpaSimply = getDPA_Simply("Simply-CDC.properties");
        } else if( appConfiguration.getCommunicationInterface().equalsIgnoreCase("spi")) {
            dpaSimply = getDPA_Simply("Simply-SPI.properties");
        } else {
            printMessageAndExit("No supported communication interface: " + appConfiguration.getCommunicationInterface());
        }
        
        initMqtt();
        
        // getting reference to IQRF DPA network to use
        Network dpaNetwork = dpaSimply.getNetwork("1", Network.class);
        if ( dpaNetwork == null ) {
            printMessageAndExit("DPA Network doesn't exist");
        }
        
        // reference to map of all nodes in the network
        Map<String, Node> nodesMap = dpaNetwork.getNodesMap();
        
        // reference to OS Info
        osInfoMap = getOsInfoFromNodes(nodesMap);
        
        // printing MIDs of nodes in the network
        printMIDs(osInfoMap);
        
        // reference to sensors
        sensorsMap = getSensorsMap(nodesMap);
        
        // setting, that last RSSI value will be returned in every DPA response or confirmation
        setGettingLastRssi(dpaNetwork);
    }
    
    // inits MQTT related functionality
    private static void initMqtt() {
        // loading MQTT configuration
        MqttConfiguration mqttConfiguration = null;
        try {
            mqttConfiguration = loadMqttConfiguration("Mqtt.json");
        } catch ( Exception ex ) {
            printMessageAndExit("Error in loading MQTT configuration: " + ex);
        } 
        
        // topics initialization
        mqttTopics = new MqttTopics.Builder().gwId(mqttConfiguration.getGwId())
                .stdSensorsProtronix("/std/sensors/protronix/")
                .stdSensorsProtronixErrors("/std/sensors/protronix/errors/")
                .build();

        try {
            mqttCommunicator = new MqttCommunicator(mqttConfiguration);
        } catch ( MqttException ex ) {
            printMessageAndExit("Error while creation of MQTT communicator: " + ex);
        }
    }
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        releaseResources();
        System.exit(1);
    }
    
    // sets getting last RSSI in DPA responses or notifications
    private static void setGettingLastRssi(Network dpaNetwork) {
        Node node0 = dpaNetwork.getNode("0");
        if ( node0 == null ) {
            printMessageAndExit("Node 0 doesn't exist");
        }
        
        Coordinator coord = node0.getDeviceObject(Coordinator.class);
        if ( coord == null ) {
            printMessageAndExit("Coordinator doesn't exist on Node 0");
        }
        
        DPA_Parameter dpaParam = coord.setDPA_Param( 
                new DPA_Parameter(DPA_Parameter.DPA_ValueType.LAST_RSSI, false, false)
        );
        
        if ( dpaParam == null ) {
            CallRequestProcessingError error = coord.getCallRequestProcessingErrorOfLastCall();
            if ( error != null ) {
                System.out.println("Error while setting DPA parameter: " + error);
            }
            printMessageAndExit("Setting DPA parameter NOT successfull.");
        }
    }
    
    // gets data from sensors and publishes them
    /*
         task:
         1. Obtain data from sensors.
         2. Creation of MQTT form of obtained sensor's data. 
         3. Sending MQTT form of sensor's data through MQTT to destination point.
    */
    private static void getAndPublishSensorData() {
        Map<String, DataToPublish> dataFromSensorsMap = getDataFromSensors();

        // getting MQTT form of data from sensors
        Map<String, List<String>> dataFromSensorsMqtt = toMqttForm(dataFromSensorsMap);

        // sending data
        mqttSendAndPublish(dataFromSensorsMqtt);
    }
    
    // init dpa simply
    private static DPA_Simply getDPA_Simply(String configFile) {
        DPA_Simply DPASimply = null;
        
        try {
            DPASimply = DPA_SimplyFactory.getSimply("config" + File.separator + "simply" + File.separator + configFile);
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }
        
        return DPASimply;
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
                        // general call error    
                        CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                        System.err.println("Getting OS info failed: " + error);
                        
                        if (error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL) {
                            // specific call error
                            DPA_AdditionalInfo dpaAddInfo = os.getDPA_AdditionalInfoOfLastCall();
                            if ( dpaAddInfo != null ) {
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                System.err.println("DPA response code: " + dpaResponseCode);
                            }
                        }
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
    
    // returns map of CO2 and VOC sensors from specified map of nodes
    private static Map<String, CompoundDeviceObject> getSensorsMap(Map<String, Node> nodesMap) {
        Map<String, CompoundDeviceObject> sensorsMap = new LinkedHashMap<>();
        
        for ( Map.Entry<String, Node> entry : nodesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            System.out.println("Getting device: " + entry.getKey());
            DeviceInfo sensorInfo = appConfiguration.getDevicesInfoMap().get(nodeId);

            switch ( sensorInfo.getType() ) {
                case "co2-t-h":
                    CO2Sensor co2Sensor = entry.getValue().getDeviceObject(CO2Sensor.class);
                    if ( co2Sensor != null ) {
                        sensorsMap.put(entry.getKey(), (CompoundDeviceObject) co2Sensor);
                        System.out.println("Device type: " + sensorInfo.getType());
                    } else {
                        System.err.println("CO2 sensor not found on node: " + nodeId);
                    }
                break;

                case "voc-t-h":
                    VOCSensor vocSensor = entry.getValue().getDeviceObject(VOCSensor.class);
                    if ( vocSensor != null ) {
                        sensorsMap.put(entry.getKey(), (CompoundDeviceObject) vocSensor);
                        System.out.println("Device type: " + sensorInfo.getType());
                    } else {
                        System.err.println("VOC sensor not found on node: " + nodeId);
                    }
                break;

                default:
                    printMessageAndExit("Device type not supported:" + sensorInfo.getType());
                break;
            }
        }
        
        return sensorsMap;
    }
    
    // returns data from sensors as specicied by map
    private static Map<String, DataToPublish> getDataFromSensors() {
        // data from sensors
        Map<String, DataToPublish> dataFromSensors = new HashMap<>();
        
        for ( Map.Entry<String, CompoundDeviceObject> entry : sensorsMap.entrySet() ) {
            
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            DeviceInfo sensorInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Getting data from sensor " + entry.getKey());
            
            String moduleId = getModuleId(entry.getKey(), osInfoMap);
            String nadr = entry.getKey();
            
            switch ( sensorInfo.getType() ) {
                case "co2-t-h":
                    CompoundDeviceObject compDevObject = entry.getValue();
                    if ( compDevObject == null ) {
                        System.err.println("Sensor not found. Id: " + entry.getKey());
                        break;
                    }
                    
                    if ( !(compDevObject instanceof CO2Sensor) ) {
                        System.err.println("Bad type of sensor. Got: " + compDevObject.getClass() 
                            + ", expected: " + CO2Sensor.class
                        );
                        break;
                    }
                    
                    CO2Sensor co2Sensor = (CO2Sensor)compDevObject;
                    CO2SensorData co2SensorData = getCo2SensorData(co2Sensor);
                    if ( co2SensorData != null ) {
                        Integer rssi = null;
                        DPA_AdditionalInfo addInfo = co2Sensor.getDPA_AdditionalInfoOfLastCall();
                        if ( addInfo == null ) {
                            System.err.println("No additional info for CO2 sensor");
                        } else {
                            rssi = addInfo.getDPA_Value();
                        }
                        dataFromSensors.put(entry.getKey(), new DataToPublish(co2SensorData, rssi) );
                    } else {
                        CallRequestProcessingState requestState = co2Sensor.getCallRequestProcessingStateOfLastCall();
                        if ( requestState == ERROR ) {                      
                            // call error    
                            CallRequestProcessingError error = co2Sensor.getCallRequestProcessingErrorOfLastCall();
                            System.err.println("Error while getting data from CO2 sensor: " + error);
                            
                            String mqttError = MqttFormatter
                                    .formatError( 
                                            String.valueOf(error),
                                            moduleId,
                                            nadr
                                    );
                            mqttPublishErrors(nodeId, mqttTopics, mqttError);
                            
                            // specific call error
                            if ( error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL ) {
                                DPA_AdditionalInfo dpaAddInfo = co2Sensor.getDPA_AdditionalInfoOfLastCall();
                                if ( dpaAddInfo != null ) {
                                    DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                    System.err.println("DPA response code: " + dpaResponseCode);  
                                }
                            }
                        } else {
                            System.err.println(
                                "Could not get data from CO2 sensor. State of the sensor: " + requestState
                            );
                        }
                    } 
                break;

                case "voc-t-h":
                    compDevObject = entry.getValue();
                    if ( compDevObject == null ) {
                        System.err.println("Sensor not found. Id: " + entry.getKey());
                        break;
                    }
                    
                    if ( !(compDevObject instanceof VOCSensor) ) {
                        System.err.println("Bad type of sensor. Got: " + compDevObject.getClass() 
                            + ", expected: " + VOCSensor.class
                        );
                        break;
                    }
                    
                    VOCSensor vocSensor = (VOCSensor)compDevObject;
                    VOCSensorData vocSensorData = getVocSensorData(vocSensor);
                    if ( vocSensorData != null ) {
                        Integer rssi = null;
                        DPA_AdditionalInfo addInfo = vocSensor.getDPA_AdditionalInfoOfLastCall();
                        if ( addInfo == null ) {
                            System.err.println("No additional info for VOC sensor");
                        } else {
                            rssi = addInfo.getDPA_Value();
                        }
                        dataFromSensors.put(entry.getKey(), new DataToPublish(vocSensorData, rssi) );
                    } else {
                        CallRequestProcessingState requestState = vocSensor.getCallRequestProcessingStateOfLastCall();
                        if ( requestState == ERROR ) {
                            // general call error
                            CallRequestProcessingError error = vocSensor.getCallRequestProcessingErrorOfLastCall();
                            System.err.println("Error while getting data from VOC sensor: " + error);
                            
                            String mqttError = MqttFormatter
                                    .formatError( 
                                            String.valueOf(error),
                                            moduleId,
                                            nadr
                                    );
                            mqttPublishErrors(nodeId, mqttTopics, mqttError);
                            
                            // specific call error
                            if (error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL) {
                                DPA_AdditionalInfo dpaAddInfo = vocSensor.getDPA_AdditionalInfoOfLastCall();
                                if ( dpaAddInfo != null ) {
                                    DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                    System.err.println("DPA response code: " + dpaResponseCode);
                                }
                            }
                        } else {
                            System.err.println(
                                "Could not get data from VOC sensor. State of the sensor: " + requestState
                            );
                        }
                    }
                break;

                default:
                    printMessageAndExit("Device type not supported:" + sensorInfo.getType());
                break;
            }
        }
        
        return dataFromSensors;
    }
    
    // tryes to get data of speciffied CO2 sensor
    private static CO2SensorData getCo2SensorData(CO2Sensor co2Sensor) {
        for ( int attempt = 0; attempt < GET_SENSOR_DATA_ATTEMPTS_NUM; attempt++ ) {
            CO2SensorData co2SensorData = co2Sensor.get();
            if ( co2SensorData != null ) {
                return co2SensorData;
            }
        }
        
        return null;
    }
    
    // tryes to get data of speciffied VOC sensor
    private static VOCSensorData getVocSensorData(VOCSensor vocSensor) {
        for ( int attempt = 0; attempt < GET_SENSOR_DATA_ATTEMPTS_NUM; attempt++ ) {
            VOCSensorData vocSensorData = vocSensor.get();
            if ( vocSensorData != null ) {
                return vocSensorData;
            }
        }
        
        return null;
    }
    
    // returns ID of module for specified sensor ID
    private static String getModuleId(String sensorId, Map<String, OsInfo> osInfoMap) {
        if ( osInfoMap.get(sensorId) != null ) {
            return osInfoMap.get(sensorId).getPrettyFormatedModuleId();
        }
        return "not-known";
    }
    
    // for specified sensor's data returns their equivalent MQTT form
    private static Map<String, List<String>> toMqttForm(
            Map<String, DataToPublish> dataFromSensorsMap
    ) {
        Map<String, List<String>> mqttAllSensorsData = new LinkedHashMap<>();
        
        // for each sensor's data
        for ( Map.Entry<String, DataToPublish> entry : dataFromSensorsMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            // mqtt data for 1 sensor
            List<String> mqttSensorData = new LinkedList<>();
            
            DeviceInfo sensorInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Preparing MQTT message for node: " + entry.getKey());
            
            DecimalFormat sensorDataFormat = new DecimalFormat("##.#");
            DataToPublish dataToPublish = entry.getValue();
            
            Integer rssi = dataToPublish.rssi;
            if ( rssi == null ) {
                rssi = RSSI_NOT_AVAILABLE;
            }
            
            String moduleId = getModuleId(entry.getKey(), osInfoMap);
            String nadr = entry.getKey();
            
            switch ( sensorInfo.getType().toLowerCase() ) {
                case "co2-t-h":
                    CO2SensorData co2SensorData = (CO2SensorData)dataToPublish.sensorData;
                    if ( co2SensorData == null ) {
                        System.out.println(
                            "No data received from device, check log for details "
                            + "about protronix uart data"
                        );
                        mqttAllSensorsData.put(entry.getKey(), null);
                        break;
                    }
                    
                    // packet id
                    pid++;
                    
                    String mqttDataCO2 = MqttFormatter
                                .formatCO2(
                                    String.valueOf(co2SensorData.getCo2()), 
                                    moduleId,
                                    nadr
                                );
                    String mqttDataTemperature = MqttFormatter
                                .formatTemperature(
                                    sensorDataFormat.format(co2SensorData.getTemperature()), 
                                    moduleId,
                                    nadr    
                                );
                    
                    String mqttDataHumidity = MqttFormatter
                                .formatHumidity(
                                    sensorDataFormat.format(co2SensorData.getHumidity()), 
                                    moduleId,
                                    nadr
                                );
                    
                    String mqttDataRssi = MqttFormatter
                                .formatRssi(
                                    sensorDataFormat.format(rssi), 
                                    moduleId,
                                    nadr
                                );
                    
                    mqttSensorData.add(mqttDataCO2);
                    mqttSensorData.add(mqttDataTemperature);
                    mqttSensorData.add(mqttDataHumidity);
                    mqttSensorData.add(mqttDataRssi);
                    
                    mqttAllSensorsData.put(entry.getKey(), mqttSensorData);
                break;

                case "voc-t-h":
                    VOCSensorData vocSensorData = (VOCSensorData)dataToPublish.sensorData;
                    if ( vocSensorData == null ) {
                        System.out.println(
                            "No data received from device, check log for details "
                            + "about protronix uart data"
                        );
                        mqttAllSensorsData.put(entry.getKey(), null);
                        break;
                    }
                    
                    // packet id
                    pid++;

                    String mqttDataVOC = MqttFormatter
                                .formatVOC(
                                    String.valueOf(vocSensorData.getVoc()), 
                                    moduleId,
                                    nadr
                                );
                    mqttDataTemperature = MqttFormatter
                                .formatTemperature(
                                    sensorDataFormat.format(vocSensorData.getTemperature()), 
                                    moduleId,
                                    nadr
                                );
                    
                    mqttDataHumidity = MqttFormatter
                                .formatHumidity(
                                    sensorDataFormat.format(vocSensorData.getHumidity()), 
                                    moduleId,
                                    nadr
                                );
                    
                    mqttDataRssi = MqttFormatter
                                .formatRssi(
                                    sensorDataFormat.format(rssi), 
                                    moduleId,
                                    nadr
                                );
                    
                    mqttSensorData.add(mqttDataVOC);
                    mqttSensorData.add(mqttDataTemperature);
                    mqttSensorData.add(mqttDataHumidity);
                    mqttSensorData.add(mqttDataRssi);

                    mqttAllSensorsData.put(entry.getKey(), mqttSensorData);
                break;

                default:
                    printMessageAndExit("Device type not supported:" + sensorInfo.getType());
                break;
            }                      
        }
        
        return mqttAllSensorsData;
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
                        mqttCommunicator.publish(
                                mqttTopics.getStdSensorsProtronix() + entry.getKey(), 
                                2, 
                                mqttData.getBytes()
                        );
                    } catch ( MqttException ex ) {
                        System.err.println("Error while publishing sync dpa message: " + ex);
                    }
                }
            } else {
                System.err.println("No data found for sensor: " + entry.getKey());
            }
        }
    }
    
    // publishes error messages to specified MQTT topics
    private static void mqttPublishErrors(int nodeId, MqttTopics mqttTopics, String errorMessage) {
        try {
            mqttCommunicator.publish(
                    mqttTopics.getStdSensorsProtronixErrors() + nodeId, 
                    2, 
                    errorMessage.getBytes()
            );
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
    private static void releaseResources() {
        if ( dpaSimply != null ) {
            dpaSimply.destroy();
        }
    }
}
