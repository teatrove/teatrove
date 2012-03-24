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
public class GenericType {
    private GenericType rootType;
    private Class<?> type;
    private Type genericType;

    private boolean _supertype;
    private GenericType supertype;
    private GenericType[] interfaces;

    private GenericHandler<?> handler;

    public GenericType(Class<?> type) {
        this((GenericType) null, type);
    }

    public GenericType(Type genericType) {
        this((GenericType) null, genericType);
    }

    public GenericType(Class<?> type, Type genericType) {
        this(null, type, genericType);
    }

    public GenericType(GenericType rootType, Class<?> type) {
        this.type = type;
        this.genericType = type;
        this.rootType = rootType == null ? this : rootType;
    }

    public GenericType(GenericType rootType, Type genericType) {
        this.genericType = genericType;
        this.rootType = rootType == null ? this : rootType;

        // lookup base type from handler
        // NOTE: this ignores the root type and just tries to resolve
        //       by the actual type bounds.
        if (genericType instanceof Class) {
            this.type = (Class<?>) genericType;
        }
        else {
            GenericType rawType =
                this.getHandler(null, genericType).getRawType();

            this.type = rawType.getType();
        }
    }

    public GenericType(GenericType rootType,
                       Class<?> type, Type genericType) {
        this.type = type;
        this.genericType = (genericType == null ? type : genericType);
        this.rootType = rootType == null ? this : rootType;
    }

    public GenericType getRootType() {
        return this.rootType;
    }

    public Class<?> getType() {
        return this.type;
    }

    public Type getGenericType() {
        return this.genericType;
    }

    public GenericType getSupertype() {
        if (!this._supertype) {
            this._supertype = true;
            if (this.type.getSuperclass() != null) {
                this.supertype = new GenericType
                (
                    this.rootType,
                    this.type.getSuperclass(),
                    this.type.getGenericSuperclass()
                );
            }
        }

        return this.supertype;
    }

    public GenericType[] getInterfaces() {
        if (this.interfaces == null) {
            Class<?>[] ifaces = this.type.getInterfaces();
            Type[] genericIfaces = this.type.getGenericInterfaces();

            this.interfaces = new GenericType[ifaces.length];
            for (int i = 0; i < ifaces.length; i++) {
                this.interfaces[i] = new GenericType
                (
                    this.rootType, ifaces[i], genericIfaces[i]
                );
            }
        }

        return this.interfaces;
    }

    public GenericType getRawType() {
        return this.getHandler().getRawType();
    }

    public boolean isInterface() {
        return this.type.isInterface();
    }
    
    public boolean isArray() {
        return this.getDimensions() > 0;
    }

    public int getDimensions() {
        return this.getHandler().getDimensions();
    }

    public GenericType getComponentType() {
        return this.getHandler().getComponentType();
    }

    public GenericType getRootComponentType() {
        return this.getHandler().getRootComponentType();
    }

    public GenericType getTypeArgument(int index) {
        GenericType[] args = this.getTypeArguments();
        if (args != null && args.length > index) {
            return args[index];
        }

        return null;
    }

    public GenericType[] getTypeArguments() {
        return this.getHandler().getTypeArguments();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) { return true; }
        else if (!(object instanceof GenericType)) { return false; }

        GenericType other = (GenericType) object;

        // compare raw types (List<X> = List, E = Object, etc)
        Class<?> thisType = this.getType();
        Class<?> otherType = other.getType();
        if ((thisType != null && otherType == null) ||
            (thisType == null && otherType != null) ||
            (thisType != null && !thisType.equals(otherType))) {
            return false;
        }

        // compare type args
        GenericType[] thisArgs = this.getTypeArguments();
        GenericType[] otherArgs = other.getTypeArguments();

        // if no arg specified, assume base type as valid only
        // ie: List = List<E>
        if ((thisArgs == null || thisArgs.length == 0) ||
            (otherArgs == null || otherArgs.length == 0)) {
            return true;
        }

        // compare each argument for validity
        //     List<?> != List<Number>
        int len = thisArgs.length;
        for (int i = 0; i < len; i++) {
            if (!thisArgs[i].equals(otherArgs[i])) {
                return false;
            }
        }

        // valid
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode += (type == null ? 11 : 11 * type.hashCode());

        /* NOTE: we do not do the following since List equals List<Number>
                 to allow non-generics form to be still valid..if we did
                 hashCode here then List.hashCode != List<Nubmer>.hashCode

        GenericType[] args = this.getTypeArguments();
        if (args != null) {
            for (GenericType arg : args) {
                hashCode += (arg.hashCode() * 17);
            }
        }
        */

        return hashCode;
    }

    @Override
    public String toString() {
        if (this.type != null) {
            return this.type.getName();
        }
        else if (this.genericType != null) {
            return this.genericType.toString();
        }
        else {
            return "null";
        }
    }

    protected GenericHandler<?> getHandler() {
        if (this.handler == null) {
            this.handler = getHandler(this.getRootType(),
                                      this.getGenericType());
        }

        return this.handler;
    }

    protected GenericHandler<?> getHandler(GenericType rootType,
                                           Type genericType) {
        if (genericType instanceof Class) {
            return new ClassHandler
            (
                rootType, (Class<?>) genericType
            );
        }
        else if (genericType instanceof ParameterizedType) {
            return new ParameterizedTypeHandler
            (
                rootType, (ParameterizedType) genericType
            );
        }
        else if (genericType instanceof TypeVariable) {
            return new TypeVariableHandler
            (
                rootType, (TypeVariable<?>) genericType
            );
        }
        else if (genericType instanceof WildcardType) {
            return new WildcardTypeHandler
            (
                rootType, (WildcardType) genericType
            );
        }
        else if (genericType instanceof GenericArrayType) {
            return new GenericArrayTypeHandler
            (
                rootType, (GenericArrayType) genericType
            );
        }
        else {
            throw new IllegalStateException
            (
                "generic type not supported: " + genericType.getClass()
            );
        }
    }
}
