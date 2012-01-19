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
 * @author Nick Hagan
 */
public class ArrayContext {

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#isArray(java.lang.Object)
     */
    public boolean isArray(Object array) {
        return (array != null && array.getClass().isArray());
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#getComponentType(java.lang.Object)
     */
    public Class<?> getComponentType(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return null;
        }

        return array.getClass().getComponentType();
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#getRootComponentType(java.lang.Object)
     */
    public Class<?> getRootComponentType(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return null;
        }

        Class<?> type = array.getClass();
        while (type.isArray()) {
            type = type.getComponentType();
        }

        return type;
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#getDimensions(java.lang.Object)
     */
    public int getDimensions(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return 0;
        }

        int dimensions = 0;
        Class<?> type = array.getClass();
        while (type.isArray()) {
            dimensions++;
            type = type.getComponentType();
        }

        return dimensions;
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#getArrayLength(java.lang.Object)
     */
    public int getArrayLength(Object array) {
        return Array.getLength(array);
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#getArrayElement(java.lang.Object, int)
     */
    public Object getArrayElement(Object array, int index) {
        return Array.get(array, index);
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#setArrayElement(java.lang.Object, int, java.lang.Object)
     */
    public void setArrayElement(Object array, int index, Object value) {
        Array.set(array, index, value);
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#createArray(java.lang.Class, int)
     */
    public Object createArray(Class<?> type, int length) {
        return Array.newInstance(type, length);
    }

    /*
     * (non-Javadoc)
     * @see disney.entrepot.context.impl.ArrayContext#createArray(java.lang.Class, int[])
     */
    public Object createArray(Class<?> type, int... dimensions) {
        return Array.newInstance(type, dimensions);
    }
    
    public double[] createDoubleArray(int length) {
        return new double[length];
    }
    
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

    public void setDoubleInArray(double[] doubleArray, double value, int index) {
        doubleArray[index] = value;
    }

    public int[] createIntArray(int length) {
        return new int[length];
    }

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

    public void setIntInArray(int[] intArray, int value, int index) {
        intArray[index] = value;
    }
}
