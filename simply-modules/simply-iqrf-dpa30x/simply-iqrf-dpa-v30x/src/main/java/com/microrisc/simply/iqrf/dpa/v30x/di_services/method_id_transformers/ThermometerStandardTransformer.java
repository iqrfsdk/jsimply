/* 
 * Copyright 2014 MICRORISC s.r.o.
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

package com.microrisc.simply.iqrf.dpa.v30x.di_services.method_id_transformers;

import com.microrisc.simply.di_services.MethodIdTransformer;
import com.microrisc.simply.iqrf.dpa.v30x.devices.Thermometer;
import java.util.EnumMap;
import java.util.Map;

/**
 * Standard method ID transformer for Thermometer. 
 * 
 * @author Michal Konopa
 */
public final class ThermometerStandardTransformer implements MethodIdTransformer {
    /**
     * Mapping of method IDs to theirs string representations.
     */
    private static final Map<Thermometer.MethodID, String> methodIdsMap = 
            new EnumMap<Thermometer.MethodID, String>(Thermometer.MethodID.class);
    
    private static void initMethodIdsMap() {
        methodIdsMap.put(Thermometer.MethodID.GET, "1");
    }
    
    static  {
        initMethodIdsMap();
    }
    
    /** Singleton. */
    private static final ThermometerStandardTransformer instance = 
            new ThermometerStandardTransformer();
    
    
    /**
     * @return ThermometerStandardTransformer instance 
     */
    static public ThermometerStandardTransformer getInstance() {
        return instance;
    }
    
    @Override
    public String transform(Object methodId) {
        if ( !(methodId instanceof Thermometer.MethodID) ) {
            throw new IllegalArgumentException(
                    "Method ID must be of type Thermometer.MethodID."
            );
        }
        return methodIdsMap.get((Thermometer.MethodID) methodId);
    }
    
}
