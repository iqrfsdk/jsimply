
package com.microrisc.simply.devices.protronix.dpa22x.types;

/**
 * Data from VOC sensor.
 * 
 * @author Michal Konopa
 */
public final class VOCSensorData {
    private final int voc;
    private final float temperature;
    private final float humidity;
    
    
    /**
     * Creates new object of VOC Sensor data and initializes it according to
     * specified values of VOC concentration, temperature and humidity.
     * 
     * @param voc VOC concentration [in ppm]
     * @param temperature temperature [in Celsius degree]
     * @param humidity relative humidity of air [in percent]
     */
    public VOCSensorData(int voc, float temperature, float humidity) {
        this.voc = voc;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    /**
     * @return the concentration of voc [in ppm] 
     */
    public int getVoc() {
        return voc;
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
