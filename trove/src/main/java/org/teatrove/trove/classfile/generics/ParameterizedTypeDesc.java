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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 
 *
 * @author Nick Hagan
 */
public class ParameterizedTypeDesc
    extends AbstractGenericTypeDesc<ParameterizedType> {

    private final ClassTypeDesc rawType;
    private final GenericTypeDesc[] typeArguments;

    public static ParameterizedTypeDesc forType(ClassTypeDesc rawType,
                                                GenericTypeDesc... typeArguments) {
        return InternFactory.intern(new ParameterizedTypeDesc(rawType, typeArguments));
    }

    public static ParameterizedTypeDesc forType(ParameterizedType type) {
        return InternFactory.intern(new ParameterizedTypeDesc(type));
    }

    protected ParameterizedTypeDesc(ClassTypeDesc rawType,
                                    GenericTypeDesc... typeArguments) {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
    }

    protected ParameterizedTypeDesc(ParameterizedType type) {
        this.rawType =
            (ClassTypeDesc) GenericTypeFactory.fromType(type.getRawType());

        Type[] args = type.getActualTypeArguments();
        this.typeArguments = new GenericTypeDesc[args.length];
        for (int i = 0; i < args.length; i++) {
            this.typeArguments[i] = GenericTypeFactory.fromType(args[i]);
        }
    }

    public ClassTypeDesc getRawType() {
        return this.rawType;
    }

    public GenericTypeDesc[] getTypeArguments() {
        return this.typeArguments;
    }

    public String getSignature() {
        StringBuilder buffer = new StringBuilder(256);

        buffer.append(this.rawType.getSignature(false))
              .append('<');

        for (GenericTypeDesc typeArgument : this.typeArguments) {
            buffer.append(typeArgument.getSignature());
        }

        buffer.append('>').append(';');
        return buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (!(other instanceof ParameterizedTypeDesc)) { return false; }

        boolean valid = false;
        ParameterizedTypeDesc type = (ParameterizedTypeDesc) other;
        if (this.rawType.equals(type.rawType)) {
            if (this.typeArguments.length == type.typeArguments.length) {
                valid = true;
                for (int i = 0; i < this.typeArguments.length; i++) {
                    if (!this.typeArguments[i].equals(type.typeArguments[i])) {
                        valid = false;
                        break;
                    }
                }
            }
        }

        return valid;
    }

    @Override
    public int hashCode() {
        int hash = (11 * this.rawType.hashCode());
        for (int i = 0; i < this.typeArguments.length; i++) {
            hash += ((13 * i) + (17 * this.typeArguments[i].hashCode()));
        }

        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append(this.rawType.toString()).append('<');
        for (int i = 0; i < this.typeArguments.length; i++) {
            if (i > 0) { buffer.append(", "); }
            buffer.append(this.typeArguments[i].toString());
        }

        buffer.append('>');
        return buffer.toString();
    }
}
