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

package org.teatrove.tea.runtime;

/**
 * Object used by template substitutions to uniquely identify themselves.
 *
 * @author Brian S O'Neill
 */
public class SubstitutionId extends java.lang.ref.WeakReference<Class<?>> {
    private final int mBlockId;

    public SubstitutionId(Object template, int blockId) {
        // Weakly reference the template class to allow it to be garbage
        // collected.
        super(template.getClass());
        mBlockId = blockId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SubstitutionId) {
            SubstitutionId other = (SubstitutionId)obj;
            if (mBlockId == other.mBlockId) {
                Object c = get();
                return c != null && c == other.get();
            }
        }
        return false;
    }
    
    public int hashCode() {
        Object c = get();
        return (c == null) ? mBlockId : c.hashCode() + mBlockId;
    }
    
    public String toString() {
        Class<?> c = get();
        if (c != null) {
            return c.getName() + '.' + mBlockId;
        }
        else {
            return super.toString() + '.' + mBlockId;
        }
    }
}
