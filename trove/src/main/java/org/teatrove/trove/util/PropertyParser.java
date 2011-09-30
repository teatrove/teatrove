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

import org.teatrove.trove.io.SourceInfo;
import org.teatrove.trove.io.SourceReader;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

/**
 * Parses a properties file similar to how {@link java.util.Properties} does,
 * except:
 *
 * <ul>
 * <li>Values have trailing whitespace trimmed.
 * <li>Quotation marks ( " or ' ) can be used to define keys and values that
 * have embedded spaces.
 * <li>Quotation marks can also be used to define multi-line keys and values
 * without having to use continuation characters.
 * <li>Properties may be nested using braces '{' and '}'.
 * </ul>
 *
 * Just like Properties, comment lines start with optional whitespace followed
 * by a '#' or '!'. Keys and values may have an optional '=' or ':' as a
 * separator, unicode escapes are supported as well as other common escapes.
 * A line may end in a backslash so that it continues to the next line.
 * Escapes for brace characters '{' and '}' are also supported.
 *
 * Example:
 *
 * <pre>
 * # Properties file
 *
 * foo = bar
 * foo.sub = blink
 * empty
 *
 * block {
 *     inner {
 *         foo = bar
 *         item
 *     }
 *     next.item = "true"
 * }
 *
 * section = test {
 *     level = 4
 *     message = "Message: "
 * }
 * </pre>
 *
 * is equivalent to
 *
 * <pre>
 * # Properties file
 *
 * foo = bar
 * foo.sub = blink
 * empty
 *
 * block.inner.foo = bar
 * block.inner.item
 * block.next.item = true
 *
 * section = test
 * section.level = 4
 * section.message = Message: 
 * </pre>
 *
 * @author Brian S O'Neill
 */
public class PropertyParser {
    // Parsed grammer (EBNF) is:
    //
    // Properties   ::= { PropertyList }
    // PropertyList ::= { Property | COMMENT }
    // Property     ::= KEY [ VALUE ] [ Block ]
    // Block        ::= LBRACE PropertyList RBRACE

    private Map mMap;

    private Vector mPropertyListeners = new Vector(64);
    private Vector mErrorListeners = new Vector(1);
    private int mErrorCount = 0;

    private Scanner mScanner;

    /**
     * @param map Map to receive properties
     */
    public PropertyParser(Map map) {
        mMap = map;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        mPropertyListeners.addElement(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener){
        mPropertyListeners.removeElement(listener);
    }

    private void dispatchPropertyChange(PropertyChangeEvent e) {
        synchronized (mPropertyListeners) {
            for (int i= 0; i<mPropertyListeners.size(); i++) {
                ((PropertyChangeListener)mPropertyListeners.elementAt(i)).
                    propertyChanged(e);
            }
        }
    }

    private void propertyChange(String key, String value, SourceInfo info) {
        dispatchPropertyChange(new PropertyChangeEvent(this, key, value, 
                                                       info));
    }

    public void addErrorListener(ErrorListener listener) {
        mErrorListeners.addElement(listener);
    }
    
    public void removeErrorListener(ErrorListener listener) {
        mErrorListeners.removeElement(listener);
    }
    
    private void dispatchParseError(ErrorEvent e) {
        mErrorCount++;
        
        synchronized (mErrorListeners) {
            for (int i = 0; i < mErrorListeners.size(); i++) {
                ((ErrorListener)mErrorListeners.elementAt(i)).parseError(e);
            }
        }
    }
    
    private void error(String str, SourceInfo info) {
        dispatchParseError(new ErrorEvent(this, str, info));
    }

    private void error(String str, Token token) {
        error(str, token.getSourceInfo());
    }

    /**
     * Parses properties from the given reader and stores them in the Map. To
     * capture any parsing errors, call addErrorListener prior to parsing.
     */    
    public void parse(Reader reader) throws IOException {
        mScanner = new Scanner(reader);

        mScanner.addErrorListener(new ErrorListener() {
            public void parseError(ErrorEvent e) {
                dispatchParseError(e);
            }
        });

        try {
            parseProperties();
        }
        finally {
            mScanner.close();
        }
    }

    private void parseProperties() throws IOException {
        Token token;
        while ((token = peek()).getId() != Token.EOF) {
            switch (token.getId()) {

            case Token.KEY:
            case Token.LBRACE:  
            case Token.COMMENT:
                parsePropertyList(null);
                break;

            case Token.RBRACE:
                token = read();
                error("No matching left brace", token);
                break;

            default:
                token = read();
                error("Unexpected token: " + token.getValue(), token);
                break;
            }
        }
    }

    private void parsePropertyList(String keyPrefix) throws IOException {
        Token token;

    loop:
        while ((token = peek()).getId() != Token.EOF) {
            switch (token.getId()) {

            case Token.KEY:
                token = read();
                parseProperty(keyPrefix, token);
                break;
                
            case Token.COMMENT:
                read();
                break;

            case Token.LBRACE:
                read();
                error("Nested properties must have a base name", token);
                parseBlock(keyPrefix);
                break;
                
            default:
                break loop;
            }
        }
    }

    private void parseProperty(String keyPrefix, Token token)
        throws IOException {

        String key = token.getValue();
        if (keyPrefix != null) {
            key = keyPrefix + key;
        }

        String value = null;

        if (peek().getId() == Token.VALUE) {
            token = read();
            value = token.getValue();
        }

        if (peek().getId() == Token.LBRACE) {
            read();
            parseBlock(key + '.');
        }
        else if (value == null) {
            value = "";
        }

        if (value != null) {
            putProperty(key, value, token);
            propertyChange(key, value, token.getSourceInfo());
        }
    }

    // When this is called, the LBRACE token has already been read.
    private void parseBlock(String keyPrefix) throws IOException {
        parsePropertyList(keyPrefix);
            
        Token token;
        if ((token = peek()).getId() == Token.RBRACE) {
            read();
        }
        else {
            error("Right brace expected", token);
        }
    }

    private void putProperty(String key, String value, Token token) {
        if (mMap.containsKey(key)) {
            error("Property \"" + key + "\" already defined", token);
        }
        mMap.put(key, value);
    }
    
    /**
     * Total number of errors accumulated by this PropertyParser instance.
     */
    public int getErrorCount() {
        return mErrorCount;
    }

    private Token read() throws IOException {
        return mScanner.readToken();
    }

    private Token peek() throws IOException {
        return mScanner.peekToken();
    }

    /**
     * 
     * @author Brian S O'Neill
     * @version

     */
    public static interface ErrorListener extends java.util.EventListener {
        public void parseError(ErrorEvent e);
    }

    /**
     * 
     * @author Brian S O'Neill
     * @version

     */
    public static class ErrorEvent extends java.util.EventObject {
        private String mErrorMsg;
        private SourceInfo mInfo;

        ErrorEvent(Object source, String errorMsg, SourceInfo info) {
            super(source);
            mErrorMsg = errorMsg;
            mInfo = info;
        }
        
        public String getErrorMessage() {
            return mErrorMsg;
        }
        
        /**
         * Returns the error message prepended with source file information.
         */
        public String getDetailedErrorMessage() {
            String prepend = getSourceInfoMessage();
            if (prepend == null || prepend.length() == 0) {
                return mErrorMsg;
            }
            else {
                return prepend + ": " + mErrorMsg;
            }
        }

        public String getSourceInfoMessage() {
            if (mInfo == null) {
                return "";
            }
            else {
                return String.valueOf(mInfo.getLine());
            }
        }
        
        /**
         * This method reports on where in the source code an error was found.
         *
         * @return Source information on this error or null if not known.
         */
        public SourceInfo getSourceInfo() {
            return mInfo;
        }
    }

    /**
     * 
     * @author Brian S O'Neill
     * @version

     */
    private static class Token implements java.io.Serializable {
        public final static int UNKNOWN = 0;
        public final static int EOF = 1;
        
        public final static int COMMENT = 2;
        public final static int KEY = 3;
        public final static int VALUE = 4;
        
        public final static int LBRACE = 5;
        public final static int RBRACE = 6;

        private final static int LAST_ID = 6;
    
        private int mTokenId;
        private String mValue;
        private SourceInfo mInfo;

        Token(int sourceLine,
              int sourceStartPos, 
              int sourceEndPos,
              int tokenId,
              String value) {
            
            mTokenId = tokenId;
            mValue = value;
            
            if (tokenId > LAST_ID) {
                throw new IllegalArgumentException("Token Id out of range: " +
                                                   tokenId);
            }
            
            mInfo = new SourceInfo(sourceLine, sourceStartPos, sourceEndPos);
            
            if (sourceStartPos > sourceEndPos) {
                // This is an internal error.
                throw new IllegalArgumentException
                    ("Token start position greater than " + 
                     "end position at line: " + sourceLine);
            }
        }
    
        public Token(SourceInfo info, int tokenId, String value) {
            mTokenId = tokenId;
        
            if (tokenId > LAST_ID) {
                throw new IllegalArgumentException("Token Id out of range: " +
                                                   tokenId);
            }
            
            mInfo = info;
        }

        public final int getId() {
            return mTokenId;
        }

        /**
         * Token code is non-null, and is exactly the same as the name for
         * its Id.
         */
        public String getCode() {
            return Code.TOKEN_CODES[mTokenId];
        }

        public final SourceInfo getSourceInfo() {
            return mInfo;
        }
        
        public String getValue() {
            return mValue;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(10);

            String image = getCode();
            
            if (image != null) {
                buf.append(image);
            }
            
            String str = getValue();
            
            if (str != null) {
                if (image != null) {
                    buf.append(' ');
                }
                buf.append('"');
                buf.append(str);
                buf.append('"');
            }
            
            return buf.toString();
        }

        private static class Code {
            public static final String[] TOKEN_CODES =
            {
                "UNKNOWN",
                "EOF",

                "COMMENT",
                "KEY",
                "VALUE",

                "LBRACE",
                "RBRACE",
            };
        }
    }

    /**
     * 
     * @author Brian S O'Neill
     * @version

     */
    private static class Scanner {
        private SourceReader mSource;

        /** The scanner supports any amount of lookahead. */
        private Stack mLookahead = new Stack();

        private boolean mScanKey = true;
        private Token mEOFToken;

        private Vector mListeners = new Vector(1);
        private int mErrorCount = 0;

        public Scanner(Reader in) {
            mSource = new SourceReader(in, null, null);
        }
        
        public void addErrorListener(ErrorListener listener) {
            mListeners.addElement(listener);
        }
        
        public void removeErrorListener(ErrorListener listener) {
            mListeners.removeElement(listener);
        }
        
        private void dispatchParseError(ErrorEvent e) {
            mErrorCount++;
            
            synchronized (mListeners) {
                for (int i = 0; i < mListeners.size(); i++) {
                    ((ErrorListener)mListeners.elementAt(i)).parseError(e);
                }
            }
        }
        
        private void error(String str, SourceInfo info) {
            dispatchParseError(new ErrorEvent(this, str, info));
        }
        
        private void error(String str) {
            error(str, new SourceInfo(mSource.getLineNumber(),
                                      mSource.getStartPosition(),
                                      mSource.getEndPosition()));
        }

        /**
         * Returns EOF as the last token.
         */
        public synchronized Token readToken() throws IOException {
            if (mLookahead.empty()) {
                return scanToken();
            }
            else {
                return (Token)mLookahead.pop();
            }
        }
        
        /** 
         * Returns EOF as the last token.
         */
        public synchronized Token peekToken() throws IOException {
            if (mLookahead.empty()) {
                return (Token)mLookahead.push(scanToken());
            }
            else {
                return (Token)mLookahead.peek();
            }
        }
        
        public synchronized void unreadToken(Token token) throws IOException {
            mLookahead.push(token);
        }
        
        public void close() throws IOException {
            mSource.close();
        }

        public int getErrorCount() {
            return mErrorCount;
        }
        
        private Token scanToken() throws IOException {
            if (mSource.isClosed()) {
                if (mEOFToken == null) {
                    mEOFToken = makeToken(Token.EOF, null);
                }
                
                return mEOFToken;
            }
            
            int c;
            
            while ( (c = mSource.read()) != -1 ) {
                switch (c) {

                case SourceReader.ENTER_CODE:
                case SourceReader.ENTER_TEXT:
                    continue;
                    
                case '#':
                case '!':
                    mScanKey = true;
                    return scanComment();

                case '{':
                    mScanKey = true;
                    return makeToken(Token.LBRACE, "{");
                case '}':
                    mScanKey = true;
                    return makeToken(Token.RBRACE, "}");
                
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z': case '.':
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z': case '_':
                    mSource.unread();
                    return scanKeyOrValue();

                case '\n':
                    mScanKey = true;
                    // fall through
                case ' ': 
                case '\0':
                case '\t':
                    continue;

                default:
                    if (Character.isWhitespace((char)c)) {
                        continue;
                    }
                    else {
                        mSource.unread();
                        return scanKeyOrValue();
                    }
                }
            }
            
            if (mEOFToken == null) {
                mEOFToken = makeToken(Token.EOF, null);
            }
            
            return mEOFToken;
        }
    
        private Token scanKeyOrValue() throws IOException { 
            StringBuffer buf = new StringBuffer(40);
            boolean trim = true;

            int startLine = mSource.getLineNumber();
            int startPos = mSource.getStartPosition();
            int endPos = mSource.getEndPosition();

            boolean skipWhitespace = true;
            boolean skipSeparator = true;

            int c;
        loop:
            while ( (c = mSource.read()) != -1 ) {
                switch (c) {

                case '\n':
                    mSource.unread();
                    break loop;
                
                case '\\':
                    int next = mSource.read();
                    if (next == -1 || next == '\n') {
                        // line continuation
                        skipWhitespace = true;
                        continue;
                    }

                    c = processEscape(c, next);
                    skipWhitespace = false;
                    break;

                case '{':
                case '}':
                    mSource.unread();
                    break loop;
                
                case '=':
                case ':':
                    if (mScanKey) {
                        mSource.unread();
                        break loop;
                    }
                    else if (skipSeparator) {
                        skipSeparator = false;
                        continue;
                    }
                    skipWhitespace = false;
                    break;

                case '\'':
                case '"':
                    if (buf.length() == 0) {
                        scanStringLiteral(c, buf);
                        endPos = mSource.getEndPosition();
                        trim = false;
                        break loop;
                    }
                    // fall through
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z': case '.':
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z': case '_':
                    skipWhitespace = false;
                    break;

                case ' ': 
                case '\0':
                case '\t':
                    if (skipWhitespace) {
                        continue;
                    }
                    if (mScanKey) {
                        break loop;
                    }
                    break;

                default:
                    if (Character.isWhitespace((char)c)) {
                        if (skipWhitespace) {
                            continue;
                        }
                        if (mScanKey) {
                            break loop;
                        }
                    }
                    else {
                        skipWhitespace = false;
                    }
                    break;
                }

                buf.append((char)c);
                endPos = mSource.getEndPosition();
                skipSeparator = false;
            }

            int tokenId;
            if (mScanKey) {
                tokenId = Token.KEY;
                mScanKey = false;
            }
            else {
                tokenId = Token.VALUE;
                mScanKey = true;
            }

            String value = buf.toString();

            if (trim) {
                value = value.trim();
            }

            return new Token(startLine, startPos, endPos, tokenId, value);
        }
        
        private Token scanComment() throws IOException {
            StringBuffer buf = new StringBuffer(40);

            int startLine = mSource.getLineNumber();
            int startPos = mSource.getStartPosition();
            int endPos = mSource.getEndPosition();

            int c;
            while ( (c = mSource.peek()) != -1 ) {
                if (c == '\n') {
                    break;
                }
                
                mSource.read();
                buf.append((char)c);
                
                endPos = mSource.getEndPosition();
            }

            return new Token(startLine, startPos, endPos,
                             Token.COMMENT, buf.toString());
        }

        private void scanStringLiteral(int quote, StringBuffer buf)
            throws IOException {

            int c;
            while ( (c = mSource.read()) != -1 ) {
                if (c == quote) {
                    return;
                }

                if (c == '\\') {
                    int next = mSource.read();
                    if (next == -1 || next == '\n') {
                        // line continuation
                        continue;
                    }
                    c = processEscape(c, next);
                }

                buf.append((char)c);
            }
        }

        private int processEscape(int c, int next) {
            switch (next) {
            case '0':
                return '\0';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'f':
                return '\f';
            case 'r':
                return '\r';

            case '\\':
            case '\'':
            case '\"':
            case '=':
            case ':':
            case '{':
            case '}':
                return next;

            default:
                error("Invalid escape code: \\" + (char)next);
                return next;
            }
        }
                
        private Token makeToken(int Id, String value) {
            return new Token(mSource.getLineNumber(), 
                             mSource.getStartPosition(),
                             mSource.getEndPosition(),
                             Id, value);
        }
    }
}
