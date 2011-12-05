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
 * @author Scott Jappinen
 */
public class NumberFormatContext {

    private static final Integer DEFAULT_INT_VALUE = new Integer(0);
    private static final Float DEFAULT_FLOAT_VALUE = new Float(0.0f);
    private static final Double DEFAULT_DOUBLE_VALUE = new Double(0.0d);

    public Integer convertStringToInteger(String value) {
        Integer result;
        try {
            result = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_INT_VALUE;
        }
        return result;

    }

    public Float convertStringToFloat(String value) {
        Float result;
        try {
            result = Float.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_FLOAT_VALUE;
        }
        return result;
    }

    public Double convertStringToDouble(String value) {
        Double result;
        try {
            result = Double.valueOf(value);
        } catch (NumberFormatException e) {
            result = DEFAULT_DOUBLE_VALUE;
        }
        return result;
    }

    public Boolean isInteger(String value) {
        Boolean result = Boolean.TRUE;
        try {
            Integer.valueOf(value);
        } catch (NumberFormatException e) {
            result = Boolean.FALSE;
        }
        return result;

    }

    public Boolean isFloat(String value) {
        Boolean result = Boolean.TRUE;
        try {
            Float.valueOf(value);
        } catch (NumberFormatException e) {
            result = Boolean.FALSE;
        }
        return result;
    }

    public Boolean isDouble(String value) {
        Boolean result = Boolean.TRUE;
        try {
            Double.valueOf(value);
        } catch (NumberFormatException e) {
            result = Boolean.FALSE;
        }
        return result;
    }

    public int getIntegerMinValue() {
        return Integer.MIN_VALUE;
    }
}
