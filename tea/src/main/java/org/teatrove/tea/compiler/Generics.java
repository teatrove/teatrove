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

package org.teatrove.tea.compiler;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * 
 * @author Nick Hagan
 */
public class Generics {

    private Generics() {
        super();
    }

    @SuppressWarnings("rawtypes")
    public static Type findType(Type type, Type... parents) {

        // ignore if no parent type
        if (parents == null || parents.length == 0) {
            return null;
        }

        // get generic types and verify type variable
        java.lang.reflect.Type ttype = type.getGenericClass();
        if (!(ttype instanceof TypeVariable)) {
            return null;
        }

        // check if type can be resolved
        TypeVariable tvar = (TypeVariable) ttype;
        for (java.lang.reflect.Type bounds : tvar.getBounds()) {
            if (bounds instanceof Class && !Object.class.equals(bounds)) {
                return new Type((Class) bounds);
            }
        }

        // search each parent type
        for (Type parent : parents) {
            TypeVariable[] tvars = parent.getObjectClass().getTypeParameters();
            for (int i = 0; i < tvars.length; i++) {
                if (tvar.getName().equals(tvars[i].getName())) {
                    java.lang.reflect.Type ptype = parent.getGenericClass();
                    if (ptype instanceof ParameterizedType) {
                        ParameterizedType pptype = (ParameterizedType) ptype;
                        java.lang.reflect.Type[] ptypes =
                            pptype.getActualTypeArguments();

                        if (i < ptypes.length) {
                            return getUnderlyingType(ptypes[i]);
                        }
                    }
                }
            }
        }

        // no matching type found
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static Class<?> getRawType(ParameterizedType type) {

        // get the raw object associated with the template
        java.lang.reflect.Type rawtype = type.getRawType();

        // ensure valid type
        if (rawtype instanceof Class) {
            return (Class) rawtype;
        }

        // raw type should only be classes
        else {
            throw new IllegalStateException
            (
                "ParameterizedType cannot have a non-Class " +
                "based rawType: " + rawtype
            );
        }
    }

    public static Type getBoundedType(TypeVariable<?> type) {

        // fail if missing bounds
        if (type.getBounds().length == 0) {
            throw new IllegalStateException
            (
                "TypeVariable has missing bounds information: " + type
            );
        }

        // get the bounds around the type to resolve to actual type
        java.lang.reflect.Type ttype = type.getBounds()[0];
        return getUnderlyingType(ttype);
    }

    public static Type getBoundedType(WildcardType type) {

        // fail if missing bounds
        if (type.getUpperBounds().length == 0) {
            throw new IllegalStateException
            (
                "WildcardType has missing bounds information: " + type
            );
        }

        // get the bounds around the type to resolve to actual type
        java.lang.reflect.Type ttype = type.getUpperBounds()[0];
        return getUnderlyingType(ttype);
    }

    @SuppressWarnings("rawtypes")
    public static Type getUnderlyingType(java.lang.reflect.Type ttype) {

        // handle <? extends Object> case
        if (ttype instanceof Class) {
            return new Type((Class) ttype);
        }

        // handle <? extends E> case
        else if (ttype instanceof TypeVariable) {

            // get underlying bounded type
            return getBoundedType((TypeVariable) ttype);
        }

        // handle <? extends Template<Object>> case
        else if (ttype instanceof ParameterizedType) {

            // get the underlying parameterized type
            Class<?> ptype = getRawType((ParameterizedType) ttype);
            return new Type(ptype, ttype);
        }

        // handle <? extends Template<Object>[]> case
        else if (ttype instanceof GenericArrayType) {

            // find root type
            Class<?> ctype = getComponentType((GenericArrayType) ttype);
            return new Type(ctype, ttype);
        }

        // handle <? extends E> case
        else if (ttype instanceof WildcardType) {
            java.lang.reflect.Type[] ubounds =
                ((WildcardType) ttype).getUpperBounds();
            if (ubounds != null && ubounds.length > 0) {
                return getUnderlyingType(ubounds[0]);
            }

            java.lang.reflect.Type[] lbounds =
                ((WildcardType) ttype).getLowerBounds();
            if (lbounds != null && lbounds.length > 0) {
                return getUnderlyingType(lbounds[0]);
            }
        }

        // unknown type
        throw new IllegalStateException
        (
            "type has invalid bounds type: " + ttype
        );
    }

    @SuppressWarnings("rawtypes")
    public static Class<?> getComponentType(GenericArrayType type) {

        // find root type and count dimensions
        int levels = 0;
        java.lang.reflect.Type ctype = type;
        while (ctype instanceof GenericArrayType) {
            levels++;
            ctype = ((GenericArrayType) ctype).getGenericComponentType();
        }

        // handle <E extends Object[]> case
        if (ctype instanceof Class) {
            return Array.newInstance((Class) ctype, new int[levels]).getClass();
        }

        // handle <E extends Template<Object>[]> case
        else if (ctype instanceof ParameterizedType) {

            // get the raw object associated with the template
            Class<?> rawtype = getRawType((ParameterizedType) ctype);
            return Array.newInstance(rawtype, new int[levels]).getClass();
        }

        // handle <E extends F[]> case
        else if (ctype instanceof TypeVariable) {

            // get the bounded underlying type
            Type jtype = getBoundedType((TypeVariable) ctype);
            return Array.newInstance(jtype.getObjectClass(), new int[levels])
                        .getClass();
        }

        // should not be any other way
        else {
            throw new IllegalStateException
            (
                "GenericArrayType has invalid component " +
                "type: " + ctype
            );
        }
    }

    public static Type getIterationType(Type type) {
        return getIterationType(type.getGenericClass());
    }

    @SuppressWarnings("rawtypes")
    public static Type getIterationType(java.lang.reflect.Type generic) {

        // handle parameterized cases (List<E>)
        if (generic instanceof ParameterizedType) {

            // find and return the actual subtype
            java.lang.reflect.Type[] subtypes =
                ((ParameterizedType) generic).getActualTypeArguments();
            if (subtypes != null && subtypes.length >= 1) {
                java.lang.reflect.Type subtype = subtypes[0];

                // handle List<Object> case
                if (subtype instanceof Class) {
                    return new Type((Class) subtype);
                }

                // handle List<Template<Object>> case
                else if (subtype instanceof ParameterizedType) {
                    return new Type(getRawType((ParameterizedType) subtype),
                                    subtype);
                }

                // handle List<E> case where E is defined at the type level
                else if (subtype instanceof TypeVariable) {
                    return getBoundedType((TypeVariable) subtype);
                }

                // handle List<?> case
                else if (subtype instanceof WildcardType) {
                    return getBoundedType((WildcardType) subtype);
                }

                // handle List<Object[]> case
                else if (subtype instanceof GenericArrayType) {

                    // find root type
                    Class<?> ctype = getComponentType((GenericArrayType) subtype);
                    return new Type(ctype, subtype);
                }
            }
        }

        // handle actual classes and look for supercase
        else if (generic instanceof Class) {
            return getIterationType(((Class) generic).getGenericSuperclass());
        }

        // unknown type, so return object
        return Type.OBJECT_TYPE;
    }

    public static Type getKeyType(Type type) {
        return getIterationType(type);
    }

    public static Type getKeyType(java.lang.reflect.Type generic) {
        return getIterationType(generic);
    }

    public static Type getValueType(Type type) {
        return getValueType(type.getGenericClass());
    }

    @SuppressWarnings("rawtypes")
    public static Type getValueType(java.lang.reflect.Type generic) {

        // handle parameterized cases (List<E>)
        if (generic instanceof ParameterizedType) {

            // find and return the actual subtype
            java.lang.reflect.Type[] subtypes =
                ((ParameterizedType) generic).getActualTypeArguments();
            if (subtypes != null && subtypes.length >= 1) {
                java.lang.reflect.Type subtype = subtypes[1];

                // handle List<Object> case
                if (subtype instanceof Class) {
                    return new Type((Class) subtype);
                }

                // handle List<Template<Object>> case
                else if (subtype instanceof ParameterizedType) {
                    return new Type(getRawType((ParameterizedType) subtype),
                                    subtype);
                }

                // handle List<E> case where E is defined at the type level
                else if (subtype instanceof TypeVariable) {
                    return getBoundedType((TypeVariable) subtype);
                }

                // handle List<?> case
                else if (subtype instanceof WildcardType) {
                    return getBoundedType((WildcardType) subtype);
                }

                // handle List<Object[]> case
                else if (subtype instanceof GenericArrayType) {

                    // find root type
                    Class<?> ctype = getComponentType((GenericArrayType) subtype);
                    return new Type(ctype, subtype);
                }
            }
        }

        // unknown type, so return object
        return Type.OBJECT_TYPE;
    }
}
