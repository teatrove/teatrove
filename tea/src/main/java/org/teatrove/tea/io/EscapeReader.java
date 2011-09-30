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

package org.teatrove.tea.io;

import java.io.*;

/**
 * This reader aids in decoding escapes in a character stream.
 *
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public abstract class EscapeReader extends PositionReader {
    protected PushbackPositionReader mSource;
    protected boolean mEscapesEnabled = true;

    private Reader mOriginal;

    /**
     * An EscapeReader needs an underlying source reader.
     *
     * @param source the source Reader
     * @param escapeSize the number of characters in an escape code
     */
    public EscapeReader(Reader source, int escapeSize) {
        super(new PushbackPositionReader(source, escapeSize));
        mSource = (PushbackPositionReader)in;
        mOriginal = source;
    }

    public Reader getOriginalSource() {
        return mOriginal;
    }

    /**
     * Escapes are enabled by default.
     */
    public boolean isEscapesEnabled() {
        return mEscapesEnabled;
    }

    /**
     * Enable or disable the processing of escapes. When disabled, this
     * Reader only functions as a PushbackReader.
     */
    public void setEscapesEnabled(boolean enabled) {
        mEscapesEnabled = enabled;
    }

    /**
     * @return the position of the next character to be read.
     */
    public int getNextPosition() {
        return mSource.getNextPosition();
    }
}
