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

import java.lang.reflect.Array;

/**
 * The array context is a Tea context that provides templates access to array
 * data and the creation of arrays dynamically.
 * 
 * @author Nicholas Hagen
 */
public class ArrayContext {
    
    /**
     * Clone the given array into a new instance of the same type and 
     * dimensions. The values are directly copied by memory, so this is not
     * a deep clone operation.  However, manipulation of the contents of the
     * array will not impact the given array.
     * 
     * @param array The array to clone
     * 
     * @return The new cloned array
     */
    public Object[] cloneArray(Object[] array) {
        Class<?> clazz = array.getClass().getComponentType();
        Object newArray =  Array.newInstance(clazz, array.length);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return (Object[]) newArray;
    }
    
    /**
     * Merge the two arrays by creating a new array that contains all items from
     * array1 followed by all items from array2.  Note that the arrays are
     * purely concatenated and any duplicates will remain.  If either array is
     * <code>null</code> or empty, then <code>null</code> is returned.  If the
     * class types of the arrays are not equal, then <code>null</code> is
     * returned.
     * 
     * @param array1 The first array
     * @param array2 The second array
     * 
     * @return The concatenation of array1 and array2 together
     */
    public Object[] mergeArrays(Object[] array1, Object[] array2) {
        Object[] retval = null;

        if (array1 == null || array1.length == 0 ||
            array2 == null || array2.length == 0) {
            return retval;
        }

        Class<?> cls1 = array1[0].getClass();
        Class<?> cls2 = array2[0].getClass();

        if (!cls1.equals(cls2)) {
            return retval;
        }

        retval = (Object[]) Array.newInstance(cls1, array1.length + array2.length);

        System.arraycopy(array1, 0, retval, 0, array1.length); 
        System.arraycopy(array2, 0, retval, array1.length, array2.length);

        return retval;
    }
    
    /**
     * Determine whether the provided object is an array or not. This returns
     * <code>true</code> if the object is non-<code>null</code> and the class
     * is an array (see {@link Class#isArray()}). Otherwise, it returns
     * <code>false</code>.
     * 
     * @param object The object to check as an array
     * 
     * @return The boolean state of whether the object is an array
     */
    public boolean isArray(Object object) {
        return (object != null && object.getClass().isArray());
    }

    /**
     * Get the component type of the given array. If the given array is not
     * actually an array, then <code>null</code> is returned. Otherwise, the
     * associated component type is returned.
     * 
     * @param array The array to get the component type from
     * 
     * @return The component type of the array or <code>null</code>
     * 
     * @see #isArray(Object)
     * @see Class#getComponentType()
     */
    public Class<?> getComponentType(Object array) {
        if (!isArray(array)) { return null; }
        return array.getClass().getComponentType();
    }

    /**
     * Get the root component type of the given array. The root component type
     * is the actual underlying type of the array. This is especially useful
     * in multi-dimensional arrays since {@link #getComponentType(Object)} would
     * return <code>Type[]</code> rather than <code>Type</code>.
     * 
     * @param array The array to get the root component type from
     * 
     * @return The root component type or <code>null</code>
     * 
     * @see #getComponentType(Object)
     */
    public Class<?> getRootComponentType(Object array) {
        if (!isArray(array)) { return null; }

        // search for root type
        Class<?> type = array.getClass();
        while (type.isArray()) {
            type = type.getComponentType();
        }

        return type;
    }

    /**
     * Get the number of dimensions of the given array. If the array is not
     * actually an array, then <code>0</code> is returned. Otherwise, if the
     * array is single-dimension, then <code>1</code> is returned. If the array
     * is multi-dimensional, the total number of dimensions is returned. For
     * example, <code>Type[][][]</code> would return <code>3</code>.
     * 
     * @param array The array to get the number of dimensions from
     * 
     * @return The number of dimensions in the given array
     */
    public int getDimensions(Object array) {
        if (!isArray(array)) { return 0; }

        int dimensions = 0;
        Class<?> type = array.getClass();
        while (type.isArray()) {
            dimensions++;
            type = type.getComponentType();
        }

        return dimensions;
    }

    /**
     * Get the length of the given array. This is a helper method for 
     * {@link Array#getLength(Object)}. If the array is not actually an array,
     * then {@link IllegalArgumentException} is thrown.
     * 
     * @param array The array to get the length from
     * 
     * @return The length of the array
     */
    public int getArrayLength(Object array) {
        if (!isArray(array)) { return -1; }
        return Array.getLength(array);
    }

    /**
     * Get the element at the given index from the given array. This is a helper
     * method for {@link Array#get(Object, int)}. If the given array is not an
     * array, then {@link IllegalArgumentException} is thrown. If the given
     * index is out of bounds, then {@link ArrayIndexOutOfBoundsException} is
     * thrown. Note that the returned value may be wrapped if the component type
     * is a primitive.
     *  
     * @param array The array to get the element from
     * @param index The index of the array to retrieve
     * 
     * @return The associated element at the given index
     */
    public Object getArrayElement(Object array, int index) {
        return Array.get(array, index);
    }

    /**
     * Set the element at the given index in the given array. This is a helper
     * method for {@link Array#set(Object, int, Object)}. If the given array is
     * not an array, then {@link IllegalArgumentException} is thrown. If the
     * given index is out of bounds, then {@link ArrayIndexOutOfBoundsException}
     * is thrown. Note that if the component type of the array is a primitive,
     * then the value will be unwrapped. This may result in a
     * {@link NullPointerException} being thrown.
     * 
     * @param array The array to set the element to
     * @param index The index of the array to set
     * @param value The value of the array to set
     */
    public void setArrayElement(Object array, int index, Object value) {
        Array.set(array, index, value);
    }

    /**
     * Create an array of the specified component type and size. This is the
     * helper method for {@link Array#newInstance(Class, int)}.
     * 
     * @param type The component type of the array
     * @param length The size of the array to create
     * 
     * @return The resulting array
     */
    public Object createArray(Class<?> type, int length) {
        return Array.newInstance(type, length);
    }

    /**
     * Create an array of the specified component type and dimension lengths. 
     * This is the helper method for {@link Array#newInstance(Class, int[])}.
     * 
     * @param type The component type of the array
     * @param dimensions The size of each dimension in the array to create
     * 
     * @return The resulting array
     */
    public Object createArray(Class<?> type, int... dimensions) {
        return Array.newInstance(type, dimensions);
    }
    
    /**
     * Create an array of <code>double</code> values of the given length.
     * 
     * @param length The size of the array to create
     * 
     * @return The created double array
     */
    public double[] createDoubleArray(int length) {
        return new double[length];
    }
    
    /**
     * Create an array of <code>double</code> values from the given array of
     * {@link Double} values. Each value in the given array will be unwrapped to
     * its primitive form. If any values within the given array are
     * <code>null</code>, then they will be ignored and the resulting array will
     * be smaller than the given array.
     * 
     * @param doubles The array of doubles to convert
     * 
     * @return The array of primitive doubles
     */
    public double[] createDoubleArray(Double[] doubles) {
        double[] result;
        if (doubles != null) {
            int count = 0;
            int inputLength = doubles.length;
            double[] tempResult = new double[inputLength];
            for (int i = 0; i < inputLength; i++) {
                if (doubles[i] != null) {
                    tempResult[count] = doubles[i].doubleValue();
                    count++;
                }
            }
            result = tempResult;
            if (count != inputLength) {
                result = new double[count];
                System.arraycopy(tempResult, 0, result, 0, count);
            }
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Set the primitive <code>double</code> value in the given array at the
     * given index.
     * 
     * @param doubleArray The array of doubles to set in
     * @param value The value to set
     * @param index The index to set at
     */
    public void setDoubleInArray(double[] doubleArray, double value, int index) {
        doubleArray[index] = value;
    }

    /**
     * Create an array of <code>int</code> values of the given length.
     * 
     * @param length The size of the array to create
     * 
     * @return The created int array
     */
    public int[] createIntArray(int length) {
        return new int[length];
    }

    /**
     * Create an array of <code>int</code> values from the given array of
     * {@link Integer} values. Each value in the given array will be unwrapped 
     * to its primitive form. If any values within the given array are
     * <code>null</code>, then they will be ignored and the resulting array will
     * be smaller than the given array.
     * 
     * @param integers The array of integers to convert
     * 
     * @return The array of primitive ints
     */
    public int[] createIntArray(Integer[] integers) {
        int[] result;
        if (integers != null) {
            int count = 0;
            int inputLength = integers.length;
            int[] tempResult = new int[inputLength];
            for (int i = 0; i < inputLength; i++) {
                if (integers[i] != null) {
                    tempResult[count] = integers[i].intValue();
                    count++;
                }
            }
            result = tempResult;
            if (count != inputLength) {
                result = new int[count];
                System.arraycopy(tempResult, 0, result, 0, count);
            }
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Set the primitive <code>int</code> value in the given array at the
     * given index.
     * 
     * @param intArray The array of ints to set in
     * @param value The value to set
     * @param index The index to set at
     */
    public void setIntInArray(int[] intArray, int value, int index) {
        intArray[index] = value;
    }
    
    /**
     * Join the contents of the given array separated by the given separator.
     * For example, the array [a, b, c] with separator ' ' would result in:
     * <code>a b c</code>.
     * 
     * @param array  The array to join
     * @param separator The separator between values
     * 
     * @return The resulting string
     */
    public String join(int[] array, String separator) {
        int size = array.length;
        StringBuilder buffer = new StringBuilder(512);
        for (int i = 0; i < size; i++) {
            int item = array[i];
            if (i > 0) { buffer.append(separator); }
            buffer.append(item);
        }
        
        return buffer.toString();
    }
    
    /**
     * Join the contents of the given array separated by the given separator.
     * For example, the array [a, b, c] with separator ' ' would result in:
     * <code>a b c</code>.
     * 
     * @param array  The array to join
     * @param separator The separator between values
     * 
     * @return The resulting string
     */
    public String join(long[] array, String separator) {
        int size = array.length;
        StringBuilder buffer = new StringBuilder(512);
        for (int i = 0; i < size; i++) {
            long item = array[i];
            if (i > 0) { buffer.append(separator); }
            buffer.append(item);
        }
        
        return buffer.toString();
    }
    
    /**
     * Join the contents of the given array separated by the given separator.
     * For example, the array [a, b, c] with separator ' ' would result in:
     * <code>a b c</code>.
     * 
     * @param array  The array to join
     * @param separator The separator between values
     * 
     * @return The resulting string
     */
    public String join(double[] array, String separator) {
        int size = array.length;
        StringBuilder buffer = new StringBuilder(512);
        for (int i = 0; i < size; i++) {
            double item = array[i];
            if (i > 0) { buffer.append(separator); }
            buffer.append(item);
        }
        
        return buffer.toString();
    }
    
    /**
     * Join the contents of the given array separated by the given separator.
     * For example, the array [a, b, c] with separator ' ' would result in:
     * <code>a b c</code>.
     * 
     * @param array  The array to join
     * @param separator The separator between values
     * 
     * @return The resulting string
     */
    public String join(Object[] array, String separator) {
        int size = array.length;
        StringBuilder buffer = new StringBuilder(512);
        for (int i = 0; i < size; i++) {
            Object item = array[i];
            if (i > 0) { buffer.append(separator); }
            buffer.append(item.toString());
        }
        
        return buffer.toString();
    }
}
