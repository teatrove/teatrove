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

import java.util.Enumeration;
import java.util.Set;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;

/**
 * A ServletContext wrapper that passes all calls to an internal
 * ServletContext. This class is designed for subclasses to override or
 * hook into the behavior of a ServletContext instance.
 *
 * @author Brian S O'Neill
 */
public class FilteredServletContext implements ServletContext {
    protected final ServletContext mContext;

    public FilteredServletContext(ServletContext context) {
        mContext = context;
    }

    public void log(String message) {
        mContext.log(message);
    }

    public void log(String message, Throwable t) {
        mContext.log(message, t);
    }

    public String getRealPath(String path) {
        return mContext.getRealPath(path);
    }

    public String getMimeType(String file) {
        return mContext.getMimeType(file);
    }

    public String getServerInfo() {
        return mContext.getServerInfo();
    }

    public Object getAttribute(String name) {
        return mContext.getAttribute(name);
    }

    public void setAttribute(String name, Object value) {
        mContext.setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        mContext.removeAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return mContext.getAttributeNames();
    }

    public ServletContext getContext(String uripath) {
        return mContext.getContext(uripath);
    }

    public RequestDispatcher getRequestDispatcher(String uripath) {
        return mContext.getRequestDispatcher(uripath);
    }

    public int getMajorVersion() {
        return mContext.getMajorVersion();
    }

    public int getMinorVersion() {
        return mContext.getMinorVersion();
    }

    public Set getResourcePaths(String path) {
        return mContext.getResourcePaths(path);
    }

    public URL getResource(String uripath) throws MalformedURLException {
        return mContext.getResource(uripath);
    }

    public InputStream getResourceAsStream(String uripath) {
        return mContext.getResourceAsStream(uripath);
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return mContext.getNamedDispatcher(name);
    }

    public Enumeration getInitParameterNames() {
        return mContext.getInitParameterNames();
    }

    public String getInitParameter(String name) {
        return mContext.getInitParameter(name);
    }

    public String getServletContextName() {
        return mContext.getServletContextName();
    }

    /**
     * @deprecated
     */
    public void log(Exception e, String message) {
        log(message, e);
    }

    /**
     * @deprecated
     */
    public Servlet getServlet(String name) throws ServletException {
        return mContext.getServlet(name);
    }

    /**
     * @deprecated
     */
    public Enumeration getServlets() {
        return mContext.getServlets();
    }

    /**
     * @deprecated
     */
    public Enumeration getServletNames() {
        return mContext.getServletNames();
    }

    public String getContextPath() {
        return mContext.getContextPath();
    }
}
