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

/**
 * Cache is a SoftHashMap that is guaranteed to have the most recently used
 * entries available. Calling "get" or "put" updates the internal MRU list,
 * but calling "containsKey" or "containsValue" will not.
 * <p>
 * Like its base class, Cache is not thread-safe and must be wrapped with
 * Collections.synchronizedMap to be made thread-safe.
 *
 * @author Brian S O'Neill
 */
public class Cache extends SoftHashMap {
    private final int mMaxRecent;
    // Contains hard references to entries.
    private final UsageMap mUsageMap;

    /**
     * Construct a Cache with an amount of recently used entries that are
     * guaranteed to always be in the Cache.
     *
     * @param maxRecent maximum amount of recently used entries guaranteed to
     * be in the Cache.
     * @throws IllegalArgumentException if maxRecent is less than or equal to
     * zero.
     */
    public Cache(int maxRecent) {
        if (maxRecent <= 0) {
            throw new IllegalArgumentException
                ("Max recent must be greater than zero: " + maxRecent);
        }
        mMaxRecent = maxRecent;
        mUsageMap = new UsageMap();
    }

    /**
     * Piggyback this Cache onto another one in order for the map of recently
     * used entries to be shared. If this Cache is more active than the one
     * it attaches to, then more of its most recently used entries will be
     * guaranteed to be in the Cache, possibly bumping out entries from the
     * other Cache.
     */
    public Cache(Cache cache) {
        mMaxRecent = cache.mMaxRecent;
        mUsageMap = cache.mUsageMap;
    }

    public int getMaxRecent() { return mMaxRecent; }

    public Object get(Object key) {
        Object value = super.get(key);
        if (value != null || super.containsKey(key)) {
            adjustMRU(key, value);
        }
        return value;
    }

    public Object put(Object key, Object value) {
        if (value == null) {
            value = new Null();
        }
        adjustMRU(key, value);
        return super.put(key, value);
    }

    public Object remove(Object key) {
        synchronized (mUsageMap) {
            mUsageMap.remove(key);
        }
        return super.remove(key);
    }

    public void clear() {
        super.clear();
        synchronized (mUsageMap) {
            mUsageMap.clear();
        }
    }

    private void adjustMRU(Object key, Object value) {
        synchronized (mUsageMap) {
            Object existing = mUsageMap.get(key);
            
            if (existing != null) {
                if (value == null && existing instanceof Null) {
                    value = existing;
                }
            }
            else {
                if (!mUsageMap.containsKey(key)) {
                    // A new entry will be put into the UsageMap, so remove
                    // least recently used if MRU is too big.
                    while (mUsageMap.size() >= mMaxRecent) {
                        mUsageMap.remove(mUsageMap.lastKey());
                    }
                }
            }
            
            mUsageMap.put(key, value);
        }
    }
}
