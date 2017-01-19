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
package com.microrisc.simply.services.network;

/**
 * Stores information needed to create one particular network context service.
 * 
 * @author Michal Konopa
 */
public final class ServiceCreationInfo {
    
    // service factory
    private final ServiceFactory serviceFactory;

    // service arguments
    private final Object[] serviceArgs;


    /**
     * Creates new object storing info about how to create a service.
     * 
     * @param serviceFactory factory to use
     * @param serviceArgs arguments for factory
     */
    public ServiceCreationInfo(ServiceFactory serviceFactory, Object[] serviceArgs) {
        this.serviceFactory = serviceFactory;
        this.serviceArgs = serviceArgs;
    }

    /**
     * @return the service factory
     */
    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    /**
     * @return the service arguments
     */
    public Object[] getServiceArgs() {
        return serviceArgs;
    }
}
