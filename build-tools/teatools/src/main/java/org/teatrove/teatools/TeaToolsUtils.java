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

import org.teatrove.tea.runtime.*;
import org.teatrove.tea.util.*;

import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.generics.GenericType;
import org.teatrove.trove.util.*;


import java.beans.*;

import java.lang.reflect.*;
import java.util.*;
import java.text.BreakIterator;

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
 * @author Mark Masse, Sean Treat
 */
public class TeaToolsUtils extends DefaultContext 
implements TeaToolsConstants {


    /** Zero-length array of properties */
    private final static PropertyDescriptor[] NO_PROPERTIES = 
        new PropertyDescriptor[0];
    
    /** Finite set of primative Class objects.  Mapping of primitive
        name to Class (i.e. "int" -> int.class) */
    private final static Hashtable<String, Class<?>> cPrimativeClasses = 
        new Hashtable<String, Class<?>>();

    /** The DescriptorComparator used to sort FeatureDescriptors */
    private final static DescriptorComparator DESCRIPTOR_COMPARATOR = 
        new DescriptorComparator();       

    static {
        
        Class<?>[] primativeClasses = 
            new Class[] {
                void.class,
                boolean.class,
                char.class,
                byte.class,
                short.class,
                int.class,
                float.class,
                double.class,
                long.class              
            };
        
        for (int i = 0; i < primativeClasses.length; i++) {
            cPrimativeClasses.put(primativeClasses[i].getName(),
                                  primativeClasses[i]);
        }
    }

    /**
     * Creates a new TeaToolsUtils
     */
    public TeaToolsUtils() {
    }

    // DefaultContext method; implemented to do nothing
    public void print(Object obj) throws Exception {
    }

    /**
     * Returns a TypeDescription object to wrap and describe the
     * specified type.
     */
    public TypeDescription createTypeDescription(Class<?> type) {
        return new TypeDescription(type, this);
    }

    /**
     * Returns an array of PropertyDescriptions to wrap and describe the
     * specified PropertyDescriptors.
     */
    public PropertyDescription[] createPropertyDescriptions(
                                                   PropertyDescriptor[] pds) {

        if (pds == null) {
            return null;
        }

        PropertyDescription[] descriptions = 
            new PropertyDescription[pds.length];

        for (int i = 0; i < pds.length; i++) {
            descriptions[i] = new PropertyDescription(pds[i], this);
        }

        return descriptions;
    }

    /**
     * Returns an array of MethodDescriptions to wrap and describe the
     * specified MethodDescriptors.
     */
    public MethodDescription[] createMethodDescriptions(
                                                     MethodDescriptor[] mds) {
        if (mds == null) {
            return null;
        }

        MethodDescription[] descriptions = 
            new MethodDescription[mds.length];

        for (int i = 0; i < mds.length; i++) {
            descriptions[i] = new MethodDescription(mds[i], this);
        }

        return descriptions;
    }

    /**
     * Returns an array of ParameterDescriptions to wrap and describe the
     * parameters of the specified MethodDescriptor.
     */
    public ParameterDescription[] createParameterDescriptions(
                                                     MethodDescriptor md) {
        if (md == null) {
            return null;
        }

        Method method = md.getMethod();
        Class<?>[] paramClasses = method.getParameterTypes();        
        int descriptionCount = paramClasses.length;
        if (acceptsSubstitution(md)) {
            descriptionCount--;
        }

        ParameterDescriptor[] pds = md.getParameterDescriptors();
        ParameterDescription[] descriptions = 
            new ParameterDescription[descriptionCount];

        for (int i = 0; i < descriptions.length; i++) {
                        
            TypeDescription type = createTypeDescription(paramClasses[i]);
            ParameterDescriptor pd = null;
            if (pds != null && i < pds.length) {
                pd = pds[i];
            }            

            descriptions[i] = new ParameterDescription(type, pd, this);
        }
        
        return descriptions;
    }


    /**
     * Creates a PackageDescriptor for the named package.
     */
    public PackageDescriptor createPackageDescriptor(String packageName) {
        return PackageDescriptor.forName(packageName);
    }

    /**
     * Creates a PackageDescriptor for the named package using the 
     * specified ClassLoader to load the PackageInfo or Package.
     */
    public PackageDescriptor createPackageDescriptor(String packageName,
                                                     ClassLoader classLoader) {
        return PackageDescriptor.forName(packageName, classLoader);
    }

    /**
     * Returns a Modifiers instance that can be used to check the modifier
     * int returned by the Class.getModifiers or Member.getModifiers
     * method.
     */
    public Modifiers getModifiers(int modifier) {
        return new Modifiers(modifier);
    }

    /**
     * Returns a Class object for a given name. Primitive classes can be 
     * loaded via thier normal names (i.e. "float"). Array classes can be
     * loaded using either the normal Java name (i.e. int[][]) or the VM
     * name (i.e. [[I).  
     * <p>
     * Note this method swallows all exceptions and simply returns null if 
     * the class could not be loaded.
     *
     * @param className the name of the Class
     */
    public Class<?> getClassForName(String className) {
        return getClassForName(className, null);
    }

    /**
     * Returns a Class object for a given name. Primitive classes can be 
     * loaded via thier normal names (i.e. "float"). Array classes can be
     * loaded using either the normal Java name (i.e. int[][]) or the VM
     * name (i.e. [[I).  
     * <p>
     * Note this method swallows all exceptions and simply returns null if 
     * the class could not be loaded.
     *
     * @param className the name of the Class
     * @param classLoader the ClassLoader to use
     */
    public Class<?> getClassForName(String className, ClassLoader classLoader) {

        if (className == null) {
            return null;
        }

        int bracketIndex = className.indexOf('[');
        if (bracketIndex > 0) {
            // Convert foo[][] to [[Lfoo;
            className = convertArrayClassName(className, bracketIndex);
        }

        if (className == null) {
            // Array class name could not be converted (should only happen
            // for void[]
            return null;
        }
        
        Class<?> clazz = cPrimativeClasses.get(className);
        if (clazz != null) {
            // Return the primitive class (i.e. int.class for "int")
            return clazz;
        }

        Throwable error = null;
        try {
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }

            if (classLoader != null) {
                clazz = classLoader.loadClass(className);
            }
        }
        catch (Throwable t) {
            error = t;
        }

        if (clazz == null) {
            // Try Class.forName
            try {
                clazz = Class.forName(className);
            }           
            catch (Throwable t) {
                if (error == null) {
                    //t.printStackTrace();
                }
                else {
                    //error.printStackTrace();
                }
            }
        }

        return clazz;
    }
    
    /**
     * Returns whether the given class or any super class or interface is
     * deprecated.
     * 
     * @see Deprecated
     */
    public boolean isDeprecated(Class<?> clazz) {

        // check if class is marked as deprecated
        if (clazz.getAnnotation(Deprecated.class) != null) { 
            return true; 
        }
        
        // check if super class is deprecated
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return isDeprecated(superClass);
        }
        
        // check if interfaces are deprecated
        for (Class<?> iface : clazz.getInterfaces()) {
            return isDeprecated(iface);
        }
        
        // not deprecated
        return false;
    }
    
    /**
     * Returns whether the given method or any super method or interface
     * declaration is deprecated.
     * 
     * @see Deprecated
     */
    public boolean isDeprecated(Method method) {
        return isDeprecated(method.getDeclaringClass(), method.getName(), 
                            method.getParameterTypes());
    }
   
    /**
     * Returns whether the given method or any super method or interface
     * declaration is deprecated.
     * 
     * @see Deprecated
     */
    protected boolean isDeprecated(Class<?> clazz, 
        String methodName, Class<?>... paramTypes) {
        
        // check if type is annotated
        if (isDeprecated(clazz)) { return true; }
        
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
    
    /**
     * Returns the full class name of the specified class.  This method 
     * provides special formatting for array and inner classes.
     */
    public String getFullClassName(Class<?> clazz) {
        return getFullClassName(getArrayClassName(clazz));
    }

    /**
     * Returns the full class name of the specified class.  This method 
     * provides special formatting for inner classes.
     */
    public String getFullClassName(String fullClassName) {

        String[] parts = parseClassName(fullClassName);             

        String className = getInnerClassName(parts[1]);
        
        if (parts[0] != null) {
            // Return the package name plus the converted class name
            return parts[0] + '.' + className;
        }
        else {
            return className;
        }
    }

    /**
     * Returns the class name of the specified class.  The class name returned
     * does not include the package. This method provides special formatting 
     * for array and inner classes.
     */
    public String getClassName(Class<?> clazz) {
        return getClassName(getArrayClassName(clazz));
    }

    /**
     * Returns the class name of the specified class.  The class name returned
     * does not include the package. This method provides special formatting 
     * for inner classes.
     */
    public String getClassName(String fullClassName) {
        return getInnerClassName(parseClassName(fullClassName)[1]);
    }

    /**
     * Returns the package name of the specified class.  Returns "" if the
     * class has no package.
     */
    public String getClassPackage(Class<?> clazz) {
        return getClassPackage(getArrayClassName(clazz));
    }

    /**
     * Returns the package name of the specified class.  Returns null if the
     * class has no package.
     */
    public String getClassPackage(String fullClassName) {        
        return parseClassName(fullClassName)[0];
    }

    /**
     * Returns the type of the specified class.
     * <p>
     * <UL>
     * <LI>A Class returns "class"
     * <LI>An Interface returns "interface"
     * <LI>An array returns null
     * <LI>A primitive returns null
     * </UL>
     */
    public String getClassTypeName(Class<?> clazz) {
        
        String type = null;

        if (clazz.isInterface()) {
            type = "interface";
        }
        else if (clazz.isPrimitive() || clazz.isArray()) {
            // type already null
        }
        else {
            type = "class";
        }

        return type;
    }

    /**
     * Create a version information string based on what the build process
     * provided.  The string is of the form "M.m.r" or 
     * "M.m.r.bbbb" (i.e. 1.1.0.0004) if the build number can be retrieved.
     * Returns <code>null</code> if the version string cannot be retrieved.
     */
    public String getPackageVersion(String packageName) { 

        if (packageName == null || packageName.trim().length() == 0) {
            return null;
        }
        
        if (!packageName.endsWith(".")) {
            packageName = packageName + ".";
        }
        
        String className = packageName + "PackageInfo";

        Class<?> packageInfoClass = getClassForName(className);
        if (packageInfoClass == null) {
            return null;
        }

        String productVersion = null;
        String buildNumber = null;

        try {

            Method getProductVersionMethod = 
                packageInfoClass.getMethod("getProductVersion", 
                                           EMPTY_CLASS_ARRAY);

            productVersion = 
                (String) getProductVersionMethod.invoke(null, 
                                                        EMPTY_OBJECT_ARRAY);
            
            Method getBuildNumberMethod = 
                packageInfoClass.getMethod("getBuildNumber", 
                                           EMPTY_CLASS_ARRAY);

            buildNumber = 
                (String) getBuildNumberMethod.invoke(null, 
                                                     EMPTY_OBJECT_ARRAY);

        }
        catch (Throwable t) { 
            // Just eat it 
        }

        if (productVersion != null && productVersion.length() > 0 &&
            buildNumber != null && buildNumber.length() > 0) {

            productVersion += '.' + buildNumber;
        }

        return productVersion;
    }


    /**
     * Splits a class name into two strings.  
     * <br>
     * [0] = package name (or null if the class is unpackaged) <br>
     * [1] = class name
     */
    public String[] parseClassName(String fullClassName) {

        int dotIndex = fullClassName.lastIndexOf(".");
        String packageName = null;
        String className = fullClassName;

        if (dotIndex > 0) {
            packageName = fullClassName.substring(0, dotIndex);
            className = fullClassName.substring(dotIndex + 1);
        }

        return new String[] { packageName, className };
    }

    /**
     * Formats the class name with trailing square brackets.
     */
    public String getArrayClassName(Class<?> clazz) {

        if (clazz.isArray()) {
            return getArrayClassName(clazz.getComponentType()) + "[]";
        }

        return clazz.getName();
    }

    /**
     * Returns the array type.  Returns the specified class if it is not an 
     * array.  
     */
    public Class<?> getArrayType(Class<?> clazz) {

        if (clazz.isArray()) {
            return getArrayType(clazz.getComponentType());
        }

        return clazz;
    }

    /**
     * Returns the array dimensions.  
     * Returns 0 if the specified class is not an array.  
     */
    public int getArrayDimensions(Class<?> clazz) {

        if (clazz.isArray()) {
            return getArrayDimensions(clazz.getComponentType()) + 1;
        }
        
        return 0;
    }

    /**
     * Returns the array dimensions String (i.e. "[][][]").  
     * Returns "" (empty string) if the specified class is not an array.  
     */
    public String getArrayDimensionsString(Class<?> clazz) {
        return createPatternString("[]", getArrayDimensions(clazz));
    }

    /**
     * Converts the user-friendly array class name to the VM friendly one.
     * For example:
     * <UL>
     * <LI><code>java.lang.String[]</code> becomes 
     * <code>[Ljava.lang.String;</code>
     * <LI><code>int[][]</code> becomes <code>[[I</code>
     * <LI><code>byte[]</code> becomes <code>[B</code>
     * </UL>
     *
     * @param className the name of the array class
     * @param bracketIndex the index (withing className) of the first '[' 
     * character
     */
    public String convertArrayClassName(String className, 
                                        int bracketIndex) {

        String dimensions = "";

        String brackets = className.substring(bracketIndex).trim();
        char[] chars = brackets.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '[') {
                dimensions = dimensions + '[';
            }
        }

        String arrayType = className.substring(0, bracketIndex).trim();
        char prefixChar = 'L';

        Class<?> clazz = cPrimativeClasses.get(arrayType);
        if (clazz != null) {

            arrayType = "";

            if (clazz == boolean.class) {
                prefixChar = 'Z';
            }        
            else if (clazz == char.class) {
                prefixChar = 'C';
            }        
            else if (clazz == byte.class) {
                prefixChar = 'B';
            }        
            else if (clazz == short.class) {
                prefixChar = 'S';
            }        
            else if (clazz == int.class) {
                prefixChar = 'I';
            }        
            else if (clazz == float.class) {
                prefixChar = 'F';
            }        
            else if (clazz == double.class) {
                prefixChar = 'D';
            }        
            else if (clazz == long.class) {
                prefixChar = 'J';
            }        
            else if (clazz == void.class) {
                return null;
            }        
        }
        else {
            arrayType = arrayType + ';';
        }

        return dimensions + prefixChar + arrayType;        
    }

    /**
     * Returns the className with '$'s changed to '.'s
     */
    public String getInnerClassName(String className) {        
        return className.replace('$', '.');     
    }


    /**
     * Introspects a Java bean to learn about all its properties, 
     * exposed methods, and events.
     *
     * @param beanClass the bean class to be analyzed
     */
    public BeanInfo getBeanInfo(Class<?> beanClass) 
    throws IntrospectionException {
        return Introspector.getBeanInfo(beanClass);
    }

    /**
     * Introspects a Java bean to learn all about its properties, exposed 
     * methods, below a given "stop" point.
     *
     * @param beanClass the bean class to be analyzed
     * @param stopClass the base class at which to stop the analysis. 
     * Any methods/properties/events in the stopClass or in its baseclasses 
     * will be ignored in the analysis
     */
    public BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass)
    throws IntrospectionException {
        return Introspector.getBeanInfo(beanClass, stopClass);
    }

    /**
     * Retrieves the value of the named FeatureDescriptor attribute.
     *
     * @return The value of the attribute. May be null if the attribute
     * is unknown.
     */
    public Object getAttributeValue(FeatureDescriptor feature,
                                    String attributeName) {

        if (feature == null || attributeName == null) {
            return null;
        }

        return feature.getValue(attributeName);
    }

    /**
     * Converts a string to normal Java variable name capitalization. 
     * This normally means converting the first character from upper case 
     * to lower case, but in the (unusual) special case when there is more
     * than one character and both the first and second characters are upper
     * case, we leave it alone. 
     * <p>
     * Thus "FooBar" becomes "fooBar" and "X" becomes "x", but "URL" stays 
     * as "URL".
     *
     * @param name the string to be decapitalized
     *
     * @return the decapitalized version of the string
     */
    public String decapitalize(String name) {

        if (name == null) {
            return "";
        }

        return Introspector.decapitalize(name);
    }


    /**
     * Returns the first sentence of the specified paragraph.  Uses
     * <code>java.text.BreakIterator.getSentenceInstance()</code>
     */
    public String getFirstSentence(String paragraph) {

        if (paragraph == null || paragraph.length() == 0) {
            return "";
        }

        BreakIterator sentenceBreaks = BreakIterator.getSentenceInstance();
        sentenceBreaks.setText(paragraph);
        int start = sentenceBreaks.first();
        int end = sentenceBreaks.next();

        String sentence = paragraph;
        
        if (start >= 0 && end <= paragraph.length()) {
            sentence = paragraph.substring(start, end);
        }

        return sentence.trim();
    }


    /**
     * Creates a String with the specified pattern repeated length
     * times.
     */
    public String createPatternString(String pattern, int length) {
        if (pattern == null) {
            return null;
        }

        int totalLength = pattern.length() * length;
        StringBuffer sb = new StringBuffer(totalLength);
        for (int i = 0; i < length; i++) {
            sb.append(pattern);
        }
        return sb.toString();
    }

    /**
     * Creates a String of spaces with the specified length.
     */
    public String createWhitespaceString(int length) {
        return createPatternString(" ", length);
    }

    /**
     * Returns the FeatureDescriptor's shortDescription or "" if the
     * shortDescription is the same as the displayName.
     */
    public String getDescription(FeatureDescriptor feature) {
        String description = feature.getShortDescription();
        
        if (description == null || 
            description.equals(feature.getDisplayName())) {
            
            description = "";
        }

        return description;
    }

    /**
     * Returns the first sentence of the FeatureDescriptor's 
     * shortDescription.  Returns "" if the shortDescription is the same as
     * the displayName (the default for reflection-generated 
     * FeatureDescriptors).  
     */
    public String getDescriptionFirstSentence(FeatureDescriptor feature) {
        return getFirstSentence(getDescription(feature));
    }   

    /**
     * Sorts an array of FeatureDescriptors based on the method name and 
     * if these descriptors are MethodDescriptors, by param count as well.
     * To prevent damage to the original array, a clone is made, sorted,
     * and returned from this method.
     */
    public FeatureDescriptor[] sortDescriptors(FeatureDescriptor[] fds) {
        /*
         * the variable dolly is used here in reference to the sheep cloned
         * a few years back by Scottish scientists. - Jonathan
         */
        FeatureDescriptor[] dolly = fds.clone();
        Arrays.sort(dolly, DESCRIPTOR_COMPARATOR);
        return dolly;
    }
    
    /**
     * Sorts an array of MethodDescriptors based on the method name and 
     * param count.
     */
    public void sortMethodDescriptors(MethodDescriptor[] mds) {
        Arrays.sort(mds, DESCRIPTOR_COMPARATOR);
    }

    /**
     * Sorts an array of PropertyDescriptors based on name.
     */
    public void sortPropertyDescriptors(PropertyDescriptor[] pds) {
        Arrays.sort(pds, DESCRIPTOR_COMPARATOR);
    }


    //
    // Tea-specific utilities
    //


    /**
     * Merges several classes together, producing a new class that has all 
     * of the methods of the combined classes.  All methods in the combined 
     * class delegate to instances of the source classes. If multiple classes
     * implement the same method, the first one provided is used. 
     * The merged class implements all of the interfaces provided by the 
     * source classes or interfaces.
     * <p>
     * This method uses org.teatrove.trove.util.MergedClass
     */
    public Class<?> createContextClass(ClassLoader loader, 
                                       ContextClassEntry[] contextClasses) 
        throws Exception {

        Object[] ret = loadContextClasses(loader, contextClasses);
        Class<?>[] classes = (Class[]) ret[0];
        String[] prefixNames = (String[]) ret[1];

        return createContextClass(loader, classes, prefixNames);
    }

    /**
     * Merges several classes together, producing a new class that has all 
     * of the methods of the combined classes.  All methods in the combined 
     * class delegate to instances of the source classes. If multiple classes
     * implement the same method, the first one provided is used. 
     * The merged class implements all of the interfaces provided by the 
     * source classes or interfaces.
     * <p>
     * This method uses org.teatrove.trove.util.MergedClass
     */
    public Class<?> createContextClass(ClassLoader loader,
                                       Class<?>[] classes,
                                       String[] prefixNames) 
    throws Exception {
                           

        ClassInjector classInjector = 
            new ClassInjector(loader, (java.io.File) null, null);
        
        Constructor<?> constructor = null;
        
        constructor = MergedClass.getConstructor(classInjector, 
                                                 classes, 
                                                 prefixNames);

        Class<?> mergedContextClass = constructor.getDeclaringClass();

        return mergedContextClass;                        
    }    

    /**
     * Loads and returns the runtime context classes specified by the 
     * ContextClassEntry array.
     *
     * @return a 2 element Object array:  <br>
     * <pre> 
     *     index [0] is the Class[] containing the context classes.
     *     index [1] is the String[] containing the context prefix names.
     */
    public Object[] loadContextClasses(ClassLoader loader,
                                       ContextClassEntry[] contextClasses) 
    throws Exception {

        if (loader == null) {
            throw new IllegalArgumentException("ClassLoader is null");
        }

        if (contextClasses == null || contextClasses.length == 0) {
            throw new IllegalArgumentException("No Context Classes");
        }
                        
        Vector<Class<?>> classVector = 
            new Vector<Class<?>>(contextClasses.length);
        String[] prefixNames = new String[contextClasses.length];
        
        String className = null;
        for (int i = 0; i < contextClasses.length; i++) {
            className = contextClasses[i].getContextClassName();    
            Class<?> contextClass = loader.loadClass(className);
            
            classVector.addElement(contextClass);
            String prefixName = contextClasses[i].getPrefixName();
            if (prefixName != null) { 
                prefixNames[i] = prefixName + "$";
            }
            else {
                prefixNames[i] = null;
            }                                
        }
        
        Class<?>[] classes = new Class<?>[classVector.size()];
        classVector.copyInto(classes);
        
        return new Object[] {classes, prefixNames};
    }


    /**
     * Returns true if it is likely that the specified class serves as 
     * a Tea runtime context class.
     */
    public boolean isLikelyContextClass(Class<?> clazz) {     
        return (OutputReceiver.class.isAssignableFrom(clazz) ||
                getClassName(clazz).toLowerCase().endsWith("context"));
    }

   
    /**
     * Gets the MethodDescriptors of the specified context class including
     * all of the MethodDescriptors for methods declared in the class's 
     * superclass and interfaces
     *
     * @param contextClass the Tea context Class to introspect (any class will
     * work fine)
     */
    public MethodDescriptor[] 
    getTeaContextMethodDescriptors(Class<?> contextClass) {

        return getTeaContextMethodDescriptors(contextClass, false);
    }

    /**
     * Gets the MethodDescriptors of the specified context class
     *
     * @param contextClass the Tea context Class to introspect (any class will
     * work fine)
     * @param specifiedClassOnly true indicates that this function should 
     * only return MethodDescriptors declared by the specified Class.  
     */
    public MethodDescriptor[] getTeaContextMethodDescriptors(
                                                 Class<?> contextClass,
                                                 boolean specifiedClassOnly) {

        Vector<MethodDescriptor> v = new Vector<MethodDescriptor>();

        MethodDescriptor[] methodDescriptors = null;
        try {                
            BeanInfo beanInfo = getBeanInfo(contextClass);
            methodDescriptors = beanInfo.getMethodDescriptors();
        }
        catch (Throwable e) {
            e.printStackTrace();            
        }

        if (methodDescriptors != null) {
            Method[] methods = contextClass.getMethods();
            if (methods.length > methodDescriptors.length) {
                methodDescriptors = addMissingContextMethodDescriptors(
                    methods, methodDescriptors);
            }

            for (int i = 0; i < methodDescriptors.length; i++) {
            
                MethodDescriptor md = methodDescriptors[i];
                Class<?> declaringClass = md.getMethod().getDeclaringClass();

                if (declaringClass != Object.class &&
                    !md.isHidden() && 
                    (!specifiedClassOnly || declaringClass == contextClass)) {
                    
                    v.addElement(md);
                }
            }            
        }
        
        methodDescriptors = new MethodDescriptor[v.size()];
        v.copyInto(methodDescriptors);
        
        sortMethodDescriptors(methodDescriptors);

        return methodDescriptors;
    }

    /**
     * Gets the complete, combined set of MethodDescriptors for the specified
     * context classes.
     *
     * @param contextClasses the Tea context classes to introspect
     */
    public MethodDescriptor[] 
    getTeaContextMethodDescriptors(Class<?>[] contextClasses) {

        Vector<MethodDescriptor> v = new Vector<MethodDescriptor>();
        Hashtable<Method, Method> methods = new Hashtable<Method, Method>();

        if (contextClasses != null) {

            // Combine all of the MethodDescriptors

            for (int i = 0; i < contextClasses.length; i++) {
                Class<?> c = contextClasses[i];
                if (c == null) {
                    continue;
                }

                MethodDescriptor[] mds = 
                    getTeaContextMethodDescriptors(c, false);
                
                for (int j = 0; j < mds.length; j++) {
                    MethodDescriptor md = mds[j];
                    Method m = md.getMethod();
                    // Check uniqueness of method
                    if (methods.get(m) == null) {
                        methods.put(m, m);
                        v.addElement(md);
                    }
                }
            }
        }

        MethodDescriptor[] methodDescriptors = new MethodDescriptor[v.size()];
        v.copyInto(methodDescriptors);

        // Sort the combined results
        sortMethodDescriptors(methodDescriptors);

        return methodDescriptors;
    }

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
    public PropertyDescriptor[] 
    getTeaBeanPropertyDescriptors(Class<?> beanClass) {

        // Code taken from KettleUtilities.getPropertyDescriptors(Class)
        
        if (beanClass == null) {
            return NO_PROPERTIES;
        }

        PropertyDescriptor[] properties = null;

        Map<String, PropertyDescriptor> allProps = null;
        try {
            allProps = BeanAnalyzer.getAllProperties(new GenericType(beanClass));
        }
        catch (Throwable t) {
            return NO_PROPERTIES;           
        }

        Collection<PropertyDescriptor> cleanProps = 
            new ArrayList<PropertyDescriptor>(allProps.size());

        Iterator<Map.Entry<String, PropertyDescriptor>> it = 
            allProps.entrySet().iterator();
        
        while (it.hasNext()) {

            Map.Entry<String, PropertyDescriptor> entry = it.next();
            String name = entry.getKey();
            PropertyDescriptor desc = entry.getValue();
            
            // Discard properties that have no name or should be hidden.
            if (name == null || name.length() == 0 || "class".equals(name)) {
                continue;
            }
            
            if (desc instanceof KeyedPropertyDescriptor) {

                KeyedPropertyDescriptor keyed = (KeyedPropertyDescriptor) desc;
                Class<?> type = keyed.getKeyedPropertyType().getRawType().getType();

                try {
                    // Convert the KeyedPropertyDescriptor to a 
                    // ArrayIndexPropertyDescriptor
                    desc = new ArrayIndexPropertyDescriptor(beanClass, type);
                }
                catch (Throwable t) {
                    continue;
                }
            }
            else if (!beanClass.isArray() && desc.getReadMethod() == null) {
                continue;
            }
            
            cleanProps.add(desc);
        }

        properties = cleanProps.toArray(new PropertyDescriptor[cleanProps.size()]);
        
        // Sort 'em!
        sortPropertyDescriptors(properties);
        
        return properties;
    }


    /**
     * Returns the full class name of the specified class.  This method 
     * provides special formatting for array and inner classes.  If the 
     * specified class is implicitly imported by Tea, then its package is
     * omitted in the returned name.
     */
    public String getTeaFullClassName(Class<?> clazz) {

        if (isImplicitTeaImport(clazz)) {
            return getClassName(clazz);
        }
        
        return getFullClassName(clazz);
    }

    /**
     * Returns true if the specified class is 
     * implicitly imported by Tea.
     * <p>
     * Returns true if the specified class represents a primitive type or
     * a class or interface defined in one of the IMPLICIT_TEA_IMPORTS 
     * packages.  This method also works for array types.
     */
    public boolean isImplicitTeaImport(Class<?> clazz) {
        if (getArrayType(clazz).isPrimitive()) {
            return true;
        }

        return isImplicitTeaImport(getClassPackage(clazz));
    }

    /**
     * Returns true if the specified class or package is
     * implicitly imported by Tea.
     */
    public boolean isImplicitTeaImport(String classOrPackageName) {

        if (classOrPackageName == null) {
            return false;
        }

        if (cPrimativeClasses.get(classOrPackageName) != null) {
            return true;
        }
        
        for (int i = 0; i < IMPLICIT_TEA_IMPORTS.length; i++) {
            String prefix = IMPLICIT_TEA_IMPORTS[i];

            if (classOrPackageName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    
    /**
     * Returns true if the specified method accepts a 
     * <code>Substitution</code> as its last parameter.
     */
    public boolean acceptsSubstitution(MethodDescriptor md) {
        return acceptsSubstitution(md.getMethod());
    }

    /**
     * Returns true if the specified method accepts a 
     * <code>Substitution</code> as its last parameter.
     */
    public boolean acceptsSubstitution(Method m) {
        Class<?>[] paramTypes = m.getParameterTypes();
        if (paramTypes.length > 0) {
            // Check if last param is a Substitution
            Class<?> lastParam = paramTypes[paramTypes.length - 1];
            return Substitution.class.isAssignableFrom(lastParam);
        }
        return false;
    }

    /**
     * Returns true if the specifed class is compatible with Tea's 
     * <code>foreach</code> statement.  Compatibility implies that the
     * class can be iterated on by the <code>foreach</code>.
     */
    public boolean isForeachCompatible(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        return (clazz.isArray() || 
                Collection.class.isAssignableFrom(clazz));         
    }

    /**
     * Returns true if the specifed class is compatible with Tea's <code>if
     * </code> statement.  Only Boolean.class and boolean.class qualify.
     */    
    public boolean isIfCompatible(Class<?> clazz) {    

        if (clazz == null) {
            return false;
        }

        return (clazz == boolean.class || clazz == Boolean.class);
    }

    
    //
    // File name utilities
    //


    /**
     * Removes the file extension (all text after the last '.' character)
     * from the specified fileName.
     * 
     * @param fileName the String with an (optional) extension 
     * 
     * @return the String param sans extension
     */
    public String removeFileExtension(String fileName) {
        
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        
        return fileName;
    }

    /**
     * Returns true if the specifed fileName is a Tea file.
     */
    public boolean isTeaFileName(String fileName) {
        return compareFileExtension(fileName, TEA_FILE_EXTENSION);        
    }
    
    /**
     * Returns true if the specified fileName ends with the specified
     * file extension.
     */
    public boolean compareFileExtension(String fileName,
                                        String extension) {
        
        if (fileName == null || extension == null) {
            return false;
        }
        
        fileName = fileName.toLowerCase().trim();
        extension = extension.toLowerCase();
        return (fileName.endsWith(extension));         
    }

    /**
     * Works around a bug in java.beans.Introspector, where sub-interfaces 
     * do not return their parent's methods, in the method descriptor array.
     */
    protected MethodDescriptor[] addMissingContextMethodDescriptors(
        Method[] methods, MethodDescriptor[] methodDescriptors) {

        List<Method> methodNames = new ArrayList<Method>(methodDescriptors.length);
        Vector<MethodDescriptor> mds = new Vector<MethodDescriptor>(methods.length);

        for (int i=0; i<methodDescriptors.length; i++) {
            methodNames.add(methodDescriptors[i].getMethod());
            mds.add(methodDescriptors[i]);
        }

        for (int i=0; i<methods.length; i++) {
            if (!methodNames.contains(methods[i])) {
                mds.add(new MethodDescriptor(methods[i]));
            }           
        }
        
        methodDescriptors = new MethodDescriptor[mds.size()];
        mds.copyInto(methodDescriptors);

        return methodDescriptors;
    }

    //
    //
    // Non-public interface
    //
    //

    /**
     * This inner class is used to compare or sort FeatureDescriptors by name 
     * if the names are the same and the FeatureDescriptor is an instance of
     * MethodDescriptor it also sorts by parameter count.
     *
     * @author Mark Masse
     * @version

     */
    private static class DescriptorComparator 
        implements Comparator<FeatureDescriptor> {

        /**
         * The compare method determines whether x is less than, greater
         * than, or equal to y.
         * 
         * @return less than zero if x < y; zero if x = y; greater than zero
         * if x > y.
         */
        public int compare(FeatureDescriptor x, FeatureDescriptor y) {
            int nameResult = compareNames(x,y);
            
            if (nameResult == 0 && x != null && y != null &&
                x instanceof MethodDescriptor && 
                y instanceof MethodDescriptor) {

                MethodDescriptor mdX = (MethodDescriptor) x;
                MethodDescriptor mdY = (MethodDescriptor) y;

                Method mX = mdX.getMethod();
                Method mY = mdY.getMethod();
                
                return (getParamCount(mX) - getParamCount(mY));
            }
            return nameResult;
        }
        
        private int compareNames(FeatureDescriptor x, FeatureDescriptor y) {
        
            if (x != null && y != null) {
                return x.getName().compareToIgnoreCase(y.getName());
            }

            return 0;
        }

        private int getParamCount(Method method) {

            int paramCount = 0;

            Class<?>[] paramClasses = method.getParameterTypes();
            if (paramClasses != null) {
                paramCount = paramClasses.length;
            }

            return paramCount;
        }
    }

    /**
     * Slight variation of Tea's KeyedPropertyDescriptor.  Does essentially
     * the same thing, but provides more user-friendly information.
     *
     * @author Mark Masse
     * @version

     */
    private class ArrayIndexPropertyDescriptor 
    extends PropertyDescriptor {
        
        private Class<?> mPropertyType;

        public ArrayIndexPropertyDescriptor(Class<?> beanClass,
                                            Class<?> propertyType) 
        throws IntrospectionException {
            
            super(BeanAnalyzer.KEYED_PROPERTY_NAME, beanClass, null, null);

            mPropertyType = propertyType;

            String typeName = getTeaFullClassName(propertyType);

            setShortDescription("An indexed property with " + 
                                typeName + " elements");
            
        }

        public Class<?> getPropertyType() {
            return mPropertyType;
        }                
    }
}
