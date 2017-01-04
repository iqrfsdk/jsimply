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

package com.microrisc.simply.demos.mqtt;

/**
 * Formats various types od sensor data to MQTT form.
 *
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public final class MqttFormatter {
    
    /**
     * Returns formated value of CO2.
     *
     * @param co2 CO2 value
     * @param moduleId ID of source module
     * @return formated value of CO2
     */
    public static String formatCO2(String co2, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"co2\"," + "\"u\":\"PPM\"," + "\"v\":" + co2 + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of VOC.
     *
     * @param voc VOC value
     * @param moduleId ID of source module
     * @return formated value of VOC
     */
    public static String formatVOC(String voc, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"voc\"," + "\"u\":\"PPM\"," + "\"v\":" + voc + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of temperature.
     *
     * @param temperature temperature
     * @param moduleId ID of source module
     * @return formated value of temperature
     */
    public static String formatTemperature(String temperature, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of humidity.
     *
     * @param humidity humidity
     * @param moduleId ID of source module
     * @return formated value of humidity
     */
    public static String formatHumidity(String humidity, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of RSSI.
     *
     * @param rssi RSSI
     * @param moduleId ID of source module
     * @return formated value of RSSI
     */
    public static String formatRssi(String rssi, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"rssi\"," + "\"u\":\"dBm\"," + "\"v\":" + rssi + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }
    
    
    /**
     * Returns formated value of specified error string.
     *
     * @param error error message
     * @return formated error message
     */
    public static String formatError(String error) {
        return "{\"e\":["
                + "{\"n\":\"error\"," + "\"u\":\"description\"," + "\"v\":" + "\"" + error + "\"}"
                + "]}";
    }
}
