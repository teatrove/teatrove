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

import java.io.IOException;
import java.io.OutputStream;

/**
 * DualOutput wraps two OutputStreams so they can be written to simultaneously.
 * This is handy for writing to a file and a class injector at the same time.
 *
 * @author Brian S O'Neill
 * @see ClassInjector
 */

public class DualOutput extends OutputStream {
    private OutputStream mOut1;
    private OutputStream mOut2;

    public DualOutput(OutputStream out1, OutputStream out2) {
        mOut1 = out1;
        mOut2 = out2;
    }

    public void write(int b) throws IOException {
        try {
            mOut1.write(b);
        }
        finally {
            mOut2.write(b);
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        try {
            mOut1.write(b, off, len);
        }
        finally {
            mOut2.write(b, off, len);
        }
    }

    public void flush() throws IOException {
        try {
            mOut1.flush();
        }
        finally {
            mOut2.flush();
        }
    }

    public void close() throws IOException {
        try {
            mOut1.close();
        }
        finally {
            mOut2.close();
        }
    }
}        
