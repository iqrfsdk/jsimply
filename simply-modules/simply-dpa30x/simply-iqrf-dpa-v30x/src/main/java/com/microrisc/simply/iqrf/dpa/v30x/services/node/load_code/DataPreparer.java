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
package com.microrisc.simply.iqrf.dpa.v30x.services.node.load_code;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares hex data in parts with effective size given to IQRF.
 *
 * @author Martin Strouhal
 * @author Michal Konopa
 */
public final class DataPreparer {

    // size of the smallest part of data which we are dividing - we are 
    // dividing data on 48's part and 16's part
    static final int SMALLEST_PART_SIZE = 16;
   
    // size of block
    private static final int BLOCK_SIZE = 64;
    
    private final CodeBlock handlerBlock;
    private ByteBuffer data;
    private static final Logger logger = LoggerFactory.getLogger(DataPreparer.class);

    public DataPreparer(CodeBlock handlerBlock, IntelHex file) {
        logger.debug("DataPreparer - new instance: handlerBlock={}, file={}", handlerBlock, file);
        this.handlerBlock = handlerBlock;
        this.data = file.getData();
    }
   
    public DataPreparer(byte[] data){
        logger.debug("DataPreparer - new instance: data={}", Arrays.toString(data));
        this.handlerBlock = new CodeBlock(0, data.length);
        this.data = ByteBuffer.wrap(data);
    }


    /**
     * Returns array of short[] prepared to effective writing into memory
     *
     * @return array of short[]
     */
    public short[][] prepare() {
        logger.debug("prepare - start");
      
        List<Short[]> list = new LinkedList<>();
      
        for (
            long address = handlerBlock.getAddressStart() / SMALLEST_PART_SIZE;
            address < handlerBlock.getAddressEnd() / SMALLEST_PART_SIZE;
            address += SMALLEST_PART_SIZE / 2
        ) {
            list.add(getDataPart(address, 0, 3, SMALLEST_PART_SIZE));
            list.add(getDataPart(address, 3, 4, SMALLEST_PART_SIZE));
            list.add(getDataPart(address, 4, 5, SMALLEST_PART_SIZE));
            list.add(getDataPart(address, 5, 8, SMALLEST_PART_SIZE));
        }
      
        short[][] resultData = new short[list.size()][];
        for (int i = 0; i < resultData.length; i++) {
            Short[] dataPart = list.get(i);
            resultData[i] = new short[dataPart.length];
            for (int j = 0; j < dataPart.length; j++) {
               resultData[i][j] = dataPart[j];
            }
        }
      
        if ( logger.isDebugEnabled() ) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("{\n");
            for (short[] resultData1 : resultData) {
                sb.append("(length: ");
                sb.append(resultData1.length);
                sb.append(") > ");
                sb.append(Arrays.toString(resultData1));
                sb.append("\n");
            }
            sb.append("\n}");
            
            logger.debug("prepare - end: {}", sb.toString());
        }
      
        return resultData;
    }
   
   /**
    * Returns array of 16-length blocks prepared to effective writing into memory.
    *
    * @return array of 16-length blocks
    */
    public short[][] prepareAs16BytesBlocks() {
        logger.debug("prepareAs16BytesBlocks - start");
      
        List<Short[]> blockList = new LinkedList<>();
        long address = handlerBlock.getAddressStart() / SMALLEST_PART_SIZE;
        
        // number of used smallest parts
        int partsNum = 0;
        
        for (
            ;
            address < handlerBlock.getAddressEnd() / SMALLEST_PART_SIZE;
            address++
        ) {
            blockList.add(getDataPart(address, 0, 1, SMALLEST_PART_SIZE));
            partsNum++;
        }
        
        // it is necessary to also add bytes, which are past of last complete block boundary
        if ( (handlerBlock.getAddressEnd() % SMALLEST_PART_SIZE) != 0 ) {
            blockList.add(getDataPart(address, 0, 1, SMALLEST_PART_SIZE));
            address++;
            partsNum++;
        }
        
        // it is necessary to add complete BLOCK of data to align with DPA specification
        while ( ((partsNum * SMALLEST_PART_SIZE) % BLOCK_SIZE) != 0 ) {
            blockList.add(getDataPart(address, 0, 1, SMALLEST_PART_SIZE));
            address++;
            partsNum++;
        }
        
        short[][] resultData = new short[blockList.size()][];
        for ( int i = 0; i < resultData.length; i++ ) {
            Short[] block = blockList.get(i);
            resultData[i] = new short[block.length];
            for ( int j = 0; j < block.length; j++ ) {
                resultData[i][j] = block[j];
            }
        }

        if ( logger.isDebugEnabled() ) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("{\n");
            for (short[] resultData1 : resultData) {
                sb.append("(length: ");
                sb.append(resultData1.length);
                sb.append(") > ");
                sb.append(Arrays.toString(resultData1));
                sb.append("\n");
            }
            sb.append("\n}");
            
            logger.debug("prepareAs16BytesBlocks - end: {}", sb.toString());
        }
      
        return resultData;
    }
     
   
    /**
     * Returns data part.
     *
     * @param address of data
     * @param startOffsetIndex start index of offset of data part
     * @param endOffsetIndex end index of offset of data part
     * @param smallestPartSize used while counting
     * @return short[]
     */
    private Short[] getDataPart(
            long address, int startOffsetIndex, int endOffsetIndex, int smallestPartSize
    ) {
        int dataPartLength = (endOffsetIndex - startOffsetIndex) * smallestPartSize;
        Short[] dataPart = new Short[dataPartLength];
        int dataIndex = 0;
        
        for (long j = (address + startOffsetIndex) * smallestPartSize;
                j < (address + endOffsetIndex) * smallestPartSize; 
                j++
        ) {
           dataPart[dataIndex] = getCheckedValue(j);
           dataIndex++;
        }
        
        return dataPart;
    }

    /** Return value if it's in range, otherwise returns 0x34FF. */
    private short getCheckedValue(long address) {
        if (address > handlerBlock.getAddressEnd()) {
           return 0x34FF;
        } else {
           return (short) (data.get((int) address) & 0xFF);
        }
    }
}