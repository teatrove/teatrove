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
import java.net.URL;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;

/**
 * ResourceCompilationProvider provides tea sources using the resource loading 
 * mechanism built into Java. The resource name must have the extension ".tea".
 *
 * @author Brian S O'Neill
 * @see java.lang.Class#getResource
 */
public class ResourceCompilationProvider implements CompilationProvider {

    private ClassLoader mLoader;
    private String mRootPackage;
    
    public ResourceCompilationProvider() {
        this(Thread.currentThread().getContextClassLoader(), "");
    }
    
    public ResourceCompilationProvider(String rootPackage) {
        this(Thread.currentThread().getContextClassLoader(), rootPackage);
    }
    
    public ResourceCompilationProvider(ClassLoader loader, String rootPackage) {
        mLoader = loader;
        mRootPackage = rootPackage;
        if (!mRootPackage.isEmpty() && !mRootPackage.endsWith("/")) {
            mRootPackage = mRootPackage.concat("/");
        }
    }
    
    @Override
    public boolean sourceExists(String name) {
        String resName = mRootPackage + name.replace('.', '/') + ".tea";
        return mLoader.getResource(resName) != null;
    }

    @Override
    public String[] getKnownTemplateNames(boolean recurse) throws IOException {
        // TODO: support ability to search entire class path although that
        // could be costly and unwanted
        return new String[0];
    }

    @Override
    public CompilationSource createCompilationSource(String name) {
        URL resource = getResource(name);
        if (resource == null) {
            return null;
        }
           
        return new ResourceSource(resource);
    }

    protected URL getResource(String name) {
        String resName = '/' + name.replace('.', '/') + ".tea";
        return this.getClass().getResource(resName);
    }
    
    public static class ResourceSource implements CompilationSource {
        
        private URL mResource;
        
        public ResourceSource(URL resource) {
            mResource = resource;
        }

        @Override
        public String getSourcePath() {
            return mResource.toExternalForm();
        }
        
        @Override
        public long getLastModified() {
            try { return mResource.openConnection().getLastModified(); }
            catch (IOException ioe) {
                System.err.println(
                    "unable to get last modified for resource: " + mResource);
                ioe.printStackTrace();
                
                return -1;
            }
        }
        
        @Override
        public InputStream getSource() 
            throws IOException {
    
            return mResource.openStream();
        }
    }
}
