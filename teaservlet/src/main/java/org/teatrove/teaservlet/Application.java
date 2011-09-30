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

package org.teatrove.teaservlet;

import javax.servlet.ServletException;

/**
 * The main hook into the TeaServlet framework. Implement this interface for
 * instantiating other required components and for providing functions to
 * templates.
 *
 * @author Reece Wilton
 */
public interface Application {
    /**
     * Initializes resources used by the Application.
     *
     * @param config the application's configuration object
     */
    public void init(ApplicationConfig config) throws ServletException;
    
    /**
     * Called by the TeaServlet when the application is no longer needed.
     */
    public void destroy();

    /**
     * Creates a context, which defines functions that are callable by
     * templates. Any public method in the context is a callable function,
     * except methods defined in Object. A context may receive a request and
     * response, but it doesn't need to use any of them. They are provided only
     * in the event that a function needs access to these objects.
     * <p>
     * Unless the getContextType method returns null, the createContext method
     * is called once for every request to the TeaServlet, so context creation
     * should have a fairly quick initialization. One way of accomplishing this
     * is to return the same context instance each time. The drawback to this
     * technique is that functions will not be able to access the current
     * request and response.
     * <p>
     * The recommended technique is to construct a new context that simply
     * references this Application and any of the passed in parameters. This
     * way, the Application contains all the resources and "business logic",
     * and the context just provides templates access to it.
     *
     * @param request the client's HTTP request
     * @param response the client's HTTP response
     * @return an object context for the templates
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response);

    /**
     * The class of the object that the createContext method will return, which
     * does not need to implement any special interface or extend any special
     * class. Returning null indicates that this Application defines no
     * context, and createContext will never be called.
     *
     * @return the class that the createContext method will return
     */
    public Class getContextType();
}
