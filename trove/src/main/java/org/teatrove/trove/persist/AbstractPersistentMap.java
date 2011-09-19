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
import java.io.InterruptedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * Just like {@link java.util.AbstractMap} except methods may throw
 * IOExceptions. Also, the toString method isn't defined, as persistent maps
 * may be very large.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 14 <!-- $-->, <!--$$JustDate:--> 02/04/09 <!-- $-->
 */
public abstract class AbstractPersistentMap implements PersistentMap {
    protected final ReadWriteLock mLock;

    protected AbstractPersistentMap(ReadWriteLock lock) {
        mLock = lock;
    }

    public int size() throws IOException {
        return entrySet().size();
    }

    public boolean isEmpty() throws IOException {
        return size() == 0;
    }

    public boolean containsValue(Object value) throws IOException {
        try {
            mLock.acquireReadLock();
            PersistentIterator i = values().iterator();
            if (value == null) {
                while (i.hasNext()) {
                    if (i.next() == null) {
                        return true;
                    }
                }
            }
            else {
                while (i.hasNext()) {
                    if (value.equals(i.next())) {
                        return true;
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
        return false;
    }

    public boolean containsKey(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            PersistentIterator i = entrySet().iterator();
            if (key == null) {
                while (i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (e.getKey() == null) {
                        return true;
                    }
                }
            }
            else {
                while (i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (key.equals(e.getKey())) {
                        return true;
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
        return false;
    }

    public Object get(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            PersistentIterator i = entrySet().iterator();
            if (key == null) {
                while (i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (e.getKey() == null) {
                        return e.getValue();
                    }
                }
            }
            else {
                while (i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (key.equals(e.getKey())) {
                        return e.getValue();
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
        return null;
    }

    public Object put(Object key, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) throws IOException {
        try {
            mLock.acquireWriteLock();
            PersistentIterator i = entrySet().iterator();
            PersistentMap.Entry correctEntry = null;
            if (key == null) {
                while (correctEntry == null && i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (e.getKey()==null) {
                        correctEntry = e;
                    }
                }
            }
            else {
                while (correctEntry == null && i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    if (key.equals(e.getKey())) {
                        correctEntry = e;
                    }
                }
            }
            
            Object oldValue = null;
            if (correctEntry != null) {
                oldValue = correctEntry.getValue();
                i.remove();
            }
            return oldValue;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    /**
     * Puts all the elements of the given map into this one without acquiring
     * a long-held locks on either map.
     */
    public void putAll(PersistentMap map) throws IOException {
        PersistentIterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            PersistentMap.Entry entry = (PersistentMap.Entry)it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() throws IOException {
        try {
            mLock.acquireWriteLock();
            entrySet().clear();
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public ReadWriteLock lock() {
        return mLock;
    }

    private transient PersistentSet keySet = null;

    public PersistentSet keySet() throws IOException {
        if (keySet == null) {
            keySet = new KeySet(mLock);
        }
        return keySet;
    }
    
    private transient PersistentCollection values = null;

    public PersistentCollection values() throws IOException {
        if (values == null) {
            values = new Values(mLock);
        }
        return values;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof PersistentMap)) {
            return false;
        }
        
        PersistentMap t = (PersistentMap)o;

        try {
            mLock.acquireReadLock();
            try {
                t.lock().acquireReadLock();
                if (t.size() != size()) {
                    return false;
                }
                
                PersistentIterator i = entrySet().iterator();
                while (i.hasNext()) {
                    PersistentMap.Entry e = (PersistentMap.Entry)i.next();
                    Object key = e.getKey();
                    Object value = e.getValue();
                    if (value == null) {
                        if (!(t.get(key) == null && t.containsKey(key))) {
                            return false;
                        }
                    }
                    else {
                        if (!value.equals(t.get(key))) {
                            return false;
                        }
                    }
                }
            }
            finally {
                t.lock().releaseLock();
            }
        }
        catch (InterruptedException e) {
            throw new UndeclaredThrowableException(e);
        }
        catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
        finally {
            mLock.releaseLock();
        }

        return true;
    }
    
    public int hashCode() {
        int h = 0;
        try {
            mLock.acquireReadLock();
            PersistentIterator i = entrySet().iterator();
            while (i.hasNext()) {
                h += i.next().hashCode();
            }
        }
        catch (InterruptedException e) {
            throw new UndeclaredThrowableException(e);
        }
        catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
        finally {
            mLock.releaseLock();
        }
        return h;
    }

    public String toString() {
        return getClass().getName() + '@' +
            Integer.toHexString(System.identityHashCode(this));
    }

    private class KeySet extends AbstractPersistentSet {
        KeySet(ReadWriteLock lock) {
            super(lock);
        }

        public PersistentIterator iterator() throws IOException {
            return new Iter(entrySet().iterator());
        }

        public PersistentIterator reverseIterator() throws IOException {
            return new Iter(entrySet().reverseIterator());
        }
        
        public int size() throws IOException {
            return AbstractPersistentMap.this.size();
        }
        
        public boolean contains(Object k) throws IOException {
            return AbstractPersistentMap.this.containsKey(k);
        }

        private class Iter implements PersistentIterator {
            private final PersistentIterator i;
            
            Iter(PersistentIterator i) {
                this.i = i;
            }
            
            public boolean hasNext() throws IOException {
                return i.hasNext();
            }
            
            public Object next() throws IOException {
                return ((PersistentMap.Entry)i.next()).getKey();
            }
            
            public void remove() throws IOException {
                i.remove();
            }
        }
    }

    private class Values extends AbstractPersistentCollection {
        Values(ReadWriteLock lock) {
            super(lock);
        }

        public PersistentIterator iterator() throws IOException {
            return new Iter(entrySet().iterator());
        }
        
        public PersistentIterator reverseIterator() throws IOException {
            return new Iter(entrySet().reverseIterator());
        }

        public int size() throws IOException {
            return AbstractPersistentMap.this.size();
        }
        
        public boolean contains(Object v) throws IOException {
            return AbstractPersistentMap.this.containsValue(v);
        }

        private class Iter implements PersistentIterator {
            private final PersistentIterator i;
            
            Iter(PersistentIterator i) {
                this.i = i;
            }
            
            public boolean hasNext() throws IOException {
                return i.hasNext();
            }
            
            public Object next() throws IOException {
                return ((PersistentMap.Entry)i.next()).getValue();
            }
            
            public void remove() throws IOException {
                i.remove();
            }
        }
    }
}
