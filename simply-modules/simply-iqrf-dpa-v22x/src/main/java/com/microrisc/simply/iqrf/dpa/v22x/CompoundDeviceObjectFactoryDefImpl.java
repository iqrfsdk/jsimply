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
package com.microrisc.simply.iqrf.dpa.v22x;

import com.microrisc.simply.DeviceObject;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CompoundDeviceObjectFactory}.
 * 
 * @author Michal Konopa
 */
public final class CompoundDeviceObjectFactoryDefImpl
implements CompoundDeviceObjectFactory 
{
    
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(CompoundDeviceObjectFactoryDefImpl.class);
    
    
    /**
     * Creates new object of CDO factory default implementation.
     */
    public CompoundDeviceObjectFactoryDefImpl() {
    }
    
    /**
     * Implementation class {@code implClass} must be subclass ( direct or indirect )
     * of {@link CompoundDeviceObject} class else {@code IllegalArgumentException} is
     * thrown.
     * @throws IllegalArgumentException if {@code implClass} is not direct or 
     *         indirect subclass of {@link CompoundDeviceObject} class.
     */
    @Override
    public CompoundDeviceObject getCompoundDeviceObject(
        String networkId, String nodeId, Class implClass, 
        List<DeviceObject> internalDevices, Configuration configuration
    ) {
        Object[] logArgs = new Object[5];
        logArgs[0] = networkId;
        logArgs[1] = nodeId;
        logArgs[2] = implClass;
        logArgs[3] = internalDevices;
        logArgs[4] = configuration;
        logger.debug(
                "getCompoundDeviceObject - start: networkId={}, nodeId={}, "
                + "implClass={}, internalDevices={}, configuration={}",
                logArgs
        );
        
        // implementation class must be subclass of CompoundDeviceObject
        if ( !(CompoundDeviceObject.class.isAssignableFrom(implClass)) ) {
            throw new IllegalArgumentException(
                    "Implementation class " + implClass.getName() + 
                    "is not subclass of " + CompoundDeviceObject.class.getName() 
            );
        }
        
        // parameter types of the constructor
        Class[] paramsTypes = new Class[] { String.class, String.class, DeviceObject[].class };

        // find the constructor
        java.lang.reflect.Constructor constructor = null;
        try {
            constructor = implClass.getConstructor(paramsTypes);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
        
        // arguments for the constructor
        Object[] args = new Object[] { networkId, nodeId, internalDevices.toArray( new DeviceObject[]{} ) };
        CompoundDeviceObject compoundDevObj = null;
        try {
            compoundDevObj = (CompoundDeviceObject)constructor.newInstance(args);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
        
        logger.debug("getCompoundDeviceObject - end: {}", compoundDevObj);
        return compoundDevObj;
    }
    
}
