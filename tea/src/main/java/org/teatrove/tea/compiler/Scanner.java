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

package org.teatrove.tea.compiler;

import java.io.*;
import java.util.Vector;
import java.util.Stack;
import org.teatrove.trove.io.SourceReader;

/**
 * A Scanner breaks up a source file into its basic elements, called
 * {@link Token Tokens}. Add an {@link CompileListener} to capture any syntax
 * errors detected by the Scanner.
 *
 * @author Brian S O'Neill
 */
public class Scanner {
    private SourceReader mSource;
    private CompilationUnit mUnit;

    private boolean mEmitSpecial;

    /** StringBuffer for temporary use. */
    private StringBuilder mWord = new StringBuilder(20);

    /** The scanner supports any amount of lookahead. */
    private Stack<Token> mLookahead = new Stack<Token>();
    
    private Token mEOFToken;

    private Vector<CompileListener> mListeners = new Vector<CompileListener>(1);
    private int mErrorCount = 0;

    private MessageFormatter mFormatter;

    public Scanner(SourceReader in) {
        this(in, null);
    }

    public Scanner(SourceReader in, CompilationUnit unit) {
        mSource = in;
        mUnit = unit;
        mFormatter = MessageFormatter.lookup(this);
    }

    public void addCompileListener(CompileListener listener) {
        mListeners.addElement(listener);
    }

    public void removeCompileListener(CompileListener listener) {
        mListeners.removeElement(listener);
    }

    private void dispatchParseError(CompileEvent e) {
        mErrorCount++;

        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                mListeners.elementAt(i).compileError(e);
            }
        }
    }

    private void error(String str, SourceInfo info) {
        dispatchParseError
            (new CompileEvent(this, CompileEvent.Type.ERROR,
                              mFormatter.format(str), info, mUnit));
    }

    private void error(String str) {
        error(str, new SourceInfo(mSource.getLineNumber(),
                                  mSource.getStartPosition(),
                                  mSource.getEndPosition()));
    }

    /**
     * Passing true causes Scanner to emit additional tokens that should not
     * be bassed into a Parser. These are {@link Token.COMMENT},
     * {@link Token.ENTER_CODE}, and {@link Token.ENTER_TEXT}. By default,
     * these special tokens are not emitted.
     */
    public void emitSpecialTokens(boolean enable) {
        mEmitSpecial = enable;
    }

    /**
     * Returns EOF as the last token.
     */
    public synchronized Token readToken() throws IOException {
        if (mLookahead.empty()) {
            return scanToken();
        }
        else {
            return mLookahead.pop();
        }
    }

    /** 
     * Returns EOF as the last token.
     */
    public synchronized Token peekToken() throws IOException {
        if (mLookahead.empty()) {
            return mLookahead.push(scanToken());
        }
        else {
            return mLookahead.peek();
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
        int c;
        int peek;
        
        int startPos;

        while ((c = mSource.read()) != -1) {
            switch (c) {

            case SourceReader.ENTER_TEXT:
                Token enter;
                if (mEmitSpecial) {
                    enter = makeStringToken(Token.ENTER_TEXT,
                                            mSource.getEndTag());
                }
                else {
                    enter = null;
                }

                Token t = scanText(c);

                if (mEmitSpecial) {
                    if (t.getStringValue().length() > 0) {
                        mLookahead.push(t);
                    }
                    return enter;
                }

                if (t.getStringValue().length() == 0) {
                    continue;
                }

                return t;

            case SourceReader.ENTER_CODE:
                // Entering code while in code is illegal. Just let the parser
                // deal with it.
                return makeStringToken(Token.ENTER_CODE,
                                       mSource.getBeginTag());

            case '(':
                return makeToken(Token.LPAREN);
            case ')':
                return makeToken(Token.RPAREN);

            case '{':
                return makeToken(Token.LBRACE);
            case '}':
                return makeToken(Token.RBRACE);

            case '[':
                return makeToken(Token.LBRACK);
            case ']':
                return makeToken(Token.RBRACK);
                
            case ';':
                return makeToken(Token.SEMI);
                
            case ',':
                return makeToken(Token.COMMA);
                
            case ':':
                return makeToken(Token.COLON);
                
            case '?':
                return makeToken(Token.QUESTION);

            case '.':
                peek = mSource.peek();

                if (peek >= '0' && peek <= '9') {
                    error("number.decimal.start");
                    return scanNumber(c);
                }
                else if (peek == '.') {
                    startPos = mSource.getStartPosition();
                    // read the second '.'
                    mSource.read();

                    peek = mSource.peek();
                    if (peek == '.') {
                        // read the third '.'
                        mSource.read();
                        return makeToken(Token.ELLIPSIS, startPos);
                    }
                    else {
                        return makeToken(Token.DOTDOT, startPos);
                    }
                }
                else {
                    return makeToken(Token.DOT);
                }
                
            case '#':
                peek = mSource.peek();
                
                if (peek == '#') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.DOUBLE_HASH, startPos);
                }
                else {
                    return makeToken(Token.HASH);
                }
                
            case '!':
                if (mSource.peek() == '=') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.NE, startPos);
                }
                else {
                    return makeStringToken(Token.UNKNOWN,
                                           String.valueOf((char)c));
                }
                
            case '<':
                if (mSource.peek() == '=') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    if (mSource.peek() == '>') {
                        mSource.read();
                        return makeToken(Token.SPACESHIP, startPos);
                    }
                    else {
                        return makeToken(Token.LE, startPos);
                    }
                }
                else {
                    return makeToken(Token.LT);
                }
                
            case '>':
                if (mSource.peek() == '=') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.GE, startPos);
                }
                else {
                    return makeToken(Token.GT);
                }

            case '=':
                if (mSource.peek() == '=') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.EQ, startPos);
                }
                if (mSource.peek() == '>') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.EQUAL_GREATER, startPos);
                }
                else {
                    return makeToken(Token.ASSIGN);
                }

            case '&':
                return makeToken(Token.CONCAT);

            case '+':
                return makeToken(Token.PLUS);
                
            case '-':
                return makeToken(Token.MINUS);
                
            case '*':
                if (mSource.peek() == '.') {
                    startPos = mSource.getStartPosition();
                    mSource.read();
                    return makeToken(Token.SPREAD, startPos);
                }
                else {
                    return makeToken(Token.MULT);
                }
                
            case '%':
                return makeToken(Token.MOD);
                
            case '/':
                startPos = mSource.getStartPosition();
                peek = mSource.peek();

                if (peek == '*') {
                    mSource.read();
                    mSource.ignoreTags(true);
                    t = scanMultiLineComment(startPos);
                    mSource.ignoreTags(false);
                    if (mEmitSpecial) {
                        return t;
                    }
                    else {
                        continue;
                    }
                }
                else if (peek == '/') {
                    mSource.read();
                    t = scanOneLineComment(startPos);
                    if (mEmitSpecial) {
                        return t;
                    }
                    else {
                        continue;
                    }
                }
                else {
                    return makeToken(Token.DIV);
                }
                
            case '\"':
            case '\'':
                mSource.ignoreTags(true);
                t = scanString(c);
                mSource.ignoreTags(false);
                return t;
                
            case '0': case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9':
                return scanNumber(c);
                
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z': case '_':
                return scanIdentifier(c);

            case ' ': 
            case '\0':
            case '\t': 
            case '\r': 
            case '\n':
                continue;

            default:
                if (Character.isWhitespace((char)c)) {
                    continue;
                }

                if (Character.isLetter((char)c)) {
                    return scanIdentifier(c);
                }
                else {
                    return makeStringToken(Token.UNKNOWN,
                                           String.valueOf((char)c));
                }
            }
        }

        if (mEOFToken == null) {
            mEOFToken = makeToken(Token.EOF);
        }
        
        return mEOFToken;
    }

    // The ENTER_TEXT code has already been scanned when this is called.
    private Token scanText(int c) throws IOException {
        // Read first character in text so that source info does not include
        // tags.
        c = mSource.read();

        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        int endPos = mSource.getEndPosition();
        StringBuilder buf = new StringBuilder(256);

        while (c != -1) {
            if (c == SourceReader.ENTER_CODE) {
                if (mEmitSpecial) {
                    mLookahead.push(makeStringToken(Token.ENTER_CODE,
                                                    mSource.getBeginTag()));
                }
                break;
            }
            else if (c == SourceReader.ENTER_TEXT) {
                buf.append(mSource.getEndTag());
            }
            else {
                buf.append((char)c);
            }

            if (mSource.peek() < 0) {
                endPos = mSource.getEndPosition();
            }

            c = mSource.read();
        }

        if (c == -1) {
            // If the last token in the source file is text, trim all trailing
            // whitespace from it.

            int length = buf.length();
            
            int i;
            for (i = length - 1; i >= 0; i--) {
                if (buf.charAt(i) > ' ') {
                    break;
                }
            }

            buf.setLength(i + 1);
        }

        String str = buf.toString();
        return new StringToken(startLine, startPos, endPos,
                               Token.STRING, str);
    }
    
    private Token scanString(int delimiter) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        mWord.setLength(0);
        
        while ( (c = mSource.read()) != -1 ) {
            if (c == delimiter) {
                break;
            }

            if (c == '\n' || c == '\r') {
                error("string.newline");
                break;
            }
            
            if (c == '\\') {
                int next = mSource.read();
                switch (next) {
                case '0':
                    c = '\0';
                    break;
                case 'b':
                    c = '\b';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 'f':
                    c = '\f';
                    break;
                case 'r':
                    c = '\r';
                    break;
                case '\\':
                    c = '\\';
                    break;
                case '\'':
                    c = '\'';
                    break;
                case '\"':
                    c = '\"';
                    break;
                default:
                    error("escape.code");
                    c = next;
                    break;
                }
            }
            
            mWord.append((char)c);
        }

        if (c == -1) {
            error("string.eof");
        }

        Token t = new StringToken(startLine,
                                  startPos, 
                                  mSource.getEndPosition(),
                                  Token.STRING,
                                  mWord.toString()); 
        
        return t;
    }

    // The first character has already been scanned when this is called.
    private Token scanNumber(int c) throws IOException {
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        mWord.setLength(0);
        
        int errorPos = -1;

        // 0 is decimal int, 
        // 1 is hex int, 
        // 2 is decimal long, 
        // 3 is hex long, 
        // 4 is float, 
        // 5 is double,
        // 6 is auto-double by decimal
        // 7 is auto-double by exponent ('e' or 'E')
        int type = 0; 

        if (c == '0') {
            if (mSource.peek() == 'x' || mSource.peek() == 'X') {
                type = 1;
                mSource.read(); // absorb the 'x'
                c = mSource.read(); // get the first digit after the 'x'
            }
        }

        for (; c != -1; c = mSource.read()) {
            if (c == '.') {
                int peek = mSource.peek();
                if (peek == '.') {
                    mSource.unread();
                    break;
                }
                else {
                    if (peek < '0' || peek > '9') {
                        error("number.decimal.end");
                    }

                    mWord.append((char)c);

                    if (type == 0) {
                        type = 6;
                    }
                    else if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }

                    continue;
                }
            }

            if (c >= '0' && c <= '9') {
                mWord.append((char)c);

                if (type == 2 || type == 3 || type == 4 || type == 5) {
                    if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }
                }

                continue;
            }

            if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                if (type == 1) {
                    mWord.append((char)c);
                    continue;
                }

                if (c == 'f' || c == 'F') {
                    if (type == 0 || type == 6 || type == 7) {
                        type = 4;
                        continue;
                    }
                }
                else if (c == 'd' || c == 'D') {
                    if (type == 0 || type == 6 || type == 7) {
                        type = 5;
                        continue;
                    }
                }
                else if (c == 'e' || c == 'E') {
                    if (type == 0 || type == 6) {
                        mWord.append((char)c);
                        type = 7;
                        int peek = mSource.peek();
                        if (peek == '+' || peek == '-') {
                            mWord.append((char)mSource.read());
                        }
                        continue;
                    }
                }

                mWord.append((char)c);

                if (errorPos < 0) {
                    errorPos = mSource.getStartPosition();
                }

                continue;
            }

            if (c == 'l' || c == 'L') {
                if (type == 0) {
                    type = 2;
                }
                else if (type == 1) {
                    type = 3;
                }
                else {
                    mWord.append((char)c);
                    if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }
                }

                continue;
            }

            if (Character.isLetterOrDigit((char)c)) {
                mWord.append((char)c);

                if (errorPos < 0) {
                    errorPos = mSource.getStartPosition();
                }
            }
            else {
                mSource.unread();
                break;
            }
        }
        
        String str = mWord.toString();
        int endPos = mSource.getEndPosition();
        Token token;

        if (errorPos >= 0) {
            token = new StringToken
                (startLine, startPos, endPos, errorPos, Token.NUMBER, str);
        }
        else {
            try {
                switch (type) {
                case 0:
                default:
                    try {
                        token = new IntToken
                            (startLine, startPos, endPos,
                             Integer.parseInt(str));
                    }
                    catch (NumberFormatException e) {
                        token = new LongToken
                            (startLine, startPos, endPos, Long.parseLong(str));
                    }
                    break;
                case 1:
                    try {
                        token = new IntToken
                            (startLine, startPos, endPos, parseHexInt(str));
                    }
                    catch (NumberFormatException e) {
                        token = new LongToken
                            (startLine, startPos, endPos, parseHexLong(str));
                    }
                    break;
                case 2:
                    token = new LongToken
                        (startLine, startPos, endPos, Long.parseLong(str));
                    break;
                case 3:
                    token = new LongToken
                        (startLine, startPos, endPos, parseHexLong(str));
                    break;
                case 4:
                    token = new FloatToken
                        (startLine, startPos, endPos, Float.parseFloat(str));
                    break;
                case 5:
                case 6:
                case 7:
                    token = new DoubleToken
                        (startLine, startPos, endPos, Double.parseDouble(str));
                    break;
                }
            }
            catch (NumberFormatException e) {
                token = new IntToken(startLine, startPos, endPos, 0);
                error("number.range", token.getSourceInfo());
            }
        }

        return token;
    }
    
    private int parseHexInt(String str) {
        if (str.length() > 8) {
            // Strip off any leading zeros.
            while (str.charAt(0) == '0') {
                str = str.substring(1);
            }
        }

        try {
            return Integer.parseInt(str, 16);
        }
        catch (NumberFormatException e) {
            if (str.length() == 8) {
                return (int)Long.parseLong(str, 16);
            }
            else {
                throw e;
            }
        }
    }

    private long parseHexLong(String str) {
        if (str.length() > 16) {
            // Strip off any leading zeros.
            while (str.charAt(0) == '0') {
                str = str.substring(1);
            }
        }

        try {
            return Long.parseLong(str, 16);
        }
        catch (NumberFormatException e) {
            if (str.length() == 16) {
                long v1 = Long.parseLong(str.substring(0, 8), 16);
                long v2 = Long.parseLong(str.substring(8), 16);
                return v1 << 32 + v2 & 0xffffffffL;
            }
            else {
                throw e;
            }
        }
    }

    // The first character has already been scanned when this is called.
    private Token scanIdentifier(int c) throws IOException {
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        int endPos = mSource.getEndPosition();
        mWord.setLength(0);
        
        mWord.append((char)c);

    loop:
        while ( (c = mSource.peek()) != -1 ) {
            switch (c) {
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z': case '_': case '$':
            case '0': case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9':
                mSource.read();
                endPos = mSource.getEndPosition();
                mWord.append((char)c);
                continue loop;
            }
                
            if (Character.isLetterOrDigit((char)c)) {
                mSource.read();
                endPos = mSource.getEndPosition();
                mWord.append((char)c);
            }
            else {
                break;
            }
        }
        
        int id = Token.findReservedWordID(mWord);
        
        Token t;

        if (id != Token.UNKNOWN) {
            t = new Token(startLine, startPos, endPos, id);
        }
        else {
            t = new StringToken(startLine, startPos, endPos,
                                Token.IDENT, mWord.toString());
        }

        mWord.setLength(0);
        return t;
    }
    
    // The two leading slashes have already been scanned when this is
    // called.
    private Token scanOneLineComment(int startPos) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        int endPos = mSource.getEndPosition();
        mWord.setLength(0);
        mWord.append('/').append('/');

        while ( (c = mSource.peek()) != -1 ) {
            if (c == '\r' || c == '\n') {
                break;
            }
            
            mSource.read();
            mWord.append((char)c);

            endPos = mSource.getEndPosition();
        }

        return new StringToken(startLine, startPos, endPos,
                               Token.COMMENT, mWord.toString());
    }

    // The leading slash and star has already been scanned when this is
    // called.
    private Token scanMultiLineComment(int startPos) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        mWord.setLength(0);
        mWord.append('/').append('*');

        while ( (c = mSource.read()) != -1 ) {
            mWord.append((char)c);

            if (c == '*') {
                if (mSource.peek() == '/') {
                    mWord.append('/');
                    mSource.read();
                    break;
                }
            }
        }

        if (c == -1) {
            error("comment.eof");
        }

        return new StringToken(startLine, startPos, mSource.getEndPosition(),
                               Token.COMMENT, mWord.toString());
    }

    private Token makeToken(int ID) {
        return new Token(mSource.getLineNumber(), 
                         mSource.getStartPosition(),
                         mSource.getEndPosition(),
                         ID);
    }

    private Token makeToken(int ID, int startPos) {
        return new Token(mSource.getLineNumber(), 
                         startPos,
                         mSource.getEndPosition(),
                         ID);
    }

    private Token makeStringToken(int ID, String str) {
        return new StringToken(mSource.getLineNumber(), 
                               mSource.getStartPosition(),
                               mSource.getEndPosition(),
                               ID,
                               str);
    }

    /** 
     * Simple test program 
     */
    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    /**
     * 
     * @author Brian S O'Neill
     */
    private static class Tester {
        public static void test(String[] arg) throws Exception {
            Token token;
            Reader file;
            Scanner s;

            // First run, display tokens in program format
            file = new BufferedReader(new FileReader(arg[2]));
            s = new Scanner(new SourceReader(file, arg[0], arg[1]));

            while ( (token = s.readToken()).getID() != Token.EOF ) {
                int id = token.getID();
                
                if (id == Token.LBRACE) {
                    System.out.println();
                }
                
                System.out.print(token + " ");
                
                if (id == Token.LBRACE || 
                    id == Token.RBRACE || 
                    id == Token.SEMI) {
                    System.out.println();

                    if (id == Token.RBRACE) {
                        System.out.println();
                    }
                }
            }
            
            System.out.println("\n\n*** Full Token Dump ***\n");

            // Second run, display detailed token information
            file = new FileReader(arg[2]);
            s = new Scanner(new SourceReader(file, arg[0], arg[1]));
            s.emitSpecialTokens(true);

            while ( (token = s.readToken()).getID() != Token.EOF ) {
                System.out.print(token.getCode() + ": ");
                System.out.print(token.getSourceInfo() + ": ");

                if (token.getID() == Token.NUMBER) {
                    switch (token.getNumericType()) {
                    case 1:
                        System.out.print("int: ");
                        break;
                    case 2:
                        System.out.print("long: ");
                        break;
                    case 3:
                        System.out.print("float: ");
                        break;
                    case 4:
                        System.out.print("double: ");
                        break;
                    default:
                        System.out.print("BAD: ");
                        break;
                    }
                }

                String value = token.getStringValue();
                if (value != null) {
                    System.out.println(value);
                }
                else {
                    System.out.println(token.getImage());
                }
            }
        }
    }
}
