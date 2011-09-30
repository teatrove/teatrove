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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 
 *
 * @author Nick Hagan
 */
public class ParameterizedTypeHandler
    extends AbstractGenericHandler<ParameterizedType> {

    private GenericType rawType;
    private GenericType[] typeArguments;

    public ParameterizedTypeHandler(GenericType rootType,
                                    ParameterizedType genericType) {
        super(rootType, genericType);
    }

    @Override
    public GenericType getRawType() {
        if (this.rawType == null) {
            ParameterizedType ptype = this.getGenericType();
            Type raw = ptype.getRawType();
            if (raw instanceof Class<?>) {
                this.rawType = new GenericType
                (
                    this.getRootType(),
                    (Class<?>) raw, this.getGenericType()
                );
            }
            else {
                this.rawType =
                    new GenericType(this.getRootType(), ptype.getRawType());
            }
        }

        return this.rawType;
    }

    @Override
    public GenericType[] getTypeArguments() {
        if (this.typeArguments == null) {
            ParameterizedType paramType = this.getGenericType();
            Type[] arguments = paramType.getActualTypeArguments();
            this.typeArguments = new GenericType[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                this.typeArguments[i] =
                    new GenericType(this.getRootType(), arguments[i]);
            }
        }

        return this.typeArguments;
    }
}
