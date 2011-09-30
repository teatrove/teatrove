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
import org.teatrove.trove.util.ReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 */
public class ReadOnlyFileBuffer implements FileBuffer {
    private static final IOException fail() {
        return new IOException("FileBuffer is read-only");
    }

    private final FileBuffer mFile;

    public ReadOnlyFileBuffer(FileBuffer file) {
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
        throw fail();
    }

    public int read(long position) throws IOException {
        return mFile.read(position);
    }

    public void write(long position, int value) throws IOException {
        throw fail();
    }
    
    public long size() throws IOException {
        return mFile.size();
    }

    public void truncate(long size) throws IOException {
        throw fail();
    }

    public ReadWriteLock lock() {
        return mFile.lock();
    }

    public boolean force() throws IOException {
        throw fail();
    }

    public boolean isReadOnly() throws IOException {
        return true;
    }

    public boolean isOpen() {
        return mFile.isOpen();
    }

    public void close() throws IOException {
        mFile.close();
    }
}
