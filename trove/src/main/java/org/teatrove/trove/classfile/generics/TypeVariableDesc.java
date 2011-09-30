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

package org.teatrove.trove.classfile.generics;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.teatrove.trove.classfile.MethodDesc;
import org.teatrove.trove.classfile.TypeDesc;

/**
 * 
 *
 * @author Nick Hagan
 */
public class TypeVariableDesc
    extends AbstractGenericTypeDesc<TypeVariable<?>> {

    private final String name;
    private final GenericDeclarationDesc declaration;
    private GenericTypeDesc bounds;

    public static TypeVariableDesc forType(String name, String className) {
        return InternFactory.intern
        (
            new TypeVariableDesc(name, forDeclaration(className))
        );
    }

    public static TypeVariableDesc forType(String name, String className,
                                           GenericTypeDesc bounds) {
        return InternFactory.intern
        (
            new TypeVariableDesc(name, forDeclaration(className), bounds)
        );
    }

    public static TypeVariableDesc forType(String name, String className,
                                           String methodName,  MethodDesc method) {
        return InternFactory.intern
        (
            new TypeVariableDesc(name,
                                 forDeclaration(className, methodName, method))
        );
    }

    public static TypeVariableDesc forType(String name, String className,
                                           String methodName, MethodDesc method,
                                           GenericTypeDesc bounds) {
        return InternFactory.intern
        (
            new TypeVariableDesc(name,
                                 forDeclaration(className, methodName, method),
                                 bounds)
        );
    }

    public static TypeVariableDesc forType(TypeVariable<?> variable) {
        TypeVariableDesc desc = new TypeVariableDesc(variable);
        TypeVariableDesc var = InternFactory.intern(desc);
        if (desc == var) {
            // only resolve if this is the first access
            var.resolve(variable);
        }

        return var;
    }

    protected TypeVariableDesc(String name, GenericDeclarationDesc declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    protected TypeVariableDesc(String name, GenericDeclarationDesc declaration,
                               GenericTypeDesc bounds) {
        this.name = name;
        this.declaration = declaration;
        this.bounds = bounds;
    }

    @SuppressWarnings("rawtypes")
    protected TypeVariableDesc(TypeVariable<?> variable) {
        this.name = variable.getName();

        GenericDeclaration declaration = variable.getGenericDeclaration();
        if (declaration instanceof Class) {
            this.declaration = forDeclaration(((Class) declaration).getName());
        }
        else if (declaration instanceof Method) {
            Method method = (Method) declaration;

            // setup return type
            TypeDesc returnType = TypeDesc.forClass(method.getReturnType());

            // setup param types
            TypeDesc[] paramTypes = new TypeDesc[method.getParameterTypes().length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] =
                    TypeDesc.forClass(method.getParameterTypes()[i]);
            }

            // setup desc
            MethodDesc methodDesc =
                MethodDesc.forArguments(returnType, paramTypes);

            // create declaration
            this.declaration =
                forDeclaration(method.getDeclaringClass().getName(),
                               method.getName(), methodDesc);
        }
        else if (declaration instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) declaration;

            // setup param types
            TypeDesc[] paramTypes = new TypeDesc[ctor.getParameterTypes().length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramTypes[i] = TypeDesc.forClass(ctor.getParameterTypes()[i]);
            }

            // setup desc
            MethodDesc methodDesc =
                MethodDesc.forArguments(TypeDesc.VOID, paramTypes);

            // create declaration
            this.declaration =
                forDeclaration(ctor.getDeclaringClass().getName(),
                               "<clinit>", methodDesc);
        }
        else {
            throw new IllegalStateException("Unknown");
        }
    }

    public String getName() {
        return this.name;
    }

    public GenericDeclarationDesc getDeclaration() {
        return this.declaration;
    }

    public GenericTypeDesc getBounds() {
        return this.bounds;
    }

    public String getSignature() {
        return "T" + this.name + ';';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (!(other instanceof TypeVariableDesc)) { return false; }

        TypeVariableDesc var = (TypeVariableDesc) other;
        return this.name.equals(var.name) &&
               this.declaration.equals(var.declaration);
    }

    @Override
    public int hashCode() {
        return (this.name.hashCode() * 11) + (this.declaration.hashCode() * 17);
    }

    @Override
    public String toString() {
        // NOTE: because the bounds can contain this same type, avoid endloop
        // recursion and only print name
        return this.name; //  + " extends " + this.bounds;
    }

    @Override
    protected void resolve(TypeVariable<?> variable) {
        Type[] bounded = variable.getBounds();
        if (bounded != null && bounded.length > 0) {
            this.bounds = GenericTypeFactory.fromType(bounded[0]);
        }
    }

    protected static GenericDeclarationDesc forDeclaration(String className) {
        return ClassDeclarationDesc.forClass(className);
    }

    protected static GenericDeclarationDesc forDeclaration(String className,
                                                           String methodName,
                                                           MethodDesc method) {
        return MethodDeclarationDesc.forMethod
        (
            MethodInfo.forMethod(className, methodName, method)
        );
    }

    protected static class MethodInfo {
        private final String className;
        private final String methodName;
        private final MethodDesc method;

        public static MethodInfo forMethod(String className, String methodName,
                                           MethodDesc method) {
            return InternFactory.intern
            (
                new MethodInfo(className, methodName, method)
            );
        }

        protected MethodInfo(String className, String methodName, MethodDesc method) {
            this.className = className;
            this.methodName = methodName;
            this.method = method;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public MethodDesc getMethod() {
            return this.method;
        }

        @Override
        public int hashCode() {
            return (this.className.hashCode() * 17) +
                   (this.methodName.hashCode() * 23) +
                   (this.method.hashCode() * 27);
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) { return true; }
            else if (!(object instanceof MethodInfo)) { return false; }

            MethodInfo info = (MethodInfo) object;
            return this.className.equals(info.className) &&
                   this.methodName.equals(info.methodName) &&
                   this.method.equals(info.method);
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder(256);
            buffer.append(this.method.getReturnType().getFullName())
                  .append(' ').append(this.className).append(':')
                  .append(this.methodName).append('(');

            TypeDesc[] params = this.method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) { buffer.append(", "); }
                buffer.append(params[i].getFullName());
            }

            return buffer.append(')').toString();
        }
    }

    protected static interface GenericDeclarationDesc {
        // marker interfacefo
    }

    protected static class ClassDeclarationDesc
        implements GenericDeclarationDesc {

        private final String className;

        public static ClassDeclarationDesc forClass(String className) {
            return InternFactory.intern(new ClassDeclarationDesc(className));
        }

        protected ClassDeclarationDesc(String className) {
            this.className = className;
        }

        public String getClassName() {
            return this.className;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) { return true; }
            else if (!(object instanceof ClassDeclarationDesc)) { return false; }

            ClassDeclarationDesc other = (ClassDeclarationDesc) object;
            return this.getClassName().equals(other.getClassName());
        }

        @Override
        public int hashCode() {
            return this.className.hashCode();
        }

        @Override
        public String toString() {
            return this.className;
        }
    }

    protected static class MethodDeclarationDesc
        implements GenericDeclarationDesc {

        private final MethodInfo method;

        public static MethodDeclarationDesc forMethod(MethodInfo method) {
            return InternFactory.intern(new MethodDeclarationDesc(method));
        }

        protected MethodDeclarationDesc(MethodInfo method) {
            this.method = method;
        }

        public MethodInfo getMethod() {
            return this.method;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) { return true; }
            else if (!(object instanceof MethodDeclarationDesc)) { return false; }

            MethodDeclarationDesc other = (MethodDeclarationDesc) object;
            return this.getMethod().equals(other.getMethod());
        }

        @Override
        public int hashCode() {
            return this.method.hashCode();
        }

        @Override
        public String toString() {
            return this.method.toString();
        }
    }
}
