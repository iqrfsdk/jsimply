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

package com.microrisc.simply.tcpcloud.mqtt;

/**
 * Formats various types od sensor data to MQTT form.
 *
 * @author Michal Konopa
 */
public final class MqttFormatter {
    
    /**
     * Returns formated value of specified counted objects.
     *
     * @param countedObjectsName name of counted objects
     * @param countedObjectsNum number of counted objects
     * @param moduleId ID of source module
     * @return formated value of counted objects
     */
    public static String formatCountedObjectsNum(
            String countedObjectsName, String countedObjectsNum, String moduleId
    ) {
        return 
            "{\"e\":["
            + "{\"n\":\"" +countedObjectsName + " \"," + "\"u\":\"persons\"," + "\"v\":" + countedObjectsNum + "}"
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
                + "{\"n\":\"error\"," + "\"u\":\"description\"," + "\"v\":" + error + "}"
                + "]}";
    }
}
