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

package org.teatrove.trove.persist;

import java.util.Comparator;
import java.io.*;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 * @see PersistentSortedMapView
 */
public interface PersistentSortedMapKernel {
    final Object NO_KEY = new String("no key");

    Comparator comparator();

    int size() throws IOException;

    boolean isEmpty() throws IOException;

    boolean containsKey(Object key) throws IOException;

    boolean containsValue(Object value) throws IOException;

    Object get(Object key) throws IOException;

    PersistentMap.Entry getEntry(Object key) throws IOException;

    Object put(Object key, Object value) throws IOException;

    Object remove(Object key) throws IOException;

    /**
     * Returns NO_KEY if map is empty.
     */
    Object firstKey() throws IOException;

    /**
     * Returns NO_KEY if map is empty.
     */
    Object lastKey() throws IOException;

    /**
     * Returns null if map is empty.
     */
    PersistentMap.Entry firstEntry() throws IOException;

    /**
     * Returns null if map is empty.
     */
    PersistentMap.Entry lastEntry() throws IOException;

    /**
     * Returns a key, contained in this map, that is higher than the one given.
     * If no contained key is higher, NO_KEY is returned.
     */
    Object nextKey(Object key) throws IOException;

    /**
     * Returns a key contained in this map, that is lower than the one given.
     * If no contained key is lower, NO_KEY is returned.
     */
    Object previousKey(Object key) throws IOException;

    /**
     * Returns an entry, contained in this map, whose key is higher than the
     * one given. If no contained entry is higher, null is returned.
     */
    PersistentMap.Entry nextEntry(Object key) throws IOException;

    /**
     * Returns an entry contained in this map, whose key is lower than the one
     * given. If no contained entry is lower, null is returned.
     */
    PersistentMap.Entry previousEntry(Object key) throws IOException;

    void clear() throws IOException;

    /**
     * Clear all the entries within the specified range. The "from key" is
     * inclusive, but the "to key" is exclusive. If any key value is specified
     * as NO_KEY, then the range is open.
     */
    void clear(Object fromKey, Object toKey) throws IOException;

    /**
     * Copy all of the map keys into the given array, starting at the given
     * index. Return the new incremented index value.
     */
    int copyKeysInto(Object[] a, int index) throws IOException;

    /**
     * Copy all of the map entries into the given array, starting at the given
     * index. Return the new incremented index value.
     */
    int copyEntriesInto(Object[] a, int index) throws IOException;

    ReadWriteLock lock();
}
