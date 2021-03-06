/*
 * Copyright 2014 MICRORISC s.r.o..
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

/**
 * Listener of {@link ProtocolStateMachine} class.
 * 
 * @author Michal Konopa
 */
public interface ProtocolStateMachineListener {
    /**
     * Notifies, that it is possible to send next request.
     */
    void onFreeForSend();
    
    /**
     * Notifies, that waiting for confirmation arrival timeouted.
     */
    void onConfirmationTimeouted();
    
    /**
     * Notifies, that waiting for response arrival timeouted.
     */
    void onResponseTimeouted();
    
    /**
     * Notifies, that machine internal error occured. 
     */
    void onError();
}
