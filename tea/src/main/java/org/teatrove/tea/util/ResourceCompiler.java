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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.trove.util.ClassInjector;

/**
 * ResourceCompiler compiles tea sources using the resource loading mechanism
 * built into Java. The resource name must have the extension ".tea".
 *
 * @author Brian S O'Neill
 * @see java.lang.Class#getResource
 */
public class ResourceCompiler extends Compiler {

    private ClassInjector mInjector;
    private String mPackagePrefix;

    /**
     * @param injector ClassInjector to feed generated classes into
     */
    public ResourceCompiler(ClassInjector injector) {
        this(injector, null);
    }

    /**
     * @param injector ClassInjector to feed generated classes into
     * @param packagePrefix The target package for the compiled templates
     */
    public ResourceCompiler(ClassInjector injector, String packagePrefix) {

        super();
        mInjector = injector;
        mPackagePrefix = packagePrefix;
    }

    public String[] compile(String name) throws IOException {
        String[] results = super.compile(name);
        return results;
    }

    public String[] compile(String[] names) throws IOException {
        String[] results = super.compile(names);
        return results;
    }

    public boolean sourceExists(String name) {
        String resName = '/' + name.replace('.', '/') + ".tea";
        return this.getClass().getResource(resName) != null;
    }

    protected CompilationUnit createCompilationUnit(String name) {
        return new Unit(name, this);
    }

    private class Unit extends AbstractCompilationUnit {
        private String mSourceFileName;

        public Unit(String name, Compiler compiler) {
            super(name, compiler);

            /*
            int index = name.lastIndexOf('.');
            if (index >= 0) {
                mSourceFileName = name.substring(index + 1) + ".tea";
            }
            else {
                mSourceFileName = name + ".tea";
            }
            */
            mSourceFileName = name.replace('.','/') + ".tea";
        }

        public String getSourceFileName() {
            return mSourceFileName;
        }

        public Reader getReader() throws IOException {
            String resName = '/' + getSourceFileName();
            return new InputStreamReader
                (this.getClass().getResourceAsStream(resName));
        }

        public String getTargetPackage() {
            return mPackagePrefix;
        }

        public OutputStream getOutputStream() throws IOException {
            return mInjector.getStream(getClassName());
        }

        public void resetOutputStream() {
            mInjector.resetStream(getClassName());
        }
    }
}
