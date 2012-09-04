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
import java.util.List;
import java.util.ArrayList;

/**
 * A ByteBuffer implementation that initially stores its data in a
 * DefaultByteBuffer, but after a certain threshold is reached, spills over
 * into a FileByteBuffer.
 *
 * @author Brian S O'Neill
 */
public class SpilloverByteBuffer implements ByteBuffer {
    private Group mGroup;
    
    private ByteBuffer mLocalBuffer;
    private ByteBuffer mSpillover;

    private List<ByteBuffer> mCaptureBuffers;

    /**
     * Create a SpilloverByteBuffer against a Group that sets the threshold
     * and can create a spillover FileByteBuffer. The Group can be shared
     * among many SpilloverByteBuffers.
     *
     * @param group a group that can be shared among many SpilloverByteBuffers
     */
    public SpilloverByteBuffer(Group group) {
        mGroup = group;
        mLocalBuffer = new DefaultByteBuffer();
    }

    public long getBaseByteCount() throws IOException {
        if (mSpillover == null) {
            return mLocalBuffer.getBaseByteCount();
        }
        else {
            return mSpillover.getBaseByteCount();
        }
    }

    public long getByteCount() throws IOException {
        if (mSpillover == null) {
            return mLocalBuffer.getByteCount();
        }
        else {
            return mSpillover.getByteCount();
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        if (mSpillover == null) {
            mLocalBuffer.writeTo(out);
        }
        else {
            mSpillover.writeTo(out);
        }
    }

    public void append(byte b) throws IOException {
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).append(b);
            }
        }

        if (mSpillover == null) {
            if (mGroup.adjustLevel(1)) {
                mLocalBuffer.append(b);
                return;
            }
            spillover();
        }

        mSpillover.append(b);
    }

    public void append(byte[] bytes) throws IOException {
        append(bytes, 0, bytes.length);
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

        if (mSpillover == null) {
            if (mGroup.adjustLevel(length)) {
                mLocalBuffer.append(bytes, offset, length);
                return;
            }
            spillover();
        }

        mSpillover.append(bytes, offset, length);
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

        if (mSpillover == null) {
            mLocalBuffer.appendSurrogate(s);
        }
        else {
            mSpillover.appendSurrogate(s);
        }
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
        mLocalBuffer.reset();
        if (mSpillover != null) {
            mSpillover.reset();
        }
        
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).reset();
            }
        }
    }
    
    public void clear() throws IOException {
        mLocalBuffer.clear();
        if (mSpillover != null) {
            mSpillover.clear();
        }
        
        List<ByteBuffer> captureBuffers;
        if ((captureBuffers = mCaptureBuffers) != null) {
            int size = captureBuffers.size();
            for (int i=0; i<size; i++) {
                captureBuffers.get(i).clear();
            }
        }
    }

    protected void finalize() throws IOException {
        if (mLocalBuffer != null) {
            long count = mLocalBuffer.getBaseByteCount();
            mLocalBuffer = null;
            mGroup.adjustLevel(-count);
        }
    }

    private void spillover() throws IOException {
        mSpillover = mGroup.createFileByteBuffer();
        // TODO: This is bad! By writing out the contents of the existing
        // buffer early, surrogates are evaluated too soon!
        mLocalBuffer.writeTo(new ByteBufferOutputStream(mSpillover));

        long count = mLocalBuffer.getBaseByteCount();
        mLocalBuffer = null;
        mGroup.adjustLevel(-count);
    }

    public static abstract class Group {
        private final long mThreshold;
        private long mLevel;

        public Group(long threshold) {
            mThreshold = threshold;
        }

        public final long getThreshold() {
            return mThreshold;
        }

        public final synchronized long getCurrentLevel() {
            return mLevel;
        }

        public abstract FileByteBuffer createFileByteBuffer()
            throws IOException;

        synchronized boolean adjustLevel(long delta) {
            long newLevel;
            if ((newLevel = mLevel + delta) > mThreshold) {
                return false;
            }
            else {
                if (newLevel < 0) {
                    newLevel = 0;
                }
                mLevel = newLevel;
                return true;
            }
        }
    }
}
