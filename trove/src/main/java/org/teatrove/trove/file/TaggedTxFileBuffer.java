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

/**
 * A simple TxFileBuffer implementation that uses a tag bit to indicate if
 * the file is in a clean state. As long as all commit operations finish, the
 * file will be tagged clean. Rollback operations are not supported.
 *
 * @author Brian S O'Neill
 */
public class TaggedTxFileBuffer extends NonTxFileBuffer
    implements TxFileBuffer
{
    private final Bitlist mBitlist;
    private final long mBitlistPos;
    private int mTranCount;
    private boolean mClosing;

    public TaggedTxFileBuffer(FileBuffer dataFile, Bitlist bitlist,
                              long bitlistPosition) {
        super(dataFile);
        mBitlist = bitlist;
        mBitlistPos = bitlistPosition;
    }

    /**
     * Truncating the file to zero restores it to a clean state.
     */
    public void truncate(long size) throws IOException {
        super.truncate(size);
        if (size == 0) {
            synchronized (this) {
                mTranCount = 0;
                // Clear the bit to indicate clean.
                mBitlist.clear(mBitlistPos);
            }
        }
    }

    public synchronized boolean force() throws IOException {
        // Order the force calls differently in case an IOException is thrown
        // on the second call.
        if (mTranCount > 0) {
            return mBitlist.force() & mFile.force();
        }
        else {
            return mFile.force() & mBitlist.force();
        }
    }

    public synchronized void close() throws IOException {
        mFile.close();
    }

    public synchronized void close(long timeout) throws IOException {
        if (!mFile.isOpen()) {
            return;
        }
        mClosing = true;
        if (timeout != 0 && mTranCount > 0) {
            try {
                if (timeout < 0) {
                    wait();
                }
                else {
                    wait(timeout);
                }
            }
            catch (InterruptedException e) {
            }
        }
        mFile.close();
        mClosing = false;
    }

    public synchronized boolean isClean() throws IOException {
        return mTranCount == 0 && !mBitlist.get(mBitlistPos);
    }

    public synchronized void begin() throws IOException {
        if (!mFile.isOpen()) {
            throw new IOException("FileBuffer closed");
        }
        if (mTranCount <= 0) {
            if (mClosing) {
                throw new IOException("FileBuffer closing");
            }
            mTranCount = 1;
            // Set the bit to indicate dirty.
            mBitlist.set(mBitlistPos);
        }
        else {
            mTranCount++;
        }
    }

    public synchronized boolean commit() throws IOException {
        if (mTranCount > 0) {
            if (--mTranCount <= 0) {
                // Clear the bit to indicate clean.
                mBitlist.clear(mBitlistPos);
                notifyAll();
            }
            return true;
        }
        return false;
    }

    public synchronized boolean rollback() {
        notifyAll();
        throw new UnsupportedOperationException();
    }
}
