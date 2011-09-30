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

package org.teatrove.trove.generics;

import java.lang.reflect.Type;

/**
 * 
 *
 * @author Nick Hagan
 */
public abstract class AbstractGenericHandler<T extends Type>
    implements GenericHandler<T> {

    private GenericType rootType;
    private T genericType;

    public AbstractGenericHandler(GenericType rootType, T genericType) {
        this.rootType = rootType;
        this.genericType = genericType;
    }

    public T getGenericType() {
        return this.genericType;
    }

    public GenericType getRootType() {
        return this.rootType;
    }

    public int getDimensions() {
        return 0;
    }

    public GenericType getComponentType() {
        return null;
    }

    public GenericType getRootComponentType() {
        return null;
    }

    public GenericType[] getTypeArguments() {
        return null;
    }
}
