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
import org.teatrove.trove.io.AbstractDataOutputStream;

/**
 * An OutputStream interface to a FileBuffer which supports repositioning.
 * FileBufferOutputStream is not thread-safe, but then its uncommon for
 * multiple threads to write to the same OutputStream.
 * 
 * @author Brian S O'Neill
 */
public class FileBufferOutputStream extends AbstractDataOutputStream
    implements DataOutput
{
    private FileBuffer mFileBuffer;
    private long mPosition;
    private final boolean mCloseBuffer;

    /**
     * Creates a FileBufferOutputStream initially positioned at the beginning
     * of the file. When this OutputStream is closed, the underlying
     * FileBuffer is also closed.
     *
     * @param fb FileBuffer to write to
     */
    public FileBufferOutputStream(FileBuffer fb) {
        this(fb, 0, true);
    }

    /**
     * Creates a FileBufferOutputStream with any start position.
     *
     * @param fb FileBuffer to write to
     * @param position Initial write position
     * @param closeBuffer When true, FileBuffer is closed when this
     * OutputStream is closed.
     */
    public FileBufferOutputStream(FileBuffer fb,
                                  long position,
                                  boolean closeBuffer) {
        mFileBuffer = fb;
        mPosition = position;
        mCloseBuffer = closeBuffer;
    }

    public void write(int b) throws IOException {
        checkClosed();
        mFileBuffer.write(mPosition, b);
        mPosition++;
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int offset, int length) throws IOException {
        checkClosed();

        if (length == 0) {
            return;
        }

        int amt;

        do {
            amt = mFileBuffer.write(mPosition, b, offset, length);
            if (amt <= 0) {
                break;
            }
            mPosition += amt;
            offset += amt;
            length -= amt;
        } while (length > 0);
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

    protected void finalize() throws IOException {
        close();
    }

    private void checkClosed() throws IOException {
        if (mFileBuffer == null) {
            throw new IOException("Stream closed");
        }
    }
}
