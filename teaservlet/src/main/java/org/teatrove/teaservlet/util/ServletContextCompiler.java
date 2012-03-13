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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.util.AbstractCompiler;
import org.teatrove.trove.util.ClassInjector;

public class ServletContextCompiler extends AbstractCompiler {
    
	private ServletContext mServletContext;
    private String[] mSourceDirs;
    private Map<String, String> mTemplates;

    public ServletContextCompiler(ServletContext servletContext,
    							  String[] rootSourceDirs,
                          		  String rootPackage,
                          		  File rootDestDir,
                          		  ClassInjector injector,
                          		  String encoding,
                          		  long precompiledTolerance) {
        super(injector, rootPackage, rootDestDir, encoding, 
              precompiledTolerance, null);

        mServletContext = servletContext;
        mSourceDirs = rootSourceDirs;
        for (int i = 0; i < mSourceDirs.length; i++) {
        	String sourceDir = mSourceDirs[i];
        	if (!sourceDir.startsWith("/")) {
        		throw new IllegalStateException("expected paths to begin with leading slash");
        	}

        	if (!sourceDir.endsWith("/")) {
        		mSourceDirs[i] = sourceDir.concat("/");
        	}
        }

        mTemplates = loadTemplates();
    }

    /**
     * Checks that the source code for a specified template exists.
     */
    public boolean sourceExists(String name) {
    	return mTemplates.containsKey(name);
    }
    
    public String[] getAllTemplateNames() {
    	return mTemplates.keySet().toArray(new String[mTemplates.size()]);
    }

    protected CompilationUnit createCompilationUnit(String name) {
        return new Unit(name,this);
    }

    private Map<String, String> loadTemplates() {
    	Map<String, String> templates = new HashMap<String, String>();
    	for (String sourceDir : mSourceDirs) {
    		listTemplates(sourceDir, sourceDir, templates);
    	}
    	
    	return templates;
    }
    
    @SuppressWarnings("unchecked")
    private void listTemplates(String root, String base, Map<String, String> templates) {
		Set<String> paths = mServletContext.getResourcePaths(base);
    	for (String path : paths) {
    		if (path.endsWith("/")) {
    			listTemplates(root, path, templates);
    		}
    		else if (path.endsWith(".tea")) {
    			String name = path.substring(root.length(), path.length() - 4).replaceAll("[\\\\/]+", ".");
    			templates.put(name, path);
    		}
    	}
    }

    public class Unit extends AbstractUnit {
        
        Unit(String name, Compiler compiler) {
            super(name, compiler);
        }

        protected long getLastModified() {
        	long remoteTimeStamp = 0L;
        	try {
        		String path = mTemplates.get(mDotPath);
        		if (path != null) {
		            URL url = mServletContext.getResource(path);
		            if (url != null) {
		            	URLConnection connection = url.openConnection();
		            	remoteTimeStamp = connection.getLastModified();
		            }
        		}
        	}
        	catch (Exception exception) {
        		mServletContext.log("unable to compile: " + 
        				getSourceFileName(), exception);
        		remoteTimeStamp = -1;
        	}
            
            return remoteTimeStamp;
        }
        
        /**
         * get a input stream containing the template source data.
         */
        @Override
        protected InputStream getTemplateSource(String templateSourceName) 
        	throws IOException {
        	
        	String path = mTemplates.get(templateSourceName);
        	return mServletContext.getResourceAsStream(path);
        }
    }
    
}
