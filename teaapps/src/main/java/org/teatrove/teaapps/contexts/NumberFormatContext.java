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
}
