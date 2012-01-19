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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Jappinen
 */
public class ObjectCacheContext {

	private Map<String, Object> mObjectCache = new HashMap<String, Object>();
	
    public Object getCachedObject(String key) {
        return mObjectCache.get(key);
    }
    
    public void putCachedObject(String key, Object value) {
        mObjectCache.put(key, value);
    }
    
    public Object deleteCachedObject(String key) {
        return mObjectCache.remove(key);
    }
}

