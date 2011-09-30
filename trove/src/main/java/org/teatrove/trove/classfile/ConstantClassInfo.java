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
 * This class corresponds to the CONSTANT_Class_info structure as defined in
 * section 4.4.1 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class ConstantClassInfo extends ConstantInfo {
    private final TypeDesc mType;
    private final ConstantUTFInfo mNameConstant;
    
    /** 
     * Will return either a new ConstantClassInfo object or one already in
     * the constant pool. If it is a new ConstantClassInfo, it will be inserted
     * into the pool.
     */
    static ConstantClassInfo make(ConstantPool cp, String className) {
        ConstantInfo ci = new ConstantClassInfo(cp, className);
        return (ConstantClassInfo)cp.addConstant(ci);
    }
    
    /** Used to describe an array class. */
    static ConstantClassInfo make(ConstantPool cp, String className, int dim) {
        ConstantInfo ci = new ConstantClassInfo(cp, className, dim);
        return (ConstantClassInfo)cp.addConstant(ci);
    }
    
    static ConstantClassInfo make(ConstantPool cp, TypeDesc type) {
        ConstantInfo ci = new ConstantClassInfo(cp, type);
        return (ConstantClassInfo)cp.addConstant(ci);
    }

    ConstantClassInfo(ConstantUTFInfo nameConstant) {
        super(TAG_CLASS);
        String name = nameConstant.getValue();
        if (!name.endsWith(";") && !name.startsWith("[")) {
            mType = TypeDesc.forClass(name);
        }
        else {
            mType = TypeDesc.forDescriptor(name);
        }
        mNameConstant = nameConstant;
    }

    private ConstantClassInfo(ConstantPool cp, String className) {
        super(TAG_CLASS);
        String desc = className.replace('.', '/');
        mType = TypeDesc.forClass(className);
        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }
    
    /** Used to describe an array class. */
    private ConstantClassInfo(ConstantPool cp, String className, int dim) {
        super(TAG_CLASS);
        TypeDesc type = TypeDesc.forClass(className);
        String desc;
        if (dim > 0) {
            while (--dim >= 0) {
                type = type.toArrayType();
            }
            desc = type.toString();
        }
        else {
            desc = className.replace('.', '/');
        }
        mType = type;
        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }
    
    private ConstantClassInfo(ConstantPool cp, TypeDesc type) {
        super(TAG_CLASS);
        String desc;
        if (type.isArray()) {
            desc = type.toString();
        }
        else {
            desc = type.getRootName().replace('.', '/');
        }
        mType = type;
        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }

    public TypeDesc getType() {
        return mType;
    }

    public int hashCode() {
        return mType.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ConstantClassInfo) {
            ConstantClassInfo other = (ConstantClassInfo)obj;
            return mType.equals(other.mType);
        }
        return false;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mNameConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_Class_info: ".concat(getType().getFullName());
    }
}
