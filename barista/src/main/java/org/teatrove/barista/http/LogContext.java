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

package org.teatrove.barista.http;

import java.io.InputStream;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.Enumeration;
import java.net.URL;
import java.net.MalformedURLException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;

/**
 * LogContext redirects log methods to a local Log and also provides a way
 * to get at the Barista-specific Log via an attribute, "org.teatrove.trove.log.Log".
 *
 * @author Brian S O'Neill
 */
class LogContext implements ServletContext {
    private final ServletContext mContext;
    private final Log mLog;
    
    LogContext(ServletContext context, Log log) {
        mContext = context;
        mLog = log;
    }
    
    public void log(String message) {
        mLog.info(message);
    }
    
    public void log(String message, Throwable t) {
        mLog.logException(new LogEvent
                          (mLog, LogEvent.ERROR_TYPE, message, t));
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
    
    public String getInitParameter(String name) {
        return mContext.getInitParameter(name);
    }
    
    public Enumeration getInitParameterNames() {
        return mContext.getInitParameterNames();
    }
    
    
    public Object getAttribute(String name) {
        Object attr = mContext.getAttribute(name);
        
        if (attr == null) {
            if ("org.teatrove.trove.log.Log".equals(name)) {
                return mLog;
            }
        }
        
        return attr;
    }
    
    public void setAttribute(String name, Object value) {
        mContext.setAttribute(name, value);
    }
    
    public void removeAttribute(String name) {
        mContext.removeAttribute(name);
    }
    
    public Enumeration getAttributeNames() {
        Set names = new HashSet();
        Enumeration enumeration = mContext.getAttributeNames();
        while (enumeration.hasMoreElements()) {
            names.add(enumeration.nextElement());
        }
        names.add("org.teatrove.trove.log.Log");
        return Collections.enumeration(names);
    }
    
    public ServletContext getContext(String uripath) {
        return mContext.getContext(uripath);
    }
    
    public RequestDispatcher getNamedDispatcher(String name) {
        return mContext.getNamedDispatcher(name);
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
    
    public URL getResource(String uripath) throws MalformedURLException {
        return mContext.getResource(uripath);
    }
    
    public InputStream getResourceAsStream(String uripath) {
        return mContext.getResourceAsStream(uripath);
    }
    
    public Set getResourcePaths(String path) {
        return mContext.getResourcePaths(path);
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
}
