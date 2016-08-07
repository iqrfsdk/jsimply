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

package com.microrisc.opengateway.mqtt;

import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * MQTT topics.
 * 
 * @author Rostislav Spinar
 */
public final class MqttTopics {
    
    private final String clientId;
    private final String stdSensorsProtronix; ;
    private final String stdSensorsProtronixErrors;

    
    /**
     * Creates new object of MQTT topics.
     * @param clientId cliend ID
     * @param stdSensorsProtronix
     * @param stdSensorsProtronixErrors 
     */
    public MqttTopics(String clientId, String stdSensorsProtronix, String stdSensorsProtronixErrors) 
    {
        this.clientId = clientId;
        this.stdSensorsProtronix = stdSensorsProtronix;
        this.stdSensorsProtronixErrors = stdSensorsProtronixErrors;
    }
    
    /**
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the std sensors Protronix
     */
    public String getStdSensorsProtronix() {
        return stdSensorsProtronix;
    }
    
    /**
     * @return the std sensors Protronix
     */
    public String getStdSensorsProtronixErrors() {
        return stdSensorsProtronixErrors;
    }
    
    private static String getMac() {
        String interfaceName = "eth0";
        //InetAddress ip;
        try {
            //ip = InetAddress.getLocalHost();
            //System.out.println("Current IP address : " + ip.getHostAddress());

            NetworkInterface network = NetworkInterface.getByName(interfaceName);

            byte[] mac = network.getHardwareAddress();
            System.out.print("Current MAC address : ");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02x", mac[i]));
            }
            System.out.println(sb.toString());
            
            return sb.toString();
            
        //} catch (UnknownHostException e) {
        //    e.printStackTrace();
        //    return "";
        } catch (SocketException e) {
            e.printStackTrace();
            return "";
        }
    }
}
