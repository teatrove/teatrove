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
import org.teatrove.trove.util.ReadWriteLock;

/**
 * Just like {@link java.util.Collection} except methods may throw
 * IOExceptions, and read/write locks are built in.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 8 <!-- $-->, <!--$$JustDate:--> 02/04/09 <!-- $-->
 */
public interface PersistentCollection {
    int size() throws IOException;

    boolean isEmpty() throws IOException;

    boolean contains(Object obj) throws IOException;

    PersistentIterator iterator() throws IOException;

    PersistentIterator reverseIterator() throws IOException;

    Object[] toArray() throws IOException;

    Object[] toArray(Object[] array) throws IOException;

    boolean add(Object obj) throws IOException;

    boolean remove(Object obj) throws IOException;

    boolean containsAll(PersistentCollection c) throws IOException;

    boolean addAll(PersistentCollection c) throws IOException;

    boolean removeAll(PersistentCollection c) throws IOException;

    boolean retainAll(PersistentCollection c) throws IOException;

    void clear() throws IOException;

    ReadWriteLock lock();
}
