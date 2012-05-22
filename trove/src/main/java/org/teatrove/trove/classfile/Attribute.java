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
 * This class corresponds to the attribute_info structure defined in section
 * 4.7 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill, Nick Hagan
 * @see ClassFile
 */
public abstract class Attribute {
    final static Attribute[] NO_ATTRIBUTES = new Attribute[0];

    final static String CODE = "Code";
    final static String CONSTANT_VALUE = "ConstantValue";
    final static String DEPRECATED = "Deprecated";
    final static String EXCEPTIONS = "Exceptions";
    final static String INNER_CLASSES = "InnerClasses";
    final static String LINE_NUMBER_TABLE = "LineNumberTable";
    final static String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
    final static String SOURCE_FILE = "SourceFile";
    final static String SYNTHETIC = "Synthetic";
    final static String SIGNATURE = "Signature";
    final static String RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    final static String RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";
    final static String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParamaterAnnotations";
    final static String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParamaterAnnotations";

    /** The ConstantPool that this attribute is defined against. */
    protected final ConstantPool mCp;

    private String mName;
    private ConstantUTFInfo mNameConstant;

    protected Attribute(ConstantPool cp, String name) {
        mCp = cp;
        mName = name;
        mNameConstant = ConstantUTFInfo.make(cp, name);
    }

    /**
     * Returns the ConstantPool that this attribute is defined against.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }

    /**
     * Returns the name of this attribute.
     */
    public String getName() {
        return mName;
    }

    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }

    /**
     * Some attributes have sub-attributes. Default implementation returns an
     * empty array.
     */
    public Attribute[] getAttributes() {
        return NO_ATTRIBUTES;
    }

    /**
     * Returns the length (in bytes) of this attribute in the class file.
     */
    public abstract int getLength();

    /**
     * This method writes the 16 bit name constant index followed by the
     * 32 bit attribute length, followed by the attribute specific data.
     */
    public final void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mNameConstant.getIndex());
        dout.writeInt(getLength());
        writeDataTo(dout);
    }

    /**
     * Write just the attribute specific data. The default implementation
     * writes nothing.
     */
    public void writeDataTo(DataOutput dout) throws IOException {
    }

    /**
     * @param attrFactory optional factory for reading custom attributes
     */
    public static Attribute readFrom(ConstantPool cp,
                                     DataInput din,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        int index = din.readUnsignedShort();
        String name = ((ConstantUTFInfo)cp.getConstant(index)).getValue();
        int length = din.readInt();

        attrFactory = new Factory(attrFactory);
        return attrFactory.createAttribute(cp, name, length, din);
    }

    private static class Factory implements AttributeFactory {

        private final AttributeFactory mAttrFactory;

	    public Factory(AttributeFactory attrFactory) {
	            mAttrFactory = attrFactory;
	    }
	
	    public Attribute createAttribute(ConstantPool cp,
	                                         String name,
	                                         final int length,
	                                         DataInput din) throws IOException {
	        if (name.equals(CODE)) {
	            return CodeAttr.define(cp, name, length, din, mAttrFactory);
	        }
	        else if (name.equals(CONSTANT_VALUE)) {
	            return ConstantValueAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(DEPRECATED)) {
	            return DeprecatedAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(EXCEPTIONS)) {
	            return ExceptionsAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(INNER_CLASSES)) {
	            return InnerClassesAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(LINE_NUMBER_TABLE)) {
	            return LineNumberTableAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(LOCAL_VARIABLE_TABLE)) {
	            return LocalVariableTableAttr.define
	                (cp, name, length, din);
	        }
	        else if (name.equals(SOURCE_FILE)) {
	            return SourceFileAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(SYNTHETIC)) {
	            return SyntheticAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(SIGNATURE)) {
	            return SignatureAttr.define(cp, name, length, din);
	        }
	        else if (name.equals(RUNTIME_VISIBLE_ANNOTATIONS)) {
	            return new RuntimeVisibleAnnotationsAttr(cp, name, length, din);
	        } 
	        else if (name.equals(RUNTIME_INVISIBLE_ANNOTATIONS)) {
	            return new RuntimeInvisibleAnnotationsAttr(cp, name, length, din);
	        }
	        else if (name.equals(RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)) {
	            return new RuntimeVisibleParameterAnnotationsAttr(cp, name, length, din);
	        }
	        else if (name.equals(RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)) {
	            return new RuntimeInvisibleParameterAnnotationsAttr(cp, name, length, din);
	        }

	        if (mAttrFactory != null) {
	            Attribute attr =
	                mAttrFactory.createAttribute(cp, name, length, din);
	            if (attr != null) {
	                return attr;
	            }
	        }
	
	        // Default case, return attribute that captures the data, but
	        // doesn't decode it.
	
	        final byte[] data = new byte[length];
	        din.readFully(data);
	
            return new Attribute(cp, name) {
                public int getLength() {
                    return length;
                }

                public void writeDataTo(DataOutput dout) throws IOException {
                    dout.write(data);
                }
            };
        }
    }
}
