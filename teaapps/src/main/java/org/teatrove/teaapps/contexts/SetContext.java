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

import java.util.Set;

/**
 * Custom Tea context that provides access to {@link Set}s including creation,
 * modification, and helper methods.
 */
public class SetContext {

    /**
     * Add the given value to the given set.
     * 
     * @param <T> The component type of the set
     * 
     * @param set The set to add to
     * @param object The object to add
     *
     * @return <code>true</code> if the object was added and not previously
     *         contained in the set, <code>false</code> otherwise
     *         
     * @see Set#add(Object)
     */
	public <T> boolean add(Set<T> set, T object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.add(object);
	}

	/**
	 * Clear the given set of all elements.
	 * 
	 * @param set The set to clear
	 * 
	 * @see Set#clear()
	 */
	public void clear(Set<?> set) {
		if (set == null) {
			return;
		}
		
		set.clear();
	}

	/**
	 * Check whether the given set contains the given object instance.
	 * 
	 * @param set The set to check in
	 * @param object The object to check for
	 * 
	 * @return <code>true</code> if the object is contained in the list,
	 *         <code>false</code> otherwise
	 *         
	 * @see Set#contains(Object)
	 */
	public boolean contains(Set<?> set, Object object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.contains(object);
	}
	
	/**
	 * Check whether the two sets are equivalent to each other and that all
	 * elements of each set of equivalent.
	 * 
	 * @param set1 The first set to compare against
	 * @param set2 The second set to compare against
	 * 
	 * @return <code>true</code> if the sets are equal, 
	 *         <code>false</code> otherwise
	 *         
	 * @see Set#equals(Object)
	 */
	public boolean equals(Set<?> set1, Set<?> set2) {
		if (set1 == null || set2 == null) {
			return set1 == null && set2 == null;
		}
		
		return set1.equals(set2);
	}

	/**
	 * Check whether the given set is empty or not.
	 * 
	 * @param set The set to check
	 * 
	 * @return <code>true</code> if the set is empty,
	 *         <code>false</code> otherwise
	 *         
	 * @see Set#isEmpty()
	 */
	public boolean isEmpty(Set<?> set) {
		if (set == null) {
			return true;
		}
		
		return set.isEmpty();
	}

	/**
	 * Remove the given object from the given set.
	 * 
	 * @param set The set to remove from
	 * @param object The object to remove
	 * 
	 * @return <code>true</code> if the object was removed and contained in the
	 *         set, <code>false</code> otherwise
	 *         
	 * @see Set#remove(Object)
	 */
	public boolean remove(Set<?> set, Object object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.remove(object);
	}

	/**
	 * Get the size of the given set.
	 * 
	 * @param set The associated set
	 * 
	 * @return The size of the set
	 * 
	 * @see Set#size()
	 */
	public int size(Set<?> set) {
		if (set == null) {
			return -1;
		}
		
		return set.size();
	}

	/**
	 * Convert the given set to an array.
	 * 
	 * @param <T> The component type of the set
	 * 
	 * @param set The set to convert
	 * 
	 * @return An array representing the elements in the set
	 * 
	 * @see Set#toArray()
	 */
	@SuppressWarnings("unchecked")
    public <T> T[] toArray(Set<T> set) {
	    if (set == null) {
	        return null;
	    }
	    
		return (T[]) set.toArray(new Object[set.size()]);
	}

}
