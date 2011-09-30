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

import java.io.Writer;
import java.io.IOException;

/**
 * A Writer that writes into a CharToByteBuffer.
 * 
 * @author Brian S O'Neill
 */
public class CharToByteBufferWriter extends Writer {
    private CharToByteBuffer mBuffer;
    private boolean mClosed;

    public CharToByteBufferWriter(CharToByteBuffer buffer) {
        mBuffer = buffer;
    }

    public void write(int c) throws IOException {
        checkIfClosed();
        mBuffer.append((char)c);
    }

    public void write(char[] chars) throws IOException {
        checkIfClosed();
        mBuffer.append(chars);
    }

    public void write(char[] chars, int offset, int length) 
        throws IOException {
        checkIfClosed();
        mBuffer.append(chars, offset, length);
    }

    public void write(String str) throws IOException {
        checkIfClosed();
        mBuffer.append(str);
    }

    public void write(String str, int offset, int length) throws IOException {
        checkIfClosed();
        mBuffer.append(str, offset, length);
    }

    public void flush() throws IOException {
        checkIfClosed();
    }

    public void close() {
        mClosed = true;
    }

    private void checkIfClosed() throws IOException {
        if (mClosed) {
            throw new IOException("Writer closed");
        }
    }
}
