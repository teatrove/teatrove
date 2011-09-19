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
 * FastBufferedOutputStream is just a slightly modified version of
 * {@link java.io.BufferedOutputStream}. The synchronization is gone, and so
 * writes are faster. Refer to the original BufferedOutputStream for
 * documentation.
 */
/*
 * @author  Arthur van Hoff
 * @version 1.27, 02/02/00
 * @since   JDK1.0
 */
public class FastBufferedOutputStream extends FilterOutputStream {
    // These fields have been renamed and made private. In the original, they
    // are protected.
    private byte[] mBuffer;
    private int mCount;

    public FastBufferedOutputStream(OutputStream out) {
        this(out, 512);
    }

    public FastBufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        mBuffer = new byte[size];
    }

    private void flushBuffer() throws IOException {
        if (mCount > 0) {
            out.write(mBuffer, 0, mCount);
            mCount = 0;
        }
    }

    public void write(int b) throws IOException {
        if (mCount >= mBuffer.length) {
            flushBuffer();
        }
        mBuffer[mCount++] = (byte)b;
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (len >= mBuffer.length) {
            flushBuffer();
            out.write(b, off, len);
            return;
        }
        if (len > mBuffer.length - mCount) {
            flushBuffer();
        }
        System.arraycopy(b, off, mBuffer, mCount, len);
        mCount += len;
    }

    public synchronized void flush() throws IOException {
        flushBuffer();
        out.flush();
    }

    public synchronized void close() throws IOException {
        flushBuffer();
        out.flush();
        out.close();
    }
}
