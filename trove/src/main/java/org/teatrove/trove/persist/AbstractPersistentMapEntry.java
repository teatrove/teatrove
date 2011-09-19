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

package org.teatrove.trove.persist;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Abstract PersistentMap.Entry implementation that makes it easier to
 * define new Map entries.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 1 <!-- $-->, <!--$$JustDate:--> 02/01/03 <!-- $-->
 */
public abstract class AbstractPersistentMapEntry
    implements PersistentMap.Entry
{
    /**
     * Always throws UnsupportedOperationException.
     */
    public Object setValue(Object value) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof PersistentMap.Entry)) {
            return false;
        }
        
        PersistentMap.Entry e = (PersistentMap.Entry)obj;
        
        try {
            Object key = getKey();
            Object value = getValue();
            return 
                (key == null ?
                 e.getKey() == null : key.equals(e.getKey())) &&
                (value == null ?
                 e.getValue() == null : value.equals(e.getValue()));
        }
        catch (IOException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
    
    public int hashCode() {
        try {
            Object key = getKey();
            Object value = getValue();
            return 
                (key == null ? 0 : key.hashCode()) ^
                (value == null ? 0 : value.hashCode());
        }
        catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    public String toString() {
        String key;
        try {
            key = String.valueOf(getKey());
        }
        catch (IOException e) {
            key = e.toString();
        }
        
        String value;
        try {
            value = String.valueOf(getValue());
        }
        catch (IOException e) {
            value = e.toString();
        }
        
        return key + '=' + value;
    }
}
