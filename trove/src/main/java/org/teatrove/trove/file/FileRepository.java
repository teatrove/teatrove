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
import java.io.FileNotFoundException;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * Allows files to be created with automatically assigned identifiers. Files
 * should not be opened or deleted unless the identifier was returned by
 * createFile.
 *
 * @author Brian S O'Neill
 */
public interface FileRepository {
    /**
     * Returns the count of files in this repository.
     */
    long fileCount() throws IOException;

    /**
     * Returns an iterator of all the file ids in this repository.
     */
    Iterator fileIds() throws IOException;

    /**
     * Returns true if the file by the given id exists.
     */
    boolean fileExists(long id) throws IOException;

    /**
     * Returns the FileBuffer for the file by the given id.
     *
     * @throws FileNotFoundException if the file doesn't exist
     */
    FileBuffer openFile(long id) throws IOException, FileNotFoundException;

    /**
     * Returns the id of the newly created file, which is never zero.
     */
    long createFile() throws IOException;

    /**
     * Returns false if the file by the given doesn't exist.
     */
    boolean deleteFile(long id) throws IOException;

    /**
     * Lock access to FileRepository methods. Use of this lock does not
     * restrict operations on open FileBuffers.
     * <p>
     * When only a read lock is held, no files may be created or deleted. When
     * an upgradable lock is held, only the owner thread can create or delete
     * files. When a write lock is held, all operations are suspended except
     * for the owner thread.
     */
    ReadWriteLock lock();

    public interface Iterator {
        boolean hasNext() throws IOException;

        long next() throws IOException;
    }
}
