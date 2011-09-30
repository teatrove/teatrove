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

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 *
 * @author Nick Hagan
 */
public class TypeVariableHandler
    extends AbstractGenericHandler<TypeVariable<?>> {

    private GenericType rawType;

    public TypeVariableHandler(GenericType rootType,
                               TypeVariable<?> genericType) {
        super(rootType, genericType);
    }

    @Override
    public GenericType getRawType() {
        if (this.rawType == null) {
            this.rawType = this.getRawType0();
        }

        return this.rawType;
    }

    @Override
    public int getDimensions() {
        return this.getRawType().getDimensions();
    }

    @Override
    public GenericType getComponentType() {
        return this.getRawType().getComponentType();
    }

    @Override
    public GenericType getRootComponentType() {
        return this.getRawType().getRootComponentType();
    }

    @Override
    public GenericType[] getTypeArguments() {
        return this.getRawType().getTypeArguments();
    }

    @SuppressWarnings("rawtypes")
    protected GenericType getRawType0() {
        TypeVariable<?> variable = this.getGenericType();

        // if no declaring root, try to resolve from type bounds
        // default to Object when type bounds does not exist
        GenericType rootType = this.getRootType();
        if (rootType == null) {
            Type[] bounds = variable.getBounds();
            if (bounds.length == 1) {
                return new GenericType(bounds[0]);
            }

            // no bounds found, so default to object
            return new GenericType(Object.class);
        }

        // otherwise, try to detect from the root object down to either the
        // method or class declaration encompassing the type variable
        String name = variable.getName();

        TypeParameter[] parameters = this.getTypeParameters(variable);
        for (int i = 0; i < parameters.length; i++) {
            TypeVariable<?> var = parameters[i].getParameter();

            // search for matching variable in the list of type params
            // from either the method, constructor or class declarations
            String variableName = parameters[i].getName();
            if (variableName.equals(name)) {
                GenericDeclaration declaration = var.getGenericDeclaration();

                // attempt to resolve the declared type argument of the
                // parameterized class by walking from the root to the
                // declared class
                GenericType result = null;
                if (declaration instanceof Class) {
                    result = resolveType
                    (
                        (Class) declaration,
                        this.getRootType(), parameters[i].getIndex()
                    );
                }

                // if unable to traverse the type, default to looking up
                // result from the bounds of the type
                if (result == null) {
                    Type[] bounds = var.getBounds();
                    if (bounds.length == 1) {
                        result =
                            new GenericType(this.getRootType(), bounds[0]);
                    }
                }

                // return if result found
                if (result != null) {
                    return result;
                }

                // otherwise, break and return object
                break;
            }
        }

        // unable to resolve, so default to object
        return new GenericType(Object.class);
    }

    protected GenericType resolveType(Class<?> typeClass,
                                      GenericType current, int index) {

        // use actual type if current not in tree
        if (!typeClass.isAssignableFrom(current.getType())) {
            return null;
        }

        // use current type if matches
        else if (typeClass.equals(current.getType())) {
            GenericType[] args = current.getTypeArguments();
            if (args != null && args.length > index) {
                return args[index];
            }

            return null;
        }

        // check if supertype matches
        GenericType supertype = current.getSupertype();
        if (supertype != null &&
            typeClass.isAssignableFrom(supertype.getType())) {

            // resolve type variable if supertype matches the type class
            if (supertype.getType().equals(typeClass)) {
                return resolveSubtype(supertype, index);
            }

            // otherwise recursively check the supertype on up the chain
            GenericType result =
                resolveType(typeClass, supertype, index);

            // if type found, return...otherwise, continue on to interfaces
            if (result != null) {
                return result;
            }
        }

        // check if any interface matches
        GenericType[] ifaces = current.getInterfaces();
        for (int j = 0; j < ifaces.length; j++) {
            GenericType iface = ifaces[j];

            // ignore if type class not in hiearchy of type clsas
            if (!typeClass.isAssignableFrom(iface.getType())) {
                continue;
            }

            // resolve type variable if match found
            if (iface.getType().equals(typeClass)) {
                return resolveSubtype(iface, index);
            }

            // otherwise recursively check the supertype on up the chain
            GenericType result = resolveType(typeClass, iface, index);

            // return if type found...otherwise, check remaining interfaces
            if (result != null) {
                return result;
            }
        }

        // none found so return null for no match
        return null;
    }

    protected GenericType resolveSubtype(GenericType type, int index) {
        GenericType result = null;
        Class<?> actualType = type.getType();
        Type genericType = type.getGenericType();

        // resolve against parameterized types
        if (genericType != null) {
            result = resolveSubtype(genericType, index);
        }

        // attempt to resolve against type variables
        if (result == null && actualType != null) {
            result = resolveSubtype(actualType, index);
        }

        // none found so return null for no match
        return result;
    }

    protected GenericType resolveSubtype(Type genericType, int index) {
        // check for available type arguments
        GenericType generic =
            new GenericType(this.getRootType(), genericType);

        GenericType[] arguments = generic.getTypeArguments();
        if (arguments != null && arguments.length > index) {
            return arguments[index].getRawType();
        }

        // none found so return null for no match
        return null;
    }

    protected GenericType resolveSubtype(Class<?> type, int index) {
        // attempt to resolve against type variables
        TypeVariable<?>[] variables = type.getTypeParameters();
        if (variables.length > index) {
            TypeVariable<?> variable = variables[index];
            Type[] bounds = variable.getBounds();
            if (bounds.length == 1) {
                return new GenericType(this.getRootType(), bounds[0]);
            }
        }

        // none found so return null for no match
        return null;
    }

    protected TypeParameter[] getTypeParameters(TypeVariable<?> variable) {
        Map<String, TypeParameter> parameters =
            new LinkedHashMap<String, TypeParameter>();

        GenericDeclaration declaration = variable.getGenericDeclaration();

        // handle method declarations
        // - pull variables from method, then declaring class
        // - method types override (or hide) class types, so ignore those
        if (declaration instanceof Method) {
            Method method = (Method) declaration;
            addTypeParameters(parameters, method);

            Class<?> declaringClass = method.getDeclaringClass();
            addTypeParameters(parameters, declaringClass);
        }

        // handle constructor declarations
        // - pull variables from constructor, then declaring class
        // - ctor types override (or hide) class types, so ignore those
        else if (declaration instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) declaration;
            addTypeParameters(parameters, ctor);

            Class<?> declaringClass = ctor.getDeclaringClass();
            addTypeParameters(parameters, declaringClass);
        }

        // handle class declarations
        else if (declaration instanceof Class) {
            Class<?> declaringClass = (Class<?>) declaration;
            addTypeParameters(parameters, declaringClass);
        }

        // default to just pull from declaration
        else {
            addTypeParameters(parameters, declaration);
        }

        // convert to array form
        int size = parameters.size();
        return parameters.values().toArray(new TypeParameter[size]);
    }

    protected void addTypeParameters(Map<String, TypeParameter> parameters,
                                     GenericDeclaration declaration) {
        TypeVariable<?>[] types = declaration.getTypeParameters();
        for (int i = 0; i < types.length; i++) {
            TypeVariable<?> type = types[i];
            String name = type.getName();
            if (!parameters.containsKey(name)) {
                parameters.put(name, new TypeParameter(type, i));
            }
        }
    }

    protected static class TypeParameter {
        private final TypeVariable<?> parameter;
        private final int index;

        public TypeParameter(TypeVariable<?> parameter, int index) {
            this.parameter = parameter;
            this.index = index;
        }

        public String getName() {
            return this.parameter.getName();
        }

        public TypeVariable<?> getParameter() {
            return this.parameter;
        }

        public int getIndex() {
            return this.index;
        }
    }
}
