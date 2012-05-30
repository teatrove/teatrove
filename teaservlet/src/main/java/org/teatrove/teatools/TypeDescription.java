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

package org.teatrove.teatools;

import org.teatrove.trove.classfile.Modifiers;

import java.beans.*;

/**
 * Object wrapper for TeaToolsUtils functions that operate on Class objects.
 * This class offers an O-O interface alternative to the TeaToolsUtils 
 * procedural design.
 *
 * @author Mark Masse
 */
public class TypeDescription extends FeatureDescription {
    
    /** The type to wrap and describe */
    private Class<?> mType;
    
    /** The BeanInfo for the type */
    private BeanInfo mBeanInfo;

    /**
     * Create a new TypeDescription to wrap the specified type. 
     * The TeaToolsUtils object provides the implementation of 
     * this class's methods.
     */
    public TypeDescription(Class<?> type, TeaToolsUtils utils) {
        super(utils);
        mType = type;
    }

    /**
     * Returns the type.
     */
    public Class<?> getType() {
        return mType;
    }

    /**
     * Returns whether the given class or any of its super classe or interfaces,
     * recursively, are deprecated.
     * 
     * @return <code>true</code> if the type is deprecated
     * 
     * @see Deprecated
     */
    public boolean isDeprecated() {
        return getTeaToolsUtils().isDeprecated(mType);
    }

    /**
     * Returns a Modifiers instance that can be used to check the type's
     * modifiers.
     */
    public Modifiers getModifiers() {
        return getTeaToolsUtils().getModifiers(mType.getModifiers());
    }

    /**
     * Returns the full name of the type.  This method 
     * provides special formatting for array and inner classes.
     */
    public String getFullName() {
        return getTeaToolsUtils().getFullClassName(mType);
    }

    /**
     * Returns the class name of the type.  The class name returned
     * does not include the package. This method provides special formatting 
     * for array and inner classes.
     */
    public String getName() {
        return getTeaToolsUtils().getClassName(mType);
    }

    /**
     * Returns the package name of the class.  Returns "" if the
     * class has no package.
     */
    public String getPackage() {
        return getTeaToolsUtils().getClassPackage(mType);
    }

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
    public String getTypeName() {        
        return getTeaToolsUtils().getClassTypeName(mType);
    }

    /**
     * Create a version information string based on what the build process
     * provided.  The string is of the form "M.m.r" or 
     * "M.m.r.bbbb" (i.e. 1.1.0.0004) if the build number can be retrieved.
     * Returns <code>null</code> if the version string cannot be retrieved.
     */
    public String getVersion() { 
        return getTeaToolsUtils().getPackageVersion(getPackage());             
    }
    
    /**
     * Returns the array type.  Returns this if it is not an 
     * array type.  
     */
    public TypeDescription getArrayType() {

        Class<?> c = getTeaToolsUtils().getArrayType(mType);      
        if (mType == c) {
            return this;
        }

        return getTeaToolsUtils().createTypeDescription(c);
    }


    /**
     * Returns the array dimensions.  
     * Returns 0 if the type is not an array.  
     */
    public int getArrayDimensions() {
        return getTeaToolsUtils().getArrayDimensions(mType);
    }

    /**
     * Returns the array dimensions String (i.e. "[][][]").  
     * Returns "" (empty string) if the type is not an array.  
     */
    public String getArrayDimensionsString() {
        return getTeaToolsUtils().getArrayDimensionsString(mType);
    }

    /**
     * Introspects a Java bean to learn about all its properties, 
     * exposed methods, and events.  Returns null if the BeanInfo 
     * could not be created.
     */
    public BeanInfo getBeanInfo() {
        if (mBeanInfo == null) {
            try { 
                mBeanInfo = getTeaToolsUtils().getBeanInfo(mType);
            }
            catch (Exception e) {
                return null;
            }
        }

        return mBeanInfo;
    }

    /**
     * Introspects a Java bean to learn all about its properties, exposed 
     * methods, below a given "stop" point.
     *
     * @param stopClass the base class at which to stop the analysis. 
     * Any methods/properties/events in the stopClass or in its baseclasses 
     * will be ignored in the analysis
     */
    public BeanInfo getBeanInfo(Class<?> stopClass) 
    throws IntrospectionException {
        return getTeaToolsUtils().getBeanInfo(mType, stopClass);
    }


    /**
     * Returns the type's PropertyDescriptors.
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        BeanInfo info = getBeanInfo();
        if (info == null) {
            return null;
        }

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        getTeaToolsUtils().sortPropertyDescriptors(pds);
        return pds;
    }

    /**
     * Returns the type's PropertyDescriptions.
     */
    public PropertyDescription[] getPropertyDescriptions() {
        return getTeaToolsUtils().createPropertyDescriptions(
                                          getPropertyDescriptors());

    }

    /**
     * Returns an array of all the available properties on the class.
     */
    public PropertyDescriptor[] getTeaBeanPropertyDescriptors() {
        return getTeaToolsUtils().getTeaBeanPropertyDescriptors(mType);
    }

    /**
     * Returns an array of all the available properties on the class.
     */
    public PropertyDescription[] getTeaBeanPropertyDescriptions() {
        return getTeaToolsUtils().createPropertyDescriptions(
                                          getTeaBeanPropertyDescriptors());
    }

    /**
     * Returns the type's MethodDescriptors.
     */
    public MethodDescriptor[] getMethodDescriptors() {
        BeanInfo info = getBeanInfo();
        if (info == null) {
            return null;
        }

        MethodDescriptor[] mds = info.getMethodDescriptors();      
        getTeaToolsUtils().sortMethodDescriptors(mds);
        return mds;
    }

    /**
     * Returns the type's MethodDescriptions.
     */
    public MethodDescription[] getMethodDescriptions() {
        return getTeaToolsUtils().createMethodDescriptions(
                                                     getMethodDescriptors());
    }


    /**
     * Gets the MethodDescriptors of the context class including
     * all of the MethodDescriptors for methods declared in the class's 
     * superclass and interfaces
     */
    public MethodDescriptor[] getTeaContextMethodDescriptors() {
        return getTeaToolsUtils().getTeaContextMethodDescriptors(mType);
    }


    /**
     * Gets the MethodDescriptions of the context class including
     * all of the MethodDescriptions for methods declared in the class's 
     * superclass and interfaces
     */
    public MethodDescription[] getTeaContextMethodDescriptions() {
        return getTeaToolsUtils().createMethodDescriptions(
                                          getTeaContextMethodDescriptors());
    }


    /**
     * Gets the MethodDescriptors of the context class
     *
     * @param thisClassOnly true indicates that this function should 
     * only return MethodDescriptors declared by the wrapped Class.  
     */
    public MethodDescriptor[] getTeaContextMethodDescriptors(
                                                 boolean thisClassOnly) {

        return getTeaToolsUtils().getTeaContextMethodDescriptors(
                                                              mType, 
                                                              thisClassOnly);
    }
    
    /**
     * Returns the full class name of the class.  This method 
     * provides special formatting for array and inner classes.  If the 
     * specified class is implicitly imported by Tea, then its package is
     * omitted in the returned name.
     */
    public String getTeaFullName() {
        return getTeaToolsUtils().getTeaFullClassName(mType);
    }

    /**
     * Returns true if the class is 
     * implicitly imported by Tea.
     * <p>
     * Returns true if the specified class represents a primitive type or
     * a class or interface defined in one of the IMPLICIT_TEA_IMPORTS 
     * packages.  This method also works for array types.
     */
    public boolean isImplicitTeaImport() {
        return getTeaToolsUtils().isImplicitTeaImport(mType);
    }

    /**
     * Returns true if the class is compatible with Tea's 
     * <code>foreach</code> statement.  Compatibility implies that the
     * class can be iterated on by the <code>foreach</code>.
     */
    public boolean isForeachCompatible() {
        return getTeaToolsUtils().isForeachCompatible(mType);
    }

    /**
     * Returns true if the class is compatible with Tea's <code>if
     * </code> statement.  Only Boolean.class and boolean.class qualify.
     */    
    public boolean isIfCompatible() {    
        return getTeaToolsUtils().isIfCompatible(mType);
    }

    /**
     * Returns true if it is likely that the class serves as 
     * a Tea runtime context class.
     */
    public boolean isLikelyContextClass() {     
        return getTeaToolsUtils().isLikelyContextClass(mType);
    }


    //
    // FeatureDescription methods
    //

    public FeatureDescriptor getFeatureDescriptor() {
        BeanInfo info = getBeanInfo();
        if (info == null) {
            return null;
        }

        return info.getBeanDescriptor();
    }


    public String getShortFormat() {
        return getName();
    }

    public String getLongFormat() {
        return getFullName();
    }



}




