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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teatrove.trove.classfile.generics.GenericTypeFactory;
import org.teatrove.trove.classfile.generics.TypeVariableDesc;

/**
 * A class used to create Java class files. Call the writeTo method
 * to produce a class file.
 *
 * <p>See <i>The Java Virtual Machine Specification</i> (ISBN 0-201-63452-X)
 * for information on how class files are structured. Section 4.1 describes
 * the ClassFile structure.
 * 
 * @author Brian S O'Neill, Nick Hagan
 */
public class ClassFile {
    private static final int MAGIC = 0xCAFEBABE;
    private static final int JDK1_1_MAJOR_VERSION = 50;
    private static final int JDK1_1_MINOR_VERSION = 0;

    private int mMajorVersion = JDK1_1_MAJOR_VERSION;
    private int mMinorVersion = JDK1_1_MINOR_VERSION;

    private final String mClassName;
    private final String mSuperClassName;
    private String mInnerClassName;
    private TypeDesc mType;

    private ConstantPool mCp;

    private Modifiers mModifiers;

    private ConstantClassInfo mThisClass;
    private ConstantClassInfo mSuperClass;

    // Holds ConstantInfo objects.
    private List<ConstantClassInfo> mInterfaces = 
        new ArrayList<ConstantClassInfo>(2);
    private Set<String> mInterfaceSet = new HashSet<String>(7);

    // Holds objects.
    private List<FieldInfo> mFields = new ArrayList<FieldInfo>();
    private List<MethodInfo> mMethods = new ArrayList<MethodInfo>();
    private List<Attribute> mAttributes = new ArrayList<Attribute>();

    private SourceFileAttr mSource;

    private List<ClassFile> mInnerClasses;
    private int mAnonymousInnerClassCount = 0;
    private InnerClassesAttr mInnerClassesAttr;

    // Is non-null for inner classes.
    private ClassFile mOuterClass;

    // List of superclass and interface instances
    private Set<Class<?>> mParentClasses = new HashSet<Class<?>>();
    
    /**
     * By default, the ClassFile defines public, non-final, concrete classes.
     * This constructor creates a ClassFile for a class that extends
     * java.lang.Object.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     */
    public ClassFile(String className) {
        this(className, (String)null);
    }

    /**
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClass Super class.
     */
    public ClassFile(String className, Class<?> superClass) {
        this(className, superClass.getName());
        this.mParentClasses.add(superClass);
    }

    /**
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClassName Full super class name.
     */
    public ClassFile(String className, String superClassName) {
        if (superClassName == null) {
            if (!className.equals(Object.class.getName())) {
                superClassName = Object.class.getName();
            }
        }

        mCp = new ConstantPool();

        // public, non-final, concrete class
        mModifiers = new Modifiers(Modifier.PUBLIC);

        mThisClass = ConstantClassInfo.make(mCp, className);
        mSuperClass = ConstantClassInfo.make(mCp, superClassName);

        mClassName = className;
        mSuperClassName = superClassName;
    }

    /**
     * Used to construct a ClassFile when read from a stream.
     */
    private ClassFile(ConstantPool cp, Modifiers modifiers,
                      ConstantClassInfo thisClass,
                      ConstantClassInfo superClass,
                      ClassFile outerClass) {

        mCp = cp;

        mModifiers = modifiers;

        mThisClass = thisClass;
        mSuperClass = superClass;

        mClassName = thisClass.getType().getRootName();
        if (superClass == null) {
            mSuperClassName = null;
        }
        else {
            mSuperClassName = superClass.getType().getRootName();
        }

        mOuterClass = outerClass;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getSuperClassName() {
        return mSuperClassName;
    }

    /**
     * Returns a TypeDesc for the type of this ClassFile.
     */
    public TypeDesc getType() {
        if (mType == null) {
            mType = TypeDesc.forClass(mClassName);
        }
        return mType;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    /**
     * Returns the names of all the interfaces that this class implements.
     */
    public String[] getInterfaces() {
        int size = mInterfaces.size();
        String[] names = new String[size];

        for (int i=0; i<size; i++) {
            names[i] = mInterfaces.get(i)
                .getType().getRootName();
        }

        return names;
    }

    /**
     * Returns all the fields defined in this class.
     */
    public FieldInfo[] getFields() {
        FieldInfo[] fields = new FieldInfo[mFields.size()];
        return mFields.toArray(fields);
    }

    /**
     * Returns all the methods defined in this class, not including
     * constructors and static initializers.
     */
    public MethodInfo[] getMethods() {
        int size = mMethods.size();
        List<MethodInfo> methodsOnly = new ArrayList<MethodInfo>(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = mMethods.get(i);
            String name = method.getName();
            if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
                methodsOnly.add(method);
            }
        }

        MethodInfo[] methodsArray = new MethodInfo[methodsOnly.size()];
        return methodsOnly.toArray(methodsArray);
    }

    /**
     * Returns all the constructors defined in this class.
     */
    public MethodInfo[] getConstructors() {
        int size = mMethods.size();
        List<MethodInfo> ctorsOnly = new ArrayList<MethodInfo>(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = mMethods.get(i);
            if ("<init>".equals(method.getName())) {
                ctorsOnly.add(method);
            }
        }

        MethodInfo[] ctorsArray = new MethodInfo[ctorsOnly.size()];
        return ctorsOnly.toArray(ctorsArray);
    }

    /**
     * Returns the static initializer defined in this class or null if there
     * isn't one.
     */
    public MethodInfo getInitializer() {
        int size = mMethods.size();

        for (int i=0; i<size; i++) {
            MethodInfo method = mMethods.get(i);
            if ("<clinit>".equals(method.getName())) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns all the inner classes defined in this class. If no inner classes
     * are defined, then an array of length zero is returned.
     */
    public ClassFile[] getInnerClasses() {
        if (mInnerClasses == null) {
            return new ClassFile[0];
        }

        ClassFile[] innerClasses = new ClassFile[mInnerClasses.size()];
        return mInnerClasses.toArray(innerClasses);
    }

    /**
     * Returns true if this ClassFile represents an inner class.
     */
    public boolean isInnerClass() {
        return mOuterClass != null;
    }

    /**
     * If this ClassFile represents a non-anonymous inner class, returns its
     * short inner class name.
     */
    public String getInnerClassName() {
        return mInnerClassName;
    }

    /**
     * Returns null if this ClassFile does not represent an inner class.
     *
     * @see #isInnerClass()
     */
    public ClassFile getOuterClass() {
        return mOuterClass;
    }

    /**
     * Returns a value indicating how deeply nested an inner class is with
     * respect to its outermost enclosing class. For top level classes, 0
     * is returned. For first level inner classes, 1 is returned, etc.
     */
    public int getClassDepth() {
        int depth = 0;

        ClassFile outer = mOuterClass;
        while (outer != null) {
            depth++;
            outer = outer.mOuterClass;
        }

        return depth;
    }

    /**
     * Returns the source file of this class file or null if not set.
     */
    public String getSourceFile() {
        if (mSource == null) {
            return null;
        }
        else {
            return mSource.getFileName();
        }
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides access to the ClassFile's ContantPool.
     *
     * @return The constant pool for this class file.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }

    /**
     * Add an interface that this class implements.
     *
     * @param interfaceName Full interface name.
     */
    public void addInterface(String interfaceName) {
        if (!mInterfaceSet.contains(interfaceName)) {
            mInterfaces.add(ConstantClassInfo.make(mCp, interfaceName));
            mInterfaceSet.add(interfaceName);
        }
    }

    /**
     * Add an interface that this class implements.
     */
    public void addInterface(Class<?> iface) {
        addInterface(iface.getName());
        this.mParentClasses.add(iface);
    }

    /**
     * Add a field to this class.
     */
    public FieldInfo addField(Modifiers modifiers,
                              String fieldName,
                              TypeDesc type) {
        FieldInfo fi = new FieldInfo(this, modifiers, fieldName, type);
        mFields.add(fi);
        return fi;
    }

    /**
     * Add a method to this class.
     *
     * @param ret Is null if method returns void.
     * @param params May be null if method accepts no parameters.
     */
    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc... params) {
        return addMethod(modifiers, methodName, null, ret, params);
    }

    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                TypeVariableDesc[] typeParams,
                                TypeDesc ret,
                                TypeDesc[] params) {
        MethodDesc md = MethodDesc.forArguments(ret, params);
        SignatureDesc sd = SignatureDesc.forMethod(typeParams, ret, params);
        return addMethod(modifiers, methodName, md, sd);
    }

    /**
     * Add a method to this class.
     *
     * @param ret Is null if method returns void.
     * @param params May be null if method accepts no parameters.
     */
    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params,
                                String[] paramNames) {
        return addMethod(modifiers, methodName, null, ret, params, paramNames);
    }

    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                TypeVariableDesc[] typeParams,
                                TypeDesc ret,
                                TypeDesc[] params,
                                String[] paramNames) {
        MethodDesc md = MethodDesc.forArguments(ret, params, paramNames);
        SignatureDesc sd = SignatureDesc.forMethod(typeParams, ret, params);
        return addMethod(modifiers, methodName, md, sd);
    }

    /**
     * Add a method to this class.
     */
    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                MethodDesc md,
                                SignatureDesc sd) {
        MethodInfo mi = new MethodInfo(this, modifiers, methodName, md, sd);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add a method to this class. This method is handy for implementing
     * methods defined by a pre-existing interface.
     */
    public MethodInfo addMethod(Method method) {
        Modifiers modifiers = new Modifiers(method.getModifiers());
        modifiers.setAbstract(this.getModifiers().isInterface());

        TypeVariableDesc[] typeParams = lookupTypeVariables(method);
        TypeDesc ret = TypeDesc.forClass(method.getReturnType(),
                                         method.getGenericReturnType());

        MethodDescriptor methodDescriptor = lookupMethodDescriptor(method);

        Class<?>[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        String[] paramNames = new String[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
            if(methodDescriptor != null) {
                paramNames[i] =
                    methodDescriptor.getParameterDescriptors()[i].getName();
            } else {
                paramNames[i] = "param$" + i;
            }
        }

        MethodInfo mi = addMethod(modifiers, method.getName(), typeParams,
                                  ret, params, paramNames);

        // exception stuff...
        // TODO: generic exceptions
        Class<?>[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        return mi;
    }
    
    /**
     * Add a method to this class. This method is handy for implementing
     * methods defined by a pre-existing interface.
     */
    public MethodInfo addMethod(Method method, Class<?> returnType,
                                Class<?>... paramClasses) {
        
        Modifiers modifiers = new Modifiers(method.getModifiers());
        modifiers.setAbstract(this.getModifiers().isInterface());

        TypeVariableDesc[] typeParams = lookupTypeVariables(method);
        TypeDesc ret = TypeDesc.forClass(returnType,
                                         method.getGenericReturnType());

        MethodDescriptor methodDescriptor = lookupMethodDescriptor(method);

        Type[] paramTypes = method.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        String[] paramNames = new String[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
            if(methodDescriptor != null) {
                paramNames[i] =
                    methodDescriptor.getParameterDescriptors()[i].getName();
            } else {
                paramNames[i] = "param$" + i;
            }
        }

        MethodInfo mi = addMethod(modifiers, method.getName(), typeParams,
                                  ret, params, paramNames);

        // exception stuff...
        // TODO: generic exceptions
        Class<?>[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        return mi;
    }
    
    private TypeVariableDesc[] lookupTypeVariables(Method method) {
        Map<String, TypeVariableDesc> args =
            new LinkedHashMap<String, TypeVariableDesc>();

        
        // TODO: better handle this by reading each return type and param
        // type and analyzing for any type variables and attempting to resolve
        // said type variables into declaring class type variables by looking
        // up actual tree for the proper root type (ie: GenericType.getRaw):
        //
        // per ret type and param
        // loop through per type variable
            // if type variable declaring class is this class, do nothing
            // else if declaring class is parent
                // per type var of active class
                    // per interface/super
                        // if contains same type var
                            // get class def and associated type var index
                            // if class equals declaring class
                                // if type matches, use type var of initial var
                            // else keep walking
                // if found, use that type
                // else, declare
        

        
        // check if declaring class of method within immediate hiearchy
        // and assume class file type parameters match (see TODO above on how
        // this really should work)
        boolean valid = false;
        Class<?> declaringClass = method.getDeclaringClass();
        for (Class<?> clazz : mParentClasses) {
            if (declaringClass.isAssignableFrom(clazz)) {
                valid = true;
                break;
            }
        }
        
        // pull in class instances first if not in hiearchy
        if (!valid) {
            TypeVariable<?>[] cargs = declaringClass.getTypeParameters();
            for (TypeVariable<?> carg : cargs) {
                args.put(carg.getName(),
                         (TypeVariableDesc) GenericTypeFactory.fromType(carg));
            }
        }

        // pull in method instances overriding class level
        TypeVariable<?>[] cargs = method.getTypeParameters();
        for (TypeVariable<?> carg : cargs) {
            args.remove(carg.getName());
            args.put(carg.getName(),
                     (TypeVariableDesc) GenericTypeFactory.fromType(carg));
        }

        // return array
        return args.values().toArray(new TypeVariableDesc[args.size()]);
    }

    private static MethodDescriptor lookupMethodDescriptor(Method method) {
        MethodDescriptor methodDescriptor = null;
        try {
            for (MethodDescriptor methodDescriptor1 : Introspector.getBeanInfo(method.getDeclaringClass()).getMethodDescriptors()) {
                if(methodDescriptor1.getMethod() == method) {
                    methodDescriptor = methodDescriptor1;
                    break;
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException("Unable to find MethodDescriptor for method "+method, e);
        }

//        if(methodDescriptor == null) {
//            throw new RuntimeException("Unable to find MethodDescriptor for method "+method);
//        }
        return methodDescriptor;
    }

    /**
     * Add a constructor to this class.
     *
     * @param params May be null if constructor accepts no parameters.
     */
    public MethodInfo addConstructor(Modifiers modifiers,
                                     TypeDesc... params) {
        String[] paramNames = MethodDesc.createGenericParameterNames(params);
        return addConstructor(modifiers, params, paramNames);
    }

    /**
     * Add a constructor to this class.
     *
     * @param params May be null if constructor accepts no parameters.
     * @param paramNames
     */
    public MethodInfo addConstructor(Modifiers modifiers,
                                     TypeDesc[] params,
                                     String[] paramNames) {
        MethodDesc md = MethodDesc.forArguments(null, params, paramNames);
        MethodInfo mi = new MethodInfo(this, modifiers, "<init>", md, null);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Adds a public, no-arg constructor with the code buffer properly defined.
     */
    public MethodInfo addDefaultConstructor() {
        Modifiers modifiers = new Modifiers();
        modifiers.setPublic(true);
        MethodInfo mi = addConstructor(modifiers, null, null);
        CodeBuilder builder = new CodeBuilder(mi);
        builder.loadThis();
        builder.invokeSuperConstructor();
        builder.returnVoid();
        return mi;
    }

    /**
     * Add a static initializer to this class.
     */
    public MethodInfo addInitializer() {
        MethodDesc md = MethodDesc.forArguments(null, null, null);
        Modifiers af = new Modifiers();
        af.setStatic(true);
        MethodInfo mi = new MethodInfo(this, af, "<clinit>", md, null);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     */
    public ClassFile addInnerClass(String innerClassName) {
        return addInnerClass(innerClassName, (String)null);
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClass Super class.
     */
    public ClassFile addInnerClass(String innerClassName, Class<?> superClass) {
        return addInnerClass(innerClassName, superClass.getName());
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClassName Full super class name.
     */
    public ClassFile addInnerClass(String innerClassName,
                                   String superClassName) {
        String fullInnerClassName;
        if (innerClassName == null) {
            fullInnerClassName =
                mClassName + '$' + (++mAnonymousInnerClassCount);
        }
        else {
            fullInnerClassName = mClassName + '$' + innerClassName;
        }

        ClassFile inner = new ClassFile(fullInnerClassName, superClassName);
        Modifiers access = inner.getModifiers();
        access.setPrivate(true);
        access.setStatic(true);
        inner.mInnerClassName = innerClassName;
        inner.mOuterClass = this;

        if (mInnerClasses == null) {
            mInnerClasses = new ArrayList<ClassFile>();
        }

        mInnerClasses.add(inner);

        // Record the inner class in this, the outer class.
        if (mInnerClassesAttr == null) {
            addAttribute(new InnerClassesAttr(mCp));
        }

        mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName,
                                        innerClassName, access);

        // Record the inner class in itself.
        inner.addAttribute(new InnerClassesAttr(inner.getConstantPool()));
        inner.mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName,
                                              innerClassName, access);

        return inner;
    }

    /**
     * Set the source file of this class file by adding a source file
     * attribute. The source doesn't actually have to be a file,
     * but the virtual machine spec names the attribute "SourceFile_attribute".
     */
    public void setSourceFile(String fileName) {
        addAttribute(new SourceFileAttr(mCp, fileName));
    }

    /**
     * Set the signature of this class file to include generics info per JDK 5.
     */
    public void setSignature(String signature) {
        addAttribute(new SignatureAttr(mCp, signature));
    }

    /**
     * Mark this class as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this class as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    /**
     * Add an attribute to this class.
     */
    public void addAttribute(Attribute attr) {
        if (attr instanceof SourceFileAttr) {
            if (mSource != null) {
                mAttributes.remove(mSource);
            }
            mSource = (SourceFileAttr)attr;
        }
        else if (attr instanceof InnerClassesAttr) {
            if (mInnerClassesAttr != null) {
                mAttributes.remove(mInnerClassesAttr);
            }
            mInnerClassesAttr = (InnerClassesAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        Attribute[] attrs = new Attribute[mAttributes.size()];
        return mAttributes.toArray(attrs);
    }

    /**
     * Sets the version to use when writing the generated ClassFile. Currently,
     * only version 45, 3 is supported, and is set by default.
     *
     * @exception IllegalArgumentException when the version isn't supported
     */
    public void setVersion(int major, int minor)
        throws IllegalArgumentException {

        if (major != JDK1_1_MAJOR_VERSION ||
            minor != JDK1_1_MINOR_VERSION) {

            throw new IllegalArgumentException("Version " + major + ", " +
                                               minor + " is not supported");
        }

        mMajorVersion = major;
        mMinorVersion = minor;
    }


    /**
     * Returns all the runtime invisible annotations defined for this class
     * file, or an empty array if none.
     */
    public Annotation[] getRuntimeInvisibleAnnotations() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof RuntimeInvisibleAnnotationsAttr) {
                return ((AnnotationsAttr) attr).getAnnotations();
            }
        }
        return new Annotation[0];
    }

    /**
     * Returns all the runtime visible annotations defined for this class file,
     * or an empty array if none.
     */
    public Annotation[] getRuntimeVisibleAnnotations() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute attr = mAttributes.get(i);
            if (attr instanceof RuntimeVisibleAnnotationsAttr) {
                return ((AnnotationsAttr) attr).getAnnotations();
            }
        }
        return new Annotation[0];
    }

    /**
     * Add a runtime invisible annotation.
     */
    public Annotation addRuntimeInvisibleAnnotation(TypeDesc type) {
        AnnotationsAttr attr = null;
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute a = mAttributes.get(i);
            if (a instanceof RuntimeInvisibleAnnotationsAttr) {
                attr = (AnnotationsAttr) a;
            }
        }
        if (attr == null) {
            attr = new RuntimeInvisibleAnnotationsAttr(mCp);
            addAttribute(attr);
        }
        Annotation ann = new Annotation(mCp);
        ann.setType(type);
        attr.addAnnotation(ann);
        return ann;
    }

    /**
     * Add a runtime visible annotation.
     */
    public Annotation addRuntimeVisibleAnnotation(TypeDesc type) {
        AnnotationsAttr attr = null;
        for (int i = mAttributes.size(); --i >= 0; ) {
            Attribute a = mAttributes.get(i);
            if (a instanceof RuntimeVisibleAnnotationsAttr) {
                attr = (AnnotationsAttr) a;
            }
        }
        if (attr == null) {
            attr = new RuntimeVisibleAnnotationsAttr(mCp);
            addAttribute(attr);
        }
        Annotation ann = new Annotation(mCp);
        ann.setType(type);
        attr.addAnnotation(ann);
        return ann;
    }

    /**
     * Writes the ClassFile to the given OutputStream. When finished, the
     * stream is flushed, but not closed.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (!(out instanceof DataOutput)) {
            out = new DataOutputStream(out);
        }

        writeTo((DataOutput)out);

        out.flush();
    }

    /**
     * Writes the ClassFile to the given DataOutput.
     */
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeInt(MAGIC);
        dout.writeShort(mMinorVersion);
        dout.writeShort(mMajorVersion);

        mCp.writeTo(dout);

        int modifier = mModifiers.getModifier();
        if (!mModifiers.isInterface()) {
            // Set the ACC_SUPER flag for classes only.
            // NOTE: we use SYNCHRONIZED which is the same value as
            //       ACC_SUPER, but Java does not have a constant field exposed
            //       for it, so we use SYNCHRONIZED instead
            modifier |= Modifier.SYNCHRONIZED;
        }
        
        dout.writeShort(modifier);

        dout.writeShort(mThisClass.getIndex());
        if (mSuperClass != null) {
            dout.writeShort(mSuperClass.getIndex());
        }
        else {
            dout.writeShort(0);
        }

        int size = mInterfaces.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Interfaces count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            int index = mInterfaces.get(i).getIndex();
            dout.writeShort(index);
        }

        size = mFields.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Field count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            FieldInfo field = mFields.get(i);
            field.writeTo(dout);
        }

        size = mMethods.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Method count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            MethodInfo method = mMethods.get(i);
            method.writeTo(dout);
        }

        size = mAttributes.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Attribute count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    /**
     * Reads a ClassFile from the given InputStream. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param in source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in) throws IOException {
        return readFrom(in, null, null);
    }

    /**
     * Reads a ClassFile from the given DataInput. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param din source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din) throws IOException {
        return readFrom(din, null, null);
    }

    /**
     * Reads a ClassFile from the given InputStream. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param in source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }
        return readFrom((DataInput)in, loader, attrFactory);
    }

    /**
     * Reads a ClassFile from the given DataInput. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param din source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        return readFrom(din, loader, attrFactory, new HashMap<String, ClassFile>(11), null);
    }

    /**
     * @param loadedClassFiles Maps name to ClassFiles for classes already
     * loaded. This prevents infinite loop: inner loads outer loads inner...
     */
    private static ClassFile readFrom(DataInput din,
                                      ClassFileDataLoader loader,
                                      AttributeFactory attrFactory,
                                      Map<String, ClassFile> loadedClassFiles,
                                      ClassFile outerClass)
        throws IOException
    {
        int magic = din.readInt();
        if (magic != MAGIC) {
            throw new IOException("Incorrect magic number: 0x" +
                                  Integer.toHexString(magic));
        }

        /*int minor =*/ din.readUnsignedShort();
        /*
        if (minor != JDK1_1_MINOR_VERSION) {
            throw new IOException("Minor version " + minor +
                                  " not supported, version " +
                                  JDK1_1_MINOR_VERSION + " is.");
        }
        */

        /*int major =*/ din.readUnsignedShort();
        /*
        if (major != JDK1_1_MAJOR_VERSION) {
            throw new IOException("Major version " + major +
                                  "not supported, version " +
                                  JDK1_1_MAJOR_VERSION + " is.");
        }
        */

        ConstantPool cp = ConstantPool.readFrom(din);
        Modifiers modifiers = new Modifiers(din.readUnsignedShort());
        modifiers.setSynchronized(false);

        int index = din.readUnsignedShort();
        ConstantClassInfo thisClass = (ConstantClassInfo)cp.getConstant(index);

        index = din.readUnsignedShort();
        ConstantClassInfo superClass = null;
        if (index > 0) {
            superClass = (ConstantClassInfo)cp.getConstant(index);
        }

        ClassFile cf =
            new ClassFile(cp, modifiers, thisClass, superClass, outerClass);
        loadedClassFiles.put(cf.getClassName(), cf);

        // Read interfaces.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            index = din.readUnsignedShort();
            ConstantClassInfo info = (ConstantClassInfo)cp.getConstant(index);
            cf.addInterface(info.getType().getRootName());
        }

        // Read fields.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mFields.add(FieldInfo.readFrom(cf, din, attrFactory));
        }

        // Read methods.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mMethods.add(MethodInfo.readFrom(cf, din, attrFactory));
        }

        // Read attributes.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            Attribute attr = Attribute.readFrom(cp, din, attrFactory);
            cf.addAttribute(attr);
            if (attr instanceof InnerClassesAttr) {
                cf.mInnerClassesAttr = (InnerClassesAttr)attr;
            }
        }

        // Load inner and outer classes.
        if (cf.mInnerClassesAttr != null && loader != null) {
            InnerClassesAttr.Info[] infos =
                cf.mInnerClassesAttr.getInnerClassesInfo();
            for (int i=0; i<infos.length; i++) {
                InnerClassesAttr.Info info = infos[i];

                if (thisClass.equals(info.getInnerClass())) {
                    // This class is an inner class.
                    if (info.getInnerClassName() != null) {
                        cf.mInnerClassName = info.getInnerClassName();
                    }
                    ConstantClassInfo outer = info.getOuterClass();
                    if (cf.mOuterClass == null && outer != null) {
                        cf.mOuterClass = readOuterClass
                            (outer, loader, attrFactory, loadedClassFiles);
                    }
                    Modifiers innerFlags = info.getModifiers();
                    modifiers.setStatic(innerFlags.isStatic());
                    modifiers.setPrivate(innerFlags.isPrivate());
                    modifiers.setProtected(innerFlags.isProtected());
                    modifiers.setPublic(innerFlags.isPublic());
                }
                else if (thisClass.equals(info.getOuterClass())) {
                    // This class is an outer class.
                    ConstantClassInfo inner = info.getInnerClass();
                    if (inner != null) {
                        ClassFile innerClass = readInnerClass
                            (inner, loader, attrFactory, loadedClassFiles, cf);

                        if (innerClass != null) {
                            if (innerClass.getInnerClassName() == null) {
                                innerClass.mInnerClassName =
                                    info.getInnerClassName();
                            }
                            if (cf.mInnerClasses == null) {
                                cf.mInnerClasses = new ArrayList<ClassFile>();
                            }
                            cf.mInnerClasses.add(innerClass);
                        }
                    }
                }
            }
        }

        return cf;
    }

    private static ClassFile readOuterClass(ConstantClassInfo outer,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map<String, ClassFile> loadedClassFiles)
        throws IOException
    {
        String name = outer.getType().getRootName();

        ClassFile outerClass = loadedClassFiles.get(name);
        if (outerClass != null) {
            return outerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, null);
    }

    private static ClassFile readInnerClass(ConstantClassInfo inner,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map<String, ClassFile> loadedClassFiles,
                                            ClassFile outerClass)
        throws IOException
    {
        String name = inner.getType().getRootName();

        ClassFile innerClass = loadedClassFiles.get(name);
        if (innerClass != null) {
            return innerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, outerClass);
    }
}
