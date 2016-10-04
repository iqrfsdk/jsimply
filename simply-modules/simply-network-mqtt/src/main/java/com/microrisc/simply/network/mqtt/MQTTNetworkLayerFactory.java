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

import com.microrisc.simply.SimplyException;
import com.microrisc.simply.network.NetworkLayer;
import com.microrisc.simply.network.AbstractNetworkLayerFactory;
import com.microrisc.simply.network.NetworkConnectionStorage;
import org.apache.commons.configuration.Configuration;

/**
 * MQTT factory for creation of network layers, which are bound to MQTT broker.
 * <p>
 * Configuration items: <br>
 * - <b>networkLayer.type.mqtt.serverURI</b>: URI of mqtt broker
 * - <b>networkLayer.type.mqtt.clientId</b>: client id used for communication
* - <b>networkLayer.type.mqtt.remoteMAC</b>: mac address of remote server (eg. bridge-iqrf-mqtt)
 * 
 * @author Martin Strouhal
 */
public class MQTTNetworkLayerFactory 
extends AbstractNetworkLayerFactory<Configuration, NetworkLayer> {
    
    // network layer parameters
    private static class NetworkLayerParams {
        NetworkConnectionStorage connectionStorage;
        String serverURI;
        String clientId;
        String remoteMAC;
        
        NetworkLayerParams(NetworkConnectionStorage connectionStorage, 
                String serverURI, String clientId, String remoteMAC) 
        { 
            this.connectionStorage = connectionStorage;
            this.serverURI = serverURI;
            this.clientId = clientId;
            this.remoteMAC = remoteMAC;
        }
    }
    
    /**
     * @return network layer parameters encapsulation object
     */
    private NetworkLayerParams createNetworkLayerParams(
            NetworkConnectionStorage connectionStorage, Configuration configProps
    ) {
        String serverURI = configProps.getString("networkLayer.type.mqtt.serverURI");
        String clientId = configProps.getString("networkLayer.type.mqtt.clientId");
        String remoteMAC = configProps.getString("networkLayer.type.mqtt.remoteMAC");
        return new NetworkLayerParams(connectionStorage, serverURI, clientId, remoteMAC);
    }
    
    /**
     * Creates MQTT network layer - according to specified network layer
     * parameters and version.
     * @param networkParams network layer parameters
     * @return MQTT network layer
     */
    private MQTTNetworkLayer createMQTTNetworkLayer(NetworkLayerParams networkParams) 
            throws Exception {
        String serverURI = networkParams.serverURI;
        String clientId = networkParams.clientId;
        String remoteMAC = networkParams.remoteMAC;
        
        return new MQTTNetworkLayer(
                        networkParams.connectionStorage,
                        serverURI, clientId, remoteMAC
        );
    }
    
    @Override
    public NetworkLayer getNetworkLayer(NetworkConnectionStorage connectionStorage, 
            Configuration configProps
    ) throws Exception {
        String networkLayerType = configProps.getString("networkLayer.type");
        
        // only for "mqtt" layer type
        if ( !networkLayerType.equals("mqtt")) {
            throw new SimplyException("Network layer must be of 'mqtt' type.");
        }
        
        NetworkLayerParams networkParams = createNetworkLayerParams(connectionStorage, configProps);
        return createMQTTNetworkLayer(networkParams);
    }
}