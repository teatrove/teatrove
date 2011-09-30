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

package org.teatrove.tea.runtime;

import org.teatrove.trove.util.BeanComparator;
import org.teatrove.trove.util.DecimalFormat;
import org.teatrove.trove.util.Pair;
import org.teatrove.trove.util.StringReplacer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * The default runtime context class that Tea templates get compiled to use.
 * All functions callable from a template are defined in the context. To add
 * more or override existing ones, do so when extending this class.
 *
 * @author Brian S O'Neill
 */
public abstract class DefaultContext extends Writer
    implements UtilityContext
{
    private static final String DEFAULT_NULL_FORMAT = "null";

    // Although the Integer.toString method keeps getting more optimized
    // with each release, it still isn't very fast at converting small values.
    private static final String[] INT_VALUES = {
         "0",  "1",  "2",  "3",  "4",  "5",  "6",  "7",  "8",  "9", 
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", 
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", 
        "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", 
        "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", 
        "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", 
        "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", 
        "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", 
        "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", 
        "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", 
    };

    private static final int FIRST_INT_VALUE = 0;
    private static final int LAST_INT_VALUE = 99;

    private static Map cLocaleCache;
    private static Map cDecimalFormatCache;

    static {
        cLocaleCache = Collections.synchronizedMap(new HashMap(7));
        cDecimalFormatCache = Collections.synchronizedMap(new HashMap(47));
    }

    private Locale mLocale;
    private String mNullFormat = DEFAULT_NULL_FORMAT;
    private DecimalFormat mDecimalFormat;

    // Fields used with date formatting.
    private DateTimeFormatter mDateTimeFormatter;
    private DateTimeZone mDateTimeZone;
    private String mDateTimePattern;

    public DefaultContext() {
    }

    /**
     * @hidden
     */
    public void write(int c) throws IOException {
        try {
            print(String.valueOf((char)c));
        }
        catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new IOException(e.toString());
        }
    }

    /**
     * @hidden
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
        try {
            if (cbuf == null) {
                print(mNullFormat);
            }
            else {
                print(new String(cbuf, off, len));
            }
        }
        catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new IOException(e.toString());
        }
    }

    /**
     * @hidden
     */
    public void flush() throws IOException {
    }

    /**
     * @hidden
     */
    public void close() throws IOException {
    }

    /**
     * Method that is the runtime receiver. Implementations should call one
     * of the toString methods when converting this object to a string.
     * <p>
     * NOTE:  This method should <b>not</b> be called directly within a 
     * template.
     *
     * @see org.teatrove.tea.compiler.Compiler#getRuntimeReceiver
     * @hidden
     */
    public abstract void print(Object obj) throws Exception;

    /**
     * @hidden
     */
    public void print(Date date) throws Exception {
        if (date == null) {
            write(mNullFormat);
        }
        else {
            if (mDateTimeFormatter == null) {
                // Force formatter and time zone to be set.
                dateFormat(null);
            }
            // DateTimeZone already set when formatter was created :-)
            mDateTimeFormatter.printTo(this, date.getTime());
        }
    }

    /**
     * @hidden
     */
    public void print(ReadableInstant instant) throws Exception {
        if (instant == null) {
            write(mNullFormat);
        }
        else {
            if (mDateTimeFormatter == null) {
                // Force formatter and time zone to be set.
                dateFormat(null);
            }
            // DateTimeZone already set when formatter was created :-)
            mDateTimeFormatter.printTo(this, instant.getMillis());
        }
    }

    /**
     * @hidden
     */
    public void print(Number n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(int n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(float n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(long n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public void print(double n) throws Exception {
        print(toString(n));
    }

    /**
     * @hidden
     */
    public String toString(Object obj) {
        if (obj == null) {
            return mNullFormat;
        }
        else if (obj instanceof String) {
            return (String)obj;
        }
        else if (obj instanceof Date) {
            return toString((Date)obj);
        }
        else if (obj instanceof Number) {
            return toString((Number)obj);
        }
        else if (obj instanceof ReadableInstant) {
            return toString((ReadableInstant)obj);
        }
        else {
            String str = obj.toString();
            return (str == null) ? mNullFormat : str;
        }
    }

    /**
     * @hidden
     */
    public String toString(String str) {
        return (str == null) ? mNullFormat : str;
    }

    /**
     * @hidden
     */
    public String toString(Date date) {
        if (date == null) {
            return mNullFormat;
        }

        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }

        // DateTimeZone already set when formatter was created :-)
        //return mDateTimeFormatter.print(date.getTime(), mDateTimeZone);
        return mDateTimeFormatter.print(date.getTime());
    }

    /**
     * @hidden
     */
    public String toString(ReadableInstant instant) {
        if (instant == null) {
            return mNullFormat;
        }

        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }

        // DateTimeZone already set when formatter was created :-)
        return mDateTimeFormatter.print(instant.getMillis());
    }

    /**
     * @hidden
     */
    public String toString(Number n) {
        if (n == null) {
            return mNullFormat;
        }
        else if (mDecimalFormat == null) {
            if (n instanceof Integer) {
                return toString(((Integer)n).intValue());
            }
            else if (n instanceof Long) {
                return toString(((Long)n).longValue());
            }
            else {
                return n.toString();
            }
        }
        else {
            if (n instanceof Integer) {
                return mDecimalFormat.format(n.intValue());
            }
            if (n instanceof Double) {
                return mDecimalFormat.format(n.doubleValue());
            }
            if (n instanceof Float) {
                return mDecimalFormat.format(n.floatValue());
            }
            if (n instanceof Long) {
                return mDecimalFormat.format(n.longValue());
            }
            return mDecimalFormat.format(n.doubleValue());
        }
    }

    /**
     * @hidden
     */
    public String toString(int n) {
        if (mDecimalFormat == null) {
            if (n <= LAST_INT_VALUE && n >= FIRST_INT_VALUE) {
                return INT_VALUES[n];
            }
            else {
                return Integer.toString(n);
            }
        }
        else {
            return mDecimalFormat.format(n);
        }
    }

    /**
     * @hidden
     */
    public String toString(float n) {
        return (mDecimalFormat == null) ? Float.toString(n) :
            mDecimalFormat.format(n);
    }

    /**
     * @hidden
     */
    public String toString(long n) {
        if (mDecimalFormat == null) {
            if (n <= LAST_INT_VALUE && n >= FIRST_INT_VALUE) {
                return INT_VALUES[(int)n];
            }
            else {
                return Long.toString(n);
            }
        }
        else {
            return mDecimalFormat.format(n);
        }
    }

    /**
     * @hidden
     */
    public String toString(double n) {
        return (mDecimalFormat == null) ? Double.toString(n) :
            mDecimalFormat.format(n);
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            mLocale = null;
            mDateTimeFormatter = null;
            mDecimalFormat = null;
        }
        else {
            synchronized (cLocaleCache) {
                Locale cached = (Locale)cLocaleCache.get(locale);
                if (cached == null) {
                    cLocaleCache.put(locale, locale);
                }
                else {
                    locale = cached;
                }
            }
            
            mLocale = locale;
            dateFormat(null);
            numberFormat(null);
        }
    }

    public void setLocale(String language, String country) {
        setLocale(new Locale(language, country));
    }

    public void setLocale(String language, String country, String variant) {
        setLocale(new Locale(language, country, variant));
    }

    public java.util.Locale getLocale() {
        return mLocale;
    }

    public Locale[] getAvailableLocales() {
        return Locale.getAvailableLocales();
    }

    public void nullFormat(String format) {
        mNullFormat = (format == null) ? DEFAULT_NULL_FORMAT : format;
    }

    public String getNullFormat() {
        return mNullFormat;
    }

    public void dateFormat(String format) {
        dateFormat(format, null);
    }

    public void dateFormat(String format, String timeZoneID) {
        DateTimeZone zone;
        if (timeZoneID != null) {
            zone = DateTimeZone.forID(timeZoneID);
        }
        else {
            zone = DateTimeZone.getDefault();
        }
        mDateTimeZone = zone;

        /* --Original before joda upgrade
        DateTimeFormat dtFormat;
        if (mLocale == null) {
            dtFormat = DateTimeFormat.getInstance(zone); --orig
        }
        else {
            dtFormat = DateTimeFormat.getInstance(zone, mLocale);
        }
    
        if (format == null) {
            format = dtFormat.getPatternForStyle("LL"); --orig
        }*/

        if (format == null) {
            format = DateTimeFormat.patternForStyle("LL", mLocale);
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format).withZone(zone);
        if (mLocale != null) {
            formatter = formatter.withLocale(mLocale);
        }

        mDateTimeFormatter = formatter;
        mDateTimePattern = format;
    }

    public String getDateFormat() {
        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }
        return mDateTimePattern;
    }

    public String getDateFormatTimeZone() {
        if (mDateTimeFormatter == null) {
            // Force formatter and time zone to be set.
            dateFormat(null);
        }
        DateTimeZone zone = mDateTimeZone;
        return zone == null ? null : zone.getID();
    }

    public TimeZone[] getAvailableTimeZones() {
        String[] IDs = TimeZone.getAvailableIDs();
        TimeZone[] zones = new TimeZone[IDs.length];
        for (int i=zones.length; --i >= 0; ) {
            zones[i] = TimeZone.getTimeZone(IDs[i]);
        }
        return zones;
    }

    public void numberFormat(String format) {
        numberFormat(format, null, null);
    }

    public void numberFormat(String format, String infinity, String NaN) {
        if (format == null && infinity == null && NaN == null) {
            if (mLocale == null) {
                mDecimalFormat = null;
            }
            else {
                mDecimalFormat = DecimalFormat.getInstance(mLocale);
            }
            return;
        }

        Object key = format;
        if (mLocale != null) {
            key = new Pair(key, mLocale);
        }

        if (infinity != null || NaN != null) {
            key = new Pair(key, infinity);
            key = new Pair(key, NaN);
        }
        
        if ((mDecimalFormat = 
             (DecimalFormat)cDecimalFormatCache.get(key)) == null) {

            mDecimalFormat = DecimalFormat.getInstance(format, mLocale);
            
            if (infinity != null) {
                mDecimalFormat = mDecimalFormat.setInfinity(infinity);
            }
            if (NaN != null) {
                mDecimalFormat = mDecimalFormat.setNaN(NaN);
            }

            cDecimalFormatCache.put(key, mDecimalFormat);
        }
    }

    public String getNumberFormat() {
        return mDecimalFormat == null ? null : mDecimalFormat.getPattern();
    }

    public String getNumberFormatInfinity() {
        return mDecimalFormat == null ? null : mDecimalFormat.getInfinity();
    }

    public String getNumberFormatNaN() {
        return mDecimalFormat == null ? null : mDecimalFormat.getNaN();
    }

    public Date currentDate() {
        return new Date();
    }

    public DateTime currentDateTime() {
        /*
        DateTimeZone zone = mDateTimeZone;
        if (zone == null) {
            return new DateTime();
        }
        else {
            return new DateTime(zone);
        }
        */
        // TODO
        return new DateTime();
    }

    public boolean startsWith(String str, String prefix) {
        return (str == null || prefix == null) ? (str == prefix) :
            str.startsWith(prefix);
    }

    public boolean endsWith(String str, String suffix) {
        return (str == null || suffix == null) ? (str == suffix) :
            str.endsWith(suffix);
    }

    public int[] find(String str, String search) {
        return find(str, search, 0);
    }

    public int[] find(String str, String search, int fromIndex) {
        if (str == null || search == null) {
            return new int[0];
        }

        int[] indices = new int[10];
        int size = 0;

        int index = fromIndex;
        while ((index = str.indexOf(search, index)) >= 0) {
            if (size >= indices.length) {
                // Expand capacity.
                int[] newArray = new int[indices.length * 2];
                System.arraycopy(indices, 0, newArray, 0, indices.length);
                indices = newArray;
            }
            indices[size++] = index;
            index += search.length();
        }

        if (size < indices.length) {
            // Trim capacity.
            int[] newArray = new int[size];
            System.arraycopy(indices, 0, newArray, 0, size);
            indices = newArray;
        }

        return indices;
    }

    public int findFirst(String str, String search) {
        return (str == null || search == null) ? -1 :
            str.indexOf(search);
    }

    public int findFirst(String str, String search, int fromIndex) {
        return (str == null || search == null) ? -1 :
            str.indexOf(search, fromIndex);
    }

    public int findLast(String str, String search) {
        return (str == null || search == null) ? -1 :
            str.lastIndexOf(search);
    }

    public int findLast(String str, String search, int fromIndex) {
        return (str == null || search == null) ? -1 :
            str.lastIndexOf(search, fromIndex);
    }

    public String substring(String str, int startIndex) {
        return (str == null) ? null : str.substring(startIndex);
    }

    public String substring(String str, int startIndex, int endIndex) {
        return (str == null) ? null : str.substring(startIndex, endIndex);
    }

    public String toLowerCase(String str) {
        return (str == null) ? null : str.toLowerCase();
    }

    public String toUpperCase(String str) {
        return (str == null) ? null : str.toUpperCase();
    }

    public String trim(String str) {
        return (str == null) ? null : str.trim();
    }

    public String trimLeading(String str) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        for (int i=0; i<length; i++) {
            if (str.charAt(i) > ' ') {
                return str.substring(i);
            }
        }

        return "";
    }

    public String trimTrailing(String str) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        for (int i=length-1; i>=0; i--) {
            if (str.charAt(i) > ' ') {
                return str.substring(0, i + 1);
            }
        }

        return "";
    }

    public String shortOrdinal(Long n) {
        return (n == null) ? null : shortOrdinal(n.longValue());
    }

    /**
     * @hidden
     */
    public String shortOrdinal(long n) {
        String str = Long.toString(n);

        if (n < 0) {
            n = -n;
        }

        n %= 100;

        if (n >= 10 && n <= 20) {
            str += "th";
        }
        else {
            if (n > 20) n %= 10;
            
            switch ((int)n) {
            case 1:
                str += "st"; break;
            case 2:
                str += "nd"; break;
            case 3:
                str += "rd"; break;
            default:
                str += "th"; break;
            }
        }

        return str;
    }

    public String ordinal(Long n) {
        return (n == null) ? null : ordinal(n.longValue());
    }

    /**
     * @hidden
     */
    public String ordinal(long n) {
        if (n == 0) {
            return "zeroth";
        }
        
        StringBuffer buf = new StringBuffer(20);

        if (n < 0) {
            buf.append("negative ");
            n = -n;
        }

        n = cardinalGroup(buf, n, 1000000000000000000L, "quintillion");
        n = cardinalGroup(buf, n, 1000000000000000L, "quadrillion");
        n = cardinalGroup(buf, n, 1000000000000L, "trillion");
        n = cardinalGroup(buf, n, 1000000000L, "billion");
        n = cardinalGroup(buf, n, 1000000L, "million");
        n = cardinalGroup(buf, n, 1000L, "thousand");

        if (n == 0) {
            buf.append("th");
        }
        else {
            cardinal999(buf, n, true);
        }

        return buf.toString();
    }

    public String cardinal(Long n) {
        return (n == null) ? null : cardinal(n.longValue());
    }

    /**
     * @hidden
     */
    public String cardinal(long n) {
        if (n == 0) {
            return "zero";
        }
        
        StringBuffer buf = new StringBuffer(20);

        if (n < 0) {
            buf.append("negative ");
            n = -n;
        }

        n = cardinalGroup(buf, n, 1000000000000000000L, "quintillion");
        n = cardinalGroup(buf, n, 1000000000000000L, "quadrillion");
        n = cardinalGroup(buf, n, 1000000000000L, "trillion");
        n = cardinalGroup(buf, n, 1000000000L, "billion");
        n = cardinalGroup(buf, n, 1000000L, "million");
        n = cardinalGroup(buf, n, 1000L, "thousand");

        cardinal999(buf, n, false);

        return buf.toString();
    }

    private static long cardinalGroup(StringBuffer buf, long n, 
                                      long threshold, String groupName) {
        if (n >= threshold) {
            cardinal999(buf, n / threshold, false);
            buf.append(' ');
            buf.append(groupName);
            n %= threshold;
            if (n >= 100) {
                buf.append(", ");
            }
            else if (n != 0) {
                buf.append(" and ");
            }
        }

        return n;
    }

    private static void cardinal999(StringBuffer buf, long n, 
                                    boolean ordinal) {
        n = cardinalGroup(buf, n, 100L, "hundred");

        if (n == 0) {
            if (ordinal) {
                buf.append("th");
            }
            return;
        }

        if (n >= 20) {
            switch ((int)n / 10) {
            case 2:
                buf.append("twen");
                break;
            case 3:
                buf.append("thir");
                break;
            case 4:
                buf.append("for");
                break;
            case 5:
                buf.append("fif");
                break;
            case 6:
                buf.append("six");
                break;
            case 7:
                buf.append("seven");
                break;
            case 8:
                buf.append("eigh");
                break;
            case 9:
                buf.append("nine");
                break;
            }

            n %= 10;
            if (n != 0) {
                buf.append("ty-");
            }
            else {
                if (!ordinal) {
                    buf.append("ty");
                }
                else {
                    buf.append("tieth");
                }
            }
        }
        
        switch ((int)n) {
        case 1:
            if (!ordinal) {
                buf.append("one");
            }
            else {
                buf.append("first");
            }
            break;
        case 2:
            if (!ordinal) {
                buf.append("two");
            }
            else {
                buf.append("second");
            }
            break;
        case 3:
            if (!ordinal) {
                buf.append("three");
            }
            else {
                buf.append("third");
            }
            break;
        case 4:
            if (!ordinal) {
                buf.append("four");
            }
            else {
                buf.append("fourth");
            }
            break;
        case 5:
            if (!ordinal) {
                buf.append("five");
            }
            else {
                buf.append("fifth");
            }
            break;
        case 6:
            if (!ordinal) {
                buf.append("six");
            }
            else {
                buf.append("sixth");
            }
            break;
        case 7:
            if (!ordinal) {
                buf.append("seven");
            }
            else {
                buf.append("seventh");
            }
            break;
        case 8:
            if (!ordinal) {
                buf.append("eight");
            }
            else {
                buf.append("eighth");
            }
            break;
        case 9:
            if (!ordinal) {
                buf.append("nine");
            }
            else {
                buf.append("ninth");
            }
            break;
        case 10:
            if (!ordinal) {
                buf.append("ten");
            }
            else {
                buf.append("tenth");
            }
            break;
        case 11:
            if (!ordinal) {
                buf.append("eleven");
            }
            else {
                buf.append("eleventh");
            }
            break;
        case 12:
            if (!ordinal) {
                buf.append("twelve");
            }
            else {
                buf.append("twelfth");
            }
            break;
        case 13:
            buf.append("thirteen");
            if (ordinal) buf.append("th");
            break;
        case 14:
            buf.append("fourteen");
            if (ordinal) buf.append("th");
            break;
        case 15:
            buf.append("fifteen");
            if (ordinal) buf.append("th");
            break;
        case 16:
            buf.append("sixteen");
            if (ordinal) buf.append("th");
            break;
        case 17:
            buf.append("seventeen");
            if (ordinal) buf.append("th");
            break;
        case 18:
            buf.append("eighteen");
            if (ordinal) buf.append("th");
            break;
        case 19:
            buf.append("nineteen");
            if (ordinal) buf.append("th");
            break;
        }
    }

    public String replace(String source, String pattern, String replacement) {
        return replace(source, pattern, replacement, 0);
    }

    public String replace(String source, String pattern,
                          String replacement, int fromIndex) {
        if (replacement == null) {
            replacement = toString(replacement);
        }
        return StringReplacer.replace(source, pattern, replacement, fromIndex);
    }

    public String replace(String source, Map patternReplacements) {
        if (source == null) {
            return null;
        }

        int mapSize = patternReplacements.size();
        String[] patterns = new String[mapSize];
        String[] replacements = new String[mapSize];

        Iterator it = patternReplacements.entrySet().iterator();
        for (int i=0; it.hasNext(); i++) {
            Map.Entry entry = (Map.Entry)it.next();
            
            patterns[i] = toString(entry.getKey());
            replacements[i] = toString(entry.getValue());
        }

        return StringReplacer.replace(source, patterns, replacements);
    }

    public String replaceFirst(String source, String pattern,
                               String replacement) {
        if (replacement == null) {
            replacement = toString(replacement);
        }
        return StringReplacer.replaceFirst(source, pattern, replacement);
    }

    public String replaceFirst(String source, String pattern,
                               String replacement, int fromIndex) {
        if (replacement == null) {
            replacement = toString(replacement);
        }
        return StringReplacer.replaceFirst(source, pattern, replacement, fromIndex);
    }

    public String replaceLast(String source, String pattern,
                              String replacement) {
        if (replacement == null) {
            replacement = toString(replacement);
        }
        return StringReplacer.replaceLast(source, pattern, replacement);
    }

    public String replaceLast(String source, String pattern,
                              String replacement, int fromIndex) {
        if (replacement == null) {
            replacement = toString(replacement);
        }
        return StringReplacer.replaceLast(source, pattern, replacement, fromIndex);
    }
    
    public boolean isArray(Object o) {
        return o!=null && o.getClass().isArray();
    }
    
    public void sort(Object[] array, String onColumn, boolean reverse) {
        Class objClass = getObjectClass(array);
        if (objClass != null) {
            sort(array, objClass, onColumn, reverse);
        }
    }
    
    public void sort(Object[] array, String[] onColumns,  boolean[] reverse) {
        Class arrayType = getObjectClass(array);
        if (arrayType != null) {
            sort(array, arrayType, onColumns, reverse);
        }       
    }
    
    public void sort(String[] array, boolean reverse, boolean ignoreCase) {
    	StringComparator comparator = new StringComparator(reverse, ignoreCase);
    	Arrays.sort(array, comparator);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void sort(Object[] array, boolean sortAscending) {
    	Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
        	Comparator comparator = null;
        	if (arrayType == String.class) {
        		comparator = new StringComparator(sortAscending, false);
        	} else if (Comparable.class.isAssignableFrom(arrayType)) {
        		comparator = new GenericComparator(sortAscending);
        	}
        	if (comparator != null) {
        		Arrays.sort(array, comparator);
        	} else {
        		System.err.println("Sorting arrays of type " + arrayType.getName() + " is not supported, " + 
        				"must implement Comparable.");
        	}
        } else {
       		System.err.println("Could not determine type of array to sort.");
        }
    }
    
    public void sortAscending(Object[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(int[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(double[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(float[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(byte[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(short[] array) {
    	Arrays.sort(array);
    }
    
    public void sortAscending(long[] array) {
    	Arrays.sort(array);
    }
    
    private static void sort(Object[] array, Class arrayType, String onColumn, boolean reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        if (onColumn != null && !onColumn.equals("")) {
            comparator = comparator.orderBy(onColumn);
        }       
        if (reverse) {            
            comparator = comparator.reverse();
        }
        Arrays.sort(array, comparator);
    }   
    
    private static void sort(Object[] array, Class arrayType, String[] onColumns, boolean[] reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        for (int i = 0; i < onColumns.length; i++) {
            comparator = comparator.orderBy(onColumns[i]);
            if (reverse[i] == true) {
                comparator = comparator.reverse();
            }
        }
        Arrays.sort(array, comparator);
    }

    private static Class getObjectClass(Object[] array) {
        Class result = null;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] != null) {
                    result = array[i].getClass();
                    break;
                }
            }
        }
        return result;
    }
    
    @SuppressWarnings("rawtypes")
	public static class GenericComparator implements Comparator<Comparable> {
    	
    	protected boolean sortAscending = true;
    	
    	public GenericComparator(boolean sortAscending) {
    		this.sortAscending = sortAscending;
    	}
    	
	    @SuppressWarnings("unchecked")
		public int compare(Comparable k1, Comparable k2) {
	    	if (k1 != null) {
				if (k2 != null) {
					int result = k1.compareTo(k2);
					return (sortAscending) ? result: -result;
				} else {
					return (sortAscending) ? 1: -1;
				}
	    	} else if (k2 != null) {
				return (sortAscending) ? -1: 1;		
			}
			return 0;
	    }
    }
    
    public static class StringComparator implements Comparator<String> {
    	
    	protected boolean sortAscending = true;
    	protected boolean ignoreCase = true;
    	
    	public StringComparator(boolean sortAscending, boolean ignoreCase) {
    		this.sortAscending = sortAscending;
    		this.ignoreCase = ignoreCase;
    	}
    	
	    public int compare(String s1, String s2) {
			if (s1 != null) {
				if (s2 != null) {
					int flag = 0;
					if (ignoreCase) {
						flag = s1.compareToIgnoreCase(s2);
					} else {
						flag = s1.compareTo(s2);
					}
					if (flag > 0) {
					    return (sortAscending) ? 1: -1;
					}
					if (flag < 0) {
					    return (sortAscending) ? -1: 1;
					}
				} else {
					return (sortAscending) ? 1: -1;
				}
			} else if (s2 != null) {
				return (sortAscending) ? -1: 1;		
			}
			return 0;
	    }
    }
}
