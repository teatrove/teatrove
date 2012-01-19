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

public class StringContext {

    public String capWords(String value) {
        StringTokenizer st = new StringTokenizer(value);
        StringBuffer buffer = new StringBuffer();
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

    public String[] splitString(String text,int maxChars) {
        String[] splitVals = {" "};
        return splitString(text,maxChars,splitVals);
    }

    public String[] splitString(String text,int maxChars,String[] splitValues) {
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
        return (String[]) list.toArray(new String[list.size()]);
    }

    public String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    public String[] tokenize(String string, String token) {
        String[] result;
        try {
            StringTokenizer st = new StringTokenizer(string, token);
            List<String> list = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                list.add(st.nextToken());
            }
            result = (String[]) list.toArray(new String[list.size()]);
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public String[] addToStringArray(String[] strArray, String str) {
        String[] result = null;
        if (strArray != null) {
            result = new String[strArray.length + 1];
            System.arraycopy(strArray, 0, result, 0, strArray.length);
            result[strArray.length] = str;
        }
        return result;
    }

    public String[] createStringArray(int size) {
        return new String[size];
    }

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
                result = (String[]) list.toArray(new String[size]);
            }
        }
        return result;
    }

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
    public int digit(String str, int radix) {
        return Character.digit(str.charAt(0), radix);
    }

    public boolean isDigit(String str) {
        return Character.isDigit(str.charAt(0));
    }

    public boolean isLetter(String str) {
        return Character.isLetter(str.charAt(0));
    }

    public boolean isLetterOrDigit(String str) {
        return Character.isLetterOrDigit(str.charAt(0));
    }
    
    public byte[] getBytes(String str, String charsetName) 
    	throws UnsupportedEncodingException
    {
        return str.getBytes(charsetName);
    }
    
    public String convertToEncoding(String subject, String encoding) throws UnsupportedEncodingException {
        return new String(subject.getBytes(encoding), encoding);
    }

    public String replaceFirstRegex(String subject, String regex, String replacement) {
        return subject.replaceFirst(regex, replacement);
    }

    public String replaceAllRegex(String subject, String regex, String replacement) {
        return subject.replaceAll(regex, replacement);
    }
}
