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
import org.teatrove.trove.util.ReadWriteLock;

/**
 * A TxFileBuffer implementation that satisfies the API requirements, but
 * doesn't actually do anything useful for transactions. All calls are
 * delegated to a wrapped FileBuffer, and calls to write and truncate call
 * begin and commit. At a minimum, subclasses implementing transaction support 
 * need to override only begin and commit.
 *
 * @author Brian S O'Neill
 */
public class NonTxFileBuffer implements TxFileBuffer {
    protected final FileBuffer mFile;

    public NonTxFileBuffer(FileBuffer file) {
        mFile = file;
    }

    public int read(long position, byte[] dst, int offset, int length)
        throws IOException
    {
        return mFile.read(position, dst, offset, length);
    }

    public int write(long position, byte[] src, int offset, int length)
        throws IOException
    {
        begin();
        int amt = mFile.write(position, src, offset, length);
        commit();
        return amt;
    }

    public int read(long position) throws IOException {
        return mFile.read(position);
    }

    public void write(long position, int value) throws IOException {
        begin();
        mFile.write(position, value);
        commit();
    }
    
    public long size() throws IOException {
        return mFile.size();
    }

    public void truncate(long size) throws IOException {
        begin();
        mFile.truncate(size);
        commit();
    }

    public ReadWriteLock lock() {
        return mFile.lock();
    }

    public boolean force() throws IOException {
        return mFile.force();
    }

    public boolean isReadOnly() throws IOException {
        return mFile.isReadOnly();
    }

    public boolean isOpen() {
        return mFile.isOpen();
    }

    public void close() throws IOException {
        mFile.close();
    }

    public void close(long timeout) throws IOException {
        mFile.close();
    }

    /**
     * Always returns false.
     */
    public boolean isRollbackSupported() {
        return false;
    }

    /**
     * Always returns true.
     */
    public boolean isClean() throws IOException {
        return true;
    }

    /**
     * Does nothing.
     */
    public void begin() throws IOException {
    }

    /**
     * Always returns false.
     */
    public boolean commit() throws IOException {
        return false;
    }

    /**
     * Always throws UnsupportedOperationException.
     */
    public boolean rollback() {
        throw new UnsupportedOperationException();
    }
}
