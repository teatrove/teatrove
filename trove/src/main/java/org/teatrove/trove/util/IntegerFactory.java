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

/**
 * A factory for creating Integer objects. Under low load, creating new Integer
 * objects is quicker than using this factory. Under heavly load, this factory
 * can offer performance benefits because the garbage collector runs less
 * frequently.
 *
 * @author Brian S O'Neill
 */
public final class IntegerFactory {
    private static final Integer[] CACHE;
    private static final int CACHE_MASK;

    static {
        Integer i = Integer.getInteger
            ("org.teatrove.trove.util.IntegerFactory.capacity");
        int cacheSize = (i == null) ? 1024 : i.intValue();
        // Ensure cache size is even power of 2.
        cacheSize--;
        int shift = 0;
        while (cacheSize > 0) {
            shift++;
            cacheSize >>= 1;
        }
        cacheSize = 1 << shift;
        CACHE = new Integer[cacheSize];
        CACHE_MASK = cacheSize - 1;
    }

    // Although accessed by multiple threads, this method doesn't need to be
    // synchronized.
    public static Integer toInteger(int value) {
        Integer[] cache = CACHE;
        int index = value & CACHE_MASK;
        Integer v = cache[index];
        if (v == null || v.intValue() != value) {
            v = new Integer(value);
            cache[index] = v;
        }
        return v;
    }

    private IntegerFactory() {
    }
}
