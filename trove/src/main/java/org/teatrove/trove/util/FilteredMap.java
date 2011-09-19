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

import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;


/**
 * This class can be used to filter the collections returned by the 
 * entrySet(), values(), and keySet() methods.  It uses the GoF 
 * Decorator pattern to hide the details of this from the interface
 * definition.  Just pass the map to filter and the FilteredMap.Filter
 * implementation to it's constructor.<p>
 *
 * Also, a second constructor takes only a backing map instance.  This
 * has a default filter that accepts anything.  It is useful for 
 * adapting a Map implementation that does not support java generics into
 * one that does.<p>
 *
 * @author Guy A. Molinari
 */
public class FilteredMap<K, V> implements Map<K, V> {

    private Map<K, V> mBackingMap = null;
    private Filter mFilter = null;


    /**
     * Decorate a backing map with a filter.
     */
    public FilteredMap(Map<K, V> backingMap, Filter filter) {
        mBackingMap = backingMap;
        mFilter = filter;
        if (mBackingMap == null)
            throw new IllegalArgumentException("Backing map must not be null");
        if (mFilter == null)
            throw new IllegalArgumentException("Filter instance must not be null");
    }


    /**
     * Decorate a backing map with a filter that transparently accepts anything.
     */
    public FilteredMap(Map<K, V> backingMap) {
        this(backingMap, new Filter() {
            public boolean accept(Map.Entry e) {
                return true;
            }
        });
    }


    /** 
     * Return a filtered set of entries.
     * @see Map.entryset
     */
    public Set<Map.Entry<K, V>> entrySet() { 
       HashSet s = new HashSet(mBackingMap.size());
       for (Map.Entry e : mBackingMap.entrySet()) {
           if (mFilter.accept(e))
               s.add(e);
       }
       return s;
    }

    
    /**
     * Return a filtered set of values.
     * @see Map.values
     */
    public Collection<V> values() {
       Set<Map.Entry<K, V>> s = entrySet();
       ArrayList l = new ArrayList(s.size());
       for (Map.Entry e : s) 
           l.add(e.getValue());
       return l;
    }


    /**
     * Return a filtered set of keys.
     * @see Map.keySet
     */
    public Set<K> keySet() {
       Set<Map.Entry<K, V>> s = entrySet();
       HashSet keys = new HashSet(s.size());
       for (Map.Entry e : mBackingMap.entrySet())
           keys.add(e.getKey());
       return keys;
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.clear()
     */
    public void clear() { 
        mBackingMap.clear(); 
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.containsKey
     */
    public boolean containsKey(Object key) { 
        return mBackingMap.containsKey(key); 
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.containsValue
     */
    public boolean containsValue(Object value) {
       return mBackingMap.containsValue(value);
    }

    
    /**
     * Delegates to backing map.
     * @see Object.equals.
     */
    public boolean equals(Object o) {
       return mBackingMap.equals(o);
    }


    /**
     * Delegates to backing map.
     * @see Object.hashCode
     */
    public int hashCode() {
       return mBackingMap.hashCode();
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.get
     */
    public V get(Object key) {
       return mBackingMap.get(key);
    }

    
    /**
     * Delegates to backing map.
     * @see java.util.Map.isEmpty
     */
    public boolean isEmpty() {
       return mBackingMap.isEmpty();
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.put
     */
    public V put(K key, V value) {
        return mBackingMap.put(key, value);
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.putAll
     */
    public void putAll(Map<? extends K,? extends V> m) {
        mBackingMap.putAll(m);
    }
    

    /**
     * Delegates to backing map.
     * @see java.util.Map.remove
     */
    public V remove(Object key) {
        return mBackingMap.remove(key);
    }


    /**
     * Delegates to backing map.
     * @see java.util.Map.size
     */
    public int size() {
        return mBackingMap.size();
    }


    /**
     * Filter interface.
     */
    public interface Filter {
        /**
         * Returns true if the given entry should be included.
         */
        public boolean accept(Map.Entry e);
    }


}
