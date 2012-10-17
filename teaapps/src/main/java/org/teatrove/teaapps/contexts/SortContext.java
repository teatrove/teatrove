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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.teatrove.trove.util.BeanComparator;

/**
 * Tea context that provides the ability to sort arrays and lists.
 */
public class SortContext {

    private static final Comparator<String> ISTRING_COMPARATOR_ASC =
        new StringComparator(true);
    
    private static final Comparator<String> ISTRING_COMPARATOR_DESC =
        Collections.reverseOrder(ISTRING_COMPARATOR_ASC);
    
    private static final Comparator<Object> TOSTRING_COMPARATOR_ASC =
        new ToStringComparator();
    
    private static final Comparator<Object> TOSTRING_COMPARATOR_DESC =
        Collections.reverseOrder(TOSTRING_COMPARATOR_ASC);
    
    /**
     * Sort the given array by using the given property to evaluate against
     * each element in the array comparing the resulting values. For example,
     * if the object array contained User instances that had a lastName
     * property, then you could sort the array by last name via:
     * <code>sort(users, 'lastName', false)</code>. Note that the determination
     * of the array type is based purely on the first non-<code>null</code> 
     * element in the array and all other instances in the array are expected to 
     * conform to the same type. Use 
     * {@link #sort(Object[], Class, String, boolean)} to explicitly provide the 
     * underlying array type.
     * 
     * @param array The array to sort
     * @param property The name of the property on the elements to sort against
     * @param reverse The state of whether to reverse the order
     */
    public void sort(Object[] array, String property, boolean reverse) {
        Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
            sort(array, arrayType, property, reverse);
        }
    }
    
    /**
     * Sort the given list by using the given property to evaluate against
     * each element in the list comparing the resulting values. For example,
     * if the object list contained User instances that had a lastName
     * property, then you could sort the list by last name via:
     * <code>sort(users, 'lastName', false)</code>. Note that the determination
     * of the list type is based purely on the first non-<code>null</code> 
     * element in the list and all other instances in the list are expected to 
     * conform to the same type. Use 
     * {@link #sort(List, Class, String, boolean)} to explicitly provide the 
     * underlying type.
     * 
     * @param list The list to sort
     * @param property The name of the property on the elements to sort against
     * @param reverse The state of whether to reverse the order
     */
    public void sort(List<?> list, String property, boolean reverse) {
        Class<?> type = getObjectClass(list);
        if (type != null) {
            sort(list, type, property, reverse);
        }
    }
    
    /**
     * Sort the given array by using the given property to evaluate against
     * each element in the array comparing the resulting values. For example,
     * if the object array contained User instances that had a lastName
     * property, then you could sort the array by last name via:
     * <code>sort(users, 'lastName', false)</code>. This ensures that each 
     * element in the array is of the given type and uses the given type to
     * perform the property lookup and evaluation.
     * 
     * @param array The array to sort
     * @param arrayType The type of elements in the array
     * @param property The name of the property on the elements to sort against
     * @param reverse The state of whether to reverse the order
     */
    @SuppressWarnings("unchecked")
    public void sort(Object[] array, Class<?> arrayType,
                     String property, boolean reverse) {
        BeanComparator comparator = 
            newBeanComparator(arrayType, property, reverse);
        Arrays.sort(array, comparator);
    }
    
    /**
     * Sort the given list by using the given property to evaluate against
     * each element in the list comparing the resulting values. For example,
     * if the object list contained User instances that had a lastName
     * property, then you could sort the list by last name via:
     * <code>sort(users, 'lastName', false)</code>. This ensures that each 
     * element in the list is of the given type and uses the given type to
     * perform the property lookup and evaluation.
     * 
     * @param list The list to sort
     * @param type The type of elements in the array
     * @param property The name of the property on the elements to sort against
     * @param reverse The state of whether to reverse the order
     */
    @SuppressWarnings("unchecked")
    public void sort(List<?> list, Class<?> type,
                     String property, boolean reverse) {
        BeanComparator comparator = 
            newBeanComparator(type, property, reverse);
        Collections.sort(list, comparator);
    }
    
    /**
     * Sort the given array by using the given properties to evaluate against
     * each element in the array comparing the resulting values. If multiple
     * values match the first property, then the second property is used,
     * and so on. For example, if the object array contained User instances that 
     * had a lastName and firstName property, then you could sort the array by 
     * last name followed by first name via:
     * <code>sort(users, #('lastName', 'firstName'), #(false, false))</code>.
     * The array of reverse flags correspond to each property so that you could
     * sort one property ascendingly and another descendingly. The two arrays
     * <em>must</em> be the same length.  Note that the determination
     * of the array type is based purely on the first non-<code>null</code> 
     * element in the array and all other instances in the array are expected to 
     * conform to the same type. Use 
     * {@link #sort(Object[], Class, String[], boolean[])} to explicitly provide 
     * the underlying array type.
     * 
     * @param array The array to sort
     * @param properties The name of the properties on the elements to sort with
     * @param reverse The states of whether to reverse the orders
     */
    public void sort(Object[] array, String[] properties, boolean[] reverse) {
        Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
            sort(array, arrayType, properties, reverse);
        }       
    }
    
    /**
     * Sort the given list by using the given properties to evaluate against
     * each element in the list comparing the resulting values. If multiple
     * values match the first property, then the second property is used,
     * and so on. For example, if the object list contained User instances that 
     * had a lastName and firstName property, then you could sort the list by 
     * last name followed by first name via:
     * <code>sort(users, #('lastName', 'firstName'), #(false, false))</code>.
     * The array of reverse flags correspond to each property so that you could
     * sort one property ascendingly and another descendingly. The two arrays
     * <em>must</em> be the same length. Note that the determination
     * of the list type is based purely on the first non-<code>null</code> 
     * element in the list and all other instances in the list are expected to 
     * conform to the same type. Use 
     * {@link #sort(List, Class, String[], boolean[])} to explicitly provide 
     * the underlying list type.
     * 
     * @param list The list to sort
     * @param properties The name of the properties on the elements to sort with
     * @param reverse The states of whether to reverse the orders
     */
    public void sort(List<?> list, String[] properties, boolean[] reverse) {
        Class<?> type = getObjectClass(list);
        if (type != null) {
            sort(list, type, properties, reverse);
        }
    }

    /**
     * Sort the given array by using the given properties to evaluate against
     * each element in the array comparing the resulting values. If multiple
     * values match the first property, then the second property is used,
     * and so on. For example, if the object array contained User instances that 
     * had a lastName and firstName property, then you could sort the array by 
     * last name followed by first name via:
     * <code>sort(users, #('lastName', 'firstName'), #(false, false))</code>.
     * The array of reverse flags correspond to each property so that you could
     * sort one property ascendingly and another descendingly. The two arrays
     * <em>must</em> be the same length. This also ensures that each element in 
     * the array is of the given type and uses the given type to perform the 
     * property lookup and evaluation.
     * 
     * @param array The array to sort
     * @param arrayType The type of elements in the array
     * @param properties The name of the properties on the elements to sort with
     * @param reverse The states of whether to reverse the orders
     */
    @SuppressWarnings("unchecked")
    public void sort(Object[] array, Class<?> arrayType, 
                     String[] properties, boolean[] reverse) {
        BeanComparator comparator = 
            newBeanComparator(arrayType, properties, reverse);
        Arrays.sort(array, comparator);
    }
    
    /**
     * Sort the given list by using the given properties to evaluate against
     * each element in the list comparing the resulting values. If multiple
     * values match the first property, then the second property is used,
     * and so on. For example, if the object list contained User instances that 
     * had a lastName and firstName property, then you could sort the list by 
     * last name followed by first name via:
     * <code>sort(users, #('lastName', 'firstName'), #(false, false))</code>.
     * The array of reverse flags correspond to each property so that you could
     * sort one property ascendingly and another descendingly. The two arrays
     * <em>must</em> be the same length. This also ensures that each element in 
     * the list is of the given type and uses the given type to perform the 
     * property lookup and evaluation.
     * 
     * @param array The array to sort
     * @param type The type of elements in the array
     * @param properties The name of the properties on the elements to sort with
     * @param reverse The states of whether to reverse the orders
     */
    @SuppressWarnings("unchecked")
    public void sort(List<?> list, Class<?> type,
                     String[] properties, boolean[] reverse) {
        BeanComparator comparator = 
            newBeanComparator(type, properties, reverse);
        Collections.sort(list, comparator);
    }
    
    /**
     * Sort the given array of strings naturally. If the ignoreCase flag is 
     * <code>true</code>, then case will not be used during sorting so that
     * 'abc' and 'ABC' are equivalent.
     * 
     * @param array The array of strings to sort
     * @param reverse The state of whether to reverse the sort
     * @param ignoreCase The state of whether to ignore the casing of strings
     */
    public void sort(String[] array, boolean reverse, boolean ignoreCase) {
    	if (ignoreCase) {
    	    if (reverse) { Arrays.sort(array, ISTRING_COMPARATOR_DESC); }
            else { Arrays.sort(array, ISTRING_COMPARATOR_ASC); }
    	}
    	else {
    	    if (reverse) { Arrays.sort(array, Collections.reverseOrder()); }
            else { Arrays.sort(array); }
    	}
    }

    /**
     * Sort the given array according to either the natural ordering of the 
     * array or using a generic string comparison by converting each element to
     * its string format via {@link Object#toString() toString}.
     * 
     * @param array The array to sort
     * @param reverse The state of whether to reverse the sort
     */
	public void sort(Object[] array, boolean reverse) {
        
        // handle purely comparable arrays
        if (array instanceof Comparable[]) {
            if (reverse) {
                Arrays.sort(array, Collections.reverseOrder());
            }
            else {
                Arrays.sort(array);
            }
        }
        
        // otherwise, use general toString comparator
        else {
            if (reverse) {
                Arrays.sort(array, TOSTRING_COMPARATOR_DESC);
            }
            else {
                Arrays.sort(array, TOSTRING_COMPARATOR_ASC);
            }
        }
    }
    
    /**
     * Sort the given list according to either the natural ordering of the 
     * elements in the list or using a generic string comparison by converting 
     * each element to its string format via {@link Object#toString() toString}.
     * If all elements in the list are {@link Comparable comparable}, then the
     * natural ordering will be used. Otherwise, the generic comparison will be
     * performed.
     * 
     * @param list The list to sort
     * @param reverse The state of whether to reverse the sort
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> void sort(List<T> list, boolean reverse) {
        Class<?> type = getObjectClass(list);
        if (Comparable.class.isAssignableFrom(type)) {
            if (reverse) {
                Collections.sort(list, Collections.reverseOrder());
            }
            else { Collections.sort((List) list); }
        }
        else {
            if (reverse) {
                Collections.sort(list, TOSTRING_COMPARATOR_DESC);
            }
            else {
                Collections.sort(list, TOSTRING_COMPARATOR_ASC);
            }
        }
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(Object[])
     */
    public void sortAscending(Object[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given list naturally.
     * 
     * @param list The list to sort
     * 
     * @see Collections#sort(List)
     */
    public <T extends Comparable<? super T>> void sortAscending(List<T> list) {
        Collections.sort(list);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(int[])
     */
    public void sortAscending(int[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(double[])
     */
    public void sortAscending(double[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(float[])
     */
    public void sortAscending(float[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(byte[])
     */
    public void sortAscending(byte[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(short[])
     */
    public void sortAscending(short[] array) {
    	Arrays.sort(array);
    }
    
    /**
     * Sort the given array naturally.
     * 
     * @param array The array to sort
     * 
     * @see Arrays#sort(long[])
     */
    public void sortAscending(long[] array) {
    	Arrays.sort(array);
    }

    private static BeanComparator newBeanComparator(Class<?> type) {
        BeanComparator comparator = BeanComparator.forClass(type);
        comparator.using(TOSTRING_COMPARATOR_ASC);
        return comparator;
    }
    
    private static BeanComparator 
    newBeanComparator(Class<?> type, String property, boolean reverse) {
       
        BeanComparator comparator = newBeanComparator(type);
     
        if (property != null && !property.equals("")) {
            comparator = comparator.orderBy(property);
        }
        
        if (reverse) {
            comparator = comparator.reverse();
        }
        
        return comparator;
    }

    private static BeanComparator 
    newBeanComparator(Class<?> type, String[] properties, boolean[] reverse) {
        
        BeanComparator comparator = newBeanComparator(type);
        
        for (int i = 0; i < properties.length; i++) {
            comparator = comparator.orderBy(properties[i]);
            if (reverse[i]) {
                comparator = comparator.reverse();
            }
        }

        return comparator;
    }
    
    private Class<?> getObjectClass(Object[] array) {
        Class<?> result = null;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] != null) {
                    result = array[i].getClass();
                    break;
                }
            }
        }
        return result;
    }
    
    private Class<?> getObjectClass(List<?> list) {
        Class<?> result = null;
        if (list != null) {
            for (Object element : list) {
                if (element != null) {
                    result = element.getClass();
                    break;
                }
            }
        }

        return result;
    }
    
    /**
     * Special comparator used to support comparing strings while also ignoring
     * case.
     */
    public static class StringComparator implements Comparator<String> {
    	
    	protected boolean ignoreCase;
    	
    	public StringComparator(boolean ignoreCase) {
    		this.ignoreCase = ignoreCase;
    	}
    	
	    public int compare(String s1, String s2) {
	        if (s1 == null || s2 == null) {
	            if (s1 == null && s2 == null) { return 0; }
	            else if (s1 == null) { return -1; }
	            else { return 1; }
	        }
	        else if (ignoreCase) { return s1.compareToIgnoreCase(s2); }
	        else { return s1.compareTo(s2); }
	    }
    }

    /**
     * Special comparator used to allow non-Comparable properties to be
     * converted to strings and sorted accordingly.
     */
    public static class ToStringComparator implements Comparator<Object> {

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public int compare(Object object1, Object object2) {
            if (object1 instanceof Comparable &&
                object2 instanceof Comparable) {
                return ((Comparable) object1).compareTo(object2);
            }
            else if (object1 == null && object2 == null) {
                return 0;
            }
            else if (object1 != null && object2 != null) {
                String string1 = object1.toString();
                String string2 = object2.toString();
                return compare(string1, string2);
            }
            else if (object1 == null) { return 1; }
            else { return -1; }
        }
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Object[], String, boolean)
     */
    //@Deprecated
    public void sortArray(Object[] array, String property, String reverse) {
        sort(array, property, Boolean.parseBoolean(reverse));
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Object[], String, boolean)
     */
    //@Deprecated
    public void sortArray(Object[] array, String property, boolean reverse) {
        sort(array, property, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Object[], Class, String, boolean)
     */
    //@Deprecated
    public void sortArray(Object[] array, Class<?> arrayType, String property,
                          boolean reverse) {
        sort(array, arrayType, property, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Object[], Class, String[], boolean[])
     */
    //@Deprecated
    public void sortArray(Object[] array, 
                          String[] properties, boolean[] reverse) {
        sort(array, properties, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Object[], Class, String[], boolean[])
     */
    //@Deprecated
    public void sortArray(Object[] array, Class<?> arrayType,
                          String[] properties, boolean[] reverse) {
        sort(array, arrayType, properties, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(String[], boolean)
     */
    //@Deprecated
    public void sortArray(String[] array, boolean reverse) {
        sort(array, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(Integer[], boolean)
     */
    //@Deprecated
    public void sortArray(Integer[] array, boolean reverse) {
        sort(array, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(int[], boolean)
     */
    //@Deprecated
    public void sortArray(int[] array, boolean reverse) {
        sortAscending(array);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, boolean)
     */
    //@Deprecated
    public void sortList(List<?> list, boolean reverse) {
        sort(list, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, String, boolean)
     */
    //@Deprecated
    public void sortList(List<?> list, String property, boolean reverse) {
        sort(list, property, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, String, boolean)
     */
    //@Deprecated
    public void sortList(List<?> list, String property, String reverse) {
        sort(list, property, Boolean.parseBoolean(reverse));
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, String[], boolean[])
     */
    //@Deprecated
    public void sortList(List<?> list, String[] properties, boolean[] reverse) {
        sort(list, properties, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, Class, String, boolean)
     */
    //@Deprecated
    public void sortList(List<?> list, Class<?> type,
                         String property, boolean reverse) {
        sort(list, type, property, reverse);
    }
    
    /**
     * Deprecated.
     * 
     * @see #sort(List, Class, String[], boolean[])
     */
    //@Deprecated
    public void sortList(List<?> list, Class<?> type, 
                         String[] properties, boolean[] reverse) {
        sort(list, type, properties, reverse);
    }
}
