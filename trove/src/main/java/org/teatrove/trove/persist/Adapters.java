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

import java.io.IOException;
import java.util.*;
import java.lang.reflect.UndeclaredThrowableException;
import org.teatrove.trove.util.ReadWriteLock;
import org.teatrove.trove.util.SecureReadWriteLock;

/**
 * Adapters to make persistent collections compatible with java.util
 * collections. IOExceptions and InterruptedExceptions thrown from wrapped
 * persistent collections will be thrown as
 * {@link UndeclaredThrowableException}s.
 *
 * @author Brian S O'Neill
 */
public class Adapters {
    public static PersistentIterator wrap(Iterator it) {
        return new PIterator(it);
    }

    public static PersistentCollection wrap(Collection c) {
        return new PCollection(c, new SecureReadWriteLock());
    }

    public static PersistentCollection wrap(Collection c,
                                            ReadWriteLock lock) {
        return new PCollection(c, lock);
    }

    public static PersistentSet wrap(Set set) {
        return new PSet(set, new SecureReadWriteLock());
    }

    public static PersistentSet wrap(Set set, ReadWriteLock lock) {
        return new PSet(set, lock);
    }

    public static PersistentSortedSet wrap(SortedSet set) {
        return new PSortedSet(set, new SecureReadWriteLock());
    }

    public static PersistentSortedSet wrap(SortedSet set,
                                           ReadWriteLock lock) {
        return new PSortedSet(set, lock);
    }

    public static PersistentMap wrap(Map map) {
        return new PMap(map, new SecureReadWriteLock());
    }

    public static PersistentMap wrap(Map map, ReadWriteLock lock) {
        return new PMap(map, lock);
    }

    public static PersistentSortedMap wrap(SortedMap map) {
        return new PSortedMap(map, new SecureReadWriteLock());
    }

    public static PersistentSortedMap wrap(SortedMap map,
                                           ReadWriteLock lock) {
        return new PSortedMap(map, lock);
    }

    public static Iterator wrap(PersistentIterator it) {
        return new JIterator(it);
    }

    public static Collection wrap(PersistentCollection c) {
        return new JCollection(c);
    }

    public static Set wrap(PersistentSet set) {
        return new JSet(set);
    }

    public static SortedSet wrap(PersistentSortedSet set) {
        return new JSortedSet(set);
    }

    public static Map wrap(PersistentMap map) {
        return new JMap(map);
    }

    public static SortedMap wrap(PersistentSortedMap map) {
        return new JSortedMap(map);
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class PIterator implements PersistentIterator {
        protected final Iterator it;

        PIterator(Iterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }
        
        public Object next() {
            return it.next();
        }
        
        public void remove() {
            it.remove();
        }
    }

    private static class PCollection implements PersistentCollection {
        protected final Collection c;
        protected final ReadWriteLock mLock;

        PCollection(Collection c, ReadWriteLock lock) {
            this.c = c;
            mLock = lock;
        }

        public int size() {
            try {
                mLock.acquireReadLock();
                return c.size();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean isEmpty() {
            try {
                mLock.acquireReadLock();
                return c.isEmpty();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean contains(Object obj) {
            try {
                mLock.acquireReadLock();
                return c.contains(obj);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public PersistentIterator iterator() {
            return wrap(c.iterator());
        }

        public PersistentIterator reverseIterator() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray() {
            try {
                mLock.acquireReadLock();
                return c.toArray();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public Object[] toArray(Object[] array) {
            try {
                mLock.acquireReadLock();
                return c.toArray(array);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean add(Object obj) {
            try {
                mLock.acquireWriteLock();
                return c.add(obj);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean remove(Object obj) {
            try {
                mLock.acquireWriteLock();
                return c.remove(obj);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean containsAll(PersistentCollection c) {
            try {
                mLock.acquireReadLock();
                try {
                    c.lock().acquireReadLock();
                    return this.c.containsAll(wrap(c));
                }
                finally {
                    c.lock().releaseLock();
                }
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean addAll(PersistentCollection c) {
            try {
                mLock.acquireWriteLock();
                try {
                    c.lock().acquireReadLock();
                    return this.c.addAll(wrap(c));
                }
                finally {
                    c.lock().releaseLock();
                }
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean removeAll(PersistentCollection c) {
            try {
                mLock.acquireWriteLock();
                try {
                    c.lock().acquireReadLock();
                    return this.c.removeAll(wrap(c));
                }
                finally {
                    c.lock().releaseLock();
                }
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean retainAll(PersistentCollection c) {
            try {
                mLock.acquireWriteLock();
                try {
                    c.lock().acquireReadLock();
                    return this.c.retainAll(wrap(c));
                }
                finally {
                    c.lock().releaseLock();
                }
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public void clear() {
            try {
                mLock.acquireWriteLock();
                c.clear();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public boolean equals(Object obj) {
            try {
                mLock.acquireReadLock();
                return c.equals(obj);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public int hashCode() {
            try {
                mLock.acquireReadLock();
                return c.hashCode();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public String toString() {
            try {
                mLock.acquireReadLock();
                return c.toString();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public ReadWriteLock lock() {
            return mLock;
        }
    }

    private static class PSet extends PCollection implements PersistentSet {
        PSet(Set set, ReadWriteLock lock) {
            super(set, lock);
        }
    }
    
    private static class PSortedSet extends PSet
        implements PersistentSortedSet 
    {
        PSortedSet(SortedSet set, ReadWriteLock lock) {
            super(set, lock);
        }

        public Comparator comparator() {
            return ((SortedSet)c).comparator();
        }
        
        public PersistentSortedSet subSet(Object from, Object to) {
            return wrap(((SortedSet)c).subSet(from, to), mLock);
        }
        
        public PersistentSortedSet headSet(Object to) {
            return wrap(((SortedSet)c).headSet(to), mLock);
        }

        public PersistentSortedSet tailSet(Object from) {
            return wrap(((SortedSet)c).tailSet(from), mLock);
        }
        
        public Object first() {
            try {
                mLock.acquireReadLock();
                return ((SortedSet)c).first();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public Object last() {
            try {
                mLock.acquireReadLock();
                return ((SortedSet)c).last();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
    }

    private static class PMap implements PersistentMap {
        protected final Map map;
        protected final ReadWriteLock mLock;

        PMap(Map map, ReadWriteLock lock) {
            this.map = map;
            mLock = lock;
        }

        public int size() {
            try {
                mLock.acquireReadLock();
                return map.size();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public boolean isEmpty() {
            try {
                mLock.acquireReadLock();
                return map.isEmpty();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public boolean containsKey(Object key) {
            try {
                mLock.acquireReadLock();
                return map.containsKey(key);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public boolean containsValue(Object value) {
            try {
                mLock.acquireReadLock();
                return map.containsValue(value);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public Object get(Object key) {
            try {
                mLock.acquireReadLock();
                return map.get(key);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public Object put(Object key, Object value) {
            try {
                mLock.acquireWriteLock();
                return map.put(key, value);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public Object remove(Object key) {
            try {
                mLock.acquireWriteLock();
                return map.remove(key);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public void putAll(PersistentMap map) {
            try {
                mLock.acquireWriteLock();
                try {
                    map.lock().acquireReadLock();
                    this.map.putAll(wrap(map));
                }
                finally {
                    map.lock().releaseLock();
                }
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public void clear() {
            try {
                mLock.acquireWriteLock();
                map.clear();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }
        
        public PersistentSet keySet() {
            return wrap(map.keySet(), mLock);
        }
        
        public PersistentCollection values() {
            return wrap(map.values(), mLock);
        }
        
        public PersistentSet entrySet() {
            return wrap(map.entrySet(), mLock);
        }

        public boolean equals(Object obj) {
            try {
                mLock.acquireReadLock();
                return map.equals(obj);
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public int hashCode() {
            try {
                mLock.acquireReadLock();
                return map.hashCode();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public String toString() {
            try {
                mLock.acquireReadLock();
                return map.toString();
            }
            catch (InterruptedException e) {
                throw new UndeclaredThrowableException(e);
            }
            finally {
                mLock.releaseLock();
            }
        }

        public ReadWriteLock lock() {
            return mLock;
        }
    }

    private static class PSortedMap extends PMap
        implements PersistentSortedMap
    {
        PSortedMap(SortedMap map, ReadWriteLock lock) {
            super(map, lock);
        }

        public Comparator comparator() {
            return ((SortedMap)map).comparator();
        }
        
        public PersistentSortedMap subMap(Object from, Object to) {
            return wrap(((SortedMap)map).subMap(from, to), mLock);
        }
        
        public PersistentSortedMap headMap(Object to) {
            return wrap(((SortedMap)map).headMap(to), mLock);
        }
        
        public PersistentSortedMap tailMap(Object from) {
            return wrap(((SortedMap)map).tailMap(from), mLock);
        }
        
        public Object firstKey() {
            return ((SortedMap)map).firstKey();
        }
        
        public Object lastKey() {
            return ((SortedMap)map).lastKey();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class JIterator implements Iterator {
        protected final PersistentIterator it;

        JIterator(PersistentIterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            try {
                return it.hasNext();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object next() {
            try {
                return it.next();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public void remove() {
            try {
                it.remove();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    private static class JCollection implements Collection {
        protected final PersistentCollection c;

        JCollection(PersistentCollection c) {
            this.c = c;
        }

        public int size() {
            try {
                return c.size();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean isEmpty() {
            try {
                return c.isEmpty();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean contains(Object obj) {
            try {
                return c.contains(obj);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public Iterator iterator() {
            try {
                return wrap(c.iterator());
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public Object[] toArray() {
            try {
                return c.toArray();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public Object[] toArray(Object[] array) {
            try {
                return c.toArray(array);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean add(Object obj) {
            try {
                return c.add(obj);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean remove(Object obj) {
            try {
                return c.remove(obj);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean containsAll(Collection c) {
            try {
                return this.c.containsAll(wrap(c));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean addAll(Collection c) {
            try {
                return this.c.addAll(wrap(c));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean removeAll(Collection c) {
            try {
                return this.c.removeAll(wrap(c));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public boolean retainAll(Collection c) {
            try {
                return this.c.retainAll(wrap(c));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public void clear() {
            try {
                c.clear();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
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
    }

    private static class JSet extends JCollection implements Set {
        JSet(PersistentSet set) {
            super(set);
        }
    }
    
    private static class JSortedSet extends JSet implements SortedSet 
    {
        JSortedSet(PersistentSortedSet set) {
            super(set);
        }

        public Comparator comparator() {
            return ((PersistentSortedSet)c).comparator();
        }
        
        public SortedSet subSet(Object from, Object to) {
            try {
                return wrap(((PersistentSortedSet)c).subSet(from, to));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public SortedSet headSet(Object to) {
            try {
                return wrap(((PersistentSortedSet)c).headSet(to));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        public SortedSet tailSet(Object from) {
            try {
                return wrap(((PersistentSortedSet)c).tailSet(from));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object first() {
            try {
                return ((PersistentSortedSet)c).first();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object last() {
            try {
                return ((PersistentSortedSet)c).last();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    private static class JMap implements Map {
        protected final PersistentMap map;

        JMap(PersistentMap map) {
            this.map = map;
        }

        public int size() {
            try {
                return map.size();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public boolean isEmpty() {
            try {
                return map.isEmpty();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public boolean containsKey(Object key) {
            try {
                return map.containsKey(key);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public boolean containsValue(Object value) {
            try {
                return map.containsValue(value);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object get(Object key) {
            try {
                return map.get(key);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object put(Object key, Object value) {
            try {
                return map.put(key, value);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object remove(Object key) {
            try {
                return map.remove(key);
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public void putAll(Map map) {
            try {
                this.map.putAll(wrap(map));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public void clear() {
            try {
                map.clear();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Set keySet() {
            try {
                return wrap(map.keySet());
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Collection values() {
            try {
                return wrap(map.values());
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Set entrySet() {
            try {
                return wrap(map.entrySet());
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
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
    }

    private static class JSortedMap extends JMap implements SortedMap {
        JSortedMap(PersistentSortedMap map) {
            super(map);
        }

        public Comparator comparator() {
            return ((PersistentSortedMap)map).comparator();
        }
        
        public SortedMap subMap(Object from, Object to) {
            try {
                return wrap(((PersistentSortedMap)map).subMap(from, to));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public SortedMap headMap(Object to) {
            try {
                return wrap(((PersistentSortedMap)map).headMap(to));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public SortedMap tailMap(Object from) {
            try {
                return wrap(((PersistentSortedMap)map).tailMap(from));
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object firstKey() {
            try {
                return ((PersistentSortedMap)map).firstKey();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        
        public Object lastKey() {
            try {
                return ((PersistentSortedMap)map).lastKey();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
}
