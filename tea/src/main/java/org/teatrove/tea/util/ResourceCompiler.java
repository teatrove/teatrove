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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

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
public class ResourceCompiler extends AbstractCompiler {

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

        super(injector, packagePrefix);
    }

    public boolean sourceExists(String name) {
        String resName = '/' + name.replace('.', '/') + ".tea";
        return this.getClass().getResource(resName) != null;
    }
    
    public String[] getAllTemplateNames() throws IOException {
        // TODO: support ability to search entire class path although that
        // could be costly and unwanted
        return new String[0];
    }

    protected CompilationUnit createCompilationUnit(String name) {
        return new Unit(name, this);
    }

    private class Unit extends AbstractUnit {
        public Unit(String name, Compiler compiler) {
            super(name, compiler);
        }

        public Reader getReader() throws IOException {
            String resName = '/' + getSourceFileName();
            return new InputStreamReader
                (this.getClass().getResourceAsStream(resName));
        }

        protected long getLastModified() {
            String resName = '/' + getSourceFileName();
            URL url = this.getClass().getResource(resName);
            if (url == null) {
                return -1;
            }
            
            try { return url.openConnection().getLastModified(); }
            catch (IOException ioe) {
                System.err.println(
                    "unable to get last modified for resource: " + resName);
                ioe.printStackTrace();
                
                return -1;
            }
        }
        
        protected InputStream getTemplateSource(String templateSourceName) 
            throws IOException {
    
            String resName = '/' + getSourceFileName();
            return this.getClass().getResourceAsStream(resName);
        }
    }
}
