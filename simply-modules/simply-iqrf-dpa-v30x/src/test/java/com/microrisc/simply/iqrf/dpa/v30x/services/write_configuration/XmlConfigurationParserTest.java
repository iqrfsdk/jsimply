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
package com.microrisc.simply.iqrf.dpa.v30x.services.write_configuration;

import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.ConfigSettings;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.ConfigSettings.Security;
import com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.XmlConfigurationParser;
import com.microrisc.simply.iqrf.dpa.v30x.types.HWP_ConfigurationByte;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test of XML Configuration parser.
 * 
 * @author Michal Konopa
 */
public class XmlConfigurationParserTest {
    
    public XmlConfigurationParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class XmlConfigurationParser.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        String defFileName = "write_config_service" + File.separator + "TR_config_3_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        
        ConfigSettings configSettings = XmlConfigurationParser.parse(defFileName, userSettingsFileName);
        assertNotNull(configSettings);
        
        HWP_ConfigurationByte[] configBytes = configSettings.getHwpConfigBytes();
        assertNotNull(configBytes);
        
        HWP_ConfigurationByte[] expConfigBytes = new HWP_ConfigurationByte[12];
        expConfigBytes[0] = new HWP_ConfigurationByte(1, 0b11111000, 0b11111000);
        expConfigBytes[1] = new HWP_ConfigurationByte(2, 0b00000010, 0b00110111);
        expConfigBytes[2] = new HWP_ConfigurationByte(5, 0b00000000, 0b00111111);
        expConfigBytes[3] = new HWP_ConfigurationByte(6, 42, 0xFF);
        expConfigBytes[4] = new HWP_ConfigurationByte(8, 7, 0xFF);
        expConfigBytes[5] = new HWP_ConfigurationByte(9, 0, 0xFF);
        expConfigBytes[6] = new HWP_ConfigurationByte(10, 6, 0xFF);
        expConfigBytes[7] = new HWP_ConfigurationByte(11, 6, 0b00000111);
        expConfigBytes[8] = new HWP_ConfigurationByte(12, 0, 0xFF);
        expConfigBytes[9] = new HWP_ConfigurationByte(17, 52, 0xFF);
        expConfigBytes[10] = new HWP_ConfigurationByte(18, 2, 0xFF);
        expConfigBytes[11] = new HWP_ConfigurationByte(0x20, 0b10000011, 0b11010111);
        
        /*
        System.out.println("Configuration bytes: ");
        for ( HWP_ConfigurationByte configByte : configBytes ) {
            System.out.println(configByte);
        }
        */
        
        assertEquals(expConfigBytes.length, configBytes.length);
        assertArrayEquals(expConfigBytes, configBytes);
        
        // checking of security attributes
        Security security = configSettings.getSecurity();
        assertNotNull(security);
        
        short[] password = security.getPassword();
        assertNotNull(password);
        
        short[] expPassword = new short[6];
        expPassword[0] = (short)98;
        expPassword[1] = 108;
        expPassword[2] = 97;
        expPassword[3] = 98;
        expPassword[4] = 108;
        expPassword[5] = 97;
 
        assertArrayEquals(expPassword, password);
        
        short[] key = security.getKey();
        assertNotNull(key);
        
        short[] expKey = new short[3];
        expKey[0] = 0x10;
        expKey[1] = 0xAF;
        expKey[2] = 0xB0;
        
        assertArrayEquals(expKey, key);
        
    }
    
}
