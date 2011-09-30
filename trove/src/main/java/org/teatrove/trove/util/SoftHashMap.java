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

import java.lang.ref.*;
import java.util.*;

/**
 * A Map that softly references its values and can be used as a simple cache.
 * SoftHashMap is not thread-safe and must be wrapped with
 * Collections.synchronizedMap to be made thread-safe. Most of the
 * implementation for this class is ripped off from java.util.HashMap
 * <p>
 * Note: Softly referenced entries may be automatically removed during
 * either accessor or mutator operations, possibly causing a concurrent
 * modification to be detected. Therefore, even if multiple threads are only
 * accessing this map, be sure to synchronize this map first. Also, do not
 * rely on the value returned by size() when using an iterator from this map.
 * The iterators may return less entries than the amount reported by size().
 * 
 * @author Brian S O'Neill
 * @see Cache
 */
public class SoftHashMap extends AbstractMap implements Map, Cloneable {
    /**
     * Test program.
     */
    /*
    public static void main(String[] arg) throws Exception {
        Map cache = new SoftHashMap();

        for (int i = 0, j = 0; i < 100000; i++, j += 15) {
            if (i % 100 == 0) {
                System.out.println("Size = " + cache.size());
            }

            //Thread.sleep(1);

            Integer key = new Integer(i);
            Integer value = new Integer(j);
            
            cache.put(key, value);
        }
      
        Map.Entry entry = (Map.Entry)cache.entrySet().iterator().next();
        System.out.println(entry);
        //entry = null;

        System.out.println(cache);

        int originalSize = cache.size();

        //cache = null;

        for (int i=0; i<100; i++) {
            System.gc();
        }

        System.out.println(cache);

        System.out.println(originalSize);
        System.out.println(cache.size());
        System.out.println(entry);

        Thread.sleep(1000000);
    }
    */

    /**
     * The hash table data.
     */
    private transient Entry mTable[];

    /**
     * The total number of mappings in the hash table.
     */
    private transient int mCount;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    private int mThreshold;

    /**
     * The load factor for the hashtable.
     *
     * @serial
     */
    private float mLoadFactor;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    private transient int mModCount = 0;

    // Views
    
    private transient Set mKeySet = null;
    private transient Set mEntrySet = null;
    private transient Collection mValues = null;

    /**
     * Constructs a new, empty map with the specified initial 
     * capacity and the specified load factor. 
     *
     * @param      initialCapacity   the initial capacity of the HashMap.
     * @param      loadFactor        the load factor of the HashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public SoftHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        }

        if (initialCapacity == 0) {
            initialCapacity = 1;
        }

        mLoadFactor = loadFactor;
        mTable = new Entry[initialCapacity];
        mThreshold = (int)(initialCapacity * loadFactor);
    }
    
    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the HashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public SoftHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public SoftHashMap() {
        this(11, 0.75f);
    }

    /**
     * Constructs a new map with the same mappings as the given map.  The
     * map is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor,
     * which is <tt>0.75</tt>.
     */
    public SoftHashMap(Map t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    /**
     * Returns the number of key-value mappings in this map, but this value
     * may be larger than actual amount of entries produced by an iterator.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return mCount;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return mCount == 0;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
        if (value == null) {
            value = new Null();
        }

        Entry tab[] = mTable;
        
        for (int i = tab.length ; i-- > 0 ;) {
            for (Entry e = tab[i], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[i] = e.mNext;
                    }
                    mCount--;
                }
                else if (value.equals(entryValue)) {
                    return true;
                }
                else {
                    prev = e;
                }
            }
        }

        return false;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * 
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @param key key whose presence in this Map is to be tested.
     */
    public boolean containsKey(Object key) {
        Entry tab[] = mTable;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                if (e.getValue() == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[index] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mHash == hash && key.equals(e.mKey)) {
                    return true;
                }
                else {
                    prev = e;
                }
            }
        }
        else {
            for (Entry e = tab[0], prev = null; e != null; e = e.mNext) {
                if (e.getValue() == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[0] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mKey == null) {
                    return true;
                }
                else {
                    prev = e;
                }
            }
        }

        return false;
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @return the value to which this map maps the specified key.
     * @param key key whose associated value is to be returned.
     */
    public Object get(Object key) {
        Entry tab[] = mTable;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[index] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mHash == hash && key.equals(e.mKey)) {
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }
        else {
            for (Entry e = tab[0], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[0] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mKey == null) {
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }

        return null;
    }

    /**
     * Scans the contents of this map, removing all entries that have a
     * cleared soft value.
     */
    private void cleanup() {
        Entry tab[] = mTable;
        
        for (int i = tab.length ; i-- > 0 ;) {
            for (Entry e = tab[i], prev = null; e != null; e = e.mNext) {
                if (e.getValue() == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[i] = e.mNext;
                    }
                    mCount--;
                }
                else {
                    prev = e;
                }
            }
        }
    }

    /**
     * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     */
    private void rehash() {
        int oldCapacity = mTable.length;
        Entry oldMap[] = mTable;
        
        int newCapacity = oldCapacity * 2 + 1;
        Entry newMap[] = new Entry[newCapacity];
        
        mModCount++;
        mThreshold = (int)(newCapacity * mLoadFactor);
        mTable = newMap;
        
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry old = oldMap[i] ; old != null ; ) {
                Entry e = old;
                old = old.mNext;

                // Only copy entry if its value hasn't been cleared.
                if (e.getValue() == null) {
                    mCount--;
                }
                else {
                    int index = (e.mHash & 0x7FFFFFFF) % newCapacity;
                    e.mNext = newMap[index];
                    newMap[index] = e;
                }
            }
        }
    }
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the HashMap previously associated
     *         <tt>null</tt> with the specified key.
     */
    public Object put(Object key, Object value) {
        if (value == null) {
            value = new Null();
        }

        // Makes sure the key is not already in the HashMap.
        Entry tab[] = mTable;
        int hash;
        int index;

        if (key != null) {
            hash = key.hashCode();
            index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (e.getValue() == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[index] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mHash == hash && key.equals(e.mKey)) {
                    e.setValue(value);
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }
        else {
            hash = 0;
            index = 0;
            for (Entry e = tab[0], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[0] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mKey == null) {
                    e.setValue(value);
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }

        mModCount++;

        if (mCount >= mThreshold) {
            // Cleanup the table if the threshold is exceeded.
            cleanup();
        }

        if (mCount >= mThreshold) {
            // Rehash the table if the threshold is still exceeded.
            rehash();
            tab = mTable;
            index = (hash & 0x7FFFFFFF) % tab.length;
        }
        
        // Creates the new entry.
        Entry e = new Entry(hash, key, (Object)value, tab[index]);
        tab[index] = e;
        mCount++;
        return null;
    }
    
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key.
     */
    public Object remove(Object key) {
        Entry tab[] = mTable;

        if (key != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            
            for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[index] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mHash == hash && key.equals(e.mKey)) {
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[index] = e.mNext;
                    }
                    mCount--;

                    e.setValue(null);
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }
        else {
            for (Entry e = tab[0], prev = null; e != null; e = e.mNext) {
                Object entryValue = e.getValue();

                if (entryValue == null) {
                    // Clean up after a cleared Reference.
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[0] = e.mNext;
                    }
                    mCount--;
                }
                else if (e.mKey == null) {
                    mModCount++;
                    if (prev != null) {
                        prev.mNext = e.mNext;
                    }
                    else {
                        tab[0] = e.mNext;
                    }
                    mCount--;

                    e.setValue(null);
                    return (entryValue instanceof Null) ? null : entryValue;
                }
                else {
                    prev = e;
                }
            }
        }

        return null;
    }
    
    /**
     * Copies all of the mappings from the specified map to this one.
     * 
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified Map.
     *
     * @param t Mappings to be stored in this map.
     */
    public void putAll(Map t) {
        Iterator i = t.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        Entry tab[] = mTable;
        mModCount++;
        for (int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        mCount = 0;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    public Object clone() {
        try { 
            SoftHashMap t = (SoftHashMap)super.clone();
            t.mTable = new Entry[mTable.length];
            for (int i = mTable.length ; i-- > 0 ; ) {
                t.mTable[i] = (mTable[i] != null) 
                    ? (Entry)mTable[i].clone() : null;
            }
            t.mKeySet = null;
            t.mEntrySet = null;
            t.mValues = null;
            t.mModCount = 0;
            return t;
        }
        catch (CloneNotSupportedException e) { 
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
    
    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet() {
        if (mKeySet == null) {
            mKeySet = new AbstractSet() {
                public Iterator iterator() {
                    return getHashIterator(IdentityMap.KEYS);
                }
                public int size() {
                    return mCount;
                }
                public boolean contains(Object o) {
                    return containsKey(o);
                }
                public boolean remove(Object o) {
                    if (o == null) {
                        if (SoftHashMap.this.containsKey(null)) {
                            SoftHashMap.this.remove(null);
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                    else {
                        return SoftHashMap.this.remove(o) != null;
                    }
                }
                public void clear() {
                    SoftHashMap.this.clear();
                }
                public String toString() {
                    return IdentityMap.toString(this);
                }
            };
        }
        return mKeySet;
    }
    
    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values() {
        if (mValues==null) {
            mValues = new AbstractCollection() {
                public Iterator iterator() {
                    return getHashIterator(IdentityMap.VALUES);
                }
                public int size() {
                    return mCount;
                }
                public boolean contains(Object o) {
                    return containsValue(o);
                }
                public void clear() {
                    SoftHashMap.this.clear();
                }
                public String toString() {
                    return IdentityMap.toString(this);
                }
            };
        }
        return mValues;
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    public Set entrySet() {
        if (mEntrySet==null) {
            mEntrySet = new AbstractSet() {
                public Iterator iterator() {
                    return getHashIterator(IdentityMap.ENTRIES);
                }

                public boolean contains(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)o;
                    Object key = entry.getKey();

                    Entry tab[] = mTable;
                    int hash = key == null ? 0 : key.hashCode();
                    int index = (hash & 0x7FFFFFFF) % tab.length;

                    for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                        Object entryValue = e.getValue();
                        
                        if (entryValue == null) {
                            // Clean up after a cleared Reference.
                            mModCount++;
                            if (prev != null) {
                                prev.mNext = e.mNext;
                            }
                            else {
                                tab[index] = e.mNext;
                            }
                            mCount--;
                        }
                        else if (e.mHash == hash && e.equals(entry)) {
                            return true;
                        }
                        else {
                            prev = e;
                        }
                    }

                    return false;
                }

                public boolean remove(Object o) {
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)o;
                    Object key = entry.getKey();
                    Entry tab[] = mTable;
                    int hash = key == null ? 0 : key.hashCode();
                    int index = (hash & 0x7FFFFFFF) % tab.length;

                    for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                        Object entryValue = e.getValue();
                        
                        if (entryValue == null) {
                            // Clean up after a cleared Reference.
                            mModCount++;
                            if (prev != null) {
                                prev.mNext = e.mNext;
                            }
                            else {
                                tab[index] = e.mNext;
                            }
                            mCount--;
                        }
                        else if (e.mHash == hash && e.equals(entry)) {
                            mModCount++;
                            if (prev != null) {
                                prev.mNext = e.mNext;
                            }
                            else {
                                tab[index] = e.mNext;
                            }
                            mCount--;

                            e.setValue(null);
                            return true;
                        }
                        else {
                            prev = e;
                        }
                    }
                    return false;
                }

                public int size() {
                    return mCount;
                }
                
                public void clear() {
                    SoftHashMap.this.clear();
                }

                public String toString() {
                    return IdentityMap.toString(this);
                }
            };
        }
        
        return mEntrySet;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator it = entrySet().iterator();
        
        buf.append("{");
        for (int i = 0; it.hasNext(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            Map.Entry e = (Map.Entry)it.next();
            buf.append(e.getKey() + "=" + e.getValue());
        }
        buf.append("}");
        return buf.toString();
    }

    private Iterator getHashIterator(int type) {
        if (mCount == 0) {
            return IdentityMap.cEmptyHashIterator;
        }
        else {
            return new HashIterator(type);
        }
    }

    /**
     * HashMap collision list entry.
     */
    private static class Entry implements Map.Entry {
        int mHash;
        Object mKey;
        Entry mNext;
        
        private Reference mValue;

        Entry(int hash, Object key, Object value, Entry next) {
            mHash = hash;
            mKey = key;
            mValue = new SoftReference(value);
            mNext = next;
        }
        
        private Entry(int hash, Object key, Reference value, Entry next) {
            mHash = hash;
            mKey = key;
            mValue = value;
            mNext = next;
        }
        
        protected Object clone() {
            return new Entry(mHash, mKey, (Reference)mValue,
                             (mNext==null ? null : (Entry)mNext.clone()));
        }
        
        // Map.Entry Ops 
        
        public Object getKey() {
            return mKey;
        }
        
        public Object getValue() {
            return mValue.get();
        }
        
        public Object setValue(Object value) {
            Object oldValue = getValue();
            if (value == null) {
                mValue.clear();
            }
            else {
                mValue = new SoftReference(value);
            }
            return oldValue;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry)o;

            Object value = getValue();
            
            return (mKey==null ? e.getKey()==null : mKey.equals(e.getKey())) &&
                (value==null ? e.getValue()==null : value.equals(e.getValue()));
        }

        public int hashCode() {
            Object value = getValue();
            return mHash ^ (value==null ? 0 : value.hashCode());
        }
        
        public String toString() {
            return mKey + "=" + getValue();
        }
    }

    private class HashIterator implements Iterator {
        private Entry[] mTable = SoftHashMap.this.mTable;
        private int mIndex = mTable.length;
        private Entry mEntry;
        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the value. Its existence will prevent the soft
        // value from being cleared.
        private Object mEntryValue;
        private Entry mLastReturned;
        private int mType;
        
        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = mModCount;
        
        HashIterator(int type) {
            mType = type;
        }
        
        public boolean hasNext() {
            while (mEntry == null ||
                   (mEntryValue = mEntry.getValue()) == null) {
                if (mEntry != null) {
                    // Clean up after a cleared Reference.
                    remove(mEntry);
                    mEntry = mEntry.mNext;
                }

                if (mEntry == null) {
                    if (mIndex <= 0) {
                        return false;
                    }
                    else {
                        mEntry = mTable[--mIndex];
                    }
                }
            }

            return true;
        }
        
        public Object next() {
            if (mModCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            mLastReturned = mEntry;
            mEntry = mEntry.mNext;

            if (mType == IdentityMap.KEYS) {
                return mLastReturned.getKey();
            }
            else if (mType == IdentityMap.VALUES) {
                Object value = mLastReturned.getValue();
                return (value instanceof Null) ? null : value;
            }
            else {
                return mLastReturned;
            }
        }
        
        public void remove() {
            if (mLastReturned == null) {
                throw new IllegalStateException();
            }
            if (mModCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            remove(mLastReturned);
            mLastReturned = null;
        }

        private void remove(Entry toRemove) {
            Entry[] tab = mTable;
            int index = (toRemove.mHash & 0x7FFFFFFF) % tab.length;
            
            for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
                if (e == toRemove) {
                    mModCount++;
                    expectedModCount++;
                    if (prev == null) {
                        tab[index] = e.mNext;
                    }
                    else {
                        prev.mNext = e.mNext;
                    }
                    mCount--;
                    return;
                }
                else {
                    prev = e;
                }
            }
            throw new ConcurrentModificationException();
        }
    }

    /**
     * This allows null references to be saved into SoftHashMap and allow
     * entries to still be garbage collected.
     */
    static class Null {
        public boolean equals(Object other) {
            return other == null || other instanceof Null;
        }

        public String toString() {
            return "null";
        }
    }
}
