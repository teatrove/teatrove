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

package org.teatrove.trove.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.classfile.CodeBuilder;
import org.teatrove.trove.classfile.Label;
import org.teatrove.trove.classfile.LocalVariable;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.classfile.Opcode;
import org.teatrove.trove.classfile.SignatureDesc;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.classfile.generics.ClassTypeDesc;
import org.teatrove.trove.classfile.generics.GenericArrayTypeDesc;
import org.teatrove.trove.classfile.generics.GenericTypeDesc;
import org.teatrove.trove.classfile.generics.GenericTypeFactory;
import org.teatrove.trove.classfile.generics.ParameterizedTypeDesc;
import org.teatrove.trove.generics.GenericType;

/**
 * Merges several classes together, producing a new class that has all of the
 * methods of the combined classes. All methods in the combined class delegate
 * to instances of the source classes. If multiple classes implement the same
 * method, the first one provided is used. The merged class implements all of
 * the interfaces provided by the source classes or interfaces.
 *
 * <p>This class performs a function almost the same as the Proxy class
 * introduced in JDK1.3. It differs in that it supports classes as well as
 * interfaces as input, and it binds to wrapped objects without using
 * runtime reflection. It is less flexible than Proxy in that there isn't way
 * to customize the method delegation, and so it isn't suitable for creating
 * dynamically generated interface implementations.
 *
 * @author Brian S O'Neill, Nick Hagan
 */
public class MergedClass {
    // Maps ClassInjectors to Maps that map to merged class names.
    // Map<ClassInjector, Map<MultiKey, String>>
    // The MultiKey is composed of class names and method prefixes. By storing
    // class names into the maps instead of classes, the classes may be
    // reclaimed by the garbage collector.
    private static Map<ClassInjector, Map<MultiKey, String>> cMergedMap;

    public static final int OBSERVER_DISABLED = 0;  // Invocation events disabled
    public static final int OBSERVER_ENABLED = 1;   // Invocation events enabled
    public static final int OBSERVER_ACTIVE = 2;    // Invocation events enabled and active.
    public static final int OBSERVER_EXTERNAL = 4;    // Invocation events enabled but triggered externally.

    private static final boolean DEBUG = 
        Boolean.getBoolean(MergedClass.class.getName().concat(".DEBUG"));
    
    static {
        try {
            cMergedMap = new IdentityMap(7);
        }
        catch (LinkageError e) {
            cMergedMap = new HashMap<ClassInjector, Map<MultiKey, String>>(7);
        }
        catch (Exception e) {
            // Microsoft VM sometimes throws an undeclared
            // ClassNotFoundException instead of doing the right thing and
            // throwing some form of a LinkageError if the class couldn't
            // be found.
            cMergedMap = new HashMap<ClassInjector, Map<MultiKey, String>>(7);
        }
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor's parameter types match the source classes.
     * An IllegalArgumentException is thrown if any of the given conditions
     * are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types
     * <li>The given classes must be loadable from the given injector
     * <li>At most 254 classes may be merged
     * </ul>
     *
     * Note: Because a constructor is limited to 254 parameters, if more than
     * 254 classes are to be merged together, call {@link #getConstructor2}.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     */
    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes)
        throws IllegalArgumentException
    {
        return getConstructor(injector, classes, null, null, OBSERVER_DISABLED);
    }
    
    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes,
                                                Class<?>[] interfaces)
        throws IllegalArgumentException
    {
        return getConstructor(injector, classes, null, interfaces, OBSERVER_DISABLED);
    }
    
    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor's parameter types match the source classes.
     * An IllegalArgumentException is thrown if any of the given conditions
     * are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * <li>At most 254 classes may be merged
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     * <p>
     * Note: Because a constructor is limited to 254 parameters, if more than
     * 254 classes are to be merged together, call {@link #getConstructor2}.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     */
    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes,
                                                String[] prefixes)
        throws IllegalArgumentException {

        return getConstructor(injector, classes, prefixes, null, OBSERVER_DISABLED);
    }

    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes,
                                                String[] prefixes,
                                                Class<?>[] interfaces)
        throws IllegalArgumentException {

        return getConstructor(injector, classes, prefixes, interfaces, OBSERVER_DISABLED);
    }
    
    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor's parameter types match the source classes.
     * An IllegalArgumentException is thrown if any of the given conditions
     * are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * <li>At most 254 classes may be merged
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     * An optional implementation of the MethodInvocationEventObserver
     * interface may be supplied.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     * <p>
     * Note: Because a constructor is limited to 254 parameters, if more than
     * 254 classes are to be merged together, call {@link #getConstructor2}.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     * @param observerMode int Invocation event handling modes.
     */
    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes,
                                                String[] prefixes,
                                                int observerMode)
        throws IllegalArgumentException
    {
        return getConstructor(injector, classes, prefixes, null, observerMode);
    }

    public static Constructor<?> getConstructor(ClassInjector injector,
                                                Class<?>[] classes,
                                                String[] prefixes,
                                                Class<?>[] interfaces,
                                                int observerMode)
        throws IllegalArgumentException
    {
        if (classes.length > 254) {
            throw new IllegalArgumentException
                ("More than 254 merged classes: " + classes.length);
        }

        Class<?> clazz = 
            getMergedClass(injector, classes, prefixes, interfaces, observerMode);

        try {
            if ((observerMode & OBSERVER_ENABLED) == 0)
                return clazz.getConstructor(classes);
            else {
                ArrayList<Class<?>> classList = 
                    new ArrayList<Class<?>>(classes.length + 1);
                classList.add(InvocationEventObserver.class);
                for (int i = 0; i < classes.length; i++)
                    classList.add(classes[i]);
                return clazz.getConstructor(classList.toArray(new Class[classList.size()]));
            }
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }
    }
    
    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor accepts one parameter type: an
     * {@link InstanceFactory}. Merged instances are requested only when
     * first needed. An IllegalArgumentException is thrown if any of the given
     * conditions are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types
     * <li>The given classes must be loadable from the given injector
     * </ul>
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     */
    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes)
        throws IllegalArgumentException
    {
        return getConstructor2(injector, classes, null, null, OBSERVER_DISABLED);
    }
    
    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes,
                                                 Class<?>[] interfaces)
        throws IllegalArgumentException
    {
        return getConstructor2(injector, classes, null, interfaces, OBSERVER_DISABLED);
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor accepts one parameter type: an
     * {@link InstanceFactory}. Merged instances are requested only when
     * first needed. An IllegalArgumentException is thrown if any of the given
     * conditions are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     */
    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes,
                                                 String[] prefixes)
            throws IllegalArgumentException {
        return getConstructor2(injector, classes, prefixes, null, OBSERVER_DISABLED);
    }
    
    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes,
                                                 String[] prefixes,
                                                 Class<?>[] interfaces)
            throws IllegalArgumentException {
        return getConstructor2(injector, classes, prefixes, interfaces, OBSERVER_DISABLED);
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor accepts one parameter type: an
     * {@link InstanceFactory}. Merged instances are requested only when
     * first needed. An IllegalArgumentException is thrown if any of the given
     * conditions are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     * An optional implementation of the MethodInvocationEventObserver
     * interface may be supplied.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     * @param observerMode int Invocation event handling modes.
     */
    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes,
                                                 String[] prefixes,
                                                 int observerMode)
        throws IllegalArgumentException
    {
        return getConstructor2(injector, classes, prefixes, null, observerMode);
    }

    public static Constructor<?> getConstructor2(ClassInjector injector,
                                                 Class<?>[] classes,
                                                 String[] prefixes,
                                                 Class<?>[] interfaces,
                                                 int observerMode)
        throws IllegalArgumentException
    {
        Class<?> clazz = getMergedClass(injector, classes, prefixes, interfaces, 
                                        observerMode);

        try {
            if ((observerMode & OBSERVER_ENABLED) != 0)
                return clazz.getConstructor(new Class[]{InstanceFactory.class, InvocationEventObserver.class});
            else
                return clazz.getConstructor(new Class[]{InstanceFactory.class});
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }
    }
    
    /**
     * Just create the bytecode for the merged class, but don't load it. Since
     * no ClassInjector is provided to resolve name conflicts, the class name
     * must be manually provided.
     *
     * @param className name to give to merged class
     * @param classes Source classes used to derive merged class
     */
    public static ClassFile buildClassFile(String className, Class<?>[] classes)
        throws IllegalArgumentException
    {
        return buildClassFile(className, classes, null, null, OBSERVER_DISABLED);
    }
    
    public static ClassFile buildClassFile(String className, Class<?>[] classes,
                                           Class<?>[] interfaces)
        throws IllegalArgumentException
    {
        return buildClassFile(className, classes, null, interfaces, OBSERVER_DISABLED);
    }

    /**
     * Just create the bytecode for the merged class, but don't load it. Since
     * no ClassInjector is provided to resolve name conflicts, the class name
     * must be manually provided.
     *
     * @param className name to give to merged class
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     */
    public static ClassFile buildClassFile(String className,
                                           Class<?>[] classes, 
                                           String[] prefixes) {
        return buildClassFile(className, classes, prefixes, null, OBSERVER_DISABLED);
    }

    public static ClassFile buildClassFile(String className,
                                           Class<?>[] classes, 
                                           String[] prefixes,
                                           Class<?>[] interfaces) {
        return buildClassFile(className, classes, prefixes, interfaces, OBSERVER_DISABLED);
    }

    /**
     * Just create the bytecode for the merged class, but don't load it. Since
     * no ClassInjector is provided to resolve name conflicts, the class name
     * must be manually provided.  An optional implementation of the
     * MethodInvocationEventObserver interface may be supplied.
     *
     * @param className name to give to merged class
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     * @param observeMethods boolean Enable function call profiling.
     */
    public static ClassFile buildClassFile(String className,
                                           Class<?>[] classes, 
                                           String[] prefixes,
                                           int observerMode)
        throws IllegalArgumentException
    {
        return buildClassFile(className, classes, prefixes, null, observerMode);
    }

    public static ClassFile buildClassFile(String className,
                                           Class<?>[] classes, 
                                           String[] prefixes,
                                           Class<?>[] interfaces,
                                           int observerMode)
        throws IllegalArgumentException
    {
        ClassEntry[] classEntries = new ClassEntry[classes.length];
        for (int i=0; i<classes.length; i++) {
            if (prefixes == null || i >= prefixes.length) {
                classEntries[i] = new ClassEntry(classes[i]);
            }
            else {
                String prefix = prefixes[i];
                if (prefix != null && prefix.length() == 0) {
                    prefix = null;
                }
                classEntries[i] = new ClassEntry(classes[i], prefix);
            }
        }
        return buildClassFile(null, className, classEntries, interfaces, observerMode);
    }
    
    private static Class<?> getMergedClass(ClassInjector injector,
                                           Class<?>[] classes,
                                           String[] prefixes,
                                           int observerMode)
        throws IllegalArgumentException
    {
        return getMergedClass(injector, classes, prefixes, null, observerMode);
    }

    private static Class<?> getMergedClass(ClassInjector injector,
                                           Class<?>[] classes,
                                           String[] prefixes,
                                           Class<?>[] interfaces, 
                                           int observerMode)
        throws IllegalArgumentException
    {
        ClassEntry[] classEntries = new ClassEntry[classes.length];
        for (int i=0; i<classes.length; i++) {
            // Load the classes from the ClassInjector, just like they will be
            // when the generated class is resolved.
            try {
                classes[i] = injector.loadClass(classes[i].getName());
            }
            catch (ClassNotFoundException e) {
                throw new IllegalArgumentException
                    ("Unable to load class from injector: " + classes[i]);
            }

            if (prefixes == null || i >= prefixes.length) {
                classEntries[i] = new ClassEntry(classes[i]);
            }
            else {
                String prefix = prefixes[i];
                if (prefix != null && prefix.length() == 0) {
                    prefix = null;
                }
                classEntries[i] = new ClassEntry(classes[i], prefix);
            }
        }

        return getMergedClass(injector, classEntries, interfaces, observerMode);
    }
    
    /**
     * Creates a class with a public constructor whose parameter types match
     * the given source classes. Another constructor is also created that
     * accepts an InstanceFactory.
     */
    private static synchronized Class<?> getMergedClass(ClassInjector injector,
                                                        ClassEntry[] classEntries,
                                                        Class<?>[] interfaces,
                                                        int observerMode)
        throws IllegalArgumentException
    {
        Map<MultiKey, String> classListMap = cMergedMap.get(injector);
        if (classListMap == null) {
            classListMap = new HashMap<MultiKey, String>(7);
            cMergedMap.put(injector, classListMap);
        }

        MultiKey key = generateKey(classEntries);
        String mergedName = classListMap.get(key);
        if (mergedName != null) {
            try {
                return injector.loadClass(mergedName);
            }
            catch (ClassNotFoundException e) {
            }
        }

        ClassFile cf;
        try {
            cf = buildClassFile(injector, null, classEntries, interfaces, 
                                observerMode);
        }
        catch (IllegalArgumentException e) {
            e.fillInStackTrace();
            throw e;
        }

        try {
            OutputStream stream = injector.getStream(cf.getClassName());
            cf.writeTo(stream);
            stream.close();
        }
        catch (Throwable e) {
            String output = outputClassToFile(cf);
            throw new RuntimeException(
                "Error generating merged class, contents saved to: " + output, 
                e
            );
        }

        if (DEBUG) {
            String output = outputClassToFile(cf);
            System.out.println("Saved merged class to: " + output);
        }
        
        Class<?> merged;
        try {
            merged = injector.loadClass(cf.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new InternalError(e.toString());
        }

        classListMap.put(key, merged.getName());
        return merged;
    }

    private static String outputClassToFile(ClassFile cf) {
        try {
            File file = 
                File.createTempFile(cf.getClassName(), ".class");
            FileOutputStream out = new FileOutputStream(file);
            cf.writeTo(out);
            out.close();
            return file.getAbsolutePath();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Error: ".concat(e.getMessage());
        }
    }
    
    private static MultiKey generateKey(ClassEntry[] classEntries) {
        int length = classEntries.length;
        Object[] mainElements = new Object[length];
        for (int i=0; i<length; i++) {
            ClassEntry classEntry = classEntries[i];
            mainElements[i] = new MultiKey(new Object[] {
                classEntry.getClazz().getName(),
                classEntry.getMethodPrefix()
            });
        }
        return new MultiKey(mainElements);
    }

    /**
     * @param className if null, auto selects a unique name against injector
     */
    private static ClassFile buildClassFile(ClassInjector injector,
                                            String className,
                                            ClassEntry[] classEntries,
                                            Class<?>[] interfaces,
                                            int observerMode)
        throws IllegalArgumentException
    {
        Set<ClassEntry> classSet = 
            new UsageSet<ClassEntry>(classEntries.length * 2 + 1);
        Set<ClassEntry> nonConflictingClasses =
            new HashSet<ClassEntry>(classEntries.length * 2 + 1);
        String commonPackage = null;
        Set<MethodEntry> conflictingMethods = new HashSet<MethodEntry>();
        Map<MethodEntry, SortedSet<MethodEntry>> methodMap = 
            new HashMap<MethodEntry, SortedSet<MethodEntry>>();

        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            Class<?> clazz = classEntry.getClazz();

            if (clazz.isPrimitive()) {
                throw new IllegalArgumentException
                    ("Merged classes cannot be primitive: " + clazz);
            }

            if (!classSet.add(classEntry)) {
                throw new IllegalArgumentException
                    ("Class is specified more than once: " + clazz);
            }

            if (!Modifier.isPublic(clazz.getModifiers())) {
                String classPackage = clazz.getName();
                int index = classPackage.lastIndexOf('.');
                if (index < 0) {
                    classPackage = "";
                }
                else {
                    classPackage = classPackage.substring(0, index);
                }

                if (commonPackage == null) {
                    commonPackage = classPackage;
                }
                else if (!commonPackage.equals(classPackage)) {
                    throw new IllegalArgumentException
                        ("Not all non-public classes defined in same " +
                         "package: " + commonPackage);
                }
            }

            // Innocent until proven guilty.
            nonConflictingClasses.add(classEntry);

            Method[] methods = clazz.getMethods();
            String prefix = classEntry.getMethodPrefix();

            for (int j=0; j<methods.length; j++) {
                Method method = methods[j];

                String name = method.getName();
                if ("getObserverMode".equals(name) || 
                    "getInvocationObserver".equals(name)) {
                    
                    continue; // don't wrap this.
                }

                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(name)) {
                    continue;
                }

                MethodEntry methodEntry = 
                    new MethodEntry(clazz, i, method, name);
                
                boolean valid = addMethods
                (
                    classEntry, methodEntry, methodMap, 
                    conflictingMethods, nonConflictingClasses
                );
                
                if (!valid && prefix == null) {
                    throw new IllegalArgumentException(
                        "Conflicting return types: " + methodEntry);
                }

                if (prefix != null) {
                    methodEntry = 
                        new MethodEntry(clazz, i, method, prefix + name);
                    
                    valid = addMethods
                    (
                        classEntry, methodEntry, methodMap, 
                        conflictingMethods, nonConflictingClasses
                    );
                    
                    if (!valid) {
                        throw new IllegalArgumentException(
                            "Conflicting return types: " + methodEntry);
                    }
                }
            }
        }

        if (className == null) {
            int id = 0;
            Iterator<ClassEntry> it = classSet.iterator();
            while (it.hasNext()) {
                id = id * 31 + it.next().hashCode();
            }

            className = "MergedClass$";
            try {
                while (true) {
                    className = "MergedClass$" + (id & 0xffffffffL);
                    if (commonPackage != null && commonPackage.length() > 0) {
                        className = commonPackage + '.' + className;
                    }
                    try {
                        injector.loadClass(className);
                    }
                    catch (LinkageError e) {
                    }
                    id++;
                }
            }
            catch (ClassNotFoundException e) {
            }
        }

        ClassFile cf = new ClassFile(className);
        cf.getModifiers().setFinal(true);
        cf.markSynthetic();

        List<GenericTypeDesc> generics = new ArrayList<GenericTypeDesc>();
        
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                GenericType classType = new GenericType(interfaces[i]);
                addAllInterfaces(cf, generics, classType);
            }
        }
        
        for (int i = 0; i < classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            if (nonConflictingClasses.contains(classEntry)) {
                // TODO: detect when multiple classes contain interfaces
                //       whose generic type parameters conflict
                //       ie: Type<Integer> vs Type<Double>
                GenericType classType = new GenericType(classEntry.getClazz());
                addAllInterfaces(cf, generics, classType);
            }
        }
        
        cf.setSignature(SignatureDesc.forClass(
            null, null, generics.toArray(new GenericTypeDesc[generics.size()])
        ).toString());
        
        Modifiers privateAccess = new Modifiers();
        privateAccess.setPrivate(true);
        Modifiers privateFinalAccess = new Modifiers();
        privateFinalAccess.setPrivate(true);
        privateFinalAccess.setFinal(true);
        Modifiers publicAccess = new Modifiers();
        publicAccess.setPublic(true);
        Modifiers privateStaticAccess = new Modifiers();
        privateStaticAccess.setPrivate(true);
        privateStaticAccess.setStatic(true);
        privateStaticAccess.setStatic(true);
        Modifiers publicStaticAccess = new Modifiers();
        publicStaticAccess.setPublic(true);
        publicStaticAccess.setStatic(true);

        // Add field to store optional InstanceFactory.
        TypeDesc instanceFactoryType =
            TypeDesc.forClass(InstanceFactory.class);
        cf.addField(privateAccess,
                    "instanceFactory", instanceFactoryType).markSynthetic();

        Method instanceFactoryMethod;
        try {
            instanceFactoryMethod =
                InstanceFactory.class.getMethod
                ("getInstance", new Class[]{int.class});
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }

        // Add field to store observer mode
        //cf.addField(privateStaticAccess, "mObserverMode", TypeDesc.forClass(int.class)).markSynthetic();
        MethodInfo mo = cf.addMethod(publicStaticAccess, "getObserverMode", TypeDesc.forClass(int.class));
        mo.markSynthetic();
        CodeBuilder a = new CodeBuilder(mo);
        //a.loadStaticField("mObserverMode", TypeDesc.forClass(int.class));
        a.loadConstant(observerMode);
        a.returnValue(TypeDesc.forClass(int.class));
        a = null;

        // Add field to store optional InvocationEventObserver
        TypeDesc methodObserverType = TypeDesc.forClass(InvocationEventObserver.class);
        if ((observerMode & OBSERVER_ENABLED) != 0) {
            cf.addField(privateStaticAccess, "mInvocationObserver", methodObserverType).markSynthetic();
            MethodInfo me = cf.addMethod(publicStaticAccess, "getInvocationObserver", methodObserverType);
            me.markSynthetic();
            CodeBuilder b = new CodeBuilder(me);
            b.loadStaticField("mInvocationObserver", methodObserverType);
            b.returnValue(TypeDesc.OBJECT);
            b = null;
        }

        // Define fields which point to wrapped objects, and define methods
        // to access the fields.
        String[] fieldNames = new String[classEntries.length];
        TypeDesc[] types = new TypeDesc[classEntries.length];
        for (int i=0; i<classEntries.length; i++) {
            Class<?> clazz = classEntries[i].getClazz();
            String fieldName = "m$" + i;
            TypeDesc type = TypeDesc.forClass(clazz);
            cf.addField(privateAccess, fieldName, type).markSynthetic();
            fieldNames[i] = fieldName;
            types[i] = type;

            // Create method that returns field, calling the InstanceFactory
            // if necessary to initialize for the first time.
            MethodInfo mi = cf.addMethod
                (privateFinalAccess, fieldName, type);
            mi.markSynthetic();
            CodeBuilder builder = new CodeBuilder(mi);
            builder.loadThis();
            builder.loadField(fieldName, type);
            builder.dup();
            Label isNull = builder.createLabel();
            builder.ifNullBranch(isNull, true);
            // Return the initialized field.
            builder.returnValue(TypeDesc.OBJECT);
            isNull.setLocation();
            // Discard null field value.
            builder.pop();
            builder.loadThis();
            builder.loadField("instanceFactory", instanceFactoryType);
            builder.dup();
            Label haveInstanceFactory = builder.createLabel();
            builder.ifNullBranch(haveInstanceFactory, false);
            // No instanceFactory: return null.
            builder.loadConstant(null);
            builder.returnValue(TypeDesc.OBJECT);
            haveInstanceFactory.setLocation();
            builder.loadConstant(i);
            builder.invoke(instanceFactoryMethod);
            builder.checkCast(type);
            builder.dup();
            builder.loadThis();
            builder.swap();
            builder.storeField(fieldName, type);
            builder.returnValue(TypeDesc.OBJECT);
        }

        // Define a constructor that initializes fields from an Object array. A method invocation
        // observer can be optionally specified.
        if (classEntries.length <= 254) {
            MethodInfo mi = null;
            if ((observerMode & OBSERVER_ENABLED) == 0)
                mi = cf.addConstructor(publicAccess, types);
            else {
                ArrayList<TypeDesc> typesList = 
                    new ArrayList<TypeDesc>(types.length + 1);
                typesList.add(methodObserverType);
                for (int i = 0; i < types.length; i++)
                    typesList.add(types[i]);
                mi = cf.addConstructor(publicAccess, typesList.toArray(
                    new TypeDesc[typesList.size()]));
            }

            CodeBuilder builder = new CodeBuilder(mi);
            builder.loadThis();
            builder.invokeSuperConstructor();
            LocalVariable[] params = builder.getParameters();


            // Handle method observer interface type
            int j = 0;
            if ((observerMode & OBSERVER_ENABLED) != 0) {
                j++;
                builder.loadThis();
                builder.loadLocal(params[0]);
                builder.storeStaticField("mInvocationObserver", methodObserverType);
            }

            for (int i = 0; i<classEntries.length; i++, j++) {
                builder.loadThis();
                builder.loadLocal(params[j]);
                builder.storeField(fieldNames[i], types[i]);
            }

            builder.returnVoid();
            builder = null;
        }

        // Define a constructor that saves an InstanceFactory. A method invocation
        // observer can be optionally added.
        MethodInfo mi = null;
        if ((observerMode & OBSERVER_ENABLED) == 0)
            mi = cf.addConstructor(publicAccess, new TypeDesc[]{instanceFactoryType});
        else
            mi = cf.addConstructor(publicAccess, new TypeDesc[]{instanceFactoryType, methodObserverType});

        CodeBuilder builder = new CodeBuilder(mi);
        builder.loadThis();
        builder.invokeSuperConstructor();
        builder.loadThis();
        builder.loadLocal(builder.getParameters()[0]);
        builder.storeField("instanceFactory", instanceFactoryType);
        if ((observerMode & OBSERVER_ENABLED) != 0) {
            builder.loadThis();
            builder.loadLocal(builder.getParameters()[1]);
            builder.storeStaticField("mInvocationObserver", methodObserverType);
        }

        builder.returnVoid();
        builder = null;

        // Define all the wrapper methods.
        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            String prefix = classEntry.getMethodPrefix();

            Class<?> clazz = classEntry.getClazz();
            Method[] methods = clazz.getMethods();
            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];
                
                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(method.getName())) {
                    continue;
                }
                
                if ("getObserverMode".equals(method.getName()) || 
                    "getInvocationObserver".equals(method.getName())) {
                    continue;
                }

                // add general method
                MethodEntry methodEntry = new MethodEntry(clazz, method);
                generateMethods(cf, fieldNames, types, methodEntry, 
                                observerMode, methodMap, conflictingMethods);

                // add method with prefix if available
                if (prefix != null) {
                    methodEntry = new MethodEntry
                    (
                        clazz, method, prefix + method.getName()
                    );
                    
                    generateMethods(cf, fieldNames, types, methodEntry, 
                                    observerMode, 
                                    methodMap, conflictingMethods);
                }
            }
        }
        
        return cf;
    }
    
    private static boolean addMethods(ClassEntry classEntry, 
        MethodEntry methodEntry, 
        Map<MethodEntry, SortedSet<MethodEntry>> methodMap, 
        Set<MethodEntry> conflictingMethods,
        Set<ClassEntry> nonConflictingClasses) {
        
        // ignore if method is conflicting with other similar methods
        // conflicting methods are ones that have different return types
        // with the same method name and parameter types
        if (conflictingMethods.contains(methodEntry)) { return true; }
        
        // check if method was previously discovered with similar signature
        SortedSet<MethodEntry> existing = methodMap.get(methodEntry);

        // method not previously discovered, so add to single sorted set
        // the set is ordered by return type so the most specific return type
        // is first on the list (this allows ease to determine bridge methods)
        if (existing == null) {
            existing = new TreeSet<MethodEntry>();
            existing.add(methodEntry);
            methodMap.put(methodEntry, existing);
            return true;
        }
        
        // otherwise, check for compatibility
        // if valid, add to the list
        // if not, mark as conflict and update conflicts
        boolean conflict = false;
        for (MethodEntry next : existing) {
            if (next.returnTypeDiffers(methodEntry)) {
                conflict = true;
                conflictingMethods.add(methodEntry);
                nonConflictingClasses.remove(classEntry);
            }
        }
        
        // add to set
        if (!conflict) {
            existing.add(methodEntry);
        }

        // return whether type caused conflict or not
        return !conflict;
    }
    
    private static void generateMethods(ClassFile cf, String[] fieldNames, 
        TypeDesc[] types, MethodEntry methodEntry, int observerMode,
        Map<MethodEntry, SortedSet<MethodEntry>> methodMap, 
        Set<MethodEntry> conflictingMethods) {
        
        // ignore if method is conflicting with other similar methods
        // conflicting methods are ones that have different return types
        // with the same method name and parameter types
        if (conflictingMethods.contains(methodEntry)) { return; }
        
        // check if the method and its bridges were properly discovered
        SortedSet<MethodEntry> methodEntries = methodMap.get(methodEntry);
        if (methodEntries != null) {
            boolean observeMethods = 
                (observerMode & OBSERVER_ENABLED) != 0 && 
                (observerMode & OBSERVER_ACTIVE) != 0;
            
            // mark processed by removing from list
            methodMap.remove(methodEntry);
            
            // add top level method as most specific type (lowest bounds)
            Iterator<MethodEntry> it = methodEntries.iterator();
            MethodEntry firstEntry = it.next();
            
            String fieldName = fieldNames[firstEntry.getIndex()];
            TypeDesc type = types[firstEntry.getIndex()];
            addWrapperMethod(cf, firstEntry, fieldName, type, observeMethods);
            
            // add any remaining methods (superclasses) as bridged methods
            while (it.hasNext()) {
                addBridgeMethod(cf, firstEntry, it.next());
            }
        }
    }
    
    private static void addAllInterfaces(ClassFile cf, 
                                         List<GenericTypeDesc> generics,
                                         GenericType type) {

        if (type == null) {
            return;
        }
        
        if (type.isInterface()) {
            Class<?> clazz = type.getRawType().getType();
            cf.addInterface(clazz);
            GenericType[] args = type.getTypeArguments();
            if (args != null && args.length > 0) {
                GenericTypeDesc[] typeArgs = new GenericTypeDesc[args.length];
                for (int i = 0; i < args.length; i++) {
                    typeArgs[i] = 
                        ClassTypeDesc.forType(args[i].getRawType().getType());
                }
                
                generics.add(ParameterizedTypeDesc.forType(
                    ClassTypeDesc.forType(clazz), typeArgs));
            }
            else {
                generics.add(ClassTypeDesc.forType(clazz));
            }
        }
        
        addAllInterfaces(cf, generics, type.getSupertype());

        GenericType[] interfaces = type.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            addAllInterfaces(cf, generics, interfaces[i]);
        }
    }

    private static GenericTypeDesc resolve(GenericType type) {
        Type genericType = type.getGenericType();
        if (genericType instanceof Class) {
            return GenericTypeFactory.fromType(genericType);
        }
        else if (genericType instanceof TypeVariable) {
            return resolve(type.getRawType());
        }
        else if (genericType instanceof ParameterizedType) {
            GenericType[] args = type.getTypeArguments();
            GenericTypeDesc[] argDescs = new GenericTypeDesc[args.length];
            for (int i = 0; i < args.length; i++) {
                argDescs[i] = resolve(args[i]);
            }

            return ParameterizedTypeDesc.forType
            (
                (ClassTypeDesc) GenericTypeFactory.fromType
                (
                    type.getRawType().getType()
                ),
                argDescs
            );
        }
        else if (genericType instanceof WildcardType) {
            return resolve(type.getRawType());
        }
        else if (genericType instanceof GenericArrayType) {
            GenericTypeDesc compType =
                resolve(type.getRootComponentType());

            for (int i =  0; i < type.getDimensions(); i++) {
                compType = GenericArrayTypeDesc.forType(compType);
            }

            return compType;
        }
        else {
            throw new IllegalStateException("invalid type: " + type);
        }
    }

    private static void addBridgeMethod(ClassFile cf,
                                        MethodEntry mainMethod,
                                        MethodEntry bridgeMethod) {

        Method method = bridgeMethod.getMethod();

        // Don't override any methods in Object, especially final ones.
        if (isDefinedInObject(method)) {
            return;
        }
        
        Modifiers modifiers = new Modifiers(method.getModifiers());
        modifiers.setAbstract(false);
        modifiers.setFinal(true);
        modifiers.setSynchronized(false);
        modifiers.setNative(false);
        modifiers.setStatic(false);
        modifiers.setBridge(true);

        Modifiers staticModifiers = (Modifiers)modifiers.clone();
        staticModifiers.setStatic(true);

        GenericType mainType = new GenericType(mainMethod.getReturnType());
        TypeDesc mainReturn = TypeDesc.forClass(mainType.getRawType().getType(),
                                         resolve(mainType));
        
        GenericType returnType = new GenericType(bridgeMethod.getReturnType());
        TypeDesc ret = TypeDesc.forClass(returnType.getRawType().getType(),
                                         resolve(returnType));

        Class<?>[] paramTypes = bridgeMethod.getParameterTypes();
        TypeDesc[] params = new TypeDesc[paramTypes.length];
        for (int i=0; i<params.length; i++) {
            GenericType paramType = new GenericType(paramTypes[i]);
            params[i] = TypeDesc.forClass(paramType.getRawType().getType(),
                                          resolve(paramType));
        }
        
        MethodInfo mi;
        if (Modifier.isStatic(method.getModifiers())) {
            mi = cf.addMethod
                (staticModifiers, bridgeMethod.getName(), ret, params);
        }
        else {
            mi = cf.addMethod(modifiers, bridgeMethod.getName(), ret, params);
        }

        // Exception stuff...
        Class<?>[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        // Check for deprecation
        if (ClassUtils.isDeprecated(method)) {
            mi.addRuntimeVisibleAnnotation(TypeDesc.forClass(Deprecated.class));
        }
        
        // Delegate to wrapped object.
        CodeBuilder builder = new CodeBuilder(mi);
        if (!Modifier.isStatic(method.getModifiers())) {
            builder.loadThis();
        }
        
        LocalVariable[] locals = builder.getParameters();
        for (int i=0; i<locals.length; i++) {
            builder.loadLocal(locals[i]);
        }
        
        builder.invokeVirtual(bridgeMethod.getName(), mainReturn, params);
        builder.returnValue(TypeDesc.OBJECT);
    }
    
    private static void addWrapperMethod(ClassFile cf,
                                         MethodEntry methodEntry,
                                         String fieldName,
                                         TypeDesc type,
                                         boolean observeMethods) {
        Method method = methodEntry.getMethod();
        TypeDesc methodObserverType =
            TypeDesc.forClass(InvocationEventObserver.class);

        // Don't override any methods in Object, especially final ones.
        if (isDefinedInObject(method)) {
            return;
        }
        
        Modifiers modifiers = new Modifiers(method.getModifiers());
        modifiers.setAbstract(false);
        modifiers.setFinal(true);
        modifiers.setSynchronized(false);
        modifiers.setNative(false);
        modifiers.setStatic(false);
        modifiers.setBridge(false);

        Modifiers staticModifiers = (Modifiers)modifiers.clone();
        staticModifiers.setStatic(true);

        Class<?> returnClass = method.getReturnType();
        Class<?> returnType = methodEntry.getReturnType();
        GenericType genericReturn = methodEntry.getGenericReturnType();
        TypeDesc ret = TypeDesc.forClass(returnType, resolve(genericReturn));

        Class<?>[] paramClasses = method.getParameterTypes();
        GenericType[] genericParams = methodEntry.getGenericParameterTypes();
        Class<?>[] paramTypes = methodEntry.getParameterTypes();
        TypeDesc[] params = new TypeDesc[paramTypes.length];
        for (int i=0; i<params.length; i++) {
            params[i] = 
                TypeDesc.forClass(paramTypes[i], resolve(genericParams[i]));
        }
        
        MethodInfo mi;
        if (Modifier.isStatic(method.getModifiers())) {
            mi = cf.addMethod
                (staticModifiers, methodEntry.getName(), ret, params);
        }
        else {
            mi = cf.addMethod(modifiers, methodEntry.getName(), ret, params);
        }

        // Exception stuff...
        Class<?>[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        // Check for deprecation
        if (ClassUtils.isDeprecated(method)) {
            mi.addRuntimeVisibleAnnotation(TypeDesc.forClass(Deprecated.class));
        }
        
        // Delegate to wrapped object.
        CodeBuilder builder = new CodeBuilder(mi);

        LocalVariable retVal = null;
        if (method.getReturnType() != void.class) {
            retVal = builder.createLocalVariable("retVal", ret);
        }

        LocalVariable startTime = null;
        if (observeMethods) {
            startTime = builder.createLocalVariable("startTime",
                TypeDesc.forClass(long.class));
            builder.loadStaticField("mInvocationObserver", methodObserverType);
            builder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                TypeDesc.forClass(long.class));
            builder.storeLocal(startTime);

        }

        if (!Modifier.isStatic(method.getModifiers())) {
            builder.loadThis();
            builder.loadField(fieldName, type);
            Label isNonNull = builder.createLabel();
            builder.dup();
            builder.ifNullBranch(isNonNull, false);
            // Discard null field value.
            builder.pop();
            // Call the field access method, which in turn calls the
            // InstanceFactory.
            builder.loadThis();
            builder.invokePrivate(fieldName, type);
            isNonNull.setLocation();
        }
        LocalVariable[] locals = builder.getParameters();
        for (int i=0; i<locals.length; i++) {
            builder.loadLocal(locals[i]);
            if (!paramClasses[i].isAssignableFrom(paramTypes[i])) {
                builder.checkCast(TypeDesc.forClass(paramClasses[i]));
            }
        }

        builder.invoke(method);

        if (method.getReturnType() != void.class) {
            if (!methodEntry.getReturnType().isAssignableFrom(returnClass)) {
                builder.checkCast(ret);
            }
            builder.storeLocal(retVal);
        }


        if (observeMethods) {
            builder.loadStaticField("mInvocationObserver", methodObserverType);
            builder.loadConstant(null);
            builder.loadConstant(methodEntry.getName());
            builder.loadStaticField("mInvocationObserver", methodObserverType);
            builder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                TypeDesc.forClass(long.class));
            builder.loadLocal(startTime);
            builder.math(Opcode.LSUB);
            builder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                new TypeDesc[] { TypeDesc.forClass(String.class), TypeDesc.forClass(String.class),
                TypeDesc.forClass(long.class) });
        }

        if (method.getReturnType() == void.class) {
            builder.returnVoid();
        }
        else {
            builder.loadLocal(retVal);
            builder.returnValue(ret);
        }
    }
    
    private static boolean isDefinedInObject(Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return true;
        }

        Class<?>[] types = method.getParameterTypes();
        String name = method.getName();

        if (types.length == 0) {
            return
                "hashCode".equals(name) ||
                "clone".equals(name) ||
                "toString".equals(name) ||
                "finalize".equals(name);
        }
        else {
            return
                types.length == 1 &&
                types[0] == Object.class &&
                "equals".equals(name);
        }
    }

    private MergedClass() {
    }

    /**
     * InstanceFactory allows merged class instances to be requested only
     * when first needed.
     */
    public interface InstanceFactory {
        /**
         * Return a merged class instance by index. This index corresponds to
         * the class index used when defining the MergedClass.
         */
        public Object getInstance(int index);
    }

    /**
     * Instances of the InvocationEventObserver can optionally be passed as a
     * parameter to an overloaded MergedClass constructor.  A class external to
     * MergedClass should implement this interface to act as a (GoF) mediator for
     * collecting elapsed time information.
     */
    public interface InvocationEventObserver {
        /**
         * Handle method post call invocation events.
         */
        public void invokedEvent(String caller, String callee, long elapsedTimeMillis);

        /**
         * Called to retrieve the current timestamp.
         */
        public long currentTime();
    }

    private static class ClassEntry {
        private final Class<?> mClazz;
        private final String mPrefix;

        public ClassEntry(Class<?> clazz) {
            this(clazz, null);
        }

        public ClassEntry(Class<?> clazz, String prefix) {
            mClazz = clazz;
            mPrefix = prefix;
        }

        public Class<?> getClazz() {
            return mClazz;
        }

        public String getMethodPrefix() {
            return mPrefix;
        }

        public int hashCode() {
            int hash = mClazz.getName().hashCode();
            return (mPrefix == null) ? hash : hash ^ mPrefix.hashCode();
        }

        public boolean equals(Object other) {
            if (other instanceof ClassEntry) {
                ClassEntry classEntry = (ClassEntry)other;
                if (mClazz == classEntry.mClazz) {
                    if (mPrefix == null) {
                        return classEntry.mPrefix == null;
                    }
                    else {
                        return mPrefix.equals(classEntry.mPrefix);
                    }
                }
            }
            return false;
        }

        public String toString() {
            return mClazz.toString();
        }
    }

    private static class MethodEntry implements Comparable<MethodEntry> {
        private final int mIndex;
        private final Method mMethod;
        private final String mName;
        private Class<?> mReturn;
        private GenericType mGenericReturn;
        private List<Class<?>> mParams;
        private List<GenericType> mGenericParams;
        private int mHashCode;

        public MethodEntry(Class<?> type, Method method) {
            this(type, -1, method, method.getName());
        }
        
        public MethodEntry(Class<?> type, Method method, String name) {
            this(type, -1, method, name);
        }
        
        public MethodEntry(Class<?> type, int index, Method method, 
                           String name) {
            mMethod = method;
            mName = name;
            mIndex = index;
            mGenericReturn = new GenericType
            (
                new GenericType(type), 
                method.getReturnType(), method.getGenericReturnType()
            );
            mReturn = mGenericReturn.getRawType().getType();

            Class<?>[] paramClasses = method.getParameterTypes();
            Type[] paramTypes = method.getGenericParameterTypes();
            mParams = new ArrayList<Class<?>>(paramClasses.length);
            mGenericParams = new ArrayList<GenericType>(paramClasses.length);
            for (int i = 0; i < paramClasses.length; i++) {
                GenericType param = new GenericType
                (
                    new GenericType(type), paramClasses[i], paramTypes[i]
                );
                
                mGenericParams.add(param);
                mParams.add(param.getRawType().getType());
            }

            int paramHash = 1;
            for (Class<?> param : mParams) {
                paramHash = 31 * paramHash + param.getName().hashCode();
            }
            
            mHashCode = mName.hashCode() ^ paramHash;
        }
        
        public Class<?> getReturnType() {
            return mReturn;
        }
        
        public GenericType getGenericReturnType() {
            return mGenericReturn;
        }
        
        public Class<?>[] getParameterTypes() {
            return mParams.toArray(new Class<?>[mParams.size()]);
        }
        
        public GenericType[] getGenericParameterTypes() {
            return mGenericParams.toArray(
               new GenericType[mGenericParams.size()]);
        }

        public Method getMethod() {
            return mMethod;
        }

        public String getName() {
            return mName;
        }

        public int getIndex() {
            return mIndex;
        }
        
        public boolean returnTypeDiffers(MethodEntry methodEntry) {
            return !(mReturn.isAssignableFrom(methodEntry.mReturn) ||
                     methodEntry.mReturn.isAssignableFrom(mReturn));
        }

        @Override
        public int hashCode() {
            return mHashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof MethodEntry)) {
                return false;
            }
            
            MethodEntry methodEntry = (MethodEntry) other;
            
            // check if name and return type equal
            // NOTE: check fully-qualified name to avoid class loader issues
            //       we accept different class loaders as the types should
            //       resolve at class injection time
            if (!mName.equals(methodEntry.mName)) {
                return false;
            }
            
            // check compatible parameter lists
            if (mParams.size() != methodEntry.mParams.size()) {
                return false;
            }
            
            // check each paramater type name (see note above on return type)
            int size = mParams.size();
            for (int i = 0; i < size; i++) {
                if (!mParams.get(i).getName().equals(
                        methodEntry.mParams.get(i).getName())) {
                    return false;
                }
            }
            
            // all valid
            return true;
        }

        @Override
        public String toString() {
            return mMethod.toString();
        }

        @Override
        public int compareTo(MethodEntry entry) {
            // check if equal
            if (mReturn.equals(entry.mReturn)) {
                return 0;
            }
            
            // check if super class
            if (mReturn.isAssignableFrom(entry.mReturn)) {
                return 1;
            }
            
            // check if subclass
            if (entry.mReturn.isAssignableFrom(mReturn)) {
                return -1;
            }
            
            // invalid case
            throw new IllegalArgumentException(
                "return types are not compatible: " + 
                mReturn + ", " + entry.mReturn
            );
        }
    }
}
