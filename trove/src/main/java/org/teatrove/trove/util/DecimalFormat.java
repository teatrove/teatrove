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

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Similar to {@link java.text.DecimalFormat}, except rounding is more
 * intuitive. Only formatting is supported, but all patterns are compatible
 * with {@link java.text.DecimalFormat}.
 * 
 * @author Brian S O'Neill
 * @version
 */
public class DecimalFormat implements Cloneable {
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

    private static Map cInstanceCache = new HashMap(7);

    public static DecimalFormat getInstance() {
        return getInstance(null, null, null);
    }

    public static DecimalFormat getInstance(String pattern)
        throws IllegalArgumentException
    {
        return getInstance(pattern, null, null);
    }

    public static DecimalFormat getInstance(Locale locale)
        throws IllegalArgumentException
    {
        return getInstance(null, locale, null);
    }

    public static DecimalFormat getInstance(String pattern, Locale locale)
        throws IllegalArgumentException
    {
        return getInstance(pattern, locale, null);
    }

    public static DecimalFormat getInstance
        (String pattern, DecimalFormatSymbols symbols)
        throws IllegalArgumentException
    {
        return getInstance(pattern, null, symbols);
    }

    public static synchronized DecimalFormat getInstance
        (String pattern, Locale locale, DecimalFormatSymbols symbols)
        throws IllegalArgumentException
    {
        Object key = pattern;
        if (locale != null) {
            key = new Pair(key, locale);
        }
        if (symbols != null) {
            key = new Pair(key, symbols);
        }

        DecimalFormat format = (DecimalFormat)cInstanceCache.get(key);

        if (format == null) {
            if (symbols == null) {
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                symbols = new DecimalFormatSymbols(locale);
            }
            if (pattern == null) {
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                if (nf instanceof java.text.DecimalFormat) {
                    pattern =
                        ((java.text.DecimalFormat)nf).toPattern();
                }
                else {
                    pattern = new java.text.DecimalFormat().toPattern();
                    symbols = null;
                }
            }
            format = new DecimalFormat(pattern, symbols);
            cInstanceCache.put(key, format);
        }

        return format;
    }

    private String mPattern;

    // Is true if integers need to be fully formatted.
    private boolean mFormatInteger;

    private String mPrefix;
    private String mSuffix;
    private String mMinusPrefix = "";
    private String mMinusSuffix = "";

    private int mMaxDigits = 20;
    private int mMaxFractDigits;
    private int mRoundMode = DecimalConvertor.ROUND_HALF_UP_DECIMAL;
    private int mMinWholeDigits;
    private int mMinFractDigits;
    private int mDecimalScale;
    private int mGroupingWidth;

    // These fields are used only with scientific notation.
    private int mExponentDigits;
    private int mExponentMultiple;

    // Localized formatting symbols.
    private char mMinusSymbol = '-';
    private char mPlusSymbol = '\uffff';
    private char mDecimalSeparator = '.';
    private char mGroupingSeparator = ',';
    private String mInfinity = "\u221e";
    private String mNaN = "\ufffd";
    private char mZeroDigit = '0';
    private char mDigit = '#';
    private char mPatternSeparator = ';';
    private char mPercent = '%';
    private char mPerMill = '\u2030';
    private String mExponentPrefix = "E";

    private DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        mPattern = pattern;

        // TODO: This doesn't account for quoted monetary symbols.
        boolean monetary = pattern.indexOf('\u00a4') >= 0;

        if (symbols != null) {
            mMinusSymbol = symbols.getMinusSign();
            if (monetary) {
                mDecimalSeparator = symbols.getMonetaryDecimalSeparator();
            }
            else {
                mDecimalSeparator = symbols.getDecimalSeparator();
            }
            mGroupingSeparator = symbols.getGroupingSeparator();
            mInfinity = symbols.getInfinity();
            mNaN = symbols.getNaN();
            mZeroDigit = symbols.getZeroDigit();

            mDigit = symbols.getDigit();
            mPatternSeparator = symbols.getPatternSeparator();
            mPercent = symbols.getPercent();
            mPerMill = symbols.getPerMill();
        }

        parsePattern();

        if (monetary) {
            String sym = symbols.getInternationalCurrencySymbol();
            mPrefix = StringReplacer.replace(mPrefix, "\u00a4\u00a4", sym);
            mSuffix = StringReplacer.replace(mSuffix, "\u00a4\u00a4", sym);
            mMinusPrefix = StringReplacer.replace(mMinusPrefix, "\u00a4\u00a4", sym);
            mMinusSuffix = StringReplacer.replace(mMinusSuffix, "\u00a4\u00a4", sym);
            
            sym = symbols.getCurrencySymbol();
            mPrefix = StringReplacer.replace(mPrefix, "\u00a4", sym);
            mSuffix = StringReplacer.replace(mSuffix, "\u00a4", sym);
            mMinusPrefix = StringReplacer.replace(mMinusPrefix, "\u00a4", sym);
            mMinusSuffix = StringReplacer.replace(mMinusSuffix, "\u00a4", sym);
        }

        mFormatInteger =
            mMinWholeDigits > 1 ||
            mMinFractDigits > 0 ||
            mDecimalScale != 0 ||
            mGroupingWidth > 0 ||
            mExponentMultiple > 0 ||
            mMinusSymbol != '-' ||
            mPlusSymbol != '\uffff' ||
            mDecimalSeparator != '.' ||
            mZeroDigit != '0';

        if (mMinusPrefix.length() == 0) {
            mMinusPrefix = null;
        }
        if (mMinusSuffix.length() == 0) {
            mMinusSuffix = null;
        }
    }

    /*
    public void format(float v, Writer out) throws IOException {
        // TODO
    }

    public void format(double v, Writer out) throws IOException {
        // TODO
    }

    public void format(int v, Writer out) throws IOException {
        // TODO
    }

    public void format(long v, Writer out) throws IOException {
        // TODO
    }
    */

    public String format(float v) {
        return format(v, new StringBuffer()).toString();
    }

    public String format(double v) {
        return format(v, new StringBuffer()).toString();
    }

    public String format(int v) {
        if (mFormatInteger) {
            return format(v, new StringBuffer()).toString();
        }

        if (v >= 0 && v < 100) {
            return INT_VALUES[v];
        }

        return Integer.toString(v);
    }

    public String format(long v) {
        if (mFormatInteger) {
            return format(v, new StringBuffer()).toString();
        }

        if (v >= 0 && v < 100) {
            return INT_VALUES[(int)v];
        }

        return Long.toString(v);
    }

    public StringBuffer format(float v, StringBuffer buf) {
        String prefix, suffix;
        char plus, minus;

        if (Float.isNaN(v)) {
            return buf.append(mNaN);
        }

        if (mMinusPrefix != null && DecimalConvertor.isNegative(v)) {
            prefix = mMinusPrefix;
            suffix = mMinusSuffix;
            plus = '\uffff';
            minus = '\uffff';
        }
        else {
            prefix = mPrefix;
            suffix = mSuffix;
            plus = mPlusSymbol;
            minus = mMinusSymbol;
        }

        buf.append(prefix);

        if (mExponentMultiple == 0) {
            DecimalConvertor.format
                (v, buf,
                 mMaxDigits, mMaxFractDigits,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth,
                 mInfinity, mNaN);
        }
        else {
            int exp = DecimalConvertor.formatScientific
                (v, buf,
                 mMaxDigits, mExponentMultiple,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth,
                 mInfinity, mNaN);

            if (!Float.isInfinite(v)) {
                appendExponent(buf, exp);
            }
        }

        if (mZeroDigit != '0') {
            replace(buf, prefix.length(), '0', mZeroDigit);
        }

        return buf.append(suffix);
    }

    public StringBuffer format(double v, StringBuffer buf) {
        String prefix, suffix;
        char plus, minus;

        if (Double.isNaN(v)) {
            return buf.append(mNaN);
        }

        if (mMinusPrefix != null && DecimalConvertor.isNegative(v)) {
            prefix = mMinusPrefix;
            suffix = mMinusSuffix;
            plus = '\uffff';
            minus = '\uffff';
        }
        else {
            prefix = mPrefix;
            suffix = mSuffix;
            plus = mPlusSymbol;
            minus = mMinusSymbol;
        }

        buf.append(prefix);

        if (mExponentMultiple == 0) {
            DecimalConvertor.format
                (v, buf,
                 mMaxDigits, mMaxFractDigits,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth,
                 mInfinity, mNaN);
        }
        else {
            int exp = DecimalConvertor.formatScientific
                (v, buf,
                 mMaxDigits, mExponentMultiple,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth,
                 mInfinity, mNaN);

            if (!Double.isInfinite(v)) {
                appendExponent(buf, exp);
            }
        }

        if (mZeroDigit != '0') {
            replace(buf, prefix.length(), '0', mZeroDigit);
        }

        return buf.append(suffix);
    }

    public StringBuffer format(int v, StringBuffer buf) {
        if (!mFormatInteger) {
            if (v < 100) {
                if (v >= 0) {
                    return buf.append(INT_VALUES[v]);
                }
                if (v > -100) {
                    return buf.append('-').append(INT_VALUES[-v]);
                }
            }
            return buf.append(v);
        }

        String prefix, suffix;
        char plus, minus;

        if (mMinusPrefix != null && v < 0) {
            prefix = mMinusPrefix;
            suffix = mMinusSuffix;
            plus = '\uffff';
            minus = '\uffff';
        }
        else {
            prefix = mPrefix;
            suffix = mSuffix;
            plus = mPlusSymbol;
            minus = mMinusSymbol;
        }

        buf.append(prefix);

        if (mExponentMultiple == 0) {
            DecimalConvertor.format
                (v, buf,
                 mMaxDigits, mMaxFractDigits,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth);
        }
        else {
            int exp = DecimalConvertor.formatScientific
                (v, buf,
                 mMaxDigits, mExponentMultiple,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth);
            appendExponent(buf, exp);
        }

        if (mZeroDigit != '0') {
            replace(buf, prefix.length(), '0', mZeroDigit);
        }

        return buf.append(suffix);
    }

    public StringBuffer format(long v, StringBuffer buf) {
        if (!mFormatInteger) {
            if (v < 100) {
                if (v >= 0) {
                    return buf.append(INT_VALUES[(int)v]);
                }
                if (v > -100) {
                    return buf.append('-').append(INT_VALUES[-(int)v]);
                }
            }
            return buf.append(v);
        }

        String prefix, suffix;
        char plus, minus;

        if (mMinusPrefix != null && v < 0) {
            prefix = mMinusPrefix;
            suffix = mMinusSuffix;
            plus = '\uffff';
            minus = '\uffff';
        }
        else {
            prefix = mPrefix;
            suffix = mSuffix;
            plus = mPlusSymbol;
            minus = mMinusSymbol;
        }

        buf.append(prefix);

        if (mExponentMultiple == 0) {
            DecimalConvertor.format
                (v, buf,
                 mMaxDigits, mMaxFractDigits,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth);
        }
        else {
            int exp = DecimalConvertor.formatScientific
                (v, buf,
                 mMaxDigits, mExponentMultiple,
                 mRoundMode,
                 mMinWholeDigits, mMinFractDigits,
                 minus, plus,
                 mDecimalSeparator, mDecimalScale,
                 mGroupingSeparator, mGroupingWidth);
            appendExponent(buf, exp);
        }

        if (mZeroDigit != '0') {
            replace(buf, prefix.length(), '0', mZeroDigit);
        }

        return buf.append(suffix);
    }

    public String getPattern() {
        return mPattern;
    }

    public int getMaxDigits() {
        return mMaxDigits;
    }

    public int getMaxFractDigits() {
        return mMaxFractDigits;
    }

    public int getRoundMode() {
        return mRoundMode;
    }

    public int getMinWholeDigits() {
        return mMinWholeDigits;
    }

    public int getMinFractDigits() {
        return mMinWholeDigits;
    }

    public int getDecimalScale() {
        return mDecimalScale;
    }

    public int getGroupingWidth() {
        return mGroupingWidth;
    }

    public int getExponentDigits() {
        return mExponentDigits;
    }

    public int getExponentMultiple() {
        return mExponentMultiple;
    }

    public char getMinusSymbol() {
        return mMinusSymbol;
    }

    public char getPlusSymbol() {
        return mPlusSymbol;
    }

    public char getDecimalSeparator() {
        return mDecimalSeparator;
    }

    public char getGroupingSeparator() {
        return mGroupingSeparator;
    }

    public String getInfinity() {
        return mInfinity;
    }

    public String getNaN() {
        return mNaN;
    }

    public char getZeroDigit() {
        return '0';
    }

    public String getExponentPrefix() {
        return mExponentPrefix;
    }

    public DecimalFormat setDecimalSeparator(char c) {
        try {
            DecimalFormat df = (DecimalFormat)clone();
            df.mDecimalSeparator = c;
            return df;
        }
        catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public DecimalFormat setGroupingSeparator(char c) {
        try {
            DecimalFormat df = (DecimalFormat)clone();
            df.mGroupingSeparator = c;
            return df;
        }
        catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public DecimalFormat setInfinity(String str) {
        try {
            DecimalFormat df = (DecimalFormat)clone();
            df.mInfinity = str;
            return df;
        }
        catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public DecimalFormat setNaN(String str) {
        try {
            DecimalFormat df = (DecimalFormat)clone();
            df.mNaN = str;
            return df;
        }
        catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public DecimalFormat setExponentPrefix(String str) {
        try {
            DecimalFormat df = (DecimalFormat)clone();
            df.mExponentPrefix = str;
            return df;
        }
        catch (CloneNotSupportedException e) {
            return this;
        }
    }

    private void appendExponent(StringBuffer buf, int exp) {
        buf.append(mExponentPrefix);

        if (exp < 0) {
            buf.append(mMinusSymbol);
            exp = -exp;
        }

        String str = (exp < 100) ? INT_VALUES[exp] : Integer.toString(exp);

        if (mExponentDigits > 1) {
            for (int i=str.length(); i<mExponentDigits; i++) {
                buf.append(mZeroDigit);
            }
        }

        buf.append(str);
    }

    private void replace(StringBuffer buf, int start,
                         char oldChar, char newChar)
    {
        for (int i = buf.length(); --i >= start; ) {
            if (buf.charAt(i) == oldChar) {
                buf.setCharAt(i, newChar);
            }
        }
    }

    private void parsePattern() {
        String pattern = mPattern;

        int length = pattern.length();
        int i = 0;

        StringBuffer buf = new StringBuffer();
        i = parsePrefixOrSuffix(pattern, i, buf);
        mPrefix = new String(buf.toString()).intern();
        buf.setLength(0);

        // Parse number format.
        boolean fract = false;
        boolean groupSeparatorAllowed = false;
        int groupIndex = 0;
        int maxWholeDigits = 0;
        char c = '\0';
        while (i < length) {
            c = pattern.charAt(i++);
            if (c == '0') { //mZeroDigit) {
                if (!fract) {
                    mMinWholeDigits++;
                    maxWholeDigits++;
                    groupSeparatorAllowed = true;
                }
                else {
                    if (mMaxFractDigits != 0) {
                        error();
                    }
                    mMinFractDigits++;
                }
            }
            else if (c == '#') { //mDigit) {
                if (!fract) {
                    if (mMinWholeDigits > 0) {
                        error();
                    }
                    maxWholeDigits++;
                    groupSeparatorAllowed = true;
                }
                else {
                    if (mMaxFractDigits == 0) {
                        mMaxFractDigits = mMinFractDigits;
                    }
                    mMaxFractDigits++;
                }
            }
            else if (c == '.') { //mDecimalSeparator) {
                if (!fract) {
                    fract = true;
                    groupSeparatorAllowed = false;
                }
                else {
                    error("Multiple decimal separators in pattern");
                }
                if (groupIndex != 0) {
                    mGroupingWidth = i - groupIndex - 1;
                    if (mGroupingWidth == 0) {
                        error();
                    }
                    groupIndex = 0;
                }
            }
            else if (c == ',') { //mGroupingSeparator) {
                if (!groupSeparatorAllowed) {
                    error();
                }
                else {
                    groupIndex = i;
                }
            }
            else if (c == 'E') {
                int count = scanRun(pattern, i, mZeroDigit);
                if (count == 0) {
                    i--;
                }
                else {
                    i += count;
                    mExponentDigits = count;
                    if (i < length) {
                        c = pattern.charAt(i++);
                        if (c != ';') {
                            i--;
                        }
                    }
                    else {
                        c = '\0';
                    }
                }
                break;
            }
            else if (c == ';') { //mPatternSeparator) {
                break;
            }
            else {
                i--;
                break;
            }
        }

        if (groupIndex != 0) {
            mGroupingWidth = i - groupIndex;
            if (mGroupingWidth == 0) {
                error();
            }
        }

        if (mMaxFractDigits < mMinFractDigits) {
            mMaxFractDigits = mMinFractDigits;
        }

        if (mExponentDigits > 0) {
            // Adjust parameters for scientific notation.
            mMaxDigits = mMinWholeDigits + mMaxFractDigits;

            if (maxWholeDigits > mMinWholeDigits && maxWholeDigits > 1) {
                mExponentMultiple = maxWholeDigits;
                mMinWholeDigits = 1;
            }
            else {
                mExponentMultiple = 1;
            }
        }

        if (c != mPatternSeparator) {
            i = parsePrefixOrSuffix(pattern, i, buf);
            mSuffix = new String(buf.toString()).intern();
            buf.setLength(0);
            if (i < length) {
                if (pattern.charAt(i) != ';') {
                    error();
                }
                else {
                    i++;
                }
            }
            else {
                return;
            }
        }
        else {
            mSuffix = "";
        }

        i = parsePrefixOrSuffix(pattern, i, buf);
        mMinusPrefix = new String(buf.toString()).intern();
        buf.setLength(0);

        while (i < length) {
            c = pattern.charAt(i++);
            if (c == mZeroDigit || c == mDigit || c == mDecimalSeparator ||
                c == mGroupingSeparator) {
                continue;
            }
            else if (c == 'E') {
                int count = scanRun(pattern, i, mZeroDigit);
                if (count == 0) {
                    i--;
                }
                else {
                    i += count;
                }
                break;
            }
            else {
                i--;
                break;
            }
        }
        
        i = parsePrefixOrSuffix(pattern, i, buf);
        mMinusSuffix = new String(buf.toString()).intern();
        if (i < length) {
            error();
        }
    }

    private int parsePrefixOrSuffix(String pattern, int i, StringBuffer buf) {
        int length = pattern.length();

        int inQuote = 0;
        while (i < length) {
            char c = pattern.charAt(i++);
            if (inQuote != 0) {
                if (c == '\'') {
                    if (inQuote == 1) {
                        buf.append('\'');
                    }
                    inQuote = 0;
                }
                else {
                    buf.append(c);
                    inQuote++;
                }
            }
            else {
                if (c == '\'') {
                    inQuote = 1;
                }
                else if (c == mZeroDigit || c == mDigit ||
                         c == mDecimalSeparator || c == mGroupingSeparator ||
                         c == mPatternSeparator ||
                         c == '\ufffe' || c == '\uffff') {

                    i--;
                    break;
                }
                else if (c == mPercent) {
                    if (mDecimalScale == 3) {
                        error("Cannot combine percent and permill " + 
                              "characters in pattern");
                    }
                    mDecimalScale = 2;
                    buf.append(c);
                }
                else if (c == mPerMill) {
                    if (mDecimalScale == 2) {
                        error("Cannot combine percent and permill " + 
                              "characters in pattern");
                    }
                    mDecimalScale = 3;
                    buf.append(c);
                }
                else {
                    buf.append(c);
                }
            }
        }
        
        if (inQuote != 0) {
            error();
        }

        return i;
    }

    private int scanRun(String pattern, int i, char c) {
        int count = 0;
        int length = pattern.length();
        for (; i < length; i++) {
            if (pattern.charAt(i) == c) {
                count++;
            }
            else {
                break;
            }
        }
        return count;
    }

    private void error() {
        error("Malformed pattern");
    }

    private void error(String message) {
        throw new IllegalArgumentException(message + " \"" + mPattern + '"');
    }
}
