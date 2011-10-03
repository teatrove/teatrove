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

import java.io.Serializable;
import org.teatrove.trove.util.Depot;

/**
 * At the moment this class just contains a single int, but this may 
 * change as the RegionCachingApplication evolves.
 *
 * @author Jonathan Colwell
 */
public class RegionCacheInfo implements Serializable {
        
    private int size;
    private int validSize;
    private int invalidSize;
    private long gets;
    private long hits;
    private long misses;
    private int avgBytes;

    RegionCacheInfo(Depot depot) {
        //mDepot = depot;
        size = depot.size();
        validSize = depot.validSize();
        invalidSize = depot.invalidSize();
        gets = depot.getCacheGets();
        hits = depot.getCacheHits();
        misses = depot.getCacheMisses();
        avgBytes = depot.calculateAvgPerEntrySize();
    }

    public int getSize() {
        return size;
    }

    public int getValidSize() {
        return validSize;
    }

    public int getInvalidSize() {
        return invalidSize;
    }

    public long getCacheGets() {
        return gets;
    }

    public long getCacheHits() {
        return hits;
    }

    public long getCacheMisses() {
        return misses;
    }

    public int getAvgEntrySizeInBytes() {
        return avgBytes;
    }
}
