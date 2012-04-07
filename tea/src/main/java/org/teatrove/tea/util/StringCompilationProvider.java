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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;

/**
 * Simple compilation provider implementation that provides a Tea template whose 
 * source is in a String. Call {@link #setTemplateSource setTemplateSource} to
 * supply source code for templates before calling compile.
 *
 * @author Brian S O'Neill
 */
public class StringCompilationProvider implements CompilationProvider {

    private Map<String, TemplateSource> mTemplateSources;

    public StringCompilationProvider() {
        mTemplateSources = new ConcurrentHashMap<String, TemplateSource>();
    }

    @Override
    public boolean sourceExists(String name) {
        return mTemplateSources.containsKey(name);
    }

    @Override
    public String[] getKnownTemplateNames(boolean recurse) 
        throws IOException {
        
        return mTemplateSources.keySet().toArray(
            new String[mTemplateSources.size()]);
    }
    
    @Override
    public CompilationSource createCompilationSource(String name) {
        TemplateSource source = mTemplateSources.get(name);
        if (source == null) {
            return null;
        }
        
        return new StringSource(source);
    }

    /**
     * Add or overwrite an existing source for the given fully-qualified dot
     * format of the given template name.
     * 
     * @param name The name of the template
     * @param source The source code for the template
     */
    public void setTemplateSource(String name, String source) {
        mTemplateSources.put(name, new TemplateSource(name, source));
    }

    public static class StringSource implements CompilationSource {
        private TemplateSource mSource;
        
        public StringSource(TemplateSource source) {
            mSource = source;
        }

        @Override
        public String getSourcePath() {
            return "string:".concat(mSource.getName());
        }
        
        @Override
        public long getLastModified() {
            return mSource.getTimestamp();
        }
        
        @Override
        public InputStream getSource() throws IOException {
            byte[] data = mSource.getSource().getBytes("UTF-8");
            return new ByteArrayInputStream(data);
        }
    }
    
    protected static class TemplateSource {
        private String mName;
        private String mSource;
        private long mTimestamp;
        
        public TemplateSource(String name, String source) {
            this.mName = name;
            this.mSource = source;
            this.mTimestamp = System.currentTimeMillis();
        }
        
        public String getName() { return this.mName; }
        public String getSource() { return this.mSource; }
        public long getTimestamp() { return this.mTimestamp; }
    }
}
