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

package com.microrisc.simply.iqrf.dpa.v30x.protocol;

import com.microrisc.simply.AbstractMessage;
import com.microrisc.simply.BaseCallResponse;
import com.microrisc.simply.CallRequest;
import com.microrisc.simply.NetworkData;
import com.microrisc.simply.NetworkLayerService;
import com.microrisc.simply.SimpleMessageSource;
import com.microrisc.simply.SimpleMethodMessageSource;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.BaseAsynchronousMessage;
import com.microrisc.simply.di_services.WaitingTimeoutService;
import com.microrisc.simply.errors.NetworkInternalError;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.SimpleDPA_AsynchronousMessageSource;
import com.microrisc.simply.iqrf.dpa.broadcasting.BroadcastRequest;
import com.microrisc.simply.iqrf.dpa.broadcasting.BroadcastResult;
import com.microrisc.simply.iqrf.dpa.v30x.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.v30x.devices.Coordinator;
import com.microrisc.simply.iqrf.dpa.v30x.di_services.method_id_transformers.CoordinatorStandardTransformer;
import com.microrisc.simply.iqrf.dpa.v30x.typeconvertors.DPA_ConfirmationConvertor;
import com.microrisc.simply.iqrf.dpa.v30x.types.DPA_Confirmation;
import com.microrisc.simply.iqrf.dpa.v30x.protocol.timing.TimingParamsStorage;
import com.microrisc.simply.network.BaseNetworkData;
import com.microrisc.simply.protocol.AbstractProtocolLayer;
import com.microrisc.simply.protocol.CallRequestComparator;
import com.microrisc.simply.protocol.MessageConvertor;
import com.microrisc.simply.protocol.SimpleRequestToResponseMatcher;
import com.microrisc.simply.typeconvertors.ValueConversionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microrisc.simply.iqrf.dpa.v30x.init.NetworkInfo;

/**
 * Protocol layer based on DPA_ProtocolProperties of IQRF.
 * 
 * @author Michal Konopa
 * @author Martin Strouhal
 */
//JUNE-2015 - improved determing and using RF mode
public final class DPA_ProtocolLayer 
extends AbstractProtocolLayer
implements ProtocolStateMachineListener
{
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPA_ProtocolLayer.class);
    
    
    
    
    /**
     * Binds sent requests with theirs time of sending.
     */
    private static class TimeRequest {
        // sent request
        CallRequest request;
        
        // time, at which the request was sent
        long sentTime;
        
        TimeRequest(CallRequest request, long sentTime) {
            this.request = request;
            this.sentTime = sentTime;
        }
        
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            String NEW_LINE = System.getProperty("line.separator");

            strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
            strBuilder.append(" Request: " + request + NEW_LINE);
            strBuilder.append(" Sent: " + sentTime + NEW_LINE);
            strBuilder.append("}");

            return strBuilder.toString();
        }
    }
    
    // information about potentionally time unlimited request - i.e. request,
    // whose response time is not inherently bounded, i.e. discovery
    private static class TimeUnlimitedRequestInfo {
        Class devIface;
        Set<String> methodIds;
        
        public TimeUnlimitedRequestInfo(Class devIface, Set<String> methodIds) {
            this.devIface = devIface;
            this.methodIds = methodIds;
        }
    }
    
    // map of time unlimited requests
    private static Map<Class, TimeUnlimitedRequestInfo> timeUnlimitedRequestsMap = new HashMap<>();
    
    // initialize set of time unlimited requests
    private static void initTimeUnlimitedRequests() {
        Set<String> coordUnlimitedOperations = new HashSet<>();
        coordUnlimitedOperations.add(
                CoordinatorStandardTransformer
                .getInstance()
                .transform(Coordinator.MethodID.RUN_DISCOVERY)
        );
        
        coordUnlimitedOperations.add(
                CoordinatorStandardTransformer
                .getInstance()
                .transform(Coordinator.MethodID.BOND_NODE)
        );
        
        timeUnlimitedRequestsMap.put(
                Coordinator.class,
                new TimeUnlimitedRequestInfo(
                        Coordinator.class,
                        coordUnlimitedOperations
                )
        );
        
        /*
        Set<String> frcUnlimitedOperations = new HashSet<>();
        frcUnlimitedOperations.add(
                FRCStandardTransformer
                .getInstance()
                .transform(FRC.MethodID.SEND)
        );

      
        frcUnlimitedOperations.add(
                FRCStandardTransformer
                .getInstance()
                .transform(FRC.MethodID.EXTRA_RESULT)
        );

        
        timeUnlimitedRequestsMap.put(
                FRC.class,
                new TimeUnlimitedRequestInfo(
                        FRC.class,
                        frcUnlimitedOperations
                )
        );
        */
    }
    
    // static initializer
    static {
        initTimeUnlimitedRequests();
    }   
    
    // indicates, whether the specified request is time unlimited
    private static boolean isTimeUnlimitedRequest(CallRequest request) {
        TimeUnlimitedRequestInfo unlimRequestInfo 
                = timeUnlimitedRequestsMap.get(request.getDeviceInterface());
        
        if ( unlimRequestInfo == null ) {
            return false;
        }
        
        return unlimRequestInfo.methodIds.contains(request.getMethodId());        
    }

    // indicates, whether a timeout is defined by user
    private static boolean isTimeoutDefinedByUserRequest(long procTime) {
        return procTime != WaitingTimeoutService.UNLIMITED_WAITING_TIMEOUT;
    }
    
    // indicates, whether a time unlimited request is in process
    private volatile boolean isTimeUnlimitedRequestInProcess = false;
    
    // indicates, whether a timeout is defined by user
    private volatile boolean isTimeoutDefinedByUserRequestInProcess = false;
    
    /** Last sent request. */
    private TimeRequest lastRequest = null;
    
    /** List of all requests, which was sent to network layer. */
    private List<TimeRequest> sentRequests = new LinkedList<>();
    
    /** Synchronization object for {@code sentRequest} data structure. */
    private final Object synchroSentRequest = new Object();
   
    // timing parameters manager
    private TimingParamsStorage timingParamsStorage;
    
    // for storing of request for timing params storage 
    private CallRequest requestForTiming = null;
    
    
    /**
     * For ensuring that sending a request to connected network together with 
     * performing of all needed settings including manipulation with Protocol Machine
     * will be executed all at once - without interruption of any other threads.
     */
    private final Object synchroSendOrReceive = new Object();
    
    
    /** Default maximal time duration [in ms] of sent requests in the protocol layer. */
    public static final long MAX_REQUEST_DURATION_DEFAULT = 10000;
    
    /** Maximal time duration [in ms] of sent requests in the protocol layer. */
    private long maxRequestDuration = MAX_REQUEST_DURATION_DEFAULT;
    
    
    /** State machine supporting DPA protocol communication. */
    private ProtocolStateMachine protoMachine = null;
    
    // state changed in protocol machine
    private final Object protoMachineStateChangeSignal = new Object();
    
    // type of errors encontered during communication with network layer
    private static enum COMMUNICATION_ERROR_TYPE {
        CONFIRMATION_TIMEOUTED,
        RESPONSE_TIMEOUTED,
        MACHINE_INTERNAL_ERROR
    }
    
    // waits before sending next request 
    private void doWaitBeforeSendRequest() throws InterruptedException {
        ProtocolStateMachine.State machineState = null;
        
        synchronized ( protoMachineStateChangeSignal ) {
            machineState = protoMachine.getState();
            while ( 
                    !protoMachine.isError()
                    && (machineState != ProtocolStateMachine.State.FREE_FOR_SEND)  
                    && (machineState != ProtocolStateMachine.State.WAITING_FOR_CONFIRMATION_ERROR)
                    && (machineState != ProtocolStateMachine.State.WAITING_FOR_RESPONSE_ERROR)
                  ) {
                protoMachineStateChangeSignal.wait();
                machineState = protoMachine.getState();
            }
        }
        
        if ( protoMachine.isError() ) {
            protoMachine.reset();
            return;
        }
        
        // checking if it is possible to send new request
        switch ( machineState ) {
            case WAITING_FOR_CONFIRMATION_ERROR:
            case WAITING_FOR_RESPONSE_ERROR:
                // reseting machine after error
                protoMachine.reset();
                break;
            case FREE_FOR_SEND:
                break;
            default:
                throw new IllegalStateException("State not expected: " + machineState);
        }
    }
    
    
    /** 
     * Deletes invalid requests. Request is invalid, if: <br> 
     * - its presence in the list exceeds maximal time duration limit
     * - is equal to specified new request
     */
    private void deleteInvalidRequests(CallRequest newRequest) {
        logger.debug("deleteInvalidRequests - start: newRequest={}", newRequest);
        
        synchronized ( synchroSentRequest ) {
            Iterator<TimeRequest> requestIt = sentRequests.iterator();
            while ( requestIt.hasNext() ) {
                TimeRequest sentRequest = requestIt.next();
                long requestDuration = System.currentTimeMillis() - sentRequest.sentTime;

                if ( requestDuration > maxRequestDuration ) {
                    logger.debug("removed request ( time exceed ): {}", sentRequest);
                    requestIt.remove();
                    continue;
                }

                if ( CallRequestComparator.areEqual(sentRequest.request, newRequest) ) {
                    logger.debug("removed request ( equality ) : {}", sentRequest);
                    requestIt.remove();
                }
            }
        }
        
        logger.debug("deleteInvalidRequests - end");
    }
    
    /**
     * Maintenace of sent request list.
     * @param callRequest new incomming request
     */
    private void maintainSentRequest(CallRequest callRequest) {
        deleteInvalidRequests(callRequest);
    }
    
    /**
     * Returns {@code true} if the specified response can be a response for 
     * specified request. Otherwise returns {@code false}.
     * @param request request to check
     * @param callResponse response to check
     * @return {@code true} if the specified response can be a reponse for 
     *                      specified request <br>
     *         {@code false}, otherwise
     */
    private boolean match(CallRequest request, BaseCallResponse callResponse) {
        return SimpleRequestToResponseMatcher.match(request, callResponse);
    }
    
    /**
     * Returns call request, which is the specified response the response on
     * that request. If no such request exists, returns {@code null}.
     * @param response
     * @return call request, which is the specified response the response on
     *         that request
     */
    private TimeRequest getCauseRequest(BaseCallResponse response) {
        logger.debug("getCauseRequest - start: response={}", response);
        
        for ( TimeRequest timeRequest : sentRequests) {
            if ( match(timeRequest.request, response) ) {
                logger.debug("getCauseRequest - end: {}", timeRequest.request);
                return timeRequest;
            }
        }
        
        logger.debug("getCauseRequest - end: null");
        return null;
    }
    
    /**
     * Processes specified message.
     * @param message message to process
     * @param msgPacket message source protocol packet
     */
    private void processMessage(AbstractMessage message) {
        logger.debug("processMessage - start: message={}", message);
        
        if ( !(message instanceof BaseCallResponse) ) {
            if ( message instanceof BaseAsynchronousMessage ) {
                // call a listener - must be synchronized because of broadcast responder thread
                synchronized ( synchroListener ) {
                    listener.onGetMessage(message);
                }
            } else {
                logger.warn("Unknown type of the message={}, discarded", message);
            }
            logger.debug("processMessage - end");
            return;
        }
        else { 
            synchronized ( synchroListener ) {
                listener.onGetMessage(message);
            }
            timingParamsStorage.updateTimingParams(requestForTiming, (BaseCallResponse)message);
        }
        
        logger.debug("processResponse - end");
    }
    
    // sends information about encountered error to the registered listener
    private void sendErrorMessage(COMMUNICATION_ERROR_TYPE errorType, TimeRequest causeRequest) {
        logger.debug("sendErrorMessage - start: causeRequest={}", causeRequest);
        
        String errorMsg = null;
        switch ( errorType ) {
            case CONFIRMATION_TIMEOUTED:
                errorMsg = "Confirmation timeouted";
                break;
            case RESPONSE_TIMEOUTED:
                errorMsg = "Response timeouted";
                break;
            case MACHINE_INTERNAL_ERROR:
                errorMsg = "Internal error";
                break;
        }
        
        BaseCallResponse errorResponse = new BaseCallResponse(
                new SimpleMethodMessageSource(
                        new SimpleMessageSource(
                                causeRequest.request.getNetworkId(), 
                                causeRequest.request.getNodeId()
                        ), 
                        causeRequest.request.getDeviceInterface(), 
                        causeRequest.request.getMethodId()
                       ), 
                new NetworkInternalError(errorMsg)
        );
        
        synchronized ( synchroSentRequest ) {
            errorResponse.setRequestId(causeRequest.request.getId());
            sentRequests.remove(causeRequest);
        }
        
        synchronized ( synchroListener ) {
            listener.onGetMessage(errorResponse);
        }
        
        logger.debug("sendErrorMessage - end");
    }
    
    // processes specified broadcast confirmation
    private void processBroadcastConfirmation(DPA_Confirmation confirmation) {
        BroadcastRequest request = (BroadcastRequest) lastRequest.request;
        BaseCallResponse response = new BaseCallResponse(
                BroadcastResult.OK, 
                null,
                new SimpleMethodMessageSource( 
                        new SimpleMessageSource(request.getNetworkId(), request.getNodeId()), 
                        request.getDeviceInterface(), 
                        request.getMethodId()
                ),
                null
        );
        response.setRequestId(request.getId());

        synchronized ( synchroListener ) {
            listener.onGetMessage(response);
        }
    }
    
    
    /** 
     * Synchronization object for listener. 
     * Listener will be called from incomming network thread and from broadcast
     * responder thread concurently.
     */
    private final Object synchroListener = new Object();
    
    
    // creates and returns asynchronous message corresponding to specified network data
    private static DPA_AsynchronousMessage createAsynchronousMessage(NetworkData networkData) {
        int nodeAddress = DPA_ProtocolProperties.getNodeAddress(networkData.getData());
        int perNum = DPA_ProtocolProperties.getPeripheralNumber(networkData.getData());
        short[] mainData = new short[
                            networkData.getData().length - DPA_ProtocolProperties.PDATA_START
                        ];
        System.arraycopy(networkData.getData(), DPA_ProtocolProperties.PDATA_START, 
                mainData, 0, mainData.length
        );
        
        return new DPA_AsynchronousMessage(
                mainData, DPA_ProtocolProperties.getHWPID(networkData.getData()),
                new SimpleDPA_AsynchronousMessageSource( 
                        new SimpleMessageSource(networkData.getNetworkId(), Integer.toString(nodeAddress)), 
                        perNum
                )
        );   
    }
    
    
    /**
     * Creates new protocol layer object with specified network layer to use.
     * Used RF mode will be set to <b>STD</b>.
     * @param networkLayerService network layer service object to use
     * @param msgConvertor message convertor to use
     */
    public DPA_ProtocolLayer(
            NetworkLayerService networkLayerService, 
            MessageConvertor msgConvertor
    ) {
        super(networkLayerService, msgConvertor);
        protoMachine = new ProtocolStateMachine();
        initTimeUnlimitedRequests();
        timingParamsStorage = new TimingParamsStorage();
    }    
    
    @Override
    public void onFreeForSend() {
        synchronized ( protoMachineStateChangeSignal ) {
            protoMachineStateChangeSignal.notifyAll();
        }
    }
    
    @Override
    public void onConfirmationTimeouted() {
        if ( listener != null ) {
            sendErrorMessage(COMMUNICATION_ERROR_TYPE.CONFIRMATION_TIMEOUTED, lastRequest);
        }
        
        synchronized ( protoMachineStateChangeSignal ) {
            protoMachineStateChangeSignal.notifyAll();
        }
    }
    
    @Override
    public void onResponseTimeouted() {
        if ( listener != null ) {
            sendErrorMessage(COMMUNICATION_ERROR_TYPE.RESPONSE_TIMEOUTED, lastRequest);
        }
        
        synchronized ( protoMachineStateChangeSignal ) {
            protoMachineStateChangeSignal.notifyAll();
        }
    }
    
    @Override
    public void onError() {
        if ( listener != null ) {
            sendErrorMessage(COMMUNICATION_ERROR_TYPE.MACHINE_INTERNAL_ERROR, lastRequest);
        }
        
        synchronized ( protoMachineStateChangeSignal ) {
            protoMachineStateChangeSignal.notifyAll();
        }
    }
    
    
    
    /**
     * This method works as follows: <br>
     * 1. Converts specified request into sequence of bytes. If an error has
     *    occured during the conversion, error of type {@code ProcessingRequestAtProtocolLayerError} 
     *    is created, error response is created and {@code processErrorResponse} 
     *    method is called.
     * 2. New {@code NetworkData} object is created and send into the network layer. 
     * 3. New {@code TimeRequest} object is created and stored into {@code sentRequest}.
     */
    @Override
    public void sendRequest(CallRequest request, long procTime) throws SimplyException {
        logger.debug("sendRequest - start: request={}, procTime={}", request, procTime);
        
        if ( listener == null ) {
            throw new SimplyException("No listener registered.");
        }
        
        // conversion to format used by application protocol
        short[] protoMsg = msgConvertor.convertToProtoFormat(request);
        
        // waiting until it is possible to send new request
        try {
            doWaitBeforeSendRequest();
        } catch ( InterruptedException ex ) {
            logger.warn(
                "Thread interrupted while waiting for sending next request."
                + "Request will not be sent", ex
            );
            return;
        } catch ( Exception ex ) {
            protoMachine.reset();
            throw new SimplyException(ex);
        }
        
        lastRequest = new TimeRequest(request, System.currentTimeMillis());
        
        // must be performed altogether to eliminating the case, when 
        // response comes to early from underlaying network layer
        synchronized ( synchroSendOrReceive ) {
            // maintenance of already sent requests
            maintainSentRequest(request);
            networkLayerService.sendData( new BaseNetworkData(protoMsg, request.getNetworkId()) );
            
            // broadcast requests are treated as NO TIME UNLIMITED
            if ( request instanceof BroadcastRequest ) {
                isTimeUnlimitedRequestInProcess = false;
                isTimeoutDefinedByUserRequestInProcess = false;
               
                try {
                    protoMachine.newRequest(request, timingParamsStorage.getTimingParams(request));
                } catch ( Exception ex ) {
                    throw new SimplyException(ex);
                } 
            } else {
                synchronized ( synchroSentRequest ) {
                    sentRequests.add( lastRequest );
                    
                    // TIME UNLIMITED requests go outside of Protocol State Machine
                    // because the machine works with precise limited timeouts
                    if ( isTimeUnlimitedRequest(request) ) {
                        isTimeUnlimitedRequestInProcess = true;
                    } else {
                        if ( isTimeoutDefinedByUserRequest(procTime) ) {
                            isTimeoutDefinedByUserRequestInProcess = true;
                        } else {
                            isTimeUnlimitedRequestInProcess = false;
                            isTimeoutDefinedByUserRequestInProcess = false;
                            
                            try {
                                protoMachine.newRequest(request, timingParamsStorage.getTimingParams(request));
                            } catch ( Exception ex ) {
                                throw new SimplyException(ex);
                            }
                        }
                    }
                    
                    // for timing parameters storage
                    requestForTiming = request;
                }
            }
        }
      
        logger.debug("sendRequest - end");
    }
    
    @Override
    public void sendRequest(CallRequest request) throws SimplyException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void start() throws SimplyException {
        logger.debug("start - start:");
        
        super.start();
        protoMachine.start();
        protoMachine.registerListener(this);
        
        logger.info("Started");
        logger.debug("start - end");
    }
    
    @Override
    public void destroy() {
        logger.debug("destroy - start:");
        
        super.destroy();
        
        sentRequests.clear();
        sentRequests = null;
        
        protoMachine.unregisterListener();
        protoMachine.destroy();
        protoMachine = null;
        
        timingParamsStorage = null;
        
        logger.info("Destroyed");
        logger.debug("destroy - end");
    }
    
    @Override
    public void onGetData(NetworkData networkData) {
        logger.debug("onGetData - start: {}", networkData);
        
        // no listener - nothing to do
        if ( listener == null ) {
            logger.debug("onGetData - end: no listener connected");
            return;
        }
        
        DPA_ResponseCode responseCode = null;
        try {
            responseCode = DPA_ProtocolProperties.getResponseCode(networkData.getData());
        } catch ( ValueConversionException ex ) {
            logger.error("Error in determining response code. Network data={}", networkData);
            logger.debug("onGetData - end");
            return;
        }
        
        // all confirmations are filtered out
        if ( responseCode == DPA_ResponseCode.CONFIRMATION ) {
            DPA_Confirmation confirmation = null;
            
            try {
                short[] confirmData = new short[
                            networkData.getData().length - DPA_ProtocolProperties.PDATA_START
                        ];
                System.arraycopy(networkData.getData(), DPA_ProtocolProperties.PDATA_START, 
                        confirmData, 0, confirmData.length
                );
                confirmation = (DPA_Confirmation)DPA_ConfirmationConvertor
                        .getInstance().toObject(confirmData);
            } catch ( ValueConversionException ex ) {
                logger.error("Error in conversion of confirmation. Network data={}", networkData);
                logger.debug("onGetData - end");
                return;
            }
            
            // if time unlimited request is NOT in process
            if ( !isTimeUnlimitedRequestInProcess ) {
                
                // if timeout was not defined by user, use standard DPA timing
                if ( !isTimeoutDefinedByUserRequestInProcess ) {
                    synchronized (synchroSendOrReceive) {
                        try {
                            protoMachine.confirmationReceived(confirmation);
                        } catch ( Exception ex ) {
                            logger.error("Internal error while confirmation reception: {}", ex);
                            logger.debug("onGetData - end");
                            return;
                        }

                        if (lastRequest.request instanceof BroadcastRequest) {
                            processBroadcastConfirmation(confirmation);
                        }
                    }
                }
            }

            logger.debug("onGetData - confirmation arrived: {}", networkData);
            return;
        }
        
        // creating message by conversion data comming from connected network
        AbstractMessage message = null;
        try {
            message = msgConvertor.convertToDOFormat(networkData);
        } catch ( SimplyException e ) {
            logger.error("Conversion error on incomming data {}", e);
            logger.debug("onGetData - end");
            return;
        }
        
        // processing of asynchronous messages
        if ( message instanceof BaseAsynchronousMessage ) {
            logger.info("New asynchronous message: {}", message);
            processMessage(message);

            logger.debug("onGetData - end");
            return;
        }
        
        // all messages, which are not responses or asynchronous are discarded
        if ( !(message instanceof BaseCallResponse) ) {
            logger.error("Unknown message: {}. Message will be discarded.", message);
            logger.debug("onGetData - end");
            return;
        }
        
        synchronized ( synchroSendOrReceive ) {
            synchronized ( synchroSentRequest ) {
                BaseCallResponse response = (BaseCallResponse)message;
                TimeRequest causeRequest = getCauseRequest(response);
            
                if ( causeRequest != null ) {
                    response.setRequestId(causeRequest.request.getId());
                    sentRequests.remove(causeRequest);
                } else {
                    logger.error("Cause request not found for response: {}. "
                        + "Response will be processed as asynchronous message", response
                    );
                    
                    DPA_AsynchronousMessage asyncResponse = createAsynchronousMessage(networkData); 
                    processMessage(asyncResponse);
                    
                    logger.debug("onGetData - end");
                    return;
                }
            }
            
            // if time unlimited request is NOT in process 
            if ( !isTimeUnlimitedRequestInProcess ) {
                
                // if timeout NOT defined by user
                if ( !isTimeoutDefinedByUserRequestInProcess ) {
                    try {
                        protoMachine.responseReceived(networkData.getData());
                    } catch ( Exception ex ) {
                        logger.error("Internal error while response reception: {}", ex);
                        logger.debug("onGetData - end");
                        return;
                    }
                }
            }
            
            // processing the message incomming from network
            processMessage(message);
        }
        
        logger.debug("onGetData - end");
    }
    
    /**
     * Configures the protocol layer according to specified information about network.
     * @param networkId ID of network which the information relate to
     * @param info information about network
     */
    public void configure(String networkId, NetworkInfo info){
        this.protoMachine.configure(networkId, info);
    }
    
    /** Checks specified maximal time duration. */
    private long checkMaxRequestDuration(long maxRequestDuration) {
        if ( maxRequestDuration < 0 ) {
            throw new IllegalArgumentException("Maximal duration of sent request"
                    + "must be nonnegative");
        }
        return maxRequestDuration;
    }
    
    /**
     * Sets maximal time duration [in ms] of sent requests in this protocol layer.
     * @param maxRequestDuration duration [in ms] to set
     * @throws IllegalArgumentException if the value is less then {@code 0}
     */
    public void setMaxRequestDuration(long maxRequestDuration) {
        this.maxRequestDuration = checkMaxRequestDuration(maxRequestDuration);
    }
}
