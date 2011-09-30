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
 * 
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
*/
public class TagReader extends EscapeReader {
    private int mTagCount;
    private int[] mTagStarts;
    private String[] mTagEnds;
    private int[] mCodes;

    private char[] mMinibuf;

    private static int maxLength(String[] tags) {
        int max = 0;

        for (int i=0; i<tags.length; i++) {
            if (tags[i].length() > max) {
                max = tags[i].length();
            }
        }

        return max;
    }

    public TagReader(Reader source, String[] tags, int[] codes) {
        super(source, maxLength(tags));

        mTagCount = tags.length;
        mTagStarts = new int[mTagCount];
        mTagEnds = new String[mTagCount];
        mCodes = new int[mTagCount];

        for (int i=0; i<mTagCount; i++) {
            mTagStarts[i] = tags[i].charAt(0);
            mTagEnds[i] = tags[i].substring(1);
            mCodes[i] = codes[i];
        }
        
        mMinibuf = new char[maxLength(tags)];
    }

    public int read() throws IOException {
        int c = mSource.read();

        if (c == -1 || !mEscapesEnabled) {
            return c;
        }

        for (int i=0; i<mTagCount; i++) {
            if (mTagStarts[i] == c) {
                int length = mTagEnds[i].length();

                mMinibuf[0] = (char)c;
                int len = mSource.read(mMinibuf, 0, length);

                if (len == length) {
                    if (new String(mMinibuf, 0, length).equals(mTagEnds[i])) {
                        return mCodes[i];
                    }
                }
     
                if (len > 0) {
                    mSource.unread(len);
                }
            }
        }

        return c;
    }

    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    private static class Tester {
        public static void test(String[] arg) throws Exception {
            String str = "This <%is a %> % > > % %% >> < % test.\n";

            System.out.println("\nOriginal: " + str);

            System.out.println("\nConverted:\n");
            
            Reader reader = new StringReader(str);
            
            TagReader tr = new TagReader
                (reader, new String[] {"<%", "%>"}, new int[] {-2, -3});

            PositionReader pr = new PositionReader(tr);

            int c;
            System.out.print(pr.getNextPosition() + "\t");
            while ( (c = pr.read()) != -1 ) {
                System.out.println((char)c + "\t" + c);
                System.out.print(pr.getNextPosition() + "\t");
            }
        }
    }
}
