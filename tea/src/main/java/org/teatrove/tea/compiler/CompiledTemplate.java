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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.teatrove.tea.parsetree.Name;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.tea.runtime.Substitution;
import org.teatrove.trove.io.SourceReader;

/**
 * Pre-compiled templates implementation.   Allows for the invocation of 
 * pre-compiled templates from other dynamically compiled or pre-compiled 
 * templates.  Nested calls to other pre-compiled templates must be from within
 * the same merged context interface.
 * 
 * @author Guy A. Molinari
 */
public class CompiledTemplate extends CompilationUnit {
    private Template mTree;
    private Class<?> mTemplateClass;
    private Method mExecuteMethod;
    private SourceInfo mCallerInfo = null;
    private CompilationUnit mCaller;
    private Variable[] mParameters;
    private Class<?> mRuntimeContext;
    private boolean mSubParam = false;
    private static final String TEMPLATE_PACKAGE = "org.teatrove.teaservlet.template";

    /**
     * This constructor is only invoked when the compiler cannot locate a source
     * file for a template and a template class of the desired name and package
     * exists.  Further verification of the template signature occurs during the
     * call to <b>getParseTree</b>.
     */
    public CompiledTemplate(String name, Compiler compiler,
            CompilationUnit caller) {
        super(name, null, compiler);
        mCaller = caller;
        try {
            if (caller != null && caller.getReader() instanceof SourceReader) {
                SourceReader r = (SourceReader) caller.getReader();
                mCallerInfo = new SourceInfo(r.getLineNumber(),
                    r.getStartPosition(), r.getEndPosition());
            }
        }
        catch (IOException ignore) { }
    }


    private static String resolveName(String name, CompilationUnit from) {
        if (from != null && name.indexOf('.') == -1) {
            String fromName = from.getName();
            if (fromName.indexOf('.') != -1)
                name = fromName.substring(0, fromName.lastIndexOf('.')) + "." + name;
        }
        return name;
    }


    /**
     * Load the template.
     */
    private Class<?> getTemplateClass() {
        String fqName = getTargetPackage() + "." + getName();
        try {
            mTemplateClass = getCompiler().loadClass(fqName);
        }
        catch (ClassNotFoundException nx) {
            try {
                mTemplateClass = getCompiler().loadClass(getName());  // Try standard path as a last resort
            }
            catch (ClassNotFoundException nx2) {
                return null;
            }
        }
        return mTemplateClass;
    }


    public String getName() {
        return resolveName(super.getName(), mCaller);
    }

    /**
     * Checks if the compiled template class is a precomiled
     *  template (i.e. it's runtime context is an interface, not a generated MergedContext)
     *
     * @return true if the template class is found, has the proper template execute method,
     * and the first parameter to the execute method (the runtime context for the template) is an interface
     */
    public boolean isValid() {
        getTemplateClass();
        if (findExecuteMethod() == null)
            throw new IllegalArgumentException("Cannot locate compiled template entry point.");
         Class<?>[] p = mExecuteMethod.getParameterTypes();
         mSubParam = false;
         mRuntimeContext = p[0];
         return mRuntimeContext.isInterface();
    }

    /**
     * Test to see of the template can be loaded before CompiledTemplate can be
     * constructed.
     */
    public static boolean exists(Compiler c, String name, CompilationUnit from) {
        String fqName = getFullyQualifiedName(resolveName(name, from));
        try {
            c.loadClass(fqName);
            return true;
        }
        catch (ClassNotFoundException nx) {
            try {
                c.loadClass(resolveName(name, from));  // Try standard path as a last resort
                return true;
            }
            catch (ClassNotFoundException nx2) {
                return false;
            }
        }
    }


    public static String getFullyQualifiedName(String name) {
       return ! name.startsWith(TEMPLATE_PACKAGE) ? TEMPLATE_PACKAGE + "." + name : name;
    }


    /**
     * Get the runtime context of the compiled template.   This is an interface for
     * compiled templates.
     */
    public Class<?> getRuntimeContext() { return mRuntimeContext; }


    /**
     * This method is called by JavaClassGenerator during the compile phase. It overrides the
     * method in CompilationUnit and returns just the reflected template signature.
     */
    public Template getParseTree() {
        getTemplateClass();
        if (findExecuteMethod() == null)
            throw new IllegalArgumentException("Cannot locate compiled template entry point.");
        reflectParameters();

        mTree = new Template(mCallerInfo, new Name(mCallerInfo, getName()),
            mParameters, mSubParam, null, null);
        mTree.setReturnType(new Type(mExecuteMethod.getReturnType(),
                                     mExecuteMethod.getGenericReturnType()));

        return mTree;
    }


    private Method findExecuteMethod() {
        mExecuteMethod = null;
        Method[] methods = mTemplateClass.getMethods();
        for (int i = 0; i < methods.length; i++)
            if (JavaClassGenerator.EXECUTE_METHOD_NAME.equals(methods[i].getName()))
                mExecuteMethod = methods[i];
        return mExecuteMethod;
    }


    private void reflectParameters() {
         Class<?>[] p = mExecuteMethod.getParameterTypes();
         java.lang.reflect.Type[] t = mExecuteMethod.getGenericParameterTypes();
         ArrayList<Variable> list = new ArrayList<Variable>();
         mSubParam = false;
         mRuntimeContext = p[0];
         if (!mRuntimeContext.isInterface())
             throw new IllegalArgumentException("No context found in compiled template execute method.");
         for (int i = 1; i < p.length; i++) {  // Ignore context parameter
             if (Substitution.class == p[i]) {
                 mSubParam = true;
                 continue;
             }
             list.add(new Variable(mCallerInfo, null, new Type(p[i], t[i])));
         }
         mParameters = list.toArray(new Variable[list.size()]);

    }


    public void setParseTree(Template tree) {
        // Do nothing parse tree is immutable;
    }


    /**
     * Return the package name that this CompilationUnit should be compiled
     * into.
     */
    public String getTargetPackage() {
        return mCaller != null && mCaller.getTargetPackage() != null ? 
            mCaller.getTargetPackage() : TEMPLATE_PACKAGE;
    }


    /**
     * @return true if the CompilationUnit should be compiled. Default is true.
     */
    public boolean shouldCompile() {
        return false;
    }


    /**
     * Delegate to calling template.
     */
    public OutputStream getOutputStream() throws IOException {
        return mCaller != null ? mCaller.getOutputStream() : null;
    }

    /**
     * Delegate to calling template.
     */
    public void resetOutputStream() {
        if (mCaller != null) {
            mCaller.resetOutputStream();
        }
    }

    /**
     * Delegate to calling template.
     */
    public Reader getReader() throws IOException {
        return mCaller != null ? mCaller.getReader() : null;
    }


    /**
     * Source is not accessible for pre-compiled templates
     */
    public String getSourceFileName() {
        return "Compiled code.";
    }
}
