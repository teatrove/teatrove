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
 * An implementation of FileBuffer that makes direct calls into the operating
 * system's file I/O functions. This class requires a native library:
 * org.teatrove_trove_file_SystemFileBuffer.
 *
 * @author Brian S O'Neill
 */
public final class SystemFileBuffer implements FileBuffer {
    public final static int MAP_RO = 1;
    public final static int MAP_RW = 2;
    public final static int MAP_COW = 3;

    static {
        System.loadLibrary("org.teatrove_trove_file_SystemFileBuffer");
    }

    private volatile long mHandle;
    private final boolean mReadOnly;
    private final SecureReadWriteLock mLock;

    public SystemFileBuffer(File file, boolean readOnly) throws IOException {
        this(file.getCanonicalPath(), readOnly, new SecureReadWriteLock());
    }

    /**
     * @param path file path must be canonical
     */
    public SystemFileBuffer(String path, boolean readOnly) throws IOException {
        this(path, readOnly, new SecureReadWriteLock());
    }

    public SystemFileBuffer(File file, boolean readOnly,
                            SecureReadWriteLock lock)
        throws IOException
    {
        this(file.getCanonicalPath(), readOnly, lock);
    }

    /**
     * @param path file path must be canonical
     */
    public SystemFileBuffer(String path, boolean readOnly,
                            SecureReadWriteLock lock)
        throws IOException
    {
        SecurityManager sc = System.getSecurityManager();
        if (sc != null) {
            sc.checkRead(path);
            if (!readOnly) {
                sc.checkWrite(path);
            }
        }

        mHandle = open(path, readOnly);
        mReadOnly = readOnly;
        mLock = lock;
    }

    public FileBuffer map(int mode, long position, int size)
        throws IOException
    {
        if (mode != MAP_RO && mode != MAP_RW && mode != MAP_COW) {
            throw new IllegalArgumentException("Unknown mode");
        }

        if (position < 0) {
            throw new IllegalArgumentException("position < 0: " + position);
        }

        if (size < 0) {
            throw new IllegalArgumentException("size < 0: " + size);
        }

        // TODO: Cache mappings keyed on mode and size. Close handles when
        // this is closed.

        long fileSize = position + size;
        return new MappedFileBuffer(openMapping(mHandle, mode, fileSize),
                                    mode, position, size, mLock);
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
            return read(mHandle, position, dst, offset, length);
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
            return write(mHandle, position, src, offset, length);
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
            throw new IllegalArgumentException("position < 0: " + position);
        }
        try {
            mLock.acquireReadLock();
            return read(mHandle, position);
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
            throw new IllegalArgumentException("position < 0: " + position);
        }
        try {
            mLock.acquireWriteLock();
            write(mHandle, position, value);
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
            return size(mHandle);
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
            truncate(mHandle, size);
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
            force(mHandle, true);
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
        if (!isOpen()) {
            throw new IOException("FileBuffer closed");
        }
        return mReadOnly;
    }

    public boolean isOpen() {
        return mHandle != 0;
    }

    public void close() throws IOException {
        try {
            mLock.acquireWriteLock();
            if (mHandle != 0) {
                close(mHandle);
                mHandle = 0;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    protected void finalize() throws IOException {
        if (mHandle != 0) {
            close(mHandle);
            mHandle = 0;
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

    private static native long open(String path, boolean readOnly)
        throws IOException;

    private static native long openMapping(long handle, int mode, long size);

    private native int read(long handle,
                            long position, byte[] dst, int offset, int length)
        throws IOException;

    private native int write(long handle,
                             long position, byte[] src, int offset, int length)
        throws IOException;

    private native int read(long handle, long position) throws IOException;

    private native void write(long handle,
                              long position, int value) throws IOException;

    private native long size(long handle) throws IOException;

    private native void truncate(long handle, long size) throws IOException;

    private native void force(long handle, boolean metaData)
        throws IOException;

    private native void close(long handle) throws IOException;
}
