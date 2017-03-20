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

import com.microrisc.simply.iqrf.dpa.v22x.types.HWP_ConfigurationByte;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser of XML configuration files.
 * 
 * @author Michal Konopa
 */
public final class XmlConfigurationParser {
    
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(XmlConfigurationParser.class);
    
    // EEPROM config. memory
    private static final int EEPROM_MEMORY = 0;
    
    // address within EEPROM memory for storing RFPGM data
    private static final int RFPGM_CONFIG_ADDRESS = 7;
    
    
    // base class of all configuration items
    private static class ConfigItem {
        protected final String id;
        protected final String name;
        
        public ConfigItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    
    // adressable config item - intended for direct writing into config. memory 
    private static interface AddressableConfigItem {
        int getMemory();
        int getAddress();
    }
    
    private static final class ConfigBit 
            extends ConfigItem implements AddressableConfigItem 
    {
        private final int memory;
        private final int address;
        private final int bitIndex;
        private final boolean value;
        
        public ConfigBit(
                String id, String name, int memory, int address, int bitIndex,
                boolean value
        ) {
            super(id, name);
            this.memory = memory;
            this.address = address;
            this.bitIndex = bitIndex;
            this.value = value;
        }

        @Override
        public int getMemory() {
            return memory;
        }

        @Override
        public int getAddress() {
            return address;
        }
    } 
    
    private static final class ConfigBits 
            extends ConfigItem implements AddressableConfigItem 
    {
        private final int memory;
        private final int address;
        private final int mask;
        private final int value;
        
        public ConfigBits(
                String id, String name, int memory, int address, int mask,
                int value
        ) {
            super(id, name);
            this.memory = memory;
            this.address = address;
            this.mask = mask;
            this.value = value;
        }
        
        @Override
        public int getMemory() {
            return memory;
        }

        @Override
        public int getAddress() {
            return address;
        }
    } 
    
    private static final class ConfigBitList extends ConfigItem {
        private final Map<String, ConfigBit> configBitMap;
        
        public ConfigBitList(String id, String name, Map<String, ConfigBit> configBitMap) {
            super(id, name);
            this.configBitMap = configBitMap;
        }
    }
    
    private static final class ConfigByte 
            extends ConfigItem implements AddressableConfigItem 
    {
        private final int memory;
        private final int address;
        private final int min;
        private final int max;
        private final int value;
        
        public ConfigByte(
                String id, String name, int memory, int address, int min, int max,
                int value
        ) {
            super(id, name);
            this.memory = memory;
            this.address = address;
            this.min = min;
            this.max = max;
            this.value = value;
        }
        
        @Override
        public int getMemory() {
            return memory;
        }

        @Override
        public int getAddress() {
            return address;
        }
    }
    
    private static final class ConfigGroup {
        private final String name;
        private final Map<String, ConfigItem> configItems;
        
        public ConfigGroup(String name, Map<String, ConfigItem> configItems) {
            this.name = name;
            this.configItems = configItems;
        }
    }
    
    
    // FOR HANDLING USER SETTINGS
    
    private static final class ConfigBitSetting extends ConfigItem {
        private final boolean value;
        
        public ConfigBitSetting(String id, String name, boolean value) {
            super(id, name);
            this.value = value;
        }
    }
    
    private static final class ConfigByteSetting extends ConfigItem {
        private final int value;
        
        public ConfigByteSetting(String id, String name, int value) {
            super(id, name);
            this.value = value;
        }
    }
    
    private static final class ConfigBitsSetting extends ConfigItem {
        private final int value;
        
        public ConfigBitsSetting(String id, String name, int value) {
            super(id, name);
            this.value = value;
        }
    }
    
    
    // HANDLING RULES
    private static final class Rule {
        private final String name;
        private final List<String> ids;
        private final String message;
        
        public Rule(String name, List<String> ids, String message) {
            this.name = name;
            this.ids = ids;
            this.message = message;
        }
    }
    
    
    // reads ConfigByte from Configuration Definition  file
    private static ConfigByte getConfigByte(HierarchicalConfiguration configByteXml) 
        throws XmlConfigurationParserException 
    {
        String id = configByteXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigByte is missing");
        }
        
        String name = configByteXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigByte is missing. ID = " + id);
        }
        
        int memory = -1;
        int address = -1;
        int min = -1;
        int max = -1;
        int value = -1;
        
        try {
            memory = configByteXml.getInt("[@Memory]", -1);
            address = configByteXml.getInt("[@Address]", -1);
            min = configByteXml.getInt("[@Min]", -1);
            max = configByteXml.getInt("[@Max]", -1);
            value = configByteXml.getInt("[@Value]", -1);
        } catch ( Exception ex ) {
            throw new XmlConfigurationParserException("Error in value parsing ConfigByte. ID = " + id);
        }
        
        if ( memory == -1 ) {
            throw new XmlConfigurationParserException("Missing Memory in ConfigByte. ID = " + id);
        }

        if ( address == -1 ) {
            throw new XmlConfigurationParserException("Missing Address in ConfigByte. ID = " + id);
        }
        
        if ( min == -1 ) {
            throw new XmlConfigurationParserException("Missing Min in ConfigByte. ID = " +  id);
        }
        
        if ( max == -1 ) {
            throw new XmlConfigurationParserException("Missing Max in ConfigByte. ID = " +  id);
        }
        
        if ( value == -1 ) {
            throw new XmlConfigurationParserException("Missing Value in ConfigByte. ID = " +  id);
        }
        
        return new ConfigByte(id, name, memory, address, min, max, value);
    }
    
    // reads ConfigBits from Configuration Definition  file
    private static ConfigBits getConfigBits(HierarchicalConfiguration configBitsXml) 
        throws XmlConfigurationParserException 
    {
        String id = configBitsXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigBits is missing");
        }
        
        String name = configBitsXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigBits is missing. ID = " + id);
        }
        
        int memory = -1;
        int address = -1;
        int mask = -1;
        int value = -1;
        
        try {
            memory = configBitsXml.getInt("[@Memory]", -1);
            address = configBitsXml.getInt("[@Address]", -1);
            mask = configBitsXml.getInt("[@Mask]", -1);
            value = configBitsXml.getInt("[@Value]", -1);
        } catch ( Exception ex ) {
            throw new XmlConfigurationParserException("Error in value parsing ConfigBits. ID = " + id);
        }
        
        if ( memory == -1 ) {
            throw new XmlConfigurationParserException("Missing Memory in ConfigBits. ID = " + id);
        }

        if ( address == -1 ) {
            throw new XmlConfigurationParserException("Missing Address in ConfigBits. ID = " + id);
        }
        
        if ( mask == -1 ) {
            throw new XmlConfigurationParserException("Missing Mask in ConfigByte. ID = " +  id);
        }
        
        if ( value == -1 ) {
            throw new XmlConfigurationParserException("Missing Value in ConfigBits. ID = " +  id);
        }
        
        return new ConfigBits(id, name, memory, address, mask, value);
    }
    
    // reads ConfigBit from Configuration Definition file
    private static ConfigBit getConfigBit(HierarchicalConfiguration configBitXml) 
            throws XmlConfigurationParserException 
    {
        String id = configBitXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigBit is missing");
        }
        
        String name = configBitXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigBit is missing. ID = " + id);
        }
        
        int memory = -1;
        int address = -1;
        int bitIndex = -1;
        
        try {
            memory = configBitXml.getInt("[@Memory]", -1);
            address = configBitXml.getInt("[@Address]", -1);
            bitIndex = configBitXml.getInt("[@BitIndex]", -1);
        } catch ( Exception ex ) {
            throw new XmlConfigurationParserException("Error in value parsing ConfigBit. ID = " + id);
        }
        
        if ( memory == -1 ) {
            throw new XmlConfigurationParserException("Missing Memory in ConfigBit. ID = " + id);
        }

        if ( address == -1 ) {
            throw new XmlConfigurationParserException("Missing Address in ConfigBit. ID = " + id);
        }
        
        if ( bitIndex == -1 ) {
            throw new XmlConfigurationParserException("Missing BitIndex in ConfigBit. ID = " +  id);
        }

        String valueStr = configBitXml.getString("[@Value]", "").toUpperCase();
        if ( valueStr.isEmpty() ) {
            throw new XmlConfigurationParserException("Missing Value in ConfigBit. ID = " + id);
        }

        boolean value = false;
        switch ( valueStr ) {
            case "TRUE": 
                value = true;
                break;
            case "FALSE":
                value = false;
                break;
            default:
                throw new XmlConfigurationParserException("Unknown value of ConfigBit. ID =  " + id);
        }
        
        return new ConfigBit(id, name, memory, address, bitIndex, value);
    }
    
    // reads ConfigBitList from Configuration Definition file
    private static ConfigBitList getConfigBitList(HierarchicalConfiguration configBitListXml) 
            throws XmlConfigurationParserException 
    {
        String id = configBitListXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigBitList is missing");
        }
        
        String name = configBitListXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigBitList is missing. ID = " + id);
        }

        Map<String, ConfigBit> configBitMap = new HashMap<>();
        
        // read all ConfigBits in ConfigBitList
        List<HierarchicalConfiguration> configBitsXml = configBitListXml.configurationsAt("ConfigBit");
        for ( HierarchicalConfiguration configBitXml : configBitsXml ) {
            ConfigBit configBit = getConfigBit(configBitXml);
            configBitMap.put(configBit.id, configBit);
        }
        
        return new ConfigBitList(id, name, configBitMap);
    }
    
    // reads ConfigGroup from Configuration Definition file
    private static ConfigGroup getConfigGroup(HierarchicalConfiguration configGroupXml) 
            throws XmlConfigurationParserException 
    {
        String configGroupName = configGroupXml.getString("[@Name]", "");
        if ( configGroupName.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigGroup is missing");
        }
        
        Map<String, ConfigItem> configItemsMap = new HashMap<>();
        
        // reading of independent instances of ConfigBit
        List<HierarchicalConfiguration> indipConfigBitsXml = configGroupXml.configurationsAt("ConfigBit");
        for ( HierarchicalConfiguration indipConfigBitXml : indipConfigBitsXml ) {
            ConfigBit configBit = getConfigBit(indipConfigBitXml);
            configItemsMap.put(configBit.id, configBit);
        }
        
        // reading of ConfigBits
        List<HierarchicalConfiguration> configBitsListXml = configGroupXml.configurationsAt("ConfigBits");
        for ( HierarchicalConfiguration configBitsXml : configBitsListXml ) {
            ConfigBits configBits = getConfigBits(configBitsXml);
            configItemsMap.put(configBits.id, configBits);
        }
        
        // reading of ConfigBytes
        List<HierarchicalConfiguration> configBytesXml = configGroupXml.configurationsAt("ConfigByte");
        for ( HierarchicalConfiguration configByteXml : configBytesXml ) {
            ConfigByte configByte = getConfigByte(configByteXml);
            configItemsMap.put(configByte.id, configByte);
        }
        
        // reading of ConfigBitLists including all contained ConfigBit instances
        List<HierarchicalConfiguration> configBitListsXml = configGroupXml.configurationsAt("ConfigBitList");
        for ( HierarchicalConfiguration configBitListXml : configBitListsXml ) {
            ConfigBitList configBitList = getConfigBitList(configBitListXml);
            configItemsMap.put(configBitList.id, configBitList);
        }
        
        return new ConfigGroup(configGroupName, configItemsMap);
    }
    
    // reads all ConfigGroup instances from Configuration Definition file and
    // stores them into map indexed by group's name
    private static Map<String, ConfigGroup> getConfigGroups(XMLConfiguration config) 
            throws XmlConfigurationParserException 
    {
        Map<String, ConfigGroup> configGroupMap = new HashMap<>();
        
        List<HierarchicalConfiguration> configGroupsXml = config.configurationsAt("ConfigGroup");
        for ( HierarchicalConfiguration configGroupXml : configGroupsXml ) {
            ConfigGroup configGroup = getConfigGroup(configGroupXml);
            configGroupMap.put(configGroup.name, configGroup);
        }
        
        return configGroupMap;
    }
    
    // reads ConfigBit from User configuration file
    private static ConfigBitSetting getConfigBitSetting(HierarchicalConfiguration configBitSettingXml) 
            throws XmlConfigurationParserException 
    {
        String id = configBitSettingXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID is missing in ConfigBit user settings.");
        }

        String name = configBitSettingXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException(
                    "Name is missing in ConfigBit user settings. ID = " + id 
            );
        }

        String valueStr = configBitSettingXml.getString("[@Value]", "").toUpperCase();
        if ( valueStr.isEmpty() ) {
            throw new XmlConfigurationParserException("Missing Value in ConfigBit user settings. ID = " + id);
        }

        boolean value = false;
        switch ( valueStr ) {
            case "TRUE": 
                value = true;
                break;
            case "FALSE":
                value = false;
                break;
            default:
                throw new XmlConfigurationParserException("Unknown value of ConfigBit user settings. ID =  " + id);
        }
        
        return new ConfigBitSetting(id, name, value);
    }
    
    // reads ConfigByte from User configuration file
    private static ConfigByteSetting getConfigByteSetting(HierarchicalConfiguration configByteSettingXml) 
            throws XmlConfigurationParserException 
    {
        String id = configByteSettingXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID is missing in ConfigByte user settings.");
        }

        String name = configByteSettingXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException(
                    "Name is missing in ConfigByte user settings. ID = " + id
            );
        }

        int value = -1;
        try {
            value = configByteSettingXml.getInt("[@Value]", -1);
        } catch ( Exception e ) {
            throw new XmlConfigurationParserException("Bad value in ConfigByte user setting. ID = " + id);
        }

        if ( value == -1 ) {
            throw new XmlConfigurationParserException("Missing value of ConfigByte user setting. ID = " + id);
        }
        
        return new ConfigByteSetting(id, name, value);
    }
    
    // reads ConfigBits from User configuration file
    private static ConfigBitsSetting getConfigBitsSetting(HierarchicalConfiguration configBitsSettingXml) 
            throws XmlConfigurationParserException 
    {
        String id = configBitsSettingXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID is missing in ConfigBits user settings.");
        }

        String name = configBitsSettingXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException(
                    "Name is missing in ConfigBits user settings. ID = " + id
            );
        }

        int value = -1;
        try {
            value = configBitsSettingXml.getInt("[@Value]", -1);
        } catch ( Exception e ) {
            throw new XmlConfigurationParserException("Bad value in ConfigBits user settings: ID = " + id );
        }

        if ( value == -1 ) {
            throw new XmlConfigurationParserException(
                    "Missing value in ConfigBits user setting: ID = " + id 
            );
        }
            
        return new ConfigBitsSetting(id, name, value);
    }
    
    // reads user settings group from user settings configuration file
    private static ConfigGroup getUserSettingsGroup(XMLConfiguration userSettingsXml) 
            throws XmlConfigurationParserException 
    {
        String userSettingsGroupName = userSettingsXml.getString("[@Name]", "");
        if ( userSettingsGroupName.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigGroup is missing");
        }
        
        String version = userSettingsXml.getString("[@Version]", "");
        if ( version.isEmpty() ) {
            throw new XmlConfigurationParserException("Version of ConfigGroup is missing");
        }
        
        // check version of user settings configuration file
        if ( !version.startsWith("2.00") ) {
            throw new XmlConfigurationParserException("Bad version. Targeted for version 2.00");
        }
        
        Map<String, ConfigItem> configItemsMap = new HashMap<>();
        
        // reading of independent instances of ConfigBit
        List<HierarchicalConfiguration> indipConfigBitSettingsXml = userSettingsXml.configurationsAt("ConfigBit");
        for ( HierarchicalConfiguration indipConfigBitSettingXml : indipConfigBitSettingsXml ) {
            ConfigBitSetting configBitSetting = getConfigBitSetting(indipConfigBitSettingXml);
            configItemsMap.put(configBitSetting.id, configBitSetting);
        }
        
        // reading of ConfigBits
        List<HierarchicalConfiguration> configBitsSettingsListXml = userSettingsXml.configurationsAt("ConfigBits");
        for ( HierarchicalConfiguration configBitsSettingXml : configBitsSettingsListXml ) {
            ConfigBitsSetting configBitsSetting = getConfigBitsSetting(configBitsSettingXml);
            configItemsMap.put(configBitsSetting.id, configBitsSetting);
        }
        
        // reading of ConfigBytes
        List<HierarchicalConfiguration> configByteSettingsXml = userSettingsXml.configurationsAt("ConfigByte");
        for ( HierarchicalConfiguration configByteSettingXml : configByteSettingsXml ) {
            ConfigByteSetting configByteSetting = getConfigByteSetting(configByteSettingXml);
            configItemsMap.put(configByteSetting.id, configByteSetting);
        }
        
        return new ConfigGroup(userSettingsGroupName, configItemsMap);
    }
  
    // returns HWP configuration byte for specified ConfigByte
    private static HWP_ConfigurationByte getUserSettingByte(
            ConfigByteSetting userConfigByte, ConfigByte configByte
    ) { 
        return new HWP_ConfigurationByte(configByte.address, userConfigByte.value, 0b11111111);
    }
    
    // returns HWP configuration byte for specified ConfigBits
    private static HWP_ConfigurationByte getUserSettingByte(
            ConfigBitsSetting userConfigBits, ConfigBits configBits
    ) {
        return new HWP_ConfigurationByte(configBits.address, userConfigBits.value, configBits.mask);
    }
    
    // sets HWP configuration byte according to specified ConfigBit setting
    private static void setUserConfigBit(
            ConfigBitSetting userConfigBit, ConfigBit configBit,
            Map<Integer, HWP_ConfigurationByte> userConfigByteMap 
    ) {
        // for solving EEPROM
        int address = configBit.address;
        if ( configBit.memory == EEPROM_MEMORY ) {
            address = 0x20;
        }
        
        // set user config bit
        HWP_ConfigurationByte hwpConfigByte = userConfigByteMap.get(address);
        if ( hwpConfigByte == null ) {
            int mask = (int)Math.pow(2, configBit.bitIndex);
            int value = (userConfigBit.value == true)? mask : 0;
            userConfigByteMap.put(address, new HWP_ConfigurationByte(address, value, mask));
        } else {
            int bitValue = (int)Math.pow(2, configBit.bitIndex);
            int mask = hwpConfigByte.getMask() | bitValue;
            int value = (userConfigBit.value == true)? 
                    (hwpConfigByte.getValue() | bitValue) 
                    : hwpConfigByte.getValue() & (~bitValue);
            hwpConfigByte.setMask(mask);
            hwpConfigByte.setValue(value);
        }
    }
    
    // returns config item according to specified ID and source map of groups
    private static ConfigItem getConfigItem(String id, Map<String, ConfigGroup> groupsMap) {
        for ( ConfigGroup configGroup : groupsMap.values() ) {
            ConfigItem configItem = configGroup.configItems.get(id);
            if ( configItem != null ) {
                return configItem;
            }
            
            // search ConfigBitList
            for ( ConfigItem configBase : configGroup.configItems.values() ) {
                if ( !(configBase instanceof ConfigBitList) ) {
                    continue;
                }
                
                ConfigBitList configBitList = (ConfigBitList) configBase;
                ConfigItem configListItem = configBitList.configBitMap.get(id);
                if ( configListItem != null ) {
                    return configListItem;
                }
            }
        }
        return null;
    }
    
    private static boolean isConfigItemValidForWrite(AddressableConfigItem configItem) 
            throws XmlConfigurationParserException 
    {
        if ( 
                (configItem.getMemory() == EEPROM_MEMORY) 
                && (configItem.getAddress() != RFPGM_CONFIG_ADDRESS) 
        ) {
            return false;
        }
        return true;
    }
    
    
    // returns config bytes to write into a module
    private static HWP_ConfigurationByte[] getHwpConfigBytes(
            Map<String, ConfigGroup> defGroupsMap, ConfigGroup userSettingsGroup
    ) throws XmlConfigurationParserException {
        SortedMap<Integer, HWP_ConfigurationByte> userConfigBytesMap = new TreeMap<>();
        
        for ( ConfigItem userSetting : userSettingsGroup.configItems.values() ) {
            ConfigItem configItem = getConfigItem(userSetting.id, defGroupsMap);
            if ( configItem == null ) {
                throw new XmlConfigurationParserException(
                        "Configuration item not found in definition file. Id = " + userSetting.id
                );
            }
            
            if ( !(configItem instanceof AddressableConfigItem) ) {
                throw new XmlConfigurationParserException(
                        "Invalid type if config item. Id = " + userSetting.id
                );
            }
            
            // check, if it is possible to write config item into memory on node
            if ( !isConfigItemValidForWrite((AddressableConfigItem)configItem) ) {
                continue;
            }
            
            if ( userSetting instanceof ConfigByteSetting ) {
                if ( !(configItem instanceof ConfigByte) ) {
                    throw new XmlConfigurationParserException(
                        "Mismatch user setting type. Id = " + userSetting.id
                    );
                }
                
                ConfigByte configByte = (ConfigByte) configItem;
                HWP_ConfigurationByte userSettingsByte 
                        = getUserSettingByte((ConfigByteSetting) userSetting, configByte);
                
                if ( configByte.memory == 0 ) {
                    userSettingsByte.setAddress(0x20);
                }
                userConfigBytesMap.put(userSettingsByte.getAddress(), userSettingsByte);
            } else if ( userSetting instanceof ConfigBitSetting ) {
                if ( !(configItem instanceof ConfigBit) ) {
                    throw new XmlConfigurationParserException(
                        "Mismatch user setting type. Id = " + userSetting.id
                    );
                }
                
                setUserConfigBit((ConfigBitSetting) userSetting, (ConfigBit)configItem, userConfigBytesMap);
            } else if ( userSetting instanceof ConfigBitsSetting ) {
                if ( !(configItem instanceof ConfigBits) ) {
                    throw new XmlConfigurationParserException(
                        "Mismatch user setting type. Id = " + userSetting.id
                    );
                }
                
                ConfigBits configBits = (ConfigBits) configItem;
                HWP_ConfigurationByte userSettingsByte 
                        = getUserSettingByte((ConfigBitsSetting) userSetting, configBits);
                
                if ( configBits.memory == 0 ) {
                    userSettingsByte.setAddress(0x20);
                }
                userConfigBytesMap.put(userSettingsByte.getAddress(), userSettingsByte);
            } else {
                throw new XmlConfigurationParserException("Unexpected user settings type: " + userSetting.getClass());
            }
        }
        
        return userConfigBytesMap.values().toArray( new HWP_ConfigurationByte[] {} );
    }
    
    // returns collection of rules
    private static Collection<Rule> getRules(XMLConfiguration defConfig) 
            throws XmlConfigurationParserException 
    {
        Collection<Rule> rules = new LinkedList<>();
        
        List<HierarchicalConfiguration> configRules = defConfig.configurationsAt("ConfigRules.Rules");
        for ( HierarchicalConfiguration configRule : configRules ) {
            String name = configRule.getString("[@Name]", "");
            if ( name.isEmpty() ) {
                throw new XmlConfigurationParserException("ConfigRule name is missing");
            }
            
            String conflictStr = configRule.getString("[@Conflict]", "");
            if ( conflictStr.isEmpty() ) {
                throw new XmlConfigurationParserException("ConfigRule Conflict set is missing");
            }
            
            String message = configRule.getString("[@Message]", "");
            
            rules.add( new Rule(name, Arrays.asList(conflictStr.split(",")), message) );
        }
        
        return rules;
    }
    
    // returns ConfigBit instance of specified ID
    private static ConfigBit getConfigBit(String id, Map<String, ConfigGroup> configGroups) 
            throws XmlConfigurationParserException 
    {
        for ( ConfigGroup configGroup : configGroups.values() ) {
            
            // finding in ConfigBitList instances
            for ( ConfigItem configBase : configGroup.configItems.values() ) {
                if ( configBase instanceof ConfigBitList ) {
                    ConfigBitList configBitList = (ConfigBitList) configBase;
                    ConfigBit configBit = configBitList.configBitMap.get(id);
                    if ( configBit != null ) {
                        return configBit;
                    }
                }
            }
            
            ConfigItem configBase = configGroup.configItems.get(id);
            if ( configBase == null ) {
                continue;
            }
            
            if ( !(configBase instanceof ConfigBit) ) {
                throw new XmlConfigurationParserException(
                        "Bad config type. ConfigBit expected. ID = " + id
                );
            }
            
            return (ConfigBit) configBase;
        }
        
        return null;
    }
    
    // checks rules in configuration groups
    private static void checkRules(
            Map<String, ConfigGroup> configGroups, Collection<Rule> rules
    ) throws XmlConfigurationParserException {
        for ( Rule rule : rules ) {
            ConfigBit configBit = getConfigBit(rule.name, configGroups);
            if ( configBit == null ) {
                throw new XmlConfigurationParserException(
                        "ConfigBit not found while applying rule. ID = " + rule.name
                );
            }

            if ( configBit.value == false ) {
                continue;
            }

            for ( String id : rule.ids ) {
                ConfigBit conflictBit = getConfigBit(id, configGroups);
                if ( conflictBit == null ) {
                    throw new XmlConfigurationParserException(
                            "ConfigBit not found while applying rule. ID = " + id
                    );
                }

                if ( conflictBit.value == true ) {
                    throw new XmlConfigurationParserException("Rule broken. Name = " + rule.name);
                }
            }
        }
    }
    
    
    /**
     * Parses specified source XML configuration files and returns corresponding
     * HWP configuration to write into module.
     * 
     * @param defFileName configuration definition XML file
     * @param userSettingsFileName user settings XML file
     * @return parsed HWP representation
     * @throws com.microrisc.simply.iqrf.dpa.v22x.services.node.write_configuration.XmlConfigurationParserException
     *         if source file is NOT valid or if some error has occured during parsing
     */
    public static HWP_ConfigurationByte[] parse(
            String defFileName, String userSettingsFileName
    ) throws XmlConfigurationParserException 
    {
        XMLConfiguration defConfig = null;
        XMLConfiguration userSettingsConfig = null;
        try {
            defConfig = new XMLConfiguration(defFileName);
            userSettingsConfig = new XMLConfiguration(userSettingsFileName);
        } catch ( ConfigurationException ex ) {
            throw new XmlConfigurationParserException(ex);
        }
        
        Map<String, ConfigGroup> configGroups = getConfigGroups(defConfig);
        ConfigGroup userSettingsGroup = getUserSettingsGroup(userSettingsConfig);
        
        Collection<Rule> rules = getRules(defConfig);
        checkRules(configGroups, rules);
        
        return getHwpConfigBytes(configGroups, userSettingsGroup);
    }
    
}
