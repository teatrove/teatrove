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
 * Various utilities for operating on a file at the bit level.
 *
 * @author Brian S O'Neill
 */
public class Bitlist {
    private static int findSubIndex(byte v) {
        return (v<0)?0:((v<16)?((v<4)?((v<2)?7:6):((v<8)?5:4)):((v<64)?((v<32)?3:2):1));
    }

    private final FileBuffer mFile;

    public Bitlist(FileBuffer file) {
        mFile = file;
    }

    /**
     * Set the bit at the given index to one.
     */
    public void set(long index) throws IOException {
        long pos = index >> 3;
        try {
            lock().acquireUpgradableLock();
            int value = mFile.read(pos);
            int newValue;
            if (value <= 0) {
                newValue = (0x00000080 >> (index & 7));
            }
            else {
                newValue = value | (0x00000080 >> (index & 7));
            }
            if (newValue != value) {
                mFile.write(pos, newValue);
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
    }

    /**
     * Clear the bit at the given index to zero.
     */
    public void clear(long index) throws IOException {
        long pos = index >> 3;
        try {
            lock().acquireUpgradableLock();
            int value = mFile.read(pos);
            int newValue;
            if (value <= 0) {
                newValue = 0;
            }
            else {
                newValue = value & (0xffffff7f >> (index & 7));
            }
            if (newValue != value) {
                mFile.write(pos, newValue);
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
    }

    /**
     * Returns false if bit is clear (zero), true if bit is set (one). If bit
     * is beyond the file buffer size, false is returned.
     */
    public boolean get(long index) throws IOException {
        try {
            lock().acquireReadLock();
            int value = mFile.read(index >> 3);
            return value > 0 && ((value << (index & 7)) & 0x80) != 0;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
    }

    /**
     * Searches the bitlist for the first set bit and returns the index to it.
     *
     * @param start first index to begin search.
     */
    public long findFirstSet(long start) throws IOException {
        return findFirstSet(start, new byte[128]);
    }

    /**
     * Searches the bitlist for the first set bit and returns the index to it.
     *
     * @param start first index to begin search.
     * @param temp temporary byte array for loading bits.
     * @return index to first set bit or -1 if not found.
     */
    public long findFirstSet(long start, byte[] temp) throws IOException {
        long pos = start >> 3;
        try {
            lock().acquireReadLock();
            while (true) {
                int amt = mFile.read(pos, temp, 0, temp.length);
                if (amt <= 0) {
                    return -1;
                }
                for (int i=0; i<amt; i++, pos++) {
                    byte val;
                    if ((val = temp[i]) != 0) {
                        long index = pos << 3;
                        if (index < start) {
                            // Clear the upper bits to skip check.
                            val &= (0x000000ff >>> (start - index));
                            if (val == 0) {
                                // False alarm.
                                continue;
                            }
                        }
                        return index + findSubIndex(val);
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
    }

    /**
     * Searches the bitlist for the first clear bit and returns the index to
     * it.
     *
     * @param start first index to begin search.
     */
    public long findFirstClear(long start) throws IOException {
        return findFirstClear(start, new byte[128]);
    }

    /**
     * Searches the bitlist for the first clear bit and returns the index to
     * it.
     *
     * @param start first index to begin search.
     * @param temp temporary byte array for loading bits.
     * @return index to first set bit or -1 if not found.
     */
    public long findFirstClear(long start, byte[] temp) throws IOException {
        long pos = start >> 3;
        try {
            lock().acquireReadLock();
            while (true) {
                int amt = mFile.read(pos, temp, 0, temp.length);
                if (amt <= 0) {
                    return -1;
                }
                for (int i=0; i<amt; i++, pos++) {
                    byte val;
                    if ((val = temp[i]) != -1) {
                        long index = pos << 3;
                        if (index < start) {
                            // Set the upper bits to skip check.
                            val |= (0xffffff00 >> (start - index));
                            if (val == -1) {
                                // False alarm.
                                continue;
                            }
                        }
                        return index + findSubIndex((byte)(val ^ 0xff));
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
    }

    /**
     * Counts all the set bits.
     */
    public long countSetBits() throws IOException {
        byte[] temp;
        long size = mFile.size();
        if (size > 1024) {
            temp = new byte[1024];
        }
        else {
            temp = new byte[(int)size];
        }

        long pos = 0;
        long count = 0;
        try {
            lock().acquireReadLock();
            while (true) {
                int amt = mFile.read(pos, temp, 0, temp.length);
                if (amt <= 0) {
                    break;
                }
                for (int i=0; i<amt; i++) {
                    byte val = temp[i];
                    switch (val & 15) {
                    default: break;
                    case 1: case 2: case 4: case 8: count++; break;
                    case 3: case 5: case 6: case 9: case 10: case 12:
                        count += 2; break;
                    case 7: case 11: case 13: case 14: count += 3; break;
                    case 15: count += 4; break;
                    }
                    switch ((val >> 4) & 15) {
                    default: break;
                    case 1: case 2: case 4: case 8: count++; break;
                    case 3: case 5: case 6: case 9: case 10: case 12:
                        count += 2; break;
                    case 7: case 11: case 13: case 14: count += 3; break;
                    case 15: count += 4; break;
                    }
                }
                pos += amt;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
        return count;
    }

    /**
     * Counts all the clear bits.
     */
    public long countClearBits() throws IOException {
        byte[] temp;
        long size = mFile.size();
        if (size > 1024) {
            temp = new byte[1024];
        }
        else {
            temp = new byte[(int)size];
        }

        long pos = 0;
        long count = 0;
        try {
            lock().acquireReadLock();
            while (true) {
                int amt = mFile.read(pos, temp, 0, temp.length);
                if (amt <= 0) {
                    break;
                }
                for (int i=0; i<amt; i++) {
                    byte val = temp[i];
                    switch (val & 15) {
                    case 0: count += 4; break;
                    case 1: case 2: case 4: case 8: count += 3; break;
                    case 3: case 5: case 6: case 9: case 10: case 12:
                        count += 2; break;
                    case 7: case 11: case 13: case 14: count++; break;
                    default: break;
                    }
                    switch ((val >> 4) & 15) {
                    case 0: count += 4; break;
                    case 1: case 2: case 4: case 8: count += 3; break;
                    case 3: case 5: case 6: case 9: case 10: case 12:
                        count += 2; break;
                    case 7: case 11: case 13: case 14: count++; break;
                    default: break;
                    }
                }
                pos += amt;
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }
        return count;
    }

    /**
     * Counts all the set bits.
     *
     * @param start index to start counting, inclusive.
     * @param end index to stop counting, exclusive. If negative, count to the
     * end.
     * @param temp temporary byte array for loading bits.
     */
    /*
    public long countSetBits(long start, long end, byte[] temp)
        throws IOException
    {
    }
    */

    /**
     * Returns the number of bits in this list.
     */
    public long size() throws IOException {
        return mFile.size() * 8;
    }

    public ReadWriteLock lock() {
        return mFile.lock();
    }

    public boolean force() throws IOException {
        return mFile.force();
    }
}
