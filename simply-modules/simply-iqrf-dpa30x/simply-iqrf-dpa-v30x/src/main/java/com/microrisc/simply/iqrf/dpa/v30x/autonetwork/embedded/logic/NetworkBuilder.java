/*
 * Copyright 2015 MICRORISC s.r.o.
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
package com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.logic;

import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.asynchrony.AsynchronousMessagingManager;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessageProperties;
import com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.def.AutonetworkPeripheral;
import com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.def.AutonetworkState;
import com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.def.AutonetworkStateConvertor;
import com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.def.AutonetworkStateType;
import com.microrisc.simply.iqrf.dpa.v30x.autonetwork.embedded.def.ValueProperties;
import com.microrisc.simply.iqrf.dpa.v30x.devices.EEPROM;
import com.microrisc.simply.iqrf.dpa.v30x.devices.RAM;
import com.microrisc.simply.iqrf.dpa.v30x.typeconvertors.RemotelyBondedModuleIdConvertor;
import com.microrisc.simply.iqrf.dpa.v30x.types.RemotelyBondedModuleId;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.typeconvertors.ValueConversionException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for automatic network building via Autonetwork
 * embedded.
 * <p>
 * @author Martin Strouhal
 */
public final class NetworkBuilder implements
        AsynchronousMessagesListener<DPA_AsynchronousMessage> {

    /** Identify actual state of network building. */
    public static enum AlgorithmState {
       NON_STARTED, RUNNING, FINISHED;
    }

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(NetworkBuilder.class);
    

    private AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager;
    private List<AutonetworkStateListener> autonetworkListeners;
    private Node coord;
    private AutonetworkPeripheral autonetworkPer;
    private NodeApprover approver;
    private AlgorithmState algorithmState = AlgorithmState.NON_STARTED;
    private AutonetworkBuildingListener buildingListener;
    
    /** 
     * Flag for using approver - when isn't accessible correct peripheral, 
     * it can't be used. 
     */
    private boolean disallowedApprover = false;

   /**
    * Creates and init new instance of {@link NetworkBuilder}.<br>
    * <b>Default values are used:</b>
    * <table border="1">
    *    <tr>
    *       <th>Value name</th>
    *       <th>Value</th>
    *    </tr>
    *    <tr>
    *       <td>Discovery TX power</td>
    *       <td>7</td>
    *    </tr>
    *    <tr>
    *       <td>Bonding time</td>
    *       <td>8</td>
    *    </tr>
    *    <tr>
    *       <td>Temporary address timeout</td>
    *       <td>3</td>
    *    </tr>
    *    <tr>
    *       <td>Unbond and restart</td>
    *       <td>true</td>
    *    </tr>
    * </table>
    * 
    * @param sourceNetwork which contains at least Coordinator
    * @param asyncManager for receing async events
    * @throws IllegalArgumentException if: <br>
    *         - {@code sourceNetwork} is {@code null} <br>
    *         - {@code asyncManager} is {@code null}
    * @throws Exception if other error encountered during creation 
    */
   public NetworkBuilder(
           Network sourceNetwork,
           AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager
   ) throws Exception {
        logger.debug(
                "<init> - start: sourceNetwork={}, asyncManager={}", 
                sourceNetwork, asyncManager
        );
        
        if ( sourceNetwork == null ) {
           throw new IllegalArgumentException("Source network cannot be null");
        }
        
        if ( asyncManager == null ) {
           throw new IllegalArgumentException("Async manager cannot be null");
        }

        // getting coordinator
        coord = sourceNetwork.getNode("0");
        if ( coord == null ) {
           throw new IllegalArgumentException("Coordinator doesn't exist");
        }

        // register the listener of asynchronous messages
        this.asyncManager = asyncManager;
        this.asyncManager.registerAsyncMsgListener(this);

        autonetworkListeners = new LinkedList<>();
        // add native listener for dynamic network building
        buildingListener = new AutonetworkBuildingListener(sourceNetwork);
        autonetworkListeners.add(buildingListener);
      
        // get peripheral for approver, if isn't accesible than disallow his using
        autonetworkPer = coord.getDeviceObject(AutonetworkPeripheral.class);
        if ( autonetworkPer == null ) {
           logger.warn("Autonetwork peripheral doesn't exist on Coordinator! Approver cannot be used!");
           disallowedApprover = true;
        }
      
        // set default values
        setValue(ValueProperties.DISCOVERY_TX_POWER, 7);
        setValue(ValueProperties.BONDING_TIME, 8);
        setValue(ValueProperties.TEMPORARY_ADDRESS_TIMEOUT, 3);
        setValue(ValueProperties.UNBOND_AND_RESTART, true);
        
        logger.info("Initialization of network builder was completed.");
        logger.debug("<init> - end");
   }

   
    /**
     * Sets value of specified type for autonetwork algorithm.
     * 
     * @param valueProp properties of the value
     * @param value value, which should be set
     * @throws java.lang.Exception if an error encountered
     *
     * @throws IllegalArgumentException thrown when:
     * <ul>
     * <li>{@code value} doesn't have correct data type</li>
     * <li>or if it is a setting approver, which can be used because autonetwork
     *     peripheral doesn't exist on coordinator</li>
     * </ul>
     */
    public void setValue(ValueProperties valueProp, Object value) throws Exception {
        logger.debug("setValue - start");
        
        Class dataType = valueProp.getDataType();
        
        if ( !dataType.isAssignableFrom(value.getClass()) ) {
            throw new IllegalArgumentException("Illegal DataType of config value.");
        }
        
        if ( dataType.equals(Integer.class) ) {
            setNumberValue((int) value, valueProp.getBytePos());
            logger.debug("setValue - end");
            return;
        } else if (dataType.equals(Boolean.class)) {
            setBooleanValue((boolean) value, valueProp.getBytePos(), valueProp.getBitPos());
            logger.debug("setValue - end");
            return;
        } else if (NodeApprover.class.isAssignableFrom(value.getClass())) {
            if ( disallowedApprover ) {
                throw new IllegalArgumentException(
                        "Autonetwork peripheral couldn't get from Coordinator. "
                        + "Approver cannot be used!"
                );
            } else {
                this.approver = (NodeApprover) value;
                setBooleanValue(true, valueProp.getBytePos(), valueProp.getBitPos());
                logger.debug("setValue - end");
                return;
            }
        }
   }
   
    /**
     * Start autonetwork with specified count of waves and bond nodes approved by
     * {@link NodeApprover} (if was set).
     * 
     * @param countOfWaves count of waves
     * @throws IllegalStateException if an error encountered during start
     */
    public void startAutonetwork(int countOfWaves) {
        logger.debug("startAutonetwork - start: countOfWaves={}", countOfWaves);

        // getting RAM DI
        RAM ram = coord.getDeviceObject(RAM.class);
        if ( ram == null ) {
           throw new IllegalStateException("RAM doesn't exist on Coordinator!");
        }

        // start autonetwork on Coordinator
        VoidType writeResult = ram.write(0x00, new short[]{(short) countOfWaves});
        if ( writeResult == null ) {
           throw new IllegalStateException("Writting data into RAM failed\n"); 
        }
        
        algorithmState = AlgorithmState.RUNNING;
        
        logger.debug("startAutonetwork - end");
    }
  
    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {
        logger.debug("onAsynchronousMessage - start: message={}", message);
        
        if ( !(message.getMainData() instanceof short[]) ) {
            logger.warn("Message data not of short[] type");
            logger.debug("onAsynchronousMessage - end");
        }
                 
        short[] mainData = (short[])message.getMainData();
        short[] stateData = Arrays.copyOfRange(mainData, 2, mainData.length);

        // conversion to autonetwork state
        Object state = null;
        try {
            state = AutonetworkStateConvertor.getInstance().toObject(stateData);
        } catch ( ValueConversionException ex ) {
            logger.debug(ex.getMessage());
        }

        // if conversion was successful, process it
        if (
            (state != null) && (state instanceof AutonetworkState)
            && ((AutonetworkState)state).getType() != AutonetworkStateType.UNKNOWN 
        ) {              
            logger.debug("autonetwork msg - start");
            AutonetworkState actualState = (AutonetworkState) state;

            logger.info("Autonetwork message: " + actualState);

            // checks if algorithm is on the end
            if ( isAlgorithmFinished(actualState)) {
                algorithmState = AlgorithmState.FINISHED;
            }

            // call autonetwork listeners
            for ( AutonetworkStateListener listener : autonetworkListeners ) {
                listener.onAutonetworkState(actualState);
            }

        // if conversion wasn't successful and approver isn't null, the data can be
        // remotely bonded module id for aprroving > try it
        } else if ( approver != null ) {          
            RemotelyBondedModuleId bondedModuleId = null;
            try {
                bondedModuleId = (RemotelyBondedModuleId)RemotelyBondedModuleIdConvertor
                        .getInstance().toObject(stateData);
            } catch ( ValueConversionException ex ) {
                logger.warn("Unsupported async message: " + Arrays.toString(mainData));
                logger.debug("onAsynchronousMessage - end");
                return;
            }

            boolean approveResult = approver.approveNode(bondedModuleId);
            if ( approveResult ) {
                autonetworkPer.async_approve();
            } else {
                autonetworkPer.async_disapprove();
            }        
        } else {
            logger.debug("Received unsupported async msg: " + Arrays.toString(mainData));
        }
                                          
        logger.debug("onAsynchronousMessage - end");
    }

    /**
     * Returns actual state of algorithm.
     * @return actual state, see {@link AlgorithmState}
     */
    public AlgorithmState getAlgorithmState() {
       return algorithmState;
    }

    /** 
     * Create actual copy of built network.
     * @return copy of actual network
     */
    public Network getNetwork() {
        logger.debug("getNetwork - start");

        if ( algorithmState != AlgorithmState.FINISHED ) {
           throw new IllegalStateException("Algorithm is running still!");
        }
        Network network = buildingListener.getNetworkCopy();

        logger.debug("getNetwork - end: {}", network);
        return network;
    }

    /** Free up used resources. */
    public void destroy() {
        logger.debug("destroy - start");
        
        asyncManager.unregisterAsyncMsgListener(this);
        for ( AutonetworkStateListener autonetworkListener : autonetworkListeners ) {
           autonetworkListener.destroy();
        }
        autonetworkListeners.clear();
        
        logger.debug("destroy - end");
    }
   
    // checks if algorithm was succesfully finished
    private boolean isAlgorithmFinished(AutonetworkState state) {
        logger.debug("isAlgorithmFinished - start: state={}", state);
        
        if ( state.getType() == AutonetworkStateType.S_START ) {
            try {
                if ( state.getAdditionalData(1) == 0 ) {
                    logger.debug("isAlgorithmFinished - end: {}", true);
                    return true;
                }
            } catch ( IllegalArgumentException ex ) {
                logger.warn("Received autonetwork message doesn't contain count of remaing waves.");
            }
        } else {
            logger.warn("AutonetworkState doesn't contain information about count of remaing waves.");
        }
        
        logger.debug("isAlgorithmFinished - end: {}", false);
        return false;
    }
   
    private void setNumberValue(int value, int pos) throws Exception {
        logger.debug("setNumberValue - start: value={}, pos={}", value, pos);
        
        // getting EEEPROM DI
        EEPROM eeprom = coord.getDeviceObject(EEPROM.class);
        if ( eeprom == null ) {
           throw new Exception("EEEPROM doesn't exist on Coordinator!");
        }

        // writing configuration
        VoidType result = eeprom.write(pos, new short[]{(short) value});
        if ( result == null ) {
            throw new Exception("Write failed.");
        }
        
        //TODO check result
        logger.debug("setNumberValue - end");
    }
   
    private void setBooleanValue(boolean value, int bytePos, int bitPos) throws Exception {
        logger.debug(
                "setBooleanValue - start: value={}, bytePos={}, bitPos={}", 
                value, bytePos, bitPos
        );

        // getting EEEPROM DI
        EEPROM eeprom = coord.getDeviceObject(EEPROM.class);
        if ( eeprom == null ) {
           throw new Exception("EEEPROM doesn't exist on Coordinator!");
        }

        short shortValue = value == true ? (short) 1 : (short) 0;
        shortValue <<= bitPos;

        // writing configuration
        VoidType result = eeprom.write(bytePos, new short[]{shortValue});
        if ( result == null ) {
            throw new Exception("Write failed.");
        }

        //TODO check result
        logger.debug("setBooleanValue - end");
    }
}