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

package com.microrisc.simply.network.mqtt;

import com.microrisc.jlibiqrf.bridge.json.simple.SimpleJsonConvertor;
import com.microrisc.jlibiqrf.bridge.mqtt.PublishableMqttMessage;
import com.microrisc.simply.NetworkData;
import com.microrisc.simply.NetworkLayerListener;
import com.microrisc.simply.network.AbstractNetworkConnectionInfo;
import com.microrisc.simply.network.AbstractNetworkLayer;
import com.microrisc.simply.network.BaseNetworkData;
import com.microrisc.simply.network.NetworkConnectionStorage;
import com.microrisc.simply.network.NetworkLayerException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements network layer using {@code MqttClient} object.
 * <p>
 * This registers itself like an listener of MQTT broker. All data 
 * coming from mqtt interface are forwarder to user's registered network listener. 
 * All data designated to underlying network are published to MQTT broker.
 * 
 * @author Martin Strouhal
 */
public final class MQTTNetworkLayer
        extends AbstractNetworkLayer implements MqttCallback {
    
    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(MQTTNetworkLayer.class);
    
    /** Mqtt client instance. */
    private MqttClient mqttClient = null;

    /** Registered network listener. */
    private NetworkLayerListener networkListener = null;

    /** Mqtt connection info. */
    private BaseMQTTConnectionInfo connectionInfo=  null;
    
    private final String publishTopicName;
    private final String subscribeTopicName;
    
    private static NetworkConnectionStorage checkStorage(NetworkConnectionStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Network Connection Storage cannot "
                    + "be less null");
        }
        return storage;
    }

    private static String checkNull(String value, String name) {
        if ( value == null ) {
            throw new IllegalArgumentException(name + " cannot be null");
        }

        if ( value.equals("") ) {
            throw new IllegalArgumentException(name + " cannot be empty string");
        }
        return value;
    }
    
    /**
     * Creates MQTT network layer object.
     * @param connectionStorage storage of network connection
     * @param serverURI for access to the network
     * @param clientId for access to the network
     * @param mac address of remote server
     * @throws MqttException if some exception has occurred
     *         during creating of mqtt network layer
     */
    public MQTTNetworkLayer(NetworkConnectionStorage connectionStorage, 
            String serverURI, String clientId, String mac)throws MqttException, Exception 
    {
        super(checkStorage(connectionStorage));
        checkNull(serverURI, "ServerURI");
        checkNull(clientId, "Client ID");
        checkNull(mac, "MAC address of remote server");
                
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttClientPersistence persistence = new MqttDefaultFilePersistence(tmpDir);
        
        this.mqttClient = new MqttClient(serverURI, clientId, persistence);
        
        publishTopicName = "/gateway/" + mac + "/tx";
        subscribeTopicName = "/gateway/" + mac + "/rx";
        
        this.connectionInfo = new BaseMQTTConnectionInfo(serverURI, clientId, mac);
    }
   
    @Override
    public void registerListener(NetworkLayerListener listener) {
        this.networkListener = listener;
        log.info("Listener registered");
    }

    @Override
    public void unregisterListener() {
        networkListener = null;

        log.info("Listener unregistered");
    }

    /**
     * Starts receiving data from mqtt interface.
     */
    @Override
    public void start() throws NetworkLayerException {
        log.debug("start - start:");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
            
        try {
            mqttClient.connect();
            mqttClient.subscribe(subscribeTopicName);
            mqttClient.setCallback(this);
        } catch (MqttException ex) {
            log.error(ex.getMessage());
            throw new NetworkLayerException(ex.getMessage());
        }
           
        log.debug("start - end");
    }
    
    @Override
    public void sendData(NetworkData networkData) throws NetworkLayerException {
        log.debug("sendData - start: networkData: netId={}, netData={}", networkData.getNetworkId(), convertDataForLog(networkData.getData()));

        // get connection info for specified request
        AbstractNetworkConnectionInfo connInfo = connectionStorage.getNetworkConnectionInfo(
                networkData.getNetworkId()
        );

        // no connection info
        if ( connInfo == null ) {
            throw new NetworkLayerException(
                    "No connection info for network: " + networkData.getNetworkId()
            );
        }

        // check, if connection infos are equals
        if ( !(this.connectionInfo.equals(connInfo)) ) {
            throw new NetworkLayerException("Connection info mismatch."
                    + "Incomming: " + connInfo
                    + ", required: " + this.connectionInfo
            );
        }
        
        // check if it's mqtt connected
        if(!mqttClient.isConnected()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error("Mqtt client is disconnedted. Message cannot be send.");
                throw new NetworkLayerException("Mqtt client is disconnedted. Message cannot be send.");
            }
        }

        try {
            PublishableMqttMessage mqttMsg = SimpleJsonConvertor.getInstance().toJson(networkData.getData());
            mqttClient.publish(publishTopicName, mqttMsg);            
            
            // in case of socket error on server
            Thread.sleep(1000);
        } catch ( Exception ex ) {
            log.error(ex.getMessage());
            throw new NetworkLayerException(ex);
        }

        log.debug("sendData - end");
    }   

    @Override
    public void destroy() {
        log.debug("destroy - start: ");

        try {
            mqttClient.unsubscribe(subscribeTopicName);
            mqttClient.disconnect();
        } catch (MqttException ex) {
            log.warn(ex.getMessage());
       }
        
        mqttClient = null;
        connectionStorage = null;

        log.info("Destroyed MQTT layer");
        log.debug("destroy - end");
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.info("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("messageArrived - start: topic={}, message={}", topic, message);
        log.debug(new String(message.getPayload()) + " in " + topic);
        short[] data = SimpleJsonConvertor.getInstance().toIQRF(new String(message.getPayload())).getData();
        String networkId = connectionStorage.getNetworkId(connectionInfo);
        networkListener.onGetData(new BaseNetworkData(data, networkId));
        log.debug("messageArrived - end");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("delivery complete: " + token);
    }
    
    // coverts data to hex values for log
    private String convertDataForLog(short[] data) {
        
        String dataString = null;

        // converts to hex for print in log
        if (log.isDebugEnabled()) {
            
            dataString = "[";
            
            for (int i = 0; i < data.length; i++) {
                if (i != data.length - 1) {
                    dataString += String.format("%02X", data[i]) + ".";
                } else {
                    dataString += String.format("%02X", data[i]) + "]";
                }
            }
        }
        
        return dataString;
    }
}