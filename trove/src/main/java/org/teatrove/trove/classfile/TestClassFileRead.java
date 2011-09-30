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

import java.io.*;

/**
 * Reads a class file and prints out its contents. 
 *
 * @author Brian S O'Neill
 */
public class TestClassFileRead {
    /**
     * @param args first argument is path to class file.
     */
    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream(args[0]);
        in = new BufferedInputStream(in);

        ClassFileDataLoader loader = new ResourceClassFileDataLoader();

        ClassFile cf = ClassFile.readFrom(in, loader, null);
        in.close();

        while (cf.getOuterClass() != null) {
            cf = cf.getOuterClass();
        }

        dump(cf);
    }

    private static void dump(ClassFile cf) {
        dump(cf, "");
    }

    private static void dump(ClassFile cf, String indent) {
        println(cf, indent);
        println("className: " + cf.getClassName(), indent);
        println("superClassName: " + cf.getSuperClassName(), indent);
        println("innerClass: " + cf.isInnerClass(), indent);
        println("innerClassName: " + cf.getInnerClassName(), indent);
        println("type: " + cf.getType(), indent);
        println("modifiers: " + cf.getModifiers(), indent);

        String[] interfaces = cf.getInterfaces();
        print("interfaces: ", indent);
        for (int i=0; i<interfaces.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(interfaces[i]);
        }
        println();

        FieldInfo[] fields = cf.getFields();
        println("fields: ", indent);
        for (int i=0; i<fields.length; i++) {
            dump(fields[i], indent + "    ");
        }

        MethodInfo[] methods = cf.getMethods();
        println("methods: ", indent);
        for (int i=0; i<methods.length; i++) {
            dump(methods[i], indent + "    ");
        }

        methods = cf.getConstructors();
        println("constructors: ", indent);
        for (int i=0; i<methods.length; i++) {
            dump(methods[i], indent + "    ");
        }

        MethodInfo init = cf.getInitializer();
        println("initializer: ", indent);
        if (init != null) {
            dump(init, indent + "    ");
        }

        ClassFile[] innerClasses = cf.getInnerClasses();
        println("innerClasses: ", indent);
        for (int i=0; i<innerClasses.length; i++) {
            dump(innerClasses[i], indent + "    ");
        }

        println("sourceFile: " + cf.getSourceFile(), indent);
        println("synthetic: " + cf.isSynthetic(), indent);
        println("deprecated: " + cf.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(cf.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(FieldInfo field, String indent) {
        println(field, indent);
        println("name: " + field.getName(), indent);
        println("type: " + field.getType(), indent);
        println("modifiers: " + field.getModifiers(), indent);
        println("constantValue: " + field.getConstantValue(), indent);
        println("synthetic: " + field.isSynthetic(), indent);
        println("deprecated: " + field.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(field.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(MethodInfo method, String indent) {
        println(method, indent);
        println("name: " + method.getName(), indent);
        println("methodDescriptor: " + method.getMethodDescriptor(), indent);
        println("modifiers: " + method.getModifiers(), indent);

        String[] exceptions = method.getExceptions();
        print("exceptions: ", indent);
        for (int i=0; i<exceptions.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(exceptions[i]);
        }
        println();

        if (method.getCodeAttr() != null) {
            println("code:", indent);
            
            PrintWriter writer = new PrintWriter(System.out);
            
            TypeDesc[] paramTypes =
                method.getMethodDescriptor().getParameterTypes();
            boolean isStatic = method.getModifiers().isStatic();
            
            new CodeDisassembler(method).disassemble
                (new CodeAssemblerPrinter(paramTypes, isStatic,
                                          writer, indent + "    ", null));
            
            writer.flush();
        }

        println("synthetic: " + method.isSynthetic(), indent);
        println("deprecated: " + method.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(method.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(Attribute[] attributes, String indent) {
        if (attributes == null) {
            return;
        }
        for (int i=0; i<attributes.length; i++) {
            Attribute attribute = attributes[i];
            println(attribute, indent);
            Attribute[] subAttributes = attribute.getAttributes();
            if (subAttributes != null && subAttributes.length > 0) {
                println("attributes: ", indent);
                dump(subAttributes, indent + "    ");
            }
        }
    }

    private static void print(Object obj, String indent) {
        System.out.print(indent);
        System.out.print(obj);
    }

    private static void println(Object obj, String indent) {
        print(obj, indent);
        println();
    }

    private static void println() {
        System.out.println();
    }
}
