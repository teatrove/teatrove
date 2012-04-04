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
 * A Set that orders its elements based on how recently they have been used.
 * Most recently used elements appear first in the Set. Elements are marked as
 * being used whenever they are added to the Set. To re-position an element,
 * re-add it.
 *
 * @author Brian S O'Neill
 */
public class UsageSet<T> extends MapBackedSet<T> {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a UsageSet in forward order, MRU first.
     */
    public UsageSet() {
        this(new HashMap<T, Object>());
    }

    public UsageSet(int capactity) {
        this(new HashMap<T, Object>(capactity));
    }

    /**
     * @param backingMap map to use for storage
     */
    @SuppressWarnings("unchecked")
    public UsageSet(Map<T, Object> backingMap) {
        super(new UsageMap(backingMap));
    }

    /**
     * With reverse order, elements are ordered least recently used first. The
     * ordering of the elements will be consistent with the order they were
     * added in. Switching to and from reverse order is performed quickly
     * and is not affected by the current size of the set.
     */
    public void setReverseOrder(boolean reverse) {
        ((UsageMap)mMap).setReverseOrder(reverse);
    }

    /**
     * Returns the first element in the set, the most recently used. If reverse
     * order, then the least recently used is returned.
     */
    public Object first() throws NoSuchElementException {
        return ((UsageMap)mMap).firstKey();
    }

    /**
     * Returns the last element in the set, the least recently used. If reverse
     * order, then the most recently used is returned.
     */
    public Object last() throws NoSuchElementException {
        return ((UsageMap)mMap).lastKey();
    }
}
