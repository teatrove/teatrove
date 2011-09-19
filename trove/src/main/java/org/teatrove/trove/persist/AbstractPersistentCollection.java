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
import org.teatrove.trove.util.ReadWriteLock;

/**
 * Just like {@link java.util.AbstractCollection} except methods may throw
 * IOExceptions. Also, the toString method isn't defined, as persistent
 * collections may be very large.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 8 <!-- $-->, <!--$$JustDate:--> 01/12/05 <!-- $-->
 */
public abstract class AbstractPersistentCollection
    implements PersistentCollection
{
    protected final ReadWriteLock mLock;

    protected AbstractPersistentCollection(ReadWriteLock lock) {
        mLock = lock;
    }

    public boolean isEmpty() throws IOException {
        try {
            mLock.acquireReadLock();
            return size() == 0;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public boolean contains(Object o) throws IOException {
        try {
            mLock.acquireReadLock();
            PersistentIterator e = iterator();
            if (o==null) {
                while (e.hasNext()) {
                    if (e.next() == null) {
                        return true;
                    }
                }
            }
            else {
                while (e.hasNext()) {
                    if (o.equals(e.next())) {
                        return true;
                    }
                }
            }
            return false;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object[] toArray() throws IOException {
        try {
            mLock.acquireReadLock();
            Object[] result = new Object[size()];
            PersistentIterator e = iterator();
            for (int i=0; e.hasNext(); i++) {
                result[i] = e.next();
            }
            return result;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object[] toArray(Object[] a) throws IOException {
        try {
            mLock.acquireReadLock();
            int size = size();
            if (a.length < size) {
                a = (Object[])java.lang.reflect.Array.newInstance
                    (a.getClass().getComponentType(), size);
            }
            
            PersistentIterator it=iterator();
            for (int i=0; i<size; i++) {
                a[i] = it.next();
            }
            
            if (a.length > size) {
                a[size] = null;
            }

            return a;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public boolean add(Object o) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) throws IOException {
        try {
            mLock.acquireWriteLock();
            PersistentIterator e = iterator();
            if (o==null) {
                while (e.hasNext()) {
                    if (e.next()==null) {
                        e.remove();
                        return true;
                    }
                }
            }
            else {
                while (e.hasNext()) {
                    if (o.equals(e.next())) {
                        e.remove();
                        return true;
                    }
                }
            }
            return false;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    /**
     * Checks if this collection contains all the same elements as the one
     * given without acquiring long-held locks on either collection.
     */
    public boolean containsAll(PersistentCollection c) throws IOException {
        PersistentIterator e = c.iterator();
        while (e.hasNext()) {
            if (!contains(e.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds all of the elements of the given collection without acquiring
     * long-held locks on either collection.
     */
    public boolean addAll(PersistentCollection c) throws IOException {
        boolean modified = false;
        PersistentIterator e = c.iterator();
        while (e.hasNext()) {
            modified |= add(e.next());
        }
        return modified;
    }

    /**
     * Removes all of the elements of the given collection without acquiring
     * long-held locks on either collection.
     */
    public boolean removeAll(PersistentCollection c) throws IOException {
        boolean modified = false;
        PersistentIterator e = iterator();
        while (e.hasNext()) {
            if (c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Retains all of the elements of the given collection without acquiring
     * long-held locks on either collection.
     */
    public boolean retainAll(PersistentCollection c) throws IOException {
        boolean modified = false;
        PersistentIterator e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Clears this collection without acquiring a long-held write lock.
     */
    public void clear() throws IOException {
        PersistentIterator e = iterator();
        while (e.hasNext()) {
            e.next();
            e.remove();
        }
    }

    public ReadWriteLock lock() {
        return mLock;
    }
}

