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

import java.util.Arrays;

/**
 * MultiKey allows arrays and arrays of arrays to be used as hashtable keys.
 * Hashcode computation and equality tests will fully recurse into the array
 * elements. MultiKey can be used in conjunction with {@link Depot} for
 * caching against complex keys.
 *
 * @author Brian S O'Neill
 * @see Pair
 */
public class MultiKey implements java.io.Serializable {
    /**
     * Computes an object hashcode for any kind of object including null,
     * arrays, and arrays of arrays.
     */    
    static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }

        Class clazz = obj.getClass();

        //if (!clazz.isArray()) {
            //return obj.hashCode();
        //}
        
        // Compute hashcode of array.

        int hash = clazz.hashCode();
        
        if (obj instanceof int[]) {
            int[] array = (int[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + hashCode(array[i]);
            }
        }
        else if (obj instanceof float[]) {
            float[] array = (float[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + Float.floatToIntBits(array[i]);
            }
        }
        else if (obj instanceof long[]) {
            long[] array = (long[])obj;
            for (int i = array.length; --i >= 0; ) {
                long value = array[i];
                hash = hash * 31 + (int)(value ^ value >>> 32);
            }
        }
        else if (obj instanceof double[]) {
            double[] array = (double[])obj;
            for (int i = array.length; --i >= 0; ) {
                long value = Double.doubleToLongBits(array[i]);
                hash = hash * 31 + (int)(value ^ value >>> 32);
            }
        }
        else if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof char[]) {
            char[] array = (char[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + (array[i] ? 1 : 0);
            }
        }
        else if (obj instanceof short[]) {
            short[] array = (short[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else {
            hash = obj.hashCode();
        }
        
        
        return hash;
    }
    
    /**
     * Performans an object equality for any kind of objects including null,
     * arrays, and arrays of arrays.
     */    
    static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        else if (obj1 == null || obj2 == null) {
            return false;
        }

        Class clazz1 = obj1.getClass();

        //if (!(clazz1.isArray())) {
            //return obj1.equals(obj2);
        //}
        
        if (clazz1 != obj2.getClass()) {
            return false;
        }
        
        // Perform array equality test.
        if (obj1 instanceof int[]) {
            return Arrays.equals((int[])obj1, (int[])obj2);
        }
        else if (obj1 instanceof Object[]) {
            // Don't use Arrays.equals for objects since it doesn't
            // recurse into arrays of arrays.
            Object[] array1 = (Object[])obj1;
            Object[] array2 = (Object[])obj2;
            
            int i;
            if ((i = array1.length) != array2.length) {
                return false;
            }
            
            while (--i >= 0) {
                if (!equals(array1[i], array2[i])) {
                    return false;
                }
            }
            
            return true;
        }
        else if (obj1 instanceof float[]) {
            return Arrays.equals((float[])obj1, (float[])obj2);
        }
        else if (obj1 instanceof long[]) {
            return Arrays.equals((long[])obj1, (long[])obj2);
        }
        else if (obj1 instanceof double[]) {
            return Arrays.equals((double[])obj1, (double[])obj2);
        }
        else if (obj1 instanceof byte[]) {
            return Arrays.equals((byte[])obj1, (byte[])obj2);
        }
        else if (obj1 instanceof char[]) {
            return Arrays.equals((char[])obj1, (char[])obj2);
        }
        else if (obj1 instanceof boolean[]) {
            return Arrays.equals((boolean[])obj1, (boolean[])obj2);
        }
        else if (obj1 instanceof short[]) {
            return Arrays.equals((short[])obj1, (short[])obj2);
        }
        else {
            return obj1.equals(obj2);
        }
    }

    private final Object mComponent;
    private final int mHash;

    /**
     * Contruct a new MultiKey against a component which may be any kind of
     * object including an array, or an array of arrays, or null.
     */
    public MultiKey(Object component) {
        mComponent = component;
        mHash = MultiKey.hashCode(component);
    }

    /**
     * Returns the original component used to construct this MultiKey.
     */
    public Object getComponent() {
        return mComponent;
    }

    public int hashCode() {
        return mHash;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        else if (other instanceof MultiKey) {
            MultiKey key = (MultiKey)other;
            return MultiKey.equals(mComponent, key.mComponent);
        }
        else {
            return false;
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        append(buf, mComponent);
        return buf.toString();
    }

    private void append(StringBuffer buf, Object obj) {
        if (obj == null) {
            buf.append("null");
            return;
        }

        if (!obj.getClass().isArray()) {
            buf.append(obj);
            return;
        }

        buf.append('[');

        if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                append(buf, array[i]);
            }
        }
        else if (obj instanceof int[]) {
            int[] array = (int[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof float[]) {
            float[] array = (float[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof long[]) {
            long[] array = (long[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof double[]) {
            double[] array = (double[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof char[]) {
            char[] array = (char[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof short[]) {
            short[] array = (short[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else {
            buf.append(obj);
        }

        buf.append(']');
    }
}
