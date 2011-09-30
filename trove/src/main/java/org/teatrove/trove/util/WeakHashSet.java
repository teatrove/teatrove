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
 * A Set that weakly references its elements and can be used as a simple cache.
 * WeakHashSet is not thread-safe and must be wrapped with
 * Collections.synchronizedSet to be made thread-safe. Most of the
 * implementation for this class is ripped off from org.teatrove.trove.SoftHashMap
 * <p>
 * Note: Softly referenced entries may be automatically removed during
 * either accessor or mutator operations, possibly causing a concurrent
 * modification to be detected. Therefore, even if multiple threads are only
 * accessing this map, be sure to synchronize this map first. Also, do not
 * rely on the value returned by size() when using an iterator from this map.
 * The iterators may return less entries than the amount reported by size().
 *
 * @author Jonathan Yam
 * @author Brian S O'Neill
 */
public class WeakHashSet extends AbstractSet implements Set, Cloneable {
    /**
     * Test program.
     */
    /*
    public static void main(String[] arg) throws Exception {

        Set cache = new WeakHashSet(10);

        for (int i = 0, j = 0; i < 100000; i++, j += 15) {
            //if (i % 100 == 0) {
            //    System.out.println("Size = " + cache.size());
            //}

            //Thread.sleep(1);

            Integer value = new Integer(j);

            cache.add(value);
        }

        //Object entry = cache.iterator().next();
        //System.out.println(entry);
        //entry = null;

        //System.out.println(cache);

        int originalSize = cache.size();

        //cache = null;

        //for (int i=0; i<100; i++) {
            System.gc();
        //}

        //System.out.println(cache);

        System.out.println(originalSize);
        int newSize = 0;
        for (Iterator i = cache.iterator(); i.hasNext();) {
            i.next();
            newSize++;
        }
        System.out.println(newSize);
        //System.out.println(entry);

        Set set = new WeakHashSet();
        System.out.println(""+set.add("hello"));
        System.out.println(""+set.add("hello"));
        System.out.println(""+set.contains("hello"));
        System.out.println(""+set.remove("hello"));
        System.out.println(""+set.remove("hello"));
        System.out.println(""+set.contains("hello"));
        System.out.println(""+set.add(null));
        System.out.println(""+set.add(null));
        System.out.println(""+set.contains(null));
        System.out.println(""+set.remove(null));
        System.out.println(""+set.remove(null));
        System.out.println(""+set.contains(null));

        Thread.sleep(1000000);
    }
    */

    /**
     * Null object to use for hashing elements that are <tt>null</tt>.
     */
    private static final Null NULL = new Null();

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


    /**
     * Constructs a new, empty map with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the HashMap.
     * @param      loadFactor        the load factor of the HashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public WeakHashSet(int initialCapacity, float loadFactor) {
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
    public WeakHashSet(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public WeakHashSet() {
        this(11, 0.75f);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The capacity of the backing <tt>HashMap</tt> instance is
     * twice the size of the specified collection or eleven (whichever is
     * greater), and the default load factor (which is <tt>0.75</tt>) is used.
     *
     * @param c the collection whose elements are to be placed into this set.
     */
    public WeakHashSet(Collection c) {
        this(Math.max(2 * c.size(), 11), 0.75f);
        addAll(c);
    }

    /**
     * Returns an iterator over the elements in this set.  The elements
     * are returned in no particular order.
     *
     * @return an Iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    public Iterator iterator() {
        return new SetIterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        return mCount;
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        return mCount == 0;
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     *
     * @param obj element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            obj = NULL;
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
                // It's in the Set
                return true;
            }
            else {
                prev = e;
            }
        }

        return false;
    }

    /**
     * Scans the contents of this map, removing all entries that have a
     * cleared weak value.
     */
    private void cleanup() {
        Entry tab[] = mTable;

        for (int i = tab.length ; i-- > 0 ;) {
            for (Entry e = tab[i], prev = null; e != null; e = e.mNext) {
                if (e.get() == null) {
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
        Entry oldSet[] = mTable;

        int newCapacity = oldCapacity * 2 + 1;
        Entry newSet[] = new Entry[newCapacity];

        mModCount++;
        mThreshold = (int)(newCapacity * mLoadFactor);
        mTable = newSet;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry old = oldSet[i] ; old != null ; ) {
                Entry e = old;
                old = old.mNext;

                // Only copy entry if its value hasn't been cleared.
                if (e.get() == null) {
                    mCount--;
                }
                else {
                    int index = (e.mHash & 0x7FFFFFFF) % newCapacity;
                    e.mNext = newSet[index];
                    newSet[index] = e;
                }
            }
        }
    }

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *
     * @param obj element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     */
    public boolean add(Object obj) {
        if (obj == null) {
            obj = NULL;
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
                // Already in set.
                return false;
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
        tab[index] = new Entry((obj == NULL ? new Null() : obj), hash, tab[index]);
        mCount++;
        return true;
    }

    /**
     * Removes the given element from this set if it is present.
     *
     * @param obj object to be removed from this set, if present.
     * @return <tt>true</tt> if the set contained the specified element.
     */
    public boolean remove(Object obj) {
        Entry tab[] = mTable;

        if (obj == null) {
            obj = NULL;
        }

        int hash = obj.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;

        for (Entry e = tab[index], prev = null; e != null; e = e.mNext) {
            Object entryValue = e.get();

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
            else if (e.mHash == hash) {
                mModCount++;
                if (prev != null) {
                    prev.mNext = e.mNext;
                }
                else {
                    tab[index] = e.mNext;
                }
                mCount--;

                return true;
            }
            else {
                prev = e;
            }
        }

        return false;
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
     * Returns a shallow copy of this <tt>HashSet</tt> instance: the elements
     * themselves are not cloned.
     *
     * @return a shallow copy of this set.
     */
    public Object clone() {
        try {
            WeakHashSet t = (WeakHashSet)super.clone();
            t.mTable = new Entry[mTable.length];
            for (int i = mTable.length ; i-- > 0 ; ) {
                t.mTable[i] = (mTable[i] != null)
                    ? (Entry)mTable[i].clone() : null;
            }
            t.mModCount = 0;
            return t;
        }
        catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }


    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator it = iterator();
        buf.append("[");
        for (int i = 0; it.hasNext(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(String.valueOf(it.next()));
        }
        buf.append("]");
        return buf.toString();
    }

    protected int hashCode(Object obj) {
        return obj.hashCode();
    }

    protected boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    private static class Entry extends WeakReference {
        int mHash;
        Entry mNext;

        Entry(Object obj, int hash, Entry next) {
            super(obj);
            mHash = hash;
            mNext = next;
        }

        protected Object clone() {
            return new Entry(get(), mHash,
                             (mNext==null ? null : (Entry)mNext.clone()));
        }

    }

    private class SetIterator implements Iterator {
        private Entry[] mTable = WeakHashSet.this.mTable;
        private int mIndex = mTable.length;
        private Entry mEntry;
        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the value. Its existence will prevent the weak
        // reference from being cleared.
        private Object mEntryValue;
        private Entry mLastReturned;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = mModCount;

        public boolean hasNext() {
            while (mEntry == null ||
                   (mEntryValue = mEntry.get()) == null) {
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

            mLastReturned = mEntry;
            mEntry = mEntry.mNext;
            return mEntryValue;
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
     * This allows null references to be saved into WeakHashSet and allow
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
