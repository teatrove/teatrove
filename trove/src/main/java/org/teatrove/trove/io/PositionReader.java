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
 * The PositionReader tracks the postion in the stream of the next character 
 * to be read. PositionReaders chain together such that the position is
 * read from the earliest PositionReader in the chain.
 *
 * <p>Position readers automatically close the underlying input stream when
 * the end of file is reached. Ordinary input streams don't do this.
 * 
 * @author Brian S O'Neill
 */
public class PositionReader extends FilterReader {
    /** Is non-null when this PositionReader is chained to another. */
    protected PositionReader mPosReader;

    protected int mPosition = 0;

    private boolean mClosed = false;

    public PositionReader(Reader reader) {
        super(reader);
        if (reader instanceof PositionReader) {
            mPosReader = (PositionReader)reader;
        }
    }

    /**
     * @return the position of the next character to be read.
     */
    public int getNextPosition() {
        return mPosition;
    }

    public int read() throws IOException {
        try {
            int c;
            if ((c = in.read()) != -1) {
                if (mPosReader == null) {
                    mPosition++;
                }
                else {
                    mPosition = mPosReader.getNextPosition();
                }
            }
            else {
                close();
            }
            
            return c;
        }
        catch (IOException e) {
            if (mClosed) {
                return -1;
            }
            else {
                throw e;
            }
        }
    }

    public int read(char[] buf, int off, int length) throws IOException {
        int i = 0;
        while (i < length) {
            int c;
            if ((c = read()) == -1) {
                return (i == 0)? -1 : i;
            }
            buf[i++ + off] = (char)c;
        }

        return i;
    }

    public void close() throws IOException {
        mClosed = true;
        super.close();
    }
}
