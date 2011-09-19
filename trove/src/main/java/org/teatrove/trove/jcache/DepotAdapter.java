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

import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import javax.cache.Cache;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;
import javax.cache.CacheEntry;
import javax.cache.CacheException;

import org.teatrove.trove.util.Depot;
import org.teatrove.trove.util.Depot.ValueWrapper;
import org.teatrove.trove.util.Depot.InvalidationListener;


/**
 * This class is an adapter that maps the Depot API to the
 * JCache interfaces.<p>
 *
 * For further info on the JCache API, see:<br>
 * https://jsr-107-interest.dev.java.net/javadoc/javax/cache/package-summary.html
 *
 * @author Guy A. Molinari
 */
public class DepotAdapter implements Cache, InvalidationListener {

    private Depot mDepot = null;
    private ArrayList<CacheListener> mListeners = new ArrayList();


    /**
     * Constructor takes Depot instance that is delegated to.
     */
    public DepotAdapter(Depot depot) {
        mDepot = depot;
        mDepot.addInvalidationListener(this);
    }


    /**
     * De-register listeners on finalization to avoid leaking memory.
     */
    protected void finalize() {
        mDepot.removeInvalidationListener(this);
    }


    /**
     * Handler for Depot invalidation events.
     * It in turn fires a cache listener onEvent() event resulting
     * from Depot expirations.
     */
    public void invalidated(Object key) {
        fireOnEvict(key);
    }


    // Cache interface specific method implementations follow:

    /**
     * Add a listener to the list of cache listeners.
     */
    public void addListener(CacheListener listener) {
        mListeners.add(listener);
    }   
 

    /**
     * Remove a listener from the list of cache listeners. 
     */
    public void removeListener(CacheListener listener) {
        mListeners.remove(listener);
    }   


    private void fireOnClear() {
        for (CacheListener l : mListeners)
            l.onClear();
    }
 

    private void fireOnEvict(Object key) {
        for (CacheListener l : mListeners)
            l.onEvict(key);
    }
 

    private void fireOnLoad(Object key) {
        for (CacheListener l : mListeners)
            l.onLoad(key);
    }
 

    private void fireOnPut(Object key) {
        for (CacheListener l : mListeners)
            l.onPut(key);
    }
 

    private void fireOnRemove(Object key) {
        for (CacheListener l : mListeners)
            l.onRemove(key);
    }
 

    /**
     * The evict method will remove objects from the cache that are no 
     * longer valid. 
     */
    public void evict() {
        mDepot.evict();
    }


    /**
     * The getAll method will return, from the cache, a Map of the 
     * objects associated with the Collection of keys in argument "keys". 
     */
    public Map getAll(Collection keys) {
        HashMap map = new HashMap();
        for (Object key : keys) 
            map.put(key, get(key));
        return map;
    }


    /**
     * The peek method will return the object associated with "key" 
     * if it currently exists (and is valid) in the cache. 
     */
    public Object peek(Object key) {
        return mDepot.peek(key);
    }

   
    /**
     * The load method provides a means to "pre load" the cache. 
     * It fires a cache listener onLoad() event.
     */
    public void load(Object key) throws CacheException {
        try {
            new Thread(new AsyncLoader(key)).start();
        }
        catch (Exception e) { throw new CacheException("Exception thrown in load().", e); }
    }


    private class AsyncLoader implements Runnable {
        private Object mKey = null;
        AsyncLoader(Object key) { mKey = key; }
        public void run() {
            mDepot.get(mKey);
            fireOnLoad(mKey);
        }
    }


    /**
     * The loadAll method provides a means to "pre load" objects into the cache. 
     * It fires a cache listener onLoad() event for each item loaded.
     */
    public void loadAll(Collection keys) throws CacheException {
        try {
            new Thread(new AsyncBatchLoader(keys)).start();
        }
        catch (Exception e) { throw new CacheException("Exception thrown in loadAll().", e); }
    }


    private class AsyncBatchLoader implements Runnable {
        private Collection mKeys = null;
        AsyncBatchLoader(Collection keys) { mKeys = keys; }
        public void run() {
            getAll(mKeys);
            for (Object key : mKeys)
                fireOnLoad(key);
        }
    }


    /**
     * Returns the CacheStatistics object associated with the cache. 
     *
     * @see javax.cache.CacheStatistics
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatisticsImpl(mDepot);
    }


    /**
     * Returns the CacheEntry object associated with the object identified by "key".
     *
     * @see javax.cache.CacheEntry
     */
    public CacheEntry getCacheEntry(Object key) {
        ValueWrapper value = mDepot.getWrappedValue(key);
        if (value != null)
            return new CacheEntryImpl(key, value);
        return null;
    }

    
    // Map interface methods follow.  Most of these delegate to the 
    // backing Depot instance.

   
    /**
     * Fires a cache listener onClear() event and delegates to backing Depot.
     * @see java.util.Map.clear()
     */ 
    public void clear() {
        mDepot.clear();
        fireOnClear();
    }

   
    /**
     * Delegates to backing Depot.
     * @see java.util.Map.containsKey()
     */ 
    public boolean containsKey(Object key) {
        return mDepot.containsKey(key);
    }


    /**
     * Delegates to backing Depot.
     * @see java.util.Map.containsValue()
     */ 
    public boolean containsValue(Object value) {
       return mDepot.containsValue(value);
    }

    
    /**
     * Delegates to backing Depot.
     * @see java.util.Map.entrySet()
     */ 
    public Set entrySet() { 
       return mDepot.entrySet();
    }

    
    /**
     * Delegates to backing Depot.
     * @see java.lang.Object.equals()
     */ 
    public boolean equals(Object o) {
       return mDepot.equals(o);
    }


    /**
     * Delegates to backing Depot.
     * @see java.lang.Object.hashCode()
     */ 
    public int hashCode() {
       return mDepot.hashCode();
    }


    /**
     * Delegates to backing Depot.
     * @see java.util.Map.get()
     */ 
    public Object get(Object key) {
       return mDepot.get(key);
    }

    
    /**
     * Delegates to backing Depot.
     * @see java.util.Map.isEmpty()
     */ 
    public boolean isEmpty() {
       return mDepot.isEmpty();
    }


    /**
     * Delegates to backing Depot.
     * @see java.util.Map.keySet()
     */ 
    public Set keySet() {
       return mDepot.keySet();
    }


    /**
     * Fires a cache listener onPut() event and delegates to backing Depot.
     * @see java.util.Map.put()
     */ 
    public Object put(Object key, Object value) {
        Object old = mDepot.put(key, value);
        fireOnPut(key);
        return old;
    }


    /**
     * Fires a cache listener onPut() event for each item in the Map 
     * and delegates to backing Depot.
     * @see java.util.Map.putAll()
     */ 
    public void putAll(Map map) {
        for (Object key : map.keySet())
            put(key, map.get(key));
    }
    

    /**
     * Fires a cache listener onRemove() event and delegates to backing Depot.
     * @see java.util.Map.remove()
     */ 
    public Object remove(Object key) {
        Object old = mDepot.remove(key);
        fireOnRemove(key);
        return old;
    }


    /**
     * Delegates to backing Depot.
     * @see java.util.Map.size()
     */ 
    public int size() {
        return mDepot.size();
    }


    /**
     * Delegates to backing Depot.
     * @see java.util.Map.values()
     */ 
    public Collection values() {
        return mDepot.values();
    }


    /**
     * Returns a reference to the backing Depot.  Hopefully there will
     * seldom be a need to do this, however, the Depot class does contain
     * functionality unique to it that is absent from javax.cache.Cache.
     */
    public Depot getDepot() { return mDepot; }


}
