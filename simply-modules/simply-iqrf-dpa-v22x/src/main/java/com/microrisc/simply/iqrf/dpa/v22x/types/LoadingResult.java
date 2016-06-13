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
package com.microrisc.simply.iqrf.dpa.v22x.types;

/**
 * Identify result of code loading.
 * 
 * @author Martin Strouhal
 */
public final class LoadingResult {
   
   private boolean result;
   
   /**
    * Creates result of loading code.
    * 
    * @param result must be {@code true} if all successful.
    */
   public LoadingResult(boolean result){
      this.result = result;
   }
   
   /**
    * Identify if code loading was successfully.
    * 
    * @return {@code true} if checksum was matched (and eventually code was
    * loaded), otherwise return {@code false}
    */
   public boolean getResult(){
      return result;
   }

   @Override
   public String toString() {
      return "Successfully loading = " + result;
   }
}
