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
 * This class corresponds to the CONSTANT_NameAndType_info structure as defined
 * in section 4.4.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantNameAndTypeInfo extends ConstantInfo {
    private String mName;
    private Descriptor mType;
    
    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    /** 
     * Will return either a new ConstantNameAndTypeInfo object or one 
     * already in the constant pool. 
     * If it is a new ConstantNameAndTypeInfo, it will be inserted
     * into the pool.
     */
    static ConstantNameAndTypeInfo make(ConstantPool cp, 
                                        String name,
                                        Descriptor type) {
        ConstantInfo ci = new ConstantNameAndTypeInfo(cp, name, type);
        return (ConstantNameAndTypeInfo)cp.addConstant(ci);
    }
    
    ConstantNameAndTypeInfo(ConstantUTFInfo nameConstant,
                            ConstantUTFInfo descConstant) {
        super(TAG_NAME_AND_TYPE);
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;

        mName = nameConstant.getValue();
        mType = Descriptor.parse(descConstant.getValue());
    }

    private ConstantNameAndTypeInfo(ConstantPool cp, 
                                    String name, 
                                    Descriptor type) {
        super(TAG_NAME_AND_TYPE);
        mName = name;
        mType = type;
        
        mNameConstant = ConstantUTFInfo.make(cp, name);
        mDescriptorConstant = ConstantUTFInfo.make(cp, mType.toString());
    }
    
    public String getName() {
        return mName;
    }

    public Descriptor getType() {
        return mType;
    }

    public int hashCode() {
        return mName.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantNameAndTypeInfo) {
            ConstantNameAndTypeInfo other = (ConstantNameAndTypeInfo)obj;
            return mName.equals(other.mName) && 
                mType.toString().equals(other.mType.toString());
        }
        
        return false;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_NameAndType_info: " + getName() + ", " + getType();
    }
}
