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

import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheFactory;
import javax.cache.CacheException;


/**
 * This class is a helper adapter that eases the use of CacheFactory
 * with the Spring framework.
 *
 * @author Ryan Christianson
 *
 */
public class SpringFactory {

    private CacheFactory mSource = null;
    private Map mEnv = null;

    public SpringFactory(CacheFactory source, Map env) {
        mSource = source;
        mEnv = env;
    }

    public Cache createCache() throws CacheException {
        return mSource.createCache(mEnv);
    }

}


