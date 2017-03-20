
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
package com.microrisc.simply.dustbin.mqtt;

/**
 * Formats dustbin data to MQTT form.
 *
 * @author Michal Konopa
 */
public final class MqttFormatter {
    
    /**
     * Returns MQTT formatted value of specified dustbin data.
     * 
     * @param clientId client ID
     * @param nodeId ID of source node
     * @param data dustbin data
     * @return formated MQTT message
     */
    public static String formatDustbinData(String clientId, String nodeId, short[] data) {
        String timeSec = Long.toString( System.currentTimeMillis() / 1000);
        String timeMsec = Long.toString( System.currentTimeMillis() % 1000);
        
        return
            "["
            + "{\"bn\":" + "\"urn:clid:" + clientId + ":ba:" + nodeId + "\"," + "\"bt\":" + timeSec + "." + timeMsec + "},"
            + "{\"n\":\"dustbin\"," + "\"u\":\"bytes\"," + "\"v\":" + data + "}"
            + "]";
    }
}
