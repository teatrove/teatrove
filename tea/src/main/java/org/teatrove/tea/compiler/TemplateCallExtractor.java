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

package org.teatrove.tea.compiler;

import java.lang.reflect.Method;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.classfile.CodeDisassembler;
import org.teatrove.trove.classfile.CodeAssemblerPrinter;

/**
 * 
 * @author Guy A. Molinari
 * 
 * A utility class to facilitate generation of template metadata.
 */
public class TemplateCallExtractor {

    public static final String TEMPLATE_PACKAGE = "org.teatrove.teaservlet.template.";


    /**
     * Find the execute method on the class file input stream.
     */
    public static MethodInfo getTemplateExecuteMethod(InputStream in) throws IOException {

        ClassFile classFile = ClassFile.readFrom(in);

        MethodInfo executeMethod = null;
        MethodInfo[] all = classFile.getMethods();

        // find the execute method on this template
        for (int i = 0; i < all.length; i++) {
            if (JavaClassGenerator.EXECUTE_METHOD_NAME.equals(
                all[i].getName()) && all[i].getModifiers().isStatic()) {
                executeMethod = all[i];
                break;
            }
        }
        in.close();
        return executeMethod;
    }

    /**
     * Find the substitute method on the class file input stream.
     */
    private static MethodInfo getTemplateSubstituteMethod(InputStream in) throws IOException {

        ClassFile classFile = ClassFile.readFrom(in);

        MethodInfo substituteMethod = null;
        MethodInfo[] all = classFile.getMethods();

        // find the execute method on this template
        for (int i = 0; i < all.length; i++) {

            if ("substitute".equals(
                all[i].getName()) &&
                all[i].getMethodDescriptor().getParameterCount() == 1) {
                substituteMethod = all[i];
                break;
            }
        }
        in.close();
        return substituteMethod;
    }

    /**
     * Get the names of all templates called within a template.
     */
    public static String[] getTemplatesCalled(String basePath, String templateName) {

        final HashMap<String, String> templatesCalledMap =
            new HashMap<String, String>();

        try {
            File templatePath = new File(new File(basePath),
                templateName.replace('.', '/') + ".class");
            if (!templatePath.exists() &&
                    templateName.startsWith(TEMPLATE_PACKAGE)) {
                templatePath = new File(new File(basePath),
                    templateName.substring(TEMPLATE_PACKAGE.length()).
                    replace('.','/') + ".class");
            }


            MethodInfo executeMethod = getTemplateExecuteMethod(new FileInputStream(templatePath));

            // Search for embedded execute methods.
            CodeDisassembler cd = new CodeDisassembler(executeMethod);
            cd.disassemble(new CodeAssemblerPrinter(executeMethod
                .getMethodDescriptor().getParameterTypes(), true, null) {

                 public void invokeStatic(String className, String methodName,
                     TypeDesc ret, TypeDesc[] params)  {
                     if (JavaClassGenerator.EXECUTE_METHOD_NAME.equals(methodName))
                         templatesCalledMap.put(className.replace('.', '/'), className);
                 }

                 public void println(String s) { } // Do nothing
            });

            MethodInfo substituteMethod = getTemplateSubstituteMethod(new FileInputStream(templatePath));

            if (substituteMethod == null)
                return (String[]) templatesCalledMap.keySet().toArray(
                    new String[templatesCalledMap.keySet().size()]);

            // Search for embedded substitute methods.
            cd = new CodeDisassembler(substituteMethod);
            cd.disassemble(new CodeAssemblerPrinter(substituteMethod
                .getMethodDescriptor().getParameterTypes(), true, null) {

                 public void invokeStatic(String className, String methodName,
                     TypeDesc ret, TypeDesc[] params)  {
                     if (JavaClassGenerator.EXECUTE_METHOD_NAME.equals(methodName))
                         templatesCalledMap.put(className.replace('.', '/'), className);
                 }

                 public void println(String s) { } // Do nothing
            });
        }
        catch (IOException ix) {
            return new String[0];
        }

        return (String[]) templatesCalledMap.keySet().toArray(
            new String[templatesCalledMap.keySet().size()]);
    }

    /**
     * Get the names of all application methods called within a template.
     */
    public static AppMethodInfo[] getAppMethodsCalled(String basePath, final String templateName,
            final String contextClass) {

        final HashMap<AppMethodInfo, AppMethodInfo> methodsCalledMap =
            new HashMap<AppMethodInfo, AppMethodInfo>();

        try {
            File templatePath = new File(new File(basePath),
                templateName.replace('.', '/') + ".class");
            if (!templatePath.exists() &&
                    templateName.startsWith(TEMPLATE_PACKAGE)) {
                templatePath = new File(new File(basePath),
                    templateName.substring(TEMPLATE_PACKAGE.length()).
                    replace('.','/') + ".class");
            }

            final MethodInfo executeMethod = getTemplateExecuteMethod(new FileInputStream(templatePath));

            CodeDisassembler cd = new CodeDisassembler(executeMethod);
            cd.disassemble(new CodeAssemblerPrinter(executeMethod
                .getMethodDescriptor().getParameterTypes(), true, null) {

                 public void invokeVirtual(String className, String methodName,
                         TypeDesc ret, TypeDesc[] params)  {
                     if ("print".equals(methodName) ||
                             "toString".equals(methodName) ||
                             "equals".equals(methodName) ||
                             (contextClass.indexOf(className) == -1 &&
                             className.indexOf("MergedClass") == -1))
                         return;

                     AppMethodInfo ami = new AppMethodInfo(methodName, params);
                     if (! methodsCalledMap.containsKey(ami))
                         methodsCalledMap.put(ami, ami);
                     else
                         methodsCalledMap.get(ami).incCallCount();
                 }

                 public void println(String s) { } // Do nothing
            });

            final MethodInfo substituteMethod = getTemplateSubstituteMethod(new FileInputStream(templatePath));

            if (substituteMethod == null)
                return methodsCalledMap.values().toArray(
                    new AppMethodInfo[methodsCalledMap.values().size()]);

            // check the substitute methods on this template
            cd = new CodeDisassembler(substituteMethod);
            cd.disassemble(new CodeAssemblerPrinter(substituteMethod
                .getMethodDescriptor().getParameterTypes(), true, null) {

                 public void invokeVirtual(String className, String methodName,
                         TypeDesc ret, TypeDesc[] params)  {
                     if ("print".equals(methodName) ||
                             "toString".equals(methodName) ||
                             "equals".equals(methodName) ||
                             (contextClass.indexOf(className) == -1 &&
                             className.indexOf("MergedClass") == -1))
                         return;

                     AppMethodInfo ami = new AppMethodInfo(methodName, params);
                     if (! methodsCalledMap.containsKey(ami))
                         methodsCalledMap.put(ami, ami);
                     else
                         methodsCalledMap.get(ami).incCallCount();
                 }

                 public void println(String s) { } // Do nothing
            });
        }
        catch (IOException ix) {
            return new AppMethodInfo[0];
        }

        return methodsCalledMap.values().toArray(
            new AppMethodInfo[methodsCalledMap.values().size()]);
    }

    public static class AppMethodInfo {

        private String mName;
        private TypeDesc[] mParams;
        private int mCallCount;

        public AppMethodInfo(String name, TypeDesc[] params) {
            mName = name;
            mParams = params != null ? params : new TypeDesc[0];
            mCallCount = 1;
        }

        public AppMethodInfo(String desc, String delim) {
            mName = desc.substring(0, desc.indexOf('('));
            StringTokenizer st = new StringTokenizer(
                desc.substring(desc.indexOf('(') + 1,
                desc.indexOf(')')), delim);
            ArrayList<TypeDesc> l = new ArrayList<TypeDesc>();
            while ( st.hasMoreTokens() )
                l.add(TypeDesc.forDescriptor(st.nextToken().trim()));
            mParams = (TypeDesc[]) l.toArray(new TypeDesc[l.size()]);
            try {
                mCallCount = Integer.parseInt(
                    desc.substring(desc.lastIndexOf(':') + 1));
            }
            catch (NumberFormatException ne) {
                mCallCount = 1;
            }
        }

        public AppMethodInfo(String desc) {
            this(desc, "|");
        }

        public AppMethodInfo(Method method) {
            Class<?>[] pTypes = method.getParameterTypes();
            java.lang.reflect.Type[] gTypes = method.getGenericParameterTypes();
            mParams = new TypeDesc[pTypes.length];
            for (int i = 0; i < mParams.length; i++)
                mParams[i] = TypeDesc.forClass(pTypes[i], gTypes[i]);
            mCallCount = 1;
            mName = method.getName();
        }

        public String getName() { return mName; }
        public TypeDesc[] getParams() { return mParams; }
        public int getCallCount() { return mCallCount; }
        public void incCallCount() { mCallCount++; }

        public int hashCode() {
            int hash = mName.hashCode();
            for (int i = 0; mParams != null && i < mParams.length; i++)
                hash ^= mParams[i].toString().hashCode();
            return hash;
        }

        public String toString() {
            return getDescriptorStr() + ":" + mCallCount;
        }

        public String getDescriptorStr(String delim) {
            StringBuffer s = new StringBuffer(mName + "(");
            for (int i = 0; mParams != null && i < mParams.length; i++) {
               s.append(mParams[i].toString());
               if (i < mParams.length - 1)
                   s.append(delim).append(" ");
            }
            s.append(")");
            return s.toString();
        }

        public String getDescriptorStr() {
            return getDescriptorStr("|");
        }

        public boolean equals(Object o) {
            for (int i = 0; mParams != null && i < mParams.length; i++)

                if (((AppMethodInfo) o).getParams().length != mParams.length ||
                        ! ((AppMethodInfo) o).getParams()[i].equals(mParams[i]))
                    return false;
            return ((AppMethodInfo) o).getName().equals(mName);
        }
    }
}
