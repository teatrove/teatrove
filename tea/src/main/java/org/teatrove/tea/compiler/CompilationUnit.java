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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;

import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.io.DualOutput;
import org.teatrove.trove.io.SourceReader;
import org.teatrove.trove.util.ClassInjector;

/**
 * A compilation unit is a unit of compilation for a given template that
 * provides source details and outputs the compiled result to a given set of
 * output streams (class files, injectors, etc).  It works hand-in-hand with the
 * associated compiler during compilation.
 * 
 * @author Brian S O'Neill
 */
public class CompilationUnit implements CompileListener {

    private String mName;
    private Compiler mCompiler;
    private Template mTree;
    private CompilationSource mSource;

    private int mErrorCount;
    private int mWarningCount;
    private File mDestDir;
    private File mDestFile;

    public CompilationUnit(String name, CompilationSource source,
                           Compiler compiler) {
        mName = name;
        mSource = source;
        mCompiler = compiler;
        
        initialize();
    }

    private void initialize() {
        String slashPath = mName.replace('.', '/');
        if (slashPath.endsWith("/")) {
            slashPath = slashPath.substring(0, slashPath.length() - 1);
        }
        
        File rootDestDir = mCompiler.getRootDestDir();
        if (rootDestDir != null) {
            if (slashPath.lastIndexOf('/') >= 0) {
                mDestDir = new File
                    (rootDestDir,
                     slashPath.substring(0,slashPath.lastIndexOf('/')));
            }
            else {
                mDestDir = rootDestDir;
            }
            
            mDestDir.mkdirs();          
            mDestFile = new File(
                mDestDir,
                slashPath.substring(slashPath.lastIndexOf('/') + 1) + 
                    ".class"
            );
        }
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
    
    public String getSourcePath() {
        return mSource == null ? null : mSource.getSourcePath();
    }

    /**
     * Get the associated compiler for this unit.
     * 
     * @return The associated compiler for this compilation unit
     */
    protected Compiler getCompiler() {
        return mCompiler;
    }
    
    /**
     * The retrieves the runtime context.  The default behavior is to delegate
     * this call to the compiler.  This is overriden to implement compiled
     * templates.
     */
    public Class<?> getRuntimeContext() {
        return mCompiler.getRuntimeContext();
    }

    /**
     * Called when there is an error when compiling this CompilationUnit.
     */
    public void compileError(CompileEvent e) {
        mErrorCount++;
    }

    /**
     * Called when there is a warning when compiling this CompilationUnit.
     */
    public void compileWarning(CompileEvent e) {
        mWarningCount++;
    }
    
    /**
     * Returns the number of errors generated while compiling this
     * CompilationUnit.
     */
    public int getErrorCount() {
        return mErrorCount;
    }

    /**
     * Returns the number of warnings generated while compiling this
     * CompilationUnit.
     */
    public int getWarningCount() {
        return mWarningCount;
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
        return mCompiler.getRootPackage();
    }

    public Reader getReader() throws IOException {
        Reader reader = null;
        InputStream in = getTemplateSource();
        String encoding = mCompiler.getEncoding();
        if (encoding == null) {
            reader = new InputStreamReader(in);
        }
        else {
            reader = new InputStreamReader(in, encoding);
        }
        
        return reader;
    }
    
    public void syncTimes() {
        if (mDestFile != null) {
            mDestFile.setLastModified(getLastModified());
        }
    }
    
    public boolean shouldCompile() {
        return getCompiler().isForceCompile() ||
               shouldCompile(getDestinationLastModified());
    }

    protected boolean shouldCompile(long timestamp) {
        if (mDestFile != null && !mDestFile.exists()) {
            return true;
        }

        long precompiledTolerance = mCompiler.getPrecompiledTolerance();
        long lastModified = getLastModified();
        if (timestamp > 0 && lastModified > 0 &&
            gteq(timestamp, lastModified, precompiledTolerance)) {

            return false;
        }

        return true;
    }

    public long getLastModified() {
        return mSource == null ? -1 : mSource.getLastModified();
    }
    
    protected long getDestinationLastModified() {
        if (mDestFile != null && mDestFile.exists()) {
            return mDestFile.lastModified();
        }
        
        return 0L;
    }
    
    /**
     * Returns the truth that a is greater than or equal to b assuming the given tolerance.
     * <p>
     * ex. tolerance = 10, a = 1000 and b = 2000, returns false<br/>
     * ex. tolerance = 10, a = 1989 and b = 2000, returns false<br/>
     * ex. tolerance = 10, a = 1990 and b = 2000, returns true<br/>
     * ex. tolerance = 10, a = 2000 and b = 2000, returns true<br/>
     * ex. tolerance = 10, a = 3000 and b = 2000, returns true<br/>
     * </p>
     *
     * @param a
     * @param b
     * @param tolerance
     * @return
     */
    private boolean gteq(long a, long b, long tolerance) {
        return a >= b - tolerance;
    }

    /**
     * @return the file that gets written by the compiler.
     */
    public File getDestinationFile() {
        return mDestFile;
    }

    public OutputStream getOutputStream() throws IOException {
        OutputStream out1 = null;
        OutputStream out2 = null;

        if (mDestDir != null) {
            if (!mDestDir.exists()) {
                mDestDir.mkdirs();
            }

            out1 = new FileOutputStream(mDestFile);
        }

        ClassInjector injector = mCompiler.getInjector();
        if (injector != null) {
            String className = getName();
            String pack = getTargetPackage();
            if (pack != null && pack.length() > 0) {
                className = pack + '.' + className;
            }
            out2 = injector.getStream(className);
        }

        OutputStream out;

        if (out1 != null) {
            if (out2 != null) {
                out = new DualOutput(out1, out2);
            }
            else {
                out = out1;
            }
        }
        else if (out2 != null) {
            out = out2;
        }
        else {
            out = new OutputStream() {
                public void write(int b) {}
                public void write(byte[] b, int off, int len) {}
            };
        }

        return new BufferedOutputStream(out);
    }
    
    public void resetOutputStream() {
        if (mDestFile != null) {
            mDestFile.delete();
        }

        ClassInjector injector = mCompiler.getInjector();
        if (injector != null) {
            injector.resetStream(getClassName());
        }
    }
    
    protected String getClassName() {
        return getClassName(null);
    }

    protected String getClassName(String innerClass) {
        String className = getName();
        String pack = getTargetPackage();
        if (pack != null && pack.length() > 0) {
            className = pack + '.' + className;
        }

        if (innerClass != null) {
            className = className + '$' + innerClass;
        }

        return className;
    }

    protected InputStream getTemplateSource() 
        throws IOException {
        
        if (mSource == null) {
            throw new IOException("no source defined for " + mName);
        }
        
        return mSource.getSource();
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
            s.addCompileListener(this);
            Parser p = new Parser(s, this);
            p.addCompileListener(this);
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
}
