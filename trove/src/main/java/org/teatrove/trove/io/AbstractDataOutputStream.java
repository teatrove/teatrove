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

package org.teatrove.trove.io;

import java.io.*;

/**
 * Unlike {@link java.io.DataOutputStream}, no OutputStream is required by the
 * constructor. OutputStream implementations that also implement DataOutput may
 * simply extend this class to simplify implementation.
 * <p>
 * AbstractDataOutputStream is not thread-safe, but then its uncommon for
 * multiple threads to write to the same OutputStream.
 *
 * @author Brian S O'Neill
 */
public abstract class AbstractDataOutputStream extends OutputStream
    implements DataOutput
{
    private byte[] mTemp;

    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public void writeByte(int v) throws IOException {
        write(v);
    }

    public void writeShort(int v) throws IOException {
        DataIO.writeShort(this, v, tempArray());
    }

    public void writeChar(int v) throws IOException {
        DataIO.writeChar(this, v, tempArray());
    }

    public void writeInt(int v) throws IOException {
        DataIO.writeInt(this, v, tempArray());
    }

    public void writeLong(long v) throws IOException {
        DataIO.writeLong(this, v, tempArray());
    }

    public void writeFloat(float v) throws IOException {
        DataIO.writeFloat(this, v, tempArray());
    }

    public void writeDouble(double v) throws IOException {
        DataIO.writeDouble(this, v, tempArray());
    }

    public void writeBytes(String s) throws IOException {
        DataIO.writeBytes(this, s);
    }

    public void writeChars(String s) throws IOException {
        DataIO.writeChars(this, s);
    }

    public void writeUTF(String s) throws IOException {
        int length = s.length();
        char[] chars = new char[length];
        s.getChars(0, length, chars, 0);
        int utflen = DataIO.calculateUTFLength(chars, 0, length);
        if (utflen > 65535) {
            throw new UTFDataFormatException();
        }
        writeShort(utflen);
        DataIO.writeUTF((OutputStream)this, chars, 0, length);
    }

    private byte[] tempArray() {
        if (mTemp == null) {
            mTemp = new byte[8];
        }
        return mTemp;
    }
}
