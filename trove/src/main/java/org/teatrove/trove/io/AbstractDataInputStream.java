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
 * Unlike {@link java.io.DataInputStream}, no InputStream is required by the
 * constructor. InputStream implementations that also implement DataInput may
 * simply extend this class to simplify implementation.
 * <p>
 * AbstractDataInputStream is not thread-safe, but then its uncommon for
 * multiple threads to read from the same InputStream.
 *
 * @author Brian S O'Neill
 */
public abstract class AbstractDataInputStream extends InputStream
    implements DataInput
{
    private byte[] mTemp;

    public void readFully(byte[] b) throws IOException {
        DataIO.readFully(this, b, 0, b.length);
    }

    public void readFully(byte[] b, int offset, int length)
        throws IOException
    {
        DataIO.readFully(this, b, offset, length);
    }

    public int skipBytes(int n) throws IOException {
        return (int)skip(n);
    }

    public boolean readBoolean() throws IOException {
        return DataIO.readBoolean(this);
    }

    public byte readByte() throws IOException {
        return DataIO.readByte(this);
    }

    public int readUnsignedByte() throws IOException {
        return DataIO.readUnsignedByte(this);
    }

    public short readShort() throws IOException {
        return DataIO.readShort(this, tempArray());
    }

    public int readUnsignedShort() throws IOException {
        return DataIO.readUnsignedShort(this, tempArray());
    }

    public char readChar() throws IOException {
        return DataIO.readChar(this, tempArray());
    }

    public int readInt() throws IOException {
        return DataIO.readInt(this, tempArray());
    }

    public long readLong() throws IOException {
        return DataIO.readLong(this, tempArray());
    }

    public float readFloat() throws IOException {
        return DataIO.readFloat(this, tempArray());
    }

    public double readDouble() throws IOException {
        return DataIO.readDouble(this, tempArray());
    }

    /**
     * Always throws an IOException.
     */
    public String readLine() throws IOException {
        throw new IOException("readLine not supported");
    }

    public String readUTF() throws IOException {
        int bytesExpected = readUnsignedShort();
        char[] chars = new char[bytesExpected];
        int charCount = DataIO.readUTF
            ((InputStream)this, chars, 0, bytesExpected, bytesExpected);
        return new String(chars, 0, charCount);
    }

    private byte[] tempArray() {
        if (mTemp == null) {
            mTemp = new byte[8];
        }
        return mTemp;
    }
}
