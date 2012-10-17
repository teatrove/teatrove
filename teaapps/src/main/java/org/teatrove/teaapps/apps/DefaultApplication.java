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
package org.teatrove.teaapps.apps;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;
import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.trove.util.PropertyMap;

/**
 * The default application is a stateful Tea application that uses a configured
 * context class to instantiate once and return as the context with each
 * request.  The context class must contain a public no-arg default constructor.
 * If the given context class implements the {@link Context} interface, then
 * the context will be initialized with the application configuration.
 * 
 * @author Scott Jappinen
 */
public class DefaultApplication implements Application {
    
    private Object context;
	private Class<?> contextClass;

	/**
	 * Default constructor.
	 */
	public DefaultApplication() {
	    super();
	}
	
	/**
	 * Initialize the application.
	 * 
	 * @param config The application configuration
	 */
    public void init(ApplicationConfig config) {
        PropertyMap properties = config.getProperties();
		String contextClassName = properties.getString("contextClass");
        if (contextClassName == null) {
            throw new IllegalArgumentException("contextClass");
        }
        
        try {
            contextClass = Class.forName(contextClassName);
            context = contextClass.newInstance();
            if (context instanceof Context) {
                Context castContext = (Context) context;
                ContextConfig contextConfig = new ContextConfig(
                    config.getProperties(), config.getLog(), 
                    config.getName(), config.getPlugins(),
                    config.getServletContext()
                );
                
                castContext.init(contextConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Unable to create context: " + contextClassName, e);
        }
    }
    
    public void destroy() {
        // nothing to do
    }
    
    /**
     * Create the context for the given request. As the default application is
     * stateful, this will always return the single instance of the associated
     * context class.
     * 
     * @param request The current request
     * @param response The current response
     * 
     * @return The single instance of the context class
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {        
        return context;
    }

    /**
     * Get the context class associated with this application.
     * 
     * @return The configured context class
     */
    public Class<?> getContextType() {
        return contextClass;
    }
}
