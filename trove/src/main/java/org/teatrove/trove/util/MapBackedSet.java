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

import java.util.*;

/**
 * A set implementation that is backed by any map.
 *
 * @author Brian S O'Neill
 */
public class MapBackedSet<T> extends AbstractSet<T> implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    protected final Map<T, Object> mMap;

    /**
     * @param map The map to back this set.
     */
    public MapBackedSet(Map<T, Object> map) {
        mMap = map;
    }

    /**
     * Returns an iterator over the elements in this set. The elements
     * are returned in the order determined by the backing map.
     *
     * @return an Iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    @Override
    public Iterator<T> iterator() {
        return mMap.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality).
     */
    @Override
    public int size() {
        return mMap.size();
    }

    /**
     * Returns true if this set contains no elements.
     *
     * @return true if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    /**
     * Returns true if this set contains the specified element.
     *
     * @return true if this set contains the specified element.
     */
    @Override
    public boolean contains(Object obj) {
        return mMap.containsKey(obj);
    }

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *
     * @param obj element to be added to this set.
     * @return true if the set did not already contain the specified element.
     */
    @Override
    public boolean add(T obj) {
        return mMap.put(obj, PRESENT) == null;
    }

    /**
     * Removes the given element from this set if it is present.
     *
     * @param obj object to be removed from this set, if present.
     * @return true if the set contained the specified element.
     */
    @Override
    public boolean remove(Object obj) {
        return mMap.remove(obj) == PRESENT;
    }

    /**
     * Removes all of the elements from this set.
     */
    @Override
    public void clear() {
        mMap.clear();
    }
}
