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
package com.microrisc.simply.network.mqtt;

import com.microrisc.simply.network.AbstractNetworkConnectionInfo;
import java.util.Objects;

/**
 * Base class of implementation of mqtt connection information.
 * 
 * @author Martin Strouhal
 */
public class BaseMQTTConnectionInfo extends AbstractNetworkConnectionInfo
        implements MQTTPortConnectionInfo {
    
    /** Mqtt server URI for access to the network. */
    protected String serverURI;
    /** Mqtt client id for access to the network. */
    protected String clientId;
    /** MAC address of remote server. */
    protected String remoteMAC;

    /**
     *  Constructor for {@link BaseMQTTPortConnectionInfo}
     * @param serverURI for access to the network
     * @param clientId for access to the network
     * @param mac address of remote server
     */
    public BaseMQTTConnectionInfo(String serverURI, String clientId, String mac) {
        this.serverURI = serverURI;
        this.clientId = clientId;
        this.remoteMAC = mac;
    }

    @Override
    public String getServerURI() {
        return serverURI;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getRemoteMAC() {
        return remoteMAC;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.serverURI);
        hash = 29 * hash + Objects.hashCode(this.clientId);
        hash = 29 * hash + Objects.hashCode(this.remoteMAC);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseMQTTConnectionInfo other = (BaseMQTTConnectionInfo) obj;
        if (!Objects.equals(this.serverURI, other.serverURI)) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        if (!Objects.equals(this.remoteMAC, other.remoteMAC)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BaseMQTTConnectionInfo{" + "serverURI=" + serverURI + ", clientId=" + clientId + ", remoteMAC=" + remoteMAC + '}';
    }
}
