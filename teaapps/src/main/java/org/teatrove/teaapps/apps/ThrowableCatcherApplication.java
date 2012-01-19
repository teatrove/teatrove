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

import org.teatrove.tea.runtime.Substitution;

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;

import org.teatrove.teaapps.ContextConfig;

import javax.servlet.ServletException;

/**
 * @author Scott Jappinen
 */
public class ThrowableCatcherApplication implements Application {

    private ThrowableHandler mThrowableHandler;

    public void init(ApplicationConfig config) throws ServletException {
        String throwableHandlerClass = config.getProperties().getString("throwableHandlerClass");
        if (throwableHandlerClass != null) {
	        try {
	            ContextConfig configSupport = new ContextConfig
	                (config.getProperties(), config.getLog(), 
	                config.getName(), config.getPlugins());
	            Class<?> clazz = Class.forName(throwableHandlerClass);
	            mThrowableHandler = (ThrowableHandler) clazz.newInstance();
	            mThrowableHandler.init(configSupport);
	        } catch (ClassNotFoundException e) {
	            config.getLog().error(e);
	            throw new ServletException(e);
	        } catch (InstantiationException e) {
	            config.getLog().error(e);
	            throw new ServletException(e);
	        } catch (IllegalAccessException e) {
	            config.getLog().error(e);
	            throw new ServletException(e);
	        } catch (Exception e) {
	            config.getLog().error(e);
	            throw new ServletException(e);				
	        }
        }
        if (mThrowableHandler == null) {
            ContextConfig configSupport = new ContextConfig
	             (config.getProperties(), config.getLog(), 
	             config.getName(), config.getPlugins());
        	 mThrowableHandler = new ThrowableHandlerDefaultSupport();
	         mThrowableHandler.init(configSupport);
        }
    }
    
    public void destroy() {}
    
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new ThrowableCatcherContextImpl(response, mThrowableHandler);
    }

    public Class<?> getContextType() {
        return ThrowableCatcherContext.class;
    }

    public class ThrowableCatcherContextImpl implements ThrowableCatcherContext {

        private ApplicationResponse mResponse;
        private ThrowableHandler mThrowableHandler;
        
        public ThrowableCatcherContextImpl(ApplicationResponse response,
                                       ThrowableHandler throwableHandler) {
            mResponse = response;
            mThrowableHandler = throwableHandler;
        }
        
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
