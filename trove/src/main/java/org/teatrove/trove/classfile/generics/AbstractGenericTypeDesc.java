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

import java.lang.reflect.Type;

/**
 * 
 *
 * @author Nick Hagan
 */
public abstract class AbstractGenericTypeDesc<T extends Type>
    implements GenericTypeDesc {

    public AbstractGenericTypeDesc() {
        super();
    }

    @Override
    public int hashCode() {
        return this.getSignature().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) { return true; }
        else if (!(object instanceof GenericTypeDesc)) { return false; }

        GenericTypeDesc other = (GenericTypeDesc) object;
        return this.getSignature().equals(other.getSignature());
    }

    protected void resolve(T type) {
        // nothing to do
    }
}
