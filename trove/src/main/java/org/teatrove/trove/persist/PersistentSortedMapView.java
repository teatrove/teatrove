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

package org.teatrove.trove.persist;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.io.*;

/**
 * A {@link PersistentSortedMap} implementation view of a
 * {@link PersistentSortedMapKernel}. With this class, persistent maps may
 * be implemented with less effort: this class takes care of all the details
 * in managing views.
 *
 * @author Brian S O'Neill
 */
public class PersistentSortedMapView extends AbstractPersistentMap
    implements PersistentSortedMap
{
    static final Object NO_KEY = PersistentSortedMapKernel.NO_KEY;

    static void checkSubMapKeyOrder(Comparator c,
                                    Object fromKey, Object toKey) {
        int result;
        if (c == null) {
            result = ((Comparable)fromKey).compareTo(toKey);
        }
        else {
            result = c.compare(fromKey, toKey);
        }
        if (result > 0) {
            throw new IllegalArgumentException
                ("From key is greater than to key");
        }
    }

    static int subSize(PersistentSortedMapKernel kernel,
                       Object fromKey, Object toKey)
        throws IOException
    {
        int size = 0;
        PersistentIterator it = new KeyIterator(kernel, fromKey, toKey);
        while (it.hasNext()) {
            size++;
        }
        return size;
    }

    /**
     * Return 0 if the test key is in the range, -1 if too low, 1 if exactly
     * the same as toKey, and 2 if higher.
     */
    static int rangeCheck(Comparator c,
                          Object fromKey, Object toKey, Object testKey) {
        if (fromKey != NO_KEY) {
            int result;
            if (c == null) {
                result = ((Comparable)fromKey).compareTo(testKey);
            }
            else {
                result = c.compare(fromKey, testKey);
            }
            if (result > 0) {
                return -1;
            }
        }
        if (toKey != NO_KEY) {
            int result;
            if (c == null) {
                result = ((Comparable)toKey).compareTo(testKey);
            }
            else {
                result = c.compare(toKey, testKey);
            }
            if (result <= 0) {
                return result == 0 ? 1 : 2;
            }
        }
        return 0;
    }

    static String rangeError(int value) {
        return value < 0 ? "too low" : "too high";
    }

    protected final PersistentSortedMapKernel mKernel;

    private transient PersistentSet mKeySet;
    private transient PersistentSet mEntrySet;

    public PersistentSortedMapView(PersistentSortedMapKernel kernel) {
        super(kernel.lock());
        mKernel = kernel;
    }

    public Comparator comparator() {
        return mKernel.comparator();
    }

    public int size() throws IOException {
        return mKernel.size();
    }

    public boolean isEmpty() throws IOException {
        return mKernel.isEmpty();
    }

    public boolean containsKey(Object key) throws IOException {
        return mKernel.containsKey(key);
    }

    public boolean containsValue(Object value) throws IOException {
        return mKernel.containsValue(value);
    }

    public Object get(Object key) throws IOException {
        return mKernel.get(key);
    }

    public Object put(Object key, Object value) throws IOException {
        return mKernel.put(key, value);
    }

    public Object remove(Object key) throws IOException {
        return mKernel.remove(key);
    }

    public Object firstKey() throws IOException {
        Object first = mKernel.firstKey();
        if (first == NO_KEY) {
            throw new NoSuchElementException();
        }
        return first;
    }

    public Object lastKey() throws IOException {
        Object last = mKernel.lastKey();
        if (last == NO_KEY) {
            throw new NoSuchElementException();
        }
        return last;
    }

    public void clear() throws IOException {
        mKernel.clear();
    }

    /**
     * The returned key set is a {@link PersistentSortedSet}, as well as its
     * views.
     */
    public PersistentSet keySet() throws IOException {
        if (mKeySet == null) {
            mKeySet = new KeySet(mKernel);
        }
        return mKeySet;
    }

    public PersistentSet entrySet() throws IOException {
        if (mEntrySet == null) {
            mEntrySet = new EntrySet(mKernel);
        }
        return mEntrySet;
    }

    public PersistentSortedMap subMap(Object fromKey, Object toKey)
        throws IOException
    {
        checkSubMapKeyOrder(mKernel.comparator(), fromKey, toKey);
        return new SubView(mKernel, fromKey, toKey);
    }

    public PersistentSortedMap headMap(Object toKey) throws IOException {
        return new SubView(mKernel, NO_KEY, toKey);
    }

    public PersistentSortedMap tailMap(Object fromKey) throws IOException {
        return new SubView(mKernel, fromKey, NO_KEY);
    }

    private static class SubView extends AbstractPersistentMap
        implements PersistentSortedMap
    {
        private final PersistentSortedMapKernel mKernel;

        // Inclusive.
        private final Object mFromKey;
        // Exclusive.
        private final Object mToKey;

        private transient PersistentSet mKeySet;
        private transient PersistentSet mEntrySet;

        SubView(PersistentSortedMapKernel kernel,
                Object fromKey, Object toKey)
        {
            super(kernel.lock());
            mKernel = kernel;
            mFromKey = fromKey;
            mToKey = toKey;
        }

        public Comparator comparator() {
            return mKernel.comparator();
        }

        public int size() throws IOException {
            return subSize(mKernel, mFromKey, mToKey);
        }

        public boolean containsKey(Object key) throws IOException {
            return rangeCheck(key) == 0 ? mKernel.containsKey(key) : false;
        }
        
        public Object get(Object key) throws IOException {
            return rangeCheck(key) == 0 ? mKernel.get(key) : null;
        }
        
        public Object put(Object key, Object value) throws IOException {
            int rangeCheck = rangeCheck(key);
            if (rangeCheck != 0) {
                throw new IllegalArgumentException
                    ("Key out of range: " + rangeError(rangeCheck));
            }
            return mKernel.put(key, value);
        }
        
        public Object remove(Object key) throws IOException {
            return rangeCheck(key) == 0 ? mKernel.remove(key) : null;
        }

        public void clear() throws IOException {
            mKernel.clear(mFromKey, mToKey);
        }

        public PersistentSet keySet() throws IOException {
            if (mKeySet == null) {
                mKeySet = new SubKeySet(mKernel, mFromKey, mToKey);
            }
            return mKeySet;
        }

        public PersistentSet entrySet() throws IOException {
            if (mEntrySet == null) {
                mEntrySet = new SubEntrySet(mKernel, mFromKey, mToKey);
            }
            return mEntrySet;
        }

        public PersistentSortedMap subMap(Object fromKey, Object toKey)
            throws IOException
        {
            checkSubMapKeyOrder(mKernel.comparator(), fromKey, toKey);
            int rangeCheck = rangeCheck(fromKey);
            if (rangeCheck != 0) {
                throw new IllegalArgumentException
                    ("'From' key out of range: " + rangeError(rangeCheck));
            }
            rangeCheck = rangeCheck(toKey);
            if (rangeCheck != 0 && rangeCheck != 1) {
                throw new IllegalArgumentException
                    ("'To' key out of range: " + rangeError(rangeCheck));
            }
            return new SubView(mKernel, fromKey, toKey);
        }
        
        public PersistentSortedMap headMap(Object toKey) throws IOException {
            int rangeCheck = rangeCheck(toKey);
            if (rangeCheck != 0) {
                throw new IllegalArgumentException
                    ("'To' key out of range: " + rangeError(rangeCheck));
            }
            return new SubView(mKernel, mFromKey, toKey);
        }
        
        public PersistentSortedMap tailMap(Object fromKey) throws IOException {
            int rangeCheck = rangeCheck(fromKey);
            if (rangeCheck != 0 && rangeCheck != 1) {
                throw new IllegalArgumentException
                    ("'From' key out of range: " + rangeError(rangeCheck));
            }
            return new SubView(mKernel, fromKey, mToKey);
        }
        
        public Object firstKey() throws IOException {
            Object first;
            if (mFromKey == NO_KEY) {
                first = mKernel.firstKey();
            }
            else if (mKernel.containsKey(mFromKey)) {
                return mFromKey;
            }
            else {
                first = mKernel.nextKey(mFromKey);
            }
            if (first == NO_KEY) {
                throw new NoSuchElementException();
            }
            return first;
        }
        
        public Object lastKey() throws IOException {
            if (mToKey == NO_KEY) {
                return mKernel.lastKey();
            }
            Object last = mKernel.previousKey(mToKey);
            if (last == NO_KEY) {
                throw new NoSuchElementException();
            }
            return last;
        }

        private int rangeCheck(Object key) {
            return PersistentSortedMapView.rangeCheck
                (mKernel.comparator(), mFromKey, mToKey, key);
        }
    }

    private static class KeySet extends AbstractPersistentSet
        implements PersistentSortedSet
    {
        private final PersistentSortedMapKernel mKernel;

        KeySet(PersistentSortedMapKernel kernel) {
            super(kernel.lock());
            mKernel = kernel;
        }

        public Comparator comparator() {
            return mKernel.comparator();
        }

        public int size() throws IOException {
            return mKernel.size();
        }

        public boolean isEmpty() throws IOException {
            return mKernel.isEmpty();
        }
        
        public PersistentIterator iterator() throws IOException {
            return new KeyIterator(mKernel);
        }

        public PersistentIterator reverseIterator() throws IOException {
            return new ReverseKeyIterator(mKernel);
        }

        public boolean contains(Object key) throws IOException {
            return mKernel.containsKey(key);
        }
        
        public boolean remove(Object key) throws IOException {
            if (mKernel.containsKey(key)) {
                mKernel.remove(key);
                return true;
            }
            return false;
        }

        public void clear() throws IOException {
            mKernel.clear();
        }

        public Object[] toArray() throws IOException {
            int size = size();
            Object[] a = new Object[size];
            if (size == 0) {
                return a;
            }
            mKernel.copyKeysInto(a, 0);
            return a;
        }
        
        public Object[] toArray(Object[] a) throws IOException {
            int size = size();
            if (a.length < size) {
                a = (Object[])java.lang.reflect.Array.newInstance
                    (a.getClass().getComponentType(), size);
            }
            if (size == 0) {
                return a;
            }
            mKernel.copyKeysInto(a, 0);
            return a;
        }

        public PersistentSortedSet subSet(Object from, Object to)
            throws IOException
        {
            checkSubMapKeyOrder(mKernel.comparator(), from, to);
            return new SubKeySet(mKernel, from, to);
        }

        public PersistentSortedSet headSet(Object to)
            throws IOException
        {
            return new SubKeySet(mKernel, NO_KEY, to);
        }

        public PersistentSortedSet tailSet(Object from)
            throws IOException
        {
            return new SubKeySet(mKernel, from, NO_KEY);
        }

        public Object first() throws IOException {
            Object first = mKernel.firstKey();
            if (first == NO_KEY) {
                throw new NoSuchElementException();
            }
            return first;
        }

        public Object last() throws IOException {
            Object last = mKernel.lastKey();
            if (last == NO_KEY) {
                throw new NoSuchElementException();
            }
            return last;
        }
    }

    private static class SubKeySet extends AbstractPersistentSet
        implements PersistentSortedSet
    {
        private final PersistentSortedMapKernel mKernel;

        // Inclusive.
        private final Object mFromKey;
        // Exclusive.
        private final Object mToKey;

        SubKeySet(PersistentSortedMapKernel kernel,
                  Object fromKey, Object toKey)
        {
            super(kernel.lock());
            mKernel = kernel;
            mFromKey = fromKey;
            mToKey = toKey;
        }

        public Comparator comparator() {
            return mKernel.comparator();
        }

        public int size() throws IOException {
            return subSize(mKernel, mFromKey, mToKey);
        }

        public PersistentIterator iterator() throws IOException {
            return new KeyIterator(mKernel, mFromKey, mToKey);
        }

        public PersistentIterator reverseIterator() throws IOException {
            return new ReverseKeyIterator(mKernel, mFromKey, mToKey);
        }

        public boolean contains(Object key) throws IOException {
            return rangeCheck(key) == 0 ? mKernel.containsKey(key) : false;
        }
        
        public boolean remove(Object key) throws IOException {
            if (rangeCheck(key) == 0 && mKernel.containsKey(key)) {
                mKernel.remove(key);
                return true;
            }
            return false;
        }

        public void clear() throws IOException {
            mKernel.clear(mFromKey, mToKey);
        }

        public PersistentSortedSet subSet(Object from, Object to)
            throws IOException
        {
            checkSubMapKeyOrder(mKernel.comparator(), from, to);
            int rangeCheck = rangeCheck(from);
            if (rangeCheck != 0) {
                throw new IllegalArgumentException
                    ("'From' element out of range: " + rangeError(rangeCheck));
            }
            rangeCheck = rangeCheck(to);
            if (rangeCheck != 0 && rangeCheck != 1) {
                throw new IllegalArgumentException
                    ("'To' element out of range: " + rangeError(rangeCheck));
            }
            return new SubKeySet(mKernel, from, to);
        }

        public PersistentSortedSet headSet(Object to)
            throws IOException
        {
            int rangeCheck = rangeCheck(to);
            if (rangeCheck != 0 && rangeCheck != 1) {
                throw new IllegalArgumentException
                    ("'To' element out of range: " + rangeError(rangeCheck));
            }
            return new SubKeySet(mKernel, mFromKey, to);
        }

        public PersistentSortedSet tailSet(Object from)
            throws IOException
        {
            int rangeCheck = rangeCheck(from);
            if (rangeCheck != 0) {
                throw new IllegalArgumentException
                    ("'From' element out of range: " + rangeError(rangeCheck));
            }
            return new SubKeySet(mKernel, from, mToKey);
        }

        public Object first() throws IOException {
            Object first;
            if (mFromKey == NO_KEY) {
                first = mKernel.firstKey();
            }
            else if (mKernel.containsKey(mFromKey)) {
                return mFromKey;
            }
            first = mKernel.nextKey(mFromKey);
            if (first == NO_KEY) {
                throw new NoSuchElementException();
            }
            return first;
        }

        public Object last() throws IOException {
            if (mToKey == NO_KEY) {
                return mKernel.lastKey();
            }
            Object last = mKernel.previousKey(mToKey);
            if (last == NO_KEY) {
                throw new NoSuchElementException();
            }
            return last;
        }

        private int rangeCheck(Object key) {
            return PersistentSortedMapView.rangeCheck
                (mKernel.comparator(), mFromKey, mToKey, key);
        }
    }

    private static class EntrySet extends AbstractPersistentSet {
        private final PersistentSortedMapKernel mKernel;

        EntrySet(PersistentSortedMapKernel kernel) {
            super(kernel.lock());
            mKernel = kernel;
        }

        public int size() throws IOException {
            return mKernel.size();
        }

        public boolean isEmpty() throws IOException {
            return mKernel.isEmpty();
        }

        public PersistentIterator iterator() throws IOException {
            return new EntryIterator(mKernel);
        }

        public PersistentIterator reverseIterator() throws IOException {
            return new ReverseEntryIterator(mKernel);
        }

        public boolean contains(Object o) throws IOException {
            if (!(o instanceof PersistentMap.Entry)) {
                return false;
            }
            PersistentMap.Entry e = (PersistentMap.Entry)o;
            Object found = mKernel.get(e.getKey());
            if (found != null) {
                return found.equals(e.getValue());
            }
            if (e.getValue() != null) {
                return false;
            }
            return mKernel.containsKey(e.getKey());
        }
        
        public boolean remove(Object o) throws IOException {
            if (!(o instanceof PersistentMap.Entry)) {
                return false;
            }
            PersistentMap.Entry e = (PersistentMap.Entry)o;
            Object found = mKernel.get(e.getKey());
            if ((found != null && found.equals(e.getValue())) ||
                (e.getValue() == null && mKernel.containsKey(e.getKey()))) {

                mKernel.remove(e.getKey());
                return true;
            }
            return false;
        }

        public void clear() throws IOException {
            mKernel.clear();
        }

        public final Object[] toArray() throws IOException {
            int size = size();
            Object[] a = new Object[size];
            if (size == 0) {
                return a;
            }
            mKernel.copyEntriesInto(a, 0);
            return a;
        }
        
        public final Object[] toArray(Object[] a) throws IOException {
            int size = size();
            if (a.length < size) {
                a = (Object[])java.lang.reflect.Array.newInstance
                    (a.getClass().getComponentType(), size);
            }
            if (size == 0) {
                return a;
            }
            mKernel.copyEntriesInto(a, 0);
            return a;
        }
    }

    private static class SubEntrySet extends AbstractPersistentSet {
        private final PersistentSortedMapKernel mKernel;

        // Inclusive.
        private final Object mFromKey;
        // Exclusive.
        private final Object mToKey;

        SubEntrySet(PersistentSortedMapKernel kernel,
                    Object fromKey, Object toKey)
        {
            super(kernel.lock());
            mKernel = kernel;
            mFromKey = fromKey;
            mToKey = toKey;
        }

        public Comparator comparator() {
            return mKernel.comparator();
        }

        public int size() throws IOException {
            return subSize(mKernel, mFromKey, mToKey);
        }

        public PersistentIterator iterator() throws IOException {
            return new EntryIterator(mKernel, mFromKey, mToKey);
        }

        public PersistentIterator reverseIterator() throws IOException {
            return new ReverseEntryIterator(mKernel, mFromKey, mToKey);
        }

        public boolean contains(Object o) throws IOException {
            if (!(o instanceof PersistentMap.Entry)) {
                return false;
            }
            PersistentMap.Entry e = (PersistentMap.Entry)o;
            if (rangeCheck(e.getKey()) != 0) {
                return false;
            }
            Object found = mKernel.get(e.getKey());
            if (found != null) {
                return found.equals(e.getValue());
            }
            if (e.getValue() != null) {
                return false;
            }
            return mKernel.containsKey(e.getKey());
        }
        
        public boolean remove(Object o) throws IOException {
            if (!(o instanceof PersistentMap.Entry)) {
                return false;
            }
            PersistentMap.Entry e = (PersistentMap.Entry)o;
            if (rangeCheck(e.getKey()) != 0) {
                return false;
            }
            Object found = mKernel.get(e.getKey());
            if ((found != null && found.equals(e.getValue())) ||
                (e.getValue() == null && mKernel.containsKey(e.getKey()))) {

                mKernel.remove(e.getKey());
                return true;
            }
            return false;
        }

        public void clear() throws IOException {
            mKernel.clear(mFromKey, mToKey);
        }

        private int rangeCheck(Object key) {
            return PersistentSortedMapView.rangeCheck
                (mKernel.comparator(), mFromKey, mToKey, key);
        }
    }

    // Although not the most efficient way of traversing a B-Tree, these
    // iterators behave better under concurrent modification. Most maps require
    // an exclusive lock during iteration, but a large persistent map shouldn't
    // have this restriction. These iterator don't require an exclusive lock to
    // be held.

    private static class KeyIterator implements PersistentIterator {
        private final PersistentSortedMapKernel mKernel;
        private Object mNextKey;
        private final Object mToKey;
        private Object mToRemove = NO_KEY;

        KeyIterator(PersistentSortedMapKernel kernel) throws IOException {
            mKernel = kernel;
            mNextKey = kernel.firstKey();
            mToKey = NO_KEY;
        }

        KeyIterator(PersistentSortedMapKernel kernel,
                    Object fromKey, Object toKey)
            throws IOException
        {
            mKernel = kernel;
            if (fromKey == NO_KEY) {
                mNextKey = kernel.firstKey();
            }
            else if (kernel.containsKey(fromKey)) {
                mNextKey = fromKey;
            }
            else {
                mNextKey = kernel.nextKey(fromKey);
            }
            mToKey = toKey;
            checkEnd();
        }

        public boolean hasNext() throws IOException {
            return mNextKey != NO_KEY;
        }
        
        public Object next() throws IOException {
            if (mNextKey == NO_KEY) {
                throw new NoSuchElementException();
            }
            Object nextKey = mNextKey;
            mNextKey = mKernel.nextKey(nextKey);
            checkEnd();
            return mToRemove = nextKey;
        }
        
        public void remove() throws IOException {
            Object toRemove = mToRemove;
            if (toRemove == NO_KEY) {
                throw new IllegalStateException();
            }
            mToRemove = NO_KEY;
            mKernel.remove(toRemove);
        }

        private void checkEnd() {
            if (mToKey == NO_KEY || mNextKey == NO_KEY) {
                return;
            }
            Comparator c = mKernel.comparator();
            int result;
            if (c == null) {
                result = ((Comparable)mNextKey).compareTo(mToKey);
            }
            else {
                result = c.compare(mNextKey, mToKey);
            }
            if (result >= 0) {
                mNextKey = NO_KEY;
            }
        }
    }

    private static class ReverseKeyIterator implements PersistentIterator {
        private final PersistentSortedMapKernel mKernel;
        private Object mPreviousKey;
        private final Object mFromKey;
        private Object mToRemove = NO_KEY;

        ReverseKeyIterator(PersistentSortedMapKernel kernel)
            throws IOException
        {
            mKernel = kernel;
            mPreviousKey = kernel.lastKey();
            mFromKey = NO_KEY;
        }

        ReverseKeyIterator(PersistentSortedMapKernel kernel,
                           Object fromKey, Object toKey)
            throws IOException
        {
            mKernel = kernel;
            if (toKey == NO_KEY) {
                mPreviousKey = kernel.lastKey();
            }
            else if (mKernel.containsKey(toKey)) {
                mPreviousKey = toKey;
                if (mPreviousKey != NO_KEY) {
                    mPreviousKey = kernel.previousKey(mPreviousKey);
                }
            }
            else {
                mPreviousKey = kernel.previousKey(toKey);
            }
            mFromKey = fromKey;
            checkEnd();
        }

        public boolean hasNext() throws IOException {
            return mPreviousKey != NO_KEY;
        }
        
        public Object next() throws IOException {
            if (mPreviousKey == NO_KEY) {
                throw new NoSuchElementException();
            }
            Object previousKey = mPreviousKey;
            mPreviousKey = mKernel.previousKey(previousKey);
            checkEnd();
            return mToRemove = previousKey;
        }
        
        public void remove() throws IOException {
            Object toRemove = mToRemove;
            if (toRemove == NO_KEY) {
                throw new IllegalStateException();
            }
            mToRemove = NO_KEY;
            mKernel.remove(toRemove);
        }

        private void checkEnd() {
            if (mFromKey == NO_KEY || mPreviousKey == NO_KEY) {
                return;
            }
            Comparator c = mKernel.comparator();
            int result;
            if (c == null) {
                result = ((Comparable)mPreviousKey).compareTo(mFromKey);
            }
            else {
                result = c.compare(mPreviousKey, mFromKey);
            }
            if (result < 0) {
                mPreviousKey = NO_KEY;
            }
        }
    }

    private static class EntryIterator implements PersistentIterator {
        private final PersistentSortedMapKernel mKernel;
        private PersistentMap.Entry mNextEntry;
        private final Object mToKey;
        private Object mToRemove = NO_KEY;

        EntryIterator(PersistentSortedMapKernel kernel) throws IOException {
            mKernel = kernel;
            mNextEntry = kernel.firstEntry();
            mToKey = NO_KEY;
        }

        EntryIterator(PersistentSortedMapKernel kernel,
                      Object fromKey, Object toKey)
            throws IOException
        {
            mKernel = kernel;
            if (fromKey == NO_KEY) {
                mNextEntry = kernel.firstEntry();
            }
            else if (kernel.containsKey(fromKey)) {
                mNextEntry = kernel.getEntry(fromKey);
            }
            else {
                mNextEntry = kernel.nextEntry(fromKey);
            }
            mToKey = toKey;
            checkEnd();
        }

        public boolean hasNext() throws IOException {
            return mNextEntry != null;
        }
        
        public Object next() throws IOException {
            if (mNextEntry == null) {
                throw new NoSuchElementException();
            }
            PersistentMap.Entry nextEntry = mNextEntry;
            mNextEntry = mKernel.nextEntry(nextEntry.getKey());
            checkEnd();
            mToRemove = nextEntry.getKey();
            return nextEntry;
        }
        
        public void remove() throws IOException {
            Object toRemove = mToRemove;
            if (toRemove == NO_KEY) {
                throw new IllegalStateException();
            }
            mToRemove = NO_KEY;
            mKernel.remove(toRemove);
        }

        private void checkEnd() throws IOException {
            if (mToKey == NO_KEY || mNextEntry == null) {
                return;
            }
            Object nextKey = mNextEntry.getKey();
            Comparator c = mKernel.comparator();
            int result;
            if (c == null) {
                result = ((Comparable)nextKey).compareTo(mToKey);
            }
            else {
                result = c.compare(nextKey, mToKey);
            }
            if (result >= 0) {
                mNextEntry = null;
            }
        }
    }

    private static class ReverseEntryIterator implements PersistentIterator {
        private final PersistentSortedMapKernel mKernel;
        private PersistentMap.Entry mPreviousEntry;
        private final Object mFromKey;
        private Object mToRemove = NO_KEY;

        ReverseEntryIterator(PersistentSortedMapKernel kernel)
            throws IOException
        {
            mKernel = kernel;
            mPreviousEntry = kernel.lastEntry();
            mFromKey = NO_KEY;
        }

        ReverseEntryIterator(PersistentSortedMapKernel kernel,
                             Object fromKey, Object toKey)
            throws IOException
        {
            mKernel = kernel;
            if (toKey == NO_KEY) {
                mPreviousEntry = kernel.lastEntry();
            }
            else if (kernel.containsKey(toKey)) {
                mPreviousEntry = kernel.getEntry(toKey);
                if (mPreviousEntry != null) {
                    mPreviousEntry =
                        kernel.previousEntry(mPreviousEntry.getKey());
                }
            }
            else {
                mPreviousEntry = kernel.previousEntry(toKey);
            }
            mFromKey = fromKey;
            checkEnd();
        }

        public boolean hasNext() throws IOException {
            return mPreviousEntry != null;
        }
        
        public Object next() throws IOException {
            if (mPreviousEntry == null) {
                throw new NoSuchElementException();
            }
            PersistentMap.Entry previousEntry = mPreviousEntry;
            mPreviousEntry = mKernel.previousEntry(previousEntry.getKey());
            checkEnd();
            mToRemove = previousEntry.getKey();
            return previousEntry;
        }
        
        public void remove() throws IOException {
            Object toRemove = mToRemove;
            if (toRemove == NO_KEY) {
                throw new IllegalStateException();
            }
            mToRemove = NO_KEY;
            mKernel.remove(toRemove);
        }

        private void checkEnd() throws IOException {
            if (mFromKey == NO_KEY || mPreviousEntry == null) {
                return;
            }
            Object previousEntry = mPreviousEntry.getKey();
            Comparator c = mKernel.comparator();
            int result;
            if (c == null) {
                result = ((Comparable)previousEntry).compareTo(mFromKey);
            }
            else {
                result = c.compare(previousEntry, mFromKey);
            }
            if (result < 0) {
                mPreviousEntry = null;
            }
        }
    }
}
