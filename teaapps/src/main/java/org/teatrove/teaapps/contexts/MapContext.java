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

import org.teatrove.trove.util.PropertyMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Custom Tea context used for providing map access to templates including
 * creation, retrieval, modification, etc.
 * 
 * @author Scott Jappinen
 */
public class MapContext {

    /**
     * Clear all elements from the given map.
     * 
     * @param map The map to clear
     * 
     * @see Map#clear()
     */
    public void clear(Map<?, ?> map) {
        map.clear();
    }
    
    /**
     * Check if the given map contains the given key.
     * 
     * @param map The map to check
     * @param key The key to check for
     * 
     * @return <code>true</code> if the key is contained in the map,
     *         <code>false</code> otherwise
     *         
     * @see Map#containsKey(Object)
     */
    public boolean containsKey(Map<?, ?> map, Object key) {
        return map.containsKey(key);
    }
    
    /**
     * Check if the given map contains the given value.
     * 
     * @param map The map to check
     * @param value The value to check for
     * 
     * @return <code>true</code> if the value is contained in the map,
     *         <code>false</code> otherwise
     *         
     * @see Map#containsValue(Object)
     */
    public boolean containsValue(Map<?, ?> map, Object value) {
        return map.containsValue(value);
    }
    
    /**
     * Create a new empty {@link HashMap}.
     * 
     * @return The new empty hash map
     */
    public Map<?, ?> createHashMap() {
        return new HashMap<Object, Object>();
    }

    /**
     * Create a new empty {@link TreeMap} that provides key sorting.
     * 
     * @return The new empty sorted map
     */
    public SortedMap<?, ?> createSortedMap() {
        return new TreeMap<Object, Object>();
    }
    
    /**
     * Create a new empty {@link PropertyMap}.
     * 
     * @return The new empty property map
     */
    public PropertyMap createPropertyMap() {
        return new PropertyMap();
    }
    
    /**
     * Create a new empty {@link LinkedHashMap} that implements the standard
     * {@link Map} interface but supported insertion-order iteration.
     * 
     * @return The new empty linked hash map
     */
    public LinkedHashMap<?, ?> createLinkedHashMap() {
        return new LinkedHashMap<Object, Object>();
    }

    /**
     * Create a new {@link PropertyMap} initialized with the elements of the
     * given map.
     * 
     * @param map The map to copy into the property map
     * 
     * @return The new property map
     */
    public PropertyMap createPropertyMap(Map<?, ?> map) {
        return new PropertyMap(map);
    }
    
    /**
     * Create a new {@link PropertyMap} initialized with the elements of the
     * given map using the given separator (ie: '.') for sub-map keys.
     * 
     * @param map The map to copy into the property map
     * @param seperator The separator to use in the property map
     * 
     * @return The new property map
     */
    public PropertyMap createPropertyMap(Map<?, ?> map, String seperator) {
        return new PropertyMap(map, seperator);
    }    

    /**
     * Get the set of all {@link Map.Entry} values in the given map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param map The map to get the entry set of
     * 
     * @return The set of all map entries
     * 
     * @see Map#entrySet()
     */
    public <K, V> Set<Map.Entry<K, V>> entrySet(Map<K, V> map) {
        return map.entrySet();
    }

    /**
     * Get the set of all keys in the given map.
     * 
     * @param <K> The component type of the keys
     * 
     * @param map The map to get the keys from
     * 
     * @return The set of all keys in the map
     * 
     * @see Map#keySet()
     */
    public <K> Set<K> keySet(Map<K, ?> map) {
        return map.keySet();
    }
    
    /**
     * Get the array of all keys in the given map.
     * 
     * @param <K> The component type of the keys
     * 
     * @param map The map to get the keys from
     * 
     * @return The array of all keys in the map
     */
    @SuppressWarnings("unchecked")
    public <K> K[] getKeys(Map<K, ?> map) {
        K[] result = null;
        Set<K> keySet = map.keySet();
        if (keySet.size() > 0) {
            result = (K[]) keySet.toArray(new Object[keySet.size()]);
        }
        
        return result;
    }

    /**
     * Get the array of all keys in the given map as strings. This assumes that
     * each key in the given map is a String.
     * 
     * @param map The map to get the keys from
     * 
     * @return The array of all keys in the map as strings
     */
    public String[] getKeysAsStrings(Map<String, ?> map) {
        String[] result = null;
        Set<String> keySet = map.keySet();
        int keySize = keySet.size();
        if (keySize > 0) {          
            result = keySet.toArray(new String[keySize]);
        }
        
        return result;
    }    
    
    /**
     * Put the given key and value in the given map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param map The map to add the key/value pair to
     * @param key The key to put in the map
     * @param value The value for the given key
     * 
     * @see Map#put(Object, Object)
     */
    public <K, V> void put(Map<K, V> map, K key, V value) {
        map.put(key, value);
    }

    /**
     * Put all of the given key and value pairs in the given map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param mapToAddTo The map to add the key/value pairs to
     * @param mapToAdd The key/value pairs to put in the map
     * 
     * @see Map#putAll(Map)
     */
    public <K, V> void putAll(Map<K, V> mapToAddTo, 
                              Map<? extends K, ? extends V> mapToAdd) {
        mapToAddTo.putAll(mapToAdd);
    }

    /**
     * Get the size of the given map.
     * 
     * @param map The map to get the size of
     * 
     * @return The size of the map
     * 
     * @see Map#size()
     */
    public int size(Map<?, ?> map) {
        return map.size();
    }

    /**
     * Remove the given key from the given map.
     * 
     * @param map The map to remove from
     * @param key The key to remove from the map
     * 
     * @return The value at the given key or <code>null</code> if non-existant
     */
    public Object remove(Map<?, ?> map, Object key) {
        return map.remove(key);
    }
    
    /**
     * Get the collection of all values in the given map.
     * 
     * @param <V> The component type of the values
     * 
     * @param map The map to retrieve the values from
     * 
     * @return The collection of values in the map
     * 
     * @see Map#values()
     */
    public <V> Collection<V> values(Map<?, V> map) {
        return map.values();
    }
    
    /**
     * Get the first key within the given sorted map. The first key will be the
     * first key ordered by the sort algorithm of the keys in the given map.
     * 
     * @param <K> The component type of the keys
     * 
     * @param map The sorted map to retrieve from
     * 
     * @return The first key within the given sorted map
     * 
     * @see SortedMap#firstKey()
     */
    public <K> K firstKey(SortedMap<K, ?> map) {
        return map.firstKey();
    }
    
    /**
     * Get the sorted map of all key/value pairs where the keys are less than
     * the given key based on the sort algorithm of the keys in the map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param map The sorted map to retrieve from
     * @param toKey The key, exclusive, to retrieve up to
     * 
     * @return A new sorted map of all key/value pairs up to the given key
     * 
     * @see SortedMap#headMap(Object)
     */
    public <K, V> SortedMap<K, V> headMap(SortedMap<K, V> map, K toKey) {
        return map.headMap(toKey);
    }
    
    /**
     * Get the last key within the given sorted map. The last key will be the
     * last key ordered by the sort algorithm of the keys in the given map.
     * 
     * @param <K> The component type of the keys
     * 
     * @param map The sorted map to retrieve from
     * 
     * @return The last key within the given sorted map
     * 
     * @see SortedMap#lastKey()
     */
    public <K> K lastKey(SortedMap<K, ?> map) {
        return map.lastKey();
    }
    
    /**
     * Get a portion of the given sorted map from the given fromKey, inclusive,
     * up to, but excluding, the given toKey. The keys are based on the sort
     * algorithm of the keys in the given map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param map The map to retrieve from
     * @param fromKey The key to start from, inclusive
     * @param toKey The key to end at, exclusive
     * 
     * @return The portion of the map from the from key to the end key
     * 
     * @see SortedMap#subMap(Object, Object)
     */
    public <K, V> SortedMap<K, V> subMap(SortedMap<K, V> map, 
                                         K fromKey, K toKey) {
        return map.subMap(fromKey, toKey);
    }
    
    /**
     * Get the sorted map of all key/value pairs where the keys are greater than
     * or equal to the given key based on the sort algorithm of the keys in the
     * map.
     * 
     * @param <K> The component type of the keys
     * @param <V> The component type of the values
     * 
     * @param map The sorted map to retrieve from
     * @param toKey The key, inclusive, to start from
     * 
     * @return A new sorted map of all key/value pairs starting with the key
     * 
     * @see SortedMap#tailMap(Object)
     */
    public <K, V> SortedMap<K, V> tailMap(SortedMap<K, V> map, K fromKey) {
        return map.tailMap(fromKey);
    }
    
    /**
     * Get the property map of all children that are descendents of the given
     * name based on the delimiter of the given property map. For example,
     * given a delimiter of '.' and the keys: "test.one", "test.two", "three"
     * will result in the submap of "one" and "two" for the name "test".
     * 
     * @param map The property map to retrieve from
     * @param name The name of the property
     * 
     * @return The sub map of all descendents
     * 
     * @see PropertyMap#subMap(String)
     */
    public PropertyMap subMap(PropertyMap map, String name) {
        return map.subMap(name);
    }
    
    /**
     * Get the set of all keys that provide valid sub-maps and descendents.
     * For example, a property map containing "test.one", "users.nick",
     * "users.john" will result in two keys: "test" and "users".
     * 
     * @param map The property map to retrieve from
     * 
     * @return The set of all unique keys that provide sub maps
     * 
     * @see PropertyMap#subMapKeySet()
     */
    public Set<?> subMapKeySet(PropertyMap map) {
        return map.subMapKeySet();
    }
    
    /**
     * Get the array of all keys that provide valid sub-maps and descendents.
     * For example, a property map containing "test.one", "users.nick",
     * "users.john" will result in two keys: "test" and "users".
     * 
     * @param map The property map to retrieve from
     * 
     * @return The array of all unique keys that provide sub maps as strings
     * 
     * @see PropertyMap#subMapKeySet()
     */
    public String[] subMapKeysAsStrings(PropertyMap map) {
        String[] result = null;
        Set<?> keySet = map.subMapKeySet();
        int keySize = keySet.size();
        if (keySize > 0) {          
            result = keySet.toArray(new String[keySize]);
        }
        return result;
    }
}
