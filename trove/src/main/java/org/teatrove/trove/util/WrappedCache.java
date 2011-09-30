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
 * A Map that implements a write-through cache to another map. Two maps are
 * supplied: one for caching and one for main storage. WrappedCache is not
 * thread-safe and must be wrapped with Collections.synchronizedMap to be made
 * thread-safe.
 *
 * @author Brian S O'Neill
 */
public class WrappedCache extends AbstractMap implements Map {
    private final Map mCacheMap;
    private final Map mBackingMap;

    /**
     * @param cacheMap the cache map should offer fast access, but it should
     * automatically limit its maximum size
     * @param backingMap the backingMap will be read from only if the requested
     * entry isn't in the cache
     */
    public WrappedCache(Map cacheMap, Map backingMap) {
        mCacheMap = cacheMap;
        mBackingMap = backingMap;
    }

    /**
     * Returns the size of the backing map.
     */
    public int size() {
        return mBackingMap.size();
    }

    /**
     * Returns the empty status of the backing map.
     */
    public boolean isEmpty() {
        return mBackingMap.isEmpty();
    }

    /**
     * Returns true if the cache contains the key or else if the backing map
     * contains the key.
     */
    public boolean containsKey(Object key) {
        return mCacheMap.containsKey(key) || mBackingMap.containsKey(key);
    }

    /**
     * Returns true if the cache contains the value or else if the backing map
     * contains the value.
     */
    public boolean containsValue(Object value) {
        return mCacheMap.containsValue(value) ||
            mBackingMap.containsValue(value);
    }

    /**
     * Returns the value from the cache, or if not found, the backing map.
     * If the backing map is accessed, the value is saved in the cache for
     * future gets.
     */
    public Object get(Object key) {
        Object value = mCacheMap.get(key);
        if (value != null || mCacheMap.containsKey(key)) {
            return value;
        }
        value = mBackingMap.get(key);
        if (value != null || mBackingMap.containsKey(key)) {
            mCacheMap.put(key, value);
        }
        return value;
    }

    /**
     * Puts the entry into both the cache and backing map. The old value in
     * the backing map is returned.
     */
    public Object put(Object key, Object value) {
        mCacheMap.put(key, value);
        return mBackingMap.put(key, value);
    }

    /**
     * Removes the key from both the cache and backing map. The old value in
     * the backing map is returned.
     */
    public Object remove(Object key) {
        mCacheMap.remove(key);
        return mBackingMap.remove(key);
    }

    /**
     * Clears both the cache and backing map.
     */
    public void clear() {
        mCacheMap.clear();
        mBackingMap.clear();
    }

    /**
     * Returns the key set of the backing map.
     */
    public Set keySet() {
        return mBackingMap.keySet();
    }

    /**
     * Returns the values of the backing map.
     */
    public Collection values() {
        return mBackingMap.values();
    }

    /**
     * Returns the entry set of the backing map.
     */
    public Set entrySet() {
        return mBackingMap.entrySet();
    }
}
