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
import org.teatrove.trove.io.AbstractDataInputStream;

/**
 * An InputStream interface to a FileBuffer which supports marking and
 * repositioning. FileBufferInputStream is not thread-safe, but then its
 * uncommon for multiple threads to read from the same InputStream.
 *
 * @author Brian S O'Neill
 */
public class FileBufferInputStream extends AbstractDataInputStream
    implements DataInput
{
    private FileBuffer mFileBuffer;
    private long mPosition;
    private long mMark;
    private final boolean mCloseBuffer;

    /**
     * Creates a FileBufferInputStream initially positioned at the beginning of
     * the file. When this InputStream is closed, the underlying FileBuffer is
     * also closed.
     *
     * @param fb FileBuffer to read from
     */
    public FileBufferInputStream(FileBuffer fb) {
        this(fb, 0, true);
    }

    /**
     * Creates a FileBufferInputStream with any start position.
     *
     * @param fb FileBuffer to read from
     * @param position Initial read position
     * @param closeBuffer When true, FileBuffer is closed when this
     * InputStream is closed.
     */
    public FileBufferInputStream(FileBuffer fb,
                                 long position,
                                 boolean closeBuffer) {
        mFileBuffer = fb;
        mPosition = position;
        mCloseBuffer = closeBuffer;
    }

    public int read() throws IOException {
        checkClosed();
        int value = mFileBuffer.read(mPosition);
        if (value >= 0) {
            mPosition++;
        }
        return value;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        checkClosed();

        if (length == 0) {
            return 0;
        }

        int originalOffset = offset;
        int amt;

        do {
            amt = mFileBuffer.read(mPosition, b, offset, length);
            if (amt <= 0) {
                break;
            }
            mPosition += amt;
            offset += amt;
            length -= amt;
        } while (length > 0);

        amt = offset - originalOffset;;
        return amt == 0 ? -1 : amt;
    }

    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long newPos = mPosition + n;
        long size = mFileBuffer.size();
        if (newPos > size) {
            newPos = size;
            n = newPos - mPosition;
        }
        mPosition = newPos;
        return n;
    }

    public int available() throws IOException {
        long avail = mFileBuffer.size() - mPosition;
        if (avail > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        else if (avail <= 0) {
            return 0;
        }
        else {
            return (int)avail;
        }
    }

    public void mark(int readlimit) {
        mMark = mPosition;
    }

    public void reset() {
        mPosition = mMark;
    }

    public boolean markSupported() {
        return true;
    }

    public void close() throws IOException {
        if (mFileBuffer != null) {
            if (mCloseBuffer) {
                mFileBuffer.close();
            }
            mFileBuffer = null;
        }
    }

    public boolean isOpen() {
        return mFileBuffer != null;
    }

    public long position() throws IOException {
        checkClosed();
        return mPosition;
    }

    public void position(long position) throws IOException {
        checkClosed();
        if (position < 0) {
            throw new IllegalArgumentException("Position < 0: " + position);
        }
        mPosition = position;
    }

    public String readLine() throws IOException {
        StringBuffer buf = null;

    loop:
        while (true) {
            int c = read();

            if (c < 0) {
                break;
            }
            else if (buf == null) {
                buf = new StringBuffer(128);
            }

            switch (c) {
            case '\n':
                break loop;
                
            case '\r':
                long oldPos = mPosition;
                int c2 = read();
                if (c2 != '\n' && c2 != -1) {
                    mPosition = oldPos;
                }
                break loop;
                
            default:
                buf.append((char)c);
                break;
            }
        }

        return buf == null ? null : buf.toString();
    }

    protected void finalize() throws IOException {
        close();
    }

    private void checkClosed() throws IOException {
        if (mFileBuffer == null) {
            throw new IOException("Stream closed");
        }
    }
}
