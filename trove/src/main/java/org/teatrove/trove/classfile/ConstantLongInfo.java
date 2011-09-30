/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.trove.classfile;

import java.io.*;

/**
 * This class corresponds to the CONSTANT_Long_info structure as defined in
 * section 4.4.5 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantLongInfo extends ConstantInfo {
    private Long mValue;
    
    /** 
     * Will return either a new ConstantLongInfo object or one already in
     * the constant pool. If it is a new ConstantLongInfo, it will be 
     * inserted into the pool.
     */
    static ConstantLongInfo make(ConstantPool cp, long value) {
        ConstantInfo ci = new ConstantLongInfo(value);
        return (ConstantLongInfo)cp.addConstant(ci);
    }
    
    ConstantLongInfo(long value) {
        super(TAG_LONG);
        mValue = new Long(value);
    }
    
    ConstantLongInfo(Long value) {
        super(TAG_LONG);
        mValue = value;
    }
    
    public Long getValue() {
        return mValue;
    }

    public int hashCode() {
        return mValue.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantLongInfo) {
            ConstantLongInfo other = (ConstantLongInfo)obj;
            return mValue.equals(other.mValue);
        }

        return false;
    }
    
    int getEntryCount() {
        return 2;
    }

    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeLong(mValue.longValue());
    }

    public String toString() {
        return "CONSTANT_Long_info: " + getValue();
    }
}
