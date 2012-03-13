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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.trove.util.ClassInjector;

/**
 * Simple compiler implementation that compiles a Tea template whose source
 * is in a String. Call {@link #setTemplateSource setTemplateSource} to
 * supply source code for templates before calling compile.
 *
 * @author Brian S O'Neill
 */
public class StringCompiler extends AbstractCompiler {

    private Hashtable<String, TemplateSource> mTemplateSources;

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
        super(injector, packagePrefix);
        mTemplateSources = new Hashtable<String, TemplateSource>();
    }

    public boolean sourceExists(String name) {
        return mTemplateSources.containsKey(name);
    }

    @Override
    public String[] getAllTemplateNames() 
        throws IOException {
        
        return mTemplateSources.keySet().toArray(
            new String[mTemplateSources.size()]);
    }
    
    protected CompilationUnit createCompilationUnit(String name) {
        return new Unit(name, this);
    }

    /**
     * @param name The name of the template
     * @param source The source code for the template
     */
    public void setTemplateSource(String name, String source) {
        mTemplateSources.put(name, new TemplateSource(source));
    }

    private class Unit extends AbstractUnit {
        public Unit(String name, Compiler compiler) {
            super(name, compiler);
        }

        protected long getLastModified() {
            TemplateSource source = mTemplateSources.get(getName());
            return source == null ? 0 : source.getTimestamp();
        }
        
        protected InputStream getTemplateSource(String templateSourceName) 
            throws IOException {

            TemplateSource source = mTemplateSources.get(getName());
            return new ByteArrayInputStream(source.getSource().getBytes("UTF-8"));
        }
    }
    
    private static class TemplateSource {
        private String mSource;
        private long mTimestamp;
        
        public TemplateSource(String source) {
            this.mSource = source;
            this.mTimestamp = System.currentTimeMillis();
        }
        
        public String getSource() { return this.mSource; }
        public long getTimestamp() { return this.mTimestamp; }
    }
}
