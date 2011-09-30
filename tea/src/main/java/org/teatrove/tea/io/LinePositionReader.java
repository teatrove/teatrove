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
 * LinePositionReader aids in printing line numbers for error reporting. 
 *
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class LinePositionReader extends PositionReader {
    private int mLineNumber = 1;
    private int mPushback = -1;

    public LinePositionReader(Reader reader) {
        super(reader);
    }

    public int read() throws IOException {
        int c;

        if (mPushback >= 0) {
            c = mPushback;
            mPushback = -1;
        }
        else {
            c = super.read();
        }

        if (c == '\n') {
            mLineNumber++;
        }
        else if (c == '\r') {
            int peek = super.read();
            if (peek != '\n') {
                mLineNumber++;
            }
            mPushback = peek;
        }

        return c;
    }

    /**
     * After calling readLine, calling getLineNumber returns the next line
     * number.
     */
    public String readLine() throws IOException {
        StringBuffer buf = new StringBuffer(80);
        int line = mLineNumber;
        int c;
        while (line == mLineNumber && (c = read()) >= 0) {
            buf.append((char)c);
        }
        return buf.toString();
    }

    /**
     * Skips forward into the stream to the line number specified. The line
     * can then be read by calling readLine. Calling getPosition
     * returns the position that the line begins.
     *
     * @return the line number reached
     */
    public int skipForwardToLine(int line) throws IOException {
        while (mLineNumber < line && read() >= 0) {}
        return mLineNumber;
    }

    /**
     * @return the number of the line currently being read or the next one
     * available.
     */
    public int getLineNumber() {
        return mLineNumber;
    }

    /**
     * Converts all whitespace characters in a String to space characters
     * (\u0020).
     */
    public static String cleanWhitespace(String str) {
        int length = str.length();

        StringBuffer buf = new StringBuffer(length);
        for (int i=0; i<length; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                buf.append(' ');
            }
            else {
                buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * Creates and returns a String containing a sequence of the specified
     * length, repeating the given character.
     */
    public static String createSequence(char c, int length) {
        if (length < 0) length = 1;

        StringBuffer buf = new StringBuffer(length);
        for (; length > 0; length--) {
            buf.append(c);
        }
        return buf.toString();
    }
}
