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
 * FastBufferedInputStream is just a slightly modified version of
 * {@link java.io.BufferedInputStream}. The synchronization is gone, and so
 * reads are faster. Refer to the original BufferedInputStream for
 * documentation.
 */
/* @author  Arthur van Hoff
 * @version 1.41, 02/02/00
 * @since   JDK1.0
 */
public class FastBufferedInputStream extends FilterInputStream {
    // These fields have been renamed and made private. In the original, they
    // are protected.
    private byte[] mBuffer;
    private int mCount;
    private int mPos;
    private int mMarkPos = -1;
    private int mMarkLimit;
    
    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }

    public FastBufferedInputStream(InputStream in) {
        this(in, 2048);
    }

    public FastBufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        mBuffer = new byte[size];
    }

    private void fill() throws IOException {
        if (mMarkPos < 0) {
            mPos = 0;
        }
        else if (mPos >= mBuffer.length) {
            if (mMarkPos > 0) {
                int sz = mPos - mMarkPos;
                System.arraycopy(mBuffer, mMarkPos, mBuffer, 0, sz);
                mPos = sz;
                mMarkPos = 0;
            }
            else if (mBuffer.length >= mMarkLimit) {
                mMarkPos = -1;
                mPos = 0;
            }
            else {
                int nsz = mPos * 2;
                if (nsz > mMarkLimit) {
                    nsz = mMarkLimit;
                }
                byte nbuf[] = new byte[nsz];
                System.arraycopy(mBuffer, 0, nbuf, 0, mPos);
                mBuffer = nbuf;
            }
        }
        mCount = mPos;
        int n = in.read(mBuffer, mPos, mBuffer.length - mPos);
        if (n > 0) {
            mCount = n + mPos;
        }
    }
    
    public int read() throws IOException {
        ensureOpen();
        if (mPos >= mCount) {
            fill();
            if (mPos >= mCount) {
                return -1;
            }
        }
        return mBuffer[mPos++] & 0xff;
    }
    
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = mCount - mPos;
        if (avail <= 0) {
            if (len >= mBuffer.length && mMarkPos < 0) {
                return in.read(b, off, len);
            }
            fill();
            avail = mCount - mPos;
            if (avail <= 0) {
                return -1;
            }
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(mBuffer, mPos, b, off, cnt);
        mPos += cnt;
        return cnt;
    }
    
    public int read(byte b[], int off, int len) throws IOException {
        ensureOpen();
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        }
        else if (len == 0) {
            return 0;
        }
        
        int n = read1(b, off, len);
        if (n <= 0) {
            return n;
        }
        while ((n < len) && (in.available() > 0)) {
            int n1 = read1(b, off + n, len - n);
            if (n1 <= 0) {
                break;
            }
            n += n1;
        }
        return n;
    }

    public long skip(long n) throws IOException {
        ensureOpen();
        if (n <= 0) {
            return 0;
        }
        long avail = mCount - mPos;
        
        if (avail <= 0) {
            if (mMarkPos <0) {
                return in.skip(n);
            }
            
            fill();
            avail = mCount - mPos;
            if (avail <= 0) {
                return 0;
            }
        }
        
        long skipped = (avail < n) ? avail : n;
        mPos += skipped;
        return skipped;
    }
    
    public int available() throws IOException {
        ensureOpen();
        return (mCount - mPos) + in.available();
    }
    
    public void mark(int readlimit) {
        mMarkLimit = readlimit;
        mMarkPos = mPos;
    }
    
    public void reset() throws IOException {
        ensureOpen();
        if (mMarkPos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        mPos = mMarkPos;
    }

    public boolean markSupported() {
        return true;
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
            mBuffer = null;
        }
    }
}
