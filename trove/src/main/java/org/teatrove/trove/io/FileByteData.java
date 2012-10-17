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

package org.teatrove.trove.io;

import java.io.OutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * A ByteData implementation that reads the contents of a file.
 *
 * @author Brian S O'Neill
 */
public class FileByteData implements ByteData {
    private static final Object NULL = new Object();
    
    private File mFile;

    // Thread-local reference to a RandomAccessFile.
    private ThreadLocal<Object> mRAF = new ThreadLocal<Object>();

    public FileByteData(File file) {
        mFile = file;
        // Open here so that if file isn't found, error is logged earlier.
        open();
    }

    public long getByteCount() throws IOException {
        // Keep file open to ensure that length doesn't change between call to
        // getByteCount and writeTo.

        RandomAccessFile raf = open();
        if (raf == null) {
            return 0;
        }
        else {
            return raf.length();
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        RandomAccessFile raf = open();
        if (raf == null) {
            return;
        }

        try {
            long length = raf.length();
            int bufSize;
            if (length > 4000) {
                bufSize = 4000;
            }
            else {
                bufSize = (int)length;
            }
            
            byte[] inputBuffer = new byte[bufSize];

            raf.seek(0);
        
            int readAmount;
            while ((readAmount = raf.read(inputBuffer, 0, bufSize)) > 0) {
                out.write(inputBuffer, 0, readAmount);
            }
        }
        finally {
            try {
                finalize();
            }
            catch (IOException e) {
            }
        }
    }

    public void reset() throws IOException {
        Object obj = mRAF.get();
        try {
            if (obj instanceof RandomAccessFile) {
                ((RandomAccessFile)obj).close();
            }
        }
        finally {
            mRAF.set(null);
        }
    }
    
    protected final void finalize() throws IOException {
        reset();
    }

    private RandomAccessFile open() {
        Object obj = mRAF.get();
        if (obj instanceof RandomAccessFile) {
            return (RandomAccessFile)obj;
        }
        else if (obj == NULL) {
            return null;
        }

        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(mFile, "r");
            mRAF.set(raf);
        }
        catch (IOException e) {
            mRAF.set(NULL);
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
        }

        return raf;
    }
}
