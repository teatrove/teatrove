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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Merged compiler represents a merging of several other compilers into a 
 * single compiler in order to cross-reference templates across compilers.
 */
public class MergedCompiler extends Compiler {

    private Compiler[] compilers;
    
    public MergedCompiler(Compiler[] compilers) {
        this.compilers = compilers;
    }
    
    @Override
    public boolean sourceExists(String name) {
        // search for matching compiler
        for (Compiler compiler : this.compilers) {
            if (compiler.sourceExists(name)) {
                return true;
            }
        }
        
        // no matching compiler found
        return false;
    }

    @Override
    public String[] getAllTemplateNames(boolean recurse) throws IOException {
        Set<String> templates = new HashSet<String>();
        for (Compiler compiler : this.compilers) {
            Collections.addAll(templates, compiler.getAllTemplateNames(recurse));
        }
        
        return templates.toArray(new String[templates.size()]);
    }
    
    @Override
    protected CompilationUnit createCompilationUnit(String name) {
        // search for matching compiler
        for (Compiler compiler : this.compilers) {
            if (compiler.sourceExists(name)) {
                CompilationUnit unit = compiler.createCompilationUnit(name);
                if (unit != null) {
                    unit.setCompiler(this);
                }
            }
        }
        
        // no matching compiler found
        return null;
    }

    @Override
    public void addErrorListener(ErrorListener listener) {
        super.addErrorListener(listener);
        for (Compiler compiler : this.compilers) {
            compiler.addErrorListener(listener);
        }
    }

    @Override
    public void removeErrorListener(ErrorListener listener) {
        super.removeErrorListener(listener);
        for (Compiler compiler : this.compilers) {
            compiler.removeErrorListener(listener);
        }
    }

    @Override
    public void addStatusListener(StatusListener listener) {
        super.addStatusListener(listener);
        for (Compiler compiler : this.compilers) {
            compiler.addStatusListener(listener);
        }
    }

    @Override
    public void removeStatusListener(StatusListener listener) {
        super.removeStatusListener(listener);
        for (Compiler compiler : this.compilers) {
            compiler.removeStatusListener(listener);
        }
    }

    @Override
    public void setCodeGenerationEnabled(boolean flag) {
        super.setCodeGenerationEnabled(flag);
        for (Compiler compiler : this.compilers) {
            compiler.setCodeGenerationEnabled(flag);
        }
    }

    @Override
    public boolean isCodeGenerationEnabled() {
        return super.isCodeGenerationEnabled();
    }

    @Override
    public void setExceptionGuardianEnabled(boolean flag) {
        super.setExceptionGuardianEnabled(flag);
        for (Compiler compiler : this.compilers) {
            compiler.setExceptionGuardianEnabled(flag);
        }
    }

    @Override
    public boolean isExceptionGuardianEnabled() {
        return super.isExceptionGuardianEnabled();
    }

    @Override
    public void setClassLoader(ClassLoader loader) {
        super.setClassLoader(loader);
        for (Compiler compiler : this.compilers) {
            compiler.setClassLoader(loader);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    @Override
    public Class<?> loadClass(String name) 
        throws ClassNotFoundException {
        
        try { return super.loadClass(name); }
        catch (ClassNotFoundException cnfe) {
            for (Compiler compiler : this.compilers) {
                try { return compiler.loadClass(name); }
                catch (ClassNotFoundException cnfe2) { continue; }
            }
        }
        
        throw new ClassNotFoundException(name);
    }

    @Override
    public void preserveParseTree(String name) {
        super.preserveParseTree(name);
        for (Compiler compiler : this.compilers) {
            compiler.preserveParseTree(name);
        }
    }

    @Override
    public String[] compile(String name) throws IOException {
        Set<String> results = new HashSet<String>();
        
        // parent resources
        String[] compiled = super.compile(name);
        Collections.addAll(results, compiled);
        
        // each merged compiler
        for (Compiler compiler : this.compilers) {
            compiled = compiler.compile(name);
            Collections.addAll(results, compiled);
        }
        
        // return results
        return results.toArray(new String[results.size()]);
    }

    @Override
    public String[] compile(String[] names) throws IOException {
        Set<String> results = new HashSet<String>();
        
        // parent resources
        String[] compiled = super.compile(names);
        Collections.addAll(results, compiled);
        
        // each merged compiler
        for (Compiler compiler : this.compilers) {
            // TODO: only compile the templates where sourceExists for given names
            // so we don't attempt to compile each for each compiler
            compiled = compiler.compile(names);
            Collections.addAll(results, compiled);
        }
        
        // return results
        return results.toArray(new String[results.size()]);
    }

    @Override
    public int getErrorCount() {
        int errorCount = super.getErrorCount();
        for (Compiler compiler : this.compilers) {
            errorCount += compiler.getErrorCount();
        }
        
        return errorCount;
    }

    @Override
    public CompilationUnit getCompilationUnit(String name, 
                                              CompilationUnit from) {
        CompilationUnit unit = super.getCompilationUnit(name, from);
        if (unit == null) {
            for (Compiler compiler : this.compilers) {
                unit = compiler.getCompilationUnit(name, from);
                if (unit != null) { 
                    unit.setCompiler(this);
                    break;
                }
            }
        }
        
        return unit;
    }

    @Override
    public String[] getImportedPackages() {
        return super.getImportedPackages();
    }

    @Override
    public void addImportedPackage(String imported) {
        super.addImportedPackage(imported);
        for (Compiler compiler : this.compilers) {
            compiler.addImportedPackage(imported);
        }
    }

    @Override
    public void addImportedPackages(String[] imports) {
        super.addImportedPackages(imports);
        for (Compiler compiler : this.compilers) {
            compiler.addImportedPackages(imports);
        }
    }

    @Override
    public void setRuntimeContext(Class<?> contextClass) {
        super.setRuntimeContext(contextClass);
        for (Compiler compiler : this.compilers) {
            compiler.setRuntimeContext(contextClass);
        }
    }
}
