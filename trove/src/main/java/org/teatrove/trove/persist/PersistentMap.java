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
 * Just like {@link java.util.Map} except methods may throw IOExceptions,
 * and read/write locks are built in.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 9 <!-- $-->, <!--$$JustDate:--> 01/12/05 <!-- $-->
 */
public interface PersistentMap {
    int size() throws IOException;

    boolean isEmpty() throws IOException;

    boolean containsKey(Object key) throws IOException;

    boolean containsValue(Object value) throws IOException;

    Object get(Object key) throws IOException;

    Object put(Object key, Object value) throws IOException;

    Object remove(Object key) throws IOException;

    void putAll(PersistentMap map) throws IOException;

    void clear() throws IOException;

    PersistentSet keySet() throws IOException;

    PersistentCollection values() throws IOException;

    PersistentSet entrySet() throws IOException;

    ReadWriteLock lock();

    static interface Entry {
        Object getKey() throws IOException;

        Object getValue() throws IOException;

        Object setValue(Object value) throws IOException;
    }
}
