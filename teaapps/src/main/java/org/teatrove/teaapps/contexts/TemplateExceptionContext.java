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
package org.teatrove.teaapps.contexts;

/**
 * Custom Tea context that allow the template to throw errors that may get
 * propogated to {@link ServletException} causing the given response to fail.
 * 
 * @author Scott Jappinen
 */
public class TemplateExceptionContext {

    /**
     * Throw a template exception with no message or cause.
     * 
     * @throws TemplateException the generated error
     */
	public void throwTemplateException() 
	    throws TemplateException {
	    
		throw new TemplateException();
	}

	/**
	 * Throw a template exception with the given message.
	 * 
	 * @param message The message to include in the exception
	 * 
	 * @throws TemplateException the generated error
	 */
    public void throwTemplateException(String message) 
        throws TemplateException {
        
        throw new TemplateException(message);
    }
    
    /**
     * Throw a template exception with the given cause.
     * 
     * @param cause The cause to include in the exception
     * 
     * @throws TemplateException the generated error
     */
    public void throwTemplateException(Throwable cause) 
        throws TemplateException {
        
        throw new TemplateException(cause);
    }
    
    /**
     * Throw a template exception with the given message and cause.
     * 
     * @param message The message to include in the exception
     * @param cause The cause to include in the exception
     * 
     * @throws TemplateException the generated error
     */
    public void throwTemplateException(String message, Throwable cause) 
        throws TemplateException {
        
        throw new TemplateException(message, cause);
    }
}
