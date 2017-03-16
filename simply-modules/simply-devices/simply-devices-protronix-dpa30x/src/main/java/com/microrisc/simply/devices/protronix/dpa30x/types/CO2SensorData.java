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

package com.microrisc.simply.devices.protronix.dpa30x.types;

/**
 * Data from CO2 sensor.
 * 
 * @author Michal Konopa
 */
public final class CO2SensorData {
    private final int co2;
    private final float temperature;
    private final float humidity;
    
    
    /**
     * Creates new object of CO2 Sensor data and initializes it according to
     * specified values of CO2 concentration, temperature and humidity.
     * 
     * @param co2 CO2 concentration [in ppm]
     * @param temperature temperature [in Celsius degree]
     * @param humidity relative humidity of air [in percent]
     */
    public CO2SensorData(int co2, float temperature, float humidity) {
        this.co2 = co2;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    /**
     * @return the concentration of co2 [in ppm] 
     */
    public int getCo2() {
        return co2;
    }

    /**
     * @return the temperature [in Celsius degree]
     */
    public float getTemperature() {
        return temperature;
    }

    /**
     * @return the humidity [in percent]
     */
    public float getHumidity() {
        return humidity;
    }
}
