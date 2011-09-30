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

package org.teatrove.trove.net;

import java.io.*;
import java.net.Socket;
import org.teatrove.trove.io.*;

/**
 * A convenient class for supporting buffering on socket I/O streams.
 *
 * @author Brian S O'Neill
 */
public class BufferedSocket extends SocketFaceWrapper {
    private final int mInBufSize;
    private final int mOutBufSize;

    private InputStream mIn;
    private OutputStream mOut;

    public BufferedSocket(Socket s) throws IOException {
        this(s, -1, -1);
    }

    public BufferedSocket(SocketFace s) throws IOException {
        this(s, -1, -1);
    }

    /**
     * @param inputBufferSize specify 0 for no buffering, -1 for default
     * @param outputBufferSize specify 0 for no buffering, -1 for default
     */
    public BufferedSocket(Socket s,
                          int inputBufferSize,
                          int outputBufferSize) throws IOException {
        super(s);
        mInBufSize = inputBufferSize;
        mOutBufSize = outputBufferSize;
    }
    
    /**
     * @param inputBufferSize specify 0 for no buffering, -1 for default
     * @param outputBufferSize specify 0 for no buffering, -1 for default
     */
    public BufferedSocket(SocketFace s,
                          int inputBufferSize,
                          int outputBufferSize) throws IOException {
        super(s);
        mInBufSize = inputBufferSize;
        mOutBufSize = outputBufferSize;
    }
    
    public synchronized InputStream getInputStream() throws IOException {
        if (mIn == null) {
            InputStream in = super.getInputStream();

            if (mInBufSize < 0) {
                mIn = new FastBufferedInputStream(in);
            }
            else if (mInBufSize > 0) {
                mIn = new FastBufferedInputStream(in, mInBufSize);
            }
            else {
                mIn = in;
            }
        }
        return mIn;
    }
    
    public synchronized OutputStream getOutputStream() throws IOException {
        if (mOut == null) {
            OutputStream out = super.getOutputStream();

            if (mOutBufSize < 0) {
                mOut = new FastBufferedOutputStream(out);
            }
            else if (mOutBufSize > 0) {
                mOut = new FastBufferedOutputStream(out, mOutBufSize);
            }
            else {
                mOut = out;
            }
        }
        return mOut;
    }
    
    public synchronized void close() throws IOException {
        // Ensure buffered output is flushed.
        if (mOutBufSize != 0 && mOut != null) {
            mOut.close();
        }
        super.close();
    }
}
