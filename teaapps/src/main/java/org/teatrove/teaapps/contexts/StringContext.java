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
package org.teatrove.teaapps.contexts;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.io.UnsupportedEncodingException;

/**
 * Custom Tea context that provides helper methods when dealing with strings.
 */
public class StringContext {

    /**
     * Capitalize each word within the given value. This assumes the words are
     * separated by whitespace (<code>\n\r\t</code> and spaces).
     * 
     * @param value The value to capitalize from
     * 
     * @return The new string with capitalized words
     */
    public String capWords(String value) {
        StringTokenizer st = new StringTokenizer(value);
        StringBuilder buffer = new StringBuilder();
        int count = st.countTokens();
        for (int i = 0; i < count; i++) {
            StringBuffer tempBuffer = new StringBuffer();
            String word = st.nextElement().toString();
            // first letter
            tempBuffer.append(word.substring(0, 1).toUpperCase());
            // rest of sentence
            tempBuffer.append( word.substring(1, word.length()).toLowerCase());
            buffer.append(tempBuffer.append(" "));
        }
        return buffer.toString().trim();
    }

    /**
     * Split the given string on spaces ensuring the maximum number of 
     * characters in each token.
     * 
     * @param text The value to split
     * @param maxChars The maximum required characters in each split token
     * 
     * @return The array of tokens from splitting the string
     * 
     * @see #splitString(String, int, String[])
     */
    public String[] splitString(String text, int maxChars) {
        String[] splitVals = {" "};
        return splitString(text,maxChars,splitVals);
    }

    /**
     * Split the given string on the given split values ensuring the maximum 
     * number of characters in each token.
     * 
     * @param text The value to split
     * @param maxChars The maximum required characters in each split token
     * @param splitValues The set of values to split on
     * 
     * @return The array of tokens from splitting the string
     */
    public String[] splitString(String text, int maxChars,
                                String[] splitValues) {
        String restOfText = text;
        List<String> list = new ArrayList<String>();
        while (restOfText.length() > maxChars) {
            String thisLine = restOfText.substring(0,maxChars);
            int lineEnd = 0;
            for (int i = 0; i < splitValues.length;i++) {
                int lastIndex = thisLine.lastIndexOf(splitValues[i]);
                if (lastIndex > lineEnd) {
                    lineEnd = lastIndex;
                }
            }
            if (lineEnd == 0) {
                list.add(thisLine);
                restOfText = restOfText.substring(maxChars);
            }
            else {
                list.add(thisLine.substring(0,lineEnd));
                restOfText = restOfText.substring(lineEnd);
            }

        }
        list.add(restOfText);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Replace each occurrence of the given pattern with the given replacement
     * value.
     * 
     * @param str The string to match and replace
     * @param pattern The pattern to match against
     * @param replace The replacement value to replace matches with
     * 
     * @return The resulting string after replacing the matching strings
     */
    public String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuilder result = new StringBuilder();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    /**
     * Tokenize the given string with the given token. This will return an array
     * of strings containing each token delimited by the given value.
     * 
     * @param string The string to tokenize
     * @param token The token delimiter to delimit on
     * 
     * @return The array of tokens based on the delimiter
     * 
     * @see StringTokenizer
     */
    public String[] tokenize(String string, String token) {
        String[] result;
        try {
            StringTokenizer st = new StringTokenizer(string, token);
            List<String> list = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                list.add(st.nextToken());
            }
            result = list.toArray(new String[list.size()]);
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * Add the given string to the string array. This will create a new array
     * expanded by one element, copy the existing array, add the given value,
     * and return the resulting array.
     * 
     * @param strArray The array of strings to add to
     * @param str The string to add
     * 
     * @return A new array containing the string array and the given string
     */
    public String[] addToStringArray(String[] strArray, String str) {
        String[] result = null;
        if (strArray != null) {
            result = new String[strArray.length + 1];
            System.arraycopy(strArray, 0, result, 0, strArray.length);
            result[strArray.length] = str;
        }
        return result;
    }

    /**
     * Create an array of strings of the given size.
     * 
     * @param size The size of the array
     * 
     * @return A new array of strings of the given size
     */
    public String[] createStringArray(int size) {
        return new String[size];
    }

    /**
     * Remove all occurrences of the given string from the given array. This
     * will create a new array containing all elements of the given array
     * except for those matching the given value.
     * 
     * @param strArray The array of strings to remove from
     * @param str The value to remove
     * 
     * @return A new array containing the given array excluding the given string
     */
    public String[] removeFromStringArray(String[] strArray, String str) {
        String[] result = null;
        if (strArray != null && strArray.length > 0) {
            List<String> list = new ArrayList<String>();
            for (int i=0; i < strArray.length; i++) {
                if (!strArray[i].equals(str)) {
                    list.add(strArray[i]);
                }
            }
            int size = list.size();
            if (size > 0) {
                result = list.toArray(new String[size]);
            }
        }
        return result;
    }

    /**
     * Remove the string at the given index from the given array. This will
     * return a new array containing the given array excluding the element at
     * the given index.
     * 
     * @param strArray The string array to remove from
     * @param index The index of the element to remove
     * 
     * @return A new array containing the given array excluding the given index
     */
    public String[] removeFromStringArray(String[] strArray, int index) {
        String[] result = null;
        if (strArray != null && index > 0 && index < strArray.length) {
            result = new String[strArray.length - 1];
            for (int i=0, j=0; i < strArray.length; i++) {
                if (i != index) {
                    result[j++] = strArray[i];
                }
            }
        }
        return result;
    }

    /**
     * Set the given string in the given array at the given index. This will
     * overwrite the existing value at the given index.
     * 
     * @param strArray The array to set in
     * @param index The index of the element to overwrite
     * @param str The string value to set
     * 
     * @return <code>true</code> if the index was valid and the value was set,
     *         <code>false</code> otherwise
     */
    public boolean setInStringArray(String[] strArray, int index, String str) {
        boolean result = false;
        if (strArray != null && index >= 0 && index < strArray.length) {
            strArray[index] = str;
            result = true;
        }
        return result;
    }

    /**************************************************************
     * These functions take String types instead of char types,
     * because Tea automatically converts chars to Strings, so this
     * is the only way to call these character functions from Tea.
     **************************************************************/
    
    /**
     * Convert the given character to an integer digit based on the given
     * radix base (ie: 10 for decimal, 16 for hexidecimal, 8 for octal, etc).
     * Note that only the first character of the string is used.
     * 
     * @param str The character to convert
     * @param radix The base to convert from
     * 
     * @return The int value of the character
     * 
     * @see Character#digit(char, int)
     */
    public int digit(String str, int radix) {
        return Character.digit(str.charAt(0), radix);
    }

    /**
     * Check if the given character is a digit or not. Note that only the first 
     * character of the string is checked.
     * 
     * @param str The character to test
     * 
     * @return <code>true</code> if the character is a digit,
     *         <code>false</code> otherwise
     * 
     * @see Character#isDigit(char)
     */
    public boolean isDigit(String str) {
        return Character.isDigit(str.charAt(0));
    }

    /**
     * Check if the given character is a letter or not. Note that only the first 
     * character of the string is checked.
     * 
     * @param str The character to test
     * 
     * @return <code>true</code> if the character is a letter,
     *         <code>false</code> otherwise
     * 
     * @see Character#isLetter(char)
     */
    public boolean isLetter(String str) {
        return Character.isLetter(str.charAt(0));
    }

    /**
     * Check if the given character is a letter or digit or not. Note that only 
     * the first character of the string is checked.
     * 
     * @param str The character to test
     * 
     * @return <code>true</code> if the character is a letter or digit,
     *         <code>false</code> otherwise
     * 
     * @see Character#isLetterOrDigit(char)
     */
    public boolean isLetterOrDigit(String str) {
        return Character.isLetterOrDigit(str.charAt(0));
    }
    
    /**
     * Get the bytes of the given string based on the given character set named.
     * 
     * @param str The value to convert
     * @param charsetName The name of the character set
     * 
     * @return The bytes representing the given string
     * 
     * @throws UnsupportedEncodingException if the character set is invalid
     * 
     * @see String#getBytes(String)
     */
    public byte[] getBytes(String str, String charsetName) 
    	throws UnsupportedEncodingException {
        
        return str.getBytes(charsetName);
    }
    
    /**
     * Convert the given string to the given encoding.
     * 
     * @param subject The value to convert
     * @param encoding The name of the encoding/character set
     * 
     * @return A new string in the given encoding
     * 
     * @throws UnsupportedEncodingException if the encoding is invalid
     */
    public String convertToEncoding(String subject, String encoding) 
        throws UnsupportedEncodingException {
        
        return new String(subject.getBytes(encoding), encoding);
    }

    /**
     * Replace the given regular expression pattern with the given replacement.
     * Only the first match will be replaced.
     * 
     * @param subject The value to replace
     * @param regex The regular expression pattern to match
     * @param replacement The replacement value
     * 
     * @return A new string based on the replacement of the expression
     * 
     * @see String#replaceFirst(String, String)
     */
    public String replaceFirstRegex(String subject, 
                                    String regex, String replacement) {
        return subject.replaceFirst(regex, replacement);
    }

    /**
     * Replace the given regular expression pattern with the given replacement.
     * This will replace all matches in the given string.
     * 
     * @param subject The value to replace
     * @param regex The regular expression pattern to match
     * @param replacement The replacement value
     * 
     * @return A new string based on the replacement of the expression
     * 
     * @see String#replaceAll(String, String)
     */
    public String replaceAllRegex(String subject, String regex, String replacement) {
        return subject.replaceAll(regex, replacement);
    }
}
