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

package org.teatrove.trove.classfile.generics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 *
 * @author Nick Hagan
 */
public class InternFactory {
    private static Map<String, Map<Object, Object>> CACHE =
        new ConcurrentHashMap<String, Map<Object, Object>>();

    private InternFactory() {
        super();
    }

    @SuppressWarnings("unchecked")
    public static <G> G intern(G type) {
        Map<Object, Object> cache = getCache(type.getClass().getName());

        G existing = (G) cache.get(type);
        if (existing == null) {
            synchronized (cache) {
                existing = (G) cache.get(type);
                if (existing == null) {
                    existing = type;
                    cache.put(type, existing);
                }
            }
        }

        return existing;
    }

    protected static Map<Object, Object> getCache(String type) {
        Map<Object, Object> cache = CACHE.get(type);
        if (cache == null) {
            synchronized (CACHE) {
                cache = CACHE.get(type);
                if (cache == null) {
                    cache = new ConcurrentHashMap<Object, Object>();
                    CACHE.put(type, cache);
                }
            }
        }

        return cache;
    }
}
