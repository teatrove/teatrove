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

import org.teatrove.tea.engine.ContextCreationException;
import org.teatrove.tea.engine.Template;
import org.teatrove.tea.log.TeaLog;
import org.teatrove.tea.log.TeaLogEvent;
import org.teatrove.tea.log.TeaLogListener;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.teaservlet.stats.TeaServletRequestStats;
import org.teatrove.teaservlet.stats.TemplateStats;
import org.teatrove.teaservlet.util.FilteredServletContext;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.log.LogListener;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.trove.util.PropertyParser;
import org.teatrove.trove.util.plugin.PluginContext;
import org.teatrove.trove.util.plugin.PluginFactory;
import org.teatrove.trove.util.plugin.PluginFactoryConfig;
import org.teatrove.trove.util.plugin.PluginFactoryConfigSupport;
import org.teatrove.trove.util.plugin.PluginFactoryException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 
    private static final Object[] NO_PARAMS = new Object[0];
    private static final boolean DEBUG = false;
    private static final String ENGINE_ATTR =
        "org.teatrove.teaservlet.TeaServletEngine";

    private TeaServletEngine mEngine;

    private static final String WAR_TEMPLATE_SOURCE_PATH = "/WEB-INF/tea";
    private static final String WAR_TEMPLATE_CLASS_PATH = "/WEB-INF/teaclasses";

    private Log mLog;
    /** Captured log events that were likely also written to a log file. */
    private List mLogEvents;

    private PropertyMap mProperties;
    private ServletContext mServletContext;
    private String mServletName;

    private String mQuerySeparator;
    private String mParameterSeparator;
    private String mValueSeparator;
    private boolean mUseSpiderableRequest;
    
    private TeaServletRequestStats mTeaServletRequestStats;

    /**
     * Initializes the TeaServlet. Creates the logger and loads the user's
     * application.
     * @param config  the servlet config
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String ver = System.getProperty("java.version");
        if (ver.startsWith("0.") || ver.startsWith("1.2") ||
            ver.startsWith("1.3")) {
            System.err.println
                ("The TeaServlet requires Java 1.4 or higher to run properly");
        }

        mProperties = new PropertyMap();
        Enumeration e = config.getInitParameterNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            mProperties.put(key, config.getInitParameter(key));
        }

        loadDefaults(mProperties);
        mServletContext = setServletContext(config);
        mServletName = setServletName(config);
        createLog(mServletContext);
        mLog.applyProperties(mProperties.subMap("log"));
        createMemoryLog(mLog);

        mTeaServletRequestStats = TeaServletRequestStats.getInstance();
        
        mTeaServletRequestStats.applyProperties(mProperties.subMap("stats"));

        //
        // simple hack to determine if the admin applications have even 
        // been mentioned in the file.
        //
        if (!mProperties.containsValue("org.teatrove.teaservlet.AdminApplication") &&
            !mProperties.containsValue("org.teatrove.barista.admin.TQAdminApplication")) {
            mLog.warn("org.teatrove.teaservlet.AdminApplication: not properly configured");
        }

        if (!mProperties.containsValue(
            "org.teatrove.barista.admin.BaristaAdminApplication")) {
            mLog.warn("org.teatrove.barista.admin.BaristaAdminApplication: not properly configured");
        }

        PluginContext pluginContext = loadPlugins(mProperties, mLog);

        mEngine = createTeaServletEngine();

        // Check to see if tea templates are being deployed within a WAR bundle.  
        // If so, append this path to the existing template path.  For Tomcat.
        Set rp = mServletContext.getResourcePaths(WAR_TEMPLATE_SOURCE_PATH);
        if (rp != null && rp.size() > 0) {
            String warPath = mServletContext.getRealPath("/") + WAR_TEMPLATE_SOURCE_PATH;
            String newPath = mProperties.getString("template.path");
            newPath = newPath != null && newPath.length() > 0 ? newPath = newPath : warPath;
            mProperties.put("template.path", newPath);
        }

        if (mProperties.getString("template.classes") == null) {
            Set crp = mServletContext.getResourcePaths("/WEB-INF");
            if (crp != null && crp.size() > 0)
                mProperties.put("template.classes", mServletContext.getRealPath("/") + WAR_TEMPLATE_CLASS_PATH);
        }

        ((TeaServletEngineImpl)getEngine()).startEngine(mProperties,
                                                        mServletContext, 
                                                        mServletName,
                                                        mLog, 
                                                        mLogEvents,
                                                        pluginContext);

        mQuerySeparator = mProperties.getString("separator.query", "?");
        mParameterSeparator = mProperties
            .getString("separator.parameter", "&");
        mValueSeparator = mProperties.getString("separator.value", "=");

        mUseSpiderableRequest =
            (!"?".equals(mQuerySeparator)) ||
            (!"&".equals(mParameterSeparator)) ||
            (!"=".equals(mValueSeparator));

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
        if (mUseSpiderableRequest) {
            request = new SpiderableRequest(request,
                                            mQuerySeparator,
                                            mParameterSeparator,
                                            mValueSeparator);
        }

        response.setContentType("text/html");
        request.setAttribute(this.getClass().getName(), this);

        processTemplate(request, response);
        response.flushBuffer();
    }

    //private initialization methods


    private TeaServletEngine createTeaServletEngine()
        throws ServletException {

        TeaServletEngineImpl engine = new TeaServletEngineImpl();

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
            mLogEvents = Collections.synchronizedList(new LinkedList());

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
            log.info("Copyright (C) 1999-2011 TeaTrove http://teatrove.org");
            logVersionInfo(TemplateLoader.class, "Tea", log);
            log.info("Copyright (C) 1997-2011 TeaTrove http://teatrove.org");
        }
    }

    private void loadDefaults(PropertyMap properties) {
        try {
            loadDefaults(properties, new HashSet(), null);
        }
        catch (IOException e) {
            // This should never happen because null was passed in as Reader.
            mLog.warn(e);
        }
    }

    private void loadDefaults(PropertyMap properties, Set files, Reader reader)
        throws IOException
    {
        String fileName;

        if (reader == null) {
            fileName = properties.getString("properties.file");
        }
        else {
            PropertyMap defaultProps = new PropertyMap();
            try {
                PropertyParser parser = new PropertyParser(defaultProps);
                parser.parse(new BufferedReader(reader));
            }
            finally {
                reader.close();
            }
            properties.putDefaults(defaultProps);
          fileName = properties.getString("properties.file");
        }

        if (fileName != null && !files.contains(fileName)) {
          
            // Prevent properties file cycle.
            files.add(fileName);
         
            try {
                loadDefaults(properties, files, new FileReader(fileName));
            }
            catch (IOException e) {
                // now try looking for the properties as a resource
                try {
                    if (getServletContext().getResourceAsStream(fileName) != null) {
                    
                        loadDefaults(properties, 
                                     files, 
                                     new InputStreamReader
                                         (getServletContext()
                                          .getResourceAsStream(fileName)));
                    }
                    else {
                        InputStream in = this.getClass().
                                     getResourceAsStream(! fileName.startsWith("/") ? "/" + fileName : fileName);
                        if (in != null)
                            loadDefaults(properties, files, new InputStreamReader(in));
                        else
                            mLog.error("Cannot locate properties at path : " + fileName);
                    }
                }
                catch (IOException ioe) {
                    mLog.warn("Error reading properties file: " 
                              + fileName);
                    mLog.warn(e.toString());
                    mLog.warn(ioe.toString());
                }
            }
        }
        else {
            try {
                PropertyMap factoryProps = 
                    properties.subMap("properties.factory");
                String className = null;
                if (factoryProps != null && factoryProps.size() > 0
                    && (className = factoryProps.getString("class")) != null) {
                
                    // Load and use custom PropertyMapFactory.
                    Class factoryClass = Class.forName(className);
                    java.lang.reflect.Constructor ctor =
                        factoryClass
                        .getConstructor(new Class[]{Map.class});
                    PropertyMapFactory factory = (PropertyMapFactory)ctor
                        .newInstance(new Object[]{factoryProps
                                                  .subMap("init")});
                    properties.putDefaults(factory.createProperties());
                }
            }
            catch (Exception e) {
                mLog.warn(e);
            }
        }
    }

    private PluginContext loadPlugins(PropertyMap properties, Log log) {
        
        PluginContext plug = new PluginContext();
        PluginFactoryConfig config = new PluginFactoryConfigSupport
            (properties, log, plug);
        
        try {
            PluginFactory.createPlugins(config);
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


    /**
     * Creates a transaction from the provided request and response and 
     * then processes that transaction by executing the target template.
     *
     * @param request  the user's http request
     * @param response  the user's http response
     */
    private void processTemplate(HttpServletRequest request,
                                 HttpServletResponse response)
        throws IOException {
        
        TeaServletTransaction tsTrans = 
            getEngine().createTransaction(request, response, true);
    
        ApplicationRequest appRequest = tsTrans.getRequest();
        ApplicationResponse appResponse = tsTrans.getResponse();

        if (appResponse.isRedirectOrError()) {
            return;
        }

        Template template = (Template)appRequest.getTemplate();

        if (template == null) {
            appResponse.sendError
                (ApplicationResponse.SC_NOT_FOUND, appRequest.getRequestURI());
            return;
        }
        
        long endTime = 0L;
        long startTime = 0L;
        long contentLength = 0;

        TemplateStats templateStats = null;
        try {
	        Object[] params = null;
	        templateStats = mTeaServletRequestStats.getStats(template.getName());
	        try {    
	            // Fill in the parameters to pass to the template.
	        	templateStats.incrementServicing();
	            Class[] paramTypes = template.getParameterTypes();
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
	                
	                    Class paramType = paramTypes[i];
	                
	                    if (!paramType.isArray()) {
	                        String value = request.getParameter(paramName);
	                        if (value == null || paramType == String.class) {
	                            params[i] = value;
	                        }
	                        else {
	                            params[i] = convertParameter(value, paramType);
	                        }
	                    }
	                    else {
	                        String[] values =
	                            request.getParameterValues(paramName);
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
	
	            if (DEBUG) {
	                mLog.debug("Executing template");
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
					if (request instanceof TeaServletStats) {
						long duration = endTime - startTime;
						((TeaServletStats)request).setTemplateDuration(duration);
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
	            msg.append(request.getRequestURI());
	            if (request.getQueryString() != null) {
	                msg.append('?');
	                msg.append(request.getQueryString());
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
	            if (!tsTrans.getResponse().isRedirectOrError()) {
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
	        templateStats.decrementServicing();
	        templateStats.log(startTime, endTime, contentLength, params);
        } catch (Exception e) {
        	 templateStats.decrementServicing();
        }
    }


    private void logVersionInfo(Class clazz, String title, Log log) {
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
                Class packinf = Class.forName(
                    classname.substring(0,classname.lastIndexOf('.'))
                    + ".PackageInfo");
                java.lang.reflect.Method mo = packinf.getMethod(
                    "getProductVersion",null);
                version = mo.invoke(null,null).toString();
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
    protected Object convertParameter(String value, Class toType) {
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
}







