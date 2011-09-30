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
 * A thread-safe Set that manages flyweights: sharable objects that are usually
 * immutable. Call the {@link #get get} method for supplying the FlyweightSet
 * with candidate flyweight instances.
 * <p>
 * Objects that do not customize the hashCode and equals methods don't make
 * sense to use as flyweights because each instance will be considered unique.
 * The object returned from the {@link #get get} method will always be the same
 * as the one passed in.
 *
 * @author Brian S O'Neill
 * @see Utils#intern
 */
public class FlyweightSet extends AbstractSet {
    // This implementation is basically a stripped down version of
    // IdentityMap in which the entries contain only a referenced key and
    // no value.
    
    private Entry mTable[];
    private int mCount;
    private int mThreshold;
    private float mLoadFactor;
    
    public FlyweightSet() {
        final int initialCapacity = 101;
        final float loadFactor = 0.75f;
        mLoadFactor = loadFactor;
        mTable = new Entry[initialCapacity];
        mThreshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Pass in a candidate flyweight object and get a unique instance from this
     * set. The returned object will always be of the same type as that passed
     * in. If the object passed in does not equal any object currently in the
     * set, it will be added to the set, becoming a flyweight.
     *
     * @param obj candidate flyweight; null is also accepted
     */    
    public synchronized Object put(Object obj) {
        // This implementation is based on the IdentityMap.put method.
        
        if (obj == null) {
            return null;
        }
        
        Entry tab[] = mTable;
        int hash = hashCode(obj);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
            Object iobj = e.get();
            if (iobj == null) {
                // Clean up after a cleared Reference.
                if (prev != null) {
                    prev.mNext = e.mNext;
                }
                else {
                    tab[index] = e.mNext;
                }
                mCount--;
            }
            else if (e.mHash == hash &&
                     obj.getClass() == iobj.getClass() &&
                     equals(obj, iobj)) {
                // Found flyweight instance.
                return iobj;
            }
            else {
                prev = e;
            }
        }
        
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
        
        // Create a new entry.
        tab[index] = new Entry(obj, hash, tab[index]);
        mCount++;
        return obj;
    }
    
    public Iterator iterator() {
        return new SetIterator();
    }

    public int size() {
        return mCount;
    }

    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        
        Entry tab[] = mTable;
        int hash = hashCode(obj);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
            Object iobj = e.get();
            if (iobj == null) {
                // Clean up after a cleared Reference.
                if (prev != null) {
                    prev.mNext = e.mNext;
                }
                else {
                    tab[index] = e.mNext;
                }
                mCount--;
            }
            else if (e.mHash == hash &&
                     obj.getClass() == iobj.getClass() &&
                     equals(obj, iobj)) {
                // Found flyweight instance.
                return true;
            }
            else {
                prev = e;
            }
        }

        return false;
    }

    public String toString() {
        return IdentityMap.toString(this);
    }

    protected int hashCode(Object obj) {
        return obj.hashCode();
    }

    protected boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    private void cleanup() {
        Entry tab[] = mTable;
        for (int i = tab.length; i-- > 0; ) {
            for (Entry e = tab[i], prev = null; e != null; e = e.mNext) {
                if (e.get() == null) {
                    // Clean up after a cleared Reference.
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
    
    private void rehash() {
        int oldCapacity = mTable.length;
        Entry[] tab = mTable;
        
        int newCapacity = oldCapacity * 2 + 1;
        Entry[] newTab = new Entry[newCapacity];
        
        mThreshold = (int)(newCapacity * mLoadFactor);
        mTable = newTab;
        
        for (int i = oldCapacity; i-- > 0; ) {
            for (Entry old = tab[i]; old != null; ) {
                Entry e = old;
                old = old.mNext;
                
                // Only copy entry if it hasn't been cleared.
                if (e.get() == null) {
                    mCount--;
                }
                else {
                    int index = (e.mHash & 0x7FFFFFFF) % newCapacity;
                    e.mNext = newTab[index];
                    newTab[index] = e;
                }
            }
        }
    }
    
    private static class Entry extends WeakReference {
        int mHash;
        Entry mNext;
        
        Entry(Object flyweight, int hash, Entry next) {
            super(flyweight);
            mHash = hash;
            mNext = next;
        }
    }

    private class SetIterator implements Iterator {
        private Entry[] mTable = FlyweightSet.this.mTable;
        private int mIndex = mTable.length;
        private Entry mEntry;
        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the flyweight. Its existence will prevent the weak
        // reference from being cleared.
        private Object mEntryFlyweight;
        
        public boolean hasNext() {
            while (mEntry == null ||
                   (mEntryFlyweight = mEntry.get()) == null) {
                if (mEntry != null) {
                    // Skip past a cleared Reference.
                    mEntry = mEntry.mNext;
                }
                else {
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
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            mEntry = mEntry.mNext;
            return mEntryFlyweight;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
