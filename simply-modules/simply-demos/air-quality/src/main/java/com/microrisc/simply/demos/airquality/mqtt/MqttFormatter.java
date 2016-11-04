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

package com.microrisc.simply.demos.airquality.mqtt;

/**
 * Formats various types od sensor data to MQTT form.
 *
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public final class MqttFormatter {
    
/**
     * Returns formated value of Protronix response message.
     *
     * @param nodeId
     * @param clientId
     * @param co2
     * @param temperature
     * @param humidity
     * 
     * @return formated MQTT message
     */
    public static String formatDeviceProtronix(int nodeId, String clientId, String co2, String temperature, String humidity) {

        String timeSec = Long.toString( System.currentTimeMillis() / 1000);
        String timeMsec = Long.toString( System.currentTimeMillis() % 1000);

        return "["
                + "{\"bn\":" + "\"urn:clid:" + clientId + ":ba:" + nodeId + "\"," + "\"bt\":" + timeSec + "." + timeMsec + "},"
                + "{\"n\":\"co2\"," + "\"u\":\"ppm\"," + "\"v\":" + co2 + "},"
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "},"
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "}"
                + "]";
    }
    
    /**
     * Returns formated value of specified error string.
     *
     * @param error error message
     * @return formated error message
     */
    public static String formatError(String error) {
        return "{\"e\":["
                + "{\"n\":\"error\"," + "\"u\":\"description\"," + "\"v\":" + error + "}"
                + "]}";
    }
}
