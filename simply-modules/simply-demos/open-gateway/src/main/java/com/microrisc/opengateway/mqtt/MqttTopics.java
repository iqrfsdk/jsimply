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

package com.microrisc.opengateway.mqtt;

/**
 * MQTT topics.
 * 
 * @author Rostislav Spinar
 */
public final class MqttTopics {
    
    private final String rootTopic;
    
    private final String stdSensorsProtronix;
    private final String stdSensorsProtronixErrors;

    private final String stdActuatorsDevtech;
    private final String stdActuatorsDevtechErrors;
    
    private final String lpSensorsIqHome;
    private final String lpSensorsIqhomeErrors;
    
    private final String lpActuatorsTeco;
    private final String lpActuatorsTecoErrors;
    
    /**
     * Creates new object of MQTT topics.
     * @param rootTopic gw ID
     * @param stdSensorsProtronix
     * @param stdSensorsProtronixErrors
     * @param stdActuatorsDevtech
     * @param stdActuatorsDevtechErrors
     * @param lpSensorsIqhome,
     * @param lpSensorsIqhomeErrors,
     * @param lpActuatorsTeco
     * @param lpActuatorsTecoErrors
     */
    public MqttTopics(String rootTopic,
            String stdSensorsProtronix,
            String stdSensorsProtronixErrors,
            String stdActuatorsDevtech,
            String stdActuatorsDevtechErrors,
            String lpSensorsIqhome,
            String lpSensorsIqhomeErrors,
            String lpActuatorsTeco,
            String lpActuatorsTecoErrors) 
    {
        this.rootTopic = rootTopic;
        
        this.stdSensorsProtronix = rootTopic + stdSensorsProtronix;
        this.stdSensorsProtronixErrors = rootTopic + stdSensorsProtronixErrors;
        
        this.stdActuatorsDevtech = rootTopic + stdActuatorsDevtech;
        this.stdActuatorsDevtechErrors = rootTopic + stdActuatorsDevtechErrors;
        
        this.lpSensorsIqHome = rootTopic + lpSensorsIqhome;
        this.lpSensorsIqhomeErrors = rootTopic + lpSensorsIqhomeErrors;
        
        this.lpActuatorsTeco = rootTopic + lpActuatorsTeco; 
        this.lpActuatorsTecoErrors = rootTopic + lpActuatorsTecoErrors;
    }
    
    /**
     * @return the client ID
     */
    public String getGwId() {
        return rootTopic;
    }

    /**
     * @return the std sensors Protronix
     */
    public String getStdSensorsProtronix() {
        return stdSensorsProtronix;
    }

    /**
     * @return the std sensors Protronix
     */
    public String getStdSensorsProtronixErrors() {
        return stdSensorsProtronixErrors;
    }

    /**
     * @return the std actuator Devtech
     */
    public String getStdActuatorsDevtech() {
        return stdActuatorsDevtech;
    }

    /**
     * @return the std actuator Devtech
     */
    public String getStdActuatorsDevtechErrors() {
        return stdActuatorsDevtechErrors;
    }

    /**
     * @return the std sensor Iqhome
     */
    public String getLpSensorsIqHome() {
        return lpSensorsIqHome;
    }

    /**
     * @return the lp sensor Iqhome
     */
    public String getLpSensorsIqhomeErrors() {
        return lpSensorsIqhomeErrors;
    }

    /**
     * @return the lp actuators Teco
     */
    public String getLpActuatorsTeco() {
        return lpActuatorsTeco;
    }

    /**
     * @return the std actuator Teco
     */
    public String getLpActuatorsTecoErrors() {
        return lpActuatorsTecoErrors;
    }
}
