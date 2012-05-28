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

import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author Brian S O'Neill
 */
public class StringReplacer {
    private StringReplacer() {
    }

    /**
     * Replaces all exact matches of the given pattern in the source string 
     * with the provided replacement.
     * 
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns.
     *
     * @return the string with any replacements applied.
     */
    public static String replace(String source, String pattern, 
                                 String replacement) {
        return replace(source, pattern, replacement, 0);
    }

    /**
     * Replaces all exact matches of the given pattern in the source string 
     * with the provided replacement, starting from the given index.
     * 
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns.
     * @param fromIndex index to start the replace
     *
     * @return the string with any replacements applied.
     */
    public static String replace(String source, String pattern,
                                 String replacement, int fromIndex) {
        if (source == null) {
            if (pattern == null) {
                return replacement;
            }
            else {
                return source;
            }
        }

        int patternLength;
        if (pattern == null || (patternLength = pattern.length()) == 0) {
            return source;
        }

        int sourceLength = source.length();

        StringBuilder buf;
        if (fromIndex <= 0) {
            fromIndex = 0;
            buf = new StringBuilder(sourceLength);
        }
        else if (fromIndex < sourceLength) {
            buf = new StringBuilder(sourceLength);
            buf.append(source.substring(0, fromIndex));
        }
        else {
            return source;
        }
        
    sourceScan:
        for (int s = fromIndex; s < sourceLength; ) {
            int k = s;
            for (int j=0; j<patternLength; j++, k++) {
                if (k >= sourceLength || 
                    source.charAt(k) != pattern.charAt(j)) {

                    buf.append(source.charAt(s));
                    s++;
                    continue sourceScan;
                }
            }

            buf.append(replacement);
            s = k;
        }

        return buf.toString();
    }

    /**
     * Applies string replacements using the pattern-replacement pairs provided
     * by the given map (associative array). The longest matching pattern is
     * used for selecting an appropriate replacement.
     *
     * @param source the source string
     * @param patternReplacements pattern-replacement pairs
     */
    public static 
    String replace(String source, Map<String, String> patternReplacements) {
        if (source == null) {
            return null;
        }

        int mapSize = patternReplacements.size();
        String[] patterns = new String[mapSize];
        String[] replacements = new String[mapSize];

        Iterator<Map.Entry<String, String>> it = 
            patternReplacements.entrySet().iterator();
        
        for (int i=0; it.hasNext(); i++) {
            Map.Entry<String, String> entry = it.next();
            
            patterns[i] = entry.getKey();
            replacements[i] = entry.getValue();
        }

        return replace(source, patterns, replacements);
    }

    public static String replace(String source, String[] patterns, 
                                 String[] replacements)
    {
        int patternsLength = patterns.length;

        int sourceLength = source.length();
        StringBuilder buf = new StringBuilder(sourceLength);

        for (int s=0; s<sourceLength; ) {
            int longestPattern = 0;
            int closestPattern = -1;

        patternScan:
            for (int i=0; i<patternsLength; i++) {
                String pattern = patterns[i];
                int patternLength = pattern.length();

                if (patternLength > 0) {
                    for (int j=0, k=s; j<patternLength; j++, k++) {
                        if (k >= sourceLength || 
                            source.charAt(k) != pattern.charAt(j)) {
                            
                            continue patternScan;
                        }
                    }

                    if (patternLength > longestPattern) {
                        longestPattern = patternLength;
                        closestPattern = i;
                    }
                }
            }

            if (closestPattern >= 0) {
                buf.append(replacements[closestPattern]);
                s += longestPattern;
            }
            else {
                buf.append(source.charAt(s));
                s++;
            }
        }

        return buf.toString();
    }

    /**
     * Replaces the first exact match of the given pattern in the source
     * string with the provided replacement.
     *
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns
     * 
     * @return the string with any replacements applied
     */
    public static String replaceFirst(String source, String pattern,
                                      String replacement) {
        return replaceOne(source, pattern, replacement,
                          findFirst(source, pattern));
    }

    /**
     * Replaces the first exact match of the given pattern in the source
     * string with the provided replacement, starting from the given index.
     *
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns
     * @param fromIndex index to start the replace
     * 
     * @return the string with any replacements applied
     */
    public static String replaceFirst(String source, String pattern,
                                      String replacement, int fromIndex) {
        return replaceOne(source, pattern, replacement,
                          findFirst(source, pattern, fromIndex));
    }

    /**
     * Replaces the last exact match of the given pattern in the source
     * string with the provided replacement.
     *
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns
     * 
     * @return the string with any replacements applied
     */
    public static String replaceLast(String source, String pattern,
                                     String replacement) {
        return replaceOne(source, pattern, replacement,
                          findLast(source, pattern));
    }

    /**
     * Replaces the last exact match of the given pattern in the source
     * string with the provided replacement, starting from the given index.
     *
     * @param source the source string
     * @param pattern the simple string pattern to search for
     * @param replacement the string to use for replacing matched patterns
     * @param fromIndex index to start the replace
     * 
     * @return the string with any replacements applied
     */
    public static String replaceLast(String source, String pattern,
                                     String replacement, int fromIndex) {
        return replaceOne(source, pattern, replacement,
                          findLast(source, pattern, fromIndex));
    }

    private static String replaceOne(String source, String pattern,
                                     String replacement, int atIndex) {
        if (atIndex < 0) {
            if (source == null && pattern == null) {
                return replacement;
            }
            else {
                return source;
            }
        }

        StringBuilder buf = new StringBuilder
            (source.length() - pattern.length() + replacement.length());
        buf.append(source.substring(0, atIndex));
        buf.append(replacement);
        buf.append(source.substring(atIndex + pattern.length()));

        return buf.toString();
    }

    private static int findFirst(String str, String search) {
        return (str == null || search == null) ? -1 :
            str.indexOf(search);
    }

    private static int findFirst(String str, String search, int fromIndex) {
        return (str == null || search == null) ? -1 :
            str.indexOf(search, fromIndex);
    }

    private static int findLast(String str, String search) {
        return (str == null || search == null) ? -1 :
            str.lastIndexOf(search);
    }

    private static int findLast(String str, String search, int fromIndex) {
        return (str == null || search == null) ? -1 :
            str.lastIndexOf(search, fromIndex);
    }
}
