package org.teatrove.trove.util;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * Helper methods for dealing with classes and methods.
 */
public final class ClassUtils {

    /**
     * Private constructor to preserve static nature of class.
     */
    private ClassUtils() {
        super();
    }
    
    /**
     * Check whether the given class is deprecated or not.  This will check the
     * given class, any parent classes, and any parent interfaces for any
     * class that contains the {@link Deprecated} annotations.
     * 
     * @param clazz The class to inspect
     * 
     * @return true if the class is deprecated, false otherwise
     */
    public static boolean isDeprecated(Class<?> clazz) {
        return isDeprecated(clazz, true);
    }
    
    /**
     * Check whether the given class is deprecated or not.  If the recursive
     * parameter is <code>true</code>, then this will check the given class, 
     * any parent classes, and any parent interfaces for any class that contains 
     * the {@link Deprecated} annotations.  Otherwise, it will only check the
     * immediate class.
     * 
     * @param clazz The class to inspect
     * @param recursive  The state of whether to recursively inspect
     * 
     * @return true if the class is deprecated, false otherwise
     */
    public static boolean isDeprecated(Class<?> clazz, boolean recursive) {
        
        // check if class is marked as deprecated
        if (clazz.getAnnotation(Deprecated.class) != null) { 
            return true; 
        }
        
        // check hiearchy if recursive
        if (recursive) {
            
            // check if super class is deprecated
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return isDeprecated(superClass, true);
            }
            
            // check if interfaces are deprecated
            for (Class<?> iface : clazz.getInterfaces()) {
                return isDeprecated(iface, true);
            }
        }
        
        // not deprecated
        return false;
    }
    
    /**
     * Check whether the given method is deprecated.  This will also recursively 
     * inspect all parent classes and parent interfaces for any other class in 
     * the hiearchy that declares the method.  It will also check if the class
     * itself is deprecated.
     * 
     * @param method  The method to inspect
     * 
     * @return true if the method is deprecated, false otherwise
     */
    public static boolean isDeprecated(Method method) {
        return isDeprecated(method.getDeclaringClass(), method.getName(), 
                            method.getParameterTypes());
    }
    
    /**
     * Check whether the given method in the given class is deprecated.  This
     * will also recursively inspect all parent classes and parent interfaces
     * for any other class in the hiearchy that declares the method.
     * 
     * @param clazz  The class containing the method
     * @param methodName  The name of the method to inspect
     * @param paramTypes  The type of parameters to the method
     * 
     * @return true if the method is deprecated, false otherwise
     */
    public static boolean isDeprecated(Class<?> clazz, String methodName, 
                                       Class<?>... paramTypes) {
       
       // ignore dates as they are substantial still
       if (Date.class.isAssignableFrom(clazz)) {
           return false;
       }

       // check if class is deprecated
       if (isDeprecated(clazz)) {
           return true;
       }
       
       // check if method is annotated
       try {
           Method method = 
               clazz.getDeclaredMethod(methodName, paramTypes);
           
           if (method.getAnnotation(Deprecated.class) != null) {
               return true;
           }
       }
       catch (NoSuchMethodException nsme) {
           // ignore and continue
       }
       
       // check superclass
       Class<?> superClass = clazz.getSuperclass();
       if (superClass != null) {
           return isDeprecated(superClass, methodName, paramTypes);
       }
       
       // check interfaces
       for (Class<?> iface : clazz.getInterfaces()) {
           return isDeprecated(iface, methodName, paramTypes);
       }
       
       // none found
       return false;
   }
}
