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
 * 
 *
 * @author Nick Hagan
 */
class SignatureAttr extends Attribute {
    private String mSignaturStr;

    private ConstantUTFInfo mSignature;

    public SignatureAttr(ConstantPool cp, String signatur) {
        super(cp, SIGNATURE);

        mSignaturStr = signatur;
        mSignature = ConstantUTFInfo.make(cp, signatur);
    }

    /**
     * Returns the signature.
     */
    public String getSignature() {
        return mSignaturStr;
    }

    /**
     * Returns a constant from the constant pool with the signature.
     */
    public ConstantUTFInfo getSignatureConstant() {
        return mSignature;
    }

    /**
     * Returns the length of the signature attribute. (2 bytes)
     */
    public int getLength() {
        return 2;
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSignature.getIndex());
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        int index = din.readUnsignedShort();
        if ((length -= 2) > 0) {
            din.skipBytes(length);
        }

        String signature = ((ConstantUTFInfo)cp.getConstant(index)).getValue();

        return new SignatureAttr(cp, signature);
    }
}
