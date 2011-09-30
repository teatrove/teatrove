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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Some generic utilities.
 *
 * @author Brian S O'Neill

 * @see java.util.Collections
 */
public class Utils {
    // The choice of determining whether these constants are public or
    // private is based on the design of the java.util.Collections class.
    // For consistency, the "empty" fields are public, and the comparators are
    // accessed from static methods.

    public static final Enumeration EMPTY_ENUMERATION = new EmptyEnum();

    /** This Map is already provided in JDK1.3. */
    public static final Map EMPTY_MAP = new EmptyMap();

    /**
     * Similar to EMPTY_MAP, except the put operation is supported, but the
     * values are immediately forgotten.
     */
    public static final Map<?, ?> VOID_MAP = new VoidMap<Object, Object>();
    
    public static final <K, V> Map<K, V> voidMap() {
        return new VoidMap<K, V>();
    }

    private static final Comparator NULL_LOW_ORDER = new NullLowOrder();

    private static final Comparator NULL_HIGH_ORDER = new NullHighOrder();

    private static final Comparator NULL_EQUAL_ORDER = new NullEqualOrder();

    private static FlyweightSet cFlyweightSet;
    
    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered low order. This Comparator
     * allows naturally ordered TreeMaps to support null values.
     */
    public static Comparator nullLowOrder() {
        return NULL_LOW_ORDER;
    }

    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered low order. This fixes Comparators that don't
     * support comparisons against null and allows them to be used in TreeMaps.
     */
    public static Comparator nullLowOrder(Comparator c) {
        return new NullLowOrderC(c);
    }

    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered high order. This Comparator
     * allows naturally ordered TreeMaps to support null values.
     */
    public static Comparator nullHighOrder() {
        return NULL_HIGH_ORDER;
    }

    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered high order. This fixes Comparators that don't
     * support comparisons against null and allows them to be used in TreeMaps.
     */
    public static Comparator nullHighOrder(Comparator c) {
        return new NullHighOrderC(c);
    }

    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered equal order. This Comparator
     * should not be used in a TreeMap, but can be used in a sorter.
     */
    public static Comparator nullEqualOrder() {
        return NULL_EQUAL_ORDER;
    }
    
    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered equal order. This Comparator should not be used in
     * a TreeMap, but can be used in a sorter.
     */
    public static Comparator nullEqualOrder(Comparator c) {
        return new NullEqualOrderC(c);
    }

    /**
     * Returns a Comparator that wraps the given Comparator, but orders in
     * reverse.
     */
    public static Comparator reverseOrder(Comparator c) {
        return new ReverseOrderC(c);
    }

    /**
     * Just like {@link String#intern() String.intern}, except it generates
     * flyweights for any kind of object, and it does not prevent them from
     * being garbage collected. Calling intern on a String does not use the
     * same String pool used by String.intern because those Strings are not
     * always garbage collected. Some virtual machines free up Strings from the
     * interned String pool, others do not.
     * <p>
     * For objects that do not customize the hashCode and equals methods,
     * calling intern is not very useful because the object returned will
     * always be the same as the one passed in.
     * <p>
     * The object type returned from intern is guaranteed to be exactly the
     * same type as the one passed in. Calling intern on null returns null.
     *
     * @param obj Object to intern
     * @return Interned object.
     * @see FlyweightSet
     */
    public static synchronized Object intern(Object obj) {
        FlyweightSet set;
        if ((set = cFlyweightSet) == null) {
            cFlyweightSet = set = new FlyweightSet();
        }
        return set.put(obj);
    }

    protected Utils() {
    }

    private static class EmptyEnum implements Enumeration, Serializable {
        public boolean hasMoreElements() {
            return false;
        }
        
        public Object nextElement() throws NoSuchElementException {
            throw new NoSuchElementException();
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return EMPTY_ENUMERATION;
        }
    };

    private static class EmptyMap<K, V> implements Map<K, V>, Serializable {
        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }
        
        public V get(Object key) {
            return null;
        }

        public V put(K key, V value) {
            throw new UnsupportedOperationException
                ("Cannot put into immutable empty map");
        }

        public V remove(Object key) {
            return null;
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException
                ("Cannot put into immutable empty map");
        }

        public void clear() {
        }

        public Set<K> keySet() {
            return Collections.emptySet();
        }

        public Collection<V> values() {
            return Collections.emptyList();
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return EMPTY_MAP;
        }
    }

    private static final class VoidMap<K, V> extends EmptyMap<K, V> {
        @Override public V put(K key, V value) {
            return null;
        }

        @Override public void putAll(Map<? extends K, ? extends V> map) {
        }
    }

    private static class NullLowOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? ((Comparable)obj1).compareTo(obj2) : 1;
            }
            else {
                return (obj2 != null) ? -1 : 0;
            }
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return nullLowOrder();
        }
    }

    private static class NullLowOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullLowOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? c.compare(obj1, obj2) : 1;
            }
            else {
                return (obj2 != null) ? -1 : 0;
            }
        }
    }

    private static class NullHighOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? ((Comparable)obj1).compareTo(obj2): -1;
            }
            else {
                return (obj2 != null) ? 1 : 0;
            }
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return nullHighOrder();
        }
    }

    private static class NullHighOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullHighOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? c.compare(obj1, obj2) : -1;
            }
            else {
                return (obj2 != null) ? 1 : 0;
            }
        }
    }

    private static class NullEqualOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            return (obj1 != null && obj2 != null) ?
                ((Comparable)obj1).compareTo(obj2) : 0;
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return nullEqualOrder();
        }
    }
    
    private static class NullEqualOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullEqualOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            return (obj1 != null && obj2 != null) ? c.compare(obj1, obj2) : 0;
        }
    }

    private static class ReverseOrderC implements Comparator, Serializable {
        private Comparator c;

        public ReverseOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            return c.compare(obj2, obj1);
        }
    }
}
