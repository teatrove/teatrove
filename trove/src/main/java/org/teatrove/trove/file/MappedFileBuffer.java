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

package org.teatrove.trove.file;

import java.io.*;
import org.teatrove.trove.util.ReadWriteLock;
import org.teatrove.trove.util.SecureReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 */
class MappedFileBuffer implements FileBuffer {
    private long mAddr;
    private int mSize;
    private final boolean mReadOnly;
    private final SecureReadWriteLock mLock;

    public MappedFileBuffer(long handle, int mode, long position, int size,
                            SecureReadWriteLock lock)
        throws IOException
    {
        mAddr = open(handle, mode, position, size);
        mSize = size;
        mReadOnly = mode == SystemFileBuffer.MAP_RO;
        mLock = lock;
    }

    public int read(long position, byte[] dst, int offset, int length)
        throws IOException
    {
        checkArgs(position, dst, offset, length);
        if (length == 0) {
            return 0;
        }
        try {
            mLock.acquireReadLock();
            checkClosed();
            if (position >= mSize) {
                return -1;
            }
            if (position + length > mSize) {
                length = (int)(mSize - position);
            }
            return read(dst, offset, length, mAddr + position);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public int write(long position, byte[] src, int offset, int length)
        throws IOException
    {
        checkArgs(position, src, offset, length);
        if (length == 0) {
            return 0;
        }
        try {
            mLock.acquireWriteLock();
            checkClosed();
            if (position >= mSize) {
                throw new EOFException("position > max file length: " +
                                       position + " > " + mSize);
            }
            if (position + length > mSize) {
                length = (int)(mSize - position);
            }
            return write(src, offset, length, mAddr + position);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public int read(long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException
                ("position < 0: " + position);
        }
        try {
            mLock.acquireReadLock();
            checkClosed();
            if (position >= mSize) {
                return -1;
            }
            return read(mAddr + position);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void write(long position, int value) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException
                ("position < 0: " + position);
        }
        try {
            mLock.acquireWriteLock();
            checkClosed();
            if (position >= mSize) {
                throw new EOFException("position > max file length: " +
                                       position + " > " + mSize);
            }
            write(mAddr + position, value);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public long size() throws IOException {
        try {
            mLock.acquireReadLock();
            checkClosed();
            return mSize;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void truncate(long size) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0: " + size);
        }
        try {
            mLock.acquireWriteLock();
            checkClosed();
            if (size < mSize) {
                mSize = (int)size;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public ReadWriteLock lock() {
        return mLock;
    }

    public boolean force() throws IOException {
        try {
            mLock.acquireWriteLock();
            checkClosed();
            force(mAddr, mSize);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
        return true;
    }

    public boolean isReadOnly() throws IOException {
        checkClosed();
        return mReadOnly;
    }

    public boolean isOpen() {
        return mAddr != 0;
    }

    public void close() throws IOException {
        try {
            mLock.acquireWriteLock();
            if (mAddr != 0) {
                mAddr = 0;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    private void checkClosed() throws IOException {
        if (mAddr == 0) {
            throw new IOException("FileBuffer closed");
        }
    }

    private void checkArgs(long position,
                           byte[] array, int offset, int length) {
        if (position < 0) {
            throw new IllegalArgumentException("position < 0: " + position);
        }
        
        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException("offset < 0: " + offset);
        }

        if (length < 0) {
            throw new IndexOutOfBoundsException("length < 0: " + length);
        }

        if (offset + length > array.length) {
            throw new ArrayIndexOutOfBoundsException
                ("offset + length > array length: " +
                 (offset + length) + " > " + array.length);
        }
    }

    private static native long open(long handle, int mode,
                                    long position, int size)
        throws IOException;

    private native int read(byte[] dst, int offset, int length, long position)
        throws IOException;

    private native int write(byte[] src, int offset, int length, long position)
        throws IOException;

    private native int read0(long position) throws IOException;

    private native void write0(long position, int value) throws IOException;

    private native void force(long addr, int size)
        throws IOException;
}
