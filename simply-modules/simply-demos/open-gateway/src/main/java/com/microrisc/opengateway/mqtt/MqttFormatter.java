/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway.mqtt;

/**
 * Formats various types od sensor data to MQTT form.
 *
 * @author Michal Konopa
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
     * Returns formated value of humidity.
     *
     * @param error message
     * @return formated error message
     */
    public static String formatError(String error) {
        return "{\"e\":["
                + "{\"n\":\"error\"," + "\"u\":\"description\"," + "\"v\":" + error + "}"
                + "]}";
    }
}
