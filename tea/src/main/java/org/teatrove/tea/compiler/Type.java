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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.beans.IntrospectionException;
import org.teatrove.tea.TypedElement;
import org.teatrove.tea.util.BeanAnalyzer;
import org.teatrove.tea.util.KeyedPropertyDescriptor;

/**
 * Immutable representation of an expression's type.
 *
 * @author Brian S O'Neill
 * @version

 * @see org.teatrove.tea.parsetree.Expression
 */
public class Type implements java.io.Serializable {
    /** Type that is compatble with all other types */
    public static final Type NULL_TYPE = new Type(Object.class) {
        public String getSimpleName() {
            return toString();
        }

        public String getFullName() {
            return toString();
        }

        public String toString() {
            return "null-type";
        }
    };

    /** Type that represents void, provided as a convenience */
    public static final Type VOID_TYPE = new Type(void.class);

    /** Type that represents all objects, provided as a convenience */
    public static final Type OBJECT_TYPE = new Type(Object.class);

    /** Type for representing ints, provided as a convenience */
    public static final Type INT_TYPE = new Type(int.class);

    /** Type for representing longs, provided as a convenience */
    public static final Type LONG_TYPE = new Type(long.class);

    /** Type for representing booleans, provided as a convenience */
    public static final Type BOOLEAN_TYPE = new Type(boolean.class);

    /** Type for representing Strings, provided as a convenience */
    public static final Type STRING_TYPE = new Type(String.class);

    /** Type for representing non-null Strings, provided as a convenience */
    public static final Type NON_NULL_STRING_TYPE = STRING_TYPE.toNonNull();

    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

    private final Class mObjectClass;
    private final Class mNaturalClass;
    private final boolean mPrimitive;

    private transient boolean mCheckedForArrayLookup;
    private transient Type mArrayElementType;
    private transient Type[] mArrayIndexTypes;
    private transient Method[] mArrayAccessMethods;
    private transient boolean mCheckedForIteration;
    private transient Type mIterationElementType;

    public Type(Class type) {
        if (type.isPrimitive()) {
            mNaturalClass = type;
            mObjectClass = convertToObject(type);
            mPrimitive = true;
        }
        else {
            mObjectClass = mNaturalClass = type;
            mPrimitive = false;
        }
    }

    private Type(Class object, Class natural) {
        mObjectClass = object;
        mNaturalClass = natural;
        if (natural.isPrimitive()) {
            mPrimitive = true;
        }
        else {
            mPrimitive = false;
        }
    }

    public Type(Class type, TypedElement te) {
        this(type);
        mIterationElementType = new Type(te.value());
    }

    Type(Type type) {
        mObjectClass = type.mObjectClass;
        mNaturalClass = type.mNaturalClass;
        mPrimitive = type.mPrimitive;
        
        mCheckedForArrayLookup = type.mCheckedForArrayLookup;
        mArrayElementType = type.mArrayElementType;
        mArrayIndexTypes = type.mArrayIndexTypes;
        mArrayAccessMethods = type.mArrayAccessMethods;
        mCheckedForIteration = type.mCheckedForIteration;
        mIterationElementType = type.mIterationElementType;
    }

    /**
     * Class returned never represents a primitive type.
     */
    public Class getObjectClass() {
        return mObjectClass;
    }

    /**
     * Returns the natural class for this type. If type is primitive, then its
     * primitive peer is returned.
     */
    public Class getNaturalClass() {
        return mNaturalClass;
    }

    /**
     * Returns true if this type is a primitive.
     */
    public boolean isPrimitive() {
        return mPrimitive;
    }

    /**
     * Returns true if this type is not primitive, but it has a primitive 
     * type peer.
     */
    public boolean hasPrimitivePeer() {
        if (mObjectClass == Integer.class ||
            mObjectClass == Boolean.class ||
            mObjectClass == Byte.class ||
            mObjectClass == Character.class ||
            mObjectClass == Short.class ||
            mObjectClass == Long.class ||
            mObjectClass == Float.class ||
            mObjectClass == Double.class ||
            mObjectClass == Void.class) {
            
            return true;
        }

        return false;
    }

    /**
     * Returns a new type from this one that represents a primitive type.
     * If this type cannot be represented by a primitive, then this is
     * returned.
     */
    public Type toPrimitive() {
        if (mPrimitive) {
            return this;
        }
        else {
            return new Type(mObjectClass, convertToPrimitive(mObjectClass));
        }
    }

    /**
     * Returns a new type from this one that represents a non-primitive type.
     * If this type actually is primitive, the returned type is marked as not
     * being able to reference null. i.e. if this type is int,
     * new Type(Integer.class).toNonNull() is returned.
     */
    public Type toNonPrimitive() {
        if (mPrimitive) {
            return new Type(mObjectClass).toNonNull();
        }
        else {
            return this;
        }
    }

    /**
     * Returns true if this type cannot reference null. For primitive types,
     * true is always returned.
     */
    public boolean isNonNull() {
        return isPrimitive();
    }

    /**
     * Returns true if this type can reference null, or simply the opposite
     * result of isNonNull.
     */
    public boolean isNullable() {
        return !isNonNull();
    }

    /**
     * Returns this type converted such that it cannot reference null.
     */
    public Type toNonNull() {
        if (isNonNull()) {
            return this;
        }
        else {
            return new Type(this) {
                public boolean isNonNull() {
                    return true;
                }
                
                public boolean isNullable() {
                    return false;
                }

                public Type toNullable() {
                    return Type.this;
                }
            };
        }
    }

    /**
     * Returns this type converted such that it can reference null. The
     * resulting type is never primitive.
     */
    public Type toNullable() {
        if (!isNonNull()) {
            return this;
        }
        else {
            return new Type(mObjectClass);
        }
    }

    /**
     * If this Type supports array lookup, then return the element type.
     * Otherwise, null is returned.
     */
    public Type getArrayElementType() throws IntrospectionException {
        if (!mCheckedForArrayLookup) {
            checkForArrayLookup();
        }
        
        return mArrayElementType;
    }

    /**
     * If this Type supports array lookup, then return the index type. Because
     * the index type may be overloaded, an array is returned.
     * Null is returned if this Type doesn't support array lookup.
     */
    public Type[] getArrayIndexTypes() throws IntrospectionException {
        if (!mCheckedForArrayLookup) {
            checkForArrayLookup();
        }

        return mArrayIndexTypes == null ? null :
            (Type[])mArrayIndexTypes.clone();
    }

    /**
     * If this Type supports array lookup, then return all of the methods
     * that can be called to access the array. If there are no methods, then
     * an empty array is returned. Null is returned only if this Type
     * doesn't support array lookup.
     */
    public Method[] getArrayAccessMethods() throws IntrospectionException {
        if (!mCheckedForArrayLookup) {
            checkForArrayLookup();
        }

        return mArrayAccessMethods == null ? null : 
            (Method[])mArrayAccessMethods.clone();
    }

    /**
     * If this type supports iteration, then the element type is returned.
     * Otherwise, null is returned.
     */
    public Type getIterationElementType() throws IntrospectionException {
        if (!mCheckedForIteration) {
            mCheckedForIteration = true;

            if (mIterationElementType != null)
                return mIterationElementType;

            if (mNaturalClass.isArray()) {
                mIterationElementType = getArrayElementType();
            }
            else if (Collection.class.isAssignableFrom(mNaturalClass) ||
                    Map.class.isAssignableFrom(mNaturalClass)) {
                mIterationElementType = OBJECT_TYPE;

                try {
                    Field field = 
                        mNaturalClass.getField
                        (BeanAnalyzer.ELEMENT_TYPE_FIELD_NAME);
                    if (field.getType() == Class.class &&
                        Modifier.isStatic(field.getModifiers())) {
                        
                        mIterationElementType = 
                            new Type((Class)field.get(null));
                    }
                }
                catch (NoSuchFieldException e) {
                }
                catch (IllegalAccessException e) {
                }
            }
        }

        return mIterationElementType;
    }

    /**
     * Returns true if this type supports iteration in the reverse direction.
     */
    public boolean isReverseIterationSupported() {
        return mNaturalClass.isArray() || 
            List.class.isAssignableFrom(mNaturalClass) ||
            Set.class.isAssignableFrom(mNaturalClass) ||
            Map.class.isAssignableFrom(mNaturalClass) ;
    }

    private void checkForArrayLookup() throws IntrospectionException {
        mCheckedForArrayLookup = true;

        if (mObjectClass.isArray()) {
            mArrayElementType = new Type(mObjectClass.getComponentType());
            mArrayAccessMethods = EMPTY_METHOD_ARRAY;
            mArrayIndexTypes = new Type[] {INT_TYPE};
            return;
        }

        try {
            Map properties = BeanAnalyzer.getAllProperties(mObjectClass);
            
            KeyedPropertyDescriptor keyed = 
                (KeyedPropertyDescriptor)properties.get
                (BeanAnalyzer.KEYED_PROPERTY_NAME);
            
            if (keyed == null) {
                return;
            }
            
            mArrayElementType = new Type(keyed.getKeyedPropertyType());
            mArrayAccessMethods = keyed.getKeyedReadMethods();
        }
        catch (ClassCastException e) {
            return;
        }

        int length = mArrayAccessMethods.length;
        mArrayIndexTypes = new Type[length];
        for (int i=0; i<length; i++) {
            Method m = mArrayAccessMethods[i];
            mArrayIndexTypes[i] = new Type(m.getReturnType());
        }
    }

    /**
     * Accessed by the TypeChecker, to override the default.
     */
    Type setArrayElementType(Type elementType) throws IntrospectionException {
        Type type = new Type(mObjectClass, mNaturalClass);
        type.checkForArrayLookup();
        type.mArrayElementType = elementType;
        return type;
    }

    public String getSimpleName() {
        if (mNaturalClass.isArray()) {
            int dim = 0;
            Class baseNat = mNaturalClass;
            Type baseType = this;
            while (baseNat.isArray()) {
                dim++;
                baseNat = baseNat.getComponentType();
                try {
                    baseType = baseType.getArrayElementType();
                }
                catch (IntrospectionException e) {
                    baseType = new Type(baseNat);
                }
            }

            String baseName = baseType.getSimpleName();
            StringBuffer nameBuf = 
                new StringBuffer(baseName.length() + dim * 2);
            nameBuf.append(baseName);

            while (dim-- > 0) {
                nameBuf.append('[');
                nameBuf.append(']');
            }

            return nameBuf.toString();
        }
        else if (mPrimitive) {
            return mNaturalClass.getName();
        }
        else {
            String name = mObjectClass.getName();
            int index = name.lastIndexOf('.');
            if (index >= 0) {
                name = name.substring(index + 1);
            }
            return name;
        }
    }

    public String getFullName() {
        if (isPrimitive()) {
            return mNaturalClass.getName();
        }

        StringBuffer nameBuf = new StringBuffer(20);
        if (isNonNull()) {
            nameBuf.append("non-null ");
        }

        if (!mNaturalClass.isArray()) {
            nameBuf.append(mObjectClass.getName());
        }
        else {
            int dim = 0;
            Class baseNat = mNaturalClass;
            Type baseType = this;
            while (baseNat.isArray()) {
                dim++;
                baseNat = baseNat.getComponentType();
                try {
                    baseType = baseType.getArrayElementType();
                }
                catch (IntrospectionException e) {
                    baseType = new Type(baseNat);
                }
            }

            String baseName = baseType.getFullName();
            nameBuf.append(baseName);

            while (dim-- > 0) {
                nameBuf.append('[');
                nameBuf.append(']');
            }
        }

        return nameBuf.toString();
    }

    public String toString() {
        if (!isPrimitive() && isNonNull()) {
            return "non-null " + mNaturalClass.getName();
        }
        else {
            return mNaturalClass.getName();
        }
    }

    public int hashCode() {
        return mNaturalClass.hashCode();
    }

    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        
        if (another instanceof Type) {
            Type t = (Type)another;

            if (this == NULL_TYPE || another == NULL_TYPE) {
                return false;
            }

            if (mNaturalClass == t.mNaturalClass &&
                isNonNull() == t.isNonNull()) {

                if (!mCheckedForArrayLookup && !t.mCheckedForArrayLookup) {
                    // Don't recursively check array element type. They
                    // will be the same. This test fixes an infinite recursion
                    // bug if the element type is of the same type.
                    return true;
                }

                try {
                    Type thisArrayType = getArrayElementType();
                    Type otherArrayType = t.getArrayElementType();
                    
                    if (thisArrayType != null) {
                        if (thisArrayType.equals(otherArrayType)) {
                            return true;
                        }
                    }
                    else {
                        if (otherArrayType == null) {
                            return true;
                        }
                    }
                }
                catch (IntrospectionException e) {
                }
            }
        }
        
        return false;
    }

    /**
     * Returns a type that is compatible with this type, and the one passed in.
     * The type returned is selected using a best-fit algorithm.
     *
     * <p>If the type passed in represents a primitive type, but this type is
     * not, the type returned is an object (or a subclass of), but never a 
     * primitive type. Compatible primitive types are returned when both this
     * and the parameter type were already primitive types.
     *
     * <p>Input types which are arrays are also supported by this method.
     *
     * <p>Returns null if the given type isn't compatible with this one.
     */
    public Type getCompatibleType(Type other) {
        if (other == null) {
            return null;
        }

        if (equals(other)) {
            if (this == NULL_TYPE) {
                return other;
            }
            else {
                return this;
            }
        }

        Class classA = mObjectClass;
        Class classB = other.mObjectClass;

        Type compat;

        if (classA == Void.class) {
            if (classB == Void.class) {
                compat = this;
            }
            else {
                return null;
            }
        }
        else if (classB == Void.class) {
            return null;
        }
        else if (other == NULL_TYPE) {
            compat = this.toNullable();
        }
        else if (this == NULL_TYPE) {
            compat = other.toNullable();
        }
        else if (Number.class.isAssignableFrom(classA) &&
            Number.class.isAssignableFrom(classB)) {

            Class clazz = compatibleNumber(classA, classB);
            if (isPrimitive() && other.isPrimitive()) {
                compat = new Type(clazz, convertToPrimitive(clazz));
            }
            else {
                compat = new Type(clazz);
            }
        }
        else {
            compat = new Type(findCommonBaseClass(classA, classB));
        }

        if (isNonNull() && other.isNonNull()) {
            compat = compat.toNonNull();
        }

        return compat;
    }
    
    /**
     * Returns the most specific common superclass or interface that can be 
     * used to represent both of the specified classes. Null is only returned
     * if either class refers to a primitive type and isn't the same as the 
     * other class.
     */
    public static Class findCommonBaseClass(Class a, Class b) {
        Class clazz = findCommonBaseClass0(a, b);
        
        if (clazz != null && clazz.isInterface()) {
            // Only return interface if it actually defines something.
            if (clazz.getMethods().length <= 0) {
                //clazz = Object.class;
            }
        }

        return clazz;
    }
    
    private static Class findCommonBaseClass0(Class a, Class b) {
        if (a == b) {
            return a;
        }

        if (a.isPrimitive() || b.isPrimitive()) {
            return null;
        }

        if (a.isArray() && b.isArray()) {
            Class clazz = findCommonBaseClass(a.getComponentType(), 
                                              b.getComponentType());

            if (clazz == null) {
                return Object.class;
            }

            return Array.newInstance(clazz, 0).getClass();
        }

        // Determine the intersection set of all the classes, superclasses and 
        // interfaces from the passed in classes.
        Set set = new HashSet(19);
        addToClassSet(set, a);

        Set setB = new HashSet(19);
        addToClassSet(setB, b);

        set.retainAll(setB);

        int size = set.size();
        if (size == 1) {
            return (Class)set.iterator().next();
        }
        else if (size == 0) {
            return Object.class;
        }
        
        // Reduce the set by removing classes/interfaces that are extended and
        // interfaces that are implemented by other classes.
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Class x = (Class)i.next();
            Iterator j = set.iterator();
            while (j.hasNext()) {
                Class y = (Class)j.next();
                if (x != y && x.isAssignableFrom(y)) {
                    i.remove();
                    break;
                }
            }
        }

        size = set.size();
        if (size == 1) {
            return (Class)set.iterator().next();
        }
        else if (size == 0) {
            return Object.class;
        }

        // Reduce the set by discarding interfaces.
        i = set.iterator();
        while (i.hasNext()) {
            if (((Class)i.next()).isInterface()) {
                i.remove();
            }
        }

        if (set.size() == 1) {
            return (Class)set.iterator().next();
        }

        return Object.class;
    }

    private static void addToClassSet(Set set, Class clazz) {
        if (clazz == null || !set.add(clazz)) {
            return;
        }

        addToClassSet(set, clazz.getSuperclass());

        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            addToClassSet(set, interfaces[i]);
        }
    }

    private static Class compatibleNumber(Class classA, Class classB) {
        if (classA == Integer.class) {
            
            if (classB == Integer.class ||
                classB == Byte.class || classB == Short.class) {
                
                return Integer.class;
            }

            if (classB == Long.class) {
                return classB;
            }
        }
        else if (classA == Byte.class || classA == Short.class) {
            if (classB == Integer.class ||
                classB == Byte.class || classB == Short.class) {
                
                return Integer.class;
            }

            if (classB == Long.class) {
                return classB;
            }

            if (classB == Float.class) {
                return Float.class;
            }
        }
        else if (classA == Float.class) {
            if (classB == Float.class) {
                return classB;
            }
            
            if (classB == Byte.class || classB == Short.class) {
                return Float.class;
            }
        }
        else if (classA == Long.class) {
            if (classB == Integer.class ||
                classB == Byte.class || classB == Short.class ||
                classB == Long.class) {
                
                return Long.class;
            }
        }

        return Double.class;
    }

    /**
     * If class passed in represents a primitive type, its object peer is
     * returned. Otherwise, it is returned unchanged.
     */
    private static Class convertToObject(Class type) {
        if (type == int.class) {
            return Integer.class;
        }
        else if (type == boolean.class) {
            return Boolean.class;
        }
        else if (type == byte.class) {
            return Byte.class;
        }
        else if (type == char.class) {
            return Character.class;
        }
        else if (type == short.class) {
            return Short.class;
        }
        else if (type == long.class) {
            return Long.class;
        }
        else if (type == float.class) {
            return Float.class;
        }
        else if (type == double.class) {
            return Double.class;
        }
        else if (type == void.class) {
            return Void.class;
        }
        else {
            return type;
        }
    }

    /**
     * If class passed in has a primitive type peer, it is returned.
     * Otherwise, it is returned unchanged.
     */
    private static Class convertToPrimitive(Class type) {
        if (type == Integer.class) {
            return int.class;
        }
        else if (type == Boolean.class) {
            return boolean.class;
        }
        else if (type == Byte.class) {
            return byte.class;
        }
        else if (type == Character.class) {
            return char.class;
        }
        else if (type == Short.class) {
            return short.class;
        }
        else if (type == Long.class) {
            return long.class;
        }
        else if (type == Float.class) {
            return float.class;
        }
        else if (type == Double.class) {
            return double.class;
        }
        else if (type == Void.class) {
            return void.class;
        }
        else {
            return type;
        }
    }

    /**
     * Returns the conversion cost of assigning the given type to this type.
     * Conversions are allowed between arrays as well if they have the
     * same dimensions. If no legal conversion exists, -1 is returned. 
     * The conversion costs are as follows:
     * 
     * <ol>
     * <li>any type is assignable by its own type
     * <li>any superclass type is assignable by a subclass type
     * <li>any object with a primitive peer can be converted to its primitive
     * <li>any primitive can be converted to its object peer
     * <li>any byte or short can be widened to an int
     * <li>any byte, short or int can be widened to a long
     * <li>any byte or short can be widened to a float
     * <li>any primitive number can be widened to a double
     * <li>any Number object can be converted to a primitive and widened
     * <li>any primitive number can be converted to a Number object
     * <li>any primitive number can be widened to a Number object
     * <li>any Number object can be widened to another Number object
     * <li>any primitive number can be narrowed to a double
     * <li>any Number object can be narrowed to a double
     * <li>any primitive number can be narrowed to a Double object
     * <li>any Number object can be narrowed to a Double object
     * <li>any primitive number can be narrowed to a long
     * <li>any Number object can be narrowed to a long
     * <li>any primitive number can be narrowed to a Long object
     * <li>any Number object can be narrowed to a Long object
     * <li>any primitive number can be narrowed to a float
     * <li>any Number object can be narrowed to a float
     * <li>any primitive number can be narrowed to a Float object
     * <li>any Number object can be narrowed to a Float object
     * <li>any primitive number can be narrowed to a int
     * <li>any Number object can be narrowed to a int
     * <li>any primitive number can be narrowed to a Integer object
     * <li>any Number object can be narrowed to a Integer object
     * <li>any primitive number can be narrowed to a short
     * <li>any Number object can be narrowed to a short
     * <li>any primitive number can be narrowed to a Short object
     * <li>any Number object can be narrowed to a Short object
     * <li>any primitive number can be narrowed to a byte
     * <li>any Number object can be narrowed to a byte
     * <li>any primitive number can be narrowed to a Byte object
     * <li>any Number object can be narrowed to a Byte object
     * <li>any primitive can be converted to an object
     * <li>NULL_TYPE can be converted to any object
     * <li>anything can be converted to a String
     * </ol>
     *
     * @return the conversion cost, or -1 if the other can't assign to this
     */
    public int convertableFrom(Type other) {
        if (equals(other)) {
            return 1;
        }

        Class thisNat = mNaturalClass;
        Class otherNat = other.mNaturalClass;

        if (thisNat.isAssignableFrom(otherNat)) {
            return 2;
        }

        if (other == NULL_TYPE && !thisNat.isPrimitive()) {
            return 38;
        }

        boolean arrayConversion = thisNat.isArray() && otherNat.isArray();
        if (arrayConversion) {
            // Get dimensions of each array.
            int thisDim = 0;
            while (thisNat.isArray()) {
                thisDim++;
                thisNat = thisNat.getComponentType();
            }

            int otherDim = 0;
            while (otherNat.isArray()) {
                otherDim++;
                otherNat = otherNat.getComponentType();
            }

            if (thisDim != otherDim) {
                return -1;
            }
        }

        int aCode = typeCode(thisNat);
        if (aCode < 0) {
            return -1;
        }

        int cost;
        int bCode = typeCode(otherNat);
        if (bCode < 0) {
            if (aCode == 18) {
                // Anything can be converted to a String.
                return 40;
            }
            else {
                return -1;
            }
        }

        cost = mCostTable[bCode][aCode];

        if (cost == 40 && arrayConversion) {
            // Since types are both arrays, lower the cost so that a conversion
            // from int[] to String[] is preferred over int[] to String.
            cost--;
        }

        return cost;
    }

    private static int typeCode(Class clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) {
                return 0;
            }
            else if (clazz == char.class) {
                return 1;
            }
            else if (clazz == byte.class) {
                return 2;
            }
            else if (clazz == short.class) {
                return 3;
            }
            else if (clazz == int.class) {
                return 4;
            }
            else if (clazz == float.class) {
                return 5;
            }
            else if (clazz == long.class) {
                return 6;
            }
            else if (clazz == double.class) {
                return 7;
            }
        }
        else {
            if (clazz == Boolean.class) {
                return 8;
            }
            else if (clazz == Character.class) {
                return 9;
            }
            else if (clazz == Byte.class) {
                return 10;
            }
            else if (clazz == Short.class) {
                return 11;
            }
            else if (clazz == Integer.class) {
                return 12;
            }
            else if (clazz == Float.class) {
                return 13;
            }
            else if (clazz == Long.class) {
                return 14;
            }
            else if (clazz == Double.class) {
                return 15;
            }
            else if (Number.class.isAssignableFrom(clazz)) {
                return 16;
            }
            else if (clazz == Object.class) {
                return 17;
            }
            else if (clazz == String.class) {
                return 18;
            }
        }

        return -1;
    }

    // 19 by 19 two dimensional byte array. [from][to]
    private static byte[][] mCostTable =
    {
        //0  1   2  3  4  5  6  7   8  9  10 11 12 13 14 15  16  17 18        
                                                                              
        { 1,-1, -1,-1,-1,-1,-1,-1,  4,-1, -1,-1,-1,-1,-1,-1, -1, 37,40}, //  0
        {-1, 1, -1,-1,-1,-1,-1,-1, -1, 4, -1,-1,-1,-1,-1,-1, -1, 37,40}, //  1
                                                                              
        {-1,-1,  1, 5, 5, 7, 6, 8, -1,-1,  4,11,11,11,11,11, 10, 37,40}, //  2
        {-1,-1, 33, 1, 5, 7, 6, 8, -1,-1, 35, 4,11,11,11,11, 10, 37,40}, //  3
        {-1,-1, 33,29, 1,21, 6, 8, -1,-1, 35,31, 4,23,11,11, 10, 37,40}, //  4
        {-1,-1, 33,29,25, 1,17, 8, -1,-1, 35,31,27, 4,19,11, 10, 37,40}, //  5
        {-1,-1, 33,29,25,21, 1,13, -1,-1, 35,31,27,23, 4,15, 10, 37,40}, //  6
        {-1,-1, 33,29,25,21,17, 1, -1,-1, 35,31,27,23,19, 4, 10, 37,40}, //  7
                                                                              
        { 3,-1, -1,-1,-1,-1,-1,-1,  1,-1, -1,-1,-1,-1,-1,-1, -1,  2,40}, //  8
        {-1, 3, -1,-1,-1,-1,-1,-1, -1, 1, -1,-1,-1,-1,-1,-1, -1,  2,40}, //  9
                                                                              
        {-1,-1,  3, 9, 9, 9, 9, 9, -1,-1,  1,12,12,12,12,12,  2,  2,40}, // 10
        {-1,-1, 34, 3, 9, 9, 9, 9, -1,-1, 36, 1,12,12,12,12,  2,  2,40}, // 11
        {-1,-1, 34,30, 3,22, 9, 9, -1,-1, 36,32, 1,24,12,12,  2,  2,40}, // 12
        {-1,-1, 34,30,26, 3,18, 9, -1,-1, 36,32,28, 1,20,12,  2,  2,40}, // 13
        {-1,-1, 34,30,26,22, 3,14, -1,-1, 36,32,28,24, 1,16,  2,  2,40}, // 14
        {-1,-1, 34,30,26,22,18, 3, -1,-1, 36,32,28,24,20, 1,  2,  2,40}, // 15
                                                                              
        {-1,-1, 34,30,26,22,18,14, -1,-1, 36,32,28,24,20,16,  1,  2,40}, // 16
                                                                              
        {-1,-1, -1,-1,-1,-1,-1,-1, -1,-1, -1,-1,-1,-1,-1,-1, -1,  1,40}, // 17
        {-1,-1, -1,-1,-1,-1,-1,-1, -1,-1, -1,-1,-1,-1,-1,-1, -1,  2, 1}, // 18
        {66,82, 73,65,78,32,83,32, 79,39, 78,69,73,76,76,-1, -1, -1,-1}  // 19
    };
}
