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

import org.teatrove.trove.io.SourceReader;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.trove.classfile.TypeDesc;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 
 * @author Brian S O'Neill
 */
public abstract class CompilationUnit implements ErrorListener {
    private String mName;
    private Compiler mCompiler;
    private Template mTree;

    private int mErrorCount;

    public CompilationUnit(String name, Compiler compiler) {
        mName = name;
        mCompiler = compiler;
    }

    public String getName() {
        return mName;
    }

    public String getShortName() {
        String name = getName();
        int index = name.lastIndexOf('.');
        if (index >= 0) {
            return name.substring(index + 1);
        }

        return name;
    }

    public Compiler getCompiler() {
        return mCompiler;
    }

    /**
     * The retrieves the runtime context.  The default behavior is to delegate
     * this call to the compiler.  This is overriden to implement compiled 
     * templates.
     */
    public Class getRuntimeContext() { return mCompiler.getRuntimeContext(); }

    /**
     * Called when there is an error when compiling this CompilationUnit.
     */
    public void compileError(ErrorEvent e) {
        mErrorCount++;
    }

    /**
     * Returns the number of errors generated while compiling this
     * CompilationUnit.
     */
    public int getErrorCount() {
        return mErrorCount;
    }

    public Template getParseTree() {
        if (mTree == null && mCompiler != null) {
            return mCompiler.getParseTree(this);
        }
        return mTree;
    }

    public void setParseTree(Template tree) {
        mTree = tree;
    }

    /**
     * Current implementation returns only the same packages as the compiler.
     *
     * @see Compiler#getImportedPackages()
     */
    public final String[] getImportedPackages() {
        return mCompiler.getImportedPackages();
    }
   
    /**
     * Return the package name that this CompilationUnit should be compiled
     * into. Default implementation returns null, or no package.
     */
    public String getTargetPackage() {
        return null;
    }

    public abstract String getSourceFileName();

    /**
     * @return A new source file reader.
     */
    public abstract Reader getReader() throws IOException;

    /**
     * @return true if the CompilationUnit should be compiled. Default is true.
     */
    public boolean shouldCompile() throws IOException {
        return true;
    }

    public boolean signatureEquals(String templateName, TypeDesc[] params, TypeDesc returnType) throws IOException {
        if(!getName().equals(templateName)) {
            return false;
        }
        
        Reader r = new BufferedReader(getReader());
        SourceReader srcReader = new SourceReader(r, "<%", "%>");        
        Template tree = null;
        try {
            Scanner s = new Scanner(srcReader, this);
            s.addErrorListener(this);
            Parser p = new Parser(s, this);
            p.addErrorListener(this);
            tree = p.parse();
            s.close();

        } finally {
            r.close();
        }        
        
        // fill sourceParams
        Variable[] vars = tree.getParams();
        TypeDesc[] sourceParams = new TypeDesc[vars.length];
        
        for (int i = 0; i < vars.length; i++) {
            String type = classNameFromVariable(
                    vars[i].getTypeName().getName(), vars[i].getTypeName().getDimensions());
            sourceParams[i] = TypeDesc.forClass(type);
        }

        // strip off merged class since it may be different for remote compiler
        TypeDesc[] temp = new TypeDesc[params.length-1];
        System.arraycopy(params, 1, temp, 0, temp.length);
        
        // compare params
        if(! Arrays.equals(temp, sourceParams)) {
            return false;
        }

        // compare return types (null is default from Template.getReturnType())
        if(null!=tree.getReturnType()) {
            TypeDesc srcReturnType = TypeDesc.forClass(tree.getReturnType().getSimpleName());
            if(! returnType.equals(srcReturnType) ) {
                return false;
            }
        }
        
        return true;
    }

    private String classNameFromVariable(String type, int dimensions) {
        
        if("short".equals(type)
        || "long".equals(type)
        || "double".equals(type)) {
            type = type.substring(0, 1).toUpperCase()+ type.substring(1);
        }

        if("int".equals(type)) {
            type = "Integer";
        }

        if("Short".equals(type)
        || "Integer".equals(type)
        || "Long".equals(type)
        || "Double".equals(type)
        || "String".equals(type)) {
            type = "java.lang."+type;
        }

        for (int j = 0; j < dimensions; j++) {
            type = type + "[]";
        }
        return type;
    }
    
    /**
     * @return An OutputStream to write compiled code to. Returning null is
     * disables code generation for this CompilationUnit.
     */
    public abstract OutputStream getOutputStream() throws IOException;
}
