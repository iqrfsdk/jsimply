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

package com.microrisc.simply.iqrf.dpa.asynchrony;

import com.microrisc.simply.AbstractMessage;
import com.microrisc.simply.asynchrony.BaseAsynchronousMessage;

/**
 * DPA asynchronous message.
 * <p>
 * Main data field is <b>all</b> bytes from the first byte(inclusive) behind the 
 * foursome (see IQRF DPA documentation) to the end of the message.
 * 
 * <p>
 * Additional data field constitutes the HW profile ID.
 * 
 * @author Michal Konopa
 */
public final class DPA_AsynchronousMessage extends BaseAsynchronousMessage {
    
    
    /**
     * Message source extended with information about peripheral ID. 
     */
    public static interface DPA_AsynchronousMessageSource 
    extends AbstractMessage.MessageSource 
    {
       /**
        * Returns number of source peripheral.
        * @return number of source peripheral
        */
       int getPeripheralNumber();
    }
    
    /**
     * Creates new object of DPA asynchronous message.
     * 
     * @param mainData main data - all bytes behind the foursome(see IQRF DPA documentation)
     * @param additionalData additional data - HWP ID
     * @param messageSource source of the message
     */
    public DPA_AsynchronousMessage(
            short[] mainData, Integer additionalData, DPA_AsynchronousMessageSource messageSource
    ) {
        super(mainData, additionalData, messageSource);
    }
    
    /**
     * Returns all bytes behind the foursome. 
     * @return all bytes behind the foursome.
     */
    @Override
    public short[] getMainData() {
        return (short[]) mainData;
    }
    
    /**
     * Returns HWP ID.
     * @return HWP ID
     */
    @Override
    public Integer getAdditionalData() {
        return (Integer) additionalData;
    }
    
    @Override
    public DPA_AsynchronousMessageSource getMessageSource() {
        return (DPA_AsynchronousMessageSource) messageSource;
    }
    
}
