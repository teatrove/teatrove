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

package org.teatrove.trove.jcache;

import javax.cache.CacheEntry;
import org.teatrove.trove.util.Depot.ValueWrapper;

/**
 * This class implements the JCache CacheEntry interface as part of the
 * Depot JCache adapter. <p>
 * 
 * Calls to Cache.peek() will not update any entry statistics.<p>
 *
 * For further info on the JCache API, see:<br>
 * https://jsr-107-interest.dev.java.net/javadoc/javax/cache/package-summary.html
 *
 * @author Guy A. Molinari
 */
public class CacheEntryImpl implements CacheEntry {

    private Object mKey = null;
    private ValueWrapper mValueWrapper = null;
    private Object mValue = null;

    // No need for a public constructor.
    CacheEntryImpl(Object key, ValueWrapper valueWrapper) {
        mKey = key;
        mValueWrapper = valueWrapper;
    }

    public Object getKey() { return mKey; }
    public Object getValue() { return mValueWrapper.getValue(); }
    public Object setValue(Object value) {  return mValueWrapper.setValue(value); }
    public boolean equals(Object o) { 
        if (mValue == null && o == null)
            return true;
        return mValue != null ? mValue.equals(o) : false;
    }
    public int hashCode() { return mValue != null ? mValue.hashCode() : -1; }

    
    /**
     * The cost in milliseconds for retrieval from the backing store, 0 if
     * the item was cached, or -1 if the retriever timed out.
     */
    public long getCost() {
        if (mValueWrapper.wasCached())
            return 0L;
        else
            return mValueWrapper.getRetrievalElapsedTime();
    }

    
    /**
     * This counter is incremented whenever it is retrieved from
     * the cache (except for calls to peek()).
     */
    public int getHits() {
        return mValueWrapper.getHits();
    }

    
    /**
     * This time stamp indicates the time at
     * which the item will expire.  This only applies
     * for Depot.Perishable instances.
     */
    public long getExpirationTime() {
        return mValueWrapper.getExpirationTime();
    }

    
    /**
     * This time stamp is updated when an item
     * exists in the cache, but is subsequently updated.
     * An update is determined by comparing the object in
     * the cache with the newly arriving object using
     * equals().
     */
    public long getLastUpdateTime() {
        return mValueWrapper.getLastUpdateTime();
    }


    /**
     * Returns a version counter that is incremented
     * when items are updated (via equals() equality
     * comparison).
     */
    public long getVersion() {
        return mValueWrapper.getVersion();
    }

    
    /**
     * This time stamp is set once when an item is loaded 
     * into the cache.
     */
    public long getCreationTime() {
        return mValueWrapper.getArrivalTime();
    }


    /**
     * This time stamp is updated whenever a cached item is
     * accessed.
     */
    public long getLastAccessTime() {
        return mValueWrapper.getLastAccessTime();
    }


    /**
     * Returns true if the item is found in the valid cache.
     */
    public boolean isValid() {
        return mValueWrapper.isValid();
    }

}

