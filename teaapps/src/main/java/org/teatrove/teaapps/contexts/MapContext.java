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
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Scott Jappinen
 */
public class MapContext {

    public void clear(Map map) {
        map.clear();
    }
    
    public boolean containsKey(Map map, Object key) {
        return map.containsKey(key);
    }
    
    public boolean containsValue(Map map, Object value) {
        return map.containsValue(value);
    }
    
    public Map createHashMap() {
        return new HashMap();
    }

    public SortedMap createSortedMap() {
        return new TreeMap();
    }
    
    public PropertyMap createPropertyMap() {
        return new PropertyMap();
    }

    public PropertyMap createPropertyMap(Map map) {
        return new PropertyMap(map);
    }
    
    public PropertyMap createPropertyMap(Map map, String seperator) {
        return new PropertyMap(map, seperator);
    }    

    public Set entrySet(Map map) {
        return map.entrySet();
    }

    public Set keySet(Map map) {
        return map.keySet();
    }
    
    public Object[] getKeys(Map map) {
        Object[] result = null;
        Set keySet = map.keySet();
        if (keySet.size() > 0) {
            result = keySet.toArray();
        }
        return result;
    }
     
    public String[] getKeysAsStrings(Map<String,Object> map) {
        String[] result = null;
        Set<String> keySet = map.keySet();
        int keySize = keySet.size();
        if (keySize > 0) {          
            result = (String[]) keySet.toArray(new String[keySize]);
        }
        return result;
    }    
    
    public void put(Map<Object,Object> map, Object key, Object value) {
        map.put(key, value);
    }

    public void putAll(Map<Object,Object> mapToAddTo, Map<Object,Object> mapToAdd) {
        mapToAddTo.putAll(mapToAdd);
    }

    public int size(Map map) {
        return map.size();
    }

    public Object remove(Map map, Object key) {
        return map.remove(key);
    }
    
    public Collection values(Map map) {
        return map.values();
    }
    
    public Object firstKey(SortedMap map) {
        return map.firstKey();
    }
    
    public SortedMap headMap(SortedMap<Object,Object> map, Object toKey) {
        return map.headMap(toKey);
    }
    
    public Object lastKey(SortedMap map) {
        return map.lastKey();
    }
    
    public SortedMap subMap(SortedMap<Object,Object> map, Object fromKey, Object toKey) {
        return map.subMap(fromKey, toKey);
    }
    
    public SortedMap tailMap(SortedMap<Object,Object> map, Object fromKey) {
        return map.tailMap(fromKey);
    }
    
    public PropertyMap subMap(PropertyMap map, String name) {
        return map.subMap(name);
    }
    
    public Set subMapKeySet(PropertyMap map) {
        return map.subMapKeySet();
    }
    
    public String[] subMapKeysAsStrings(PropertyMap map) {
        String[] result = null;
        Set<Object> keySet = map.subMapKeySet();
        int keySize = keySet.size();
        if (keySize > 0) {          
            result = (String[]) keySet.toArray(new String[keySize]);
        }
        return result;
    }
}
