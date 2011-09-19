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

import java.io.OutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * A ByteBuffer implementation that keeps byte data in memory.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 18 <!-- $-->, <!--$$JustDate:--> 01/05/30 <!-- $-->
 */
public class DefaultByteBuffer implements ByteBuffer, Serializable {
    private static final int BUFFER_SIZE = 512;

    // A List of ByteData instances.
    private List mChunks;

    private byte[] mBuffer;
    private int mCursor;

    private int mBaseCount;

    private List mCaptureBuffers;

    public DefaultByteBuffer() {
        mChunks = new ArrayList(100);
    }

    public long getBaseByteCount() {
        if (mBuffer != null) {
            return mBaseCount + mCursor;
        }
        else {
            return mBaseCount;
        }
    }

    public long getByteCount() throws IOException {
        long count;
        if (mBuffer != null) {
            count = mCursor;
        }
        else {
            count = 0;
        }

        int size = mChunks.size();
        for (int i=0; i<size; i++) {
            count += ((ByteData)mChunks.get(i)).getByteCount();
        }

        return count;
    }

    public void writeTo(OutputStream out) throws IOException {
        int size = mChunks.size();
        for (int i=0; i<size; i++) {
            ((ByteData)mChunks.get(i)).writeTo(out);
        }

        if (mBuffer != null && mCursor != 0) {
            out.write(mBuffer, 0, mCursor);
        }
    }

    public void append(byte b) throws IOException {
        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                ((ByteBuffer)captureBuffers.get(i)).append(b);
            }
        }

        if (mBuffer == null) {
            mBuffer = new byte[BUFFER_SIZE];
            mCursor = 0;
        }
        else if (mCursor >= mBuffer.length) {
            mChunks.add(new ArrayByteData(mBuffer));
            mBaseCount += BUFFER_SIZE;
            mBuffer = new byte[BUFFER_SIZE];
            mCursor = 0;
        }

        mBuffer[mCursor++] = b;
    }

    public void append(byte[] bytes) throws IOException {
        append(bytes, 0, bytes.length);
    }

    public void append(byte[] bytes, int offset, int length)
        throws IOException
    {
        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                ((ByteBuffer)captureBuffers.get(i)).append
                    (bytes, offset, length);
            }
        }

        while (length > 0) {
            if (mBuffer == null) {
                if (length >= BUFFER_SIZE) {
                    byte[] copy = new byte[length];
                    System.arraycopy(bytes, offset, copy, 0, length);
                    mChunks.add(new ArrayByteData(copy));
                    mBaseCount += length;
                    return;
                }
                
                mBuffer = new byte[BUFFER_SIZE];
                mCursor = 0;
            }
            
            int available = BUFFER_SIZE - mCursor;
            
            if (length <= available) {
                System.arraycopy(bytes, offset, mBuffer, mCursor, length);
                mCursor += length;
                return;
            }
            
            System.arraycopy(bytes, offset, mBuffer, mCursor, available);
            mChunks.add(new ArrayByteData(mBuffer));
            mBaseCount += BUFFER_SIZE;
            mBuffer = null;
            offset += available;
            length -= available;
        }
    }

    public void appendSurrogate(ByteData s) throws IOException {
        if (s == null) {
            return;
        }

        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                ((ByteBuffer)captureBuffers.get(i)).appendSurrogate(s);
            }
        }

        if (mBuffer != null && mCursor > 0) {
            mChunks.add(new ArrayByteData(mBuffer, 0, mCursor));
            mBaseCount += mCursor;
            mBuffer = null;
        }
        mChunks.add(s);
    }

    public void addCaptureBuffer(ByteBuffer buffer) {
        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) == null) {
            captureBuffers = mCaptureBuffers = new ArrayList();
        }
        captureBuffers.add(buffer);
    }

    public void removeCaptureBuffer(ByteBuffer buffer) {
        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            captureBuffers.remove(buffer);
        }
    }

    public void reset() throws IOException {
        int size = mChunks.size();
        for (int i=0; i<size; i++) {
            ((ByteData)mChunks.get(i)).reset();
        }

        List captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                ((ByteData)captureBuffers.get(i)).reset();
            }
        }
    }
}
