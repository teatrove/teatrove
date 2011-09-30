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
 * This class corresponds to the CONSTANT_String_info structure as defined in
 * section 4.4.3 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantStringInfo extends ConstantInfo {
    private String mStr;
    
    private ConstantUTFInfo mStringConstant;
    
    /** 
     * Will return either a new ConstantStringInfo object or one already in
     * the constant pool. If it is a new ConstantStringInfo, it will be 
     * inserted into the pool.
     */
    static ConstantStringInfo make(ConstantPool cp, String str) {
        ConstantInfo ci = new ConstantStringInfo(cp, str);
        return (ConstantStringInfo)cp.addConstant(ci);
    }
    
    ConstantStringInfo(ConstantUTFInfo constant) {
        super(TAG_STRING);
        mStr = constant.getValue();
        mStringConstant = constant;
    }

    private ConstantStringInfo(ConstantPool cp, String str) {
        super(TAG_STRING);
        mStr = str;
        mStringConstant = ConstantUTFInfo.make(cp, str);
    }
    
    public String getValue() {
        return mStr;
    }

    public int hashCode() {
        return mStr.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantStringInfo) {
            ConstantStringInfo other = (ConstantStringInfo)obj;
            return mStr.equals(other.mStr);
        }
        
        return false;
    }
    
    boolean hasPriority() {
        return true;
    }

    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mStringConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_String_info: " + getValue();
    }
}
