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

/**
 * 
 * @author Brian S O'Neill
 */
class LocationRangeImpl implements LocationRange {
    private final Location mStart;
    private final Location mEnd;

    LocationRangeImpl(Location a, Location b) {
        if (a.compareTo(b) <= 0) {
            mStart = a;
            mEnd = b;
        }
        else {
            mStart = b;
            mEnd = a;
        }
    }

    LocationRangeImpl(LocationRange a, LocationRange b) {
        mStart = (a.getStartLocation().compareTo(b.getStartLocation()) <= 0) ?
            a.getStartLocation() : b.getStartLocation();

        mEnd = (b.getEndLocation().compareTo(a.getEndLocation()) >= 0) ?
            b.getEndLocation() : a.getEndLocation();
    }

    public Location getStartLocation() {
        return mStart;
    }

    public Location getEndLocation() {
        return mEnd;
    }

    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }

    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        LocationRange other = (LocationRange)obj;

        int result = getStartLocation().compareTo(other.getStartLocation());

        if (result == 0) {
            result = getEndLocation().compareTo(other.getEndLocation());
        }

        return result;
    }

    public String toString() {
        int start = getStartLocation().getLocation();
        int end = getEndLocation().getLocation() - 1;

        if (start == end) {
            return String.valueOf(start);
        }
        else {
            return start + ".." + end;
        }
    }
}
