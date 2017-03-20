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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration;

/**
 * Configuration bytes to store into memory.
 * 
 * @author Michal Konopa
 */
public final class ConfigBytes {
    /** Length of configuration array [in bytes] */
    private static final int CONFIG_LEN = 31;
    
    private short checksum;
    private short[] configuration;
    private short rfpgm;
    
    
    /**
     * Creates new configuration bytes. All bytes all set to 0.
     */
    public ConfigBytes() {
        this.checksum = 0;
        this.configuration = new short[CONFIG_LEN];
        this.rfpgm = 0;
    }

    /**
     * @return the checksum
     */
    public short getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(short checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the configuration bytes
     */
    public short[] getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration bytes to set
     */
    public void setConfiguration(short[] configuration) {
        if ( configuration.length != CONFIG_LEN ) {
            throw new IllegalArgumentException(
                    "Configuration must be " + CONFIG_LEN + " bytes long."
            );
        }
        
        System.arraycopy(configuration, 0, this.configuration, 0, CONFIG_LEN);
    }

    /**
     * @return the rfpgm
     */
    public short getRfpgm() {
        return rfpgm;
    }

    /**
     * @param rfpgm the rfpgm to set
     */
    public void setRfpgm(short rfpgm) {
        this.rfpgm = rfpgm;
    }
    
    
}
