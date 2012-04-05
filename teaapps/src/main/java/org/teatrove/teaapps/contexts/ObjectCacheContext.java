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
package org.teatrove.teaapps.contexts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Tea context that provides a simple and shared object cache in the
 * application. The context has a single shared cache with no boundaries or
 * constraints.
 * 
 * @author Scott Jappinen
 */
public class ObjectCacheContext {

	private Map<String, Object> mObjectCache = 
	    new ConcurrentHashMap<String, Object>();
	
	/**
	 * Get the object associated with the given key or <code>null</code> if it
	 * does not exist.
	 * 
	 * @param key The name of key associated with the cached object
	 * 
	 * @return The associated object or <code>null</code>
	 */
    public Object getCachedObject(String key) {
        return mObjectCache.get(key);
    }
    
    /**
     * Put the given object into the cache with the given key.
     * 
     * @param key The name of the key to associated with the object
     * @param value The value to place in the cache
     */
    public void putCachedObject(String key, Object value) {
        mObjectCache.put(key, value);
    }
    
    /**
     * Remove the object from the cache for the given key.
     * 
     * @param key The name of the key to remove from the cache
     * 
     * @return The value associated with the key that was removed or
     *         <code>null</code> if the value did not exist
     */
    public Object deleteCachedObject(String key) {
        return mObjectCache.remove(key);
    }
}

