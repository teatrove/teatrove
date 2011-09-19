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

package org.teatrove.barista.http;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

/**
 * @author Brian S O'Neill
 */
public class ServletInputStreamImpl extends ServletInputStream {
    private InputStream mIn;
    
    public ServletInputStreamImpl(InputStream in) {
        mIn = in;
    }
    
    public int read() throws IOException {
        return mIn.read();
    }
    
    public int read(byte b[], int off, int len) throws IOException {
        return mIn.read(b, off, len);
    }
    
    public long skip(long n) throws IOException {
        return mIn.skip(n);
    }
    
    public int available() throws IOException {
        return mIn.available();
    }
    
    public void close() throws IOException {
        mIn.close();
    }
    
    public void mark(int readlimit) {
        mIn.mark(readlimit);
    }
    
    public void reset() throws IOException {
        mIn.reset();
    }
    
    public boolean markSupported() {
        return mIn.markSupported();
    }
}
