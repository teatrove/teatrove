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
import java.io.IOException;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * Creates unmodifiable views of PersistentCollections. Attempts to modify
 * the collection will cause an UnsupportedOperationException to be thrown.
 *
 * @author Brian S O'Neill
 */
public class UnmodifiableViews {
    public static PersistentIterator wrap(PersistentIterator it) {
        return (it instanceof UIterator) ? it : new UIterator(it);
    }

    public static PersistentCollection wrap(PersistentCollection c) {
        return (c instanceof UCollection) ? c : new UCollection(c);
    }

    public static PersistentSet wrap(PersistentSet set) {
        return (set instanceof USet) ? set : new USet(set);
    }

    public static PersistentSortedSet wrap(PersistentSortedSet set) {
        return (set instanceof USortedSet) ? set : new USortedSet(set);
    }

    public static PersistentMap wrap(PersistentMap map) {
        return (map instanceof UMap) ? map : new UMap(map);
    }

    public static PersistentSortedMap wrap(PersistentSortedMap map) {
        return (map instanceof USortedMap) ? map : new USortedMap(map);
    }

    private static class UIterator implements PersistentIterator {
        protected final PersistentIterator it;

        UIterator(PersistentIterator it) {
            this.it = it;
        }

        public boolean hasNext() throws IOException {
            return it.hasNext();
        }
        
        public Object next() throws IOException {
            return it.next();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class UCollection implements PersistentCollection {
        protected final PersistentCollection c;

        UCollection(PersistentCollection c) {
            this.c = c;
        }

        public int size() throws IOException {
            return c.size();
        }

        public boolean isEmpty() throws IOException {
            return c.isEmpty();
        }

        public boolean contains(Object obj) throws IOException {
            return c.contains(obj);
        }

        public PersistentIterator iterator() throws IOException {
            return wrap(c.iterator());
        }

        public PersistentIterator reverseIterator() throws IOException {
            return wrap(c.reverseIterator());
        }

        public Object[] toArray() throws IOException {
            return c.toArray();
        }

        public Object[] toArray(Object[] array) throws IOException {
            return c.toArray(array);
        }

        public boolean add(Object obj) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(PersistentCollection c) throws IOException {
            return this.c.containsAll(c);
        }

        public boolean addAll(PersistentCollection c) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(PersistentCollection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(PersistentCollection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object obj) {
            return c.equals(obj);
        }

        public int hashCode() {
            return c.hashCode();
        }

        public String toString() {
            return c.toString();
        }

        public ReadWriteLock lock() {
            return c.lock();
        }
    }

    private static class USet extends UCollection implements PersistentSet {
        USet(PersistentSet set) {
            super(set);
        }
    }
    
    private static class USortedSet extends USet
        implements PersistentSortedSet
    {
        USortedSet(PersistentSortedSet set) {
            super(set);
        }

        public Comparator comparator() {
            return ((PersistentSortedSet)c).comparator();
        }
        
        public PersistentSortedSet subSet(Object from, Object to)
            throws IOException
        {
            return wrap(((PersistentSortedSet)c).subSet(from, to));
        }
        
        public PersistentSortedSet headSet(Object to) throws IOException {
            return wrap(((PersistentSortedSet)c).headSet(to));
        }

        public PersistentSortedSet tailSet(Object from) throws IOException {
            return wrap(((PersistentSortedSet)c).tailSet(from));
        }
        
        public Object first() throws IOException {
            return ((PersistentSortedSet)c).first();
        }
        
        public Object last() throws IOException {
            return ((PersistentSortedSet)c).last();
        }
    }

    private static class UMap implements PersistentMap {
        protected final PersistentMap map;

        UMap(PersistentMap map) {
            this.map = map;
        }

        public int size() throws IOException {
            return map.size();
        }
        
        public boolean isEmpty() throws IOException {
            return map.isEmpty();
        }
        
        public boolean containsKey(Object key) throws IOException {
            return map.containsKey(key);
        }
        
        public boolean containsValue(Object value) throws IOException {
            return map.containsValue(value);
        }
        
        public Object get(Object key) throws IOException {
            return map.get(key);
        }
        
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }
        
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }
        
        public void putAll(PersistentMap map) {
            throw new UnsupportedOperationException();
        }
        
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        public PersistentSet keySet() throws IOException {
            return wrap(map.keySet());
        }
        
        public PersistentCollection values() throws IOException {
            return wrap(map.values());
        }
        
        public PersistentSet entrySet() throws IOException {
            return wrap(map.entrySet());
        }

        public boolean equals(Object obj) {
            return map.equals(obj);
        }

        public int hashCode() {
            return map.hashCode();
        }

        public String toString() {
            return map.toString();
        }

        public ReadWriteLock lock() {
            return map.lock();
        }
    }

    private static class USortedMap extends UMap
        implements PersistentSortedMap
    {
        USortedMap(PersistentSortedMap map) {
            super(map);
        }

        public Comparator comparator() {
            return ((PersistentSortedMap)map).comparator();
        }
        
        public PersistentSortedMap subMap(Object from, Object to)
            throws IOException
        {
            return wrap(((PersistentSortedMap)map).subMap(from, to));
        }
        
        public PersistentSortedMap headMap(Object to) throws IOException {
            return wrap(((PersistentSortedMap)map).headMap(to));
        }
        
        public PersistentSortedMap tailMap(Object from) throws IOException {
            return wrap(((PersistentSortedMap)map).tailMap(from));
        }
        
        public Object firstKey() throws IOException {
            return ((PersistentSortedMap)map).firstKey();
        }
        
        public Object lastKey() throws IOException {
            return ((PersistentSortedMap)map).lastKey();
        }
    }
}
