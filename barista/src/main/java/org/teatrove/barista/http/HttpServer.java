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
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.trove.log.*;
import org.teatrove.trove.net.SocketFactory;
import org.teatrove.trove.net.HttpClient;
import org.teatrove.trove.net.PlainSocketFactory;
import org.teatrove.trove.util.UsageMap;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyChangeListener;
import org.teatrove.trove.util.ThreadPool;
import org.teatrove.teatools.PackageDescriptor;
import org.teatrove.trove.util.plugin.PluginContext;
import org.teatrove.trove.util.plugin.PluginFactory;
import org.teatrove.trove.util.plugin.PluginFactoryConfig;
import org.teatrove.trove.util.plugin.PluginFactoryConfigSupport;
import org.teatrove.trove.util.plugin.PluginFactoryException;
import org.teatrove.barista.util.*;
import org.teatrove.barista.validate.*;
import org.teatrove.trove.util.PropertyMapFactory;

/**
 * HttpServer is the main class for a HTTP server implementation. A main
 * method is defined that requires that the name of a properties file be
 * passed to it. It is not required that HttpServer be the main entry point
 * for an application. An application can instantiate and embed any number of
 * HttpServers.
 * <p>
 * Servlets that are instantiated from HttpServer have access to a specific
 * Barista attribute named "org.teatrove.barista.http.HttpServer" that references this
 * HttpServer instance.
 *
 * @author Brian S O'Neill
 */
public class HttpServer {
    private static HttpServer cMainServer;

    /**
     * Main entry point. Requires an argument that specifies the name of the
     * properties file to load. The "httpServer" property group is read in
     * and passed to the init method of a new HttpServer instance.
     */
    public static void main(String[] args) throws Exception {
        cMainServer = new HttpServer(args);

        cMainServer.start();
        if (cMainServer.mShutdownThread == null) {
            // Shutdown hooks not supported, so allow it to be destroyed from
            // stop method.
        }
    }

    /**
     * Singleton accessor
     */
    public static HttpServer getInstance() { return cMainServer; }


    /**
     * Accessors for ServletContext
     */
    public ServletContext getServletContext() { 
        Iterator i = mServletDispatchers.values().iterator();
        if (i.hasNext())
            return (ServletContext) i.next();
        else
            return null;
    }

    /**
     * Stops the HttpServer instance that was created by the main method.
     * This method is called by Java2Service when stopping server.
     */
    public static void stop() {
        HttpServer mainServer = cMainServer;
        if (mainServer != null) {
            mainServer.destroy();
        }
    }

    private static PropertyMapFactory createPropertyMapFactory(String[] args) 
        throws Exception {

        if (args.length < 1) {
            throw new Exception("Properties file required for first argument");
        }

        PropertyMapFactory factory;
        if (args.length == 1) {
            factory = new URLPropertyMapFactory(args[0]);
        }
        else {
            // Load and use custom PropertyMapFactory.
            Class factoryClass = Class.forName(args[1]);
            java.lang.reflect.Constructor ctor =
                factoryClass.getConstructor(new Class[]{String.class});
            factory =
                (PropertyMapFactory)ctor.newInstance(new Object[]{args[0]});
        }

        return factory;
        
        
    }

    private Date mStartDate;
    private Date mRestartDate;

    private Thread mShutdownThread;
    private boolean mDestroyed;

    private PropertyMapFactory mPropertyMapFactory;
    private LineNumberCollector mPropertyLineNumbers;
    private Config mConfig;
    private Log mLog;
    private Log mImpressionLog;

    private List mLogEvents;

    private Config mRootServletConfig;
    // Map of Strings to Config objects for Servlets.
    private UsageMap mServletConfigs;

    private Config mRootFilterConfig;
    // Map of Strings to Config objects for Filters.
    private UsageMap mFilterConfigs;

    private Config mServletDispatcherConfig;
    // Map of Strings to HttpServletDispatchers
    private Map mServletDispatchers;

    private Config mHttpHandlerConfig;
    // Map of Strings to HttpHandlers
    private Map mHttpHandlers;

    private PluginContext mPluginContext = new PluginContext();

    /**
     * HttpServer must be {@link #start started} in order to actually run.
     */
    public HttpServer(String[] args) throws Exception {
        this(createPropertyMapFactory(args));
    }

    /**
     * HttpServer must be {@link #start started} in order to actually run.
     */
    public HttpServer(PropertyMapFactory factory) throws Exception {
        mPropertyMapFactory = factory;
        mLog = Syslog.log();
    }

    
    /**
     * Call start to start the server. Call again to reload some configuration
     * and to reload servlets.
     */
    public void start() throws Exception {

        mPropertyLineNumbers = new LineNumberCollector();
        final PropertyMap properties = mPropertyMapFactory.
            createProperties(mPropertyLineNumbers).subMap("httpServer");

        final Config config;

        synchronized (this) {
            if (mConfig == null) {
                config = RootConfig.forProperties(properties);
            }
            else {
                config = new Config() {
                    private Log mLog = mConfig.getLog();
                    private ThreadPool mThreadPool = mConfig.getThreadPool();

                    public PropertyMap getProperties() {
                        return properties;
                    }

                    public Log getLog() {
                        return mLog;
                    }
                    
                    public ThreadPool getThreadPool() {
                        return mThreadPool;
                    }
                };
            }
        }

        // Initialize in new thread to ensure all derived threads are in the
        // same thread group.
        Runnable startup = new Runnable() {
            public void run() {
                init(config);
            }
        };

        Thread t = new Thread(config.getThreadPool(), startup, "startup");
        t.start();

        try {
            t.join();
        }
        catch (InterruptedException e) {
            config.getLog().info("Initialization interrupted");
        }
    }

    private synchronized void init(Config config) {
        if (mConfig == null) {
            mConfig = config;
        }
        Log log = config.getLog();

        if (mStartDate == null) {
            log.info(Utils.getServerInfo() + " starting...");
        }
        else {
            log.info(Utils.getServerInfo() + " restarting...");
        }

        /*

        // Validate properties
        try {   
            PropertyMapFactory validationFactory = new URLPropertyMapFactory
                (getClass().getResource
                 ("/org/teatrove/barista/util/validator.properties"));
            PropertyMapValidator.validatePropertyMap
                (config.getProperties(), validationFactory.createProperties());
        }
        catch (Exception e) {
            log.warn(e.toString());
        }
        */

        //
        // validate the loaded PropertyMap
        //

        try {
            URL url = RulesEvaluator.getRulesURLForClass(
                "org/teatrove/barista/validate/BaristaPropertyMapValidator");
            PropertyMapFactory factory = createPropertyMapFactory(new String[] 
                {url.toString()});
            PropertyMap validator = factory.createProperties();
            
            Validator pmv = 
                new BaristaPropertyMapValidator(config.getProperties(), 
                                                validator,mPropertyLineNumbers.
                                                getChangeMap());

            LogErrorListener el = new LogErrorListener(mLog);
            pmv.addValidationListener(el);
            mLog.info("Checking Barista Properties File...");
            pmv.validate();
            mLog.info("Finished Checking Barista Properties File");
            mLog.info(el.getErrorCount() + " Errors Found");
            mLog.info(el.getWarnCount() + " Warnings Found");
            pmv.removeValidationListener(el);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        Log impressionLog = new Log("HTTP impressions", null);

        // Create a list to store the most recent log events.

        List logEvents = createMemoryLog(log, config.getProperties());

        // Create all the Servlet config objects.
        Config rootServletConfig = new DerivedConfig(config, "servlets");
        UsageMap servletConfigs = new UsageMap();
        servletConfigs.setReverseOrder(true);
        createServletConfigs(servletConfigs, rootServletConfig);

        // Create all the Filter config objects.
        Config rootFilterConfig = new DerivedConfig(config, "filters");
        UsageMap filterConfigs = new UsageMap();
        filterConfigs.setReverseOrder(true);
        createFilterConfigs(filterConfigs, rootFilterConfig);

        // Create all the ServletDispatchers.
        Config servletDispatcherConfig =
            new DerivedConfig(config, "servletDispatchers");
        Map servletDispatchers = new HashMap();
        createServletDispatchers(servletDispatchers,
                                 servletDispatcherConfig,
                                 impressionLog,
                                 servletConfigs,
                                 filterConfigs);

        // Create all the HttpHandlers.
        Config httpHandlerConfig = new DerivedConfig(config, "httpHandlers");
        Map httpHandlers;
        if (mHttpHandlers != null) {
            httpHandlers = new HashMap(mHttpHandlers);
        }
        else {
            httpHandlers = new HashMap();
        }

        createHttpHandlers(httpHandlers, httpHandlerConfig,
                           impressionLog, servletDispatchers);

        if (httpHandlers.size() == 0) {
            log.warn("No HTTP handlers configured");
        }

        // Save all this stuff only when successful.
        mConfig = config;
        mLog = log;
        mImpressionLog = impressionLog;
        mLogEvents = logEvents;
        mRootServletConfig = rootServletConfig;
        mRootFilterConfig = rootFilterConfig;
        mServletConfigs = servletConfigs;
        mFilterConfigs = filterConfigs;
        mServletDispatcherConfig = servletDispatcherConfig;
        mServletDispatchers = servletDispatchers;
        mHttpHandlerConfig = httpHandlerConfig;
        mHttpHandlers = httpHandlers;

        PluginFactoryConfig pluginConfig = new PluginFactoryConfigSupport
            (config.getProperties(), mLog, mPluginContext);
        
        try {
            PluginFactory.createPlugins(pluginConfig);
        } 
        catch (PluginFactoryException e) {
            mLog.warn("Error loading plugins.");
            mLog.warn(e);
        }
        
        if (mStartDate == null) {
            log.info(Utils.getServerInfo() + " started");

            Thread shutdownThread = new Thread("shutdown") {
                public void run() {
                    HttpServer.this.destroy();
                }
            };
            shutdownThread.setPriority(Thread.MAX_PRIORITY - 1);

            try {
                Runtime.getRuntime().addShutdownHook(shutdownThread);
                mShutdownThread = shutdownThread;
            }
            catch (LinkageError e) {
                // VM must not support shutdown hook.
            }

            mStartDate = new Date();
            //connectToUsageServer();
        }
        else {
            log.info(Utils.getServerInfo() + " restarted");
            mRestartDate = new Date();
        }

        /*
         * Print out the mapping of the teaservlet so people who
         * don't RTFM can find the admin pages
         */
        // printOutAdminPageLink(config.getProperties(),config.getLog());

    }

    public synchronized Config getConfig() {
        return mConfig;
    }

    public synchronized void destroy() {
        if (mDestroyed) {
            return;
        }

        mLog.info(Utils.getServerInfo() + " stopping...");

        // Shut down all the handlers.
        if (mHttpHandlers != null) {
            Iterator handlers = mHttpHandlers.values().iterator();
            while (handlers.hasNext()) {
                HttpHandler handler = (HttpHandler)handlers.next();
                try {
                    handler.init(null);
                }
                catch (Exception e) {
                    mLog.debug(e.toString());
                }
            }
        }

        mLog.info(Utils.getServerInfo() + " stopped");
        mDestroyed = true;
    }

    protected void finalize() {
        destroy();
        if (mShutdownThread != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(mShutdownThread);
            }
            catch (IllegalStateException e) {
            }
        }
    }

    /**
     * Returns the lines that have been written to the log file. This is used
     * by the admin functions.
     */
    public synchronized LogEvent[] getLogEvents() {
        if (mLogEvents == null) {
            return new LogEvent[0];
        }
        else {
            LogEvent[] events = new LogEvent[mLogEvents.size()];
            return (LogEvent[])mLogEvents.toArray(events);
        }
    }

    /**
     * Returns a mapping from HTTP handler names to HttpHandler instances.
     */
    public synchronized Map getHttpHandlers() {
        return Collections.unmodifiableMap(mHttpHandlers);
    }

    public synchronized Date getStartDate() {
        return mStartDate;
    }

    /**
     * Returns null if never restarted.
     */
    public synchronized Date getLastRestartDate() {
        return mRestartDate;
    }

    /**
     * @param servletConfigs Map of Strings to servlet Config objects that
     * must be filled in only for entries that have changed.
     */
    protected void createServletConfigs(Map servletConfigs,
                                        Config config) {
        Iterator servletNames =
            config.getProperties().subMapKeySet().iterator();

        while (servletNames.hasNext()) {
            String name = (String)servletNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            if (!configChanged
                ((Config)servletConfigs.get(name), derivedConfig)) {
                continue;
            }

            servletConfigs.put(name, derivedConfig);
        }
    }

    /**
     * @param filterConfigs Map of Strings to filter Config objects that
     * must be filled in only for entries that have changed.
     */
    protected void createFilterConfigs(Map filterConfigs,
                                       Config config) {
        Iterator filterNames =
            config.getProperties().subMapKeySet().iterator();

        while (filterNames.hasNext()) {
            String name = (String)filterNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            if (!configChanged
                ((Config)filterConfigs.get(name), derivedConfig)) {
                continue;
            }

            filterConfigs.put(name, derivedConfig);
        }
    }

    /**
     * Creates HttpServletDispatchers that have a Barista-specific default
     * attribute named "org.teatrove.barista.http.HttpServer" that references this
     * HttpServer.
     *
     * @param servletDispatchers Map of Strings to HttpServletDispatchers that
     * must be filled in only for entries that have changed.
     */
    protected void createServletDispatchers
        (Map servletDispatchers, Config config, Log impressionLog,
         Map servletConfigs, Map filterConfigs)
    {
      
        Iterator dispatcherNames =
            config.getProperties().subMapKeySet().iterator();

        while (dispatcherNames.hasNext()) {
            String name = (String)dispatcherNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            HttpServletDispatcher disp =
                (HttpServletDispatcher)servletDispatchers.get(name);

            if (disp != null &&
                !configChanged(disp.getConfig(), derivedConfig)) {
                continue;
            }

            mLog.info("Configuring ServletDispatcher: " + name);

            disp = new HttpServletDispatcher(servletConfigs, filterConfigs);

            // Allow servlets to get a Barista-specific attribute.
            disp.setAttribute("org.teatrove.barista.http.HttpServer", this);

            disp.init(new HttpHandlerStage.DefaultConfig
                      (derivedConfig, impressionLog));
           
            servletDispatchers.put(name, disp);
        }
    }

    /**
     * @param httpHandlers Map of Strings to HttpHandlers
     */
    protected void createHttpHandlers
        (Map httpHandlers, Config config, Log iLog, Map servletDispatchers)
    {
        Iterator handlerNames =
            config.getProperties().subMapKeySet().iterator();

        while (handlerNames.hasNext()) {
            String name = (String)handlerNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            HttpHandler handler =
                (HttpHandler)httpHandlers.get(name);

            /*
            if (handler != null &&
                !configChanged(handler.getConfig(), derivedConfig)) {
                continue;
            }
            */

            mLog.info("Configuring HttpHandler: " + name);

            PropertyMap handlerProperties = derivedConfig.getProperties();

            String dispName =
                handlerProperties.getString("servlet.dispatcher");

            if (dispName == null) {
                mLog.error("Property \"servlet.dispatcher\" not specified");
                continue;
            }

            HttpServletDispatcher disp =
                (HttpServletDispatcher)servletDispatchers.get(dispName);

            if (disp == null) {
                mLog.error("No ServletDispatcher found named: " +
                           dispName);
                continue;
            }

            HttpHandlerStage[] stages = {
                disp
            };

            try {
                if (handler == null) {
                    handler = new HttpHandler
                        (new HttpHandler.DefaultConfig
                         (derivedConfig, iLog, stages));
                }
                else {
                    handler.init
                        (new HttpHandler.DefaultConfig
                         (handler.getConfig(),
                          derivedConfig, iLog, stages));
                }

                httpHandlers.put(name, handler);
            }
            catch (Exception e) {
                mLog.error(e);
            }
        }
    }

    private URL[] parseClassPath(String classpath) {
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
                    mLog.error(e.toString());
                }
            }
        }

        return (URL[])urls.toArray(new URL[urls.size()]);
    }

    private boolean configChanged(Config config1, Config config2) {
        try {
            return !config1.getProperties().equals(config2.getProperties());
        }
        catch (NullPointerException e) {
        }
        return true;
    }

    private List createMemoryLog(Log log, PropertyMap properties) {
        // Create memory log listener.
        final List logEvents = Collections.synchronizedList(new LinkedList());

        // The maximum number of log events to store in memory.
        int max = 100;
        String maxLogEventsParam = properties.getString("log.max");
        if (maxLogEventsParam != null) {
            try {
                max = Integer.parseInt(maxLogEventsParam);
            }
            catch (NumberFormatException e) {
            }
        }

        final int logEventsMax = max;

        log.addLogListener(new LogListener() {
            public void logMessage(LogEvent e) {
                checkSize();
                logEvents.add(e);
            }

            public void logException(LogEvent e) {
                checkSize();
                logEvents.add(e);
            }

            private void checkSize() {
                while (logEvents.size() >= logEventsMax) {
                    logEvents.remove(0);
                }
            }
        });
        
        return logEvents;
    }

    /**
     * Connect to the Usage Server to register use.
     */
    private void connectToUsageServer() {
        //wrap everything with a try-catch statement to keep silent.
        try {
        Runnable r = new Runnable() {

                private boolean keepLogging;

                public void stopLogging() {
                    synchronized (this) {
                        keepLogging = false;
                        notify();
                    }
                }

                public void run() {

                    try {
                        keepLogging = true;

                        String[] hostnames = {};

                        // see if other hosts are available.
                        try {   
                            BufferedReader bin = new BufferedReader(new InputStreamReader(getClass().getResource("/org/teatrove/barista/http/UsageLogHosts.txt").openStream()));
                        
                            List hostList = new ArrayList();
                            String nextHostName = null;
                            while ((nextHostName = bin.readLine()) != null) {
                                hostList.add(nextHostName);
                                mLog.debug(nextHostName);
                            }
                            hostnames = (String[])hostList
                                .toArray(new String[hostList.size()]);
                        }
                        catch (Exception e) {}

                        UsageLoggingClient ulc = 
                            new UsageLoggingClient(hostnames);

                        while (keepLogging) {
                            try {
                                ulc.logUsage();

                                synchronized (this) {
                                    try {
                                        // wait for a day or so.
                                        wait(1001 * 60 * 60 * 24);
                                    }
                                    catch (InterruptedException ie) {
                                        // doh!
                                    }
                                }
                            }
                            catch (Throwable e) {}
                        }
                    }
                    catch (Throwable e) {}
                }
            };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }
        catch (Exception e) {
            // ignore any exceptions.
        }
    }

    /*
      private void printOutAdminPageLink(PropertyMap properties, Log log) {
        try {
            final Class adminAppClass = org.teatrove.teaservlet.AdminApplication.class;
            final String initAdmin = ".init.admin.";
            PropertyMap handyMap = new PropertyMap();
            try {
                //Find out which servlets are instances of the TeaServlet.
                PropertyMap servletProps = properties.subMap("servlets");
                Set servletKeys = servletProps.subMapKeySet();
                Iterator serveIt = servletKeys.iterator();
                while (serveIt.hasNext()) {
                    String servletName = (String)serveIt.next();
                    String servletClassName = 
                        servletProps.getString(servletName + ".class");
                    if (servletClassName != null) {
                        Class servletClass = Class.forName(servletClassName);
                        if (org.teatrove.teaservlet.TeaServlet.class
                            .isAssignableFrom(servletClass)) {
                    
                            PropertyMap appProps = servletProps
                                .subMap(servletName + ".init.applications");
                            Iterator appIt = appProps.subMapKeySet().iterator();
                            String adminPair = null;
                            while (appIt.hasNext() && adminPair == null) {
                                try {
                                    String appName = (String)appIt.next();
                                    String className = appProps
                                        .getString(appName + ".class");
                                  
                                    if (className != null 
                                        && adminAppClass
                                        .isAssignableFrom(Class.forName(className))) {
                                        adminPair = appProps.getString(appName 
                                                                       + initAdmin + "key");
                                        if (adminPair != null) {
                                            String val = appProps.getString(appName + initAdmin + "value");
                                            if (val != null) {
                                                adminPair = adminPair + '=' + val;
                                            }
                                            else {
                                                adminPair = "";
                                            }
                                        }
                                        else {
                                            adminPair = "";
                                        }
                                    }
                            
                                }
                                catch (Exception e) {
                                    log.debug(e);
                                    adminPair = null;
                                }
                            }
                            if (adminPair == null) {
                                log.warn("The AdminApplication has not been configured for the " + servletName + " instance of the TeaServlet.");
                            }
                            else {
                                handyMap.put("teaservlet." + servletName, adminPair);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                log.debug(e);
            }

            // Match up servlets with the dispatcher mapping.
            PropertyMap dispProps = properties.subMap("servletDispatchers");
            Iterator dispIt = dispProps.subMapKeySet().iterator();
            while (dispIt.hasNext()) {
                String dispName = (String)dispIt.next();
                PropertyMap servletMap = dispProps.subMap(dispName + ".servletMap");
                Iterator mapIt = servletMap.entrySet().iterator();
                while (mapIt.hasNext()) {
                    Map.Entry entry = (Map.Entry)mapIt.next();
                    String admin = handyMap.getString("teaservlet." 
                                                      + (String)entry.getValue());
                    if (admin != null) {
                        String mapKey = (String)entry.getKey();
                        while (mapKey.endsWith("*") || mapKey.endsWith("/")) {
                            mapKey = mapKey.substring(0,mapKey.length()-1);
                        }
                        if (admin.length() > 0) {
                            admin = '?' + admin;
                        }
                        handyMap.put("dispatcher." + dispName, 
                                     mapKey + "/system/teaservlet/Admin" + admin);
                    }
                }
            }

            // Now obtain the handler information
            PropertyMap handlerProps = properties.subMap("httpHandlers");
            Iterator handleIt = handlerProps.subMapKeySet().iterator();
            while (handleIt.hasNext()) {
                String handlerName = (String)handleIt.next();
                String dispatcher = handlerProps
                    .getString(handlerName + ".servlet.dispatcher");
                if (dispatcher != null) {
                    String link = handyMap.getString("dispatcher." + dispatcher);
                    if (link != null) {
                        String portNum = handlerProps
                            .getString(handlerName + ".socket.port");
                        String bind =  handlerProps
                            .getString(handlerName + ".socket.bind");
                        if (bind == null) {
                            bind = InetAddress.getLocalHost().getHostName();
                        }
                        if (portNum == null) {
                            portNum = "";
                        }
                        else {
                            portNum = ':' + portNum;
                        }
                        log.info("The Admin pages may be found at: http://" 
                                 + bind + portNum + link);
                    }
                }
            }
    
        }
        catch (Exception e) {
            log.debug(e);
            }
        }
    */

    /**
     * Simple HTTP client that provides the ability for "client applications" 
     * to connect to an HTTP server for the purpose of usage logging.
     * <p>
     * As an example, this class can be used whenever an application is started
     * to enable remote usage logging.
     *
     * @author Mark Masse
     * @version
     * <!--$$Revision:--> 78 <!-- $-->, <!--$$JustDate:-->  8/11/04 <!-- $-->
     */
    private static class UsageLoggingClient {

        private SocketFactory[] mFactories;
        private HashMap mUsageParameters;
        private String uri = "/barista/usage?";

        UsageLoggingClient(String[] hostnames) {
   
            mUsageParameters = new HashMap();
     
            mFactories = new SocketFactory[hostnames.length];
            
            for (int j = 0; j < hostnames.length;j++) {
                
                try {           
                    mFactories[j] = 
                        new PlainSocketFactory(InetAddress
                                               .getByName(hostnames[j]),
                                                       80, 15000);
                }
                catch (Exception e) {
                    mFactories[j] = null;
                }                  
            }  
 
            String user = null,version = null,dir = null;
            try {
                user = System.getProperty("user.name");
            }
            catch (Exception e) {}

            try {
                PackageDescriptor pd = PackageDescriptor.forName("org.teatrove.barista");
                version = pd.getImplementationVersion(); 
            }
            catch (Exception e) {}

            try {
                dir = System.getProperty("user.dir");
            }
            catch (Exception e) {}

            StringBuffer uriBuf = new StringBuffer(uri);
            uriBuf.append("user=");
            uriBuf.append(user);
            uriBuf.append("&version=");
            uriBuf.append(version);
            uriBuf.append("&directory=");
            uriBuf.append(dir);
            uri = uriBuf.toString();
        }
       
        /**
         * Creates a URL connection to the configured server URL passing all of
         * the configured parameters.  
         *
         * @return true if successful.
         */
        public boolean logUsage() {
            
            //Loop until we successfully connect.
            for (int j = 0; j < mFactories.length; j++) {
                if (mFactories[j] != null) {
                    try {
                        HttpClient client = new HttpClient(mFactories[j]);
                        client.setURI(uri);
                        HttpClient.Response resp = client.getResponse();
                        if (resp.getStatusCode() == 200) {
                            return true;
                        }
                    }
                    catch (Exception e) {}
                }
            }
            return false;
        }
    }
}
