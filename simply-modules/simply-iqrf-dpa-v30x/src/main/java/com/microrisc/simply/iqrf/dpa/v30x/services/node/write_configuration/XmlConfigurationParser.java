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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration;

import com.microrisc.simply.iqrf.dpa.v30x.types.HWP_ConfigurationByte;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    private static final class ConfigFrameCheck
            extends ConfigItem implements AddressableConfigItem {
        private final int memory;
        private final int address;
        private final boolean value;
        
        public ConfigFrameCheck(
                String id, String name, int memory, int address, boolean value
        ) {
            super(id, name);
            this.memory = memory;
            this.address = address;
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
    
    private static final class ConfigEditPwd
            extends ConfigItem implements AddressableConfigItem {
        private final int memory;
        private final int address;
        private final String value;
        private final int size;
        
        public ConfigEditPwd(
                String id, String name, int memory, int address, String value, int size
        ) {
            super(id, name);
            this.memory = memory;
            this.address = address;
            this.value = value;
            this.size = size;
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
    
    private static final class ConfigFrameCheckSetting extends ConfigItem {
        private final boolean value;
        
        public ConfigFrameCheckSetting(String id, String name, boolean value) {
            super(id, name);
            this.value = value;
        }
    }
    
    private static final class ConfigEditPwdSetting extends ConfigItem {
        private final String value;
        
        public ConfigEditPwdSetting(String id, String name, String value) {
            super(id, name);
            this.value = value;
        }
    }
    
    
    
    // HANDLING RULES
    private static abstract class Rule {
        protected final String id;
        
        public Rule(String id) {
            this.id = id;
        }
    } 
    
    private static final class ConflictRule extends Rule {
        private final List<String> group;
        private final String message;
        
        public ConflictRule(String id, List<String> group, String message) {
            super(id);
            this.group = group;
            this.message = message;
        }
    }
    
    private static final class ContextRule extends Rule {
        private final List<String> group;
        private final boolean not;
        
        public ContextRule(String id, List<String> group, boolean not) {
            super(id);
            this.group = group;
            this.not = not;
        }
    }
    
    
    // special valid ASCII values in security strings
    private static final Set<Character> specialValidAsciiValues = new HashSet<>(); 
    
    static {
        specialValidAsciiValues.add(' ');
        specialValidAsciiValues.add('!');
        specialValidAsciiValues.add('"');
        specialValidAsciiValues.add('#');
        specialValidAsciiValues.add('$');
        specialValidAsciiValues.add('%');
        specialValidAsciiValues.add('&');
        specialValidAsciiValues.add('\'');
        specialValidAsciiValues.add('(');
        specialValidAsciiValues.add(')');
        specialValidAsciiValues.add('*');
        specialValidAsciiValues.add('+');
        specialValidAsciiValues.add('"');
        specialValidAsciiValues.add(',');
        specialValidAsciiValues.add('-');
        specialValidAsciiValues.add('.');
        specialValidAsciiValues.add('/');
        specialValidAsciiValues.add(':');
        specialValidAsciiValues.add(';');
        specialValidAsciiValues.add('<');
        specialValidAsciiValues.add('=');
        specialValidAsciiValues.add('>');
        specialValidAsciiValues.add('?');
        specialValidAsciiValues.add('@');
        specialValidAsciiValues.add('[');
        specialValidAsciiValues.add('\\');
        specialValidAsciiValues.add(']');
        specialValidAsciiValues.add('^');
        specialValidAsciiValues.add('_');
        specialValidAsciiValues.add('`');
        specialValidAsciiValues.add('{');
        specialValidAsciiValues.add('|');
        specialValidAsciiValues.add('}');
        specialValidAsciiValues.add('~');
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
    
    // reads ConfigFrameCheck from Configuration Definition file
    private static ConfigFrameCheck getConfigFrameCheck(HierarchicalConfiguration configFrameCheckXml) 
            throws XmlConfigurationParserException 
    {
        String id = configFrameCheckXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigFrameCheck is missing");
        }
        
        String name = configFrameCheckXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigFrameCheck is missing. ID = " + id);
        }
        
        int memory = -1;
        int address = -1;
        
        try {
            memory = configFrameCheckXml.getInt("[@Memory]", -1);
            address = configFrameCheckXml.getInt("[@Address]", -1);
        } catch ( Exception ex ) {
            throw new XmlConfigurationParserException("Error in value parsing ConfigFrameCheck. ID = " + id);
        }
        
        if ( memory == -1 ) {
            throw new XmlConfigurationParserException("Missing Memory in ConfigFrameCheck. ID = " + id);
        }

        if ( address == -1 ) {
            throw new XmlConfigurationParserException("Missing Address in ConfigFrameCheck. ID = " + id);
        }

        String valueStr = configFrameCheckXml.getString("[@Value]", "").toUpperCase();
        if ( valueStr.isEmpty() ) {
            throw new XmlConfigurationParserException("Missing Value in ConfigFrameCheck. ID = " + id);
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
                throw new XmlConfigurationParserException("Unknown value of ConfigFrameCheck. ID =  " + id);
        }
        
        return new ConfigFrameCheck(id, name, memory, address, value);
    }
    
    // reads ConfigEditPwd from Configuration Definition file
    private static ConfigEditPwd getConfigEditPwd(HierarchicalConfiguration configEditPwdXml) 
            throws XmlConfigurationParserException 
    {
        String id = configEditPwdXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID of ConfigEditPwd is missing");
        }
        
        String name = configEditPwdXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException("Name of ConfigEditPwd is missing. ID = " + id);
        }
        
        int memory = -1;
        int address = -1;
        int size = -1;
        
        try {
            memory = configEditPwdXml.getInt("[@Memory]", -1);
            address = configEditPwdXml.getInt("[@Address]", -1);
            size = configEditPwdXml.getInt("[@Size]", -1);
        } catch ( Exception ex ) {
            throw new XmlConfigurationParserException("Error in value parsing ConfigEditPwd. ID = " + id);
        }
        
        if ( memory == -1 ) {
            throw new XmlConfigurationParserException("Missing Memory in ConfigEditPwd. ID = " + id);
        }

        if ( address == -1 ) {
            throw new XmlConfigurationParserException("Missing Address in ConfigEditPwd. ID = " + id);
        }

        if ( size == -1 ) {
            throw new XmlConfigurationParserException("Missing Size in ConfigEditPwd. ID = " + id);
        }
        
        String value = configEditPwdXml.getString("[@Value]", "");
        
        return new ConfigEditPwd(id, name, memory, address, value, size);
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
        
        // reading of ConfigFrameCheck
        List<HierarchicalConfiguration> configFrameChecksXml = configGroupXml.configurationsAt("ConfigFrameCheck");
        for ( HierarchicalConfiguration configFrameCheckXml : configFrameChecksXml ) {
            ConfigFrameCheck configFrameCheck = getConfigFrameCheck(configFrameCheckXml);
            configItemsMap.put(configFrameCheck.id, configFrameCheck);
        }
        
        // reading of ConfigEditPwd
        List<HierarchicalConfiguration> configEditPwdsXml = configGroupXml.configurationsAt("ConfigEditPwd");
        for ( HierarchicalConfiguration configEditPwdXml : configEditPwdsXml ) {
            ConfigEditPwd configEditPwd = getConfigEditPwd(configEditPwdXml);
            configItemsMap.put(configEditPwd.id, configEditPwd);
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
    
    // reads ConfigFrameCheck from User configuration file
    private static ConfigFrameCheckSetting getConfigFrameCheckSetting(
            HierarchicalConfiguration configFrameCheckSettingXml
    ) throws XmlConfigurationParserException {
        String id = configFrameCheckSettingXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID is missing in ConfigFrameCheck user settings.");
        }

        String name = configFrameCheckSettingXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException(
                    "Name is missing in ConfigFrameCheck user settings. ID = " + id
            );
        }

        boolean value = false;
        
        String valueStr = configFrameCheckSettingXml.getString("[@Value]", "");
        if ( valueStr.isEmpty() ) {
            throw new XmlConfigurationParserException("Missing value in ConfigFrameCheck user setting. ID = " + id);
        }
        
        switch ( valueStr.toLowerCase() ) {
            case "true":
                value = true;
                break;
            case "false":
                value = false;
                break;
            default:
                throw new XmlConfigurationParserException("Bad value in ConfigByte user setting. ID = " + id);
        }
        
        return new ConfigFrameCheckSetting(id, name, value);
    }
    
    // reads ConfigEditPwd from User configuration file
    private static ConfigEditPwdSetting getConfigEditPwdSetting(
            HierarchicalConfiguration configEditPwdSettingXml
    ) throws XmlConfigurationParserException {
        String id = configEditPwdSettingXml.getString("[@ID]", "");
        if ( id.isEmpty() ) {
            throw new XmlConfigurationParserException("ID is missing in ConfigEditPwd user settings.");
        }

        String name = configEditPwdSettingXml.getString("[@Name]", "");
        if ( name.isEmpty() ) {
            throw new XmlConfigurationParserException(
                    "Name is missing in ConfigEditPwdSetting user settings. ID = " + id
            );
        }

        String value = configEditPwdSettingXml.getString("[@Value]");
        return new ConfigEditPwdSetting(id, name, value);
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
        if ( !version.startsWith("3.00") ) {
            throw new XmlConfigurationParserException("Bad version. Targeted for version 3.00");
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
        
        // reading of ConfigFrameChecks
        List<HierarchicalConfiguration> configFrameCheckSettingsXml = userSettingsXml.configurationsAt("ConfigFrameCheck");
        for ( HierarchicalConfiguration configFrameCheckSettingXml : configFrameCheckSettingsXml ) {
            ConfigFrameCheckSetting configFrameCheckSetting = getConfigFrameCheckSetting(configFrameCheckSettingXml);
            configItemsMap.put(configFrameCheckSetting.id, configFrameCheckSetting);
        }
        
        // reading of ConfigEditPwd
        List<HierarchicalConfiguration> configEditPwdSettingsXml = userSettingsXml.configurationsAt("ConfigEditPwd");
        for ( HierarchicalConfiguration configEditPwdSettingXml : configEditPwdSettingsXml ) {
            ConfigEditPwdSetting configPwdEditSetting = getConfigEditPwdSetting(configEditPwdSettingXml);
            configItemsMap.put(configPwdEditSetting.id, configPwdEditSetting);
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
        
        // special processing for setting 109 - Dual or single channel
        if ( configBit.id.equals("109") ) {
            hwpConfigByte = userConfigByteMap.get(address);
            if ( configBit.value == true ) {
                hwpConfigByte.setValue( hwpConfigByte.getValue() | 0b11 );
            }
            
            hwpConfigByte.setMask( hwpConfigByte.getMask() | 0b11 );
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
        
        ConfigGroup securityGroup = defGroupsMap.get("Security");
        
        for ( ConfigItem userSetting : userSettingsGroup.configItems.values() ) {
            ConfigItem configItem = getConfigItem(userSetting.id, defGroupsMap);
            if ( configItem == null ) {
                throw new XmlConfigurationParserException(
                        "Configuration item not found in definition file. Id = " + userSetting.id
                );
            }
            
            // filter out settings related to security group
            if ( securityGroup.configItems.containsKey(configItem.id) ) {
                continue;
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
    private static Map<String, Rule> getRules(XMLConfiguration defConfig) 
            throws XmlConfigurationParserException 
    {
        Map<String, Rule> rules = new HashMap<>();
        
        List<HierarchicalConfiguration> configRules = defConfig.configurationsAt("ConfigRules.Rules");
        for ( HierarchicalConfiguration configRule : configRules ) {
            String id = configRule.getString("[@ID]", "");
            if ( id.isEmpty() ) {
                throw new XmlConfigurationParserException("ConfigRule ID is missing");
            }
            
            String conflictStr = configRule.getString("[@Conflict]", "");
            if ( !conflictStr.isEmpty() ) {
                String message = configRule.getString("[@Message]", "");
                rules.put(id, new ConflictRule(id, Arrays.asList(conflictStr.split(",")), message) );
                continue;
            }
            
            String contextStr = configRule.getString("[@Context]", "");
            if ( !contextStr.isEmpty() ) {
                String notStr = configRule.getString("[@Not]", "");
                
                if ( notStr.isEmpty() ) {
                    throw new XmlConfigurationParserException("Context rule value not specified.");
                }
                
                boolean notValue = false;
                
                String notStrUpperCase = notStr.toLowerCase();
                switch ( notStrUpperCase ) {
                    case "true": 
                        notValue = true;
                        break;
                    case "false":
                        notValue = false;
                        break;
                    default:
                        throw new XmlConfigurationParserException("Unknown value of context rule: " + notStr);
                }
                
                rules.put(id, new ContextRule(id, Arrays.asList(contextStr.split(",")), notValue) );
                continue;
            }
            
            // unknown rule
            throw new XmlConfigurationParserException("Unknown rule. ID = " + id);   
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
    
    // returns ConfigFrameCheck instance of specified ID from specified groups
    private static ConfigFrameCheck getConfigFrameCheck(String id, Map<String, ConfigGroup> configGroups) 
            throws XmlConfigurationParserException 
    {
        for ( ConfigGroup configGroup : configGroups.values() ) {
   
            for ( ConfigItem configBase : configGroup.configItems.values() ) {
                if ( configBase instanceof ConfigFrameCheck ) {
                    ConfigFrameCheck configFrameCheck = (ConfigFrameCheck) configBase;
                    if ( configFrameCheck.id.equals(id) ) {
                        return configFrameCheck;
                    }
                }
            }
        }
        
        return null;
    }
    
    private static void checkConflictRule(
            ConflictRule rule, Map<String, ConfigGroup> configGroups
    ) throws XmlConfigurationParserException {
        ConfigBit configBit = getConfigBit(rule.id, configGroups);
        if ( configBit == null ) {
            /*
            throw new XmlConfigurationParserException(
                    "ConfigBit not found while applying rule. ID = " + rule.id
            );
            */
            logger.warn("ConfigBit not found while applying rule. ID = {}", rule.id);
            return;
        }

        if ( configBit.value == false ) {
            return;
        }

        for ( String id : rule.group ) {
            ConfigBit conflictBit = getConfigBit(id, configGroups);
            if ( conflictBit == null ) {
                throw new XmlConfigurationParserException(
                        "ConfigBit not found while applying rule. ID = " + id
                );
            }

            if ( conflictBit.value == true ) {
                throw new XmlConfigurationParserException("Rule broken. ID = " + rule.id);
            }
        }
    }
    
    private static void checkContextRule(
            ContextRule rule, Map<String, ConfigGroup> configGroups
    ) throws XmlConfigurationParserException {
        ConfigFrameCheck configFrameCheck = getConfigFrameCheck(rule.id, configGroups);
        if ( configFrameCheck == null ) {
            throw new XmlConfigurationParserException(
                    "ConfigFrameCheck not found while applying rule. ID = " + rule.id
            );
        }
        
        for ( String id : rule.group ) {
            ConfigItem configItem = getConfigItem(id, configGroups);
            if ( configItem == null ) {
                throw new XmlConfigurationParserException(
                        "Config item not found while applying rule. ID = " + id
                );
            }
        }
    }
    
    // checks rules in configuration groups
    private static void checkRules(
            Map<String, ConfigGroup> configGroups, Collection<Rule> rules
    ) throws XmlConfigurationParserException {
        for ( Rule rule : rules ) {
            if ( rule instanceof ConflictRule ) {
                checkConflictRule((ConflictRule)rule, configGroups);
            } else if ( rule instanceof ContextRule ) {
                checkContextRule((ContextRule)rule, configGroups);
            } else {
                throw new XmlConfigurationParserException("Unsupported rule type: " + rule.getClass());
            }
        }
    }
    
    // encapsulates things related to parsing of security settings
    private static final class Security {
        enum ItemType {
            PASSWORD,
            KEY
        }
        
        enum InputFormat {
            HEX,
            ASCII
        }
        
        // frame checks config item IDs
        public static final String FRAME_CHECK_ID_PASSWORD = "300";
        public static final String FRAME_CHECK_ID_KEY = "303";
        
        // input formats config items IDs
        public static final String INPUT_FORMAT_ID_PASSWORD = "301";
        public static final String INPUT_FORMAT_ID_KEY = "304";
        
        // security values config items IDs
        public static final String VALUE_ID_PASSWORD = "302";
        public static final String VALUE_ID_KEY = "305";
    }
    
    private static Security.InputFormat getSecurityInputFormat(
            ConfigGroup userSettingsGroup,
            String inFormatId
    ) throws XmlConfigurationParserException {
        ConfigItem configItem = userSettingsGroup.configItems.get(inFormatId);
        if ( configItem == null ) {
            throw new XmlConfigurationParserException(
                    "Security input format config item is missing in user settings."
                    + " ID: " + inFormatId
            );
        }
        
        if ( !(configItem instanceof ConfigBitsSetting) ) {
            throw new XmlConfigurationParserException(
                    "Security input format config item is not ConfigBits."
                    + " ID: " + inFormatId
            );
        } 
        
        ConfigBitsSetting inputFormatItem = (ConfigBitsSetting)configItem;
        
        switch ( inputFormatItem.value ) {
            case 0:
                return Security.InputFormat.HEX;
            case 1:
                return Security.InputFormat.ASCII;
            default:
                throw new XmlConfigurationParserException(
                    "Unknown value of security input format config item."
                    + " ID: " + inFormatId
                );  
        }
    }
    
    private static String getSecurityString(
            ConfigGroup securityGroup,
            ConfigGroup userSettingsGroup, 
            String secStringId
    ) throws XmlConfigurationParserException {
        
        // get maximal size of the string - from security settings
        ConfigItem configItem = securityGroup.configItems.get(secStringId);
        if ( configItem == null ) {
            throw new XmlConfigurationParserException(
                    "Security string config item is missing in security settings."
                    + " ID: " + secStringId
            );
        }
        
        if ( !(configItem instanceof ConfigEditPwd) ) {
            throw new XmlConfigurationParserException(
                    "Security string config item is not ConfigEditPwd."
                    + " ID: " + secStringId
            );
        } 
        
        ConfigEditPwd secStringItem = (ConfigEditPwd) configItem;
        
        
        configItem = userSettingsGroup.configItems.get(secStringId);
        if ( configItem == null ) {
            throw new XmlConfigurationParserException(
                    "Security string config item is missing in user settings."
                    + " ID: " + secStringId
            );
        }
        
        if ( !(configItem instanceof ConfigEditPwdSetting) ) {
            throw new XmlConfigurationParserException(
                    "Security string config item is not ConfigEditPwd in user settings."
                    + " ID: " + secStringId
            );
        } 
        
        ConfigEditPwdSetting secStringUserItem = (ConfigEditPwdSetting)configItem;
        
        // check for maximal size
        if ( secStringUserItem.value.length() > secStringItem.size ) {
            throw new XmlConfigurationParserException(
                    "Security string size exceeds maximal size."
                    + " ID: " + secStringId
            );
        }
        
        return secStringUserItem.value;
    }
    
    private static char checkSecurityCharacter(char ch)
        throws XmlConfigurationParserException 
    {
        if ( ch >= '0' && ch <= '9' ) {
            return ch;
        }
        
        if ( ch >= 'a' && ch <= 'z' ) {
            return ch;
        }
        
        if ( ch >= 'A' && ch <= 'Z' ) {
            return ch;
        }
        
        if ( specialValidAsciiValues.contains(ch) ) {
            return ch;
        }
        
        throw new XmlConfigurationParserException(
                "Invalid character in security string: " + ch
        );
    } 
    
    private static short[] getSecurityValue(
            Security.InputFormat inputFormat, 
            String secString
    ) throws XmlConfigurationParserException {
        short[] secValue = null;
        
        switch ( inputFormat ) {
            case ASCII:
                secValue = new short[secString.length()];
                for ( int i = 0; i < secValue.length; i++ ) {
                    secValue[i] = (short)checkSecurityCharacter(secString.charAt(i));
                }
                break;
                
            case HEX:
                if ( secString.length() % 2 != 0 ) {
                    throw new XmlConfigurationParserException(
                        "Number of characters in security string in HEX format must be even."
                    );
                }
                
                secValue = new short[secString.length() / 2];
                
                StringBuilder sb = new StringBuilder();
                sb.append('0');
                sb.append('0');
                
                for ( int i = 0, j = 0; i < secString.length(); i +=2, j++ ) {
                    sb.setCharAt(0, secString.charAt(i));
                    sb.setCharAt(1, secString.charAt(i+1));
                    
                    String valueStr = sb.toString();
                    try {
                        secValue[j] = (short)Integer.parseInt(valueStr, 16);
                    } catch ( NumberFormatException e ) {
                        throw new XmlConfigurationParserException(
                            "Invalid value in hexa security string: " + valueStr
                        );
                    }
                }
                
                break;
            default:
                throw new XmlConfigurationParserException(
                        "Unknown input security format: " + inputFormat
                );
        }
        
        return secValue;
    }
    
    private static short[] getSecurityItem(
            ConfigGroup securityGroup,
            ConfigGroup userSettingsGroup,
            Map<String, Rule> rules,
            Security.ItemType securityItem
    ) throws XmlConfigurationParserException {
        
        String frameCheckId = null;
        String inputFormatId = null;
        String valueId = null;
        
        switch ( securityItem ) {
            case PASSWORD:
                frameCheckId = Security.FRAME_CHECK_ID_PASSWORD;
                inputFormatId = Security.INPUT_FORMAT_ID_PASSWORD;
                valueId = Security.VALUE_ID_PASSWORD;
                break;
            case KEY:
                frameCheckId = Security.FRAME_CHECK_ID_KEY;
                inputFormatId = Security.INPUT_FORMAT_ID_KEY;
                valueId = Security.VALUE_ID_KEY;
                break;
            default:
                throw new XmlConfigurationParserException("Unknown security item: " + securityItem);
        }
        
        // search for rule for frame check item
        Rule rule = rules.get(frameCheckId);
        if ( rule == null ) {
            throw new XmlConfigurationParserException(
                    "Security rule "  + frameCheckId + " is missing."
            );
        }
        
        if ( !(rule instanceof ContextRule) ) {
            throw new XmlConfigurationParserException(
                    "Security rule "  + frameCheckId + " is not context rule."
            );
        } 
        
        ContextRule securityRule = (ContextRule) rule;
        
        // get frame check item
        ConfigItem configItem = userSettingsGroup.configItems.get(frameCheckId);
        if ( configItem == null ) {
            throw new XmlConfigurationParserException(
                    "Security item "  + frameCheckId + " is missing in user settings."
            );
        }
        
        if ( !(configItem instanceof ConfigFrameCheckSetting) ) {
            throw new XmlConfigurationParserException(
                    "Security item "  + frameCheckId + " is not of ConfigFrameCheck type."
            );
        } 
        
        ConfigFrameCheckSetting frameCheckConfigItem = (ConfigFrameCheckSetting)configItem;
        
        // resolution of rule according to item
        if ( securityRule.not == false ) {
            if ( frameCheckConfigItem.value == false ) {
                return null;
            }
        } else {
            if ( frameCheckConfigItem.value == true ) {
                return null;
            }
        }
        
        Security.InputFormat inputFormat = getSecurityInputFormat(userSettingsGroup, inputFormatId);
        String securityString = getSecurityString(securityGroup, userSettingsGroup, valueId);
        
        return getSecurityValue(inputFormat, securityString);
    }
    
    
    // returns security settings
    private static ConfigSettings.Security getSecuritySettings(
        Map<String, ConfigGroup> configGroups,
        ConfigGroup userSettingsGroup,
        Map<String, Rule> rules
    ) throws XmlConfigurationParserException 
    {
        ConfigGroup securityGroup = configGroups.get("Security");
        if ( securityGroup == null ) {
            throw new XmlConfigurationParserException("Security config group is missing.");
        }
        
        short[] password = getSecurityItem(securityGroup, userSettingsGroup, rules, Security.ItemType.PASSWORD);
        short[] key = getSecurityItem(securityGroup, userSettingsGroup, rules, Security.ItemType.KEY);
        
        return new ConfigSettings.Security(password, key);
    }
    
    
    /**
     * Parses specified source XML configuration files and returns corresponding
     * HWP configuration to write into module.
     * 
     * @param defFileName configuration definition XML file
     * @param userSettingsFileName user settings XML file
     * @return parsed HWP representation
     * @throws com.microrisc.simply.iqrf.dpa.v30x.services.node.write_configuration.XmlConfigurationParserException
     *         if source file is NOT valid or if some error has occured during parsing
     */
    public static ConfigSettings parse(
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
        
        Map<String, Rule> rules = getRules(defConfig);
        checkRules(configGroups, rules.values());
        
        HWP_ConfigurationByte[] hwpConfigBytes = getHwpConfigBytes(configGroups, userSettingsGroup);
        ConfigSettings.Security securitySettings 
                = getSecuritySettings(configGroups, userSettingsGroup, rules);
        
        return new ConfigSettings(hwpConfigBytes, securitySettings);
    }
    
}
