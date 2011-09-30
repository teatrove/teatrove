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
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;
import java.lang.ref.*;
import org.teatrove.trove.util.SoftHashMap;
import org.teatrove.trove.util.ReadWriteLock;
import org.teatrove.trove.util.SecureReadWriteLock;

/**
 * MultiplexFile is a growable list of FileBuffers, stored completely
 * within another FileBuffer. MutiplexFile forms the basis for small database
 * systems.
 *
 * @author Brian S O'Neill
 */
public class MultiplexFile {
    private static final int MAGIC = 0xc1a3af92;

    // TODO: Try to eliminate the use of this method since it creates a
    // temporary byte array.
    private static long getScaledValue(FileBuffer buffer, int scale,
                                       long position)
        throws IOException
    {
        return getScaledValue(new byte[8], buffer, scale, position);
    }

    private static long getScaledValue(byte[] temp,
                                       FileBuffer buffer, int scale,
                                       long position)
        throws IOException
    {
        int amt = buffer.read(position, temp, 8 - scale, scale);

        if (amt < scale) {
            // Reached the end of the file, so fill the rest with zeros.
            int i = 8 - scale;
            if (amt > 0) {
                i += amt;
            }
            while (i < 8) {
                temp[i++] = (byte)0;
            }
        }

        long value = 0;

        switch (scale) {
        default:
        case 8:
            value |= ((long)temp[0]) << 56;
        case 7:
            value |= (temp[1] & 0xffL) << 48;
        case 6:
            value |= (temp[2] & 0xffL) << 40;
        case 5:
            value |= (temp[3] & 0xffL) << 32;
        case 4:
            value |= (temp[4] & 0xffL) << 24;
        case 3:
            value |= (temp[5] & 0xffL) << 16;
        case 2:
            value |= (temp[6] & 0xffL) << 8;
        case 1:
            value |= temp[7] & 0xffL;
        case 0:
        }

        return value;
    }

    // TODO: Try to eliminate the use of this method since it creates a
    // temorary byte array.
    private static void putScaledValue(FileBuffer buffer, int scale,
                                       long position, long value)
        throws IOException
    {
        putScaledValue(new byte[8], buffer, scale, position, value);
    }

    private static void putScaledValue(byte[] temp,
                                       FileBuffer buffer, int scale,
                                       long position, long value)
        throws IOException
    {
        switch (scale) {
        default:
        case 8:
            temp[0] = (byte)(value >> 56);
        case 7:
            temp[1] = (byte)(value >> 48);
        case 6:
            temp[2] = (byte)(value >> 40);
        case 5:
            temp[3] = (byte)(value >> 32);
        case 4:
            temp[4] = (byte)(value >> 24);
        case 3:
            temp[5] = (byte)(value >> 16);
        case 2:
            temp[6] = (byte)(value >> 8);
        case 1:
            temp[7] = (byte)value;
        case 0:
        }
        
        buffer.write(position, temp, 8 - scale, scale);
    }

    final TxFileBuffer mBackingFile;

    final int mBlockSize;
    final int mBlockIdScale;
    final long mMaxBlockId;
    final int mLengthScale;
    final long mMaxLength;
    final int mFileTableEntrySize;

    final int mTotalBlocksPosition;
    final int mFirstFreeBlockPosition;
    final int mFileTableSelfEntryPosition;
    final int mFreeBlocksBitlistEntryPosition;

    final long[] mLevelMaxSizes;
    final long[] mLevelMaxBlocks;

    // Total number of blocks used in the backing file, less one.
    private long mTotalBlocks;

    private final FileBuffer mFileTable;
    private final SecureReadWriteLock mFileTableLock;

    // Blocks that are free have a set bit in the bitlist. Using clear to mark
    // used blocks is important in the correct operation of the bitlist, since
    // it is a file itself. A special behavior is used when allocing blocks
    // for this file. The blocks will start cleared, and no call is needed to
    // indicate the block is in use. If the call were allowed to happen,
    // it would recurse infinitely.
    private final Bitlist mFreeBlocksBitlist;
    private long mFirstFreeBlock;

    // Used to clear contents of newly allocated blocks.
    private final byte[] mClearArray;
    
    // Maps file ids to InternalFiles. Use the mFileTable lock when accessing
    // this map.
    private final Map mOpenFiles;

    /**
     * Open an existing MultiplexFile.
     *
     * @param reserved Number of bytes to reserve before the MultiplexFile
     * header.
     */
    public MultiplexFile(FileBuffer fb, int reserved) throws IOException {
        this(fb, false, reserved, 0, 0, 0);
    }

    /*
     * Header format:
     *
     * reserved (variable)
     * magic number (4 bytes)
     * block size (4 bytes)
     * block id scale (1 byte)
     * length scale (1 byte)
     * total blocks used (variable: block id scale)
     * first free block id (variable: block id scale)
     * file table self entry (variable: length scale + block id scale)
     * free block bitlist entry (variable: length scale + block id scale)
     */

    /**
     * Creates a new MultiplexFile, destroying the contents of the given
     * FileBuffer. If the FileBuffer implements TxFileBuffer, MultiplexFile
     * will use its transaction features.
     *
     * @param fb FileBuffer to store data for MultiplexFile
     * @param reserved Number of bytes to reserve before the MultiplexFile
     * header.
     * @param blockSize Size of file blocks, in bytes
     * @param blockIdScale Number of bytes to use for storing block ids
     * @param lengthScale Number of bytes to use for storing file length
     */
    public MultiplexFile(FileBuffer fb, int reserved, int blockSize,
                         int blockIdScale, int lengthScale)
        throws IOException
    {
        this(fb, true, reserved, blockSize, blockIdScale, lengthScale);
    }

    private MultiplexFile(FileBuffer fb, boolean create, int reserved, 
                          int blockSize, int blockIdScale, int lengthScale)
        throws IOException
    {
        if (fb instanceof TxFileBuffer) {
            mBackingFile = (TxFileBuffer)fb;
        }
        else {
            mBackingFile = new NonTxFileBuffer(fb);
        }

        // Total blocks index is located at (reserved)  + (magic number size) +
        // (block size size) + (block id scale size) + (length scale size)
        mTotalBlocksPosition = reserved + 4 + 4 + 1 + 1;

        if (create) {
            if (blockSize <= 0) {
                throw new IllegalArgumentException("Block size must be > 0");
            }

            if (blockIdScale < 1 || blockIdScale > 8) {
                throw new IllegalArgumentException
                    ("Valid block id scale range is 1..8");
            }

            if (blockIdScale * 2 > blockSize) {
                // Intermediate mapping blocks need to have at least two
                // children.
                throw new IllegalArgumentException
                    ("Block size must be large enough " + 
                     "to contain two block ids: " + blockIdScale * 2 + " > " +
                     blockSize);
            }

            if (blockSize % blockIdScale != 0) {
                throw new IllegalArgumentException
                    ("Block size must be multiple of block id scale: " +
                     blockSize + " % " + blockIdScale + " = " +
                     (blockSize % blockIdScale));
            }

            if (lengthScale < 0 || lengthScale > 8) {
                // If the length scale is zero, the file lengths can only
                // the size of one block or zero. This offers more efficient
                // storage when implementing a table of fixed sized rows using
                // MultiFile. Block size is set to row size, and every file
                // represents a row.
                throw new IllegalArgumentException
                    ("Valid length scale range is 0..8");
            }

            mBlockSize = blockSize;
            mBlockIdScale = blockIdScale;
            mLengthScale = lengthScale;
        }
        else {
            DataInput din = new FileBufferInputStream(fb, reserved, false);
            if (MAGIC != din.readInt()) {
                throw new IOException("Incorrect magic number");
            }
            mBlockSize = din.readInt();
            mBlockIdScale = din.readByte();
            mLengthScale = din.readByte();
        }

        long max = (1L << (mBlockIdScale * 8)) - 1;
        if (max <= 1) {
            max = Long.MAX_VALUE;
        }
        mMaxBlockId = max;

        // Calculate the maximum file length.
        if (mLengthScale == 0) {
            max = mBlockSize;
        }
        else {
            max = (1L << (mLengthScale * 8));
            if (max <= 1) {
                max = Long.MAX_VALUE;
            }
            BigInteger maxAllocation =
                BigInteger.valueOf(mMaxBlockId).multiply
                (BigInteger.valueOf(mBlockSize));
            if (maxAllocation.compareTo(BigInteger.valueOf(max)) < 0) {
                // Max allocation is less than computed length.
                max = maxAllocation.longValue();
            }
            // TODO: The computation of max length does not take into account
            // the overhead associated with file tables.
        }
        mMaxLength = max;

        mFileTableEntrySize = mLengthScale + mBlockIdScale;

        if (mFileTableEntrySize > mBlockSize) {
            throw new IllegalArgumentException
                ("Block size must be large enough " + 
                 "to contain one file table entry: " + mFileTableEntrySize +
                 " > " + mBlockSize);
        }

        // First free block position is adjusted by size of total blocks field.
        mFirstFreeBlockPosition = mTotalBlocksPosition + mBlockIdScale;

        // File table self entry position is adjusted by size of first free
        // block entry.
        mFileTableSelfEntryPosition = mFirstFreeBlockPosition + mBlockIdScale;

        // Free blocks bitlist entry position is adjusted by size of file table
        // self entry.
        mFreeBlocksBitlistEntryPosition =
            mFileTableSelfEntryPosition + mFileTableEntrySize;

        int headerSize = mFreeBlocksBitlistEntryPosition + mFileTableEntrySize;

        if (create) {
            DataOutput dout = new FileBufferOutputStream(fb, reserved, false);

            dout.writeInt(MAGIC);
            dout.writeInt(mBlockSize);
            dout.writeByte((byte)mBlockIdScale);
            dout.writeByte((byte)mLengthScale);

            // Calculate initial total blocks as number of blocks consumed by
            // header. Note: mTotalBlocks always is one less than the actual
            // number of total.
            setTotalBlocks((headerSize - 1) / mBlockSize);
            // First free block of zero means none available.
            setFirstFreeBlock(0);
            putLengthEntry(fb, mFreeBlocksBitlistEntryPosition, 0);
            putLengthEntry(fb, mFileTableSelfEntryPosition, 0);
        }
        else {
            mTotalBlocks = getBlockId(fb, mTotalBlocksPosition);
            mFirstFreeBlock = getBlockId(fb, mFirstFreeBlockPosition);
        }

        // Pre-compute the maximum file sizes for each number of levels of
        // indirection. This array will be truncated because binary searches
        // will be applied on it.
        long[] levelMaxSizes = new long[64];
        long levelSize = mBlockSize;
        long blockIdsPerBlock = mBlockSize / mBlockIdScale;
        int level = 0;
        while (level < 64) {
            levelMaxSizes[level++] = levelSize;
            long nextLevelSize = levelSize * blockIdsPerBlock;
            if (nextLevelSize / blockIdsPerBlock == levelSize) {
                levelSize = nextLevelSize;
            }
            else {
                // Overflowed. Store max possible for highest level.
                levelMaxSizes[level++] = Long.MAX_VALUE;
                break;
            }
        }
        mLevelMaxSizes = new long[level];
        System.arraycopy(levelMaxSizes, 0, mLevelMaxSizes, 0, level);

        // Pre-compute the level exponents used in computing map node indexes.
        // These values are also the maximum number of blocks per level.
        // This array does not need to be truncated since no binary search is
        // applied to it.
        mLevelMaxBlocks = new long[64];
        long exponent = 1;
        level = 0;
        while (level < 64) {
            mLevelMaxBlocks[level++] = exponent;
            if (exponent < Long.MAX_VALUE) {
                long nextExponent = exponent * blockIdsPerBlock;
                if (nextExponent / blockIdsPerBlock == exponent) {
                    exponent = nextExponent;
                }
                else {
                    // Overflowed. Use largest value for remaining entries.
                    exponent = Long.MAX_VALUE;
                }
            }
        }

        mClearArray = new byte[Math.min(mBlockSize, 4096)];

        InternalFile internalFileTable =
            new InternalFile(fb, mFileTableSelfEntryPosition, false);
        mFileTableLock = internalFileTable.mLock;
        mFileTable = new IndirectFile(internalFileTable);
        mFreeBlocksBitlist = new Bitlist(new IndirectFile(new InternalFile
            (fb, mFreeBlocksBitlistEntryPosition, true)));

        mOpenFiles = new SoftHashMap();
    }

    /**
     * Opens a file within this MultiplexFile. If the file
     * has not yet been opened, it is created with a length of zero.
     */
    public FileBuffer openFile(long id) throws IOException {
        return new IndirectFile(openInternal(new Long(id)));
    }

    /**
     * Deletes the specified file. Opening it up again re-creates it.
     */
    public void deleteFile(long id) throws IOException {
        if (id >= getFileCount()) {
            return;
        }

        Long key = new Long(id);
        InternalFile file = openInternal(key);

        // Acquire write lock on file to delete before acquiring lock on
        // file table. This ensures a consistent lock order with all other
        // operations and prevents deadlock. Operations on internal files
        // will first lock the internal file, and then lock the shared file
        // table when necessary.
        try {
            file.lock().acquireWriteLock();
            // Use the file table lock to prevent multiple threads from using
            // mOpenFiles.
            try {
                mFileTableLock.acquireWriteLock();
                file.delete();
                mOpenFiles.remove(key);
            }
            finally {
                mFileTableLock.releaseLock();
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            file.lock().releaseLock();
        }
    }

    private InternalFile openInternal(Long key) throws IOException {
        if (key.longValue() < 0) {
            throw new IllegalArgumentException
                ("File id must be positive: " + key);
        }

        try {
            mFileTableLock.acquireUpgradableLock();
            InternalFile file = (InternalFile)mOpenFiles.get(key);
            if (file == null) {
                long id = key.longValue();
                long fileTablePos = id * mFileTableEntrySize;
                mFileTableLock.acquireWriteLock();
                try {
                    file = (InternalFile)mOpenFiles.get(key);
                    if (file != null) {
                        return file;
                    }
                    if (id >= getFileCount()) {
                        // Force the file table to expand in order to increment
                        // the file count.
                        putLengthEntry(mFileTable, fileTablePos, 0);
                    }
                    file = new InternalFile(mFileTable, fileTablePos, false);
                    mOpenFiles.put(key, file);
                }
                finally {
                    mFileTableLock.releaseLock();
                }
            }
            return file;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mFileTableLock.releaseLock();
        }
    }

    public long getFileCount() throws IOException {
        return mFileTable.size() / mFileTableEntrySize;
    }

    /**
     * Truncating the file count deletes any files with ids at or above the
     * new count.
     */
    public void truncateFileCount(long count) throws IOException {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0: " + count);
        }
        try {
            mFileTableLock.acquireUpgradableLock();
            long oldCount = getFileCount();
            if (count >= oldCount) {
                return;
            }
            
            mFileTableLock.acquireWriteLock();
            try {
                for (long id = oldCount; --id >= count; ) {
                    deleteFile(id);
                }
                mFileTable.truncate(count * mFileTableEntrySize);
            }
            finally {
                mFileTableLock.releaseLock();
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mFileTableLock.releaseLock();
        }
    }

    public int getBlockSize() {
        return mBlockSize;
    }

    public int getBlockIdScale() {
        return mBlockIdScale;
    }

    public int getLengthScale() {
        return mLengthScale;
    }

    public long getMaximumFileLength() {
        return mMaxLength;
    }

    public void dumpStructure(long id) throws IOException {
        InternalFile file = openInternal(new Long(id));
        System.out.println("Size: " + file.size());
        System.out.print("Block list: ");
        file.dumpBlocks();
    }

    // TODO: Provide a compaction method for releasing free blocks to the
    // underlying file system.

    // TODO: Provide a method for getting the free size.

    /**
     * Calculates the level of indirection needed to access a block in a file
     * of the given length. If the level is zero, the file table points
     * directly to a data block. If the level is one, the file table points to
     * file table block. A file table block contains block ids which map to
     * data blocks or more file table blocks.
     */
    final int calcLevels(long length) {
        // The calculation is essentially a math log operation, but the table
        // lookup is faster.
        int index = Arrays.binarySearch(mLevelMaxSizes, length);
        return (index < 0) ? ~index : index;
    }

    final long calcBlockCount(long length) {
        return (length + mBlockSize - 1) / mBlockSize;
    }

    final long allocBlock(boolean markInUse) throws IOException {
        try {
            mFreeBlocksBitlist.lock().acquireWriteLock();
            long blockId = mFirstFreeBlock;
            
            if (blockId <= 0) {
                // No free blocks, so expand backing file.
                blockId = mTotalBlocks + 1;
                if (blockId > mMaxBlockId) {
                    throw new IOException
                        ("Cannot allocate any more file blocks");
                }
                setTotalBlocks(blockId);
            }
            else {
                // Find next free block, and update the field.
                long nextFree = mFreeBlocksBitlist.findFirstSet(blockId + 1);
                if (nextFree <= 0) {
                    setFirstFreeBlock(0);
                }
                else {
                    setFirstFreeBlock(nextFree);
                }
            }
            
            if (markInUse) {
                mFreeBlocksBitlist.clear(blockId);
            }

            return blockId;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mFreeBlocksBitlist.lock().releaseLock();
        }
    }

    final void clearBlock(long blockId) throws IOException {
        clearSubBlock(blockId, 0, mBlockSize);
    }

    final void clearSubBlock(long blockId, int start, int length)
        throws IOException
    {
        if (length <= mClearArray.length) {
            mBackingFile.write(blockId * mBlockSize + start,
                               mClearArray, 0, length);
        }
        else {
            long pos = blockId * mBlockSize + start;
            do {
                int amt = Math.min(length, mClearArray.length);
                amt = mBackingFile.write(pos, mClearArray, 0, amt);
                pos += amt;
                length -= amt;
            } while (length > 0);
        }
    }

    /**
     * Attempting to free block 0 or a block that is already free has no
     * effect.
     */
    final void freeBlock(long blockId) throws IOException {
        if (blockId == 0) {
            // Can't free block 0.
        }
        else {
            try {
                mFreeBlocksBitlist.lock().acquireWriteLock();
                mFreeBlocksBitlist.set(blockId);
                if (mFirstFreeBlock == 0 || blockId < mFirstFreeBlock) {
                    setFirstFreeBlock(blockId);
                }
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                mFreeBlocksBitlist.lock().releaseLock();
            }
        }
    }

    final long getLengthEntry(FileBuffer buffer, long position)
        throws IOException
    {
        if (mLengthScale == 0) {
            if (getBlockId(buffer, position) == 0) {
                return 0;
            }
            return mBlockSize;
        }
        else {
            try {
                buffer.lock().acquireReadLock();
                long length =
                    getScaledValue(buffer, mLengthScale, position);
                if (length == 0 &&
                    getBlockId(buffer, position + mLengthScale) == 0) {
                    return 0;
                }
                return length + 1;
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                buffer.lock().releaseLock();
            }
        }
    }

    final long putLengthEntry(FileBuffer buffer, long position, long length) 
        throws IOException
    {
        if (mLengthScale == 0) {
            if (length != 0) {
                return mBlockSize;
            }
            putBlockId(buffer, position, 0);
        }
        else {
            try {
                buffer.lock().acquireWriteLock();
                if (length == 0) {
                    putScaledValue(buffer, mLengthScale, position, 0);
                    putBlockId(buffer, position + mLengthScale, 0);
                }
                else {
                    putScaledValue
                        (buffer, mLengthScale, position, length - 1);
                }
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                buffer.lock().releaseLock();
            }
        }
        return length;
    }

    final long getBlockId(FileBuffer buffer, long position) 
        throws IOException
    {
        return getScaledValue(buffer, mBlockIdScale, position);
    }

    final long getBlockId(byte[] tempArray, FileBuffer buffer, long position) 
        throws IOException
    {
        return getScaledValue(tempArray, buffer, mBlockIdScale, position);
    }

    /**
     * Returns 0 if referring block is 0.
     */
    final long getBlockId(long refBlockId, int index)
        throws IOException
    {
        if (refBlockId == 0) {
            return 0;
        }
        return getBlockId(mBackingFile, refBlockId * mBlockSize + index);
    }

    /**
     * Returns 0 if referring block is 0.
     */
    final long getBlockId(byte[] tempArray, long refBlockId, int index)
        throws IOException
    {
        if (refBlockId == 0) {
            return 0;
        }
        return getBlockId
            (tempArray, mBackingFile, refBlockId * mBlockSize + index);
    }

    final void putBlockId(FileBuffer buffer, long position, long blockId) 
        throws IOException
    {
        putScaledValue(buffer, mBlockIdScale, position, blockId);
    }

    final void putBlockId(byte[] tempArray,
                          FileBuffer buffer, long position, long blockId) 
        throws IOException
    {
        putScaledValue(tempArray, buffer, mBlockIdScale, position, blockId);
    }

    final void putBlockId(long refBlockId, int index, long blockId)
        throws IOException
    {
        putBlockId(mBackingFile, refBlockId * mBlockSize + index, blockId);
    }
    
    final void putBlockId(byte[] tempArray,
                          long refBlockId, int index, long blockId)
        throws IOException
    {
        putBlockId(tempArray, mBackingFile,
                   refBlockId * mBlockSize + index, blockId);
    }
    
    private void setTotalBlocks(long totalBlocks) throws IOException {
        mTotalBlocks = totalBlocks;
        putBlockId(mBackingFile, mTotalBlocksPosition, totalBlocks);
    }

    private void setFirstFreeBlock(long blockId) throws IOException {
        mFirstFreeBlock = blockId;
        putBlockId(mBackingFile, mFirstFreeBlockPosition, blockId);
    }

    private final static class IndirectFile
        implements FileBuffer, ReadWriteLock
    {
        private final InternalFile mFile;
        private final SecureReadWriteLock mLock;
        private boolean mClosed;

        IndirectFile(InternalFile file) {
            mFile = file;
            mLock = file.mLock;
        }

        public int read(long position, byte[] dst, int offset, int length)
            throws IOException
        {
            checkArgs(position, dst, offset, length);
            checkClosed();
            try {
                mLock.acquireReadLock();
                return mFile.read(position, dst, offset, length);
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
            checkClosed();
            try {
                mLock.acquireWriteLock();
                return mFile.write(position, src, offset, length);
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
            checkClosed();
            try {
                mLock.acquireReadLock();
                return mFile.read(position);
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
            checkClosed();
            try {
                mLock.acquireWriteLock();
                mFile.write(position, value);
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
                return mFile.size();
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
            checkClosed();
            try {
                mLock.acquireWriteLock();
                mFile.truncate(size);
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                mLock.releaseLock();
            }
        }

        public ReadWriteLock lock() {
            return this;
        }
        
        public void acquireReadLock() throws InterruptedException {
            checkClosedLock();
            mLock.acquireReadLock();
        }

        public boolean acquireReadLock(long timeout)
            throws InterruptedException
        {
            checkClosedLock();
            return mLock.acquireReadLock(timeout);
        }
        
        public void acquireUpgradableLock() throws InterruptedException {
            checkClosedLock();
            mLock.acquireUpgradableLock();
        }
        
        public boolean acquireUpgradableLock(long timeout)
            throws InterruptedException
        {
            checkClosedLock();
            return mLock.acquireUpgradableLock(timeout);
        }
        
        public void acquireWriteLock() throws InterruptedException {
            checkClosedLock();
            mLock.acquireWriteLock();
        }
        
        public boolean acquireWriteLock(long timeout)
            throws InterruptedException
        {
            checkClosedLock();
            return mLock.acquireWriteLock(timeout);
        }
        
        public boolean releaseLock() {
            if (mClosed) {
                // FileBuffer is closed.
                boolean result = false;
                try {
                    // Release all the locks held by this thread.
                    while (mLock.releaseLock()) {
                        result |= true;
                    }
                }
                finally {
                    return result;
                }
            }

            // FileBuffer is open, just release one lock.
            return mLock.releaseLock();
        }

        public long getDefaultTimeout() {
            return mLock.getDefaultTimeout();
        }
        
        public boolean force() throws IOException {
            checkClosed();
            try {
                mLock.acquireReadLock();
                return mFile.force();
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean isReadOnly() {
            return false;
        }

        public boolean isOpen() {
            return !mClosed;
        }
        
        public void close() throws IOException {
            mClosed = true;
            // Release all the locks held by this thread.
            // TODO: Figure out how to release locks held by all threads
            // that have used this IndirectFile.
            while (mLock.releaseLock());
        }
        
        private void checkArgs(long position,
                               byte[] array, int offset, int length) {
            if (position < 0) {
                throw new IllegalArgumentException
                    ("position < 0: " + position);
            }
            
            if (offset < 0) {
                throw new ArrayIndexOutOfBoundsException
                    ("offset < 0: " + offset);
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

        private void checkClosed() throws IOException {
            if (mClosed) {
                // FileBuffer is closed.
                try {
                    // Release all the locks held by this thread.
                    while (mLock.releaseLock());
                }
                finally {
                    throw new IOException("FileBuffer closed");
                }
            }
        }

        private void checkClosedLock() throws IllegalStateException {
            if (mClosed) {
                // FileBuffer is closed.
                try {
                    // Release all the locks held by this thread.
                    while (mLock.releaseLock());
                }
                finally {
                    throw new IllegalStateException("FileBuffer closed");
                }
            }
        }
    }

    /**
     * InternalFile doesn't do any bounds checks or locking.
     * Therefore, it must be wrapped with IndirectFile.
     */
    private final class InternalFile implements FileBuffer {
        // Lock is declared here so that is may be shared my multiple
        // IndirectFiles.
        final SecureReadWriteLock mLock = new SecureReadWriteLock();

        private FileBuffer mFileTableBuffer;
        private final long mFileTablePosition;
        private final boolean mIsFreeFile;
        private MapNode mRootNode;

        private long mLength = -1;
        // Levels of indirection before reaching data blocks. Must be
        // recalculated after adjusting length.
        private int mLevels;

        private byte[] mWriteOneByte;

        InternalFile(FileBuffer fileTableBuffer,
                     long fileTablePosition,
                     boolean isFreeFile)
            throws IOException
        {
            mFileTableBuffer = fileTableBuffer;
            mFileTablePosition = fileTablePosition;
            mIsFreeFile = isFreeFile;
            long blockId = getFileTableEntryBlockId();
            if (blockId != 0) {
                mRootNode = new MapNode(blockId);
            }
        }

        public int read(long position, byte[] dst, int offset, int length)
            throws IOException
        {
            checkDeleted();

            if (position >= size()) {
                return -1;
            }

            if (length == 0) {
                return 0;
            }

            if ((position + length) > size()) {
                length = (int)(size() - position);
            }

            return read(position, dst, offset, length, mLevels, mRootNode);
        }

        private int read(long position, byte[] dst, int offset, int length,
                         int level, MapNode node)
            throws IOException
        {
            if (level == 0) {
                // At level 0, the block contains actual file data.

                int indexInBlock = (int)(position % mBlockSize);
                int amt = mBlockSize - indexInBlock;

                if (amt > length) {
                    amt = length;
                }

                if (node == null) {
                    for (int i = amt; --i >= 0; ) {
                        dst[offset + i] = (byte)0;
                    }
                }
                else {
                    mBackingFile.read
                        (node.mBlockId * mBlockSize + indexInBlock,
                         dst, offset, amt);
                }

                return amt;
            }

            // Above level 0, file block contains pointers to other file
            // blocks. Compute index to first file block pointer.

            int index = ((int)(position / mLevelMaxBlocks[level] % mBlockSize))
                / mBlockIdScale;

            int originalOffset = offset;

            // Decrement level for recursion.
            level--;

            int blockIdsPerBlock = mBlockSize / mBlockIdScale;
            for (; index < blockIdsPerBlock; index++) {
                int amt =
                    read(position, dst, offset, length,
                         level, node == null ? null : node.getChild(index));
                if (amt <= 0) {
                    break;
                }
                offset += amt;
                if ((length -= amt) <= 0) {
                    break;
                }
                position += amt;
            }

            return offset - originalOffset;
        }

        public int write(long position, byte[] src, int offset, int length)
            throws IOException
        {
            checkDeleted();

            if (length == 0) {
                return 0;
            }

            if (position >= mMaxLength) {
                throw new EOFException("position > max file length: " +
                                       position + " > " + mMaxLength);
            }

            long newLength = position + length;
            long oldLength = size();

            if (newLength > mMaxLength) {
                newLength = mMaxLength;
                length = (int)(newLength - position);
            }

            mBackingFile.begin();

            if (newLength > oldLength) {
                // Expand length.
                final int newLevels = calcLevels(newLength);
                if (newLevels > mLevels && mRootNode != null) {
                    for (int i = mLevels; i < newLevels; i++) {
                        long blockId = allocBlock(!mIsFreeFile);
                        clearBlock(blockId);
                        MapNode newRoot = new MapNode(blockId);
                        newRoot.setChild(0, mRootNode);
                        mRootNode = newRoot;
                    }
                    putFileTableEntryBlockId(mRootNode.mBlockId);
                }

                mLength = putLengthEntry
                    (mFileTableBuffer, mFileTablePosition, newLength);
                mLevels = newLevels;
            }

            if (mRootNode == null) {
                long blockId = allocBlock(!mIsFreeFile);
                if (mLevels > 0 || mIsFreeFile) {
                    // This block will be used for mapping blocks, so ensure it
                    // is clear. Free file blocks also need to be cleared.
                    clearBlock(blockId);
                }
                mRootNode = new MapNode(blockId);
                putFileTableEntryBlockId(mRootNode.mBlockId);
            }

            if (position > oldLength) {
                // Clear the region in the file between the old end and the
                // position to write to.
                clearNewRegion(oldLength, position - oldLength);
            }

            int amt = write
                (position, src, offset, length, mLevels, mRootNode);

            mBackingFile.commit();
            return amt;
        }

        private int write(long position, byte[] src, int offset, int length,
                          int level, MapNode node)
            throws IOException
        {
            if (level == 0) {
                // At level 0, the block contains actual file data.

                int indexInBlock = (int)(position % mBlockSize);
                int amt = mBlockSize - indexInBlock;

                if (amt >= length) {
                    amt = length;
                }

                mBackingFile.write
                    (node.mBlockId * mBlockSize + indexInBlock,
                     src, offset, amt);

                return amt;
            }

            // Above level 0, file block contains pointers to other file
            // blocks. Compute index to first file block pointer.

            int index = ((int)(position / mLevelMaxBlocks[level] % mBlockSize))
                / mBlockIdScale;

            int originalOffset = offset;

            // Decrement level for recursion.
            level--;

            int blockIdsPerBlock = mBlockSize / mBlockIdScale;
            for (; index < blockIdsPerBlock; index++) {
                MapNode nextNode = node.getChild(index);
                //boolean nextDirty = false;

                if (nextNode == null) {
                    long newBlockId = allocBlock(!mIsFreeFile);
                    if (level > 0 || mIsFreeFile) {
                        // This block will be used for mapping blocks, so
                        // ensure it is clear. Free file blocks also need to
                        // be cleared.
                        clearBlock(newBlockId);
                    }
                    nextNode = node.setChild(index, newBlockId);
                }

                int amt = write(position, src, offset, length,
                                level, nextNode);
                if (amt <= 0) {
                    break;
                }
                offset += amt;
                if ((length -= amt) <= 0) {
                    break;
                }
                position += amt;
            }

            return offset - originalOffset;
        }

        private void clearNewRegion(long position, long length)
            throws IOException
        {
            // Compute the first "seam": The first block boundary at or after
            // the to-be-cleared position.
            long seam = (position + mBlockSize - 1) / mBlockSize * mBlockSize;
            long len = seam - position;
            if (len > 0) {
                if (len > length) {
                    len = length;
                }
                writeClear(position, len);
                if (len >= length) {
                    return;
                }
            }
            
            // Since the middle of the clear region of the file hasn't
            // been written to yet, no blocks should be allocated in the
            // middle. It can be skipped.
            
            // Compute the second "seam": The first block boundary at or before
            // the end of the to-be-cleared region.
            seam = (position + length) / mBlockSize * mBlockSize;
            len = position + length - seam;
            if (len > 0) {
                writeClear(seam, len);
            }
        }

        /**
         * This method should not be used to expand the length of a file unless
         * it does not increase in the number of levels.
         */
        private void writeClear(long position, long length)
            throws IOException
        {
            if (length <= mClearArray.length) {
                write(position, mClearArray, 0, (int)length,
                      mLevels, mRootNode);
                return;
            }

            do {
                int amt = (int)Math.min(length, (long)mClearArray.length);
                amt = write(position, mClearArray, 0, amt, mLevels, mRootNode);
                position += amt;
                length -= amt;
            } while (length > 0);
        }

        public int read(long position) throws IOException {
            checkDeleted();

            if (position >= size()) {
                return -1;
            }

            return read(position, mLevels, mRootNode);
        }

        private int read(long position, int level, MapNode node)
            throws IOException
        {
            if (node == null) {
                return 0;
            }

            if (level == 0) {
                // At level 0, the block contains actual file data.
                int indexInBlock = (int)(position % mBlockSize);

                int value = mBackingFile.read
                    (node.mBlockId * mBlockSize + indexInBlock);

                return value < 0 ? 0 : value;
            }

            // Above level 0, file block contains pointers to other file
            // blocks. Compute index to first file block pointer.

            int index = ((int)(position / mLevelMaxBlocks[level] % mBlockSize))
                / mBlockIdScale;

            return read(position, level - 1, node.getChild(index));
        }

        public void write(long position, int value) throws IOException {
            // Writing to the file is more complicated than reading, so I'm
            // not going to put much work into optimizing the one byte write.
            byte[] writeOne;
            if ((writeOne = mWriteOneByte) == null) {
                writeOne = mWriteOneByte = new byte[1];
            }
            writeOne[0] = (byte)value;
            write(position, writeOne, 0, 1);
        }

        public long size() throws IOException {
            checkDeleted();
            if (mLength < 0) {
                mLength = getLengthEntry(mFileTableBuffer, mFileTablePosition);
                mLevels = calcLevels(mLength);
            }
            return mLength;
        }
        
        public void truncate(long size) throws IOException {
            checkDeleted();

            if (size >= size()) {
                return;
            }

            long blockCount = calcBlockCount(mLength);
            long newBlockCount = calcBlockCount(size);
            long blocksToFree = blockCount - newBlockCount;
            MapNode rootNode = mRootNode;
            long rootBlockId = rootNode == null ? 0 : rootNode.mBlockId;

            mBackingFile.begin();

            mLength = putLengthEntry
                (mFileTableBuffer, mFileTablePosition, size);

            if (blocksToFree == 0) {
                mBackingFile.commit();
                return;
            }

            // Possibly set a new root node to accomodate the reduced size.
            // Do this before freeing blocks. If the system crashes while
            // freeing blocks, then the file is still okay, but there will be a
            // bunch of unreachable blocks.

            int newLevels;
            List chain = null;

            if (size == 0) {
                putFileTableEntryBlockId(0);
                mRootNode = null;
                    
                if (mLevels == 0) {
                    freeBlock(rootBlockId);
                    mBackingFile.commit();
                    return;
                }

                newLevels = mLevels;
            }
            else {
                newLevels = calcLevels(size);
                if (newLevels < mLevels) {
                    chain = new ArrayList(64);
                    MapNode newRoot = rootNode;
                    for (int i=mLevels; newRoot!=null && i>newLevels; i--) {
                        chain.add(newRoot);
                        newRoot = newRoot.getChild(0);
                    }
                    putFileTableEntryBlockId
                        (newRoot == null ? 0 : newRoot.mBlockId);
                    mRootNode = newRoot;
                }
            }

            // Free up all necessary blocks, starting from rootNode.

            long amt = truncateBlocks
                (blockCount - 1, blocksToFree, mLevels, rootNode);

            if (amt > 0) {
                freeBlock(rootBlockId);
            }
            else if (chain != null) {
                // Free the chain of blocks leading to the new root.
                for (int i=chain.size(); --i >= 0; ) {
                    freeBlock(((MapNode)chain.get(i)).mBlockId);
                }
            }

            mLevels = newLevels;

            mBackingFile.commit();
        }

        /**
         * Returns amount freed. Is positive if totally freed, ~amt if
         * partially freed.
         */
        private long truncateBlocks(long endBlockId, long blocksToFree,
                                    int level, MapNode node)
            throws IOException
        {
            if (level == 0) {
                // Recursion to level zero should never happen.
                return 1;
            }

            int blockIdsPerBlock = mBlockSize / mBlockIdScale;

            if (node == null) {
                // Return what would be freed.
                long amt = endBlockId % blockIdsPerBlock + 1;
                if (amt > blocksToFree) {
                    return ~blocksToFree;
                }
                else {
                    return amt;
                }
            }

            // Above level 0, recurse down to lower levels. Compute index
            // to end file block pointer and determine if this block will be
            // totally freed.

            int index;
            boolean totallyFreed;
            if (level <= 1) {
                index = (int)(endBlockId % blockIdsPerBlock);
                totallyFreed = blocksToFree > index;
            }
            else {
                index = (int)(endBlockId / mLevelMaxBlocks[level - 1] %
                              blockIdsPerBlock);
                totallyFreed = blocksToFree >
                    (endBlockId % mLevelMaxBlocks[level]);
            }

            long original = blocksToFree;

            // Decrement level for recursion.
            level--;

            for (; index >= 0 && blocksToFree > 0; index--) {
                MapNode childNode = node.getChild(index);

                long amt = truncateBlocks
                    (endBlockId, blocksToFree, level, childNode);

                if (amt < 0) {
                    amt = ~amt;
                }
                else if (childNode != null) {
                    freeBlock(childNode.mBlockId);
                    if (!totallyFreed) {
                        // Since this node will not be totally freed, be
                        // sure to clear the reference to the child node.
                        node.setChild(index, 0);
                    }
                }
                
                endBlockId -= amt;
                blocksToFree -= amt;
            }

            if (totallyFreed) {
                return original - blocksToFree;
            }
            else {
                return ~(original - blocksToFree);
            }
        }

        public ReadWriteLock lock() {
            return mLock;
        }

        public boolean force() throws IOException {
            checkDeleted();
            return mBackingFile.force();
        }

        public boolean isReadOnly() throws IOException {
            checkDeleted();
            return false;
        }

        public boolean isOpen() {
            return mFileTableBuffer != null;
        }

        public void close() throws IOException {
            // Buffer may be closed only by deleting it.
        }

        void delete() throws IOException {
            try {
                mLock.acquireWriteLock();
                truncate(0);
                mFileTableBuffer = null;
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                mLock.releaseLock();
            }
        }

        void dumpBlocks() throws IOException {
            try {
                mLock.acquireReadLock();
                dumpBlocks(0, size(), mLevels, mRootNode);
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            finally {
                mLock.releaseLock();
            }
            System.out.println();
        }

        private long dumpBlocks(long position, long length,
                                int level, MapNode node)
            throws IOException
        {
            if (length == 0) {
                return 0;
            }

            if (level == 0) {
                if (node == null) {
                    System.out.print("null ");
                }
                else {
                    System.out.print(node.mBlockId);
                    System.out.print(' ');
                }
                return length - mBlockSize;
            }

            int index = ((int)(position / mLevelMaxBlocks[level] % mBlockSize))
                / mBlockIdScale;

            // Decrement level for recursion.
            level--;

            int blockIdsPerBlock = mBlockSize / mBlockIdScale;
            for (; index < blockIdsPerBlock; index++) {
                long newLength = dumpBlocks
                    (position, length, level,
                     node == null ? null : node.getChild(index));
                long amt = length - newLength;
                if ((length = newLength) <= 0) {
                    break;
                }
                position += amt;
            }

            return length;
        }

        private long getFileTableEntryBlockId() throws IOException {
            return getBlockId
                (mFileTableBuffer, mFileTablePosition + mLengthScale);
        }

        private void putFileTableEntryBlockId(long blockId)
            throws IOException
        {
            putBlockId
                (mFileTableBuffer, mFileTablePosition + mLengthScale, blockId);
        }

        private void checkDeleted() throws IOException {
            if (mFileTableBuffer == null) {
                throw new FileNotFoundException("FileBuffer deleted");
            }
        }
    }

    private class MapNode {
        public final long mBlockId;

        private final byte[] mTemp = new byte[8];

        // A cache to child MapNodes.
        private Reference[] mChildren;

        public MapNode(long blockId) {
            mBlockId = blockId;
        }

        /**
         * Returns null if no child at index.
         */
        public synchronized MapNode getChild(int index) throws IOException {
            if (mChildren == null) {
                mChildren = new Reference[mBlockSize / mBlockIdScale];
            }
            Reference ref = mChildren[index];
            MapNode child;
            if (ref == null || (child = (MapNode)ref.get()) == null) {
                long childId =
                    getBlockId(mTemp, mBlockId, index * mBlockIdScale);
                if (childId == 0) {
                    return null;
                }
                child = new MapNode(childId);
                mChildren[index] = new SoftReference(child);
            }
            return child;
        }

        /**
         * Returns null if set child block id is zero.
         */
        public synchronized MapNode setChild(int index, long childId)
            throws IOException
        {
            if (mChildren == null) {
                mChildren = new Reference[mBlockSize / mBlockIdScale];
            }
            putBlockId(mTemp, mBlockId, index * mBlockIdScale, childId);
            if (childId == 0) {
                mChildren[index] = null;
                return null;
            }
            MapNode child = new MapNode(childId);
            mChildren[index] = new SoftReference(child);
            return child;
        }

        /**
         * Return null if set child is null or child id is zero.
         */
        public MapNode setChild(int index, MapNode child) throws IOException {
            return setChild(index, child == null ? 0L : child.mBlockId);
        }

        public String toString() {
            return super.toString() + ':' + mBlockId;
        }
    }
}

