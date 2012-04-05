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
import java.util.Comparator;

import org.teatrove.trove.util.BeanComparator;

/**
 * Tea context that provides the ability to sort arrays.
 */
public class SortContext {

    /**
     * Sort the given array by using the given property to evaluate against
     * each element in the array comparing the resulting values. For example,
     * if the object array contained User instances that had a lastName
     * property, then you could sort the array by last name via:
     * <code>sort(users, 'lastName', false)</code>.
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
     * Sort the given array by using the given properties to evaluate against
     * each element in the array comparing the resulting values. If multiple
     * values match the first property, then the second property is used,
     * and so on. For example, if the object array contained User instances that 
     * had a lastName and firstName property, then you could sort the array by 
     * last name followed by first name via:
     * <code>sort(users, #('lastName', 'firstName'), #(false, false))</code>.
     * The array of reverse flags correspond to each property so that you could
     * sort one property ascendingly and another descendingly. The two arrays
     * <em>must</em> be the same length.
     * 
     * @param array The array to sort
     * @param properties The name of the properties on the elements to sort with
     * @param reverse The states of whether to reverse the orders
     */
    public void sort(Object[] array, String[] properties,  boolean[] reverse) {
        Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
            sort(array, arrayType, properties, reverse);
        }       
    }
    
    /**
     * Sort the given array of strings. If the ignoreCase flag is 
     * <code>true</code>, then case will not be used during sorting so that
     * 'abc' and 'ABC' are equivalent.
     * 
     * @param array The array of strings to sort
     * @param reverse The state of whether to reverse the sort
     * @param ignoreCase The state of whether to ignore the casing of strings
     */
    public void sort(String[] array, boolean reverse, boolean ignoreCase) {
    	StringComparator comparator = new StringComparator(reverse, ignoreCase);
    	Arrays.sort(array, comparator);
    }

    /**
     * Sort the given array according to the natural ordering of the array. If
     * the array contains string values, a standard string comparator is used.
     * If the array contains values that implement {@link Comparable}, then the
     * natural ordering of the implementation is used. Otherwise, the array is
     * not sorted.
     * 
     * @param array The array to sort
     * @param sortAscending The flag of whether to sort ascending or not
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void sort(Object[] array, boolean sortAscending) {
    	Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
        	Comparator comparator = null;
        	if (String.class.equals(arrayType)) {
        		comparator = new StringComparator(sortAscending, false);
        	} else if (Comparable.class.isAssignableFrom(arrayType)) {
        		comparator = new GenericComparator(sortAscending);
        	}
        	if (comparator != null) {
        		Arrays.sort(array, comparator);
        	} else {
        		System.err.println("Sorting arrays of type " + 
        		    arrayType.getName() + " is not supported, " + 
        			"must implement Comparable."
        		);
        	}
        } else {
       		System.err.println("Could not determine type of array to sort.");
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
    
    @SuppressWarnings("unchecked")
    private void sort(Object[] array, Class<?> arrayType,
                      String property, boolean reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        if (property != null && !property.equals("")) {
            comparator = comparator.orderBy(property);
        }       
        if (reverse) {            
            comparator = comparator.reverse();
        }
        Arrays.sort(array, comparator);
    }   
    
    @SuppressWarnings("unchecked")
    private void sort(Object[] array, Class<?> arrayType, 
                      String[] properties, boolean[] reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        for (int i = 0; i < properties.length; i++) {
            comparator = comparator.orderBy(properties[i]);
            if (reverse[i] == true) {
                comparator = comparator.reverse();
            }
        }
        Arrays.sort(array, comparator);
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
    
    @SuppressWarnings("rawtypes")
	public class GenericComparator implements Comparator<Comparable> {
    	
    	protected boolean sortAscending = true;
    	
    	public GenericComparator(boolean sortAscending) {
    		this.sortAscending = sortAscending;
    	}
    	
	    @SuppressWarnings("unchecked")
		public int compare(Comparable k1, Comparable k2) {
	    	if (k1 != null) {
				if (k2 != null) {
					int result = k1.compareTo(k2);
					return (sortAscending) ? result: -result;
				} else {
					return (sortAscending) ? 1: -1;
				}
	    	} else if (k2 != null) {
				return (sortAscending) ? -1: 1;		
			}
			return 0;
	    }
    }
    
    public class StringComparator implements Comparator<String> {
    	
    	protected boolean sortAscending = true;
    	protected boolean ignoreCase = true;
    	
    	public StringComparator(boolean sortAscending, boolean ignoreCase) {
    		this.sortAscending = sortAscending;
    		this.ignoreCase = ignoreCase;
    	}
    	
	    public int compare(String s1, String s2) {
			if (s1 != null) {
				if (s2 != null) {
					int flag = 0;
					if (ignoreCase) {
						flag = s1.compareToIgnoreCase(s2);
					} else {
						flag = s1.compareTo(s2);
					}
					if (flag > 0) {
					    return (sortAscending) ? 1: -1;
					}
					if (flag < 0) {
					    return (sortAscending) ? -1: 1;
					}
				} else {
					return (sortAscending) ? 1: -1;
				}
			} else if (s2 != null) {
				return (sortAscending) ? -1: 1;		
			}
			return 0;
	    }
    }
}
