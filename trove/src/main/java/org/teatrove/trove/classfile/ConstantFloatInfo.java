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
 * This class corresponds to the CONSTANT_Float_info structure as defined in
 * section 4.4.4 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantFloatInfo extends ConstantInfo {
    private Float mValue;
    
    /** 
     * Will return either a new ConstantFloatInfo object or one already in
     * the constant pool. If it is a new ConstantFloatInfo, it will be 
     * inserted into the pool.
     */
    static ConstantFloatInfo make(ConstantPool cp, float value) {
        ConstantInfo ci = new ConstantFloatInfo(value);
        return (ConstantFloatInfo)cp.addConstant(ci);
    }
    
    ConstantFloatInfo(float value) {
        super(TAG_FLOAT);
        mValue = new Float(value);
    }

    ConstantFloatInfo(Float value) {
        super(TAG_FLOAT);
        mValue = value;
    }
    
    public Float getValue() {
        return mValue;
    }

    public int hashCode() {
        return mValue.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantFloatInfo) {
            ConstantFloatInfo other = (ConstantFloatInfo)obj;
            return mValue.equals(other.mValue);
        }
        
        return false;
    }
    
    boolean hasPriority() {
        return true;
    }

    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeFloat(mValue.floatValue());
    }

    public String toString() {
        return "CONSTANT_Float_info: " + getValue();
    }
}
