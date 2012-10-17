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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.teatrove.tea.engine.ContextCreationException;
import org.teatrove.tea.engine.Template;
import org.teatrove.tea.log.TeaLog;
import org.teatrove.tea.log.TeaLogEvent;
import org.teatrove.tea.log.TeaLogListener;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.teaservlet.assets.Asset;
import org.teatrove.teaservlet.stats.TeaServletRequestStats;
import org.teatrove.teaservlet.stats.TemplateStats;
import org.teatrove.teaservlet.util.FilteredServletContext;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.log.LogListener;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.trove.util.StatusEvent;
import org.teatrove.trove.util.StatusListener;
import org.teatrove.trove.util.SubstitutionFactory;
import org.teatrove.trove.util.plugin.PluginContext;
import org.teatrove.trove.util.plugin.PluginFactory;
import org.teatrove.trove.util.plugin.PluginFactoryConfig;
import org.teatrove.trove.util.plugin.PluginFactoryConfigSupport;
import org.teatrove.trove.util.plugin.PluginFactoryException;
import org.teatrove.trove.util.resources.ResourceFactory;

/**
 * The TeaServlet allows Tea templates to define dynamic web pages. The URI
 * that is passed into the servlet determines what template will be called.
 * Within the template, custom functions can be called that return JavaBeans.
 * These functions are accessed via {@link Application Applications} that are
 * configured to run in the TeaServlet.
 * <p>
 * The TeaServlet accepts the following initialization parameters:
 * <ul>
 * <li>properties.file - optional path to file with TeaServlet properties, in format used by {@link org.teatrove.teaservlet.util.PropertyParser}
 * <li>template.path - path to the templates
 * <li>template.classes - directory to save compiled templates
 * <li>template.default - the default name for templates
 * <li>template.file.encoding - character encoding of template source files
 * <li>template.exception.guardian - when true, runtime exceptions during template execution don't abort page output
 * <li>autocompile - when true, will compile the template when it is requested if the template has been updated (just like JSP!)
 * <li>autocompile.recurse - when true (default), will compile any sub-template that has changed regardless of if the requested template has changed.  Only meaningful if autocompile=true
 * <li>separator.query - override the query separator of '?'
 * <li>separator.parameter - override the parameter separator of '&'
 * <li>separator.value - override the parameter separator of '='
 * <li>log.enabled - turns on/off log (boolean)
 * <li>log.debug - turns on/off log debug messages (boolean)
 * <li>log.info - turns on/off log info messages (boolean)
 * <li>log.warn - turns on/off log warning messages (boolean)
 * <li>log.error - turns on/off log error messages (boolean)
 * <li>log.max - the max log lines to keep in memory
 * <li>applications.[name].class - the application class (required)
 * <li>applications.[name].init.* - prefix for application specific initialization parameters
 * <li>applications.[name].log.enabled - turns on/off application log (boolean)
 * <li>applications.[name].log.debug - turns on/off application log debug messages (boolean)
 * <li>applications.[name].log.info - turns on/off application log info messages (boolean)
 * <li>applications.[name].log.warn - turns on/off application log warning messages (boolean)
 * <li>applications.[name].log.error - turns on/off application log error messages (boolean)
 * </ul>
 *
 * @author Reece Wilton
 * @version

 */
public class TeaServlet extends HttpServlet {
 
    private static final long serialVersionUID = 1L;

    private static final Object[] NO_PARAMS = new Object[0];
    private static final boolean DEBUG = false;
    private static final String ENGINE_ATTR =
        "org.teatrove.teaservlet.TeaServletEngine";

    private TeaServletEngine mEngine;

    private static final String WAR_TEMPLATE_SOURCE_PATH = "/WEB-INF/tea";
    private static final String WAR_TEMPLATE_CLASS_PATH = "/WEB-INF/teaclasses";

    private boolean mDebugEnabled;
    
    private Log mLog;
    /** Captured log events that were likely also written to a log file. */
    private List<LogEvent> mLogEvents;

    private PropertyMap mProperties;
    private PropertyMap mSubstitutions;
    private ServletConfig mServletConfig;
    private ServletContext mServletContext;
    private String mServletName;
    private ResourceFactory mResourceFactory;
    private boolean mInstrumentationEnabled;

    private String mQuerySeparator;
    private String mParameterSeparator;
    private String mValueSeparator;
    private boolean mUseSpiderableRequest;
    
    private TeaServletRequestStats mTeaServletRequestStats;

    private TeaServletStatusListener mPluginListener;
    private TeaServletStatusListener mApplicationListener;
    private TeaServletStatusListener mTemplateListener;
    
    private Future<Boolean> mInitializer;
    
    /**
     * Initializes the TeaServlet. Creates the logger and loads the user's
     * application.
     * @param config  the servlet config
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mServletConfig = config;
        
        config.getServletContext().log("Initializing TeaServlet...");
        
        String ver = System.getProperty("java.version");
        if (ver.startsWith("0.") || ver.startsWith("1.2") ||
            ver.startsWith("1.3")) {
            config.getServletContext()
                .log("The TeaServlet requires Java 1.4 or higher to run properly");
        }

        mServletContext = setServletContext(config);
        mServletName = setServletName(config);
        
        mProperties = new PropertyMap();
        mSubstitutions = SubstitutionFactory.getDefaults();
        mResourceFactory = 
            new TeaServletResourceFactory(config.getServletContext(), 
                                          mSubstitutions);
        
        Enumeration<?> e = config.getInitParameterNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = 
                SubstitutionFactory.substitute(config.getInitParameter(key));
            
            if (key.equals("debug")) {
                mDebugEnabled = Boolean.parseBoolean(value);
                continue;
            }

            mProperties.put(key, value);
        }

        loadDefaults();
        discoverProperties();
        createListeners();
        createLog(mServletContext);
        mLog.applyProperties(mProperties.subMap("log"));
        createMemoryLog(mLog);
        
        mInstrumentationEnabled = 
            mProperties.getBoolean("instrumentation.enabled", true);
        
        Initializer initializer = new Initializer();
        if (mProperties.getBoolean("startup.background", false)) {
            mInitializer = 
                Executors.newSingleThreadExecutor().submit(initializer);
        }
        else {
            initializer.call();
        }
    }
    
    protected boolean isInitialized() {
        return mInitializer == null || mInitializer.isDone();
    }
    
    protected boolean isRunning() {
        if (mInitializer == null) { return true; }
        
        try { return isInitialized() && mInitializer.get().booleanValue(); }
        catch (Exception exception) { return false; }
    }
    
    protected class Initializer implements Callable<Boolean> {
        public Boolean call() {
            try { init(mServletConfig); return Boolean.TRUE; }
            catch (ServletException exception) {
                
                mServletConfig.getServletContext().log("Unable to initialize", exception);
                return Boolean.FALSE;
            }
        }
        
        protected void init(ServletConfig config) throws ServletException {
            config.getServletContext().log("Starting TeaServlet...");
        
            mTeaServletRequestStats = TeaServletRequestStats.getInstance();
            mTeaServletRequestStats.applyProperties(mProperties.subMap("stats"));

            PluginContext pluginContext = loadPlugins(mProperties, mLog);
    
            mEngine = createTeaServletEngine();
    
            // Check to see if tea templates are being deployed within a WAR bundle.  
            // If so, append this path to the existing template path.  For Tomcat.
            Set<?> rp = mServletContext.getResourcePaths(WAR_TEMPLATE_SOURCE_PATH);
            if (rp != null && rp.size() > 0) {
                String warPath = mServletContext.getRealPath("/") + WAR_TEMPLATE_SOURCE_PATH;
                String newPath = mProperties.getString("template.path");
                newPath = newPath != null && newPath.length() > 0 ? newPath : warPath;
                mProperties.put("template.path", newPath);
            }
    
            if (mProperties.getString("template.classes") == null) {
                Set<?> crp = mServletContext.getResourcePaths("/WEB-INF");
                if (crp != null && crp.size() > 0)
                    mProperties.put("template.classes", mServletContext.getRealPath("/") + WAR_TEMPLATE_CLASS_PATH);
            }
    
            TeaServletEngineImpl engine = ((TeaServletEngineImpl)getEngine());
            engine.setApplicationListener(mApplicationListener);
            engine.setTemplateListener(mTemplateListener);
            engine.startEngine(mProperties, mServletContext, mServletName, 
                               mLog, mLogEvents, pluginContext);
    
            mQuerySeparator = mProperties.getString("separator.query", "?");
            mParameterSeparator = mProperties
                .getString("separator.parameter", "&");
            mValueSeparator = mProperties.getString("separator.value", "=");
    
            mUseSpiderableRequest =
                (!"?".equals(mQuerySeparator)) ||
                (!"&".equals(mParameterSeparator)) ||
                (!"=".equals(mValueSeparator));
    
            config.getServletContext().log("TeaServlet complete...");
        }
    }

    public PropertyMap getProperties() { return mProperties; }

    /**
     * Destroys the TeaServlet and the user's application.
     */
    public void destroy() {
        if (mEngine != null) {
            mLog.info("Destroying Engine");
            mEngine.destroy();
        }
        mLog.info("Destroying TeaServlet");
        super.destroy();
    }

    /**
     * Logger access.
     */
    public Log getLog() { return mLog; }

    TeaServletEngine getEngine() {
        if (mEngine == null) {
            mLog.debug("the engine aint there");
        }
        return mEngine;
    }

    /**
     * Returns information about the TeaServlet.
     */
    public String getServletInfo() {
        return "Tea template servlet";
    }

    /**
     * Process the user's http post request.
     */
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
        throws ServletException, IOException
    {
        doGet(request, response);
    }

    protected void printStatus(PrintWriter writer, 
                               TeaServletStatusListener status) {
        
        String state = "waiting";
        if (status.isComplete()) { state = "valid"; }
        else if (status.isActive()) { state = "active"; }
        
        writer.print("\"id\":" + status.getId() + ",");
        writer.print("\"state\":\"" + state + "\",");
        writer.print("\"progress\":" + "active".equals(state) + ",");
        writer.print("\"text\":\"Loading " + status.getType() + "...\",");
        writer.print("\"name\":\"" + status.getType() + "\",");
        writer.print("\"status\":{");
            writer.print("\"count\":" + status.getTotal() + ",");
            writer.print("\"index\":" + status.getCurrent() + ",");
            writer.print("\"percent\":" + status.getPercent());
            if (status.getCurrentName() != null) {
                writer.print(",\"current\":{ \"name\":\"" + status.getCurrentName() + "\" }");
            }
            // current : { name:x, state:x, text:x }
            // items : [ { name:x, state:x, text:x } ]
        writer.print("}");
    }
    
    protected void printStatus(HttpServletResponse response) 
        throws IOException {
        
        String contextPath = mServletContext.getContextPath();
        
        // send json-encoded status response
        response.setContentType("application/json");
        
        // write the json status
        PrintWriter writer = response.getWriter();
        writer.print("{");
            writer.print("\"timestamp\":" + System.currentTimeMillis() + ",");
            writer.print("\"initialized\":" + isInitialized() + ",");
            writer.print("\"running\":" + isRunning() + ",");
            writer.print("\"contextPath\":\"" + contextPath + "\",");
            writer.print("\"statuses\":[");
                writer.print("{");
                    writer.print("\"id\":1,");
                    writer.print("\"state\":\"valid\",");
                    writer.print("\"progress\":false,");
                    writer.print("\"text\":\"Loading configuration...\",");
                    writer.print("\"name\":\"configuration\"");
                writer.print("},");
                
                writer.print("{");
                    printStatus(writer, mPluginListener);
                writer.print("},");

                writer.print("{");
                    printStatus(writer, mApplicationListener);
                writer.print("},");

                writer.print("{");
                    printStatus(writer, mTemplateListener);
                writer.print("}");
            
            writer.print("]");
        writer.print("}");
        
        // flush the buffer
        response.flushBuffer();
    }
    
    protected boolean processStatus(HttpServletRequest request,
                                    HttpServletResponse response) 
        throws IOException {
        
        String path = request.getPathInfo();
            
        // check password
        String adminKey = mProperties.getString("admin.key");
        String adminValue = mProperties.getString("admin.value");
        if (AdminApplication.adminCheck(adminKey, adminValue, 
                                        request, response)) {

            // check if status request
            if ("/system/status.json".equals(path)) {
                printStatus(response);
                return true;
            }
            
            // once we have initialized, no need to further process
            if (isRunning()) { return false; }
            
            // verify our path matches the expected path
            String pattern = mProperties.getString("startup.path");
            if (pattern != null && path.matches(pattern)) {
                    
                // verify the startup file that was provided
                String resource = mProperties.getString("startup.file");
                if (resource != null) {
                    InputStream input = 
                        TeaServlet.class.getResourceAsStream(resource);
                    
                    // copy the data to the response if valid
                    if (input != null) {
                        int read = 0;
                        byte[] data = new byte[512];
                
                        input = new BufferedInputStream(input);
                        ServletOutputStream output = 
                            response.getOutputStream();

                        while ((read = input.read(data)) >= 0) {
                            output.write(data, 0, read);
                        }
                
                        input.close();
                
                        response.flushBuffer();
                        return true;
                    }
                }
            }
        }
        
        // handle error code if not yet initialized
        if (!isInitialized()) {
            // request not processed so send uninitialized error code
            int errorCode = mProperties.getInt("startup.codes.initializing", 503);
            response.sendError(errorCode);
            return true;
        }
        
        // nothing to process
        return false;
    }
    
    /**
     * Process the user's http get request. Process the template that maps to
     * the URI that was hit.
     * @param request  the user's http request
     * @param response  the user's http response
     */
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException, IOException
    {
        if (processStatus(request, response)) {
            return;
        }
        
        if (!isRunning()) {
            int errorCode = mProperties.getInt("startup.codes.error", 503);
            response.sendError(errorCode);
            return;
        }
        
        if (mUseSpiderableRequest) {
            request = new SpiderableRequest(request,
                                            mQuerySeparator,
                                            mParameterSeparator,
                                            mValueSeparator);
        }

        // start transaction
        TeaServletTransaction tsTrans = 
            getEngine().createTransaction(request, response, true);
    
        // load associated request/response
        ApplicationRequest appRequest = tsTrans.getRequest();
        ApplicationResponse appResponse = tsTrans.getResponse();

        // process template
        processTemplate(appRequest, appResponse);
        appResponse.finish();
        
        // flush the output
        response.flushBuffer();
    }

    private TeaServletEngine createTeaServletEngine()
        throws ServletException {

        TeaServletEngineImpl engine;
        if (mProperties.getBoolean("autocompile", false)) {
            engine = new TeaServletEngineImplDev();
        } else {
            engine = new TeaServletEngineImpl();             
        }

        Object tsea = mServletContext.getAttribute(ENGINE_ATTR);
        
        if (tsea == null || !(tsea instanceof TeaServletEngine[])) {
            tsea = new TeaServletEngine[]{engine};
        }
        else {
            TeaServletEngine[] old_tsea = (TeaServletEngine[])tsea;
            int old_length = old_tsea.length;
            tsea = new TeaServletEngine[old_length + 1];
            int i;
            for (i=0; i < old_length; i++) {
                ((TeaServletEngine[])tsea)[i+1] = old_tsea[i];
            }
            ((TeaServletEngine[])tsea)[0] = engine;
        }
        mServletContext.setAttribute(ENGINE_ATTR, tsea);
    
        return engine;
    }
    
    private ServletContext setServletContext(ServletConfig config) {
        
        ServletContext context = new FilteredServletContext
            (config.getServletContext())
            {
                public Object getAttribute(String name) {
                    if (name == TeaServlet.class.getName()) {
                        return TeaServlet.this;
                    }
                    else {
                        return super.getAttribute(name);
                    }
                }
            };
        return context;
    }

    private String setServletName(ServletConfig config) {
        try {
            return config.getServletName();
        }
        catch (LinkageError e) {
            // If running in under Servlet API 2.1, look for special attribute
            // that provides servlet name.
            try {
                return (String)getServletContext()
                    .getAttribute("servletName");
            }
            catch (ClassCastException e2) {
                return "TeaServlet";
            }
        }
    }

    private void createLog(final ServletContext context) {

        if (mLog == null) {
            try {
                mLog = (Log)context.getAttribute
                    ("org.teatrove.trove.log.Log");
            }
            catch (ClassCastException e) {
            }

            // Log instance may not be provided, so make a Log that passes
            // messages to standard ServletContext log.
            if (mLog == null) {
                mLog = new Log(getServletName(), null);
                
                mLog.addLogListener(new LogListener() {
                        public void logMessage(LogEvent e) {
                            String message = e.getMessage();
                            if (message != null) {
                                context.log(message);
                            }
                        }
                        
                        public void logException(LogEvent e) {
                            String message = e.getMessage();
                            Throwable t = e.getException();
                            if (t == null) {
                                context.log(message);
                            }
                            else {
                                context.log(message, t);
                            }
                        }
                    });
            }
        }

        String fullStackTrace = mProperties.getString("log.fullStackTrace", "false");
        if (!fullStackTrace.equals("true")) {
        	mLog = new TeaLog(mLog);
        }
    }

    private void createMemoryLog(Log log) {

        if (log != null) {

            // Create memory log listener.
            mLogEvents = 
                Collections.synchronizedList(new LinkedList<LogEvent>());

            // The maximum number of log events to store in memory.
            final int logEventsMax = mProperties.getInt("log.max", 100);

            log.addRootLogListener(new TeaLogListener() {
                public void logMessage(LogEvent e) {
                    checkSize();
                    mLogEvents.add(e);
                }

                public void logException(LogEvent e) {
                    checkSize();
                    mLogEvents.add(e);
                }

                public void logTeaStackTrace(TeaLogEvent e) {
                    checkSize();
                    mLogEvents.add(e);
				}				

                private void checkSize() {
                    while (mLogEvents.size() >= logEventsMax) {
                        mLogEvents.remove(0);
                    }
                }
            });

            logVersionInfo(TeaServlet.class, "TeaServlet", log);
            log.info("Copyright (C) 1999-2012 TeaTrove http://teatrove.org");
            logVersionInfo(TemplateLoader.class, "Tea", log);
            log.info("Copyright (C) 1997-2012 TeaTrove http://teatrove.org");
        }
    }

    private void createListeners() {
        mPluginListener = new TeaServletStatusListener(2, "plugins");
        mApplicationListener = new TeaServletStatusListener(3, "applications");
        mTemplateListener = new TeaServletStatusListener(4, "templates");
    }
    
    private void discoverProperties() throws ServletException {
        // load our class loader to resolve resources
        ClassLoader cloader = TeaServlet.class.getClassLoader();
        
        // specify list of configs to load
        final String[] configNames = { 
            "META-INF/teaservlet.xml", "META-INF/teaservlet.properties" 
        };

        // load each set of configs and process accordingly
        for (String configName : configNames) {

            if (mDebugEnabled) {
                mServletContext.log("Searching for " + configName + " configurations");
            }
            
            // attempt to load all resources of given name
            Enumeration<URL> configs = null;
            try { configs = cloader.getResources(configName); }
            catch (IOException ioe) {
                throw new ServletException(
                    "unable to load " + configName + " from classpath", ioe);
            }
            
            while (configs.hasMoreElements()) {
                InputStream input = null;
                PropertyMap properties = null;
                
                // load associated properties of config
                URL url = configs.nextElement();
                if (mDebugEnabled) {
                    mServletContext.log("Loading configuration " + url.toExternalForm());
                }

                try { 
                    input = url.openStream();
                    properties = mResourceFactory.getResourceAsProperties
                    (
                         url.getPath(), input
                    );
                }
                catch (IOException ioe) {
                    throw new ServletException(
                        "unable to load " + url.toExternalForm(), ioe);
                }
                
                // merge in properties into application properties
                mergeProperties(properties);
    
                // TODO: scan annotations within associated JAR/class path
            }
        }
    }

    private void additiveMerge(PropertyMap properties, String property,
                               String separator) {
        String merging = properties.getString(property);
        if (merging != null) {
            String existing = mProperties.getString(property);
            if (existing == null || existing.length() == 0) {
                mProperties.put(property, merging);
            }
            else {
                mProperties.put(property, existing + separator + merging);
            }
        }
    }

    private void mergeProperties(PropertyMap properties) {
        
        // merge in properties w/o overwriting
        mProperties.putDefaults(properties);

        // custom merges
        additiveMerge(properties, "template.path", ";");
        additiveMerge(properties, "template.imports", ";");
        additiveMerge(properties, "assets.path", ",");
    }
    
    private void loadDefaults() throws ServletException {
        try {
            loadDefaults(mProperties, new HashSet<String>());
        }
        catch (Exception e) {
        	mServletContext.log("unable to load defaults", e);
        }
    }
    
    private PropertyMap loadProperties(PropertyMap factoryProps) 
        throws Exception {
        
        String className = null;
        PropertyMapFactory factory = null;
        if (factoryProps != null && factoryProps.size() > 0
            && (className = factoryProps.getString("class")) != null) {
            if (mDebugEnabled) {
                mServletContext.log("Using property factory: " + className);
            }
            
            // Load and use custom PropertyMapFactory.
            Class<?> factoryClass = Class.forName(className);
            java.lang.reflect.Constructor<?> ctor =
                factoryClass.getConstructor(new Class[]{Map.class});
            factory = (PropertyMapFactory)
                ctor.newInstance(new Object[]{factoryProps.subMap("init")});
        } else if (factoryProps == null){
        	if (mDebugEnabled) {
        	    mServletContext.log("factoryProps is null.");
        	}
    	} else if (factoryProps.size() == 0) {
    	    if (mDebugEnabled) {
    	        mServletContext.log("factory props size is 0.");
    	    }
    	} else {
    	    if (mDebugEnabled) {
    	        mServletContext.log("className is null");
    	    }
    	}
        
        // return properties
        PropertyMap result = null;
        if (factory != null) {
        	result = factory.createProperties();
        	if (mDebugEnabled) {
        	    mServletContext.log("properties: " + result);
        	}
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private void loadDefaults(PropertyMap properties, Set<String> files)
        throws Exception
    {
        // update substitutions if provided
        PropertyMap substitutions = properties.subMap("substitutions");
        if (substitutions != null && substitutions.size() > 0) {
            PropertyMap subs = SubstitutionFactory.getSubstitutions
            (
                substitutions, mResourceFactory
            );
            
            if (subs != null) {
                mSubstitutions.putAll(subs);
            }
            
            properties.remove("substitutions");
        }

        // Get file and perform substitution of env variables/system props
        String fileName = properties.getString("properties.file");
        if (mDebugEnabled) {
            mServletContext.log("properties.file: " + fileName);
        }
        
        if (fileName != null) {
            fileName = SubstitutionFactory.substitute(fileName, mSubstitutions);            
        }
        
        // parse file if not yet parsed
        if (fileName != null && !files.contains(fileName)) {
            // Prevent properties file cycle.
            files.add(fileName);
            
            // load properties
            PropertyMap props =
                mResourceFactory.getResourceAsProperties(fileName);
            
            if (props != null) {
                properties.putAll(props);
            }
            
            loadDefaults(properties, files);
        }
        else {
            PropertyMap factoryProps = 
                properties.subMap("properties.factory");
            if (factoryProps != null && factoryProps.size() > 0) {
            	PropertyMap map = loadProperties(factoryProps);
                properties.putAll(map);
            }
        }
    }

    private PluginContext loadPlugins(PropertyMap properties, Log log) {
        
        PluginContext plug = new PluginContext(mResourceFactory);
        PluginFactoryConfig config = new PluginFactoryConfigSupport
            (properties, log, plug);
        
        try {
            PluginFactory.createPlugins(config, mPluginListener);
        } 
        catch (PluginFactoryException e) {
            log.warn("Error loading plugins.");
            log.warn(e);
        }

        return plug;        
    }

    /**
     * Inserts a plugin to expose parts of the teaservlet via the 
     * EngineAccess interface.
     *
    private void createEngineAccessPlugin(PluginContext context) {

        Plugin engineAccess = new EngineAccessPlugin(this);
        context.addPlugin(engineAccess);
        context.addPluginListener(engineAccess);
    }
    */

    private boolean processResource(ApplicationRequest appRequest,
                                    ApplicationResponse appResponse) 
        throws IOException {
        
        // get the associated system path
        String requestURI = null;
        if ((requestURI = appRequest.getPathInfo()) == null) {
            String context = appRequest.getContextPath();
            
            requestURI = appRequest.getRequestURI();
            if (requestURI.startsWith(context)) {
                requestURI = requestURI.substring(context.length());
            }
        }

        // check for valid asset
        Asset asset = getEngine().getAssetEngine().getAsset(requestURI);
        if (asset == null) {
            return false;
        }
        
        // set mime type
        appResponse.setContentType(asset.getMimeType());
        
        // write contents
        int read = -1;
        byte[] contents = new byte[1024];
        InputStream input = asset.getInputStream();
        ServletOutputStream output = appResponse.getOutputStream();
        while ((read = input.read(contents)) >= 0) {
            output.write(contents, 0, read);
        }

        // complete response
        appResponse.finish();
        
        // success
        return true;
    }

    /**
     * Creates a transaction from the provided request and response and 
     * then processes that transaction by executing the target template.
     *
     * @param request  the user's http request
     * @param response  the user's http response
     */
    private boolean processTemplate(ApplicationRequest appRequest,
                                    ApplicationResponse appResponse)
        throws IOException {
        
        // check if redirect or erroring out
        if (appResponse.isRedirectOrError()) {
            return false;
        }

        // set initial content type and helper attributes
        appResponse.setContentType("text/html");
        appRequest.setAttribute(this.getClass().getName(), this);

        // lookup template
        Template template = (Template)appRequest.getTemplate();

        // process as resource if no template available
        if (template == null) {
            if (!processResource(appRequest, appResponse)) {
                appResponse.sendError(404);
                return false;
            }
            return true;
        }
        
        long endTime = 0L;
        long startTime = 0L;
        long contentLength = 0;

        TemplateStats templateStats = null; 
        if (mInstrumentationEnabled) {
            templateStats = mTeaServletRequestStats.getStats(template.getName());
        }
        
        try {
	        Object[] params = null;
	        try {    
	            if (templateStats != null) {
	                templateStats.incrementServicing();
	            }

	            // Fill in the parameters to pass to the template.
	            Class<?>[] paramTypes = template.getParameterTypes();
	            if (paramTypes.length == 0) {
	                params = NO_PARAMS;
	            }
	            else {
	                params = new Object[paramTypes.length];
	                String[] paramNames = template.getParameterNames();
	                for (int i=0; i<paramNames.length; i++) {
	                    String paramName = paramNames[i];
	                    if (paramName == null) {
	                        continue;
	                    }
	                
	                    Class<?> paramType = paramTypes[i];
	                
	                    if (!paramType.isArray()) {
	                        String value = appRequest.getParameter(paramName);
	                        if (value == null || paramType == String.class) {
	                            params[i] = value;
	                        }
	                        else {
	                            params[i] = convertParameter(value, paramType);
	                        }
	                    }
	                    else {
	                        String[] values =
	                            appRequest.getParameterValues(paramName);
	                        if (values == null || paramType == String[].class) {
	                            params[i] = values;
	                        }
	                        else {
	                            paramType = paramType.getComponentType();
	                            Object converted =
	                                Array.newInstance(paramType, values.length);
	                            params[i] = converted;
	                            for (int j=0; j<values.length; j++) {
	                                Array.set
	                                    (converted, j,
	                                     convertParameter(values[j], paramType));
	                            }
	                        }
	                    }
	                }
	            }
	
	            startTime = System.currentTimeMillis();
	            try {
	                try {
	                    appRequest.getTemplate()
	                        .execute(appResponse.getHttpContext(), params);
	                }
	                catch (ContextCreationException cce) {
	                    // unwrap the inner exception
	                    throw (Exception)cce.getUndeclaredThrowable();
	                }
	            }
	            catch (AbortTemplateException e) {
	                if (DEBUG) {
	                    mLog.debug("Template execution aborted!");
	                    mLog.debug(e);
	                }
	            }
	            catch (RuntimeException e) {
	                if (getEngine()
	                    .getTemplateSource().isExceptionGuardianEnabled()) {
	                    // Just log the error and use what the template wrote out.
	                    mLog.error(e);
	                }
	                else {
	                    throw new ServletException(e);
	                }
	            }
	            catch (IOException e) {
	            	// TODO: shouldn't we be throwing this as a ServletException?
	            	//       otherwise its not logged to the TeaLog.
	                throw e;
	            }
	            catch (ServletException e) {
	                throw e;
	            }
	            catch (Exception e) {
	                throw new ServletException(e);
	            }
	            // TODO: shouldn't we be catching errors and not just exceptions?
	        	//       otherwise its not logged to the TeaLog.
	            finally {
					endTime = System.currentTimeMillis();
					if (appRequest instanceof TeaServletStats) {
						long duration = endTime - startTime;
						((TeaServletStats)appRequest).setTemplateDuration(duration);
					}
	            }
	
	            if (DEBUG) {
	                mLog.debug("Finished executing template");
	            }
	        }
	        catch (ServletException e) {
	            // Log exception
	            StringBuffer msg = new StringBuffer();
	            msg.append("Error processing request for ");
	            msg.append(appRequest.getRequestURI());
	            if (appRequest.getQueryString() != null) {
	                msg.append('?');
	                msg.append(appRequest.getQueryString());
	            }
	            mLog.error(msg.toString());
	
	            Throwable t = e;
	            while (t instanceof ServletException) {
	                e = (ServletException)t;
	                if (e.getRootCause() != null) {
	                    String message = e.getMessage();
	                    if (message != null && message.length() > 0) {
	                        mLog.error(message);
	                    }
	                    mLog.error(t = e.getRootCause());
	                }
	                else {
	                    mLog.error(e);
	                    break;
	                }
	            }
	
	            // Internal server error unless header is already set
	            if (!appResponse.isRedirectOrError()) {
	                String displayMessage = e.getLocalizedMessage();
	                if (displayMessage == null || displayMessage.length() == 0) {
	                    appResponse.sendError
	                        (ApplicationResponse.SC_INTERNAL_SERVER_ERROR);
	                }
	                else {
	                    appResponse.sendError
	                        (ApplicationResponse.SC_INTERNAL_SERVER_ERROR, displayMessage);
	                }
	            }
	          
	        }
	        contentLength = appResponse.getResponseBuffer().getByteCount();
	        appResponse.finish();
	        if (templateStats != null) {
    	        templateStats.decrementServicing();
    	        templateStats.log(startTime, endTime, contentLength, params);
	        }
        } catch (Exception e) {
            if (templateStats != null) {
                templateStats.decrementServicing();
            }
        }
		return true;
    }


    private void logVersionInfo(Class<?> clazz, String title, Log log) {
        Package pack = clazz.getPackage();
        String version = null;
        if (pack != null) {
            if (pack.getImplementationTitle() != null) {
                title = pack.getImplementationTitle();
            }
            version = pack.getImplementationVersion();
        }
        if (version == null) {
            try {
                String classname = clazz.getName();
                Class<?> packinf = Class.forName(
                    classname.substring(0,classname.lastIndexOf('.'))
                    + ".PackageInfo");
                java.lang.reflect.Method mo = packinf.getMethod(
                    "getProductVersion");
                version = mo.invoke(null).toString();
            }
            catch (Exception pie) {
                log.info("PackageInfo not found");
            }
            if (version == null) {
                version = "<unknown>";
            }
        }
        log.info(title + " version " + version);
    }

    /**
     * Converts the given HTTP parameter value to the requested type so that
     * it can be passed directly as a template parameter. This method is called
     * if the template that is directly requested accepts non-String
     * parameters, and the request provides non-null values for those
     * parameters. The template may request an array of values for a parameter,
     * in which case this method is called not to create the array, but rather
     * to convert any elements put into the array.
     *
     * <p>This implementation supports converting parameters of the following
     * types, and returns null for all others. If a conversion fails, null
     * is returned.
     *
     * <ul>
     * <li>Integer
     * <li>Long
     * <li>Float
     * <li>Double
     * <li>Number
     * <li>Object
     * </ul>
     *
     * When converting to Number, an instance of Integer, Long or Double is
     * returned, depending on which parse succeeds first. A request for an
     * Object returns either a Number or a String, depending on the success of
     * a number parse.
     *
     * @param value non-null HTTP parameter value to convert
     * @param toType Type to convert to
     * @return an instance of toType or null
     */
    protected Object convertParameter(String value, Class<?> toType) {
        if (toType == Boolean.class) {
            return ! "".equals(value) ? new Boolean("true".equals(value)) : 
                null;
        }
        if (toType == Integer.class) {
            try {
                return new Integer(value);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        else if (toType == Long.class) {
            try {
                return new Long(value);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        else if (toType == Float.class) {
            try {
                return new Float(value);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        else if (toType == Double.class) {
            try {
                return new Double(value);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        else if (toType == Number.class || toType == Object.class) {
            try {
                return new Integer(value);
            }
            catch (NumberFormatException e) {
            }
            try {
                return new Long(value);
            }
            catch (NumberFormatException e) {
            }
            try {
                return new Double(value);
            }
            catch (NumberFormatException e) {
                return (toType == Object.class) ? value : null;
            }
        }
        else {
            return null;
        }
    }
    
    private class TeaServletStatusListener implements StatusListener {

        private int id;
        private String type;
        
        private int current;
        private int total;
        private boolean active;
        private boolean complete;
        private String currentName;
        
        public TeaServletStatusListener(int id, String type) {
            this.id = id;
            this.type = type;
        }

        public int getId() { return this.id; }
        public String getType() { return this.type; }
        
        public int getCurrent() { return current; }
        public int getTotal() { return total; }
        public String getCurrentName() { return currentName; }
        public boolean isActive() { return active; }
        public boolean isComplete() { return complete; }
        
        public double getPercent() {
            if (total == 0) { return 0.0; }
            else { return (100.0 * current / total); }
        }
        
        @Override
        public void statusStarted(StatusEvent event) {
            update(event);
            this.active = true;
        }
        
        @Override
        public void statusUpdate(StatusEvent event) {
            update(event);
        }
        
        @Override
        public void statusCompleted(StatusEvent event) {
            update(event);
            this.active = false;
            this.complete = true;
        }
        
        protected void update(StatusEvent event) {
            this.current = event.getCurrent();
            this.total = event.getTotal();
            this.currentName = event.getCurrentName();
        }
    }
}
