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

package org.teatrove.teaservlet;

import org.teatrove.teatools.*;

import java.beans.*;

/**
 * A Tea Tool's best friend.  This class has several useful methods for writing
 * tools that work with Tea.  Many of these methods were taken from Kettle 
 * 3.0.x so that they could be reused in future versions and in other 
 * applications.
 * <p>
 * This class was written with the intent that it could be used as a tea
 * context class. It provides a collection of functions to make introspection
 * possible from Tea.  
 *
 * @author Mark Masse
 */
public interface TeaToolsContext {

    /**
     * Returns a bean full of handy information about the specified class.
     */
    public HandyClassInfo getHandyClassInfo(Class<?> clazz);

    /**
     * Returns a bean full of handy information about the specified class.
     */
    public HandyClassInfo getHandyClassInfo(String className);

    /**
     * Returns the first sentence of the specified paragraph.  Uses
     * <code>java.text.BreakIterator.getSentenceInstance()</code>
     */
    public String getFirstSentence(String paragraph);

    /**
     * Creates a String with the specified pattern repeated length
     * times.
     */
    public String createPatternString(String pattern, int length);

    /**
     * Creates a String of spaces with the specified length.
     */
    public String createWhitespaceString(int length);

    /**
     * provides a bean to contain an assortment of methods to handle class 
     * names and properties.
     */
    interface HandyClassInfo {    
    
         /**
          * Returns the class name of the specified class.  This method 
          * provides special formatting for array and inner classes.
          */
         public String getFullName();
        
        
        /**
         * Returns the class name of the specified class.  The class name returned
         * does not include the package. This method provides special formatting 
         * for array and inner classes.
         */
        public String getName();

        /**
         * Returns the package name of the specified class.  Returns null if the
         * class has no package.
         */
        public String getPackage();

        /**
         * Returns the type.
         */
        public Class<?> getType();
        
        /**
         * Returns the name of the type of the Class described by this 
         * TypeDescription.
         * <p>
         * <UL>
         * <LI>A Class returns "class"
         * <LI>An Interface returns "interface"
         * <LI>An array returns null
         * <LI>A primitive returns null
         * </UL>
         */
        public String getTypeName();

        /**
         * Returns whether the given class or any of its super classe or interfaces,
         * recursively, are deprecated.
         * 
         * @return <code>true</code> if the type is deprecated
         * 
         * @see Deprecated
         */
        public boolean isDeprecated();
        
        /**
         * Returns the array type.  Returns the specified class if it is 
         * not an array.  
         */
        public TypeDescription getArrayType();

        /**
         * Returns the array dimensions.  
         * Returns 0 if the specified class is not an array.  
         */
        public int getArrayDimensions();

        /**
         * Returns the array dimensions String (i.e. "[][][]").  
         * Returns "" (empty string) if the specified class is not an array.
         */
        public String getArrayDimensionsString();

        /**
         * Returns the shortDescription or "" if the
         * shortDescription is the same as the displayName.
         */
        public String getDescription();
        
        /**
         * Returns the first sentence of the 
         * shortDescription.  Returns "" if the shortDescription is the same as
         * the displayName (the default for reflection-generated 
         * FeatureDescriptors).  
         */
        public String getDescriptionFirstSentence();
        
        /**
         * Create a version information string based on what the build process
         * provided.  The string is of the form "M.m.r" or 
         * "M.m.r.bbbb" (i.e. 1.1.0.0004) if the build number can be retrieved.
         * Returns <code>null</code> if the version string cannot be retrieved.
         */
        public String getVersion();

        /**
         * Introspects a Java bean to learn about all its properties, 
         * exposed methods, and events.
         *
         * @param beanClass the bean class to be analyzed
         */
        public BeanInfo getBeanInfo();
        
        /**
         * Returns the type's MethodDescriptions.
         */
        public MethodDescription[] getMethodDescriptions();
        
        /**
         * Returns the type's PropertyDescriptions.
         */
        public PropertyDescription[] getPropertyDescriptions();
        
        /**
         * A function that returns an array of all the available properties on
         * a given class.
         * <p>
         * <b>NOTE:</b> If possible, the results of this method should be cached
         * by the caller.
         *
         * @param beanClass the bean class to introspect
         *
         * @return an array of all the available properties on the specified class.
         */
        public PropertyDescriptor[] getTeaBeanPropertyDescriptors();
        
        /**
         * A function that returns an array of all the available properties on
         * a given class.
         * <p>
         * <b>NOTE:</b> If possible, the results of this method should be cached
         * by the caller.
         *
         * @param beanClass the bean class to introspect
         *
         * @return an array of all the available properties on the specified class.
         */
        public PropertyDescription[] getTeaBeanPropertyDescriptions();
        
        /**
         * Gets the MethodDescriptors of the specified context class including
         * all of the MethodDescriptors for methods declared in the class's 
         * superclass and interfaces
         *
         * @param contextClass the Tea context Class to introspect (any class will
         * work fine)
         */
        public MethodDescriptor[] getTeaContextMethodDescriptors();

        /**
         * Gets the MethodDescriptions of the specified context class including
         * all of the MethodDescriptors for methods declared in the class's 
         * superclass and interfaces
         *
         * @param contextClass the Tea context Class to introspect (any class will
         * work fine)
         */
        public MethodDescription[] getTeaContextMethodDescriptions();
    }
}
