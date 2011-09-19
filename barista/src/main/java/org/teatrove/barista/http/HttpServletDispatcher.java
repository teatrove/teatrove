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
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.trove.net.CheckedSocketException;
import org.teatrove.trove.net.CheckedInterruptedIOException;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.util.*;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.Transaction;
import org.teatrove.trove.util.tq.UncaughtExceptionListener;
import org.teatrove.trove.util.tq.UncaughtExceptionEvent;
import org.teatrove.barista.util.*;
import org.teatrove.trove.util.PatternMatcher;

/**
 * @author Brian S O'Neill
 */
public class HttpServletDispatcher
    implements ServletContext, HttpHandlerStage
{
    private static final String ATTR_REQUEST_URI =
        "javax.servlet.include.request_uri";
    private static final String ATTR_CONTEXT_PATH =
        "javax.servlet.include.context_path";
    private static final String ATTR_SERVLET_PATH =
        "javax.servlet.include.servlet_path";
    private static final String ATTR_PATH_INFO =
        "javax.servlet.include.path_info";
    private static final String ATTR_QUERY_STRING =
        "javax.servlet.include.query_string";

    private static String decodeURI(String uri) {
        if (uri.indexOf('%') >= 0 || uri.indexOf('+') >= 0) {
            try {
                return java.net.URLDecoder.decode(uri);
            }
            catch (Exception e) {
            }
        }
        return uri;
    }

    private Map mServletConfigs;
    private Map mFilterConfigs;

    // Maps ServletPools to TransactionQueues.
    private Map mServletPoolQueues;

    private Config mConfig;
    private Log mLog;

    // Maps names to ServletPools.
    private UsageMap mNamedServletMap;
    // Maps path patterns to ServletPools. Not all servlets are mapped, and
    // some may be mapped multiple times.
    private PatternMatcher mServletMatcher;

    // Maps names to Filters.
    private UsageMap mNamedFilterMap;
    // Maps path patterns to Filters. Not all filters are mapped, and some may
    // be mapped multiple times.
    private PatternMatcher mFilterMatcher;

    private UncaughtExceptionListener mUncaughtExceptionListener;
    private Map mAttributes;
    private Vector mContextAttributeListeners;
    private Vector mContextListeners;

    private Map mCharsetAliases;

    /**
     * The servletConfigs parameter maps Servlet names to Config objects. The
     * configuration properties contain the following:
     * <ul>
     * <li>class (required Servlet class)
     * <li>init (optional map of initialization parameters to pass to new
     *           Servlet instances)
     * <li>directory.root (optional root directory for supporting
     *                     ServletContext.getRealPath)
     * </ul>
     * <p>
     * The filterConfigs parameter maps Filter names to Config objects. The
     * configuration properties contain the following:
     * <ul>
     * <li>class (required Filter class)
     * <li>init (optional map of initialization parameters to pass to new
     *           Filter instances)
     * </ul>
     * <p>
     * HttpServletDispatcher must be {@link #init(Config) initialized} before
     * it can be used. Servlets will be initialized in the order they are
     * passed in here. Mapped servlets are also preloaded. Filters are
     * initialized in the order they are passed in here, and this order also
     * determines the chain sequence.
     */
    public HttpServletDispatcher(Map servletConfigs,
                                 Map filterConfigs) {
        mServletConfigs = servletConfigs;
        mFilterConfigs = filterConfigs;
        mServletPoolQueues = new IdentityMap();
    }

    public void init(HttpHandlerStage.Config config) {
        if (config == null) {
            destroy();
        }
        else {
            init(new DefaultConfig(config));
        }
    }

    public synchronized void init(Config config) {
        if (config == null) {
            destroy();
            return;
        }

        mConfig = config;
        mLog = config.getLog();

        mUncaughtExceptionListener = new UncaughtExceptionListener() {
            public void uncaughtException(UncaughtExceptionEvent event) {
                Throwable e = event.getException();
                mLog.logException(new LogEvent(mLog, LogEvent.ERROR_TYPE, e,
                                               event.getThread()));
            }
        };

        // Load and map Servlets.

        mNamedServletMap = new UsageMap();
        mNamedServletMap.setReverseOrder(true);

        Iterator it = mServletConfigs.keySet().iterator();
        while (it.hasNext()) {
            String name = (String)it.next();
            loadServletPool
                (name, (org.teatrove.barista.util.Config)mServletConfigs.get(name));
        }

        Map servletMap = config.getServletMap();
        Map patternMap = new HashMap();

        // Now map Servlets.

        it = servletMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String pattern = (String)entry.getKey();
            String name = (String)entry.getValue();

            mLog.info("Mapping servlet \"" + name +
                      "\" by \"" + pattern + '\"');

            ServletPool pool = (ServletPool)mNamedServletMap.get(name);

            if (pool == null) {
                mLog.error("No servlet found named: " + name);
            }
            else {
                patternMap.put(pattern, pool);
            }
        }

        addSpecialPatterns(patternMap);

        if (!patternMap.containsKey("*")) {
            // Add default "catch-all" servlet. This allows filters to run
            // even if no matching servlet has been explicitly provided.
            ServletPool pool = new ServletPool(this, mConfig.getClassLoader());
            PropertyMap props = new PropertyMap();
            String className = NotFoundServlet.class.getName();
            props.put("class", className);
            org.teatrove.barista.util.Config servletConfig =
                new DerivedConfig(mConfig, className, props);
            try {
                pool.init(servletConfig);
                patternMap.put("*", pool);
            }
            catch (ClassNotFoundException e) {
                throw new InternalError(e.toString());
            }
        }

        mServletMatcher = PatternMatcher.forPatterns(patternMap);

        // Preload mapped servlets after they have been loaded. This ensures
        // that calls to getRealPath from the servlet's init method will work.

        it = servletMap.values().iterator();
        while (it.hasNext()) {
            String name = (String)it.next();
            try {
                ServletPool pool = ((ServletPool)mNamedServletMap.get(name));
                if (pool != null) {
                    pool.preload();
                }
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

        // Load and map Filters.

        mNamedFilterMap = new UsageMap();
        mNamedFilterMap.setReverseOrder(true);

        Map filterMap = config.getFilterMap();
        patternMap = new HashMap();

        it = mFilterConfigs.keySet().iterator();
        int order = 0;
        while (it.hasNext()) {
            String name = (String)it.next();
            loadFilter(name,
                       (org.teatrove.barista.util.Config)mFilterConfigs.get(name),
                       order++);
        }

        // Now map filters.

        it = filterMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String pattern = (String)entry.getKey();
            String names = (String)entry.getValue();

            StringTokenizer st = new StringTokenizer(names, ",;");
            while (st.hasMoreTokens()) {
                String name = st.nextToken().trim();

                mLog.info("Mapping filter \"" + name +
                          "\" by \"" + pattern + '\"');

                Filter filter = (Filter)mNamedFilterMap.get(name);

                if (filter == null) {
                    mLog.error("No filter found named: " + name);
                    continue;
                }

                Object existing = patternMap.get(pattern);
                if (existing == null) {
                    patternMap.put(pattern, filter);
                }
                else if (existing instanceof List) {
                    ((List)existing).add(filter);
                }
                else {
                    List filters = new ArrayList();
                    filters.add((Filter)existing);
                    filters.add(filter);
                    patternMap.put(pattern,filters);
                }
            }
        }

        addSpecialPatterns(patternMap);
        mFilterMatcher = PatternMatcher.forPatterns(patternMap);
        
        // ContextListener and ContextAttributeListener setup
        PropertyMap listenerProps = config.getContextListenerProperties();
        Iterator listenToIt = listenerProps.subMapKeySet().iterator();
        while (listenToIt.hasNext()) {
            String listenerName = listenToIt.next().toString();
            try {
                String listenerClassName = 
                    listenerProps.getString(listenerName + ".class");
                if (listenerClassName != null && listenerClassName.length() > 0) {
                    Class listenerClass = Class.forName(listenerClassName);
                    Object listenerObj = listenerClass.newInstance();
                    boolean added = false;
                    if (listenerObj instanceof 
                        ServletContextAttributeListener) {
                        if (mContextAttributeListeners == null) {
                            mContextAttributeListeners = new Vector();
                        }
                        mContextAttributeListeners.add(listenerObj);
                        added = true;
                    }
                    if (listenerObj instanceof 
                        ServletContextListener) {
                        if (mContextListeners == null) {
                            mContextListeners = new Vector();
                        }
                        mContextListeners.add(listenerObj);
                        added = true;
                    }
                    if (!added) {
                        System.err.println
                            ("Listener is not an instance of " +
                             "ServletContextListener or " +
                             "ServletContextAttributeListener: " +
                             listenerClass);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // notify listeners that context is now initialized
        if (mContextListeners != null && mContextListeners.size() > 0) {
            Iterator initIt = mContextListeners.iterator();
            ServletContextEvent event = new ServletContextEvent(this);
            while (initIt.hasNext()) {
                ((ServletContextListener)initIt.next()).contextInitialized(event);
            }
        }

        // set required temporary directory attribute.
        setAttribute("javax.servlet.context.tempdir", config.getTempDir());

        mCharsetAliases = config.getCharsetAliases();
    }

    private void addSpecialPatterns(Map patternMap) {
        String[] patterns = 
            (String[])patternMap.keySet().toArray(new String[patternMap.size()]);

        String special;

        for (int i=0; i<patterns.length; i++) {
            String pattern = patterns[i];
            if (pattern.endsWith("/*")) {
                special = pattern.substring(0, pattern.length() - 2);
                if (!patternMap.containsKey(special)) {
                    patternMap.put(special, patternMap.get(pattern));
                }
            }
            /* This special pattern is pretty annoying. Screw the spec.
            else if (pattern.equals("/")) {
                special = "/*";
                if (!patternMap.containsKey(special)) {
                    patternMap.put(special, patternMap.get(pattern));
                }
            }
            */
        }
    }

    public SessionInfo[] getSessionInfos() {
        return ((Config)mConfig).getSessionStrategy().getSessionInfos();
    }

    public HttpHandlerStage.Config getConfig() {
        return mConfig;
    }

    public boolean handle(HttpServerConnection con, 
                          HttpHandlerStage.Chain chain)
    {
        HttpServletRequestImpl request = new HttpServletRequestImpl(con);
        HttpServletResponseImpl response =
            new HttpServletResponseImpl(con, mCharsetAliases);

        request.setSessionSupport
            (mConfig.getSessionStrategy().createSupport(request, response));
        
        // Have the request set this as its ServletContext.
        request.setServletContext(this);
        
        String uri = decodeURI(request.getRequestURI());

        PatternMatcher.Result matchingServlet = mServletMatcher.getMatch(uri);
        if (matchingServlet == null) {
            sendError(request, response, response.SC_NOT_FOUND, uri);
            return false;
        }

        String[] pathBuckets = {"", uri, null};
        splitMatchedURI(matchingServlet, pathBuckets);
        request.setContextPath(pathBuckets[0]);
        request.setServletPath(pathBuckets[1]);
        request.setPathInfo(pathBuckets[2]);

        ServletPool pool = (ServletPool)matchingServlet.getValue();

        ServletFilterChainTransaction tran = new ServletFilterChainTransaction
            (createFilterChain(uri), pool, request, response);

        try {
            TransactionQueue queue;
            Servlet servlet = tran.getServlet();
            if (servlet instanceof TQServlet) {
                queue = ((TQServlet)servlet).selectQueue(request);
                if (queue == null) {
                    queue = selectDefaultQueue(pool);
                }
            }
            else {
                queue = selectDefaultQueue(pool);
            }
            if (queue.enqueue(tran)) {
                return true;
            }
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

        try {
            tran.cancel();
        }
        catch (SocketException e) {
            //mLog.debug(e.toString());
        }
        catch (IOException e) {
            mLog.warn(e);
        }
        catch (Exception e) {
            mLog.error(e);
        }

        return false;
    }

    // Required ServletContext methods.

    public void log(String message) {
        mLog.info(message);
    }

    public void log(String message, Throwable t) {
        mLog.logException(new LogEvent(mLog, LogEvent.ERROR_TYPE, message, t));
    }

    public String getRealPath(String uri) {
        uri = decodeURI(uri);

        // This method accepts a URI, but some servlets (Jasper) think that
        // '\' is a valid URI separator.
        char separator = File.separatorChar;
        if (separator != '/' && uri.indexOf(separator) >= 0) {
            uri = uri.replace(separator, '/');
        }

        PatternMatcher.Result entry;
        synchronized (this) {
            entry = mServletMatcher.getMatch(uri);
        }

        if (entry == null) {
            return null;
        }

        ServletPool pool = (ServletPool)entry.getValue();
        if (pool == null) {
            return null;
        }

        File rootDir = pool.getDirectoryRoot();
        if (rootDir == null) {
            return null;
        }

        String[] pathBuckets = {"", uri, null};
        splitMatchedURI(entry, pathBuckets);
        if (pathBuckets[2] == null) {
            // If no pathInfo, use servletPath instead.
            if (pathBuckets[1] != null) {
                uri = pathBuckets[1];
            }
            else {
                return null;
            }
        }
        else {
            uri = pathBuckets[2];
        }
        
        try {
            File realFile = new File(rootDir, uri).getCanonicalFile();

            // Security check. Make sure the canonical file is still in the
            // root directory. I do an additional check for ".." patterns in
            // the canonical file because of a defect that incorrectly converts
            // the pattern ".../..." to "..\.." on win32, which is a security
            // hole.
            String realFilePath = realFile.getPath();
            if (realFilePath.indexOf("..") >= 0 || 
                !realFilePath.startsWith(rootDir.getPath())) {

                return null;
            }

            return realFilePath;
        }
        catch (IOException e) {
            return null;
        }
    }

    public synchronized String getMimeType(String file) {
        if (file == null) {
            return null;
        }

        String ext;
        int index = file.lastIndexOf('.');
        if (index < 0) {
            ext = file;
        }
        else {
            ext = file.substring(index + 1);
        }
        
        return (String)mConfig.getMimeTypeMap().get(ext);
    }

    public String getInitParameter(String name) {
        return (String)mConfig.getProperties().get(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(mConfig.getProperties().keySet());
    }

    public String getServerInfo() {
        return mConfig.getServerInfo();
    }

    public String getServletContextName() {
        return mLog.getName();
    }

    public synchronized Object getAttribute(String name) {
        if (mAttributes == null) {
            return null;
        }
        else {
            return mAttributes.get(name);
        }
    }

    public synchronized void setAttribute(String name, Object value) {
        if (mAttributes == null) {
            mAttributes = new HashMap(7);
        }

        Object old = mAttributes.put(name, value);
        
        // Attribute Listener stuff from API 2.3
        if (mContextAttributeListeners != null 
            && mContextAttributeListeners.size() > 0) { 
            Iterator invalidateIt = mContextAttributeListeners.iterator();
            while (invalidateIt.hasNext()) {
                ServletContextAttributeListener attrListener = 
                    (ServletContextAttributeListener)invalidateIt.next();
                if (old == null) {
                    attrListener
                        .attributeAdded
                        (new ServletContextAttributeEvent(this, name, value));
                }
                else {
                    attrListener
                        .attributeReplaced
                        (new ServletContextAttributeEvent(this, name, old));
                }
            }
        }
    }

    public synchronized void removeAttribute(String name) {
        if (mAttributes != null) {
            Object removed = mAttributes.remove(name);
        
            // Attribute Listener stuff from API 2.3
            if (mContextAttributeListeners != null 
                && mContextAttributeListeners.size() > 0) { 
                Iterator invalidateIt = mContextAttributeListeners.iterator();
                while (invalidateIt.hasNext()) {
                    ((ServletContextAttributeListener)invalidateIt.next())
                        .attributeRemoved
                        (new ServletContextAttributeEvent(this, name,removed));
                }
            }
        }
    }

    public synchronized Enumeration getAttributeNames() {
        if (mAttributes == null) {
            return Utils.EMPTY_ENUMERATION;
        }
        else {
            return Collections.enumeration(mAttributes.keySet());
        }
    }

    public ServletContext getContext(String uripath) {
        // Don't return the specific context used by the actual servlet.
        // Otherwise, one servlet can spoof logs from another.
        return this;
    }

    public synchronized RequestDispatcher getNamedDispatcher(String name) {
        // Can't run any filters since no URI is provided.
        return new ServletRequestDispatcher
            ((ServletPool)mNamedServletMap.get(name));
    }

    public synchronized RequestDispatcher getRequestDispatcher(String uri) {
        if (uri == null) {
            return null;
        }

        // Now separate the query string from the uri
        String query = null;
        int index = uri.indexOf('?');
        if (index >= 0) {
            if (index + 1 < uri.length()) {
                query = uri.substring(index + 1);
            }
            else {
                query = "";
            }
            uri = uri.substring(0, index);
        }
        
        uri = decodeURI(uri);

        PatternMatcher.Result matchingServlet = mServletMatcher.getMatch(uri);
        if (matchingServlet == null) {
            return null;
        }

        String[] pathBuckets = {"", uri, null};
        splitMatchedURI(matchingServlet, pathBuckets);

        ServletPool pool = (ServletPool)matchingServlet.getValue();

        Filter[] filters = createFilterChain(uri);

        return new ServletRequestDispatcher
            (filters, pool, uri, pathBuckets[0], pathBuckets[1], pathBuckets[2], query);
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 3;
    }

    /**
     * @return an empty set since this isn't really implemented.
     * I think this could provide a security hole if you tell
     * the whole world where to find your resources.
     */
    public Set getResourcePaths(String path) {
        return Collections.EMPTY_SET;
    }

    public URL getResource(String uripath) throws MalformedURLException {
        String realPath = getRealPath(uripath);
        return realPath == null ? null : new URL("file:" + realPath);
    }

    public InputStream getResourceAsStream(String uripath) {
        String realPath = getRealPath(uripath);
        try {
            return realPath == null ? null : new FileInputStream(realPath);
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * @deprecated
     */
    public void log(Exception e, String message) {
        log(message, e);
    }

    /**
     * @deprecated Null is always returned
     */
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    /**
     * @deprecated An empty enumeration is always returned
     */
    public Enumeration getServlets() {
        return org.teatrove.trove.util.Utils.EMPTY_ENUMERATION;
    }

    /**
     * @deprecated An empty enumeration is always returned
     */
    public Enumeration getServletNames() {
        return org.teatrove.trove.util.Utils.EMPTY_ENUMERATION;
    }

    /**
     * Returns all the servlet pools in the order that they were initialized.
     */
    public ServletPool[] getServletPools() {
        return (ServletPool[])mNamedServletMap.values().toArray(new ServletPool[0]);
    }

    /**
     * Returns all the Filters in the order that they were initialized.
     */
    public Filter[] getFilters() {
        return (Filter[])mNamedFilterMap.values().toArray(new Filter[0]);
    }

    /**
     * Returns all the TransactionQueues used by servlets.
     */
    public synchronized TransactionQueue[] getTransactionQueues() {
        List list = new ArrayList();
        list.addAll(mServletPoolQueues.values());

        ServletPool[] pools = getServletPools();
        for (int i=0; i<pools.length; i++) {
            if (TQServlet.class.isAssignableFrom(pools[i].getServletClass())) {
                TransactionQueue[] more;
                try {
                    Servlet s = pools[i].get();
                    try {
                        more = ((TQServlet)s).getTransactionQueues();
                    }
                    finally {
                        pools[i].put(s);
                    }

                    for (int j=0; j<more.length; j++) {
                        list.add(more[j]);
                    }
                }
                catch (ServletException e) {
                    mLog.warn(e);
                }
            }
        }

        TransactionQueue[] queues = new TransactionQueue[list.size()];
        return (TransactionQueue[])list.toArray(queues);
    }

    /**
     * Returns the transaction queues for the given servlet.
     */
    public synchronized TransactionQueue[] getTransactionQueues(String servlet)
    {
        ServletPool pool = (ServletPool)mNamedServletMap.get(servlet);
        if (pool == null) {
            return new TransactionQueue[0];
        }

        List list = new ArrayList();
        Object queue = mServletPoolQueues.get(pool);
        if (queue != null) {
            list.add(queue);
        }

        if (TQServlet.class.isAssignableFrom(pool.getServletClass())) {
            TransactionQueue[] more;
            try {
                Servlet s = pool.get();
                try {
                    more = ((TQServlet)s).getTransactionQueues();
                }
                finally {
                    pool.put(s);
                }

                for (int j=0; j<more.length; j++) {
                    list.add(more[j]);
                }
            }
            catch (ServletException e) {
                mLog.warn(e);
            }
        }

        TransactionQueue[] queues = new TransactionQueue[list.size()];
        return (TransactionQueue[])list.toArray(queues);
    }

    /*
     * used to notify the ServletContextListener that the context has been 
     * destroyed.
     */
    protected void finalize() {
        if (mContextListeners != null && mContextListeners.size() > 0) {
            Iterator it = mContextListeners.iterator();
            ServletContextEvent event = new ServletContextEvent(this);
            while (it.hasNext()) {
                ((ServletContextListener)it.next()).contextDestroyed(event);
            }
        }
    }

    private ServletPool loadServletPool(String name,
                                        org.teatrove.barista.util.Config config)
    {
        ServletPool pool = (ServletPool)mNamedServletMap.get(name);
        if (pool != null) {
            return pool;
        }

        try {
            pool = new ServletPool(this, mConfig.getClassLoader());
            pool.init(config);
        }
        catch (Exception e) {
            mLog.error(e);
            return null;
        }
        catch (LinkageError e) {
            mLog.error(e);
            return null;
        }
        
        mNamedServletMap.put(name, pool);

        return pool;
    }

    private Filter loadFilter(String name,
                              org.teatrove.barista.util.Config config, int order) {
        Filter filter = (Filter)mNamedFilterMap.get(name);
        if (filter != null) {
            return filter;
        }

        PropertyMap props = config.getProperties();

        try {
            ClassLoader loader = mConfig.getClassLoader();
            Class filterClass;
            if (loader == null) {
                filterClass = Class.forName(props.getString("class"));
            }
            else {
                filterClass = loader.loadClass(props.getString("class"));
            }
            
            filter = (Filter)filterClass.newInstance();

            // Make the filter sortable, and also provide compatibility with
            // removed filter methods.
            filter = new SortableFilter(filter, order);

            filter.init(new FilterConfigImpl
                (config.getLog(), props.subMap("init"), this));
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
            return null;
        }
        catch (Exception e) {
            mLog.error(e);
            return null;
        }
        catch (LinkageError e) {
            mLog.error(e);
            return null;
        }

        mNamedFilterMap.put(name, filter);
        return filter;
    }

    /**
     * Determine the contextPath, servletPath, and pathInfo.
     *
     * <pre>
     * pattern | contextPath | servletPath | pathInfo
     * --------+-------------+-------------+---------
     *  *      | No          | Yes  ""     | Yes  *
     *  *j     | No          | Yes  *j     | No
     *  *j*    | No          | Yes  *j     | Yes  *
     *  i*     | No          | Yes  i      | Yes  *
     *  i*j    | Yes  i      | Yes  *j     | No
     *  i*j*   | Yes  i      | Yes  *j     | Yes  *
     * </pre>
     *
     * @param pathBuckets upon entry, first entry is "", second is URI, and
     * third is null. Upon exit, first entry is contextPath, second is
     * servletPath, and third entry is pathInfo.
     */
    private void splitMatchedURI(PatternMatcher.Result result,
                                 String[] pathBuckets) {
        int[] wildcardPositions = result.getWildcardPositions();
 
        if (wildcardPositions.length == 0) {
            // Non-wildcard match; no changes.
            return;
        }

        String uri = pathBuckets[1];
        int index = wildcardPositions[0];

        if (index == 0) {
            // Pattern is '*', '*j', or '*j*'.
            if (wildcardPositions.length == 2) {
                // Pattern is '*' or '*j'.
                if (wildcardPositions[1] == uri.length()) {
                    // Pattern is '*'.
                    pathBuckets[1] = "";
                    pathBuckets[2] = uri;
                }
                else {
                    // Pattern is '*j'.
                    // No changes.
                }
            }
            else {
                // Pattern is '*j*'.
                index = wildcardPositions[2];
                if (index > 0 && uri.charAt(index - 1) == '/') {
                    // Ensure pathInfo starts with '/'.
                    index--;
                }
                pathBuckets[1] = uri.substring(0, index);
                pathBuckets[2] = uri.substring(index);
            }
        }
        else {
            // Pattern is 'i*', 'i*j', or 'i*j*'.
            if (uri.charAt(index - 1) == '/') {
                // Ensure pathInfo or servletPath starts with '/'.
                index--;
            }
            if (wildcardPositions.length == 2) {
                // Pattern is 'i*' or 'i*j'.
                if (wildcardPositions[1] == uri.length()) {
                    // Pattern is 'i*'.
                    pathBuckets[1] = uri.substring(0, index);
                    pathBuckets[2] = uri.substring(index);
                }
                else {
                    // Pattern is 'i*j'.
                    pathBuckets[0] = uri.substring(0, index);
                    pathBuckets[1] = uri.substring(index);
                }
            }
            else {
                // Pattern is 'i*j*'.
                pathBuckets[0] = uri.substring(0, index);
                int index2 = wildcardPositions[2];
                if (uri.charAt(index2 - 1) == '/') {
                    // Ensure pathInfo starts with '/'.
                    index2--;
                }
                pathBuckets[1] = uri.substring(index, index2);
                pathBuckets[2] = uri.substring(index2);
            }
        }
    }

    private String mergeQueryStrings(String originalQuery, String newQuery) {
        if (originalQuery == null || originalQuery.length() == 0) {
            return newQuery;
        }
        else if (newQuery == null || newQuery.length() == 0) {
            return originalQuery;
        }
        else {
            StringBuffer buf = new StringBuffer
                (newQuery.length() + 1 + originalQuery.length());
            buf.append(newQuery);
            buf.append('&');
            buf.append(originalQuery);
            return buf.toString();
        }
    }

    private synchronized TransactionQueue selectDefaultQueue(ServletPool pool)
    {
        TransactionQueue q = (TransactionQueue)mServletPoolQueues.get(pool);
        if (q == null) {
            q = new TransactionQueue
                (mConfig.getThreadPool(), pool.getName(), 10, 10);
            q.addUncaughtExceptionListener(mUncaughtExceptionListener);
            q.applyProperties(mConfig.getTransactionQueueProperties());
            mServletPoolQueues.put(pool, q);
        }
        return q;
    }

    private Filter[] createFilterChain(String uri) {
        PatternMatcher.Result[] matches =
            mFilterMatcher.getMatches(uri, Integer.MAX_VALUE);

        Filter[] filters;
        int filtersLength;
        if (matches == null || (filtersLength = matches.length) == 0) {
            filters = null;
        }
        else {
            if (filtersLength == 1) {
                Object obj = matches[0].getValue();
                if (obj instanceof Filter) {
                    filters = new Filter[]{(Filter)obj};
                }
                else {
                    Set set = new TreeSet();
                    set.addAll((List)obj);
                    filters = (Filter[])set.toArray(new Filter[set.size()]);
                }
            }
            else {
                // Stuff into a TreeSet to eliminate duplicates and to sort.
                // The assumption is that all the filters are wrapped in a
                // SortableFilter or are in a List.
                Set set = new TreeSet();
                for (int j=0; j<filtersLength; j++) {
                    Object obj = matches[j].getValue();
                    if (obj instanceof Filter) {
                        set.add(obj);
                    }
                    else {
                        set.addAll((List)obj);
                    }
                }
                filters = (Filter[])set.toArray(new Filter[set.size()]);
            }
        }

        return filters;
    }

    private void sendError(HttpServletRequest req, HttpServletResponse res,
                           int sc)
    {
        sendError(req, res, sc, null, null);
    }

    private void sendError(HttpServletRequest req, HttpServletResponse res,
                           int sc, String msg)
    {
        sendError(req, res, sc, msg, null);
    }

    private void sendError(HttpServletRequest req, HttpServletResponse res,
                           int sc, String msg, Throwable thrown)
    {
        // Set the Error forwarding Attributes.
        req.setAttribute("javax.servlet.error.status_code", new Integer(sc));
        req.setAttribute("javax.servlet.error.message", msg);
        req.setAttribute("javax.servlet.error.request_uri", 
                         req.getRequestURI());
        if (thrown != null) {
            req.setAttribute("javax.servlet.error.exception_type",
                             thrown.getClass());
            req.setAttribute("javax.servlet.error.exception", thrown);
        }

        try {
            res.sendError(sc, msg);
        }
        catch (IllegalStateException e) {
            if (res.isCommitted()) {
                String info = "Cannot send error " + sc +
                    " because response is committed";
                if (msg != null) {
                    info = info + ' ' + msg;
                }
                mLog.info(info);
            }
            else {
                mLog.warn(e);
            }
        }
        catch (SocketException e) {
            //mLog.debug(e.toString());
        }
        catch (IOException e) {
            mLog.warn(e);
        }
    }

    private void destroy() {
        // First, destroy all the servlets.
        ServletPool[] pools = getServletPools();
        for (int i=0; i<pools.length; i++) {
            try {
                pools[i].init(null);
            }
            catch (Exception e) {
                mLog.debug(e);
            }
        }

        Filter[] filters = getFilters();
        for (int i=0; i<filters.length; i++) {
            try {
                filters[i].destroy();
            }
            catch (Exception e) {
                mLog.debug(e);
            }
        }
    }

    public interface Config extends HttpHandlerStage.Config {
        public String getServerInfo();

        /**
         * Returns the ClassLoader that should be used for loading Servlets,
         * Filters, and HttpSessionStrategies. Return null to use system class
         * loader.
         */
        public ClassLoader getClassLoader();

        public HttpSessionStrategy getSessionStrategy();

        /**
         * Returns a mapping of file extensions to MIME types.
         */
        public Map getMimeTypeMap();

        /**
         * Returns default properties that should be applied to all servlet
         * TransactionQueues.
         */
        public PropertyMap getTransactionQueueProperties();

        /**
         * Returns properties for configuring ServletContextListener and 
         * ServletContextAttributesListener implementations.
         */
        public PropertyMap getContextListenerProperties();

        /**
         * Returns mappings of path patterns (with wildcards) to Servlet names.
         */
        public Map getServletMap();

        /**
         * Returns mappings of path patterns (with wildcards) to Filter names.
         */
        public Map getFilterMap();

        /**
         * Returns a temporary directory for this ServletContext
         * defaults to the temp dir of this process.
         */
        public File getTempDir();

        /**
         * Optionally provide mappings between HTTP charset codes and Java
         * character encodings. Java can already understand and convert many
         * charset codes to character encodings, but this allows additional
         * ones to be provided or existing ones to be overridden.
         *
         * @return Maps string charset codes to character encoding strings.
         */
        public Map getCharsetAliases();
    }

    public static class DefaultConfig extends ConfigWrapper implements Config {
        private final Log mImpressionLog;
        private final ClassLoader mClassLoader;
        private final HttpSessionStrategy mSessionStrategy;
        private final PropertyMap mQueueProperties;
        private final PropertyMap mListenerProperties;
        private final PropertyMap mProperties;

        private Map mMimeMap;

        public DefaultConfig(HttpHandlerStage.Config config) {
            super(config);
            mImpressionLog = config.getImpressionLog();
            PropertyMap props = getProperties();
            mProperties = props;
            mClassLoader =
                createServletClassLoader(props.getString("classpath"));
            mSessionStrategy =
                createSessionStrategy(props.subMap("session.strategy"));
            mQueueProperties = props.subMap("transactionQueue");
            mListenerProperties = props.subMap("context.listeners");
        }

        public Log getImpressionLog() {
            return mImpressionLog;
        }

        public String getServerInfo() {
            return Utils.getServerInfo();
        }
        
        public ClassLoader getClassLoader() {
            return mClassLoader;
        }

        public HttpSessionStrategy getSessionStrategy() {
            return mSessionStrategy;
        }

        public Map getMimeTypeMap() {
            if (mMimeMap != null) {
                return mMimeMap;
            }

            mMimeMap = new HashMap();
            InputStream in = getClass().getResourceAsStream("MIME.properties");

            try {
                PropertyParser parser = new PropertyParser(mMimeMap);
                parser.parse(new BufferedReader(new InputStreamReader(in)));
            }
            catch (Exception e) {
                if (in == null) {
                    getLog().warn("MIME.properties resource not found");
                }
                else {
                    getLog().error(e);
                }
            }

            return mMimeMap;
        }

        public PropertyMap getTransactionQueueProperties() {
            return mQueueProperties;
        }

        public PropertyMap getContextListenerProperties() {
            return mListenerProperties;
        }

        public Map getServletMap() {
            return getProperties().subMap("servletMap");
        }

        public Map getFilterMap() {
            return getProperties().subMap("filterMap");
        }

        public File getTempDir() {
            String tempFile = mProperties.getString("tempdir");
            if (tempFile == null) {
                tempFile = System.getProperty("java.io.tmpdir");
            }
            if (tempFile != null) {
                return new File(tempFile);            
            }
            return null;
        }

        /**
         * Reads the sub-properties under "charset.aliases".
         */
        public Map getCharsetAliases() {
            Map charsetAliases = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            charsetAliases.putAll(getProperties().subMap("charset.aliases"));
            return charsetAliases;
        }

        private ClassLoader createServletClassLoader(String classpath) {
            ClassLoader parent = getClass().getClassLoader();
            if (classpath != null) {
                StringTokenizer st = new StringTokenizer(classpath, " ,;:");
                List urls = new ArrayList();
                while (st.hasMoreTokens()) {
                    String path = st.nextToken();
                    path = path.replace(File.separatorChar, '/');
                    
                    if (!path.toLowerCase().endsWith(".jar")) {
                        path += '/';
                    }
                    
                    try {
                        urls.add(new URL(path));
                    }
                    catch (MalformedURLException e) {
                        try {
                            urls.add(new URL("file", null, -1, path));
                        }
                        catch (MalformedURLException e2) {
                            getLog().error(e.toString());
                        }
                    }
                }
                return new URLClassLoader
                    ((URL[])urls.toArray(new URL[urls.size()]), parent);
            }
            return parent;
        }

        private HttpSessionStrategy createSessionStrategy(PropertyMap props) {
            HttpSessionStrategy instance;
            try {
                String className = props.getString("class");
                if (className == null) {
                    className = BaristaSessionStrategy.class.getName();
                }

                Class clazz;
                if (mClassLoader == null) {
                    clazz = Class.forName(className);
                }
                else {
                    clazz = mClassLoader.loadClass(className);
                }
                
                instance = (HttpSessionStrategy)clazz.newInstance();
                instance.init(props.subMap("init"));
            }
            catch (Exception e) {
                getLog().error(e);
                getLog().info("Using the default session strategy instead.");
                instance = new BaristaSessionStrategy();
                instance.init(props.subMap("init"));
            }
            return instance;
        }
    }

    /**
     * Transaction that handles primary execution of a Servlet.
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision:--> 127 <!-- $-->, <!--$$JustDate:--> 03-11-05 <!-- $-->
     */
    private class ServletFilterChainTransaction
        implements FilterChain, Transaction
    {
        private Filter[] mFilters;
        private int mNextFilter;
        private ServletPool mPool;
        private HttpServletRequestImpl mRequest;
        private HttpServletResponseImpl mResponse;
        private Servlet mServlet;

        public ServletFilterChainTransaction(Filter[] filters,
                                             ServletPool pool,
                                             HttpServletRequestImpl request,
                                             HttpServletResponseImpl response)
        {
            mFilters = filters;
            mPool = pool;
            mRequest = request;
            mResponse = response;
        }

        public void service() {
            try {
                doFilter(mRequest, mResponse);
            }
            finally {
                try {
                    mResponse.close();
                }
                catch (CheckedSocketException e) {
                    if (!mRequest.isMatchingSocket(e.getSource())) {
                        mLog.error(e);
                    }
                }
                catch (IOException e) {
                    mLog.debug(e);
                }
            }
        }

        public void doFilter(ServletRequest req, ServletResponse resp) {
            Servlet servlet = null;
            try {
                Filter nextFilter = getNextFilter();
                if (nextFilter != null) {
                    nextFilter.doFilter(req, resp, this);
                }
                else {
                    servlet = getServlet();
                    mServlet = null;
                    servlet.service(req, resp);
                }
            }
            catch (CheckedInterruptedIOException e) {
                if (mRequest.isMatchingSocket(e.getSource())) {
                    //mLog.debug(e.toString());
                    sendError(mRequest, mResponse,
                              mResponse.SC_REQUEST_TIMEOUT, null, e);
                }
                else {
                    mLog.error(e);
                    sendError(mRequest, mResponse,
                              mResponse.SC_INTERNAL_SERVER_ERROR, null, e);
                }
            }
            catch (CheckedSocketException e) {
                if (mRequest.isMatchingSocket(e.getSource())) {
                    //mLog.debug(e.toString());
                }
                else {
                    mLog.error(e);
                    sendError(mRequest, mResponse,
                              mResponse.SC_INTERNAL_SERVER_ERROR, null, e);
                }
            }
            catch (UnavailableException e) {
                Throwable t = e.getRootCause();
                if (t != null) {
                    if (e.getMessage() != null) {
                        mLog.error(e.getMessage());
                    }
                    mLog.error(t);
                }
                else {
                    if (e.isPermanent()) {
                        mLog.error(e);
                    }
                    else {
                        mLog.info(e);
                    }
                }

                int retry = e.getUnavailableSeconds();
                if (retry >= 0) {
                    mResponse.setIntHeader(HttpHeaders.RETRY_AFTER, retry);
                    sendError(mRequest, mResponse,
                              mResponse.SC_SERVICE_UNAVAILABLE,
                              "Try again after " + retry + " seconds", e);
                }
                else {
                    sendError(mRequest, mResponse,
                              mResponse.SC_SERVICE_UNAVAILABLE, null, e);
                }
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
                sendError(mRequest, mResponse,
                          mResponse.SC_INTERNAL_SERVER_ERROR, null, e);
            }
            catch (Exception e) {
                mLog.error(e);
                sendError(mRequest, mResponse,
                          mResponse.SC_INTERNAL_SERVER_ERROR, null, e);
            }
            catch (AbortServlet e) {
                // Special error used to abort any servlet.
            }
            catch (Error e) {
                sendError(mRequest, mResponse,
                          mResponse.SC_INTERNAL_SERVER_ERROR, null, e);
                throw e;
            }
            finally {
                mResponse.resetOutputType();
                if (servlet != null) {
                    mPool.put(servlet);
                }
            }    
        }

        public void cancel() throws Exception {
            if (mServlet != null) {
                mPool.put(mServlet);
                mServlet = null;
            }
            if (mResponse != null) {
                sendError(mRequest, mResponse,
                          mResponse.SC_SERVICE_UNAVAILABLE,
                          "Service Unavailable");
                mResponse.close();
            }
        }
        
        /**
         * Servlet retrieved must be used by calling service or cancel.
         * Otherwise it won't be returned to the pool.
         */
        private Servlet getServlet() throws ServletException {
            if (mServlet == null) {
                mServlet = mPool.get();
            }
            return mServlet;
        }

        /**
         * @return the next filter in the chain or null if only the servlet
         * remains.
         */
        private Filter getNextFilter() {
            if (mFilters != null) {
                if (mNextFilter < mFilters.length) {
                    return mFilters[mNextFilter++];
                }
            }
            return null;
        }
    }

    private class ServletRequestDispatcher
        implements BaristaRequestDispatcher, FilterChain
    {
        private Filter[] mFilters;
        private int mNextFilter;
        private ServletPool mPool;
        private String mURI;
        private String mContextPath;
        private String mServletPath;
        private String mPathInfo;
        private String mQuery;
        private Servlet mServlet;

        public ServletRequestDispatcher(Filter[] filters,
                                        ServletPool pool,
                                        String uri,
                                        String contextPath,
                                        String servletPath,
                                        String pathInfo,
                                        String query) {
            mFilters = filters;
            mPool = pool;
            mURI = uri;
            mContextPath = contextPath;
            mServletPath = servletPath;
            mPathInfo = pathInfo;
            mQuery = query;
        }

        /**
         * Used for requesting named dispatcher. No filters can be run.
         */
        public ServletRequestDispatcher(ServletPool pool) {
            mPool = pool;
        }
        
        public void doFilter(ServletRequest request, ServletResponse response) 
            throws ServletException, IOException
        {
            Filter nextFilter = getNextFilter();
            if (nextFilter != null) {
                nextFilter.doFilter(request, response, this);
            }
            else {
                mServlet.service(request, response);
            }
        }

        public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, IOException
        {
            if (response.isCommitted()) {
                throw new IllegalStateException("Response has been committed");
            }
 
            if (request instanceof HttpServletRequest) {
                String query;
                if (mURI == null) {
                    query = mQuery;
                }
                else {
                    query = mergeQueryStrings
                        (((HttpServletRequest)request).getQueryString(),
                         mQuery);
                }
                
                // Wrap this request to hide orginal destination info.
                request = new ForwardedRequestWrapper
                    ((HttpServletRequest)request, mURI, mContextPath,
                     mServletPath, mPathInfo, query);
            }

            response.reset();
            
            try {
                mServlet = mPool.get();
                doFilter(request, response);
            }
            finally {
                mPool.put(mServlet);
                if (!response.isCommitted()) {
                    response.flushBuffer();
                }
            }
        }

        public void include(ServletRequest request, ServletResponse response)
            throws ServletException, IOException
        {
            if (request instanceof HttpServletRequest &&
                response instanceof HttpServletResponse) {

                String query;
                if (mURI == null) {
                    query = mQuery;
                }
                else {
                    query = mergeQueryStrings
                        (((HttpServletRequest)request).getQueryString(),
                         mQuery);
                }
                
                request = new IncludedRequestWrapper
                    ((HttpServletRequest)request, mURI, mContextPath,
                     mServletPath, mPathInfo, query);

                if (response instanceof HttpServletResponseImpl) {
                    ((HttpServletResponseImpl)response).resetOutputType();
                }

                response = new IncludedResponseWrapper
                    ((HttpServletResponse)response);
            }
            
            try {
                mServlet = mPool.get();
                doFilter(request, response);
            }
            finally {
                mPool.put(mServlet);
            }
        }

        public void insert(ServletRequest request, ServletResponse response)
            throws ServletException, IOException
        {
            if (request instanceof HttpServletRequest &&
                response instanceof HttpServletResponse) {

                request = new ForwardedRequestWrapper
                    ((HttpServletRequest)request, mURI, mContextPath,
                     mServletPath, mPathInfo, mQuery);

                if (response instanceof HttpServletResponseImpl) {
                    ((HttpServletResponseImpl)response).resetOutputType();
                }

                response = new IncludedResponseWrapper
                    ((HttpServletResponse)response);
            }
            
            try {
                mServlet = mPool.get();
                doFilter(request, response);
            }
            finally {
                mPool.put(mServlet);
            }
        }

        /**
         * @return the next filter in the chain or null if only the servlet
         * remains.
         */
        private Filter getNextFilter() {
            if (mFilters != null) {
                if (mNextFilter < mFilters.length) {
                    return mFilters[mNextFilter++];
                }
            }
            return null;
        }
    }

    private class RequestWrapper extends HttpServletRequestWrapper {
        private String mQuery;
        private Map mParameterMap;

        RequestWrapper(HttpServletRequest req, String query) {
            super(req);
            mQuery = query;
        }

        public String getQueryString() {
            return mQuery;
        }

        public Map getParameterMap() {
            if (mParameterMap != null) {
                return mParameterMap;
            }

            Map map = new HashMap();
            Utils.parseQueryString(mQuery, map);
            Utils.ensureOnlyStringArrayValues(map);

            // Merge in old properties.
            Iterator it = super.getParameterMap().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                Utils.addToParameterMap(map, entry.getKey(), entry.getValue());
            }

            mParameterMap = Collections.unmodifiableMap(map);
            return mParameterMap;
        }
    
        public String getParameter(String name) {
            Object param = getParameterMap().get(name);
            if (param != null) {
                if (param instanceof String[]) {
                    if (((String[])param).length > 0) {
                        return ((String[])param)[0];
                    }
                }
                else if (param instanceof String) {
                    return (String)param;
                }
            }
            return null;
        }
        
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }
    
        public String[] getParameterValues(String name) {
            Object params = getParameterMap().get(name);
            if (params != null) {
                if (params instanceof String[]) {
                    return (String[])params;
                }
                else if (params instanceof String) {
                    String[] stringz = new String[1];
                    stringz[0] = (String)params;
                    return stringz;
                }
            }
            return null;
        }
    }
    
    private class ForwardedRequestWrapper extends RequestWrapper {
        private String mURI;
        private String mContextPath;
        private String mServletPath;
        private String mPathInfo;

        ForwardedRequestWrapper(HttpServletRequest req,
                                String uri, String contextPath,
                                String servletPath, String pathInfo,
                                String query) {
            super(req, query);
            mURI = uri;
            mContextPath = contextPath;
            mServletPath = servletPath;
            mPathInfo = pathInfo;
        }
        
        public String getRequestURI() {
            return mURI;
        }
        
        public String getContextPath() {
            return mContextPath;
        }
        
        public String getServletPath() {
            return mServletPath;
        }
        
        public String getPathInfo() {
            return mPathInfo;
        }
    }

    private class IncludedRequestWrapper extends RequestWrapper {
        private Map mAttributes;
        
        IncludedRequestWrapper(HttpServletRequest req,
                               String uri, String contextPath,
                               String servletPath, String pathInfo,
                               String query) {
            super(req, query);
            if (uri != null) {
                mAttributes = new HashMap();
                setAttribute(ATTR_REQUEST_URI, uri);
                setAttribute(ATTR_CONTEXT_PATH, contextPath);
                setAttribute(ATTR_SERVLET_PATH, servletPath);
                setAttribute(ATTR_PATH_INFO, pathInfo);
                setAttribute(ATTR_QUERY_STRING, query);
            }
        }
        
        public void setAttribute(String name, Object o) {
            mAttributes.put(name, o);
        }
        
        public Object getAttribute(String name) {
            Object obj = null;
            if (mAttributes != null) {
                obj = mAttributes.get(name);
            }
            if (obj == null) {
                obj = super.getAttribute(name);
            }
            return obj;
        }
    }
    
    /**
     * When a response object is used within the include method of
     * ServletRequestDispatcher, calls to modify headers and status code must
     * be ignored.
     */
    private class IncludedResponseWrapper extends HttpServletResponseWrapper {
        public IncludedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        public void setContentType(String type) {
        }

        public void setLocale(Locale loc) {
        }

        public void setContentLength(int len) {
        }

        public void addCookie(Cookie cookie) {
        }

        public void setHeader(String name, String value) {
        }

        public void setIntHeader(String name, String value) {
        }

        public void setDateHeader(String name, String value) {
        }

        public void addHeader(String name, String value) {
        }

        public void addIntHeader(String name, String value) {
        }

        public void addDateHeader(String name, String value) {
        }

        public void sendError(int sc) {
        }

        public void sendError(int sc, String msg) {
        }

        public void sendRedirect(String loc) {
        }

        public void setStatus(int sc) {
        }

        public void setStatus(int sc, String msg) {
        }
    }
}
