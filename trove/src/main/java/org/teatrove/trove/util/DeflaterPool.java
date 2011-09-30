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
 * Since {@link Deflater Deflaters} can be expensive to allocate, re-use them
 * with this pool.
 *
 * @author Brian S O'Neill
 * @version
 */
public class DeflaterPool {
    private static final int LIMIT;

    private static List cPool;
    private static List cNoWrapPool;

    static {
        LIMIT = Integer.getInteger("org.teatrove.trove.util.DeflaterPool.LIMIT", 8).intValue();
    }

    public static synchronized void clear() {
        if (cPool != null) {
            Iterator it = cPool.iterator();
            while (it.hasNext()) {
                ((Deflater)it.next()).end();
            }
            cPool = null;
        }
        if (cNoWrapPool != null) {
            Iterator it = cNoWrapPool.iterator();
            while (it.hasNext()) {
                ((Deflater)it.next()).end();
            }
            cNoWrapPool = null;
        }
    }

    public static synchronized Deflater get(int level, boolean nowrap) {
        List pool;

        if (nowrap) {
            if ((pool = cNoWrapPool) == null) {
                return new Deflater(level, true);
            }
        }
        else {
            if ((pool = cPool) == null) {
                return new Deflater(level, false);
            }
        }

        if (pool.isEmpty()) {
            return new Deflater(level, nowrap);
        }

        Deflater d = (Deflater)pool.remove(pool.size() - 1);
        d.setLevel(level);
        d.setStrategy(Deflater.DEFAULT_STRATEGY);
        return d;
    }

    public static Deflater get(int level) {
        return get(level, false);
    }

    public static Deflater get() {
        return get(Deflater.DEFAULT_COMPRESSION, false);
    }

    public static synchronized void put(Deflater d) {
        d.reset();
        List pool;

        if (d.isNoWrap()) {
            if ((pool = cNoWrapPool) == null) {
                pool = cNoWrapPool = new ArrayList(LIMIT);
            }
        }
        else {
            if ((pool = cPool) == null) {
                pool = cPool = new ArrayList(LIMIT);
            }
        }

        if (pool.size() < LIMIT) {
            pool.add(d);
        }
        else {
            d.end();
        }
    }
}
