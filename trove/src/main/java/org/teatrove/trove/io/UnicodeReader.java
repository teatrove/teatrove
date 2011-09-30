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
 * This reader handles unicode escapes in a character stream as defined by
 * <i>The Java Language Specification</i>. 
 * 
 * <p>A unicode escape consists of six characters: '\' and 'u' followed by 
 * four hexadecimal digits. If the format of the escape is not correct, then 
 * the escape is unprocessed. To prevent a correctly formatted unicode escape 
 * from being processed, preceed it with another '\'.
 *
 * @author Brian S O'Neill
 */
public class UnicodeReader extends EscapeReader {
    /** Just a temporary buffer for holding the four hexadecimal digits. */
    private char[] mMinibuf = new char[4];

    private boolean mEscaped;

    /**
     * A UnicodeReader needs an underlying source Reader.
     *
     * @param source the source PositionReader
     */
    public UnicodeReader(Reader source) {
        super(source, 6);
    }

    public int read() throws IOException {
        int c = mSource.read();

        if (c != '\\' || !mEscapesEnabled) {
            mEscaped = false;
            return c;
        }

        c = mSource.read();

        // Have scanned "\\"? (two backslashes)
        if (c == '\\') {
            mEscaped = !mEscaped;
            mSource.unread();
            return '\\';
        }

        // Have not scanned '\', 'u'?
        if (c != 'u') {
            mSource.unread();
            return '\\';
        }

        // At this point, have scanned '\', 'u'.

        // If previously escaped, then don't process unicode escape.
        if (mEscaped) {
            mEscaped = false;
            mSource.unread();
            return '\\';
        }

        int len = mSource.read(mMinibuf, 0, 4);
        
        if (len == 4) {
            try {
                int val = 
                    Integer.valueOf(new String(mMinibuf, 0, 4), 16).intValue();

                return val;
            }
            catch (NumberFormatException e) {
                // If the number is not a parseable as hexadecimal, then
                // treat this as a bad format and do not process the
                // unicode escape.
            }
        }

        // Unread the four hexadecimal characters and the leading 'u'.
        if (len >= 0) {
            mSource.unread(len + 1);
        }

        return '\\';
    }

    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    private static class Tester {
        public static void test(String[] arg) throws Exception {
            String str = 
                "This is \\" + "u0061 test.\n" +
                "This is \\" + "u00612 test.\n" +
                "This is \\" + "u0061" + "\\" + "u0061" + " test.\n" +
                "This is \\" + "u061 test.\n" +
                "This is \\\\" + "u0061 test.\n" +
                "This is \\" + "a test.\n";

            System.out.println("\nOriginal:\n");
            
            Reader reader = new StringReader(str);

            int c;
            while ( (c = reader.read()) >= 0 ) {
                System.out.print((char)c);
            }

            System.out.println("\nConverted:\n");
            
            reader = new StringReader(str);
            reader = new UnicodeReader(reader);

            while ( (c = reader.read()) != -1 ) {
                System.out.print((char)c);
            }

            System.out.println("\nUnread test 1:\n");
            
            reader = new StringReader(str);
            PushbackPositionReader pr = 
                new PushbackPositionReader(new UnicodeReader(reader), 1);

            while ( (c = pr.read()) != -1 ) {
                pr.unread();
                c = pr.read();
                System.out.print((char)c);
            }

            System.out.println("\nUnread test 2:\n");
            
            reader = new StringReader(str);
            pr = new PushbackPositionReader(new UnicodeReader(reader), 2);

            int i = 0;
            while ( (c = pr.read()) != -1 ) {
                if ( (i++ % 5) == 0 ) {
                    c = pr.read();
                    pr.unread();
                    pr.unread();
                    c = pr.read();
                }

                System.out.print((char)c);
            }

            System.out.println("\nUnread position test:\n");

            reader = new StringReader(str);
            pr = new PushbackPositionReader(new UnicodeReader(reader), 2);

            System.out.print(pr.getNextPosition() + "\t");
            i = 0;
            while ( (c = pr.read()) != -1 ) {
                if ( (i++ % 5) == 0 ) {
                    c = pr.read();
                    pr.unread();
                    pr.unread();
                    c = pr.read();
                }

                System.out.println((char)c);
                System.out.print(pr.getNextPosition() + "\t");
            }
        }
    }
}
