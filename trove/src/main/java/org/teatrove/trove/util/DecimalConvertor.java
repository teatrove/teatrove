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

/**
 * Accurately converts floating point numbers to decimal digits. The core
 * functions implement the Fixed-Precision Positive Floating-Point Printout
 * algorithm presented in "How to Print Floating-Point Numbers Accurately" by
 * Guy L. Steele Jr and Jon L White.
 *
 * @author Brian S O'Neill
 * @version
 */
public class DecimalConvertor {
    /**
     * Rounding mode that rounds up if the remainder is 1/2 or more. This
     * rounding mode is more exact than ROUND_HALF_UP_DECIMAL, but it may
     * display unexpected results.
     */
    public static final int ROUND_HALF_UP = 0;

    /**
     * When using this rounding mode, an additional decimal digit may be
     * produced. If this digit is 5 or more, the number is rounded up. The
     * additional digit is discarded. Although the produced number is less
     * exact than ROUND_HALF_UP, it is more like what a user would expect. For
     * example, 6.35 when converted to 2 digits is 6.4. With ROUND_HALF_UP, the
     * digits are 6.3. This is because the exact binary floating point value is
     * slightly less than 6.35.
     */
    public static final int ROUND_HALF_UP_DECIMAL = 1;

    static final int[] I_TENTH_POWERS;
    static final long[] L_TENTH_POWERS;
    
    static {
        I_TENTH_POWERS = new int[10];
        {
            int v = 1;
            for (int i=0; i<10; i++) {
                I_TENTH_POWERS[i] = v;
                v *= 10;
            }
        }
        
        L_TENTH_POWERS = new long[19];
        {
            long v = 1;
            for (int i=0; i<19; i++) {
                L_TENTH_POWERS[i] = v;
                v *= 10;
            }
        }
    }
        
    /**
     * Formats a floating point value like Float.toString, except values
     * are never formatted in scientific notation.
     *
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     */
    public static void format(float v, StringBuffer buf) {
        format(v, buf, 10, 100, ROUND_HALF_UP, 1, 1,
               '-', '\uffff', '.', 0, ',', 0, "Infinity", "NaN");
    }

    /**
     * Formats a floating point value like Double.toString, except values
     * are never formatted in scientific notation.
     *
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     */
    public static void format(double v, StringBuffer buf) {
        format(v, buf, 20, 100, ROUND_HALF_UP, 1, 1,
               '-', '\uffff', '.', 0, ',', 0, "Infinity", "NaN");
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     * @param infinity i.e. "Infinity" or "\u221e"
     * @param nan i.e. "NaN" or "\ufffd"
     */
    public static void format(float v,
                              StringBuffer buf,
                              int maxDigits, int maxFractDigits,
                              int roundMode,
                              int minWholeDigits, int minFractDigits,
                              char minusSymbol, char plusSymbol,
                              char decimalSeparator, int decimalScale,
                              char groupingSeparator, int groupingWidth,
                              String infinity, String nan)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator,
                           infinity, nan)) {
            return;
        }

        char[] digits = new char[Math.min(10, maxDigits) + 1];
        maxFractDigits += decimalScale;
        int result = toDecimalDigits
            (v, digits, 0, maxDigits, maxFractDigits, roundMode);

        if ((result & 0xffff) == 0) {
            formatSpecials((int)0, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator);
            return;
        }

        formatResult(digits, result, buf, maxFractDigits, minWholeDigits,
                     minFractDigits, decimalSeparator, decimalScale,
                     groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce 
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     * @param infinity i.e. "Infinity" or "\u221e"
     * @param nan i.e. "NaN" or "\ufffd"
     */
    public static void format(double v,
                              StringBuffer buf,
                              int maxDigits, int maxFractDigits,
                              int roundMode,
                              int minWholeDigits, int minFractDigits,
                              char minusSymbol, char plusSymbol,
                              char decimalSeparator, int decimalScale,
                              char groupingSeparator, int groupingWidth,
                              String infinity, String nan)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator,
                           infinity, nan)) {
            return;
        }

        char[] digits = new char[Math.min(20, maxDigits) + 1];
        maxFractDigits += decimalScale;
        int result = toDecimalDigits
            (v, digits, 0, maxDigits, maxFractDigits, roundMode);

        if ((result & 0xffff) == 0) {
            formatSpecials((int)0, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator);
            return;
        }

        formatResult(digits, result, buf, maxFractDigits, minWholeDigits,
                     minFractDigits, decimalSeparator, decimalScale,
                     groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     */
    public static void format(int v,
                              StringBuffer buf,
                              int maxDigits, int maxFractDigits,
                              int roundMode,
                              int minWholeDigits, int minFractDigits,
                              char minusSymbol, char plusSymbol,
                              char decimalSeparator, int decimalScale,
                              char groupingSeparator, int groupingWidth)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator)) {
            return;
        }

        if (v < 0) {
            v = -v;
        }

        char[] digits = new char[Math.min(10, maxDigits) + 1];
        maxFractDigits += decimalScale;
        int result = toDecimalDigits(v & 0xffffffffL, 32, 32, digits, 0,
                                     maxDigits, maxFractDigits, roundMode);

        if ((result & 0xffff) == 0) {
            formatSpecials((int)0, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator);
            return;
        }

        formatResult(digits, result, buf, maxFractDigits, minWholeDigits,
                     minFractDigits, decimalSeparator, decimalScale,
                     groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     */
    public static void format(long v,
                              StringBuffer buf,
                              int maxDigits, int maxFractDigits,
                              int roundMode,
                              int minWholeDigits, int minFractDigits,
                              char minusSymbol, char plusSymbol,
                              char decimalSeparator, int decimalScale,
                              char groupingSeparator, int groupingWidth)
    {
        if (v == (int)v) {
            format((int)v, buf, maxDigits, maxFractDigits, roundMode,
                   minWholeDigits, minFractDigits,
                   minusSymbol, plusSymbol,
                   decimalSeparator, decimalScale,
                   groupingSeparator, groupingWidth);
            return;
        }

        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator)) {
            return;
        }

        if (v < 0) {
            v = -v;
        }

        char[] digits = new char[Math.min(20, maxDigits) + 1];
        maxFractDigits += decimalScale;
        LargeUInt largev = new LargeUInt();
        largev.setValue(v);
        int result = toDecimalDigits
            (largev, 64, 64, digits, 0, maxDigits, maxFractDigits, roundMode);

        if ((result & 0xffff) == 0) {
            formatSpecials((int)0, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator);
            return;
        }

        formatResult(digits, result, buf, maxFractDigits, minWholeDigits,
                     minFractDigits, decimalSeparator, decimalScale,
                     groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param exponentMultiple when > 1, decimal exponent is adjusted to be
     * a multiple of this; i.e. use 3 for engineering notation
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     * @param infinity i.e. "Infinity" or "\u221e"
     * @param nan i.e. "NaN" or "\ufffd"
     * @return decimal exponent
     */
    public static int formatScientific(float v,
                                       StringBuffer buf,
                                       int maxDigits, int exponentMultiple,
                                       int roundMode,
                                       int minWholeDigits, int minFractDigits,
                                       char minusSymbol, char plusSymbol,
                                       char decimalSeparator, int decimalScale,
                                       char groupingSeparator,
                                       int groupingWidth,
                                       String infinity, String nan)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator,
                           infinity, nan)) {
            return 0;
        }

        char[] digits = new char[Math.min(10, maxDigits) + 1];
        int result = toDecimalDigits(v, digits, 0, maxDigits, 100, roundMode);
        
        return formatScientificResult(digits, result, buf, exponentMultiple,
                                      minWholeDigits, minFractDigits,
                                      decimalSeparator, decimalScale,
                                      groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param exponentMultiple when > 1, decimal exponent is adjusted to be
     * a multiple of this; i.e. use 3 for engineering notation
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     * @param infinity i.e. "Infinity" or "\u221e"
     * @param nan i.e. "NaN" or "\ufffd"
     * @return decimal exponent
     */
    public static int formatScientific(double v,
                                       StringBuffer buf,
                                       int maxDigits, int exponentMultiple,
                                       int roundMode,
                                       int minWholeDigits, int minFractDigits,
                                       char minusSymbol, char plusSymbol,
                                       char decimalSeparator, int decimalScale,
                                       char groupingSeparator,
                                       int groupingWidth,
                                       String infinity, String nan)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator,
                           infinity, nan)) {
            return 0;
        }

        char[] digits = new char[Math.min(20, maxDigits) + 1];
        int result = toDecimalDigits
            (v, digits, 0, maxDigits, 100, roundMode);
        
        return formatScientificResult(digits, result, buf, exponentMultiple,
                                      minWholeDigits, minFractDigits,
                                      decimalSeparator, decimalScale,
                                      groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param exponentMultiple when > 1, decimal exponent is adjusted to be
     * a multiple of this; i.e. use 3 for engineering notation
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @param groupingWidth i.e. 3 or 0 for none
     * @return decimal exponent
     */
    public static int formatScientific(int v,
                                       StringBuffer buf,
                                       int maxDigits, int exponentMultiple,
                                       int roundMode,
                                       int minWholeDigits, int minFractDigits,
                                       char minusSymbol, char plusSymbol,
                                       char decimalSeparator, int decimalScale,
                                       char groupingSeparator,
                                       int groupingWidth)
    {
        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator)) {
            return 0;
        }

        if (v < 0) {
            v = -v;
        }

        char[] digits = new char[Math.min(10, maxDigits) + 1];
        int result = toDecimalDigits(v & 0xffffffffL, 32, 32, digits, 0,
                                     maxDigits, 100, roundMode);

        return formatScientificResult(digits, result, buf, exponentMultiple,
                                      minWholeDigits, minFractDigits,
                                      decimalSeparator, decimalScale,
                                      groupingSeparator, groupingWidth);
    }

    /**
     * @param v value
     * @param buf buffer to receive formatted number
     * @param maxDigits maximum number of manstissa digits to produce
     * @param exponentMultiple when > 1, decimal exponent is adjusted to be
     * a multiple of this; i.e. use 3 for engineering notation
     * @param roundMode i.e. ROUND_HALF_UP
     * @param minWholeDigits minimum number of whole digits to produce
     * @param minFractDigits minimum number of fractional digits to produce
     * @param minusSymbol i.e. '-' or '\uffff' for none
     * @param plusSymbol i.e. '+' or '\uffff' for none
     * @param decimalSeparator i.e. '.'
     * @param decimalScale i.e. 0 for none or 2 for percentages
     * @param groupingWidth i.e. 3 or 0 for none
     * @param groupingSeparator i.e. ',' or '\uffff' for none
     * @return decimal exponent
     */
    public static int formatScientific(long v,
                                       StringBuffer buf,
                                       int maxDigits, int exponentMultiple,
                                       int roundMode,
                                       int minWholeDigits, int minFractDigits,
                                       char minusSymbol, char plusSymbol,
                                       char decimalSeparator, int decimalScale,
                                       char groupingSeparator,
                                       int groupingWidth)
    {
        if (v == (int)v) {
            return formatScientific((int)v, buf,
                                    maxDigits, exponentMultiple,
                                    roundMode,
                                    minWholeDigits, minFractDigits,
                                    minusSymbol, plusSymbol,
                                    decimalSeparator, decimalScale,
                                    groupingSeparator, groupingWidth);
        }

        if (formatSpecials(v, buf, minWholeDigits, minFractDigits,
                           minusSymbol, plusSymbol, decimalSeparator)) {
            return 0;
        }

        if (v < 0) {
            v = -v;
        }

        char[] digits = new char[Math.min(20, maxDigits) + 1];
        LargeUInt largev = new LargeUInt();
        largev.setValue(v);
        int result = toDecimalDigits
            (largev, 64, 64, digits, 0, maxDigits, 100, roundMode);

        return formatScientificResult(digits, result, buf, exponentMultiple,
                                      minWholeDigits, minFractDigits,
                                      decimalSeparator, decimalScale,
                                      groupingSeparator, groupingWidth);
    }

    private static boolean formatSpecials(float v,
                                          StringBuffer buf,
                                          int minWholeDigits,
                                          int minFractDigits,
                                          char minusSymbol, char plusSymbol,
                                          char decimalSeparator,
                                          String infinity, String nan)
    {
        if (Float.isNaN(v)) {
            buf.append(nan);
            return true;
        }

        if (isNegative(v)) {
            if (minusSymbol != '\uffff') {
                buf.append(minusSymbol);
            }
        }
        else {
            if (plusSymbol != '\uffff') {
                buf.append(plusSymbol);
            }
        }

        if (Float.isInfinite(v)) {
            buf.append(infinity);
            return true;
        }

        if (v == 0) {
            if (minWholeDigits <= 0 && minFractDigits <= 0) {
                buf.append('0');
                return true;
            }
            for (int i=0; i<minWholeDigits; i++) {
                buf.append('0');
            }
            if (minFractDigits > 0) {
                buf.append(decimalSeparator);
                for (int i=0; i<minFractDigits; i++) {
                    buf.append('0');
                }
            }
            return true;
        }

        return false;
    }

    private static boolean formatSpecials(double v,
                                          StringBuffer buf,
                                          int minWholeDigits,
                                          int minFractDigits,
                                          char minusSymbol, char plusSymbol,
                                          char decimalSeparator,
                                          String infinity, String nan)
    {
        if (Double.isNaN(v)) {
            buf.append(nan);
            return true;
        }

        if (isNegative(v)) {
            if (minusSymbol != '\uffff') {
                buf.append(minusSymbol);
            }
        }
        else {
            if (plusSymbol != '\uffff') {
                buf.append(plusSymbol);
            }
        }

        if (Double.isInfinite(v)) {
            buf.append(infinity);
            return true;
        }

        if (v == 0) {
            if (minWholeDigits <= 0 && minFractDigits <= 0) {
                buf.append('0');
                return true;
            }
            for (int i=0; i<minWholeDigits; i++) {
                buf.append('0');
            }
            if (minFractDigits > 0) {
                buf.append(decimalSeparator);
                for (int i=0; i<minFractDigits; i++) {
                    buf.append('0');
                }
            }
            return true;
        }

        return false;
    }

    private static boolean formatSpecials(int v,
                                          StringBuffer buf,
                                          int minWholeDigits,
                                          int minFractDigits,
                                          char minusSymbol, char plusSymbol,
                                          char decimalSeparator)
    {
        if (v < 0) {
            if (minusSymbol != '\uffff') {
                buf.append(minusSymbol);
            }
        }
        else {
            if (plusSymbol != '\uffff') {
                buf.append(plusSymbol);
            }
        }

        if (v == 0) {
            if (minWholeDigits <= 0 && minFractDigits <= 0) {
                buf.append('0');
                return true;
            }
            for (int i=0; i<minWholeDigits; i++) {
                buf.append('0');
            }
            if (minFractDigits > 0) {
                buf.append(decimalSeparator);
                for (int i=0; i<minFractDigits; i++) {
                    buf.append('0');
                }
            }
            return true;
        }

        return false;
    }

    private static boolean formatSpecials(long v,
                                          StringBuffer buf,
                                          int minWholeDigits,
                                          int minFractDigits,
                                          char minusSymbol, char plusSymbol,
                                          char decimalSeparator)
    {
        if (v < 0) {
            if (minusSymbol != '\uffff') {
                buf.append(minusSymbol);
            }
        }
        else {
            if (plusSymbol != '\uffff') {
                buf.append(plusSymbol);
            }
        }

        if (v == 0) {
            if (minWholeDigits <= 0 && minFractDigits <= 0) {
                buf.append('0');
                return true;
            }
            for (int i=0; i<minWholeDigits; i++) {
                buf.append('0');
            }
            if (minFractDigits > 0) {
                buf.append(decimalSeparator);
                for (int i=0; i<minFractDigits; i++) {
                    buf.append('0');
                }
            }
            return true;
        }

        return false;
    }

    private static void formatResult(char[] digits, int result,
                                     StringBuffer buf,
                                     int maxFractDigits,
                                     int minWholeDigits, int minFractDigits,
                                     char decimalSeparator, int decimalScale,
                                     char groupingSeparator, int groupingWidth)
    {
        int ndigits = result & 0xffff;

        // Ignore extra trailing zeros.
        while (ndigits > 0 && digits[ndigits - 1] == '0') {
            ndigits--;
        }
        
        int decimal = (result >> 16) + decimalScale;
        int wholeDigits = decimal <= 0 ? 0 : decimal;
        int fractOffset = 0;

        int wholeCounter = Math.max(wholeDigits, minWholeDigits);
        int wcs = wholeCounter - 1;

        if (wholeCounter <= 1 || groupingSeparator == '\uffff') {
            groupingWidth = 0;
        }

        for (int i=wholeDigits; i<minWholeDigits; i++) {
            if (groupingWidth != 0 && (wholeCounter-- % groupingWidth) == 0) {
                if (wholeCounter != wcs) {
                    buf.append(groupingSeparator);
                }
            }
            buf.append('0');
        }

        if (wholeDigits > 0) {
            int amt = Math.min(wholeDigits, ndigits);
            fractOffset += amt;

            if (groupingWidth == 0) {
                buf.append(digits, 0, amt);
                for (int i=amt; i<wholeDigits; i++) {
                    buf.append('0');
                }
            }
            else {
                for (int i=0; i<amt; i++) {
                    if ((wholeCounter-- % groupingWidth) == 0) {
                        if (wholeCounter != wcs) {
                            buf.append(groupingSeparator);
                        }
                    }
                    buf.append(digits[i]);
                }
                for (int i=amt; i<wholeDigits; i++) {
                    if ((wholeCounter-- % groupingWidth) == 0) {
                        buf.append(groupingSeparator);
                    }
                    buf.append('0');
                }
            }
        }

        int fractDigits = ndigits - decimal;
        if (fractDigits < 0) {
            fractDigits = 0;
        }

        if (fractOffset >= ndigits) {
            if (minFractDigits > 0) {
                buf.append(decimalSeparator);
                for (int i=0; i<minFractDigits; i++) {
                    buf.append('0');
                }
            }
        }
        else if (fractDigits > 0 || minFractDigits > 0) {
            buf.append(decimalSeparator);

            for (int i = decimal; i++ < 0; ) {
                buf.append('0');
            }

            if (fractOffset < ndigits) {
                buf.append(digits, fractOffset, ndigits - fractOffset);
            }

            for (int i=fractDigits; i<minFractDigits; i++) {
                buf.append('0');
            }
        }
    }

    private static int formatScientificResult
        (char[] digits, int result,
         StringBuffer buf,
         int exponentMultiple,
         int minWholeDigits, int minFractDigits,
         char decimalSeparator, int decimalScale,
         char groupingSeparator, int groupingWidth)
    {
        int k = result >> 16;
        if (decimalScale != 0) {
            k += decimalScale;
            result = k << 16 | result & 0xffff;
        }
        int exponent = k - 1;
        decimalScale = -exponent;

        if (exponentMultiple > 1) {
            int rem = exponent % exponentMultiple;
            if (rem < 0) {
                rem += exponentMultiple;
            }
            exponent -= rem;
            decimalScale += rem;
        }
        else {
            if (minWholeDigits > 1) {
                exponent -= (minWholeDigits - 1);
                decimalScale += (minWholeDigits - 1);
            }
        }

        formatResult(digits, result, buf, 100, minWholeDigits,
                     minFractDigits, decimalSeparator, decimalScale,
                     groupingSeparator, groupingWidth);

        return exponent;
    }

    /**
     * Produces decimal digits for non-zero, finite floating point values.
     * The sign of the value is discarded. Passing in Infinity or NaN produces
     * invalid digits. The maximum number of decimal digits that this
     * function will likely produce is 9.
     *
     * @param v value
     * @param digits buffer to receive decimal digits
     * @param offset offset into digit buffer
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @return Upper 16 bits: decimal point offset; lower 16 bits: number of
     * digits produced
     */
    public static int toDecimalDigits(float v,
                                      char[] digits, int offset, int maxDigits,
                                      int maxFractDigits, int roundMode)
    {
        int bits = Float.floatToIntBits(v);
        int f = bits & 0x7fffff;
        int e = (bits >> 23) & 0xff;
        if (e != 0) {
            // Normalized number.
            return toDecimalDigits(f + 0x800000, e - 126, 24, digits, offset,
                                   maxDigits, maxFractDigits, roundMode);
        }
        else {
            // Denormalized number.
            return toDecimalDigits(f, -125, 23, digits, offset,
                                   maxDigits, maxFractDigits, roundMode);
        }
    }

    /**
     * Produces decimal digits for non-zero, finite floating point values.
     * The sign of the value is discarded. Passing in Infinity or NaN produces
     * invalid digits. The maximum number of decimal digits that this
     * function will likely produce is 18.
     *
     * @param v value
     * @param digits buffer to receive decimal digits
     * @param offset offset into digit buffer
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @return Upper 16 bits: decimal point offset; lower 16 bits: number of
     * digits produced
     */
    public static int toDecimalDigits(double v,
                                      char[] digits, int offset, int maxDigits,
                                      int maxFractDigits, int roundMode)
    {
        // NOTE: The value 144115188075855872 is converted to
        // 144115188075855870, which is as correct as possible. Java's
        // Double.toString method doesn't round off the last digit.

        long bits = Double.doubleToLongBits(v);
        long f = bits & 0xfffffffffffffL;
        int e = (int)((bits >> 52) & 0x7ff);
        if (e != 0) {
            // Normalized number.
            return toDecimalDigits(f + 0x10000000000000L, e - 1022, 53,
                                   digits, offset, maxDigits, maxFractDigits,
                                   roundMode);
        }
        else {
            // Denormalized number.
            return toDecimalDigits(f, -1023, 52, digits, offset,
                                   maxDigits, maxFractDigits, roundMode);
        }
    }

    /**
     * Produces decimal digits for binary floating point values greater than
     * zero.
     *
     * @param f floating point mantissa
     * @param e floating point exponent
     * @param p floating point precision (number of bits)
     * @param digits buffer to receive decimal digits
     * @param offset offset into digit buffer
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @return Upper 16 bits: decimal point offset; lower 16 bits: number of
     * digits produced
     */
    public static int toDecimalDigits(int f, int e, int p,
                                      char[] digits, int offset,
                                      int maxDigits, int maxFractDigits,
                                      int roundMode)
    {
        if (f == 0 || maxDigits == 0) {
            return 0;
        }

        // These magic numbers for determining when to extend precision
        // were determined empirically by calling rangeTest.
        if (p > 24 || e > 25 || (p - e) > 26) {
            if (p > 56 || e > 56 || (p - e) > 58) {
                LargeUInt largef = new LargeUInt();
                largef.setValue(f);
                return toDecimalDigits
                    (largef, e, p, digits, offset, maxDigits, maxFractDigits,
                     roundMode);
            }
            return toDecimalDigits
                ((long)f, e, p, digits, offset, maxDigits, maxFractDigits,
                 roundMode);
        }

        int S = 1;
        int R = f;
        int mlo = 1;
        
        if (p > e) {
            S <<= p - e;
        }
        else {
            R <<= e - p;
            mlo <<= e - p;
        }

        int mhi = mlo;

        // Begin Simple-Fixup

        if (f == (1 << (p - 1))) {
            mhi <<= 1;
            R <<= 1;
            S <<= 1;
        }

        int k = 0;
        int limit = (S + 9) / 10;
        while (R < limit) {
            k--;
            R *= 10;
        }

        if (k < 0) {
            if (mlo == mhi) {
                mlo = (mhi *= I_TENTH_POWERS[-k]);
            }
            else {
                mlo *= I_TENTH_POWERS[-k];
                mhi *= I_TENTH_POWERS[-k];
            }
        }

        int cursor = offset;
        limit = (R << 1) + mhi;
        int S2 = S << 1;
        while (S2 <= limit) {
            S *= 10;
            S2 = S << 1;
            k++;
        }

        // End Simple-Fixup

        limit = k + maxFractDigits;
        if (limit > maxDigits) {
            limit = offset + maxDigits;
        }
        else {
            limit += offset;
        }

        if (limit <= offset) {
            if (limit == offset) {
                if ((R << 1) >= S) {
                    digits[cursor++] = '1';
                    k++;
                }
            }
            return (k << 16) | (cursor - offset);
        }

        if (roundMode == ROUND_HALF_UP_DECIMAL) {
            // Emit one additional digit to be tested.
            limit++;
        }

        while (true) {
            int temp = R * 10;
            digits[cursor++] = (char)((temp / S) + '0');
            R = temp % S;
            
            if (mlo == mhi) {
                mlo = (mhi *= 10);
            }
            else {
                mlo *= 10;
                mhi *= 10;
            }
            
            temp = R << 1;
            if (temp < mlo) {
                if (temp > (S2 - mhi)) {
                    // low and high
                    if (temp >= S) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
            else if (temp > (S2 - mhi)) {
                // high and not low
                k += increment(digits, offset, cursor);
                break;
            }
            
            if (cursor >= limit) {
                if (roundMode != ROUND_HALF_UP_DECIMAL) {
                    if (temp >= S) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
        }
        
        if (cursor >= limit && roundMode == ROUND_HALF_UP_DECIMAL) {
            // Chop off extra digit.
            cursor--;
            if (digits[cursor] >= '5') {
                k += increment(digits, offset, cursor);
            }
        }

        return (k << 16) | (cursor - offset);
    }

    /**
     * Produces decimal digits for binary floating point values greater than
     * zero.
     *
     * @param f floating point mantissa
     * @param e floating point exponent
     * @param p floating point precision (number of bits)
     * @param digits buffer to receive decimal digits
     * @param offset offset into digit buffer
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @return Upper 16 bits: decimal point offset; lower 16 bits: number of
     * digits produced
     */
    public static int toDecimalDigits(long f, int e, int p,
                                      char[] digits, int offset,
                                      int maxDigits, int maxFractDigits,
                                      int roundMode)
    {
        if (f == 0 || maxDigits == 0) {
            return 0;
        }

        // These magic numbers for determining when to extend precision
        // were determined empirically by calling rangeTest.
        if (p > 56 || e > 56 || (p - e) > 58) {
            LargeUInt largef = new LargeUInt();
            largef.setValue(f);
            return toDecimalDigits
                (largef, e, p, digits, offset, maxDigits, maxFractDigits,
                 roundMode);
        }

        long S = 1L;
        long R = f;
        long mlo = 1L;
        
        if (p > e) {
            S <<= p - e;
        }
        else {
            R <<= e - p;
            mlo <<= e - p;
        }

        long mhi = mlo;

        // Begin Simple-Fixup

        if (f == (1L << (p - 1))) {
            mhi <<= 1;
            R <<= 1;
            S <<= 1;
        }

        int k = 0;
        long limit = (S + 9) / 10;
        while (R < limit) {
            k--;
            R *= 10;
        }

        if (k < 0) {
            if (mlo == mhi) {
                mlo = (mhi *= L_TENTH_POWERS[-k]);
            }
            else {
                mlo *= L_TENTH_POWERS[-k];
                mhi *= L_TENTH_POWERS[-k];
            }
        }

        limit = (R << 1) + mhi;
        long S2 = S << 1;
        while (S2 <= limit) {
            S *= 10;
            S2 = S << 1;
            k++;
        }

        // End Simple-Fixup

        int cursor = offset;
        int ilimit = k + maxFractDigits;
        if (ilimit > maxDigits) {
            ilimit = offset + maxDigits;
        }
        else {
            ilimit += offset;
        }

        if (ilimit <= offset) {
            if (ilimit == offset) {
                if ((R << 1) >= S) {
                    digits[cursor++] = '1';
                    k++;
                }
            }
            return (k << 16) | (cursor - offset);
        }

        if (roundMode == ROUND_HALF_UP_DECIMAL) {
            // Emit one additional digit to be tested.
            ilimit++;
        }

        while (true) {
            long temp = R * 10;
            digits[cursor++] = (char)((temp / S) + '0');
            R = temp % S;
            
            if (mlo == mhi) {
                mlo = (mhi *= 10);
            }
            else {
                mlo *= 10;
                mhi *= 10;
            }
            
            temp = R << 1;
            if (temp < mlo) {
                if (temp > (S2 - mhi)) {
                    // low and high
                    if (temp >= S) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
            else if (temp > (S2 - mhi)) {
                // high and not low
                k += increment(digits, offset, cursor);
                break;
            }
            
            if (cursor >= ilimit) {
                if (roundMode != ROUND_HALF_UP_DECIMAL) {
                    if (temp >= S) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
        }

        if (cursor >= ilimit && roundMode == ROUND_HALF_UP_DECIMAL) {
            // Chop off extra digit.
            cursor--;
            if (digits[cursor] >= '5') {
                k += increment(digits, offset, cursor);
            }
        }

        return (k << 16) | (cursor - offset);
    }

    /**
     * Produces decimal digits for binary floating point values greater than
     * zero.
     *
     * @param f floating point mantissa <b>(value is destroyed)</b>
     * @param e floating point exponent
     * @param p floating point precision (number of bits)
     * @param digits buffer to receive decimal digits
     * @param offset offset into digit buffer
     * @param maxDigits maximum number of digits to produce
     * @param maxFractDigits maximum number of fractional digits to produce
     * @param roundMode i.e. ROUND_HALF_UP
     * @return Upper 16 bits: decimal point offset; lower 16 bits: number of
     * digits produced
     */
    public static int toDecimalDigits(LargeUInt f, int e, int p,
                                      char[] digits, int offset,
                                      int maxDigits, int maxFractDigits,
                                      int roundMode)
    {
        if (f.isZero() || maxDigits == 0) {
            return 0;
        }

        LargeUInt S = new LargeUInt();
        S.setValue(1);
        LargeUInt R = new LargeUInt(f);
        LargeUInt mlo = new LargeUInt();
        mlo.setValue(1);
        
        if (p > e) {
            S.shiftLeft(p - e);
        }
        else {
            R.shiftLeft(e - p);
            mlo.shiftLeft(e - p);
        }

        LargeUInt mhi = mlo;

        // Begin Simple-Fixup

        LargeUInt temp = new LargeUInt();
        temp.setValue(1);
        temp.shiftLeft(p - 1);

        if (f.compare(temp) == 0) {
            R.shiftLeft(1);
            S.shiftLeft(1);
            // Set this again later to be 2 * mlo.
            mhi = null;
        }
        
        int k = 0;
        temp.setValue(S);
        temp.add(9);
        temp.divideByTen();
        while (R.compare(temp) < 0) {
            k--;
            R.multiplyByTen();
        }

        if (k < 0) {
            mlo.multiplyByTenthPower(-k);
        }

        if (mhi == null) {
            mhi = new LargeUInt(mlo);
            mhi.shiftLeft(1);
        }

        temp.setValue(R);
        temp.shiftLeft(1);
        temp.add(mhi);
        LargeUInt S2 = new LargeUInt(S);
        S2.shiftLeft(1);
        int oldK = k;
        while (S2.compare(temp) < 0) {
            S2.multiplyByTen();
            k++;
        }

        if (k > oldK) {
            S.multiplyByTenthPower(k - oldK);
        }

        // End Simple-Fixup

        int cursor = offset;
        int limit = k + maxFractDigits;
        if (limit > maxDigits) {
            limit = offset + maxDigits;
        }
        else {
            limit += offset;
        }

        if (limit <= offset) {
            if (limit == offset) {
                temp.setValue(R);
                temp.shiftLeft(1);
                if (temp.compare(S) >= 0) {
                    digits[cursor++] = '1';
                    k++;
                }
            }
            return (k << 16) | (cursor - offset);
        }

        if (roundMode == ROUND_HALF_UP_DECIMAL) {
            // Emit one additional digit to be tested.
            limit++;
        }

        LargeUInt temp2 = f;

        while (true) {
            R.multiplyByTen();
            digits[cursor++] = (char)(R.divideClose(S) + '0');
            
            if (mlo == mhi) {
                mlo.multiplyByTen();
            }
            else {
                mlo.multiplyByTen();
                mhi.multiplyByTen();
            }
            
            temp.setValue(R);
            temp.shiftLeft(1);
            temp2.setValue(S2);
            temp2.subtract(mhi);
            
            if (temp.compare(mlo) < 0) {
                if (temp.compare(temp2) > 0) {
                    // low and high
                    if (temp.compare(S) >= 0) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
            else if (temp.compare(temp2) > 0) {
                // high and not low
                k += increment(digits, offset, cursor);
                break;
            }
            
            if (cursor >= limit) {
                if (roundMode != ROUND_HALF_UP_DECIMAL) {
                    if (temp.compare(S) >= 0) {
                        k += increment(digits, offset, cursor);
                    }
                }
                break;
            }
        }

        if (cursor >= limit && roundMode == ROUND_HALF_UP_DECIMAL) {
            // Chop off extra digit.
            cursor--;
            if (digits[cursor] >= '5') {
                k += increment(digits, offset, cursor);
            }
        }

        return (k << 16) | (cursor - offset);
    }

    private static int increment(char[] digits, int offset, int cursor) {
        while (true) {
            char c;
            if ((c = digits[--cursor]) != '9') {
                digits[cursor] = (char)(c + 1);
                return 0;
            }
            if (cursor == offset) {
                digits[cursor] = '1';
                return 1;
            }
            digits[cursor] = '0';
        }
    }

    public static boolean isNegative(float v) {
        return v < 0 || (v == 0 && 1 / v < 0);
    }

    public static boolean isNegative(double v) {
        return v < 0 || (v == 0 && 1 / v < 0);
    }

    /**
     * Mutable large unsigned integer class, similar to BigInteger, but
     * specialized. The set of operations is limited to those needed for
     * converting binary floating point numbers to decimal.
     *
     * @author Brian S O'Neill
     * @version

     */
    public final static class LargeUInt {
        // Internally, the number is stored as an array of radix 4294967296
        // digits.  Digits are stored little-endian: least-significant digit is
        // at index 0. This facilitates expansion and contraction as precision
        // requirements change.
        
        private int[] mDigits;
        
        // Amount of valid digits stored in mDigits.
        private int mLength;
        
        public LargeUInt() {
            mDigits = new int[8];
            mLength = 1;
        }
        
        public LargeUInt(int initialCapacity) {
            if (initialCapacity < 1) {
                throw new IllegalArgumentException();
            }
            mDigits = new int[initialCapacity];
            mLength = 1;
        }
        
        public LargeUInt(LargeUInt v) {
            int[] vdigits = v.mDigits;
            int[] digits = new int[vdigits.length];
            int i = mLength = v.mLength;
            while (--i >= 0) {
                digits[i] = vdigits[i];
            }
            mDigits = digits;
        }
        
        public void setZero() {
            int[] digits = mDigits;
            int length = mLength;
            for (int i=0; i<length; i++) {
                digits[i] = 0;
            }
            mLength = 1;
        }
        
        public void setValue(int v) {
            int[] digits = mDigits;
            int length = mLength;
            digits[0] = v;
            for (int i=1; i<length; i++) {
                digits[i] = 0;
            }
            mLength = 1;
        }

        public void setValue(long v) {
            if ((v & 0xffffffff00000000L) == 0) {
                setValue((int)v);
                return;
            }
            int[] digits = mDigits;
            int length = mLength;
            digits[0] = (int)v;
            if (length < 2) {
                expand(2 - length)[1] = (int)(v >> 32);
            }
            else {
                digits[1] = (int)(v >> 32);
                for (int i=2; i<length; i++) {
                    digits[i] = 0;
                }
            }
        }
        
        public void setValue(LargeUInt v) {
            int[] digits = mDigits;
            int vlength = v.mLength;
            
            if (vlength > digits.length) {
                mDigits = (int[])v.mDigits.clone();
            }
            else {
                System.arraycopy(v.mDigits, 0, digits, 0, vlength);
            }
            
            mLength = vlength;
        }
        
        /**
         * Add v into this LargeUInt. (0 <= v <= 0xffffffffL)
         */
        public void add(long v) {
            int[] digits = mDigits;
            int length = mLength;
            
            int i;
            for (i=0; i<length; i++) {
                v += (digits[i] & 0xffffffffL);
                digits[i] = (int)v;
                if (v < 0x100000000L) {
                    return;
                }
                else {
                    v = 1;
                }
            }
            
            expand(1)[i] = 1;
        }
        
        /**
         * Add v into this LargeUInt.
         */
        public void add(LargeUInt v) {
            int[] digits = mDigits;
            int[] vdigits = v.mDigits;
            int length = mLength;
            int vlength = v.mLength;
            
            if (length < vlength) {
                digits = expand(vlength - length);
                length = vlength;
            }

            long c = 0;
            int i, j;
            for (i=0,j=0; i<length; i++) {
                c += (digits[i] & 0xffffffffL);
                if (j < vlength) {
                    c += (vdigits[j++] & 0xffffffffL);
                }
                digits[i] = (int)c;
                if (c < 0x100000000L) {
                    if (j >= vlength) {
                        return;
                    }
                    c = 0;
                }
                else {
                    c = 1;
                }
            }
            
            if (c != 0) {
                expand(1)[i] = 1;
            }
        }
        
        /**
         * Subtract v from this LargeUInt. (0 <= v <= 0xffffffffL)
         *
         * @return borrow bit
         */
        public int subtract(long v) {
            int[] digits = mDigits;
            int length = mLength;
            
            int i;
            for (i=0; i<length; i++) {
                v = (digits[i] & 0xffffffffL) - v;
                digits[i] = (int)v;
                if (v >= 0) {
                    shrinkLength();
                    return 0;
                }
                v = 1;
            }
            
            return 1;
        }
        
        /**
         * Subtract v from this LargeUInt.
         *
         * @return borrow bit
         */
        public int subtract(LargeUInt v) {
            int[] digits = mDigits;
            int[] vdigits = v.mDigits;
            int length = mLength;
            int vlength = v.mLength;
            
            if (length < vlength) {
                digits = expand(vlength - length);
                length = vlength;
            }
            
            long c = 0;
            int i, j;
            for (i=0,j=0; i<length; i++) {
                c = (digits[i] & 0xffffffffL) - c;
                if (j < vlength) {
                    c -= (vdigits[j++] & 0xffffffffL);
                }
                digits[i] = (int)c;
                if (c >= 0) {
                    if (j >= vlength) {
                        shrinkLength();
                        return 0;
                    }
                    c = 0;
                }
                else {
                    c = 1;
                }
            }
            
            return (int)c;
        }
        
        /**
         * Bitwise shift left, which is equivalent to multiplying this
         * LargeUInt by 2^v.
         */
        public void shiftLeft(int v) {
            int[] digits;
            int length;
            
            if (v <= 31) {
                digits = mDigits;
                length = mLength;
            }
            else {
                // Coarse shift.
                int coarse = v >>> 5; // v>>>5 == v/32
                digits = expand(coarse);
                length = mLength;
                
                int i = length - 1;
                while (i >= 0) {
                    int k = i - coarse;
                    if (k >= 0) {
                        digits[i--] = digits[k];
                    }
                    else {
                        while (i >= 0) {
                            digits[i--] = 0;
                        }
                        break;
                    }
                }
                
                v &= 31;
            }
            
            // Fine shift. This is essentially a specialized version of
            // multiply by 2^n. The '* v' expression is replaced by '<< v'.

            if (v == 0) {
                return;
            }
            
            long p = 0;
            for (int i=0; i<length; i++) {
                p += (digits[i] & 0xffffffffL) << v;
                digits[i] = (int)p;
                p >>>= 32;
            }
            
            if (p != 0) {
                expand(1)[mLength - 1] = (int)p;
            }
        }
        
        /**
         * Multiply this LargeUInt by v. (0 <= v <= 0xffffffffL)
         */
        public void multiply(long v) {
            if (v == 0) {
                setZero();
                return;
            }
            if (v == 1) {
                return;
            }
            
            int[] digits = mDigits;
            int length = mLength;
            
            long p = 0;
            for (int i=0; i<length; i++) {
                p += (digits[i] & 0xffffffffL) * v;
                digits[i] = (int)p;
                p >>>= 32;
            }
            
            if (p != 0) {
                expand(1)[mLength - 1] = (int)p;
            }
        }
        
        /**
         * Multiply this LargeUInt by ten.
         */
        public void multiplyByTen() {
            int[] digits = mDigits;
            int length = mLength;
            
            long p = 0;
            for (int i=0; i<length; i++) {
                p += (digits[i] & 0xffffffffL) * 10;
                digits[i] = (int)p;
                p >>>= 32;
            }
            
            if (p != 0) {
                expand(1)[mLength - 1] = (int)p;
            }
        }
        
        /**
         * Multiply this LargeUInt by 10^v.
         */
        public void multiplyByTenthPower(int v) {
            // I don't feel like implementing long multiplication. Just
            // multiply several times by large powers of ten.
            while (v > 9) {
                multiply(L_TENTH_POWERS[9]);
                v -= 9;
            }
            if (v != 0) {
                multiply(L_TENTH_POWERS[v]);
            }
        }
        
        /**
         * Divide this LargeUInt by v. (0 <= v <= 0xffffffffL)
         */
        public void divide(long v) throws ArithmeticException {
            if (v == 0) {
                throw new ArithmeticException();
            }
            if (v == 1) {
                return;
            }
            
            int[] digits = mDigits;
            int length = mLength;
            
            long divChunk = 0;
            for (int i=length; --i >= 0; ) {
                divChunk += digits[i] & 0xffffffffL;
                if (v <= divChunk) {
                    long q = divChunk / v;
                    digits[i] = (int)q;
                    divChunk -= (q * v);
                }
                else {
                    digits[i] = 0;
                    if (i == (length - 1) && i > 0) {
                        // Shrink the length.
                        mLength = (--length);
                    }
                }
                divChunk <<= 32;
            }
        }
        
        /**
         * Divide this LargeUInt by ten.
         */
        public void divideByTen() {
            int[] digits = mDigits;
            int length = mLength;
            
            long divChunk = 0;
            for (int i=length; --i >= 0; ) {
                divChunk += digits[i] & 0xffffffffL;
                if (10 <= divChunk) {
                    long q = divChunk / 10;
                    digits[i] = (int)q;
                    divChunk -= (q * 10);
                }
                else {
                    digits[i] = 0;
                    if (i == (length - 1) && i > 0) {
                        // Shrink the length.
                        mLength = (--length);
                    }
                }
                divChunk <<= 32;
            }
        }
        
        /**
         * Divide this LargeUInt by v, storing the remainder in this and
         * returning the quotient. This method assumes that (this / v) is very
         * small, less than 10. If the ratio is larger, performance degrades.
         */
        public int divideClose(LargeUInt v) throws ArithmeticException {
            if (v.isZero()) {
                throw new ArithmeticException("/ by zero");
            }
            
            // Just subtract until remainder is less than divisor. I don't want
            // to implement a full-blown long division routine.
            int q = 0;
            while (compare(v) >= 0) {
                q++;
                subtract(v);
            }
            return q;
        }

        /**
         * Returns <0 if (this < v), 0 if (this == v), or >0 if (this > v).
         */
        public int compare(LargeUInt v) {
            int[] digits = mDigits;
            int[] vdigits = v.mDigits;
            int length = mLength;
            int vlength = v.mLength;
            
            if (length == vlength) {
                for (int i = length; --i >= 0; ) {
                    int a = digits[i];
                    int b = vdigits[i];
                    if (a < b) {
                        return ((a ^ b) >= 0) ? -1 : 1;
                    }
                    else if (a > b) {
                        return ((a ^ b) >= 0) ? 1 : -1;
                    }
                }
                return 0;
            }
            else {
                // This works under the assumption that mLength is always no
                // larger than it needs to be because shrinkLength is called
                // whenever the most significant digits could go to zero.
                return length - vlength;
            }
        }
        
        public boolean isZero() {
            return mLength == 1 && mDigits[0] == 0;
        }
        
        public String toString() {
            int[] digits = mDigits;
            int length = mLength;
            
            StringBuffer buf = new StringBuffer(2 + length * 8);
            buf.append('0');
            buf.append('x');
            
            for (int i=length; --i>=0; ) {
                int digitsi = digits[i];
                String sub = Long.toHexString(((long)digitsi) & 0xffffffffL);
                int len = sub.length();
                if (len < 8 && i != (length - 1)) {
                    for (int j=8-len; --j>=0; ) {
                        buf.append('0');
                    }
                }
                buf.append(sub);
            }
            
            return buf.toString();
        }
        
        private void shrinkLength() {
            int[] digits = mDigits;
            int length = mLength;
            
            int i;
            for (i = length; --i >= 0; ) {
                if (digits[i] != 0) {
                    break;
                }
            }
            
            if (i + 1 < length) {
                mLength = (i < 0) ? 1 : i + 1;
            }
        }
        
        private int[] expand(int amt) {
            int[] digits = mDigits;
            int length = mLength;
            
            if (length + amt > digits.length) {
                // Double the storage capacity.
                int newLength = length * 2;
                if (newLength < (length + amt)) {
                    newLength = length + amt;
                }
                int[] newDigits = new int[newLength];
                System.arraycopy(digits, 0, newDigits, 0, length);
                digits = mDigits = newDigits;
            }
            
            mLength = length + amt;
            return digits;
        }
    }
}
