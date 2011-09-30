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

import java.io.IOException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.classfile.CodeBuilder;
import org.teatrove.trove.classfile.Label;
import org.teatrove.trove.classfile.LocalVariable;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.classfile.Opcode;
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
    private static Map cMergedMap;

    public static final int OBSERVER_DISABLED = 0;  // Invocation events disabled
    public static final int OBSERVER_ENABLED = 1;   // Invocation events enabled
    public static final int OBSERVER_ACTIVE = 2;    // Invocation events enabled and active.
    public static final int OBSERVER_EXTERNAL = 4;    // Invocation events enabled but triggered externally.

    static {
        try {
            cMergedMap = new IdentityMap(7);
        }
        catch (LinkageError e) {
            cMergedMap = new HashMap(7);
        }
        catch (Exception e) {
            // Microsoft VM sometimes throws an undeclared
            // ClassNotFoundException instead of doing the right thing and
            // throwing some form of a LinkageError if the class couldn't
            // be found.
            cMergedMap = new HashMap(7);
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
    public static Constructor getConstructor(ClassInjector injector,
                                             Class[] classes)
        throws IllegalArgumentException
    {
        return getConstructor(injector, classes, null, OBSERVER_DISABLED);
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
    public static Constructor getConstructor(ClassInjector injector,
                                             Class[] classes,
                                             String[] prefixes)
        throws IllegalArgumentException {

        return getConstructor(injector, classes, prefixes, OBSERVER_DISABLED);
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
    public static Constructor getConstructor(ClassInjector injector,
                                             Class[] classes,
                                             String[] prefixes,
                                             int observerMode)
        throws IllegalArgumentException
    {
        if (classes.length > 254) {
            throw new IllegalArgumentException
                ("More than 254 merged classes: " + classes.length);
        }

        Class clazz = getMergedClass(injector, classes, prefixes, observerMode);

        try {
            if ((observerMode & OBSERVER_ENABLED) == 0)
                return clazz.getConstructor(classes);
            else {
                ArrayList classList = new ArrayList(classes.length + 1);
                classList.add(InvocationEventObserver.class);
                for (int i = 0; i < classes.length; i++)
                    classList.add(classes[i]);
                return clazz.getConstructor((Class[]) classList.toArray(new Class[classList.size()]));
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
    public static Constructor getConstructor2(ClassInjector injector,
                                              Class[] classes)
        throws IllegalArgumentException
    {
        return getConstructor2(injector, classes, null, OBSERVER_DISABLED);
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
    public static Constructor getConstructor2(ClassInjector injector,
                                              Class[] classes,
                                              String[] prefixes)
            throws IllegalArgumentException {
        return getConstructor2(injector, classes, prefixes, OBSERVER_DISABLED);

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
    public static Constructor getConstructor2(ClassInjector injector,
                                              Class[] classes,
                                              String[] prefixes,
                                              int observerMode)
        throws IllegalArgumentException
    {
        Class clazz = getMergedClass(injector, classes, prefixes, observerMode);

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
    public static ClassFile buildClassFile(String className, Class[] classes)
        throws IllegalArgumentException
    {
        return buildClassFile(className, classes, null, OBSERVER_DISABLED);
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
                                           Class[] classes, String[] prefixes) {
        return buildClassFile(className, classes, prefixes, OBSERVER_DISABLED);
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
                                           Class[] classes, String[] prefixes,
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
        return buildClassFile(null, className, classEntries, observerMode);
    }

    private static Class getMergedClass(ClassInjector injector,
                                        Class[] classes,
                                        String[] prefixes,
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

        return getMergedClass(injector, classEntries, observerMode);
    }

    /**
     * Creates a class with a public constructor whose parameter types match
     * the given source classes. Another constructor is also created that
     * accepts an InstanceFactory.
     */
    private static synchronized Class getMergedClass(ClassInjector injector,
                                                     ClassEntry[] classEntries,
                                                     int observerMode)
        throws IllegalArgumentException
    {
        Map classListMap = (Map)cMergedMap.get(injector);
        if (classListMap == null) {
            classListMap = new HashMap(7);
            cMergedMap.put(injector, classListMap);
        }

        Object key = generateKey(classEntries);
        String mergedName = (String)classListMap.get(key);
        if (mergedName != null) {
            try {
                return injector.loadClass(mergedName);
            }
            catch (ClassNotFoundException e) {
            }
        }

        ClassFile cf;
        try {
            cf = buildClassFile(injector, null, classEntries, observerMode);
        }
        catch (IllegalArgumentException e) {
            e.fillInStackTrace();
            throw e;
        }

        /*
        try {
            java.io.FileOutputStream out =
                new java.io.FileOutputStream(cf.getClassName() + ".class");
            cf.writeTo(out);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        */

        try {
            OutputStream stream = injector.getStream(cf.getClassName());
            cf.writeTo(stream);
            stream.close();
        }
        catch (IOException e) {
            throw new InternalError(e.toString());
        }

        Class merged;
        try {
            merged = injector.loadClass(cf.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new InternalError(e.toString());
        }

        classListMap.put(key, merged.getName());
        return merged;
    }

    private static Object generateKey(ClassEntry[] classEntries) {
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
                                            int observerMode)
        throws IllegalArgumentException
    {
        Set classSet = new UsageSet(classEntries.length * 2 + 1);
        Set nonConflictingClasses = new HashSet(classEntries.length * 2 + 1);
        String commonPackage = null;
        Map methodMap = new HashMap();


        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            Class clazz = classEntry.getClazz();

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
                if ("getObserverMode".equals(name) || "getInvocationObserver".equals(name))
                    continue; // don't wrap this.

                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(name)) {
                    continue;
                }

                MethodEntry methodEntry = new MethodEntry(clazz, method, name);
                MethodEntry existing = (MethodEntry)methodMap.get(methodEntry);

                if (existing == null) {
                    methodMap.put(methodEntry, methodEntry);
                }
                else if (existing.returnTypeDiffers(methodEntry)) {
                    nonConflictingClasses.remove(classEntry);
                    if (prefix == null) {
                        throw new IllegalArgumentException
                            ("Conflicting return types: " +
                             existing + ", " + methodEntry);
                    }
                }

                if (prefix != null) {
                    name = prefix + name;

                    methodEntry = new MethodEntry(clazz, method, name);
                    existing = (MethodEntry)methodMap.get(methodEntry);

                    if (existing == null) {
                        methodMap.put(methodEntry, methodEntry);
                    }
                    else if (existing.returnTypeDiffers(methodEntry)) {
                        nonConflictingClasses.remove(classEntry);
                        throw new IllegalArgumentException
                            ("Conflicting return types: " +
                             existing + ", " + methodEntry);
                    }
                }
            }
        }

        if (className == null) {
            int id = 0;
            Iterator it = classSet.iterator();
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

        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            if (nonConflictingClasses.contains(classEntry)) {
                addAllInterfaces(cf, classEntry.getClazz());
            }
        }

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
            Class clazz = classEntries[i].getClazz();
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
                ArrayList typesList = new ArrayList(types.length + 1);
                typesList.add(methodObserverType);
                for (int i = 0; i < types.length; i++)
                    typesList.add(types[i]);
                mi = cf.addConstructor(publicAccess, (TypeDesc[]) typesList.toArray(
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

        Set methodSet = methodMap.keySet();

        // Define all the wrapper methods.
        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            String prefix = classEntry.getMethodPrefix();
            String fieldName = fieldNames[i];
            TypeDesc type = types[i];

            Class clazz = classEntry.getClazz();
            Method[] methods = clazz.getMethods();
            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];
                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(method.getName())) {
                    continue;
                }
                if ("getObserverMode".equals(method.getName()) || "getInvocationObserver".equals(method.getName()))
                    continue;

                MethodEntry methodEntry = new MethodEntry(clazz, method);
                if (methodSet.contains(methodEntry)) {
                    methodSet.remove(methodEntry);
                    addWrapperMethod(cf, methodEntry, fieldName, type, (observerMode & OBSERVER_ENABLED) != 0 && (observerMode & OBSERVER_ACTIVE) != 0);
                }

                if (prefix != null) {
                    methodEntry = new MethodEntry
                        (clazz, method, prefix + method.getName());
                    if (methodSet.contains(methodEntry)) {
                        methodSet.remove(methodEntry);
                        addWrapperMethod(cf, methodEntry, fieldName, type, (observerMode & OBSERVER_ENABLED) != 0 && (observerMode & OBSERVER_ACTIVE) != 0);
                    }
                }
            }
        }

        return cf;
    }

    private static void addAllInterfaces(ClassFile cf, Class clazz) {
        if (clazz == null) {
            return;
        }

        if (clazz.isInterface()) {
            cf.addInterface(clazz);
        }

        addAllInterfaces(cf, clazz.getSuperclass());

        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            addAllInterfaces(cf, interfaces[i]);
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

    private static void addWrapperMethod(ClassFile cf,
                                         MethodEntry methodEntry,
                                         String fieldName,
                                         TypeDesc type,
                                         boolean observeMethods) {
        Class methodType = methodEntry.getType();
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

        Modifiers staticModifiers = (Modifiers)modifiers.clone();
        staticModifiers.setStatic(true);

        GenericType returnType =
            new GenericType(new GenericType(methodType), method.getReturnType(),
                            method.getGenericReturnType());

        TypeDesc ret = TypeDesc.forClass(returnType.getRawType().getType(),
                                         resolve(returnType));

        Class[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            GenericType paramType =
                new GenericType(new GenericType(methodType), paramClasses[i],
                                paramTypes[i]);

            params[i] = TypeDesc.forClass(paramType.getRawType().getType(),
                                          resolve(paramType));
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
        Class[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
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
        }

        builder.invoke(method);

        if (method.getReturnType() != void.class)
            builder.storeLocal(retVal);


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

        Class[] types = method.getParameterTypes();
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
        private final Class mClazz;
        private final String mPrefix;

        public ClassEntry(Class clazz) {
            this(clazz, null);
        }

        public ClassEntry(Class clazz, String prefix) {
            mClazz = clazz;
            mPrefix = prefix;
        }

        public Class getClazz() {
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

    private static class MethodEntry {
        private final Class mType;
        private final Method mMethod;
        private final String mName;
        private Class mReturn;
        private List mParams;
        private int mHashCode;

        public MethodEntry(Class type, Method method) {
            this(type, method, method.getName());
        }

        public MethodEntry(Class type, Method method, String name) {
            mType = type;
            mMethod = method;
            mName = name;
            mReturn = method.getReturnType();
            mParams = Arrays.asList(method.getParameterTypes());
            mHashCode = mName.hashCode() ^ mReturn.hashCode() ^ mParams.hashCode();
        }

        public Class getType() {
            return mType;
        }

        public Method getMethod() {
            return mMethod;
        }

        public String getName() {
            return mName;
        }

        public boolean returnTypeDiffers(MethodEntry methodEntry) {

            return ! (getMethod().getReturnType().isAssignableFrom(
                methodEntry.getMethod().getReturnType()) ||
                methodEntry.getMethod().getReturnType().isAssignableFrom(
                getMethod().getReturnType()));
            /*
            return getMethod().getReturnType() !=
                methodEntry.getMethod().getReturnType();
            */
        }

        public int hashCode() {
            return mHashCode;
        }

        public boolean equals(Object other) {
            if (!(other instanceof MethodEntry)) {
                return false;
            }
            MethodEntry methodEntry = (MethodEntry)other;
            return mName.equals(methodEntry.mName) &&
                mReturn.equals(methodEntry.mReturn) &&
                mParams.equals(methodEntry.mParams);
        }

        public String toString() {
            return mMethod.toString();
        }
    }
}
