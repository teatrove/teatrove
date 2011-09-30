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

package org.teatrove.trove.classfile;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 *
 * @author Nick Hagan
 */
public final class MethodUtils {

    /*
     * Private constructor
     */
    private MethodUtils() {
        super();
    }
    
    /**
     * Get the set of methods that require implementation based on the given
     * interface.  If the types are specified, then they will be passed through
     * and methods overridden per the generic types.  This may result in bridged
     * methods.  If parent is specified, then only non-implemented methods will
     * be returned.
     * 
     * @param iface  The associated interface
     * @param types  The set of named type variables
     * @param parent  The optional parent
     * 
     * @return  The set of methods to implement
     */    
    public static 
    Set<MethodEntry> getMethodsToImplement(Class<?> iface,
                                           Map<String, Class<?>> types,
                                           Class<?> parent) {
        // TODO: this does not take in account type params defined at the
        // method level yet..ie: <C extends Something> C getC() { ... }
        
        Set<MethodEntry> methods = new HashSet<MethodEntry>();
        addMethods(methods, iface, types, parent);
        return methods;
    }

    /**
     * Get the set of methods that require implementation based on the given
     * method for the given interface.  If the types are specified, then they 
     * will be passed through and methods overridden per the generic types.  
     * This may result in zero or more bridged methods corresponding to the
     * given method.
     * 
     * @param iface  The associated interface
     * @param types  The set of named type variables
     * @param parent  The optional parent
     * @param method  The method to implement
     * 
     * @return  The set of methods to implement
     */
    public static
    Set<MethodEntry> getMethodsToImplement(Class<?> iface,
                                           Map<String, Class<?>> types,
                                           Class<?> parent, Method method) {
        // TODO: this does not take in account type params defined at the
        // method level yet..ie: <C extends Something> C getC() { ... }
        
        Set<MethodEntry> methods = new HashSet<MethodEntry>();
        addMethods(methods, iface, types, parent, method);
        return methods;        
    }
    
    /**
     * Get the number of dimensions for the specified type.  If the type is
     * a {@link GenericArrayType}, the dimensions will equal the number of
     * actual dimensions.  Otherwise, 0 will be returned.
     * 
     * @param type  The underlying type
     * 
     * @return  The number of dimensions
     */
    private static int getDimensions(Type type) {
        int dimensions = 0;
        while (type instanceof GenericArrayType) {
            dimensions++;
            type = ((GenericArrayType) type).getGenericComponentType();
        }
        
        return dimensions;
    }

    /**
     * Get the {@link TypeVariable} instance for the specified {@link Type}.
     * If the type is not a {@link TypeVariable}, then <code>null</code> is
     * returned.  If the type is a {@link GenericArrayType}, its root type is
     * looked up and then checked.
     * 
     * @param type  The associated type
     * 
     * @return  The type variable instance or <code>null</code>
     */
    private static TypeVariable<?> getTypeVariable(Type type) {
        // find root type if generic array
        while (type instanceof GenericArrayType) {
            type = ((GenericArrayType) type).getGenericComponentType();
        }
        
        // return type variable if valid
        if (type instanceof TypeVariable) {
            return (TypeVariable<?>) type;
        }
        
        // non-type variable, return null
        return null;
    }
    
    /**
     * Check whether a bridge is required for the given method and generic
     * type information.  If the return type or any parameter contains a type
     * variable that is specified and overridden with a more specific type
     * via the given types, then a bridge will be required.
     * 
     * @param types  The set of named type variables
     * @param method  The method to check
     * 
     * @return  The state of whether a bridge is required
     */
    private static boolean isBridgeRequired(Map<String, Class<?>> types,
                                            Method method) {
        // check return type
        TypeVariable<?> var = getTypeVariable(method.getGenericReturnType());
        if (var != null && types.containsKey(var.getName())) {
            return true;
        }
        
        // check param types
        for (Type paramType : method.getGenericParameterTypes()) {
            var = getTypeVariable(paramType);
            if (var != null && types.containsKey(var.getName())) {
                return true;
            }            
        }
        
        // not required
        return false;
    }
    
    /**
     * Find the specified method on the given class.  If not specified, then
     * <code>null</code> is returned.  Otherwise, the associated method is
     * returned.  The specified method definition does not need to be from the
     * specified class.  This method is usually used to check a method defined
     * in an interface or superclass on another class within the hierarchy.
     * 
     * @param clazz  The class to check
     * @param method  The method definition
     * 
     * @return The associated method if existant, otherwise <code>null</code>
     */
    private static Method findMethod(Class<?> clazz, Method method) {
        try {
            return clazz.getMethod(method.getName(), 
                                   method.getParameterTypes());
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Get the root class type based on the given type and set of named type
     * variables.  If the type is already a Class, it is renamed as is.  If the
     * type is a parameterized type, then the raw type is returned.  If the
     * type is a wildcard, the lower bounds is returned.  Otherwise, if the 
     * type is a type variable, its associated value from the types is returned.
     * 
     * @param types  The named type variales to pass thru
     * @param type  The associated type
     * 
     * @return  The underlying root class for the type
     */
    private static final Class<?> getRootType(Map<String, Class<?>> types,
                                              Type type) {
        
        // if type explicitly defined, pass thru as is
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        
        // if type defined as parameter, pass thru root type
        else if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            return getRootType(types, raw);
        }
        
        // if type defined as wildcard, pass thru bounds
        // NOTE: this does not support ? super X format
        else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            if (bounds.length >= 1) {
                return getRootType(types, bounds[0]);
            }
        }
        
        // otherwise, either generic array or plain type variable
        // lookup in types and return specified type if provided
        else {
            TypeVariable<?> raw = getTypeVariable(type);
            if (raw != null) {
                // verify type is overridden
                String name = raw.getName();
                if (types.containsKey(name)) {
                    // get actual type and convert to array if necessary
                    Class<?> actual = types.get(name);
                    int dimensions = getDimensions(type);
                    if (dimensions >= 1) {
                        actual = Array.newInstance
                        (
                            actual, dimensions
                        ).getClass();
                    }
                    
                    return actual;
                }
            }
        }
        
        // nothing found
        return null;
    }

    /**
     * Get the actual type for the given type.  If the type is a type variable
     * that is specified in the given types, then the associated class is
     * returned.  Otherwise, the specified class is returned.
     * 
     * @param types  The set of named type variables
     * @param type  The generic type of the method
     * @param clazz  The underlying type of the method
     * 
     * @return  The actual type to use in the method
     */
    private static Class<?> getMethodType(Map<String, Class<?>> types, 
                                          Type type, Class<?> clazz) {
        
        // check if type is an actual type variable
        TypeVariable<?> variable = getTypeVariable(type);
        if (variable != null) {
            
            // verify type is overridden
            String name = variable.getName();
            if (types.containsKey(name)) {
                
                // convert to array if necessary
                Class<?> actual = types.get(name);
                int dimensions = getDimensions(type);
                if (dimensions > 0) {
                    actual = Array.newInstance(actual, dimensions).getClass();
                }
                
                // return overridden type
                return actual;
            }
        }
        
        // nothing overriden, so just return base class
        return clazz;
    }
    
    /**
     * Add a method to the set of existing methods.  If the method is not
     * bridged, it will overwrite any existing signature.  Otherwise, it will
     * only update the set if not previously defined.  Note that the signature
     * of the method entry includes the method name, parameter types, and the
     * return type.
     * 
     * @param methods  The set of methods
     * @param method  The method to add
     */
    private static void addMethod(Set<MethodEntry> methods, 
                                  MethodEntry method) {
        if (!method.isBridged()) {
            methods.remove(method);
            methods.add(method);
        }
        else if (!methods.contains(method)) {
            methods.add(method);
        }
    }
    
    /**
     * Recursive method that adds all applicable methods for the given class,
     * which is expected to be an interface.  The tree is walked for the class
     * and all super interfaces in recursive fashion to get each available 
     * method.  Any method that is already defined by the given parent will be
     * ignored.  Otherwise, the method, including any required bridged methods
     * will be added.  The specified list of types define the optional type
     * parameters of the given interface class.  These types will propogate up
     * the tree as necessary for any super-interfaces that match.
     * 
     * @param methods  The set of existing methods
     * @param clazz  The current interface class
     * @param types  The set of named type variable
     * @param parent  The optional parent class
     */
    private static void addMethods(Set<MethodEntry> methods, 
                                   Class<?> clazz, Map<String, Class<?>> types,
                                   Class<?> parent) {
        
        // validate interface
        if (!clazz.isInterface()) { 
            throw new IllegalStateException("class must be interface: " + clazz);
        }
        
        // loop through each method (only those declared)
        for (Method method : clazz.getDeclaredMethods()) {
            addMethods(methods, clazz, types, parent, method);
        }

        // recursively add each super interface providing type info if valid
        addParentMethods(methods, clazz, types, parent, null);
    }
    
    /**
     * Recursive method that adds all applicable methods related to the given 
     * class, which is expected to be an interface.  The tree is walked for the 
     * class and all super interfaces in recursive fashion to get each available 
     * method that matches the provided method.  Any method that is already 
     * defined by the given parent will be ignored.  Otherwise, the method, 
     * including any required bridged methods will be added.  The specified list 
     * of types define the optional type parameters of the given interface 
     * class.  These types will propogate up the tree as necessary for any 
     * super-interfaces that match.
     * 
     * @param methods  The set of existing methods
     * @param clazz  The current interface class
     * @param types  The set of named type variable
     * @param parent  The optional parent class
     * @param method  The method to implement
     */
    private static void addMethods(Set<MethodEntry> methods, 
                                   Class<?> clazz, Map<String, Class<?>> types,
                                   Class<?> parent, Method method) {
        
        // validate interface
        //if (!clazz.isInterface()) { 
        //    throw new IllegalStateException("class must be interface: " + clazz);
        //}
        
        // check if the method requires a bridge and add generic method
        // with overridden type params
        MethodEntry impl = null;
        boolean bridged = isBridgeRequired(types, method);
        
        // do not add method if already implemented
        // ignore if method already defined on the parent
        if (!bridged && parent != null && findMethod(parent, method) != null) {
            return;
        }
        
        // bridge if necessary
        if (bridged) {
            // get the underying generic type if provided
            Class<?> returnType = getMethodType
            (
                types, method.getGenericReturnType(), method.getReturnType()
            );
            
            // get the underlying generic params if provided
            Type[] paramTypes = method.getGenericParameterTypes();
            Class<?>[] paramClasses = method.getParameterTypes();
            for (int i = 0; i < paramClasses.length; i++) {
                paramClasses[i] = getMethodType
                (
                    types, paramTypes[i], paramClasses[i]
                );
            }
            
            // check if only return type is overridden and only add impl
            // if not yet defined...this is possible since return type is
            // not part of a method signature for equality purposes so we
            // only need to implement in a concrete implementation once and
            // bridge all other methods.  Parameters, on the other hand
            // must always be explicitly implemented to satisfy their
            // interface definition.
            MethodEntry bridgeMethod = null;
            if (Arrays.equals(paramClasses, method.getParameterTypes())) {
                for (MethodEntry mentry : methods) {
                    if (mentry.getName().equals(method.getName()) &&
                        Arrays.equals(mentry.getParamTypes(), 
                                      method.getParameterTypes())) {
                        bridgeMethod = mentry;
                    }
                }
            }
            
            // add implementation
            impl = new MethodEntry(
                method.getName(), returnType, paramClasses, bridgeMethod
            );
            addMethod(methods, impl);
        }
        
        // add actual method definition, bridging if implemented above
        addMethod(methods, new MethodEntry(method.getName(),
                                           method.getReturnType(), 
                                           method.getParameterTypes(),
                                           impl));
        
        // recursively add each super interface providing type info if valid
        addParentMethods(methods, clazz, types, parent, method);
    }

    /**
     * Recursively add all methods walking the interface hiearchy for the given
     * class.
     * 
     * @param methods  The set of existing methods
     * @param clazz  The current interface class
     * @param types  The set of named type variable
     * @param parent  The optional parent class
     * @param method  The method to implement
     */
    private static void addParentMethods(Set<MethodEntry> methods, 
                                         Class<?> clazz, 
                                         Map<String, Class<?>> types,
                                         Class<?> parent, Method method) {
        // recursively add each super interface providing type info if valid
        Class<?>[] ifaces = clazz.getInterfaces();
        Type[] generics = clazz.getGenericInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            Class<?> iface = ifaces[i];
            Type generic = iface;
            if (generics != null && i < generics.length) {
                generic = generics[i];
            }
            
            // check if interface is parameterized and lookup type information
            // cascading current type information if types match
            if (generic instanceof ParameterizedType) {
                TypeVariable<?>[] vars = iface.getTypeParameters();
                ParameterizedType ptype = (ParameterizedType) generic;
                Type[] arguments = ptype.getActualTypeArguments();
                
                // check each parameter for specific type 
                Map<String, Class<?>> args = new HashMap<String, Class<?>>();
                for (int j = 0; j < vars.length; j++) {
                    Type arg = arguments[j];
                    TypeVariable<?> var = vars[j];

                    // check if root type defined and pass type thru
                    Class<?> root = getRootType(types, arg);
                    if (root != null) {
                        args.put(var.getName(), root);
                    }
                }
                
                // recursively invoke
                if (method == null) {
                    addMethods(methods, (Class<?>) ptype.getRawType(), args, 
                               parent);
                }
                else {
                    addMethods(methods, (Class<?>) ptype.getRawType(), args, 
                               parent, method);
                }
            }
            
            // normal class, so recursively invoke as is
            else if (iface instanceof Class<?>) {
                if (method == null) {
                    addMethods(methods, iface, new HashMap<String, Class<?>>(),
                               parent);
                }
                else {
                    addMethods(methods, iface, new HashMap<String, Class<?>>(),
                               parent, method);
                }
            }
        }
    }
    
    /**
     * Method description containing the method definition and the bridge
     * definition, if valid.
     */
    public static class MethodEntry {
        private String name;
        private Class<?> returnType;
        private Class<?>[] paramTypes;
        private MethodEntry bridgedMethod;
        
        public MethodEntry(String name,
                           Class<?> returnType, Class<?>[] paramTypes,
                           MethodEntry bridgedMethod) {
            this.name = name;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.bridgedMethod = bridgedMethod;
        }
        
        public String getName() { return this.name; }
        public Class<?> getReturnType() { return this.returnType; }
        public Class<?>[] getParamTypes() { return this.paramTypes; }
        public boolean isBridged() { return this.bridgedMethod != null; }
        public MethodEntry getBridgedMethod() { return this.bridgedMethod; }
        
        public boolean equals(Object object) {
            if (object == this) { return true; }
            else if (!(object instanceof MethodEntry)) { return false; }
            
            MethodEntry entry = (MethodEntry) object;
            if (!this.getName().equals(entry.getName()) ||
                !this.getReturnType().equals(entry.getReturnType()) ||
                !Arrays.equals(this.getParamTypes(), entry.getParamTypes())) {
                
                return false; 
            }
            
            return true;
        }
        
        public int hashCode() {
            return 13 + 
                (19 * this.name.hashCode()) +
                (21 * this.returnType.hashCode()) + 
                (23 * Arrays.hashCode(this.paramTypes));
        }
        
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(this.returnType.getName());
            buffer.append(" " + this.name + "(");
            for (int i = 0; i < this.paramTypes.length; i++) {
                if (i > 0) { buffer.append(", "); }
                buffer.append(this.paramTypes[i].getName());
            }
            
            buffer.append(")");
            
            if (this.bridgedMethod != null) {
                buffer.append(" [bridge] ")
                      .append(this.bridgedMethod.toString());
            }
            
            return buffer.toString();
        }
    }
}
