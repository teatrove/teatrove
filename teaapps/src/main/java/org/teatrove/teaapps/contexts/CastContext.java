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

import java.util.Date;

import org.joda.time.DateTime;

/**
 * The CastContext provides a Tea context for performing casting in Tea. This
 * context is a helper context for known types.  The following are equivalent:
 * 
 * <pre>
 *     value = getSomeObject()
 *     ints1 = toInt(value)
 *     ints2 = value as Integer
 * </pre>
 * 
 * Note that this purely provides casting and not conversions.  If the value 
 * cannot be cast, then a {@link ClassCastException} will be thrown.
 * 
 * @author Scott Jappinen
 */
public class CastContext {

    /**
     * Cast the given value as an Integer.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as an Integer
     */
    public Integer toInt(Object value) {
        return (Integer) value;
    }
    
    /**
     * Cast the given value as a Long.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as an Long
     */
    public Long toLong(Object value) {
    	return (Long) value;
    }
    
    /**
     * Cast the given value as a Float.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a Float
     */
    public Float toFloat(Object value) {
    	return (Float) value;
    }
    
    /**
     * Cast the given value as a Double.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a Double
     */
    public Double toDouble(Object value) {
    	return (Double) value;
    }
    
    /**
     * Cast the given value as a String.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a String
     */
    public String toString(Object value) {
    	return (String) value;
    }
    
    /**
     * Cast the given value as a Boolean.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a Boolean
     */
    public Boolean toBoolean(Object value) {
    	return (Boolean) value;
    }
    
    /**
     * Cast the given value as a {@link DateTime}.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a {@link DateTime}
     */
    public DateTime toDateTime(Object value) {
    	return (DateTime) value;
    }
    
    /**
     * Cast the given value as a {@link Date}.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a {@link Date}
     */
    public Date toDate(Object value) {
    	return (Date) value;
    }
    
    /**
     * Cast the given value as an int array.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as an int array
     */
    public int[] toIntArray(Object value) {
    	return (int[]) value;
    }
    
    /**
     * Cast the given value as a long array.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a long array
     */
    public long[] toLongArray(Object value) {
    	return (long[]) value;
    }
    
    /**
     * Cast the given value as a float array.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a float array
     */
    public float[] toFloatArray(Object value) {
    	return (float[]) value;
    }
    
    /**
     * Cast the given value as a double array.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a double array
     */
    public double[] toDoubleArray(Object value) {
    	return (double[]) value;
    }
    
    /**
     * Cast the given value as a String array.
     * 
     * @param value The value to cast
     * 
     * @return The value cast as a String array
     */
    public String[] toStringArray(Object value) {
    	return (String[]) value;
    }
}
