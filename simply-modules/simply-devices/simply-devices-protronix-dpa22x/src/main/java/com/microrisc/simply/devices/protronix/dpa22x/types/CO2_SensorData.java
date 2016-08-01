
package com.microrisc.simply.iqrf.dpa.v22x.protronix.types;

/**
 * Data from CO2 sensor.
 * 
 * @author Michal Konopa
 */
public final class CO2_SensorData {
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
    public CO2_SensorData(int co2, float temperature, float humidity) {
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
