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

package org.teatrove.teaservlet.io;

import java.io.*;

/**
 * Provides references to temporary files that are automatically deleted when
 * closed, finalized, or when the system exits.
 *
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class TemporaryFile extends RandomAccessFile {
    /**
     * Creates a new writable temporary file that is deleted when closed,
     * finalized, or when the system exits.
     *
     * @see File#createTempFile
     */
    public static RandomAccessFile createTemporaryFile
        (String prefix, String suffix, File directory) throws IOException {

        File file = File.createTempFile(prefix, suffix, directory);
        file.deleteOnExit();
        return new TemporaryFile(file);
    }

    /**
     * Creates a new writable temporary file that is deleted when closed,
     * finalized, or when the system exits.
     *
     * @see File#createTempFile
     */
    public static RandomAccessFile createTemporaryFile
        (String prefix, String suffix) throws IOException {

        return createTemporaryFile(prefix, suffix, null);
    }

    private File mFile;

    private TemporaryFile(File file) throws IOException {
        super(file, "rw");
        mFile = file;
    }

    public void close() throws IOException {
        try {
            super.close();
        }
        finally {
            mFile.delete();
        }
    }

    protected void finalize() throws IOException {
        close();
    }
}
