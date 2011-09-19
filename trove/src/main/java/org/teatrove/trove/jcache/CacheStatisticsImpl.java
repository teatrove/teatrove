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

package org.teatrove.trove.jcache;

import org.teatrove.trove.util.Depot;
import javax.cache.CacheStatistics;


/**
 * This class implements the JCache CacheStatistics interface as part of the
 * Depot JCache adapter. <p>
 * 
 * Calls to Cache.peek() will not update any cache statistics.<p>
 *
 * For further info on the JCache API, see:<br>
 * https://jsr-107-interest.dev.java.net/javadoc/javax/cache/package-summary.html
 *
 * @author Guy A. Molinari
 */
public class CacheStatisticsImpl implements CacheStatistics {

    private Depot mDepot = null;

    CacheStatisticsImpl(Depot depot) {
        mDepot = depot;
    }

   
    /** 
     * Reset statistics counters.
     */
    public void clearStatistics() {
        mDepot.reset();
    }

    
    /**
     * Number of times an item was found cached
     * since start up or last "clear".
     */
    public int getCacheHits() {
        return (int) mDepot.getCacheHits();
    }

   
    /**
     * Number of times an item was not found in
     * the cache, necessitating back end retrieval
     * since start up or last "clear".
     */
    public int getCacheMisses() {
        return (int) mDepot.getCacheMisses();
    }


    /**
     * Size of the cache.  Not affected by clearing.
     */
    public int getObjectCount() {
        return mDepot.size();
    }


    /**
     * Always returns STATISTICS_ACCURACY_BEST_EFFORT.
     */
    public int getStatisticsAccuracy() {
        return STATISTICS_ACCURACY_BEST_EFFORT;
    }

}
