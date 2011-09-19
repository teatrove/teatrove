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

import java.io.*;
import java.util.*;
import javax.servlet.*;
import org.teatrove.trove.log.*;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.barista.util.*;

/**
 * A ServletPool wraps instances of a single type of Servlet so that they
 * may be used and recycled. All Servlet instances have access to a special
 * Barista-specific attribute named "org.teatrove.trove.log.Log" which references a
 * Log instance for the Servlet.
 *
 * @author Brian S O'Neill
 */
public class ServletPool {
    private ServletContext mContext;
    private ServletConfig mServletConfig;
    private ClassLoader mClassLoader;

    private Config mConfig;
    private Class mServletClass;
    private Map mInitParameters;
    private File mDirectoryRoot;

    private Log mLog;
    private boolean mIsSingleThreadModel;
    
    // Instances are accessed like a stack.
    private LinkedList mServletInstances;

    private String mServletInfo;

    /**
     * Must call init to fully initialize ServletPool.
     */
    public ServletPool(ServletContext context, ClassLoader classLoader) {
        mContext = context;
        mClassLoader = classLoader;
    }

    // Required dispatcher methods.

    /**
     * Reads the following property groups and properties:
     *
     * <ul>
     * <li>class (required Servlet class)
     * <li>init (optional map of initialization parameters to pass to new
     *           Servlet instances)
     * <li>directory.root (optional root directory for supporting
     *                     ServletContext.getRealPath)
     * </ul>
     *
     * Initializing with null implies that this ServletPool is being removed
     * from service.
     */
    public synchronized void init(Config config)
        throws ClassNotFoundException, ClassCastException, LinkageError
    {
        if (config == null) {
            destroy(mServletInstances);
            mServletInstances = null;
            return;
        }

        mConfig = config;
        mLog = config.getLog();
        mServletConfig = new ServletConfigImpl(mContext, mLog);

        PropertyMap properties = config.getProperties();

        String className = properties.getString("class");
        if (className == null || className.length() == 0) {
            if (mServletClass == null) {
                throw new ClassNotFoundException
                    ("Servlet class name not specified");
            }
            else {
                className = null;
            }
        }

        if (className != null) {
            if (mClassLoader == null) {
                mServletClass = Class.forName(className);
            }
            else {
                mServletClass = mClassLoader.loadClass(className);
            }
        }

        if (!Servlet.class.isAssignableFrom(mServletClass)) {
            throw new ClassCastException
                ("Class is not a Servlet: " + mServletClass);
        }

        mIsSingleThreadModel =
            SingleThreadModel.class.isAssignableFrom(mServletClass);

        if (properties.subMapKeySet().contains("init")) {
            mInitParameters = properties.subMap("init");
        }

        if (mInitParameters == null) {
            mInitParameters = Utils.EMPTY_MAP;
        }

        String root = properties.getString("directory.root");
        if (root != null) {
            File rootDir;
            try {
                rootDir = new File(root).getCanonicalFile();

                if (!rootDir.exists()) {
                    mLog.warn("Directory doesn't exist: " + rootDir);
                }
                else if (!rootDir.isDirectory()) {
                    mLog.warn("Not a directory: " + rootDir);
                }
                else if (!rootDir.canRead()) {
                    mLog.warn("Unable to read from: " + rootDir);
                }
                else if (rootDir.isHidden()) {
                    mLog.warn("Directory is hidden: " + rootDir);
                }
                else {
                    mDirectoryRoot = rootDir;
                }
            }
            catch (IOException e) {
                mLog.warn(e);
            }
        }

        destroy(mServletInstances);
        mServletInstances = new LinkedList();

        if (properties.getBoolean("preload", false)) {
            try {
                preload();
            }
            catch (ServletException e) {
                Throwable t = e.getRootCause();
                if (t != null) {
                    if (e.getMessage() != null) {
                        mLog.error(e.getMessage());
                    }
                    mLog.error(t);
                }
                else {
                    mLog.error(e);
                }
            }
            catch (Exception e) {
                mLog.error(e);
            }
        }
    }

    public Config getConfig() {
        return mConfig;
    }

    public String getName() {
        return mLog.getName();
    }

    public String getDescription() {
        return mServletInfo == null ? "" : mServletInfo;
    }

    // Additional methods.

    public Class getServletClass() {
        return mServletClass;
    }

    public boolean isSingleThreadModel() {
        return mIsSingleThreadModel;
    }

    public Map getInitParameters() {
        return mInitParameters;
    }

    public File getDirectoryRoot() {
        return mDirectoryRoot;
    }

    /**
     * Retrieve a Servlet instance from the pool.
     */
    public synchronized Servlet get() throws ServletException {
        if (mServletInstances.size() == 0) {
            try {
                createServlet();
            }
            catch (InstantiationException e) {
                throw new ServletException(e);
            }
            catch (IllegalAccessException e) {
                throw new ServletException(e);
            }
        }

        // If servlet is multi-thread aware, always return the same instance.
        // Otherwise, new instances will need to be created and pooled.

        if (!mIsSingleThreadModel) {
            return (Servlet)mServletInstances.getLast();
        }
        else {
            return (Servlet)mServletInstances.removeLast();
        }
    }

    /**
     * Put a Servlet instance back into the pool.
     */
    public synchronized void put(Servlet servlet) {
        // If servlet is multi-thread aware, no need to put back into stack
        // since it was never removed from it.

        if (servlet == null || !mIsSingleThreadModel) {
            return;
        }

        if (mServletClass.isInstance(servlet)) {
            mServletInstances.addLast(servlet);
        }
        else {
            servlet.destroy();
        }
    }

    /**
     * Ensures that at least one servlet is instantiated and initialized.
     */
    public void preload() throws ServletException {
        Servlet servlet = null;
        try {
            servlet = get();
        }
        finally {
            put(servlet);
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        destroy(mServletInstances);
    }

    private void destroy(final LinkedList servletInstances) {
        if (servletInstances == null || servletInstances.size() == 0) {
            return;
        }

        Iterator it = servletInstances.iterator();
        while (it.hasNext()) {
            Servlet servlet = (Servlet)it.next();
            try {
                servlet.destroy();
            }
            catch (Exception e) {
                mLog.error("Error destroying servlet: " + getName());
                mLog.error(e);
            }
        }
    }

    private void createServlet()
        throws IllegalAccessException, InstantiationException, ServletException
    {
        Servlet servlet = (Servlet)mServletClass.newInstance();
        servlet.init(mServletConfig);
        mServletInfo = servlet.getServletInfo();
        mServletInstances.addLast(servlet);
    }

    private class ServletConfigImpl implements ServletConfig {
        private final ServletContext mContext;
     
        public ServletConfigImpl(ServletContext context, Log log) {
            mContext = new LogContext(context, log);
        }
   
        public ServletContext getServletContext() {
            return mContext;
        }
        
        public String getInitParameter(String name) {
            try {
                return (String)mInitParameters.get(name);
            }
            catch (ClassCastException e) {
                return null;
            }
        }
        
        public Enumeration getInitParameterNames() {
            return Collections.enumeration(mInitParameters.keySet());
        }
        
        public String getServletName() {
            return getName();
        }
    }
}
