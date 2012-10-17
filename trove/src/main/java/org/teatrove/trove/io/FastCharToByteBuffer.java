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
import java.util.Set;
import java.util.HashSet;

/**
 * A CharToByteBuffer implementation that converts ISO-8859-1 encoded
 * characters faster. To force fast conversion, construct FastCharToByteBuffer
 * with ISO-8859-1 as the default encoding.
 * 
 * @author Brian S O'Neill
 */
public class FastCharToByteBuffer implements CharToByteBuffer, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int TEMP_BUF_LEN = 512;
    private static final Set<String> SUPPORTED_ENCODINGS;

    static {
        SUPPORTED_ENCODINGS = new HashSet<String>();
        SUPPORTED_ENCODINGS.add("8859-1");
        SUPPORTED_ENCODINGS.add("8859_1");
        SUPPORTED_ENCODINGS.add("iso-8859-1");
        SUPPORTED_ENCODINGS.add("ISO-8859-1");
        SUPPORTED_ENCODINGS.add("iso8859_1");
        SUPPORTED_ENCODINGS.add("ISO8859_1");
    }

    static boolean isSupportedEncoding(String encoding) {
        return SUPPORTED_ENCODINGS.contains(encoding);
    }

    private ByteBuffer mBuffer;

    private transient byte[] mTempBytes;
    private transient char[] mTempChars;

    private CharToByteBuffer mSlowConvertor;

    /**
     * @param buffer Buffer that receives the characters converted to bytes.
     */    
    public FastCharToByteBuffer(ByteBuffer buffer) {
        this(buffer, null);
    }

    /**
     * @param buffer Buffer that receives the characters converted to bytes.
     * @param defaultEncoding Default character encoding to use if setEncoding
     * is not called.
     */    
    public FastCharToByteBuffer(ByteBuffer buffer, String defaultEncoding) {
        mBuffer = buffer;
        try {
            setEncoding(defaultEncoding);
        }
        catch (IOException e) {
        }
    }

    public void setEncoding(String enc) throws IOException {
        drain();
        if (isSupportedEncoding(enc)) {
            mSlowConvertor = null;
        }
        else {
            mSlowConvertor = new DefaultCharToByteBuffer(mBuffer, enc);
        }
    }

    public String getEncoding() throws IOException {
        return mSlowConvertor != null ?
            mSlowConvertor.getEncoding() : "ISO-8859-1";
    }

    public long getBaseByteCount() throws IOException {
        drain();
        return mBuffer.getBaseByteCount();
    }

    public long getByteCount() throws IOException {
        drain();
        return mBuffer.getByteCount();
    }

    public void writeTo(OutputStream out) throws IOException {
        drain();
        mBuffer.writeTo(out);
    }

    public void append(byte b) throws IOException {
        drain();
        mBuffer.append(b);
    }

    public void append(byte[] bytes) throws IOException {
        append(bytes, 0, bytes.length);
    }

    public void append(byte[] bytes, int offset, int length)
        throws IOException
    {
        if (length != 0) {
            drain();
            mBuffer.append(bytes, offset, length);
        }
    }

    public void appendSurrogate(ByteData s) throws IOException {
        if (s != null) {
            drain();
            mBuffer.appendSurrogate(s);
        }
    }

    public void addCaptureBuffer(ByteBuffer buffer) throws IOException {
        drain();
        mBuffer.addCaptureBuffer(buffer);
    }

    public void removeCaptureBuffer(ByteBuffer buffer) throws IOException {
        drain();
        mBuffer.removeCaptureBuffer(buffer);
    }

    public void append(char c) throws IOException {
        char[] chars = getTempChars();
        chars[0] = c;
        append(chars, 0, 1);
    }

    public void append(char[] chars) throws IOException {
        append(chars, 0, chars.length);
    }

    public void append(char[] chars, int offset, int length)
        throws IOException
    {
        if (mSlowConvertor != null) {
            mSlowConvertor.append(chars, offset, length);
            return;
        }

        appendFast(chars, offset, length);
    }

    public void append(String str) throws IOException {
        append(str, 0, str.length());
    }

    public void append(String str, int offset, int length) throws IOException {
        if (mSlowConvertor != null) {
            mSlowConvertor.append(str, offset, length);
            return;
        }

        if (length == 0) {
            return;
        }

        char[] tempChars = getTempChars();
        int bufLen = tempChars.length;

        while (length >= bufLen) {
            str.getChars(offset, offset + bufLen, tempChars, 0);
            offset += bufLen;
            length -= bufLen;
            appendFast(tempChars, 0, bufLen);
        }

        if (length > 0) {
            str.getChars(offset, offset + length, tempChars, 0);
            appendFast(tempChars, 0, length);
        }
    }

    public void reset() throws IOException {
        mBuffer.reset();
    }

    public void clear() throws IOException {
        mBuffer.clear();
        if (mSlowConvertor != null) {
            mSlowConvertor.clear();
        }
    }
    
    public void drain() throws IOException {
        if (mSlowConvertor != null) {
            mSlowConvertor.drain();
        }
    }

    private void appendFast(char[] chars, int offset, int length)
        throws IOException
    {
        byte[] tempBytes = getTempBytes();
        int bufLen = tempBytes.length;
        int bi = 0;
        int climit = offset + length;

        for (int ci = offset; ci < climit; ci++) {
            tempBytes[bi++] = (byte)chars[ci];
            if (bi >= bufLen) {
                mBuffer.append(tempBytes, 0, bufLen);
                bi = 0;
            }
        }

        if (bi > 0) {
            mBuffer.append(tempBytes, 0, bi);
        }
    }

    private byte[] getTempBytes() {
        if (mTempBytes == null) {
            mTempBytes = new byte[TEMP_BUF_LEN];
        }
        return mTempBytes;
    }

    private char[] getTempChars() {
        if (mTempChars == null) {
            mTempChars = new char[TEMP_BUF_LEN];
        }
        return mTempChars;
    }
}
