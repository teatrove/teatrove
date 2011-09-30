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

/**
 * A Map that orders its keys based on how recently they have been used.
 * Most recently used keys appear first in the Map. Keys are marked as being
 * used whenever they are put into to the Map. To re-position a key, put it
 * back in.
 * 
 * @author Brian S O'Neill
 */
public class UsageMap extends AbstractMap implements java.io.Serializable {
    private Map mRecentMap;
    private boolean mReverse;

    private Entry mMostRecent;
    private Entry mLeastRecent;

    private transient Set mEntrySet;

    /**
     * Creates a UsageMap in forward order, MRU first.
     */
    public UsageMap() {
        this(new HashMap());
    }

    /**
     * @param backingMap map to use for storage
     */
    public UsageMap(Map backingMap) {
        mRecentMap = backingMap;
    }

    /**
     * With reverse order, keys are ordered least recently used first. The
     * ordering of the map entries will be consistent with the order they were
     * put into it. Switching to and from reverse order is performed quickly
     * and is not affected by the current size of the map.
     */
    public void setReverseOrder(boolean reverse) {
        mReverse = reverse;
    }

    /**
     * Returns the first key in the map, the most recently used. If reverse
     * order, then the least recently used is returned.
     */
    public Object firstKey() throws NoSuchElementException {
        Entry first = (mReverse) ? mLeastRecent : mMostRecent;
        if (first != null) {
            return first.mKey;
        }
        else if (mRecentMap.size() == 0) {
            throw new NoSuchElementException();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the last key in the map, the least recently used. If reverse
     * order, then the most recently used is returned.
     */
    public Object lastKey() throws NoSuchElementException {
        Entry last = (mReverse) ? mMostRecent : mLeastRecent;
        if (last != null) {
            return last.mKey;
        }
        else if (mRecentMap.size() == 0) {
            throw new NoSuchElementException();
        }
        else {
            return null;
        }
    }

    public int size() {
        return mRecentMap.size();
    }

    public boolean isEmpty() {
        return mRecentMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return mRecentMap.containsKey(key);
    }

    public Object get(Object key) {
        Entry e = (Entry)mRecentMap.get(key);
        return (e == null) ? null : e.mValue;
    }

    public Object put(Object key, Object value) {
        Entry e = (Entry)mRecentMap.get(key);
        Object old;

        if (e == null) {
            old = null;
            e = new Entry(key, value);
            mRecentMap.put(key, e);
        }
        else {
            old = e.mValue;
            e.mValue = value;

            if (e == mMostRecent) {
                return old;
            }

            // Delete entry from linked list.
            if (e.mPrev == null) {
                mMostRecent = e.mNext;
            }
            else {
                e.mPrev.mNext = e.mNext;
            }
            if (e.mNext == null) {
                mLeastRecent = e.mPrev;
            }
            else {
                e.mNext.mPrev = e.mPrev;
            }
            e.mPrev = null;
        }

        if (mMostRecent == null) {
            mMostRecent = e;
        }
        else {
            e.mNext = mMostRecent;
            mMostRecent.mPrev = e;
            mMostRecent = e;
        }

        if (mLeastRecent == null) {
            mLeastRecent = e;
        }

        return old;
    }

    public Object remove(Object key) {
        Entry e = (Entry)mRecentMap.remove(key);
        
        if (e == null) {
            return null;
        }
        else {
            // Delete entry from linked list.
            if (e.mPrev == null) {
                mMostRecent = e.mNext;
            }
            else {
                e.mPrev.mNext = e.mNext;
            }
            if (e.mNext == null) {
                mLeastRecent = e.mPrev;
            }
            else {
                e.mNext.mPrev = e.mPrev;
            }

            return e.mValue;
        }
    }

    public void putAll(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            mRecentMap.put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mRecentMap.clear();
        mMostRecent = null;
        mLeastRecent = null;
    }

    public Set entrySet() {
        if (mEntrySet != null) {
            return mEntrySet;
        }

        mEntrySet = new AbstractSet() {
            public Iterator iterator() {
                if (mReverse) {
                    return new Iterator() {
                        private Entry mPrev = mLeastRecent;
                        private Entry mLast = null;

                        public boolean hasNext() {
                            return mPrev != null;
                        }

                        public Object next() {
                            if ((mLast = mPrev) == null) {
                                throw new NoSuchElementException();
                            }
                            else {
                                mPrev = mPrev.mPrev;
                                return mLast;
                            }
                        }

                        public void remove() {
                            if (mLast == null) {
                                throw new IllegalStateException();
                            }
                            else {
                                UsageMap.this.remove(mLast.mKey);
                                mLast = null;
                            }
                        }
                    };
                }
                else {
                    return new Iterator() {
                        private Entry mNext = mMostRecent;
                        private Entry mLast = null;

                        public boolean hasNext() {
                            return mNext != null;
                        }

                        public Object next() {
                            if ((mLast = mNext) == null) {
                                throw new NoSuchElementException();
                            }
                            else {
                                mNext = mNext.mNext;
                                return mLast;
                            }
                        }

                        public void remove() {
                            if (mLast == null) {
                                throw new IllegalStateException();
                            }
                            else {
                                UsageMap.this.remove(mLast.mKey);
                                mLast = null;
                            }
                        }
                    };
                }
            }

            public int size() {
                return mRecentMap.size();
            }

            public boolean isEmpty() {
                return mRecentMap.isEmpty();
            }
            
            public boolean contains(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Entry e = (Entry)mRecentMap.get(((Map.Entry)obj).getKey());
                return e != null && e.equals(obj);
            }

            public boolean remove(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                if (contains(obj)) {
                    UsageMap.this.remove(((Map.Entry)obj).getKey());
                    return true;
                }
                else {
                    return false;
                }
            }

            public void clear() {
                UsageMap.this.clear();
            }
        };

        return mEntrySet;
    }

    private static class Entry extends AbstractMapEntry
        implements java.io.Serializable
    {
        public Entry mPrev;
        public Entry mNext;
        public Object mKey;
        public Object mValue;

        public Entry(Object key, Object value) {
            mKey = key;
            mValue = value;
        }

        public Object getKey() {
            return mKey;
        }

        public Object getValue() {
            return mValue;
        }

        public Object setValue(Object value) {
            Object old = mValue;
            mValue = value;
            return old;
        }
    }
}
