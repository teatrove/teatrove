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

package org.teatrove.tea.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.OutputStream;
import java.util.Hashtable;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.CompilationUnit;

/**
 * Simple compiler implementation that compiles a Tea template whose source
 * is in a String. Call {@link #setTemplateSource setTemplateSource} to
 * supply source code for templates before calling compile.
 *
 * @author Brian S O'Neill
 * @version

 */
public class StringCompiler extends Compiler {

    private ClassInjector mInjector;
    private String mPackagePrefix;
    private Hashtable mTemplateSources;
    
    /**
     * @param injector ClassInjector to feed generated classes into
     */
    public StringCompiler(ClassInjector injector) {
        this(injector, null);
    }

    /**
     * @param injector ClassInjector to feed generated classes into
     * @param packagePrefix The target package for the compiled templates
     */
    public StringCompiler(ClassInjector injector, String packagePrefix) {
        super();
        mInjector = injector;
        mPackagePrefix = packagePrefix;
        mTemplateSources = new Hashtable();
    }

    public boolean sourceExists(String name) {
        return mTemplateSources.containsKey(name);
    }

    protected CompilationUnit createCompilationUnit(String name) {
        return new Unit(name, this);
    }

    /**
     * @param name The name of the template
     * @param source The source code for the template
     */
    public void setTemplateSource(String name, String source) {
        mTemplateSources.put(name, source);
    }

    private class Unit extends CompilationUnit {
        private String mSourceFileName;

        public Unit(String name, Compiler compiler) {
            super(name, compiler);

            mSourceFileName = 
                name.substring(name.lastIndexOf('.') + 1) + ".tea";
        }

        public String getSourceFileName() {
            return mSourceFileName;
        }
        
        public String getTargetPackage() {
            return mPackagePrefix;
        }
        
        public Reader getReader() throws IOException {
            String source = (String)mTemplateSources.get(getName());
            return new StringReader(source);
        }
        
        public OutputStream getOutputStream() throws IOException {
            String className = getName();
            String pack = getTargetPackage();
            if (pack != null && pack.length() > 0) {
                className = pack + '.' + className;
            }

            return mInjector.getStream(className);
        }
    }
}
