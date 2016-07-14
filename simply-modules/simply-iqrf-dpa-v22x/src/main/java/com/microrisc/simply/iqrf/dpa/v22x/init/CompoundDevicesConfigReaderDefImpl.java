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
package com.microrisc.simply.iqrf.dpa.v22x.init;

import com.microrisc.simply.iqrf.dpa.v22x.CompoundDeviceObjectFactory;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Default implementation of {@link CompoundDevicesConfigReader}.
 * 
 * @author Michal Konopa
 */
public final class CompoundDevicesConfigReaderDefImpl 
implements CompoundDevicesConfigReader 
{
    
    // return implementing class Class object
    private Class getImplClass(HierarchicalConfiguration deviceConfig) {
        String implClassName = deviceConfig.getString("implClass", "");
        if ( implClassName.isEmpty() ) {
            throw new RuntimeException("Implementation class not specified.");
        }
        
        try {
            Class implClass = Class.forName(implClassName);
            return implClass;
        } catch ( ClassNotFoundException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    // return factory class name
    private String getFactoryClassName(HierarchicalConfiguration deviceConfig) {
        String factoryClassName = deviceConfig.getString("factory.class", "");
        if ( factoryClassName.isEmpty() ) {
            throw new RuntimeException("Factory class not specified.");
        }
        return factoryClassName;
    }
    
    // returns list of Device Iterface of internal devices
    private List<Class> getDevIfacesOfInternalDevices(HierarchicalConfiguration deviceConfig) {
        List<Class> devIfacesOfInternalDevices = new LinkedList<>();
        
        List<HierarchicalConfiguration> intDevicesConfig = deviceConfig.configurationsAt("internalDevice");
        for ( HierarchicalConfiguration intDeviceConfig : intDevicesConfig ) {
            String devIfaceClassName = intDeviceConfig.getString("interface", "");
            if ( devIfaceClassName.isEmpty() ) {
                throw new RuntimeException("Device Interface of internal device is missing.");
            }
            
            try {
                Class devIfaceClass = Class.forName(devIfaceClassName);
                devIfacesOfInternalDevices.add(devIfaceClass);
            } catch ( ClassNotFoundException ex ) {
                throw new RuntimeException(ex);
            }
        }
        
        return devIfacesOfInternalDevices;
    }
    
    // returns Component Device Object Factory object
    private CompoundDeviceObjectFactory getFactoryObject(String factoryClassName) {
        try {
            Class factoryClass = Class.forName(factoryClassName);
            Constructor constructor = factoryClass.getConstructor();
            return (CompoundDeviceObjectFactory)constructor.newInstance();
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public CompoundDevicesConfiguration read(Configuration configuration) {
        String compoundDevicesConfigFile = configuration.getString(
                "initialization.compoundDevices.configFile",
                ""
        );
        
        // if config file is missing
        if ( compoundDevicesConfigFile.isEmpty() ) {
            throw new RuntimeException("Specification of configuration file is missing.");
        }
        
        XMLConfiguration compoundDeviceConfig = null;
        try {
            compoundDeviceConfig = new XMLConfiguration(compoundDevicesConfigFile);
        } catch ( ConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
        
        // for reusing created factory objects
        Map<String, CompoundDeviceObjectFactory> factoryMap = new HashMap<>();
        
        // list of CD configurations 
        List<CompoundDeviceConfiguration> compoundDeviceConfigList = new LinkedList<>();
        
        List<HierarchicalConfiguration> networksConfig = compoundDeviceConfig.configurationsAt("network");
        for ( HierarchicalConfiguration networkConfig : networksConfig ) {
            String networkId = networkConfig.getString("[@id]");
            if ( networkId == null ) {
                throw new RuntimeException("ID of network not specified."); 
            }
            
            List<HierarchicalConfiguration> nodesConfig = networkConfig.configurationsAt("node");
            for ( HierarchicalConfiguration nodeConfig : nodesConfig ) {
                String nodeId = nodeConfig.getString("[@id]");
                if ( nodeId == null ) {
                    throw new RuntimeException("ID of node not specified."); 
                }
                
                List<HierarchicalConfiguration> devicesConfig = nodeConfig.configurationsAt("device");
                for ( HierarchicalConfiguration deviceConfig : devicesConfig ) {
                    Class implClass = getImplClass(deviceConfig);
                    String factoryClassName = getFactoryClassName(deviceConfig);
                    
                    if ( !factoryMap.containsKey(factoryClassName) ) {
                        factoryMap.put(factoryClassName, getFactoryObject(factoryClassName));
                    }
                    
                    List<Class> devIfacesOfInternalClasses = getDevIfacesOfInternalDevices(deviceConfig);
                    
                    // creating result CD configuration and adding it into list
                    compoundDeviceConfigList.add( 
                            new CompoundDeviceConfiguration(
                                    networkId, nodeId, implClass, devIfacesOfInternalClasses,
                                    new BaseConfiguration(), factoryMap.get(factoryClassName)
                            )
                    );
                }
            }
        }
        
        return new CompoundDevicesConfigurationDefImpl(compoundDeviceConfigList);
    }
    
}
