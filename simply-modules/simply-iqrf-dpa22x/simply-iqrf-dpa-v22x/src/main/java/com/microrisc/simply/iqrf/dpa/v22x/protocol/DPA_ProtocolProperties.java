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

package com.microrisc.simply.iqrf.dpa.v22x.protocol;

import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.typeconvertors.AbstractConvertor;
import com.microrisc.simply.typeconvertors.ValueConversionException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encapsulates properties of IQRF DPA application protocol. 
 * Implemented according to:
 *  "IQRF DPA Framework. Technical guide. Version v2.27. IQRF OS v3.08D
 *  14.4.2016"
 * document.
 * <p>
 * Request message format: <br>
 * NADR | PNUM | PCMD | HWPID | PData
 * 
 * <p>
 * Response message format: <br>
 * NADR | PNUM | PCMD | HWPID | RESPONSE_CODE | DPA_ProtocolProperties Value | RESPONSE_DATA
 
 <p>
 Fields: <br>
 NADR - Network device address ( 0 Coordinator, 1-0xEF Node address, 0xFF Broadcast )
        length: 2 bytes
 PNUM - Perihperal number ( 0 IQMESH, 1 OS, 2-0x6F other peripherals )
 PCMD - Command specifying an action to be taken. Actual allowed value range 
        depends on the peripheral type. The most significant bit indicates DPA_ProtocolProperties 
        response message.
 HWPID  - HW Profile, length: 2B
 DATA - array of bytes ( only optionally )
 RESPONSE_CODE - response code, including error code
                  length: 2 bytes
 RESPONSE_DATA - optional response data. Node response data is sent in a case
              of error. 
 * 
 * @author Michal Konopa
 */
public final class DPA_ProtocolProperties {
    
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPA_ProtocolProperties.class);
    
    /**
     * NADR properties.
     */
    public static class NADR_Properties {
        
        // suppresses default constructor for noninstantiability
        private NADR_Properties() {
            throw new AssertionError();
        }
        
        /** IQMESH Coordinator address. */
        public static final int IQMESH_COORDINATOR_ADDRESS = 0x00;
        
        /** IQMESH Node addresses. */
        public static final int IQMESH_NODE_ADDRESS_MIN = 0x01;
        public static final int IQMESH_NODE_ADDRESS_MAX = 0xEF;

        /** Local ( over SPI ) device address. */
        public static final int LOCAL_DEVICE_ADDRESS = 0xFC;

        /** IQMESH Temporary address. */
        public static final int IQMESH_TEMPORARY_ADDRESS = 0xFE;

        /** IQMESH Broadcast address. */
        public static final int IQMESH_BROADCAST_ADDRESS = 0xFF;
        
        /**
         * Indicates, whether the specified value of NADR is reserved.
         * @param nadr NADR value to check
         * @return {@code true} if {@code nadr} is reserved <br>
         *         {@code false} otherwise
         */
        public static boolean isReserved(int nadr) {
            return (
                    ( nadr >= 0xF0 ) && ( nadr <= 0xFB )
                    || ( nadr == 0xFD )
                    || ( nadr >= 0x100 ) && ( nadr <= 0xFFFF )
            );
        }
    }
    
    /**
     * PNUM properties.
     */
    public static class PNUM_Properties {
        
        // Suppresses default constructor for noninstantiability
        private PNUM_Properties() {
            throw new AssertionError();
        }
        
        /** Numbers of standard peripherals. */
        public static final int COORDINATOR =   0x00;
        public static final int NODE =          0x01;
        public static final int OS =            0x02;
        public static final int EEPROM =        0x03;
        public static final int EEEPROM =       0x04;
        public static final int RAM =           0x05;
        public static final int LEDR =          0x06;
        public static final int LEDG =          0x07;
        public static final int SPI =           0x08;
        public static final int IO =            0x09;
        public static final int THERMOMETER =   0x0A;
        public static final int PWM =           0x0B;
        public static final int UART =          0x0C;
        public static final int FRC =           0x0D;        
        
        /** User peripherals properties. */
        public static final int USER_PERIPHERAL_START =   0x20;
        public static final int USER_PERIPHERAL_END =     0x6F;
        
        /**
         * Indicates, whether the specified value of PNUM is a user peripheral.
         * @param pnum PNUM value to check
         * @return {@code true} if {@code pnum} is a user peripheral <br>
         *         {@code false} otherwise
         */
        public static boolean isUser(int pnum) {
            return (( pnum >= 0x20 ) && ( pnum <= 0x6F ));
        }
        
        /**
         * Indicates, whether the specified value of PNUM is reserved.
         * @param pnum PNUM value to check
         * @return {@code true} if {@code pnum} is reserved <br>
         *         {@code false} otherwise
         */
        public static boolean isReserved(int pnum) {
            return (( pnum >= 0x70 ) && ( pnum <= 0xFF ));
        }
        
        /**
         * Indicates, whether the specified value of PNum is reserved for standard
         * peripheral.
         * @param pnum PNUM value to check
         * @return {@code true} if {@code pnum} is reserved for standard peripheral <br>
         *         {@code false} otherwise
         */
        public static boolean isReservedForStandard(int pnum) {
            return (( pnum >= 0x00 ) && ( pnum <= 0x1F ));
        }
    }
    
    /**
     * PCMD properties.
     */
    public static class PCMD_Properties {
        
        // suppresses default constructor for noninstantiability
        private PCMD_Properties() {
            throw new AssertionError();
        }
        
        /** PCmd values range. */
        public static final int VALUE_MIN = 0x00;
        public static final int VALUE_MAX = 0x3E;
        
        /**
         * Indicates, whether the specified value of PCMD is reserved.
         * @param pcmd PCMD value to check
         * @return {@code true} if {@code pcmd} is reserved <br>
         *         {@code false} otherwise
         */
        public static boolean isReserved(int pcmd) {
            return ( pcmd >= 0x3F ) && ( pcmd <= 0xFF ) ;
        }
    }
    
   /**
    * HW Profile ID properties.
    * 
    * @author Michal Konopa
    */
   public static class HWPID_Properties {
       
       /**
        * Types of HW profiles.
        */
       public static enum TYPE {
           DEFAULT,
           RESERVED,
           CERTIFIED,
           USER,
           DO_NOT_CHECK
       }

       /** Default HW profile. */
       public static final int DEFAULT = 0x00;
       
       /** "Do not check" HW profile value. */
       public static final int DO_NOT_CHECK = 0xFFFF;
       

       // suppresses default constructor for noninstantiability
       private HWPID_Properties() {
           throw new AssertionError();
       }
       
       /**
        * Indicates, whether the specified value of HWPID is reserved.
        * @param hwpId HWPID to check
        * @return {@code true} if {@code hwpId} is reserved HW profile <br>
        *         {@code false} otherwise
        */
       public static boolean isReserved(int hwpId) {
           if ( hwpId == DEFAULT ) {
               return false;
           }
           return (hwpId & 0b01) == 0 || (hwpId == 0xFFFF);
       }
       
       /**
        * Indicates, whether the specified value of HWPID is cirtified HW profile.
        * @param hwpId HWPID to check
        * @return {@code true} if {@code hwpId} is certified HW profile <br>
        *         {@code false} otherwise
        */
       public static boolean isCertified(int hwpId) {
           return (hwpId & 0b111) != 0 || (hwpId & 0b1110) != 0;
       }

       /**
        * Indicates, whether the specified value of HWPID is user HW profile.
        * @param hwpId HW profile to check
        * @return {@code true} if {@code hwpId} is user HW profile <br>
        *         {@code false} otherwise
        */
       public static boolean isUser(int hwpId) {
           if ( hwpId == 0xFFFF ) {
               return false;
           }
           return (hwpId & 0b1111) == 0b1111;
       }

       /**
        * Indicates, whether the specified value of HWPID is "Do not check".
        * @param hwpId HW profile to check
        * @return {@code true} if {@code hwpId} is "Do not check"<br>
        *         {@code false} otherwise
        */
       public static boolean isDoNotCheck(int hwpId) {
           return ( hwpId == DO_NOT_CHECK );
       }
       

       /**
        * Returns type of the specified value of HW profile. 
        * @param hwpId HW profile, which the type to return for
        * @return type of HW profile
        * @throws IllegalArgumentException if {@code hwpId} is out of [0 .. 0xFFFF] interval
        */
       public static TYPE getType(int hwpId) {
           if ( (hwpId < 0) || (hwpId > 0xFFFF) ) {
               throw new IllegalArgumentException("Value of HW profile out of bounds: " + hwpId);
           }

           if ( hwpId == DEFAULT ) {
               return TYPE.DEFAULT;
           }

           if ( isCertified(hwpId) ) {
               return TYPE.CERTIFIED;
           }

           if ( isUser(hwpId) ) {
               return TYPE.USER;
           }
           
           if ( isDoNotCheck(hwpId) ) {
               return TYPE.DO_NOT_CHECK;
           }
           
           if ( isReserved(hwpId) ) {
               return TYPE.RESERVED;
           }

           // run should not reach this place
           throw new IllegalStateException("Uknown value of HW profile: " + hwpId);
       }

   }
   
    
   /**
     * FRC properties.
     */
    public static class FRC_Properties {
        
        /**
         * Type of collected data.
         */
        public static enum CollectedDataType {
            TWO_BITS,
            ONE_BYTE,
            TWO_BYTES;
        }
        
        // suppresses default constructor for noninstantiability
        private FRC_Properties() {
            throw new AssertionError();
        }
        
        
        /**
         * For specified FRC command returns type of collected data. 
         * If FRC command value is outside of all collected data types ranges, 
         * {@code null} is returned.
         * @param frcCommand FRC command, which to return type of collected data for
         * @return type of collected data <br>
         *         {@code null}, if {@code frcCommand} is outside of all collected 
         *         data types ranges
         */
        public static CollectedDataType getCollectedDataType(int frcCommand) {
            if ( frcCommand >= 0x00 && frcCommand <= 0x7F ) {
                return CollectedDataType.TWO_BITS;
            }
            
            if ( frcCommand >= 0x80 && frcCommand <= 0xDF ) {
                return CollectedDataType.ONE_BYTE;
            }
            
            if ( frcCommand >= 0xE0 && frcCommand <= 0xFF ) {
                return CollectedDataType.TWO_BYTES;
            }
            
            return null;
        }
    }
    
    
    
    /** Start index of node address field. */
    public static final int NADR_START = 0;
    
    /** Length of node address field. */
    public static final int NADR_LENGTH = 2;
    
    
    /** Start index of perihperal number field. */
    public static final int PNUM_START = 2;
    
    /** Length of method perihperal number field. */
    public static final int PNUM_LENGTH = 1;
    
    
    /** Start index of data address field. */
    public static final int PCMD_START = 3;
    
    /** Length of data address field. */
    public static final int PCMD_LENGTH = 1;
    
    /** Minimum value of PCMD field. */
    public static final int PCMD_VALUE_MIN = 0x00;
    
    /** Maximal value of PCMD field. */
    public static final int PCMD_VALUE_MAX = 0x3E;
    
    
    /** Start of HW Profile. */
    public static final int HW_PROFILE_START = 4;
    
    /** Length of HW Profile field. */
    public static final int HW_PROFILE_LENGTH = 2;
    
    
    /** Start index of optional data field. */
    public static final int PDATA_START = 6;
    
    /** The maximum length of data in bytes. */
    public static final int PDATA_MAX_LENGTH = 56;
    
    
    
    /** Start index of response code field. */
    public static final int RESPONSE_CODE_START = 6;
    
    /** Length of response code field. */
    public static final int RESPONSE_CODE_LENGTH = 1;
    
    
    /** Start index of DPA_ProtocolProperties Value field. */
    public static final int DPA_VALUE_START = 7;
    
    /** Length of DPA_ProtocolProperties Value field. */
    public static final int DPA_VALUE_LENGTH = 1;
    
    
    /** Start index of response data. */
    public static final int RESPONSE_DATA_START = DPA_VALUE_START + DPA_VALUE_LENGTH;
    
    
    // Suppress default constructor for noninstantiability
    private DPA_ProtocolProperties() {
        throw new AssertionError();
    }
    
    
    /**
     * Sets part of specified protocol message to specified Integer data.
     * @param protoMsg message to set
     * @param data source data
     * @param startIndex start index in the message
     * @param dataLength number of bytes to copy
     */
    static private void setMessageData_Int(short[] protoMsg, int data, int startIndex, 
            int dataLength) {
        logger.debug("setMessageData_Integer - start: protoMsg={}, data={}", 
                protoMsg, data
        );
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(data);
        for ( int byteId = 0; byteId < dataLength; byteId++ ) {
            protoMsg[byteId + startIndex] = (short)(byteBuffer.get(byteId) & 0xFF);
        }
        
        logger.debug("setMessageData_Integer - end");
    }
    
    /**
     * Returns Integer-typed specified part of specified protocol message.
     * 
     * @param message source message
     * @param startIndex start index in the message
     * @param dataLength number of bytes
     */
    static private int getMessageDataAsInt(short[] message, int startIndex, 
            int dataLength
    ) {
        logger.debug("getMessageDataAsInt - start: message={}, startIndex={}, dataLength={}",
            message, startIndex, dataLength
        );
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int byteId = 0; byteId < dataLength; byteId++) {
            byteBuffer.put((byte)message[startIndex + byteId]);
        }
        
        Integer data = byteBuffer.getInt(0);
        
        logger.debug("getMessageDataAsInt - end: {}", data);
        return data;
    }
    
    /**
     * Returns length of tripple.
     * @return length of tripple.
     */
    static int getTrippleLength() {
        return PCMD_START + PCMD_LENGTH;
    }
    
    /**
     * Sets NADR field of specified message to specified value.
     * @param protoMsg message to set
     * @param nodeAddress value to use
     */
    static void setNodeAddress(short[] protoMsg, int nodeAddress) {
        setMessageData_Int(protoMsg, nodeAddress, NADR_START, NADR_LENGTH);
    }
    
    /**
     * Sets PNUM field of specified message to specified value.
     * @param protoMsg message to set
     * @param perNumber value to use
     */
    static void setPeripheralNumber(short[] protoMsg, int perNumber) {
        setMessageData_Int(protoMsg, perNumber, PNUM_START, PNUM_LENGTH);
    }
    
    /**
     * Sets PCMD field of specified message to specified value.
     * @param protoMsg message to set
     * @param command value to use
     */
    static void setCommand(short[] protoMsg, int command) {
        setMessageData_Int(protoMsg, command, PCMD_START, PCMD_LENGTH);
    }
    
    /**
     * Sets specified message NADDR field to specified device ID. 
     * @param protoMsg message to set
     * @param deviceId device ID
     */
    static void setNodeAddress(short[] protoMsg, String deviceId) {
        logger.debug("setAddress - start: protoMsg={}, deviceId={}", protoMsg, deviceId);
        
        int addr = Integer.parseInt(deviceId);
        setMessageData_Int(protoMsg, addr, NADR_START, NADR_LENGTH);
        
        logger.debug("setAddress - end");
    }
    
    /**
     * Sets specified message PNUM field to specified peripheral identifier.
     * @param protoMsg message to set
     * @param perId peripheral identifier
     */
    static void setPerihperalNumber(short[] protoMsg, String perId) {
        logger.debug("setPerNum - start: protoMsg={}, perNum={}", protoMsg, perId);
        
        int ifaceNum = Integer.parseInt(perId);
        setMessageData_Int(protoMsg, ifaceNum, PNUM_START, PNUM_LENGTH);
        
        logger.debug("setPerNum - end");
    }
    
    
    /** RESPONSE PROCESSING. */
    
    /**
     * Returns NADR field of specified message.
     * 
     * @param protoMsg source message
     * @return NADR field of specified message.
     */
    static int getNodeAddress(short[] protoMsg) {
        logger.debug("getNodeAddress - start: protoMsg={}", protoMsg);
        
        int nodeAddress = getMessageDataAsInt(protoMsg, NADR_START, 1); 
        
        logger.debug("getNodeAddress - end: {}", nodeAddress);
        return nodeAddress;
    }
    
    /**
     * Returns PNUM field of specified message.
     * @param protoMsg source message
     * @return PNUM field of specified message.
     */
    public static int getPeripheralNumber(short[] protoMsg) {
        logger.debug("getPeripheralNumber - start: protoMsg={}", protoMsg);
        
        int perNumber = getMessageDataAsInt(protoMsg, PNUM_START, PNUM_LENGTH);
        
        logger.debug("getPeripheralNumber - end: {}", perNumber);
        return perNumber;
    }
    
    /**
     * Returns PCMD field of specified message.
     * @param protoMsg source message
     * @return PCMD field of specified message.
     */
    public static int getCommand(short[] protoMsg) {
        logger.debug("getCommand - start: protoMsg={}", protoMsg);
        
        int command = getMessageDataAsInt(protoMsg, PCMD_START, PCMD_LENGTH);
        
        logger.debug("getCommand - end: {}", command);
        return command;
    }
    
    /**
     * Returns HWP ID.
     * 
     * @param message source message
     * @return HWP ID
     */
    public static int getHWPID(short[] message) {
        return getMessageDataAsInt(message, HW_PROFILE_START, HW_PROFILE_LENGTH);
    }
            
    /**
     * Returns RESPONSE_CODE field of specified message ( must be a response ).
     * @param protoMsg source message
     * @return RESPONSE_CODE field of specified message
     * @throws ValueConversionException, if response code contains unknown value
     */
    public static DPA_ResponseCode getResponseCode(short[] protoMsg) 
            throws ValueConversionException {
        logger.debug("getResponseCode - start: protoMsg={}", protoMsg);
        
        int responseIntCode = getMessageDataAsInt(
                protoMsg, RESPONSE_CODE_START, RESPONSE_CODE_LENGTH
        );
        for ( DPA_ResponseCode responseCode : DPA_ResponseCode.values() ) {
            if ( responseCode.getCodeValue() == responseIntCode ) {
                logger.debug("getResponseCode - end: {}", responseCode);
                return responseCode;
            }
        }
        
        // unknown reponse code
        throw new ValueConversionException("Unknown response code: " + responseIntCode);
    }
    
    /**
     * Retruns response code length.
     * @return length of response code
     */
    static int getResponseCodeLength() {
        return RESPONSE_CODE_LENGTH;
    }
    
    /**
     * Returns RESPONSE_DATA field of specified message ( must be a response ).
     * @param protoMsg source message
     * @return RESPONSE_DATA field of specified message
     */
    static short[] getResponseData(short[] protoMsg) {
        logger.debug("getResponseData - start: protoMsg={}", protoMsg);
        
        int responseLength = protoMsg.length - RESPONSE_DATA_START;
        short[] responseData = new short[protoMsg.length - RESPONSE_DATA_START];
        System.arraycopy(protoMsg, RESPONSE_DATA_START, responseData, 0, responseLength);
        
        logger.debug("getResponseData - end: {}", responseData);
        return responseData;
    }
    
    /**
     * Returns converted RESPONSE_DATA field.
     * @param protoMsg source message
     * @param typeConvertor type convertor to use     
     * @return RESPONSE_DATA field of specified message.
     * @throws ValueConversionException
     */
    static Object getReturnValue(short[] protoMsg, AbstractConvertor typeConvertor) 
            throws ValueConversionException 
    {
        logger.debug("getReturnValue - start: protoMsg={}, typeConvertor={}", 
                protoMsg, typeConvertor
        );
        
        short[] retVal = new short[protoMsg.length - RESPONSE_DATA_START];
        System.arraycopy(protoMsg, RESPONSE_DATA_START, retVal, 0, retVal.length);
        
        // may throw exception
        Object retValObj = typeConvertor.toObject(retVal); 
      
        logger.debug("getReturnValue - end: {}", retValObj);
        return retValObj;
    }
    
    /**
     * Determines, if the specified message is a response.
     * 
     * @param protoMsg source message
     * @return {@code true} if specified message is response <br>
     *         {@code false} otherwise
     */
    public static boolean isResponse(short[] protoMsg) {
        int command = getCommand(protoMsg);
        return ((command & 0x80) == 0x80 );
    }
}
