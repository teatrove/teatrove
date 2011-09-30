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
 * Abstract Map.Entry implementation that makes it easier to define new Map
 * entries.
 *
 * @author Brian S O'Neill
 */
public abstract class AbstractMapEntry implements Map.Entry {
    /**
     * Always throws UnsupportedOperationException.
     */
    public Object setValue(Object value) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        
        Map.Entry e = (Map.Entry)obj;
        
        Object key = getKey();
        Object value = getValue();
        
        return 
            (key == null ?
             e.getKey() == null : key.equals(e.getKey())) &&
            (value == null ?
             e.getValue() == null : value.equals(e.getValue()));
    }
    
    public int hashCode() {
        Object key = getKey();
        Object value = getValue();
        
        return 
            (key == null ? 0 : key.hashCode()) ^
            (value == null ? 0 : value.hashCode());
    }

    public String toString() {
        return String.valueOf(getKey()) + '=' + String.valueOf(getValue());
    }
}
