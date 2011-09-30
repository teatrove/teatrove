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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * 
 *
 * @author Nick Hagan
 */
public class GenericTypeFactory {
    public static GenericTypeDesc fromType(Type type) {
        GenericTypeDesc desc = null;

        if (type instanceof TypeVariable) {
            desc = TypeVariableDesc.forType((TypeVariable<?>) type);
        }
        else if (type instanceof ParameterizedType) {
            desc = ParameterizedTypeDesc.forType((ParameterizedType) type);
        }
        else if (type instanceof WildcardType) {
            desc = WildcardTypeDesc.forType((WildcardType) type);
        }
        else if (type instanceof GenericArrayType) {
            desc = GenericArrayTypeDesc.forType((GenericArrayType) type);
        }
        else if (type instanceof Class) {
            desc = ClassTypeDesc.forType((Class<?>) type);
        }
        else {
            throw new IllegalStateException("unsupported generic type: " + type);
        }

        return desc;
    }
}
