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

import java.io.*;

/**
 * Doesn't increment a counter for each byte written. FastDataOutputStream is
 * not thread-safe, but then its uncommon for multiple threads to write to the 
 * same OutputStream.
 *
 * @author Brian S O'Neill
 */
public class FastDataOutputStream extends AbstractDataOutputStream
    implements DataOutput
{
    protected final OutputStream mOut;

    public FastDataOutputStream(OutputStream out) {
        mOut = out;
    }

    public void write(int b) throws IOException {
        mOut.write(b);
    }

    public void write(byte[] b) throws IOException {
        mOut.write(b);
    }

    public void write(byte[] b, int offset, int length) throws IOException {
        mOut.write(b, offset, length);
    }

    public void flush() throws IOException {
        mOut.flush();
    }

    public void close() throws IOException {
        mOut.close();
    }
}
