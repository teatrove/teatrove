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

package org.teatrove.trove.util;

/**
 * A zlib deflater interface that matches {@link java.util.zip.Deflater},
 * except additional flush operations are supported. This class requires native
 * code support and looks for a library named "org.teatrove_trove_util_Deflater".
 *
 * @author Brian S O'Neill
 * @version
 */
public class Deflater {
    public static final int
        DEFLATED = 8,
        NO_COMPRESSION = 0,
        BEST_SPEED = 1,
        BEST_COMPRESSION = 9,
        DEFAULT_COMPRESSION = -1,
        FILTERED = 1,
        HUFFMAN_ONLY = 2,
        DEFAULT_STRATEGY = 0;

    private static final int
        NO_FLUSH = 0,
        SYNC_FLUSH = 2,
        FULL_FLUSH = 3,
        FINISH = 4;

    static {
        System.loadLibrary("org.teatrove_trove_util_Deflater");
        initIDs();
    }

    // Pointer to strm used by native deflate functions.
    private long mStream;
    private boolean mNoWrap;

    private int mStrategy;
    private int mLevel;
    private boolean mSetParams;

    private int mFlushOption = NO_FLUSH;
    private boolean mFinished;

    private byte[] mInputBuf;
    private int mInputOffset;
    private int mInputLength;

    public Deflater(int level, boolean nowrap) {
        mStream = init(DEFAULT_STRATEGY, level, nowrap);
        mStrategy = DEFAULT_STRATEGY;
        mLevel = level;
        mNoWrap = nowrap;
    }

    public Deflater(int level) {
        this(level, false);
    }

    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    public boolean isNoWrap() {
        return mNoWrap;
    }

    public synchronized void setInput(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        mInputLength = len;
        mInputOffset = off;
        mInputBuf = b;
    }
    
    public synchronized void setInput(byte[] b) {
        mInputLength = b.length;
        mInputOffset = 0;
        mInputBuf = b;
    }
    
    public synchronized void setDictionary(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        setDictionary(mStream, b, off, len);
    }
    
    public synchronized void setDictionary(byte[] b) {
        setDictionary(mStream, b, 0, b.length);
    }

    public synchronized void setStrategy(int strategy) {
        mStrategy = strategy;
        mSetParams = true;
    }
    
    public synchronized void setLevel(int level) {
        mLevel = level;
        mSetParams = true;
    }

    public boolean needsInput() {
        return mInputLength <= 0;
    }

    /**
     * When called, indicates that the current input buffer contents should be
     * flushed out when deflate is next called.
     */
    public void flush() {
        mFlushOption = SYNC_FLUSH;
    }

    /**
     * When called, indicates that the current input buffer contents should be
     * flushed out when deflate is next called, but all compression information
     * up to this point is cleared.
     */
    public void fullFlush() {
        mFlushOption = FULL_FLUSH;
    }

    /**
     * When called, indicates that compression should end with the current
     * contents of the input buffer. Deflate must be called to get the final
     * compressed bytes.
     */
    public void finish() {
        mFlushOption = FINISH;
    }

    public synchronized boolean finished() {
        return mFinished;
    }

    public int deflate(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        return deflate0(b, off, len);
    }

    public int deflate(byte[] b) {
        return deflate0(b, 0, b.length);
    }

    private synchronized int deflate0(byte[] b, int off, int len) {
        int amt = deflate(mStream, mFlushOption, mSetParams,
                          mInputBuf, mInputOffset, mInputLength,
                          b, off, len);
        if (amt < len) {
            if (mFlushOption == SYNC_FLUSH || mFlushOption == FULL_FLUSH) {
                mFlushOption = NO_FLUSH;
            }
        }
        return amt;
    }

    public synchronized int getAdler() {
        return getAdler(mStream);
    }

    public synchronized int getTotalIn() {
        return getTotalIn(mStream);
    }

    public synchronized int getTotalOut() {
        return getTotalOut(mStream);
    }

    public synchronized void reset() {
        mFinished = false;
        mFlushOption = NO_FLUSH;
        mInputBuf = null;
        mInputLength = 0;
        reset(mStream);
    }

    public synchronized void end() {
        end(mStream);
    }

    protected void finalize() {
        end();
    }

    private void boundsCheck(byte[] b, int off, int len) {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private static native void initIDs();

    private native long init(int strategy, int level, boolean nowrap);

    private native void setDictionary(long strm, byte[] b, int off, int len);

    private native int deflate(long strm, int flushOpt, boolean setParams,
                               byte[] inBuf, int inOff, int inLen,
                               byte[] outBuf, int outOff, int outLen);

    private native int getAdler(long strm);
    private native int getTotalIn(long strm);
    private native int getTotalOut(long strm);
    private native void reset(long strm);
    private native void end(long strm);
}
