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
import java.util.Comparator;

/**
 * Just like {@link java.util.SortedSet} except methods may throw IOExceptions.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 6 <!-- $-->, <!--$$JustDate:--> 01/12/05 <!-- $-->
 */
public interface PersistentSortedSet extends PersistentSet {
    Comparator comparator();

    PersistentSortedSet subSet(Object fromElement, Object toElement)
        throws IOException;

    PersistentSortedSet headSet(Object toElement) throws IOException;

    PersistentSortedSet tailSet(Object fromElement) throws IOException;

    Object first() throws IOException;

    Object last() throws IOException;
}
