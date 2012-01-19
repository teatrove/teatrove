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

import org.teatrove.trove.util.BeanComparator;

import java.util.Arrays;
import java.util.Comparator;

public class SortContext {
    
    public static void sort(Object[] array, String onColumn, boolean reverse) {
        Class objClass = getObjectClass(array);
        if (objClass != null) {
            sort(array, objClass, onColumn, reverse);
        }
    }
    
    public static void sort(Object[] array, String[] onColumns,  boolean[] reverse) {
        Class arrayType = getObjectClass(array);
        if (arrayType != null) {
            sort(array, arrayType, onColumns, reverse);
        }       
    }
    
    public static void sort(String[] array, boolean reverse, boolean ignoreCase) {
    	StringComparator comparator = new StringComparator(reverse, ignoreCase);
    	Arrays.sort(array, comparator);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void sort(Object[] array, boolean sortAscending) {
    	Class<?> arrayType = getObjectClass(array);
        if (arrayType != null) {
        	Comparator comparator = null;
        	if (arrayType == String.class) {
        		comparator = new StringComparator(sortAscending, false);
        	} else if (Comparable.class.isAssignableFrom(arrayType)) {
        		comparator = new GenericComparator(sortAscending);
        	}
        	if (comparator != null) {
        		Arrays.sort(array, comparator);
        	} else {
        		System.err.println("Sorting arrays of type " + arrayType.getName() + " is not supported, " + 
        				"must implement Comparable.");
        	}
        } else {
       		System.err.println("Could not determine type of array to sort.");
        }
    }
    
    public static void sortAscending(Object[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(int[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(double[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(float[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(byte[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(short[] array) {
    	Arrays.sort(array);
    }
    
    public static void sortAscending(long[] array) {
    	Arrays.sort(array);
    }
    
    private static void sort(Object[] array, Class arrayType, String onColumn, boolean reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        if (onColumn != null && !onColumn.equals("")) {
            comparator = comparator.orderBy(onColumn);
        }       
        if (reverse) {            
            comparator = comparator.reverse();
        }
        Arrays.sort(array, comparator);
    }   
    
    private static void sort(Object[] array, Class arrayType, String[] onColumns, boolean[] reverse) {
        BeanComparator comparator = BeanComparator.forClass(arrayType);
        for (int i = 0; i < onColumns.length; i++) {
            comparator = comparator.orderBy(onColumns[i]);
            if (reverse[i] == true) {
                comparator = comparator.reverse();
            }
        }
        Arrays.sort(array, comparator);
    }

    private static Class getObjectClass(Object[] array) {
        Class result = null;
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
	public static class GenericComparator implements Comparator<Comparable> {
    	
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
    
    public static class StringComparator implements Comparator<String> {
    	
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
    
    
    /*
    public static void main(String[] args) {
    	Integer[] test1 = new Integer[] { 4, 7, 3, 5, 9, 8 };
    	print(test1);
    	sort(test1, false);
    	print(test1);
    	
    	String[] test2 = new String[] { "Jappy",
    			"Vic",
    			"Chris",
    			"Erik",
    			"Matt" };
    	print(test2);
    	sort(test2, false);
    	print(test2);
    }
    
    private static void print(Object[] array) {
    	StringBuilder builder = new StringBuilder();
    	builder.append("{ ");
    	for (int i=0; i < array.length; i++) {
    		builder.append(array[i]);
    		if (i < array.length - 1) {
    			builder.append(", ");
    		}
    	}
    	builder.append(" }");
    	System.out.println(builder.toString());
    }*/
}
