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

/**
 * Simple object for pairing two objects together for use as a hash or tree
 * key.
 *
 * @author Brian S O'Neill
 * @see MultiKey
 */
public class Pair implements Comparable, java.io.Serializable {
    private final Object mObj1;
    private final Object mObj2;

    public Pair(Object obj1, Object obj2) {
        mObj1 = obj1;
        mObj2 = obj2;
    }

    public Object getFirst() {
        return mObj1;
    }

    public Object getSecond() {
        return mObj2;
    }
    
    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        Pair other = (Pair)obj;

        Object a = mObj1;
        Object b = other.mObj1;

        firstTest: {
            if (a == null) {
                if (b != null) {
                    return 1;
                }
                // Both a and b are null.
                break firstTest;
            }
            else {
                if (b == null) {
                    return -1;
                }
            }

            int result = ((Comparable)a).compareTo(b);
            
            if (result != 0) {
                return result;
            }
        }

        a = mObj2;
        b = other.mObj2;
        
        if (a == null) {
            if (b != null) {
                return 1;
            }
            // Both a and b are null.
            return 0;
        }
        else {
            if (b == null) {
                return -1;
            }
        }
        
        return ((Comparable)a).compareTo(b);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }
        
        Pair key = (Pair)obj;
        
        return 
            (mObj1 == null ?
             key.mObj1 == null : mObj1.equals(key.mObj1)) &&
            (mObj2 == null ?
             key.mObj2 == null : mObj2.equals(key.mObj2));
    }
    
    public int hashCode() {
        return 
            (mObj1 == null ? 0 : mObj1.hashCode()) +
            (mObj2 == null ? 0 : mObj2.hashCode());
    }
    
    public String toString() {
        return "[" + mObj1 + ':' + mObj2 + ']';
    }
}
