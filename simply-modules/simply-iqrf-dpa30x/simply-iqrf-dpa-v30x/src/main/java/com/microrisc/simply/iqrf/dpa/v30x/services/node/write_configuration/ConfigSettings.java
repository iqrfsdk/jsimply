/*
 * Copyright 2017 MICRORISC s.r.o.
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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration;

import com.microrisc.simply.iqrf.dpa.v30x.types.HWP_ConfigurationByte;

/**
 * Configuration settings to write into a module.
 * 
 * @author Michal Konopa
 */
public final class ConfigSettings {
    
    /**
     * Security settings.
     */
    public static final class Security {
        
        // password
        private final short[] password;
        
        // key
        private final short[] key;
       
        
        /**
         * Creates new object of security settings.
         * 
         * @param password password
         * @param key key
         */
        public Security(short[] password, short[] key) {
            this.password = password;
            this.key = key;
        }

        /**
         * @return the password
         */
        public short[] getPassword() {
            return password;
        }

        /**
         * @return the key
         */
        public short[] getKey() {
            return key;
        }
        
    }
    
    private final HWP_ConfigurationByte[] hwpConfigBytes;
    private final Security security;
    
    
    /**
     * Creates new object of configuration settings.
     * 
     * @param hwpConfigBytes HWP bytes
     * @param security security
     */
    public ConfigSettings(HWP_ConfigurationByte[] hwpConfigBytes, Security security) {
        this.hwpConfigBytes = hwpConfigBytes;
        this.security = security;
    }

    /**
     * @return HWP configuration bytes 
     */
    public HWP_ConfigurationByte[] getHwpConfigBytes() {
        return hwpConfigBytes;
    }

    /**
     * @return the security
     */
    public Security getSecurity() {
        return security;
    }
    
}
