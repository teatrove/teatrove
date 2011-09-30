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
 * This class corresponds to the SourceFile_attribute structure as defined 
 * in section 4.7.2 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @see ClassFile
 */
class SourceFileAttr extends Attribute {
    private String mFileName;

    private ConstantUTFInfo mSourcefile;
    
    public SourceFileAttr(ConstantPool cp, String fileName) {
        super(cp, SOURCE_FILE);
        
        mFileName = fileName;
        mSourcefile = ConstantUTFInfo.make(cp, fileName);
    }
    
    /**
     * Returns the source file name.
     */
    public String getFileName() {
        return mFileName;
    }
    
    /**
     * Returns a constant from the constant pool with the source file name.
     */
    public ConstantUTFInfo getFileNameConstant() {
        return mSourcefile;
    }

    /**
     * Returns the length of the source file attribute. (2 bytes)
     */
    public int getLength() {
        return 2;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSourcefile.getIndex());
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        int index = din.readUnsignedShort();
        if ((length -= 2) > 0) {
            din.skipBytes(length);
        }

        String filename = ((ConstantUTFInfo)cp.getConstant(index)).getValue();

        return new SourceFileAttr(cp, filename);
    }
}
