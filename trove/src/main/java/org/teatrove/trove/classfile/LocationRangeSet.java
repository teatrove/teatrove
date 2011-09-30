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

package org.teatrove.trove.classfile;

import java.util.*;

/**
 * Produces a reduced sorted set of non-overlapping {@link LocationRange}
 * objects.
 *
 * @author Brian S O'Neill
 */
class LocationRangeSet {
    /**
     * Reduces a set of LocationRange objects.
     */
    public static SortedSet reduce(SortedSet locations) {
        SortedSet newSet = new TreeSet();
        Iterator it = locations.iterator();

        while (it.hasNext()) {
            LocationRange next = (LocationRange)it.next();

            if (newSet.size() == 0) {
                newSet.add(next);
                continue;
            }

            if (next.getStartLocation().compareTo
                (next.getEndLocation()) >= 0) {
                continue;
            }

            // Try to reduce the set by joining adjacent ranges or eliminating
            // overlap.

            LocationRange last = (LocationRange)newSet.last();

            if (next.getStartLocation().compareTo
                (last.getEndLocation()) <= 0) {

                if (last.getEndLocation().compareTo
                    (next.getEndLocation()) <= 0) {

                    newSet.remove(last);
                    newSet.add(new LocationRangeImpl(last, next));
                }
                continue;
            }

            newSet.add(next);
        }

        return newSet;
    }
}
