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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;

/**
 * JarCompilationProvider provides access to tea source files by reading them 
 * from a given JAR file. When given a JAR file, all files with the JAR file
 * with the extension ".tea" will be provided. 
 *
 * @author Brian S O'Neill
 */
public class JarCompilationProvider implements CompilationProvider {

    private JarOfTemplates mSourceJar;
    
    public JarCompilationProvider(File sourceJarFile) {
        
    	if (sourceJarFile == null ) {
            throw new IllegalArgumentException("sourceJarFile");
        }
        
        try { mSourceJar = new JarOfTemplates(sourceJarFile); }
        catch (IOException ioe) {
            throw new IllegalArgumentException("sourceJarFile", ioe);
        }
    }

    @Override
    public String[] getKnownTemplateNames(boolean recurse) 
        throws IOException {
        
        Collection<String> sources = new TreeSet<String>();
        gatherSources(sources);
        return sources.toArray(new String[sources.size()]);
    }
    
    @Override
    public boolean sourceExists(String name) {
        return findJarEntry(name) != null;
    }
    
    @Override
    public CompilationSource createCompilationSource(String name) {
        JarEntry jarEntry = findJarEntry(name);
        if (jarEntry == null) {
            return null;
        }
        
        try { return new JarredSource(jarEntry, mSourceJar); }
        catch (IOException ioe) {
            // TODO: log error
            return null;
        }
    }

    protected JarEntry findJarEntry(String name) {
        String fileName = name.replace('.', '/') + ".tea";
        return mSourceJar.getEntry(fileName);
    }

    protected void gatherSources(Collection<String> sources) 
        throws IOException {
    
        Enumeration<JarEntry> entries = mSourceJar.getEntries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.endsWith(".tea")) {
                name = name.substring(0, name.lastIndexOf(".tea"));
                name = name.replace('/', '.');
                if(!sources.contains(name)) {
                    sources.add(name);
                }
            }
        }
    }

    public static class JarredSource implements CompilationSource {

        private JarEntry mJarEntry;
        private JarOfTemplates mJarOfTemplates;
        private URL mUrl;

        public JarredSource(JarEntry entry, JarOfTemplates jarOfTemplates) 
            throws IOException {

            mJarEntry = entry;
            mJarOfTemplates = jarOfTemplates;
            mUrl = new URL(mJarOfTemplates.getUrl(), mJarEntry.toString());
        }

        public JarEntry getJarEntry() {
            return mJarEntry;
        }
        
        @Override
        public String getSourcePath() {
            return mUrl.toExternalForm();
        }

        @Override
        public InputStream getSource() 
            throws IOException {
            
            return mJarOfTemplates.getInputStream(mJarEntry);
        }
        
        @Override
        public long getLastModified() {
            return (mJarEntry == null ? -1 : mJarEntry.getTime());            
        }
    }

    protected static class JarOfTemplates {
        private JarFile mJarFile;
        private JarURLConnection mConn;
        private URL mUrl;

        public JarOfTemplates(File file) throws IOException {
            mUrl = makeJarUrlFromFile(file);
            
            mConn = (JarURLConnection)mUrl.openConnection();
            mConn.setUseCaches(false);
            
            mJarFile = mConn.getJarFile();
        }

        public JarEntry getEntry(JarEntry jarEntry) {
            return getEntry(jarEntry.getName());
        }

        public JarEntry getEntry(String name) {
            return mJarFile.getJarEntry(name);
        }

        public Enumeration<JarEntry> getEntries() {
            // TODO cache with weak ref and check url headers for changes
            return mJarFile.entries();
        }

        public InputStream getInputStream(JarEntry jarEntry) throws IOException {
            return mJarFile.getInputStream(jarEntry);
        }

        public URL getUrl() {
            return mUrl;
        }

        public void close() throws IOException {
            if(mJarFile!=null) mJarFile.close();
            mJarFile = null;
            mConn = null;
        }

        URL makeJarUrlFromFile(File path) {
            String urlStr = path.toString();

            urlStr = urlStr.replace("\\", "/");
            if(urlStr.startsWith("jar:file:")) {
                urlStr = urlStr.replaceFirst("/", "///");
            } else {
                if(urlStr.indexOf("//")<0) {
                    urlStr = urlStr.replaceFirst("/", "//");
                }
            }

            if(!urlStr.endsWith("/")) urlStr = urlStr+ "/";

            try {
                return new URL(urlStr);
            } catch(MalformedURLException ex) {
                throw new RuntimeException("not a jar url: "+urlStr, ex);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            this.close();
            super.finalize();
        }


    }
}
