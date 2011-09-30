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
import java.io.FileNotFoundException;
import org.teatrove.trove.util.ReadWriteLock;
import org.teatrove.trove.util.SecureReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 */
public class MultiplexFileRepository implements FileRepository {
    private final MultiplexFile mMF;

    private final FileBuffer mFreeIds;
    // Set bits indicate that the file exists.
    private final Bitlist mFreeIdBitlist;
    private final FileBufferInputStream mFreeIdsIn;
    private final FileBufferOutputStream mFreeIdsOut;
    private final ReadWriteLock mLock;

    /**
     * @param mf MultiplexFile to store files
     */
    public MultiplexFileRepository(MultiplexFile mf) throws IOException {
        this(mf, 1);
    }

    /**
     * @param mf MultiplexFile to store files
     * @param firstId First id in MultiplexFile to use
     */
    public MultiplexFileRepository(MultiplexFile mf, int firstId)
        throws IOException
    {
        mMF = mf;
        if (firstId <= 0) {
            // Never use file zero.
            firstId = 1;
        }
        mFreeIds = mf.openFile(firstId);
        mFreeIdBitlist = new Bitlist(mf.openFile(firstId + 1));
        mFreeIdsIn = new FileBufferInputStream(mFreeIds);
        mFreeIdsOut = new FileBufferOutputStream(mFreeIds);
        mLock = mFreeIds.lock();
    }

    public long fileCount() throws IOException {
        try {
            mLock.acquireReadLock();
            return mFreeIdBitlist.size() - mFreeIds.size() / 8;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Iterator fileIds() throws IOException {
        return new Iter();
    }

    public boolean fileExists(long id) throws IOException {
        try {
            mLock.acquireReadLock();
            return mFreeIdBitlist.get(id);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public FileBuffer openFile(long id)
        throws IOException, FileNotFoundException
    {
        try {
            mLock.acquireReadLock();
            if (!mFreeIdBitlist.get(id)) {
                throw new FileNotFoundException(String.valueOf(id));
            }
            return mMF.openFile(id);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public long createFile() throws IOException {
        try {
            mLock.acquireWriteLock();
            long id;
            if (mFreeIds.size() < 8) {
                id = mMF.getFileCount();
                // This creates the file.
                mMF.openFile(id).close();
            }
            else {
                long pos = mFreeIds.size() - 8;
                mFreeIdsIn.position(pos);
                id = mFreeIdsIn.readLong();
                mFreeIds.truncate(pos);
            }
            mFreeIdBitlist.set(id);
            return id;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public boolean deleteFile(long id) throws IOException {
        try {
            mLock.acquireUpgradableLock();
            if (mFreeIdBitlist.get(id)) {
                mLock.acquireWriteLock();
                try {
                    mMF.deleteFile(id);
                    mFreeIdBitlist.clear(id);
                    long pos = mFreeIds.size();
                    mFreeIdsOut.position(pos);
                    mFreeIdsOut.writeLong(id);
                }
                finally {
                    mLock.releaseLock();
                }
                return true;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
        return false;
    }

    public ReadWriteLock lock() {
        return mLock;
    }

    public int getBlockSize() {
        return mMF.getBlockSize();
    }

    public int getBlockIdScale() {
        return mMF.getBlockIdScale();
    }

    public int getLengthScale() {
        return mMF.getLengthScale();
    }

    public long getMaximumFileLength() {
        return mMF.getMaximumFileLength();
    }

    private class Iter implements Iterator {
        private byte[] mTemp;
        private long mIndex;

        Iter() throws IOException {
            mTemp = new byte[32];
            mIndex = mFreeIdBitlist.findFirstSet(0, mTemp);
        }

        public boolean hasNext() throws IOException {
            return mIndex >= 0;
        }

        public long next() throws IOException {
            if (mIndex < 0) {
                throw new java.util.NoSuchElementException();
            }
            long index = mIndex;
            mIndex = mFreeIdBitlist.findFirstSet(index + 1, mTemp);
            return index;
        }
    }
}
