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

package com.microrisc.simply.devices.protronix.dpa30x.utils;

/**
 * Modbus CRC setter.
 * 
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public final class ModbusCRCSetter {
    
    // calculates CRC and returns it
    private static int calculateCrc(short[] dataIn) {
        int crc = 0xFFFF;

        for (int i = 0; i < dataIn.length - 2; i++) {
            crc ^= (dataIn[i] & 0xFF);

            for (int j = 0; j < 8; j++) {
                boolean bitOne = ((crc & 0x01) == 0x01);
                crc >>>= 1;
                if (bitOne) {
                    crc ^= 0x0000A001;
                }
            }
        }

        //System.out.println("CRC: " + Integer.toHexString(crc & 0xFFFF));
        return crc;
    }
    
    /**
     * Sets last 2 bytes of specified data according to calculated CRC.
     * @param data data on which will be the calculated CRC set
     * @return data set according to calculated CRC
     */
    public static short[] set(short[] data) {
        int crc = calculateCrc(data);

        data[data.length - 2] = (short) (crc & 0xFF);
        data[data.length - 1] = (short) ((crc & 0xFF00) >> 8);

        return data;
    }
}
