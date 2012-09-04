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
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * A ByteBuffer implementation that can read from an open file or can write
 * to it. This implementation is best suited for temporary byte data that is
 * too large to hold in memory.
 *
 * @author Brian S O'Neill
 */
public class FileByteBuffer implements ByteBuffer {
    private RandomAccessFile mFile;
    private List<Surrogate> mSurrogates;
    private List<ByteBuffer> mCaptureBuffers;

    /**
     * Creates a FileByteBuffer on a RandomAccessFile. If the file is opened
     * read-only, then the append operations will fail.
     *
     * @param file The file to use as a buffer.
     */
    public FileByteBuffer(RandomAccessFile file) throws IOException {
        mFile = file;
        file.seek(0);
    }

    public long getBaseByteCount() throws IOException {
        return mFile.length();
    }

    public long getByteCount() throws IOException {
        long count = getBaseByteCount();
        if (mSurrogates == null) {
            return count;
        }
        
        int size = mSurrogates.size();
        for (int i=0; i<size; i++) {
            count += mSurrogates.get(i).mByteData.getByteCount();
        }

        return count;
    }

    public void writeTo(OutputStream out) throws IOException {
        long length = mFile.length();
        int bufSize;
        if (length > 4000) {
            bufSize = 4000;
        }
        else {
            bufSize = (int)length;
        }
        
        byte[] inputBuffer = new byte[bufSize];
        
        mFile.seek(0);

        if (mSurrogates != null) {
            long currentPos = 0;
            
            int size = mSurrogates.size();
            for (int i=0; i<size; i++) {
                Surrogate s = mSurrogates.get(i);
                currentPos = writeTo(inputBuffer, out, currentPos, s.mPos);
                s.mByteData.writeTo(out);
            }
        }

        // Write out the rest of the file.
        int readAmount;
        while ((readAmount = mFile.read(inputBuffer, 0, bufSize)) > 0) {
            out.write(inputBuffer, 0, readAmount);
        }
    }

    private long writeTo(byte[] inputBuffer, OutputStream out, 
                         long fromPos, long toPos) throws IOException {
        if (toPos == fromPos) {
            return fromPos;
        }

        int bufSize = inputBuffer.length;
        int readAmount;

        while (toPos > fromPos) {
            int amount;
            if (bufSize <= (toPos - fromPos)) {
                amount = bufSize;
            }
            else {
                amount = (int)(toPos - fromPos);
            }

            while ((readAmount = mFile.read(inputBuffer, 0, amount)) > 0) {
                out.write(inputBuffer, 0, readAmount);
                fromPos += readAmount;
                amount -= readAmount;
                if (amount <= 0) {
                    break;
                }
            }

            if (readAmount <= 0) {
                break;
            }
        }

        return fromPos;
    }

    public void append(byte b) throws IOException {
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).append(b);
            }
        }

        mFile.write(b);
    }

    public void append(byte[] bytes) throws IOException {
        mFile.write(bytes);
    }

    public void append(byte[] bytes, int offset, int length) 
        throws IOException 
    {
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).append(bytes, offset, length);
            }
        }

        mFile.write(bytes, offset, length); 
    }

    public void appendSurrogate(ByteData s) throws IOException {
        if (s == null) {
            return;
        }

        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).appendSurrogate(s);
            }
        }

        if (mSurrogates == null) {
            mSurrogates = new ArrayList<Surrogate>();
        }

        mSurrogates.add(new Surrogate(s));
    }

    public void addCaptureBuffer(ByteBuffer buffer) {
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) == null) {
            captureBuffers = mCaptureBuffers = new ArrayList<ByteBuffer>();
        }
        captureBuffers.add(buffer);
    }

    public void removeCaptureBuffer(ByteBuffer buffer) {
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            captureBuffers.remove(buffer);
        }
    }

    public void reset() throws IOException {
        int i, size;

        List<Surrogate> surrogates;
        if ((surrogates = mSurrogates) != null) {
            size = surrogates.size();
            for (i=0; i<size; i++) {
                surrogates.get(i).mByteData.reset();
            }
        }

        List<ByteBuffer> byteDatas;
        if ((byteDatas = mCaptureBuffers) != null) {
            size = byteDatas.size();
            for (i=0; i<size; i++) {
                byteDatas.get(i).reset();
            }
        }
    }

    public void clear() throws IOException {
        // nothing to do as we do not want to clear the entire file as doing
        // so may be harmful as the file could have been opened for read and
        // write and the clear would overwrite the previously written data
        
        int i, size;
        List<ByteBuffer> byteDatas;
        if ((byteDatas = mCaptureBuffers) != null) {
            size = byteDatas.size();
            for (i=0; i<size; i++) {
                byteDatas.get(i).reset();
            }
        }
    }
    
    private class Surrogate {
        public final ByteData mByteData;
        public final long mPos;

        public Surrogate(ByteData data) throws IOException {
            mByteData = data;
            mPos = mFile.getFilePointer();
        }
    }
}
