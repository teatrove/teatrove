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

package org.teatrove.teaservlet.util;

import java.util.Map;

/**
 * 
 * @author Brian S O'Neill
 */
public class NameValuePair<T> implements Comparable<NameValuePair<T>>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String mName;
    private T mValue;

    public NameValuePair(String name, T value) {
        mName = name;
        mValue = value;
    }

    public NameValuePair(Map.Entry<String, T> entry) {
        mName = entry.getKey();
        mValue = entry.getValue();
    }

    public final String getName() {
        return mName;
    }

    public final T getValue() {
        return mValue;
    }

    @SuppressWarnings("rawtypes")
    public boolean equals(Object other) {
        if (other instanceof NameValuePair) {
            NameValuePair<?> pair = (NameValuePair) other;
            if (getName() == null) {
                if (pair.getName() != null) {
                    return false;
                }
            }
            else {
                if (!getName().equals(pair.getName())) {
                    return false;
                }
            }
            if (getValue() == null) {
                if (pair.getValue() != null) {
                    return false;
                }
            }
            else {
                if (!getValue().equals(pair.getValue())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public int hashCode() {
        return (mName.hashCode() ^ 17) & (mValue.hashCode() ^ 11);
    }

    public String toString() {
        return getName() + '=' + getValue();
    }

    /**
     * Comparison is based on case-insensitive ordering of "name".
     */
    public int compareTo(NameValuePair<T> other) {
        String otherName = other.mName;

        if (mName == null) {
            return otherName == null ? 0 : 1;
        }

        if (otherName == null) {
            return -1;
        }

        return mName.compareToIgnoreCase(otherName);
    }
}
