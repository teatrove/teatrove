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

package org.teatrove.teaservlet.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;

public class ServletContextCompilationProvider implements CompilationProvider {
    
	private ServletContext mServletContext;
    private String mSourceDir;
    private Map<String, String> mTemplates;

    public ServletContextCompilationProvider(ServletContext servletContext,
    							  String rootSourceDir) {
        mServletContext = servletContext;
        mSourceDir = rootSourceDir;
    	if (!mSourceDir.startsWith("/")) {
    		throw new IllegalStateException("expected paths to begin with leading slash");
    	}

    	if (!mSourceDir.endsWith("/")) {
    	    mSourceDir = mSourceDir.concat("/");
    	}

        mTemplates = loadTemplates();
    }

    /**
     * Checks that the source code for a specified template exists.
     */
    @Override
    public boolean sourceExists(String name) {
    	return getResource(name) != null;
    }
    
    @Override
    public String[] getKnownTemplateNames(boolean recurse) {
    	return mTemplates.keySet().toArray(new String[mTemplates.size()]);
    }

    @Override
    public CompilationSource createCompilationSource(String name) {
        URL resource = getResource(name);
        return (resource == null ? null : new ServletContextSource(resource));
    }

    protected URL getResource(String name) {
        String path = mTemplates.get(name);
        if (path != null) {
            try { return mServletContext.getResource(path); }
            catch (IOException ioe) { /* ignore */ }
        }
        
        return null;
    }
    
    protected Map<String, String> loadTemplates() {
    	Map<String, String> templates = new HashMap<String, String>();
    	listTemplates(mSourceDir, mSourceDir, templates);
    	return templates;
    }
    
    @SuppressWarnings("unchecked")
    private void listTemplates(String root, String base, 
                               Map<String, String> templates) {
		Set<String> paths = mServletContext.getResourcePaths(base);
    	for (String path : paths) {
    		if (path.endsWith("/")) {
    			listTemplates(root, path, templates);
    		}
    		else if (path.endsWith(".tea")) {
    			String name = 
    			    path.substring(root.length(), path.length() - 4)
    			        .replaceAll("[\\\\/]+", ".");
    			
    			templates.put(name, path);
    		}
    	}
    }

    public class ServletContextSource implements CompilationSource {
        
        private URL mResource;
        
        public ServletContextSource(URL resource) {
            mResource = resource;
        }

        @Override
        public String getSourcePath() {
            return mResource.toExternalForm();
        }
        
        @Override
        public long getLastModified() {
        	long remoteTimeStamp = -1;
        	try {
            	URLConnection connection = mResource.openConnection();
            	return connection.getLastModified();
        	}
        	catch (IOException exception) {
        		mServletContext.log(
        		    "unable to open resource: " + getSourcePath(), exception);
        	}
            
            return remoteTimeStamp;
        }
        
        /**
         * get a input stream containing the template source data.
         */
        @Override
        public InputStream getSource() 
        	throws IOException {
        	
            return mResource.openStream();
        }
    }
    
}
