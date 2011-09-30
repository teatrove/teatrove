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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.File;
import java.io.RandomAccessFile;
import org.teatrove.trove.util.ReadWriteLock;
import org.teatrove.trove.util.SecureReadWriteLock;

/**
 * A FileBufferImplementation that calls into the standard Java
 * RandomAccessFile class.
 *
 * @author Brian S O'Neill
 */
public class RandomAccessFileBuffer implements FileBuffer {
    private RandomAccessFile mRAF;
    private long mPosition;

    // Bit 0 set: read only
    // Bit 1 set: closed
    private volatile int mFlags;

    private final SecureReadWriteLock mLock;
    
    public RandomAccessFileBuffer(File file, boolean readOnly)
        throws IOException
    {
        this(new RandomAccessFile(file, readOnly ? "r" : "rw"),
             readOnly, new SecureReadWriteLock());
    }

    /**
     * @param readOnly specify access mode of raf
     */
    public RandomAccessFileBuffer(RandomAccessFile raf, boolean readOnly)
        throws IOException
    {
        this(raf, readOnly, new SecureReadWriteLock());
    }

    public RandomAccessFileBuffer(File file, boolean readOnly,
                                  SecureReadWriteLock lock)
        throws IOException
    {
        this(new RandomAccessFile(file, readOnly ? "r" : "rw"),
             readOnly, lock);
    }

    /**
     * @param readOnly specify access mode of raf
     */
    public RandomAccessFileBuffer(RandomAccessFile raf, boolean readOnly,
                                  SecureReadWriteLock lock)
        throws IOException
    {
        mRAF = raf;
        mPosition = raf.getFilePointer();
        mFlags = readOnly ? 1 : 0;
        mLock = lock;
    }

    public int read(long position, byte[] dst, int offset, int length)
        throws IOException
    {
        try {
            // Exclusive lock must be acquired because of the mutable file
            // position. Pretty lame, eh?
            mLock.acquireWriteLock();
            if (position != mPosition) {
                mRAF.seek(position);
                mPosition = position;
            }
            int amt = mRAF.read(dst, offset, length);
            if (amt > 0) {
                mPosition += amt;
            }
            return amt;
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
        try {
            mLock.acquireWriteLock();
            if (position != mPosition) {
                mRAF.seek(position);
                mPosition = position;
            }
            mRAF.write(src, offset, length);
            mPosition += length;
            return length;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public int read(long position) throws IOException {
        try {
            // Exclusive lock must be acquired because of the mutable file
            // position. Pretty lame, eh?
            mLock.acquireWriteLock();
            if (position != mPosition) {
                mRAF.seek(position);
                mPosition = position;
            }
            int value = mRAF.read();
            if (value >= 0) {
                mPosition++;
            }
            return value;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void write(long position, int value) throws IOException {
        try {
            mLock.acquireWriteLock();
            if (position != mPosition) {
                mRAF.seek(position);
                mPosition = position;
            }
            mRAF.write(value);
            mPosition++;
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
            return mRAF.length();
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void truncate(long size) throws IOException {
        try {
            mLock.acquireWriteLock();
            if (size < size()) {
                mRAF.setLength(size);
                mPosition = mRAF.getFilePointer();
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
            mRAF.getFD().sync();
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
        return (mFlags & 1) != 0;
    }

    public boolean isOpen() {
        return (mFlags & 2) == 0;
    }

    public void close() throws IOException {
        try {
            mLock.acquireWriteLock();
            if (isOpen()) {
                mRAF.close();
                mFlags |= 2;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }
}
