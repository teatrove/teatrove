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

package org.teatrove.teaservlet;

import org.teatrove.tea.runtime.Substitution;

/**
 * 
 * @author Brian S O'Neill
 */
public interface RegionCachingContext {

    /**
     * Caches and reuses a region of a page. The cached region expies after
     * a default time-to-live period has elapsed.
     *
     * @param s substitution block whose contents will be cached
     */
    public void cache(Substitution s) throws Exception;

    /**
     * Caches and reuses a region of a page. The cached region expies after
     * a default time-to-live period has elapsed. An additional parameter
     * is specified which helps to identify the uniqueness of the region.
     *
     * @param key key to further identify cache region uniqueness
     * @param s substitution block whose contents will be cached
     */
    public void cache(Object key, Substitution s)
        throws Exception;

    /**
     * Caches and reuses a region of a page. The cached region expies after
     * the specified time-to-live period has elapsed.
     *
     * @param ttlMillis maximum time to live of cached region, in milliseconds
     * @param s substitution block whose contents will be cached
     */
    public void cache(long ttlMillis, Substitution s) throws Exception;

    /**
     * Caches and reuses a region of a page. The cached region expies after
     * the specified time-to-live period has elapsed. An additional parameter
     * is specified which helps to identify the uniqueness of the region.
     *
     * @param ttlMillis maximum time to live of cached region, in milliseconds
     * @param key key to further identify cache region uniqueness
     * @param s substitution block whose contents will be cached
     */
    public void cache(long ttlMillis, Object key, Substitution s)
        throws Exception;

    /**
     * Caches and reuses a region of a page. The cached region expies after
     * the specified time-to-live period has elapsed. An additional parameter
     * is specified which helps to identify the uniqueness of the region.
     * A boolean paramaeter indicates if the key parameter is used as the
     * only unique identifier for the cache region.
     *
     * @param ttlMillis maximum time to live of cached region, in milliseconds
     * @param key key to further identify cache region uniqueness
     * @param useCustomKeyOnly true to indicate that the key parameter is used
     * as the only unique identifier for the cache region
     * @param s substitution block whose contents will be cached
     */
    public void cache(long ttlMillis, Object key, boolean useCustomKeyOnly, Substitution s)
        throws Exception;

    public void nocache(Substitution s) throws Exception;

    public RegionCacheInfo getRegionCacheInfo();
    
    public RegionCachingApplication.ClusterCacheInfo getClusterCacheInfo();

    public int getCacheSize();

    public int getValidEntryCount();

    public int getInvalidEntryCount();

    public long getCacheGets();

    public long getCacheHits();

    public long getCacheMisses();

    public void resetDepotStats();

}
