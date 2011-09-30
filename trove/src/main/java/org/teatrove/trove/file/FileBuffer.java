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
 * Interface for accessing and modifying the contents of a file. When a lock is
 * requested on a FileBuffer, a stronger one may be acquired, if the FileBuffer
 * doesn't support the type requested.
 *
 * @author Brian S O'Neill
 */
public interface FileBuffer {
    /**
     * Read bytes into the given array, returning the actual amount read.
     */
    int read(long position, byte[] dst, int offset, int length)
        throws IOException;

    /**
     * Write bytes from the given array, returning the actual amount written.
     */
    int write(long position, byte[] src, int offset, int length)
        throws IOException;

    /**
     * Read one byte at the given position, returning -1 if end of file.
     */
    int read(long position) throws IOException;

    /**
     * Write one byte at the given position.
     */
    void write(long position, int value) throws IOException;
    
    long size() throws IOException;

    void truncate(long size) throws IOException;

    ReadWriteLock lock();

    /**
     * Forces unwritten data to the physical disk.
     *
     * @return false if partially forced.
     */
    boolean force() throws IOException;

    boolean isReadOnly() throws IOException;

    boolean isOpen();

    void close() throws IOException;
}
