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
 * Just like {@link java.util.AbstractCollection} except methods may throw
 * IOExceptions.
 * 
 * @author Brian S O'Neill
 */
public abstract class AbstractPersistentSet
    extends AbstractPersistentCollection implements PersistentSet
{
    protected AbstractPersistentSet(ReadWriteLock lock) {
        super(lock);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PersistentSet)) {
            return false;
        }
            
        PersistentCollection c = (PersistentCollection)o;
            
        try {
            mLock.acquireReadLock();
            try {
                c.lock().acquireReadLock();
                if (c.size() != size()) {
                    return false;
                }
                return containsAll(c);
            }
            finally {
                c.lock().releaseLock();
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
    }
    
    public int hashCode() {
        int h = 0;
        try {
            mLock.acquireReadLock();
            PersistentIterator i = iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                if (obj != null) {
                    h += obj.hashCode();
                }
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

    /**
     * Removes all of the elements of the given collection without acquiring
     * long-held locks on either collection.
     */
    public boolean removeAll(PersistentCollection c) throws IOException {
        boolean modified = false;
        if (size() > c.size()) {
            for (PersistentIterator i = c.iterator(); i.hasNext(); ) {
                modified |= remove(i.next());
            }
        }
        else {
            for (PersistentIterator i = iterator(); i.hasNext(); ) {
                if(c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }
}
