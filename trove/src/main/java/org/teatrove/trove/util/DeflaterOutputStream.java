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

package org.teatrove.trove.util;

import java.io.*;

/**
 * Just like {@link java.util.zip.DeflaterOutputStream}, except uses a
 * different {@link Deflater} implementation.
 *
 * @author Brian S O'Neill
 * @version
 */
public class DeflaterOutputStream extends FilterOutputStream {
    protected Deflater mDeflater;
    protected byte[] mBuffer;

    public DeflaterOutputStream(OutputStream out, Deflater def, byte[] buf) {
        super(out);
        mDeflater = def;
        mBuffer = buf;
    }
   
    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        this(out, def, new byte[size]);
    }

    public DeflaterOutputStream(OutputStream out, Deflater def) {
        this(out, def, 512);
    }

    public DeflaterOutputStream(OutputStream out) {
        this(out, new Deflater());
    }

    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (mDeflater.finished()) {
            throw new IOException("write beyond end of stream");
        }
        mDeflater.setInput(b, off, len);
        while (!mDeflater.needsInput()) {
            deflate();
        }
    }

    public void flush() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.flush();
            int len;
            do {
                len = deflate();
            } while (len == mBuffer.length);
        }
    }

    public void fullFlush() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.fullFlush();
            int len;
            do {
                len = deflate();
            } while (len == mBuffer.length);
        }
    }

    public void finish() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.finish();
            do {
                deflate();
            } while (!mDeflater.finished());
        }
    }

    public void close() throws IOException {
        finish();
        super.close();
    }
    
    private int deflate() throws IOException {
        int len = mDeflater.deflate(mBuffer, 0, mBuffer.length);
        if (len > 0) {
            out.write(mBuffer, 0, len);
        }
        return len;
    }
}
