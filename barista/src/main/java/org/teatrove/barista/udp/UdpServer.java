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

package org.teatrove.barista.udp;

import java.io.*;
import java.util.*;
import java.net.*;

import org.teatrove.trove.log.*;
import org.teatrove.trove.util.UsageMap;
import org.teatrove.trove.util.PropertyMap;

import org.teatrove.trove.util.ThreadPool;

import org.teatrove.barista.util.*;
import org.teatrove.barista.validate.*;
import org.teatrove.trove.util.PropertyMapFactory;

/**
 * UdpServer is the main class for a UDP implementation. A main
 * method is defined that requires that the name of a properties file be
 * passed to it. 
 * 
 * Servlets that are instantiated from UdpServer have access to a specific
 * attribute named "go.udpServer.udp.UdpServer" that references this
 * UdpServer instance.
 *
 * @author Tammy Wang
 */
public class UdpServer {
    private static UdpServer cMainServer;

    /**
     * Main entry point. Requires an argument that specifies the name of the
     * properties file to load. The "udpServer" property group is read in
     * and passed to the init method of a new UdpServer instance.
     */
    public static void main(String[] args) throws Exception {
        UdpServer server = new UdpServer(args);
        server.start();
        if (server.mShutdownThread == null) {
            // Shutdown hooks not supported, so allow it to be destroyed from
            // stop method.
            cMainServer = server;
        }
    }

    /**
     * Stops the UdpServer instance that was created by the main method.
     * This method is called by Java2Service when stopping server.
     */
    public static void stop() {
        UdpServer mainServer = cMainServer;
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
    // Map of Strings to UdpServletDispatchers
    private Map mServletDispatchers;

    private Config mUdpHandlerConfig;
    // Map of Strings to UdpHandlers
    private Map mUdpHandlers;

    /**
     * UdpServer must be {@link #start started} in order to actually run.
     */
    public UdpServer(String[] args) throws Exception {
        this(createPropertyMapFactory(args));
    }

    /**
     * UdpServer must be {@link #start started} in order to actually run.
     */
    public UdpServer(PropertyMapFactory factory) throws Exception {
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
            createProperties(mPropertyLineNumbers).subMap("udpServer");

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
		log.info("init servletDispathers");
        Config servletDispatcherConfig =
            new DerivedConfig(config, "servletDispatchers");
        Map servletDispatchers = new HashMap();
        createServletDispatchers(servletDispatchers,
                                 servletDispatcherConfig,
                                 servletConfigs,
                                 filterConfigs);

        // Create all the UdpHandlers.
        Config udpHandlerConfig = new DerivedConfig(config, "udpHandlers");
        Map udpHandlers;
        if (mUdpHandlers != null) {
            udpHandlers = new HashMap(mUdpHandlers);
        }
        else {
            udpHandlers = new HashMap();
        }

        createUdpHandlers(udpHandlers, udpHandlerConfig,servletDispatchers);

        if (udpHandlers.size() == 0) {
            log.warn("No UDP handlers configured");
        }

        // Save all this stuff only when successful.
        mConfig = config;
        mLog = log;
        mRootServletConfig = rootServletConfig;
        mServletConfigs = servletConfigs;
        mFilterConfigs = filterConfigs;
        mServletDispatcherConfig = servletDispatcherConfig;
        mServletDispatchers = servletDispatchers;
        mUdpHandlerConfig = udpHandlerConfig;
        mUdpHandlers = udpHandlers;

        
        if (mStartDate == null) {
            log.info(Utils.getServerInfo() + " started");

            Thread shutdownThread = new Thread("shutdown") {
                public void run() {
                    UdpServer.this.destroy();
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
           
        }
        else {
            log.info(Utils.getServerInfo() + " restarted");
            mRestartDate = new Date();
        }


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
        if (mUdpHandlers != null) {
            Iterator handlers = mUdpHandlers.values().iterator();
            while (handlers.hasNext()) {
                UdpHandler handler = (UdpHandler)handlers.next();
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
     * Returns a mapping from UDP handler names to UdpHandler instances.
     */
    public synchronized Map getUdpHandlers() {
        return Collections.unmodifiableMap(mUdpHandlers);
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
     * Creates UdpServletDispatchers that have a Barista-specific default
     * attribute named "org.teatrove.barista.udp.UdpServer" that references this
     * UdpServer.
     *
     * @param servletDispatchers Map of Strings to UdpServletDispatchers that
     * must be filled in only for entries that have changed.
     */
    protected void createServletDispatchers
        (Map servletDispatchers, Config config,
         Map servletConfigs, Map filterConfigs)
    {
      
        Iterator dispatcherNames =
            config.getProperties().subMapKeySet().iterator();

        while (dispatcherNames.hasNext()) {
            String name = (String)dispatcherNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            UdpServletDispatcher disp =
                (UdpServletDispatcher)servletDispatchers.get(name);

            if (disp != null &&
                !configChanged(disp.getConfig(), derivedConfig)) {
                continue;
            }

            mLog.info("Configuring ServletDispatcher: " + name);

            disp = new UdpServletDispatcher(servletConfigs, filterConfigs);

            // Allow servlets to get a Barista-specific attribute.
            disp.setAttribute("go.udpServer.udp.UdpServer", this);

            disp.init(new UdpHandlerStage.DefaultConfig
                      (derivedConfig));
           
            servletDispatchers.put(name, disp);
        }
    }



 
      /**
     * @param udpHandlers Map of Strings to UdpHandlers
     */
    protected void createUdpHandlers
        (Map udpHandlers, Config config,  Map servletDispatchers)
    {
        Iterator handlerNames =
            config.getProperties().subMapKeySet().iterator();

        while (handlerNames.hasNext()) {
            String name = (String)handlerNames.next();
            Config derivedConfig = new DerivedConfig(config, name);

            UdpHandler handler =
                (UdpHandler)udpHandlers.get(name);
            mLog.info("Configuring UdpHandler: " + name);

            PropertyMap handlerProperties = derivedConfig.getProperties();

		String dispName =
                handlerProperties.getString("servlet.dispatcher");

            if (dispName == null) {
                mLog.error("Property \"servlet.dispatcher\" not specified");
                continue;
            }

            UdpServletDispatcher disp =
                (UdpServletDispatcher)servletDispatchers.get(dispName);

            if (disp == null) {
                mLog.error("No ServletDispatcher found named: " +
                           dispName);
                continue;
            }

            UdpHandlerStage[] stages = {
                disp
            };

            try {
                if (handler == null) {
                   handler = new UdpHandler
                        (new UdpHandler.DefaultConfig
                        (derivedConfig,stages));
                }
                else {
                    handler.init
                        (new UdpHandler.DefaultConfig
                         (handler.getConfig(),
                         derivedConfig,stages));
                }

                udpHandlers.put(name, handler);
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

   
     
   }
           
