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
        String defFileName = "write_config_service" + File.separator + "TR_config_2_00.xml";
        String userSettingsFileName = "write_config_service" + File.separator + "config.xml";
        HWP_ConfigurationByte[] result = XmlConfigurationParser.parse(defFileName, userSettingsFileName);
        
        HWP_ConfigurationByte[] expResult = new HWP_ConfigurationByte[12];
        expResult[0] = new HWP_ConfigurationByte(1, 0b11111000, 0b11111000);
        expResult[1] = new HWP_ConfigurationByte(2, 0b00100110, 0b00111111);
        expResult[2] = new HWP_ConfigurationByte(5, 0b00000011, 0b00111111);
        expResult[3] = new HWP_ConfigurationByte(6, 42, 0xFF);
        expResult[4] = new HWP_ConfigurationByte(8, 1, 0xFF);
        expResult[5] = new HWP_ConfigurationByte(9, 5, 0xFF);
        expResult[6] = new HWP_ConfigurationByte(10, 6, 0xFF);
        expResult[7] = new HWP_ConfigurationByte(11, 3, 0b00000111);
        expResult[8] = new HWP_ConfigurationByte(12, 0, 0xFF);
        expResult[9] = new HWP_ConfigurationByte(17, 52, 0xFF);
        expResult[10] = new HWP_ConfigurationByte(18, 5, 0xFF);
        expResult[11] = new HWP_ConfigurationByte(0x20, 0b11010101, 0b11010101);
        
        /*
        System.out.println("Configuration bytes: ");
        for ( HWP_ConfigurationByte configByte : result ) {
            System.out.println(configByte);
        }
        */
        
        assertEquals(expResult.length, result.length);
        assertArrayEquals(expResult, result);
    }
    
}
