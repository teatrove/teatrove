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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A CharToByteBuffer that keeps track of interned strings (mainly string
 * literals) and statically caches the results of those strings after applying
 * a byte conversion. This can improve performance if many of the strings being
 * passed to the append method have been converted before.
 *
 * @author Brian S O'Neill
 */
public class InternedCharToByteBuffer
    implements CharToByteBuffer, Serializable
{
    private static final long serialVersionUID = 1L;

    private static final int MIN_LENGTH = 40;

    // private static final Object MARKER = new Object();

    // private static Map<String, Map> cEncodings = new HashMap<String, Map>(7);

    // private static Random cLastRandom = new Random();

    /*
    private static Map getConvertedCache(String encoding) {
        synchronized (cEncodings) {
            Map cache = cEncodings.get(encoding);
            if (cache == null) {
                cache = Collections.synchronizedMap(new IdentityMap());
                cEncodings.put(encoding, cache);
            }
            return cache;
        }
    }
    */

    private CharToByteBuffer mBuffer;
    //private Random mRandom;
    //private transient Map mConvertedCache;

    public InternedCharToByteBuffer(CharToByteBuffer buffer)
        throws IOException
    {
        mBuffer = buffer;
        //mConvertedCache = getConvertedCache(buffer.getEncoding());
    }

    public void setEncoding(String enc) throws IOException {
        mBuffer.setEncoding(enc);
        //mConvertedCache = getConvertedCache(mBuffer.getEncoding());
    }

    public String getEncoding() throws IOException {
        return mBuffer.getEncoding();
    }

    public long getBaseByteCount() throws IOException {
        return mBuffer.getBaseByteCount();
    }

    public long getByteCount() throws IOException {
        return mBuffer.getByteCount();
    }
    
    public void writeTo(OutputStream out) throws IOException {
        mBuffer.writeTo(out);
    }
    
    public void append(byte b) throws IOException {
        mBuffer.append(b);
    }
    
    public void append(byte[] bytes) throws IOException {
        mBuffer.append(bytes);
    }
    
    public void append(byte[] bytes, int offset, int length)
        throws IOException {

        mBuffer.append(bytes, offset, length);
    }
    
    public void appendSurrogate(ByteData s) throws IOException {
        mBuffer.appendSurrogate(s);
    }

    public void addCaptureBuffer(ByteBuffer buffer) throws IOException {
        mBuffer.addCaptureBuffer(buffer);
    }
    
    public void removeCaptureBuffer(ByteBuffer buffer) throws IOException {
        mBuffer.removeCaptureBuffer(buffer);
    }
    
    public void append(char c) throws IOException {
        mBuffer.append(c);
    }
    
    public void append(char[] chars) throws IOException {
        mBuffer.append(chars);
    }
    
    public void append(char[] chars, int offset, int length) 
        throws IOException {

        mBuffer.append(chars, offset, length);
    }
    
    public void append(String str) throws IOException {
        if (str.length() < MIN_LENGTH) {
            mBuffer.append(str);
            return;
        }
        mBuffer.append(str);
/*        Map cache = mConvertedCache;

        // Caching performed using a two pass technique. This is done to
        // avoid the cost of String.getBytes() for strings that aren't
        // actually interned.

        Object value;

        if ((value = cache.get(str)) != null) {
            byte[] bytes;
            if (value != MARKER) {
                bytes = (byte[])value;
            }
            else {
                // This is at least the second time the string has been seen,
                // so assume it has been interned and call String.getBytes().
                String enc = getEncoding();
                if (enc != null) {
                    bytes = str.getBytes(enc);
                }
                else { 
                    // no encoding specified so use default.
                    bytes = str.getBytes();
                }
                cache.put(str, bytes);
            }

            mBuffer.append(bytes);
        }
        else {
            // Just put a marker at first to indicate that the string has been
            // seen, but don't call String.getBytes() just yet.
            if (mRandom == null) {
                mRandom = getRandom();
            }
            if ((mRandom.nextInt() & 31) == 0) {
                // Only mark sometimes in order to reduce the amount of times
                // put is called for strings that will never be seen again.
                // Calculating a random number is cheaper than putting into an
                // IdentityMap because no objects are created. A consequence of
                // this optimization is that it will take more iterations to
                // discover the real string literals, but they will be
                // discovered eventually.
                cache.put(str, MARKER);
            }
            mBuffer.append(str);
        } */
    }
    
    public void append(String str, int offset, int length) throws IOException {
        mBuffer.append(str, offset, length);
    }

    public void reset() throws IOException {
        mBuffer.reset();
    }

    public void clear() throws IOException {
        mBuffer.clear();
    }
    
    public void drain() throws IOException {
        mBuffer.drain();
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        // mConvertedCache = getConvertedCache(mBuffer.getEncoding());
    }
}
