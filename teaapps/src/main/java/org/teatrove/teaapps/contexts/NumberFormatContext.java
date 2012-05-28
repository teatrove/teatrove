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

/**
 * Tea context that provides helper methods for converting and testing numeric
 * values.
 * 
 * @author Scott Jappinen
 */
public class NumberFormatContext {

    private static final Integer DEFAULT_INT_VALUE = new Integer(0);
    private static final Float DEFAULT_FLOAT_VALUE = new Float(0.0f);
    private static final Double DEFAULT_DOUBLE_VALUE = new Double(0.0d);

    /**
     * Convert the given value to an {@link Integer}. If the value is not in a 
     * valid numeric format, then a default value is returned.
     * 
     * @param value The value to convert
     * 
     * @return The converted numeric value
     * 
     * @see #isInteger(String)
     * @see Integer#valueOf(String)
     */
    public Integer convertStringToInteger(String value) {
        Integer result;
        try {
            result = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_INT_VALUE;
        }
        return result;

    }

    /**
     * Convert the given value to a {@link Float}. If the value is not in a 
     * valid numeric format, then a default value is returned.
     * 
     * @param value The value to convert
     * 
     * @return The converted numeric value
     * 
     * @see #isFloat(String)
     * @see Float#valueOf(String)
     */
    public Float convertStringToFloat(String value) {
        Float result;
        try {
            result = Float.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_FLOAT_VALUE;
        }
        return result;
    }

    /**
     * Convert the given value to a {@link Double}. If the value is not in a 
     * valid numeric format, then a default value is returned.
     * 
     * @param value The value to convert
     * 
     * @return The converted numeric value
     * 
     * @see #isDouble(String)
     * @see Double#valueOf(String)
     */
    public Double convertStringToDouble(String value) {
        Double result;
        try {
            result = Double.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_DOUBLE_VALUE;
        }
        return result;
    }

    /**
     * Check whether the given value is a valid numeric value that can be
     * converted to an {@link Integer}.
     * 
     * @param value The associated value to test
     * 
     * @return <code>true</code> The value is a valid integer,
     *         <code>false</code> otherwise
     *         
     * @see #convertStringToInteger(String)
     * @see Integer#valueOf(String)
     */
    public boolean isInteger(String value) {
        boolean result = true;
        try {
            Integer.valueOf(value);
        } catch (NumberFormatException e) {
            result = false;
        }
        return result;

    }

    /**
     * Check whether the given value is a valid numeric value that can be
     * converted to an {@link Float}.
     * 
     * @param value The associated value to test
     * 
     * @return <code>true</code> The value is a valid float,
     *         <code>false</code> otherwise
     *         
     * @see #convertStringToFloat(String)
     * @see Float#valueOf(String)
     */
    public boolean isFloat(String value) {
        boolean result = true;
        try {
            Float.valueOf(value);
        } catch (NumberFormatException e) {
            result = false;
        }
        return result;
    }

    /**
     * Check whether the given value is a valid numeric value that can be
     * converted to an {@link Double}.
     * 
     * @param value The associated value to test
     * 
     * @return <code>true</code> The value is a valid double,
     *         <code>false</code> otherwise
     *         
     * @see #convertStringToDouble(String)
     * @see Double#valueOf(String)
     */
    public boolean isDouble(String value) {
        boolean result = true;
        try {
            Double.valueOf(value);
        } catch (NumberFormatException e) {
            result = false;
        }
        return result;
    }

    /**
     * Get the minimum value for an integer.
     * 
     * @return {@link Integer#MIN_VALUE}
     */
    public int getIntegerMinValue() {
        return Integer.MIN_VALUE;
    }
    
    /**
     * Get the maximum value for an integer.
     * 
     * @return {@link Integer#MAX_VALUE}
     */
    public int getIntegerMaxValue() {
        return Integer.MAX_VALUE;
    }

    
    /**
     * A function that converts an integer to a short ordinal value.
     * i.e. 1st, 2nd, 3rd etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the short ordinal value of the specified
     * number
     */
    public String shortOrdinal(Long n) {
        return (n == null) ? null : shortOrdinal(n.longValue());
    }

    /**
     * A function that converts an integer to a short ordinal value.
     * i.e. 1st, 2nd, 3rd etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the short ordinal value of the specified
     * number
     */
    public String shortOrdinal(long n) {
        String str = Long.toString(n);

        if (n < 0) {
            n = -n;
        }

        n %= 100;

        if (n >= 10 && n <= 20) {
            str = str.concat("th");
        }
        else {
            if (n > 20) n %= 10;

            switch ((int) n) {
            case 1:
                str = str.concat("st"); break;
            case 2:
                str = str.concat("nd"); break;
            case 3:
                str = str.concat("rd"); break;
            default:
                str = str.concat("th"); break;
            }
        }

        return str;
    }

    /**
     * A function that converts an integer to an ordinal value. i.e. first,
     * second, third etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the ordinal value of the specified number
     */
    public String ordinal(Long n) {
        return (n == null) ? null : ordinal(n.longValue());
    }

    /**
     * A function that converts an integer to an ordinal value. i.e. first,
     * second, third etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the ordinal value of the specified number
     */
    public String ordinal(long n) {
        if (n == 0) {
            return "zeroth";
        }

        StringBuilder buffer = new StringBuilder(20);

        if (n < 0) {
            buffer.append("negative ");
            n = -n;
        }

        n = cardinalGroup(buffer, n, 1000000000000000000L, "quintillion");
        n = cardinalGroup(buffer, n, 1000000000000000L, "quadrillion");
        n = cardinalGroup(buffer, n, 1000000000000L, "trillion");
        n = cardinalGroup(buffer, n, 1000000000L, "billion");
        n = cardinalGroup(buffer, n, 1000000L, "million");
        n = cardinalGroup(buffer, n, 1000L, "thousand");

        if (n == 0) {
            buffer.append("th");
        }
        else {
            cardinal999(buffer, n, true);
        }

        return buffer.toString();
    }
    
    /**
     * A function that converts an integer to a cardinal value. i.e. one,
     * two, three etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the cardinal value of the specified number
     */
    public String cardinal(Long n) {
        return (n == null) ? null : cardinal(n.longValue());
    }

    /**
     * A function that converts an integer to a cardinal value. i.e. one,
     * two, three etc.
     *
     * @param n the number to convert
     *
     * @return a String containing the cardinal value of the specified number
     */
    public String cardinal(long n) {
        if (n == 0) {
            return "zero";
        }

        StringBuilder buffer = new StringBuilder(20);

        if (n < 0) {
            buffer.append("negative ");
            n = -n;
        }

        n = cardinalGroup(buffer, n, 1000000000000000000L, "quintillion");
        n = cardinalGroup(buffer, n, 1000000000000000L, "quadrillion");
        n = cardinalGroup(buffer, n, 1000000000000L, "trillion");
        n = cardinalGroup(buffer, n, 1000000000L, "billion");
        n = cardinalGroup(buffer, n, 1000000L, "million");
        n = cardinalGroup(buffer, n, 1000L, "thousand");

        cardinal999(buffer, n, false);

        return buffer.toString();
    }

    private static long cardinalGroup(StringBuilder buffer, long n,
                                      long threshold, String groupName) {
        if (n >= threshold) {
            cardinal999(buffer, n / threshold, false);
            buffer.append(' ');
            buffer.append(groupName);
            n %= threshold;
            if (n >= 100) {
                buffer.append(", ");
            }
            else if (n != 0) {
                buffer.append(" and ");
            }
        }

        return n;
    }

    private static void cardinal999(StringBuilder buffer, long n,
                                    boolean ordinal) {
        n = cardinalGroup(buffer, n, 100L, "hundred");

        if (n == 0) {
            if (ordinal) {
                buffer.append("th");
            }
            return;
        }

        if (n >= 20) {
            switch ((int) (n / 10)) {
            case 2:
                buffer.append("twen");
                break;
            case 3:
                buffer.append("thir");
                break;
            case 4:
                buffer.append("for");
                break;
            case 5:
                buffer.append("fif");
                break;
            case 6:
                buffer.append("six");
                break;
            case 7:
                buffer.append("seven");
                break;
            case 8:
                buffer.append("eigh");
                break;
            case 9:
                buffer.append("nine");
                break;
            }

            n %= 10;
            if (n != 0) {
                buffer.append("ty-");
            }
            else {
                if (!ordinal) { buffer.append("ty"); }
                else { buffer.append("tieth"); }
            }
        }

        switch ((int) n) {
        case 1:
            if (!ordinal) { buffer.append("one"); }
            else { buffer.append("first"); }
            break;
        case 2:
            if (!ordinal) { buffer.append("two"); }
            else { buffer.append("second"); }
            break;
        case 3:
            if (!ordinal) { buffer.append("three"); }
            else { buffer.append("third"); }
            break;
        case 4:
            if (!ordinal) { buffer.append("four"); }
            else { buffer.append("fourth"); }
            break;
        case 5:
            if (!ordinal) { buffer.append("five"); }
            else { buffer.append("fifth"); }
            break;
        case 6:
            if (!ordinal) { buffer.append("six"); }
            else { buffer.append("sixth"); }
            break;
        case 7:
            if (!ordinal) { buffer.append("seven"); }
            else { buffer.append("seventh"); }
            break;
        case 8:
            if (!ordinal) { buffer.append("eight"); }
            else { buffer.append("eighth"); }
            break;
        case 9:
            if (!ordinal) { buffer.append("nine"); }
            else { buffer.append("ninth"); }
            break;
        case 10:
            if (!ordinal) { buffer.append("ten"); }
            else { buffer.append("tenth"); }
            break;
        case 11:
            if (!ordinal) { buffer.append("eleven"); }
            else { buffer.append("eleventh"); }
            break;
        case 12:
            if (!ordinal) { buffer.append("twelve"); }
            else { buffer.append("twelfth"); }
            break;
        case 13:
            buffer.append("thirteen");
            if (ordinal) buffer.append("th");
            break;
        case 14:
            buffer.append("fourteen");
            if (ordinal) buffer.append("th");
            break;
        case 15:
            buffer.append("fifteen");
            if (ordinal) buffer.append("th");
            break;
        case 16:
            buffer.append("sixteen");
            if (ordinal) buffer.append("th");
            break;
        case 17:
            buffer.append("seventeen");
            if (ordinal) buffer.append("th");
            break;
        case 18:
            buffer.append("eighteen");
            if (ordinal) buffer.append("th");
            break;
        case 19:
            buffer.append("nineteen");
            if (ordinal) buffer.append("th");
            break;
        }
    }

}
