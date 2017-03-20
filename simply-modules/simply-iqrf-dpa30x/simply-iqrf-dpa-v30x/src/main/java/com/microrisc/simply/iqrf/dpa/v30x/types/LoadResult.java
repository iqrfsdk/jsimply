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
package com.microrisc.simply.iqrf.dpa.v30x.types;

/**
 * Identify result of code loading.
 * 
 * @author Martin Strouhal
 */
public final class LoadResult {
   
    private boolean result;


    /**
     * Creates result of loading code.
     * 
     * @param result must be {@code true} if all successful.
     */
    public LoadResult(boolean result){
       this.result = result;
    }
   
    /**
     * Indicatses whether code loading was successful.
     * 
     * @return {@code true} if checksum was matched (and code was possibly loaded) <br>
     *         {@code false} otherwise
     */
    public boolean getResult(){
       return result;
    }

   @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append(" Result: " + result + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
}
