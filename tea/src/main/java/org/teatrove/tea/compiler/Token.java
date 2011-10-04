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

import java.io.PrintStream;

/**
 * A Token represents the smallest whole element of a source file. Tokens are
 * produced by a {@link Scanner}.
 *
 * @author Brian S O'Neill
 */
public class Token implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** Token ID for an unknown token. */
    public final static int UNKNOWN = 0;
    /** Token ID for the end of file. */
    public final static int EOF = 1;

    // These tokens are not emitted by the Scanner unless explicitly enabled.
    /** Token ID for a single-line or multi-line comment. */
    public final static int COMMENT = 2;
    /** Token ID for the start of a code region. */
    public final static int ENTER_CODE = 3;
    /** Token ID for the start of a text region. */
    public final static int ENTER_TEXT = 4;

    /** Token ID for a string literal. */
    public final static int STRING = 5;
    /** Token ID for a number literal. */
    public final static int NUMBER = 6;
    /** Token ID for an identifier. */
    public final static int IDENT = 7;

    // brackets
    private final static int FIRST_BRACKET_ID = 8;

    /** Token ID for the left parenthesis: '(' */
    public final static int LPAREN = 8;
    /** Token ID for the right parenthesis: ')' */
    public final static int RPAREN = 9;
    /** Token ID for the left brace: '{' */
    public final static int LBRACE = 10;
    /** Token ID for the right brace: '}' */
    public final static int RBRACE = 11;
    /** Token ID for the left bracket: '[' */
    public final static int LBRACK = 12;
    /** Token ID for the right bracket: ']' */
    public final static int RBRACK = 13;

    private final static int LAST_BRACKET_ID = 13;

    // operators
    private final static int FIRST_OPERATOR_ID = 19;

    /** Token ID for the semi-colon: ';' */
    public final static int SEMI = 14;
    /** Token ID for the comma: ',' */
    public final static int COMMA = 15;
    /** Token ID for the dot: '.' */
    public final static int DOT = 16;
    /** Token ID for the double dot: '..' */
    public final static int DOTDOT = 17;
    /** Token ID for the ellipsis: '...' */
    public final static int ELLIPSIS = 18;
    /** Token ID for the forward slash: '/' */
    public final static int SLASH = 19;
    /** Token ID for the hash: '#' */
    public final static int HASH = 20;
    /** Token ID for the double hash: '##' */
    public final static int DOUBLE_HASH = 21;
    /** Token ID for the less than operator: '<' */
    public final static int LT = 22;
    /** Token ID for the less than or equal operator: '<=' */
    public final static int LE = 23;
    /** Token ID for the equal operator: '==' */
    public final static int EQ = 24;
    /** Token ID for the greater than or equal operator: '>=' */
    public final static int GE = 25;
    /** Token ID for the greater than operator: '>' */
    public final static int GT = 26;
    /** Token ID for the not equal operator: '!=' */
    public final static int NE = 27;
    /** Token ID for the concatenation operator: '&' */
    public final static int CONCAT = 28;
    /** Token ID for the plus operator: '+' */
    public final static int PLUS = 29;
    /** Token ID for the minus operator: '-' */
    public final static int MINUS = 30;
    /** Token ID for the multiplication operator: '*' */
    public final static int MULT = 31;
    /** Token ID for the division operator: '/' */
    public final static int DIV = 32;
    /** Token ID for the modulus operator: '%' */
    public final static int MOD = 33;
    /** Token ID for the assignment operator: '=' */
    public final static int ASSIGN = 34;
    /** Token ID for equal greater: '=>' */
    public final static int EQUAL_GREATER = 35;
    /** Token ID for colon: ':' */
    public final static int COLON = 36;
    /** Token ID for question mark: '?' */
    public final static int QUESTION = 37;
    /** Token ID for lambda operator: '->' */
    public final static int LAMBDA = 38;
    /** Token ID for spaceship (comparator) operator: '<=>' */
    public final static int SPACESHIP = 39;
    /** Token ID for spread operator: *. */
    public final static int SPREAD = 40;

    private final static int LAST_OPERATOR_ID = 40;
    // reserved words
    private final static int FIRST_RESERVED_ID = 41;

    // literals

    /** Token ID for the null literal: 'null' */
    public final static int NULL = 41;
    /** Token ID for the true literal: 'true' */
    public final static int TRUE = 42;
    /** Token ID for the false literal: 'false' */
    public final static int FALSE = 43;

    // keywords

    /** Token ID for the not keyword: 'not' */
    public final static int NOT = 44;
    /** Token ID for the or keyword: 'or' */
    public final static int OR = 45;
    /** Token ID for the and keyword: 'and' */
    public final static int AND = 46;
    /** Token ID for the if keyword: 'if' */
    public final static int IF = 47;
    /** Token ID for the else keyword: 'else' */
    public final static int ELSE = 48;
    /** Token ID for the is-a keyword: 'isa' */
    public final static int ISA = 49;
    /** Token ID for the for-each keyword: 'foreach' */
    public final static int FOREACH = 50;
    /** Token ID for the in keyword: 'in' */
    public final static int IN = 51;
    /** Token ID for the reverse keyword: 'reverse' */
    public final static int REVERSE = 52;
    /** Token ID for the template keyword: 'template' */
    public final static int TEMPLATE = 53;
    /** Token ID for the call keyword: 'call' */
    public final static int CALL = 54;
    /** Token ID for the break keyword: 'break' */
    public final static int BREAK = 55;
    /** Token ID for the define keyword: 'define' */
    public final static int DEFINE = 56;
    /** Token ID for the as keyword: 'as' */
    public final static int AS = 57;
    /** Token ID for the import keyword: 'import' */
    public final static int IMPORT = 58;
    /** Token ID for the continue keyword: 'continue' */
    public final static int CONTINUE = 59;
    /** Token ID for the class keyword: 'class' */
    public final static int CLASS = 60;

    private final static int LAST_RESERVED_ID = 60;

    private final static int LAST_ID = 60;

    private int mTokenID;
    private SourceInfo mInfo;

    Token(int sourceLine,
          int sourceStartPos,
          int sourceEndPos,
          int tokenID) {

        this(sourceLine,
             sourceStartPos,
             sourceEndPos,
             sourceStartPos,
             tokenID);
    }

    Token(int sourceLine,
          int sourceStartPos,
          int sourceEndPos,
          int sourceDetailPos,
          int tokenID) {

        mTokenID = tokenID;

        if (tokenID > LAST_ID) {
            throw new IllegalArgumentException("Token ID out of range: " +
                                               tokenID);
        }

        if (sourceStartPos == sourceDetailPos) {
            mInfo = new SourceInfo(sourceLine,
                                   sourceStartPos, sourceEndPos);
        }
        else {
            mInfo = new SourceDetailedInfo(sourceLine,
                                           sourceStartPos, sourceEndPos,
                                           sourceDetailPos);
        }

        if (sourceStartPos > sourceEndPos) {
            // This is an internal error.
            throw new IllegalArgumentException
                ("Token start position greater than end position at line: " +
                 sourceLine);
        }
    }

    public Token(SourceInfo info, int tokenID) {
        mTokenID = tokenID;

        if (tokenID > LAST_ID) {
            throw new IllegalArgumentException("Token ID out of range: " +
                                               tokenID);
        }

        mInfo = info;
    }

    /**
     * Returns true if id is a reserved word
     * @param id The Token id to test
     */
    public final static boolean isReservedWord(int id) {
        return FIRST_RESERVED_ID <= id && id <= LAST_RESERVED_ID;
    }

    /**
     * Returns true if id is an operator
     * @param id The Token id to test
     */
    public final static boolean isOperator(int id) {
        return FIRST_OPERATOR_ID <= id && id <= LAST_OPERATOR_ID;
    }

    /**
     * Returns true if id is a bracket
     * @param id The Token id to test
     */
    public final static boolean isBracket(int id) {
        return FIRST_BRACKET_ID <= id && id <= LAST_BRACKET_ID;
    }

    /**
     * Returns true if id is an open bracket: (,[,{
     * @param id The Token id to test
     */
    public final static boolean isOpenBracket(int id) {
        return (id == LPAREN || id == LBRACE || id == LBRACK);
    }

    /**
     * Returns true if id is a close bracket: ), ], }
     * @param id The Token id to test
     */
    public final static boolean isCloseBracket(int id) {
        return (id == RPAREN || id == RBRACE || id == RBRACK);
    }

    /**
     * If the given id is a bracket: (,[,{,},],) then the matching bracket's
     * id is returned. If id is not a bracket, then -1 is returned.
     */
    public final static int getMatchingBracket(int id) {
        if(isOpenBracket(id)) {
            return ++id;
        }
        else if(isCloseBracket(id)) {
            return --id;
        }

        return -1;
    }

    /**
     * If the given StringBuilder starts with a valid token type, its ID is
     * returned. Otherwise, the token ID UNKNOWN is returned.
     */
    public static int findReservedWordID(StringBuilder word) {
        char c = word.charAt(0);

        switch (c) {
        case 'a':
            if (matches(word, "and")) return AND;
            if (matches(word, "as")) return AS;
            break;
        case 'b':
            if(matches(word, "break")) return BREAK;
            break;
        case 'c':
            if (matches(word, "call")) return CALL;
            if (matches(word, "class___")) return CLASS;
            if (matches(word, "continue")) return CONTINUE;
            break;
        case 'd':
            if (matches(word, "define")) return DEFINE;
            break;
        case 'e':
            if (matches(word, "else")) return ELSE;
            break;
        case 'f':
            if (matches(word, "foreach")) return FOREACH;
            if (matches(word, "false")) return FALSE;
            break;
        case 'i':
            if (matches(word, "if")) return IF;
            if (matches(word, "import")) return IMPORT;
            if (matches(word, "in")) return IN;
            if (matches(word, "isa")) return ISA;
            break;
        case 'n':
            if (matches(word, "null")) return NULL;
            if (matches(word, "not")) return NOT;
            break;
        case 'o':
            if (matches(word, "or")) return OR;
            break;
        case 'r':
            if (matches(word, "reverse")) return REVERSE;
            break;
        case 't':
            if (matches(word, "true")) return TRUE;
            if (matches(word, "template")) return TEMPLATE;
            break;
        }

        return UNKNOWN;
    }

    /**
     * Case sensitive match test.
     * @param val must be lowercase
     */
    private static boolean matches(StringBuilder word, String val) {
        int len = word.length();
        if (len != val.length()) return false;

        // Start at index 1, assuming that the first characters have already
        // been checked to match.
        for (int index = 1; index < len; index++) {
            char cw = word.charAt(index);
            char cv = val.charAt(index);

            if (cw != cv) {
                return false;
            }
        }

        return true;
    }

    /**
     * Dumps the contents of this Token to System.out.
     */
    public final void dump() {
        dump(System.out);
    }

    /**
     * Dumps the contents of this Token.
     * @param out The PrintStream to write to.
     */
    public final void dump(PrintStream out) {
        out.println("Token [Code: " + getCode() + "] [Image: " +
                    getImage() + "] [Value: " + getStringValue() +
                    "] [Id: " + getID() + "] [start: " +
                    mInfo.getStartPosition() + "] [end " +
                    mInfo.getEndPosition() + "]");
    }

    /**
     * Returns the ID of this Token, which identifies what type of token it is.
     */
    public final int getID() {
        return mTokenID;
    }

    /**
     * Returns true if this Token is a reserved word.
     */
    public final boolean isReservedWord() {
        return isReservedWord(mTokenID);
    }

    /**
     * Returns true if this Token is a bracket: (,[,{,},],)
     */
    public final boolean isBracket() {
        return isBracket(mTokenID);
    }

    /**
     * Returns true if this Token is an open bracket: (,[,{
     */
    public final boolean isOpenBracket() {
        return isOpenBracket(mTokenID);
    }

    /**
     * Returns true if this Token is a close bracket: ),],}
     */
    public final boolean isCloseBracket() {
        return isCloseBracket(mTokenID);
    }

    /**
     * Returns true if this Token is an operator
     */
    public final boolean isOperator() {
        return isOperator(mTokenID);
    }

    /**
     * Token image represents what a static token looks like in a source file.
     * Token image is null if token is a string, number or identifier because
     * these tokens don't have static images.
     */
    public String getImage() {
        return Code.TOKEN_IMAGES[mTokenID];
    }

    /**
     * Token code is non-null, and is exactly the same as the name for its ID.
     */
    public String getCode() {
        return Code.TOKEN_CODES[mTokenID];
    }

    /**
     * Returns information regarding where in the source file this token
     * came from.
     */
    public final SourceInfo getSourceInfo() {
        return mInfo;
    }

    public String getStringValue() {
        return null;
    }

    /**
     * Only valid if token is a number. Returns 0 if token is not a number
     * or is an invalid number. Returns 1 for int, 2 for long, 3 for
     * float and 4 for double. The token ID for all numbers (even invalid ones)
     * is NUMBER.
     *
     * @return 0, 1, 2, 3 or 4.
     */
    public int getNumericType() {
        return 0;
    }

    /** Only valid if token is a number. */
    public int getIntValue() {
        return 0;
    }

    /** Only valid if token is a number. */
    public long getLongValue() {
        return 0L;
    }

    /** Only valid if token is a number. */
    public float getFloatValue() {
        return 0.0f;
    }

    /** Only valid if token is a number. */
    public double getDoubleValue() {
        return 0.0d;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(10);

        String image = getImage();

        if (image != null) {
            buf.append(image);
        }

        String str = getStringValue();

        if (str != null) {
            if (image != null) {
                buf.append(' ');
            }
            buf.append(str);
        }

        return buf.toString();
    }

    private static class Code {
        public final static String[] TOKEN_IMAGES =
        {
            null,
            null,

            null,
            null,
            null,

            null,
            null,
            null,

            "(",
            ")",
            "{",
            "}",
            "[",
            "]",

            ";",
            ",",
            ".",
            "..",
            "...",
            "/",
            "#",
            "##",
            "<",
            "<=",
            "==",
            ">=",
            ">",
            "!=",
            "&",
            "+",
            "-",
            "*",
            "/",
            "%",
            "=",
            "=>",
            ":",
            "?",
            "->",
            "<=>",
            "*.",

            "null",
            "true",
            "false",

            "not",
            "or",
            "and",
            "if",
            "else",
            "isa",
            "foreach",
            "in",
            "reverse",
            "template",
            "call",
            "break",
            "define",
            "as",
            "import",
            "continue",
            "class___"
        };

        public static final String[] TOKEN_CODES =
        {
            "UNKNOWN",
            "EOF",

            "COMMENT",
            "ENTER_CODE",
            "ENTER_TEXT",

            "STRING",
            "NUMBER",
            "IDENT",

            "LPAREN",
            "RPAREN",
            "LBRACE",
            "RBRACE",
            "LBRACK",
            "RBRACK",

            "SEMI",
            "COMMA",
            "DOT",
            "DOTDOT",
            "ELLIPSIS",
            "SLASH",
            "HASH",
            "DOUBLE_HASH",
            "LT",
            "LE",
            "EQ",
            "GE",
            "GT",
            "NE",
            "CONCAT",
            "PLUS",
            "MINUS",
            "MULT",
            "DIV",
            "MOD",
            "ASSIGN",
            "EQUAL_GREATER",
            "COLON",
            "QUESTION",
            "LAMBDA",
            "SPACESHIP",
            "SPREAD",

            "NULL",
            "TRUE",
            "FALSE",

            "NOT",
            "OR",
            "AND",
            "IF",
            "ELSE",
            "ISA",
            "FOREACH",
            "IN",
            "REVERSE",
            "TEMPLATE",
            "CALL",
            "BREAK",
            "DEFINE",
            "AS",
            "IMPORT",
            "CONTINUE",
            "CLASS"
        };

        static {
            if (TOKEN_IMAGES.length != TOKEN_CODES.length) {
                // Internal error.
                throw new RuntimeException
                    ("TOKEN_IMAGES and TOKEN_CODES have different lengths");
            }
        }
    }
}
