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

package com.microrisc.simply.iqrf.dpa.v30x.di_services;

import com.microrisc.simply.di_services.StandardServices;
import com.microrisc.simply.iqrf.dpa.di_services.HwProfileService;
  
/**
 * DPA Standard services for Device Interfaces
 * 
 * @author Michal Konopa
 */
public interface DPA_StandardServices 
extends StandardServices, DPA_AdditionalInfoService, HwProfileService {
}