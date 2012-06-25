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

import javax.servlet.ServletException;

import org.teatrove.tea.runtime.Substitution;
import org.teatrove.teaapps.ContextConfig;
import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.trove.util.PropertyMap;

/**
 * Tea application that supports catching exceptions during processing of a 
 * template and provides a {@link ThrowableHandler} to handle those exceptions
 * and provide the array of resulting exceptions.  
 * 
 * @author Scott Jappinen
 * 
 * @see ThrowableCatcherContext
 */
public class ThrowableCatcherApplication implements Application {

    private ThrowableHandler mThrowableHandler;
    private ThrowableCatcherContext mContext;

    /**
     * Initialize the application with the provided configuration. The 
     * following options are supported:
     * 
     * <dl>
     * <dt>throwableHandlerClass</dt>
     * <dd>
     *     The fully-qualified name of the custom {@link ThrowableHandler} class
     *     to use when processing exceptions. Defalts to
     *     {@link ThrowableHandlerDefaultSupport}.
     * </dd>
     * </dl>
     * 
     * @param config The application configuration
     */
    public void init(ApplicationConfig config) throws ServletException {
        PropertyMap properties = config.getProperties();
        String throwableHandlerClass = 
            properties.getString("throwableHandlerClass");
        
        // load the provided handler class if provided
        if (throwableHandlerClass != null) {
	        try {
	            ContextConfig configSupport = new ContextConfig(
	                config.getProperties(), config.getLog(), 
	                config.getName(), config.getPlugins(),
	                config.getServletContext()
	            );
	            
	            Class<?> clazz = Class.forName(throwableHandlerClass);
	            mThrowableHandler = (ThrowableHandler) clazz.newInstance();
	            mThrowableHandler.init(configSupport);
	        } catch (Exception e) {
	            config.getLog().error(e);
	            throw new ServletException(e);				
	        }
        }
        
        // default to the default handler
        if (mThrowableHandler == null) {
            ContextConfig configSupport = new ContextConfig(
                 config.getProperties(), config.getLog(), 
	             config.getName(), config.getPlugins()
	         );
            
        	 mThrowableHandler = new ThrowableHandlerDefaultSupport();
	         mThrowableHandler.init(configSupport);
        }
        
        // create the singleton context
        mContext = new ThrowableCatcherContextImpl();
    }
    
    public void destroy() {
        // nothing to do
    }
    
    /**
     * Create a context for the given request/response. This creates an instance
     * of {@link ThrowableCatcherContext} using the optionally configured 
     * {@link ThrowableHandler} handler.
     * 
     * @param request The current request
     * @param response The current response
     * 
     * @return The associated context
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return mContext;
    }

    /**
     * Get the context type of this application. This returns the
     * {@link ThrowableCatcherContext} class.
     * 
     * @return The throwable catcher context
     */
    public Class<?> getContextType() {
        return ThrowableCatcherContext.class;
    }

    /**
     * Helper class that provides an implementation of 
     * {@link ThrowableCatcherContext} using a configured throwable handler.
     */
    public class ThrowableCatcherContextImpl 
        implements ThrowableCatcherContext {

        /**
         * Default constructor.
         */
        public ThrowableCatcherContextImpl() {
            super();
        }
        
        /**
         * {@inheritDoc}
         */
        public Throwable[] catchThrowables(Substitution substitution) {
            Throwable[] result = null;
            try {
                substitution.substitute();
            } catch (Throwable t) {
                result = mThrowableHandler.handleThrowables(t);
            }
            
            return result;
        }
    }
}
