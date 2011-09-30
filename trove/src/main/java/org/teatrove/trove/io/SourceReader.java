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
 * The SourceReader provides several services for reading source input.
 * It calculates line numbers, position in the source file, supports two
 * character pushback, extracts code from text that allows mixed code and
 * plain text, and it processes unicode escape sequences that appear in
 * source code.
 *
 * <p>Readers return -1 when the end of the stream, has been reached, and so
 * does SourceReader. SourceReader will also return other special negative
 * values to indicate a tag substitution. ENTER_CODE is returned to indicate
 * that characters read are in source code, and ENTER_TEXT is returned to
 * indicate that characters read are in plain text. The first character read
 * from a SourceReader is either ENTER_CODE or ENTER_TEXT;
 *
 * @author Brian S O'Neill
 */
public class SourceReader extends PushbackPositionReader {
    public static final int ENTER_CODE = -2;
    public static final int ENTER_TEXT = -3;

    private static Reader createReader(Reader source, 
                                       String beginTag, String endTag) {
        String[] tags = new String[4];
        int[] codes = new int[4];

        int i = 0;
        // Convert different kinds of line breaks into the newline character.
        tags[i] = "\r\n"; codes[i++] = '\n';
        tags[i] = "\r"; codes[i++] = '\n';

        if (beginTag != null && beginTag.length() > 0) {
            tags[i] = beginTag; codes[i++] = ENTER_CODE;
        }

        if (endTag != null && endTag.length() > 0) {
            tags[i] = endTag; codes[i++] = ENTER_TEXT;
        }

        if (i < 4) {
            String[] newTags = new String[i];
            System.arraycopy(tags, 0, newTags, 0, i);
            tags = newTags;

            int[] newCodes = new int[i];
            System.arraycopy(codes, 0, newCodes, 0, i);
            codes = newCodes;
        }

        return new UnicodeReader(new TagReader(source, tags, codes));
    }

    private UnicodeReader mUnicodeReader;
    private TagReader mTagReader;
    private boolean mClosed = false;

    // The current line in the source. (1..)
    private int mLine = 1;

    private int mFirst;

    private String mBeginTag;
    private String mEndTag;

    /**
     * The begin and end tags for a SourceReader are optional. If the begin
     * tag is null or has zero length, then the SourceReader starts reading
     * characters as if they were source code.
     *
     * <p>If the end tag is null or has zero length, then a source code
     * region continues to the end of the input Reader's characters.
     *
     * @param source the source reader
     * @param beginTag tag that marks the beginning of a source code region
     * @param endTag tag that marks the end of a source code region
     */ 
    public SourceReader(Reader source, String beginTag, String endTag) {
        this(source, beginTag, endTag, false);
    }

    /**
     * The begin and end tags for a SourceReader are optional. If the begin
     * tag is null or has zero length, then the SourceReader starts reading
     * characters as if they were source code.
     *
     * <p>If the end tag is null or has zero length, then a source code
     * region continues to the end of the input Reader's characters.
     *
     * @param source the source reader
     * @param beginTag tag that marks the beginning of a source code region
     * @param endTag tag that marks the end of a source code region
     * @param inCode flag that indicates if the stream is starting in code
     */ 
    public SourceReader(Reader source, String beginTag, String endTag, 
                        boolean inCode) {
        super(createReader(source, beginTag, endTag), 2);
        mUnicodeReader = (UnicodeReader)in;
        mTagReader = (TagReader)mUnicodeReader.getOriginalSource();

        boolean codeMode = ((beginTag == null || beginTag.length() == 0) || 
                            inCode);
        mFirst = (codeMode) ? ENTER_CODE : ENTER_TEXT;

        mBeginTag = beginTag;
        mEndTag = endTag;
    }

    public String getBeginTag() {
        return mBeginTag;
    }

    public String getEndTag() {
        return mEndTag;
    }

    /** 
     * All newline character patterns are are converted to \n. 
     */
    public int read() throws IOException {
        int c;

        if (mFirst != 0) {
            c = mFirst;
            mFirst = 0;
        }
        else {
            c = super.read();
        }

        if (c == '\n') {
            mLine++;
        }
        else if (c == ENTER_CODE) {
            mUnicodeReader.setEscapesEnabled(true);
        }
        else if (c == ENTER_TEXT) {
            mUnicodeReader.setEscapesEnabled(false);
        }
        
        return c;
    }

    public int getLineNumber() {
        return mLine;
    }

    /**
     * The position in the reader where the last read character ended. The
     * position of the first character read from a Reader is zero.
     *
     * <p>The end position is usually the same as the start position, but
     * sometimes a SourceReader may combine multiple characters into a
     * single one.
     *
     * @return the end position where the last character was read
     */
    public int getEndPosition() {
        int e = getNextPosition() - 1;
        return (e < getStartPosition()) ? getStartPosition() : e;
    }

    public void ignoreTags(boolean ignore) {
        mTagReader.setEscapesEnabled(!ignore);
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() throws IOException {
        mClosed = true;
        super.close();
    }

    protected void unreadHook(int c) {
        if (c == '\n') {
            mLine--;
        }
        else if (c == ENTER_CODE) {
            mUnicodeReader.setEscapesEnabled(false);
        }
        else if (c == ENTER_TEXT) {
            mUnicodeReader.setEscapesEnabled(true);
        }
    }

    /** 
     * Simple test program 
     */
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
                "This is \\" + "a test.\n" +
                "Plain text <%put code here%> plain text.\n" +
                "Plain text <\\" + "u0025put code here%> plain text.\n" +
                "Plain text <%put code here\\" + "u0025> plain text.\n";

            Reader reader;

            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                System.out.println("\nOriginal:\n");
                
                reader = new StringReader(str);

                int c;
                while ( (c = reader.read()) != -1 ) {
                    System.out.print((char)c);
                }
            }

            System.out.println("\nTest 1:\n");

            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                reader = new StringReader(str);
            }

            SourceReader sr = new SourceReader(reader, "<%", "%>");

            int c;
            while ( (c = sr.read()) != -1 ) {
                System.out.print((char)c);
                System.out.print("\t" + c);
                System.out.print("\t" + sr.getLineNumber());
                System.out.print("\t" + sr.getStartPosition());
                System.out.println("\t" + sr.getEndPosition());
            }

            System.out.println("\nTest 2:\n");
            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                reader = new StringReader(str);
            }

            sr = new SourceReader(reader, null, null);

            while ( (c = sr.read()) != -1 ) {
                System.out.print((char)c);
                System.out.print("\t" + c);
                System.out.print("\t" + sr.getLineNumber());
                System.out.print("\t" + sr.getStartPosition());
                System.out.println("\t" + sr.getEndPosition());
            }
        }
    }
}
