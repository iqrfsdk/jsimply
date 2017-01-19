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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code;

import com.microrisc.simply.BaseNode;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.iqrf.dpa.v22x.devices.EEEPROM;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadResult;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.ServiceResult;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for Simple Load Code Service.
 * 
 * @author Michal Konopa
 */
public class SimpleLoadCodeServiceTest {
    
    public SimpleLoadCodeServiceTest() {
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
     * Unicast ok.
     */
    @Test
    public void unicastOk() {
        TestingOs os = new TestingOs("1", "1");
        os.setBatchReturnValue( new VoidType() );
        os.setLoadCodeReturnValue( new LoadResult(true));
        
        TestingEEEPROM eeeprom = new TestingEEEPROM("1", "1");
        eeeprom.setExtendedWriteReturnValue(new VoidType());
        
        Map<Class, DeviceObject> deviceObjects = new HashMap<>();
        deviceObjects.put(OS.class, (DeviceObject)os);
        deviceObjects.put(EEEPROM.class, (DeviceObject)eeeprom);
        
        LoadCodeService loadCodeService 
                = new SimpleLoadCodeService( new BaseNode("1", "1", deviceObjects));
        
        String sourceFile = "test" + File.separator + "resources" + File.separator 
                +  "load_code_service" + File.separator 
                + "CustomDpaHandler-LED-Green-On-7xD-V300-161122.hex";
        LoadCodeServiceParameters params
                = new LoadCodeServiceParameters(sourceFile, 
                        0x0000,
                        LoadingCodeProperties.LoadingAction.ComputeAndMatchChecksumWithoutCodeLoading,
                        LoadingCodeProperties.LoadingContent.Hex
                );
        
        ServiceResult<LoadCodeResult, LoadCodeProcessingInfo> serviceResult 
                = loadCodeService.loadCode(params);
        
        assertEquals(ServiceResult.Status.SUCCESSFULLY_COMPLETED, serviceResult.getStatus());
        assertNull(serviceResult.getProcessingInfo().getError());
        
        LoadCodeResult loadResult = serviceResult.getResult();
        Map<String, Boolean> loadResultsMap = loadResult.getAllNodeResultsMap();
        assertNotNull(loadResultsMap);
        assertEquals(loadResultsMap.size(), 1);
        
        Boolean nodeResult = loadResult.getNodeResult("1");
        assertNotNull(nodeResult);
        assertEquals(true, nodeResult);
    }

}
