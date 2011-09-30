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
import java.io.Serializable;

/**
 * A Map supporting null keys that wraps a Map that doesn't support null keys.
 * NullKeyMap substitutes null keys with a special placeholder object. This
 * technique does not work when the wrapped Map is a TreeMap because it cannot
 * be compared against other objects. In order for TreeMaps to support null
 * keys, use any of the null ordering comparators found in the {@link Utils}
 * class.
 *
 * @author Brian S O'Neill
 */
public class NullKeyMap extends AbstractMap implements Serializable {
    // Instead of using null as a key, use this placeholder.
    private static final Object NULL = new Serializable() {};

    private Map mMap;

    private transient Set mKeySet;
    private transient Set mEntrySet;

    /**
     * @param map The map to wrap.
     */
    public NullKeyMap(Map map) {
        mMap = map;
    }
    
    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return (key == null) ? mMap.containsKey(NULL) : mMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return mMap.containsValue(value);
    }

    public Object get(Object key) {
        return (key == null) ? mMap.get(NULL) : mMap.get(key);
    }

    public Object put(Object key, Object value) {
        return mMap.put((key == null) ? NULL : key, value);
    }

    public Object remove(Object key) {
        return (key == null) ? mMap.remove(NULL) : mMap.remove(key);
    }

    public void putAll(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mMap.clear();
    }

    public Set keySet() {
        if (mKeySet == null) {
            mKeySet = new AbstractSet() {
                public Iterator iterator() {
                    final Iterator it = mMap.keySet().iterator();

                    return new Iterator() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        public Object next() {
                            Object key = it.next();
                            return (key == NULL) ? null : key;
                        }

                        public void remove() {
                            it.remove();
                        }
                    };
                }
                
                public boolean contains(Object key) {
                    return containsKey((key == null) ? NULL : key);
                }

                public boolean remove(Object key) {
                    if (key == null) {
                        key = NULL;
                    }
                    if (containsKey(key)) {
                        NullKeyMap.this.remove(key);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                public int size() {
                    return NullKeyMap.this.size();
                }
                
                public void clear() {
                    NullKeyMap.this.clear();
                }
            };
        }

        return mKeySet;
    }

    public Collection values() {
        return mMap.values();
    }

    public Set entrySet() {
        if (mEntrySet == null) {
            mEntrySet = new AbstractSet() {
                public Iterator iterator() {
                    final Iterator it = mMap.entrySet().iterator();

                    return new Iterator() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        public Object next() {
                            final Map.Entry entry = (Map.Entry)it.next();
                            if (entry.getKey() == NULL) {
                                return new AbstractMapEntry() {
                                    public Object getKey() {
                                        return null;
                                    }

                                    public Object getValue() {
                                        return entry.getValue();
                                    }

                                    public Object setValue(Object value) {
                                        return entry.setValue(value);
                                    }
                                };
                            }
                            else {
                                return entry;
                            }
                        }

                        public void remove() {
                            it.remove();
                        }
                    };
                }
                
                public boolean contains(Object obj) {
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)obj;
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (key == null) {
                        key = NULL;
                    }
                    Object lookup = get(key);
                    if (lookup == null) {
                        return value == null;
                    }
                    else {
                        return lookup.equals(value);
                    }
                }

                public boolean remove(Object obj) {
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = ((Map.Entry)obj);
                    Object key = entry.getKey();
                    if (key == null) {
                        key = NULL;
                    }
                    if (containsKey(key)) {
                        NullKeyMap.this.remove(key);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                public int size() {
                    return NullKeyMap.this.size();
                }
                
                public void clear() {
                    NullKeyMap.this.clear();
                }
            };
        }

        return mEntrySet;
    }
}
