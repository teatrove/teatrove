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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.teatrove.trove.util.IntegerFactory;

/**
 * 
 * @author Guy A. Molinari
 */

public class WrapperTypeConversionUtil {

    public static final int OP_ADD = 0;
    public static final int OP_SUB = 1;
    public static final int OP_MULT = 2;
    public static final int OP_DIV = 3;
    public static final int OP_MOD = 4;

    protected static final int TYPE_DOUBLE = 4;
    protected static final int TYPE_FLOAT = 3;
    protected static final int TYPE_LONG = 2;
    protected static final int TYPE_INT = 1;
    
    public static Number math(int op, int leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal, rightVal.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) {
            return math(op, leftVal, rightVal.floatValue()); 
        }
        else if (type == TYPE_LONG) { 
            return math(op, leftVal, rightVal.longValue()); 
        }
        else { return math(op, leftVal, rightVal.intValue()); }
    }
    
    public static Number math(int op, long leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal, rightVal.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) { 
            return math(op, leftVal, rightVal.floatValue());
        }
        else { return math(op, leftVal, rightVal.longValue()); }
    }
    
    public static Number math(int op, float leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal, rightVal.doubleValue()); 
        }
        else { return math(op, leftVal, rightVal.floatValue()); }
    }

    public static Number math(int op, double leftVal, Number rightVal) {
        return math(op, leftVal, rightVal.doubleValue());
    }
    
    public static Number math(int op, Number leftVal, int rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal.doubleValue(), rightVal); 
        }
        else if (type == TYPE_FLOAT) { 
            return math(op, leftVal.floatValue(), rightVal);
        }
        else if (type == TYPE_LONG) { 
            return math(op, leftVal.longValue(), rightVal); 
        }
        else { return math(op, leftVal.intValue(), rightVal); }
    }
    
    public static Number math(int op, Number leftVal, long rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal.doubleValue(), rightVal); 
        }
        else if (type == TYPE_FLOAT) { 
            return math(op, leftVal.floatValue(), rightVal); 
        }
        else { return math(op, leftVal.longValue(), rightVal); }
    }
    
    public static Number math(int op, Number leftVal, float rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return math(op, leftVal.doubleValue(), rightVal); 
        }
        else { return math(op, leftVal.floatValue(), rightVal); }
    }

    public static Number math(int op, Number leftVal, double rightVal) {
        return math(op, leftVal.doubleValue(), rightVal);
    }
    
    /**
     * Perform a mathematical operation against two numbers. This first
     * coerces the two numbers to the best matching type, then performs the
     * given operation, and returns a similar type.  For example, adding a
     * {@link Long} and an {@link Integer} results in a {@link Long} being
     * returned.  In all cases, one of the built-in primitive wrappers will
     * be returned (ie: {@link Double}, {@link Float}, {@link Long}, or
     * {@link Integer}).  The following operations are supported:
     * 
     * <dl>
     *   <dt>{@link #OP_ADD}</dt>
     *   <dd>Add both numbers</dd>
     *   
     *   <dt>{@link #OP_SUB}</dt>
     *   <dd>Substract both numbers</dd>
     *   
     *   <dt>{@link #OP_MULT}</dt>
     *   <dd>Multiply both numbers</dd>
     *   
     *   <dt>{@link #OP_DIV}</dt>
     *   <dd>Divide both numbers</dd>
     *   
     *   <dt>{@link #OP_MOD}</dt>
     *   <dd>Perform a modulus operation on both numbers</dd>
     * </dl>
     * 
     * @param op The operation to perform
     * @param left The left value to operate on
     * @param right The right value to operate on
     * 
     * @return The result of the operation in the suitable type
     */
    public static Number math(int op, Number left, Number right) {
        int type = getCompatibleType(left, right);
        if (type == TYPE_DOUBLE) { 
            return math(op, left.doubleValue(), right.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) { 
            return math(op, left.floatValue(), right.floatValue());
        }
        else if (type == TYPE_LONG) { 
            return math(op, left.longValue(), right.longValue()); 
        }
        else if (type == TYPE_INT) { 
            return math(op, left.intValue(), right.intValue()); 
        }
        else {
            throw new IllegalStateException("unsupported number type: " + type);
        }
    }
    
    public static Double math(int op, double leftVal, double rightVal) {
        double result = 0;
        if (op == OP_ADD) { result = leftVal + rightVal; }
        else if (op == OP_SUB) { result = leftVal - rightVal; }
        else if (op == OP_MULT) { result = leftVal * rightVal; }
        else if (op == OP_DIV) { result = leftVal / rightVal; }
        else if (op == OP_MOD) { result = leftVal % rightVal; }
        else { throw new IllegalStateException("invalid opcode: " + op); }
        
        return Double.valueOf(result);
    }
                
    public static Float math(int op, float leftVal, float rightVal) {
        float result = 0;
        if (op == OP_ADD) { result = leftVal + rightVal; }
        else if (op == OP_SUB) { result = leftVal - rightVal; }
        else if (op == OP_MULT) { result = leftVal * rightVal; }
        else if (op == OP_DIV) { result = leftVal / rightVal; }
        else if (op == OP_MOD) { result = leftVal % rightVal; }
        else { throw new IllegalStateException("invalid opcode: " + op); }
        
        return Float.valueOf(result);
    }
    
    public static Long math(int op, long leftVal, long rightVal) {
        long result = 0;
        if (op == OP_ADD) { result = leftVal + rightVal; }
        else if (op == OP_SUB) { result = leftVal - rightVal; }
        else if (op == OP_MULT) { result = leftVal * rightVal; }
        else if (op == OP_DIV) { result = leftVal / rightVal; }
        else if (op == OP_MOD) { result = leftVal % rightVal; }
        else { throw new IllegalStateException("invalid opcode: " + op); }
        
        return Long.valueOf(result);
    }

    public static Integer math(int op, int leftVal, int rightVal) {
        int result = 0;
        if (op == OP_ADD) { result = leftVal + rightVal; }
        else if (op == OP_SUB) { result = leftVal - rightVal; }
        else if (op == OP_MULT) { result = leftVal * rightVal; }
        else if (op == OP_DIV) { result = leftVal / rightVal; }
        else if (op == OP_MOD) { result = leftVal % rightVal; }
        else { throw new IllegalStateException("invalid opcode: " + op); }
        
        return Integer.valueOf(result);
    }

    public static boolean isValid(Number value) {
        int type = getNumberType(value);
        if (type == TYPE_DOUBLE) { 
            return isValid(value.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) {
            return isValid(value.floatValue()); 
        }
        else if (type == TYPE_LONG) { 
            return isValid(value.longValue()); 
        }
        else if (type == TYPE_INT) {
            return isValid(value.intValue());
        }
        else { return isValid(value.doubleValue()); }
    }
    
    public static boolean isValid(double value) {
        return Double.compare(value, 0.0) != 0;
    }
    
    public static boolean isValid(float value) {
        return Float.compare(value, 0.0f) != 0;
    }
    
    public static boolean isValid(long value) {
        return value != 0L;
    }
    
    public static boolean isValid(int value) {
        return value != 0;
    }
    
    public static int compare(int leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal, rightVal.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) {
            return compare(leftVal, rightVal.floatValue()); 
        }
        else if (type == TYPE_LONG) { 
            return compare(leftVal, rightVal.longValue()); 
        }
        else { return compare(leftVal, rightVal.intValue()); }
    }
    
    public static int compare(long leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal, rightVal.doubleValue()); 
        }
        else if (type == TYPE_FLOAT) {
            return compare(leftVal, rightVal.floatValue()); 
        }
        else { return compare(leftVal, rightVal.longValue()); }
    }
    
    public static int compare(float leftVal, Number rightVal) {
        int type = getNumberType(rightVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal, rightVal.doubleValue()); 
        }
        else { return compare(leftVal, rightVal.floatValue()); }
    }
    
    public static int compare(double leftVal, Number rightVal) {
        return compare(leftVal, rightVal.doubleValue());
    }
    
    public static int compare(Number leftVal, int rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal.doubleValue(), rightVal); 
        }
        else if (type == TYPE_FLOAT) {
            return compare(leftVal.floatValue(), rightVal); 
        }
        else if (type == TYPE_LONG) { 
            return compare(leftVal.longValue(), rightVal); 
        }
        else { return compare(leftVal.intValue(), rightVal); }
    }
    
    public static int compare(Number leftVal, long rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal.doubleValue(), rightVal); 
        }
        else if (type == TYPE_FLOAT) {
            return compare(leftVal.floatValue(), rightVal); 
        }
        else { return compare(leftVal.longValue(), rightVal); }
    }
    
    public static int compare(Number leftVal, float rightVal) {
        int type = getNumberType(leftVal);
        if (type == TYPE_DOUBLE) { 
            return compare(leftVal.doubleValue(), rightVal); 
        }
        else { return compare(leftVal.floatValue(), rightVal); }
    }
    
    public static int compare(Number leftVal, double rightVal) {
        return compare(leftVal.doubleValue(), rightVal);
    }
    
    public static int compare(int leftVal, int rightVal) {
        return (leftVal == rightVal ? 0 : leftVal < rightVal ? -1 : 1);
    }
    
    public static int compare(long leftVal, long rightVal) {
        return (leftVal == rightVal ? 0 : leftVal < rightVal ? -1 : 1);
    }
    
    public static int compare(float leftVal, float rightVal) {
        return Float.compare(leftVal, rightVal);
    }
    
    public static int compare(double leftVal, double rightVal) {
        return Double.compare(leftVal, rightVal);
    }
    
    /**
     * This performs a mostly safe comparison between two abstract numbers.
     * If both numbers share the same hiearchy and that hiearchy implements
     * {@link Comparable}, then {@link Comparable#compareTo(Object)} is used to
     * compare the values.  Otherwise, the most compatible data type is used
     * to perform the conversion.  For example, a {@link Integer} compared to
     * a {@link Long} would compare as a long via {@link Number#longValue()}.
     * If either data type cannot be determined, then they are assumed to be
     * doubles and compared in order to avoid potential data loss.
     * 
     * @param left The left value to compare
     * @param right The right value to compare
     * 
     * @return less than 0 if left is less than right, 0 if equal, and greater
     *         than 0 if left is greater than right
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static int compare(Number left, Number right) {
        
        // compare directly when both are the same type and implement 
        // comparable (ie: Integer, Long, Short, etc wrappers all do)
        if (left.getClass().isAssignableFrom(right.getClass()) &&
            Comparable.class.isAssignableFrom(left.getClass())) {
            
            return ((Comparable) left).compareTo(right);
        }

        // compare directly in opposite direction in case the right side is
        // higher in the hiearchy
        else if (right.getClass().isAssignableFrom(left.getClass()) &&
                 Comparable.class.isAssignableFrom(right.getClass())) {
            return -((Comparable) right).compareTo(left);
        }
        
        // when not the same type, attempt to coerce to the greatest matching
        // types
        
        int type = getCompatibleType(left, right);
        if (type == TYPE_DOUBLE) {
            return compare(left.doubleValue(), right.doubleValue());
        }
        else if (type == TYPE_FLOAT) {
            return compare(left.floatValue(), right.floatValue());
        }
        else if (type == TYPE_LONG) {
            return compare(left.longValue(), right.longValue());
        }
        else if (type == TYPE_INT) {
            return compare(left.intValue(), right.intValue());
        }
        else {
            throw new IllegalStateException("unsupported number type: " + type);
        }
    }

    /**
     * Get the type most compatible between the two values that would be the
     * most safe to compare against.  This is generally the highest degree of
     * the number type. For example, a {@link #TYPE_INT} compared to a
     * {@link #TYPE_LONG} would compare against the long data type to avoid
     * losing data from a long to an int.
     * 
     * @param left  The left side value to compare
     * @param right The right side value to compare
     * 
     * @return {@link #TYPE_DOUBLE}, {@link #TYPE_FLOAT}, {@link #TYPE_LONG},
     *         or {@link #TYPE_INT} based on the compatible data type
     *         
     * @see #getNumberType(Number)
     */
    protected static int getCompatibleType(Number left, Number right) {
        return Math.max(getNumberType(left), getNumberType(right));
    }
    
    /**
     * Get the type of number for a given numerical value. This returns a given
     * constant based on the type of number including atomic values such as
     * {@link AtomicInteger} and {@link AtomicLong} and the big data values such
     * as {@link BigDecimal} and {@link BigInteger}.  Numbers with the same
     * number type are more directly comparable using a given primitive type.
     * For example, {@link Long} and {@link AtomicLong} are both types of
     * {@link #TYPE_LONG} and comparable via {@link Number#longValue()}.  If the
     * type is undefined or an unknown subclass of {@link Number}, then 
     * {@link #TYPE_DOUBLE} is assumed in order to be most compatible without
     * losing potential values.
     * 
     * @param value The value to check
     * 
     * @return {@link #TYPE_DOUBLE}, {@link #TYPE_FLOAT}, {@link #TYPE_LONG},
     *         or {@link #TYPE_INT} based on the data type
     */
    protected static int getNumberType(Number value) {
        if (value.getClass() == Double.TYPE ||
            value instanceof Double ||
            value instanceof BigDecimal) {
            return TYPE_DOUBLE;
        }
        else if (value.getClass() == Float.TYPE ||
                 value instanceof Float) {
            return TYPE_FLOAT;
        }
        else if (value.getClass() == Long.TYPE ||
                 value instanceof Long ||
                 value instanceof AtomicLong ||
                 value instanceof BigInteger) {
            return TYPE_LONG;
        }
        else if (value.getClass() == Integer.TYPE ||
                 value.getClass() == Short.TYPE ||
                 value.getClass() == Byte.TYPE ||
                 value instanceof Integer ||
                 value instanceof AtomicInteger ||
                 value instanceof Short ||
                 value instanceof Byte) {
            return TYPE_INT;
        }
        else {
            return TYPE_DOUBLE;
        }
    }
    
    public static Number convert(Number from, Class<?> toType) {
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
        if (value == null) { return null; }
        else if (value instanceof Integer) { return (Integer) value; }
        else { return IntegerFactory.toInteger(value.intValue()); }
    }

    public static Integer toInteger(Boolean value) {
        return value != null ? IntegerFactory.toInteger(
            value.booleanValue() ? 1 : 0) : null;
    }

    public static Integer toInteger(Character value) {
        return value != null ? IntegerFactory.toInteger(
            value.charValue()) : null;
    }

    public static Integer toInteger(String value) {
        try {
            return value != null ? IntegerFactory.toInteger(
                Integer.parseInt(value)) : null;
        }
        catch (NumberFormatException nx) { return null; }
    }

    public static Double toDouble(Number value) {
        if (value == null) { return null; }
        else if (value instanceof Double) { return (Double) value; }
        else { return Double.valueOf(value.doubleValue()); }
    }

    public static Long toLong(Number value) {
        if (value == null) { return null; }
        else if (value instanceof Long) { return (Long) value; }
        else { return Long.valueOf(value.longValue()); }
    }

    public static Short toShort(Number value) {
        if (value == null) { return null; }
        else if (value instanceof Short) { return (Short) value; }
        else { return Short.valueOf(value.shortValue()); }
    }

    public static Float toFloat(Number value) {
        if (value == null) { return null; }
        else if (value instanceof Float) { return (Float) value; }
        else { return Float.valueOf(value.floatValue()); }
    }

    public static Byte toByte(Number value) {
        if (value == null) { return null; }
        else if (value instanceof Byte) { return (Byte) value; }
        else { return Byte.valueOf(value.byteValue()); }
    }
}
