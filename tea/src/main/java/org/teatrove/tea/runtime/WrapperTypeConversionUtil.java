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

import org.teatrove.trove.util.IntegerFactory;

/**
 * 
 * @author Guy A. Molinari
 * @version

 */

public class WrapperTypeConversionUtil {

    public static Number convert(Number from, Class toType) {
        if (toType == Integer.class || toType == Integer.TYPE)
            return toInteger(from);
        if (toType == Double.class || toType == Double.TYPE)
            return toDouble(from);
        if (toType == Long.class || toType == Long.TYPE)
            return toLong(from);
        if (toType == Short.class || toType == Short.TYPE)
            return toShort(from);
        if (toType == Float.class || toType == Float.TYPE)
            return toFloat(from);
        if (toType == Byte.class || toType == Byte.TYPE)
            return toByte(from);
        return from;
    }

    public static Integer toInteger(Number value) {
        return value != null ? IntegerFactory.toInteger(value.intValue()) : null;
    }

    public static Integer toInteger(Boolean value) {
        return value != null ? IntegerFactory.toInteger(
            value.booleanValue() ? 1 : 0) : null;
    }

    public static Integer toInteger(Character value) {
        return value != null ? IntegerFactory.toInteger(
            (int) value.charValue()) : null;
    }

    public static Integer toInteger(String value) {
        try {
            return value != null ? IntegerFactory.toInteger(
                Integer.parseInt(value)) : null;
        }
        catch (NumberFormatException nx) { return null; }
    }

    public static Double toDouble(Number value) {
        return value != null ? new Double(value.doubleValue()) : null;
    }

    public static Long toLong(Number value) {
        return value != null ? new Long(value.longValue()) : null;
    }

    public static Short toShort(Number value) {
        return value != null ? new Short(value.shortValue()) : null;
    }

    public static Float toFloat(Number value) {
        return value != null ? new Float(value.floatValue()) : null;
    }

    public static Byte toByte(Number value) {
        return value != null ? new Byte(value.byteValue()) : null;
    }
}
