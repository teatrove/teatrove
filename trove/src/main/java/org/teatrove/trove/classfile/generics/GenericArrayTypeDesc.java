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

/**
 * 
 *
 * @author Nick Hagan
 */
public class GenericArrayTypeDesc
    extends AbstractGenericTypeDesc<GenericArrayType> {

    private final GenericTypeDesc componentType;

    public static GenericArrayTypeDesc forType(GenericTypeDesc componentType) {
        return InternFactory.intern(new GenericArrayTypeDesc(componentType));
    }

    public static GenericArrayTypeDesc forType(GenericArrayType type) {
        return InternFactory.intern(new GenericArrayTypeDesc(type));
    }

    protected GenericArrayTypeDesc(GenericTypeDesc componentType) {
        this.componentType = componentType;
    }

    protected GenericArrayTypeDesc(GenericArrayType type) {
        this.componentType =
            GenericTypeFactory.fromType(type.getGenericComponentType());
    }

    public GenericTypeDesc getComponentType() {
        return this.componentType;
    }

    public String getSignature() {
        return "[".concat(this.componentType.getSignature());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (!(other instanceof GenericArrayTypeDesc)) { return false; }

        GenericArrayTypeDesc var = (GenericArrayTypeDesc) other;
        return this.componentType.equals(var.componentType);
    }

    @Override
    public int hashCode() {
        return (17 * this.componentType.hashCode());
    }

    @Override
    public String toString() {
        return this.getComponentType().toString().concat("[]");
    }
}
