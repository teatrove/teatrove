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

import java.util.*;
import java.io.*;

/**
 * This class corresponds to the InnerClasses_attribute structure introduced in
 * JDK1.1. It is not defined in the first edition of 
 * <i>The Java Virual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
class InnerClassesAttr extends Attribute {
    private List mInnerClasses = new ArrayList();

    public InnerClassesAttr(ConstantPool cp) {
        super(cp, INNER_CLASSES);
    }

    /**
     * @param inner The full inner class name
     * @param outer The full outer class name
     * @param name The simple name of the inner class, or null if anonymous
     * @param modifiers Modifiers for the inner class
     */
    public void addInnerClass(String inner,
                              String outer,
                              String name,
                              Modifiers modifiers) {
        
        ConstantClassInfo innerInfo = ConstantClassInfo.make(mCp, inner);
        ConstantClassInfo outerInfo; 
        if (outer == null) {
            outerInfo = null;
        }
        else {
            outerInfo = ConstantClassInfo.make(mCp, outer);
        }

        ConstantUTFInfo nameInfo;
        if (name == null) {
            nameInfo = null;
        }
        else {
            nameInfo = ConstantUTFInfo.make(mCp, name);
        }

        mInnerClasses.add(new Info(innerInfo, outerInfo, nameInfo, 
                                   modifiers.getModifier()));
    }

    public Info[] getInnerClassesInfo() {
        Info[] infos = new Info[mInnerClasses.size()];
        return (Info[])mInnerClasses.toArray(infos);
    }

    public int getLength() {
        return 2 + 8 * mInnerClasses.size();
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mInnerClasses.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            ((Info)mInnerClasses.get(i)).writeTo(dout);
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        InnerClassesAttr innerClasses = new InnerClassesAttr(cp);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int inner_index = din.readUnsignedShort();
            int outer_index = din.readUnsignedShort();
            int name_index = din.readUnsignedShort();
            int af = din.readUnsignedShort();
            
            ConstantClassInfo inner;
            if (inner_index == 0) {
                inner = null;
            }
            else {
                inner = (ConstantClassInfo)cp.getConstant(inner_index);
            }

            ConstantClassInfo outer;
            if (outer_index == 0) {
                outer = null;
            }
            else {
                outer = (ConstantClassInfo)cp.getConstant(outer_index);
            }
            
            ConstantUTFInfo innerName;
            if (name_index == 0) {
                innerName = null;
            }
            else {
                innerName = (ConstantUTFInfo)cp.getConstant(name_index);
            }

            Info info = new Info(inner, outer, innerName, af);
            innerClasses.mInnerClasses.add(info);
        }

        return innerClasses;
    }

    public static class Info {
        private ConstantClassInfo mInner;
        private ConstantClassInfo mOuter;
        private ConstantUTFInfo mName;
        private int mModifier;

        Info(ConstantClassInfo inner,
             ConstantClassInfo outer,
             ConstantUTFInfo name,
             int modifier) {

            mInner = inner;
            mOuter = outer;
            mName = name;
            mModifier = modifier;
        }

        /**
         * Returns null if no inner class specified.
         */
        public ConstantClassInfo getInnerClass() {
            return mInner;
        }

        /**
         * Returns null if no outer class specified.
         */
        public ConstantClassInfo getOuterClass() {
            return mOuter;
        }

        /**
         * Returns null if no inner class specified or is anonymous.
         */
        public String getInnerClassName() {
            if (mName == null) {
                return null;
            }
            else {
                return mName.getValue();
            }
        }

        /**
         * Returns a copy of the modifiers.
         */
        public Modifiers getModifiers() {
            return new Modifiers(mModifier);
        }

        public void writeTo(DataOutput dout) throws IOException {
            if (mInner == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mInner.getIndex());
            }

            if (mOuter == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mOuter.getIndex());
            }

            if (mName == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mName.getIndex());
            }
            
            dout.writeShort(mModifier);
        }
    }
}
