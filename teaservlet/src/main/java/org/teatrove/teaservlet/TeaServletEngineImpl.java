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

import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.Template;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateSource;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.Utils;
import org.teatrove.trove.util.plugin.Plugin;
import org.teatrove.trove.util.plugin.PluginContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Jonathan Colwell
 * @version

 */
public class TeaServletEngineImpl implements TeaServletEngine {

    private static final boolean DEBUG = false;

    // fields needed for implementing the TeaServletEngine interface
    private Log mLog;
    private PropertyMap mProperties;
    private ServletContext mServletContext;
    private String mServletName;

    private ApplicationDepot mApplicationDepot;
    private TeaServletTemplateSource mTemplateSource;
    private List mLogEvents;

    private PluginContext mPluginContext;

    protected void startEngine(PropertyMap properties,
                               ServletContext servletContext,
                               String servletName,
                               Log log,
                               List memLog,
                               PluginContext plug)
        throws ServletException {

        try {
            setProperties(properties);
            setServletContext(servletContext);
            setServletName(servletName);
            setLog(log);
            setLogEvents(memLog);
            setPluginContext(plug);
            mApplicationDepot = new ApplicationDepot(this);

            mLog.debug("loading templates");
            getTemplateSource().compileTemplates(null, false);
        }
        catch (Exception e) {
                throw (ServletException) new ServletException(e).initCause(e);
        }
    }

    // Methods required to implement TeaServletEngine.

    public String getInitParameter(String name) {
        return mProperties.getString(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(mProperties.keySet());
    }

    public String getServletName() {
        return mServletName;
    }

    private void setServletName(String name) {
        mServletName = name;
    }

    public ServletContext getServletContext() {
        return mServletContext;
    }

    private void setServletContext(ServletContext context) {
        mServletContext = context;
    }

    public String getName() {
        return mLog.getName();
    }

    public Log getLog() {
        return mLog;
    }

    private void setLog(Log log) {
        mLog = log;
    }

    public Plugin getPlugin(String name) {
        if (mPluginContext == null) {
            return null;
        }
        return mPluginContext.getPlugin(name);
    }

    public Map getPlugins() {
        if (mPluginContext == null) {
            return Utils.VOID_MAP;
        }
        return mPluginContext.getPlugins();
    }

    private void setPluginContext(PluginContext pContext) {
        mPluginContext = pContext;
    }

    public PropertyMap getProperties() {
        return mProperties;
    }

    private void setProperties(PropertyMap properties) {
        mProperties = properties;
    }

    public TeaServletTransaction createTransaction
        (HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        return createTransaction(request, response, false);
    }

    public TeaServletTransaction createTransaction
        (HttpServletRequest request, HttpServletResponse response,
         boolean lookupTemplate)
        throws IOException {


        try {

            TeaServletTemplateSource templateSrc = getTemplateSource();

            TeaServletContextSource contextSrc = (TeaServletContextSource)
                templateSrc.getContextSource();

            Template template = null;

            if (lookupTemplate) {
                // get template path
                String path;
                if ((path = request.getPathInfo()) == null) {
                    if ((path = request.getServletPath()) == null) {
                        path = "/";
                    }
                    else {
                        // Strip off any extension.
                        int index = path.lastIndexOf('.');
                        if (index >= 0) {
                            path = path.substring(0, index);
                        }
                    }
                }

                if (DEBUG) {
                    // add some comic relief!
                    mLog.debug("aagggghhh... i've been hit! (" + path + ")");
                    mLog.debug("Finding template for " + path);
                }

                // Find the matching template.
                template = findTemplate(path, request, response, templateSrc);
            }

            // Wrap the user's http response.
            ApplicationResponse appResponse =
                new ApplicationResponseImpl(response, this);


            ApplicationRequest appRequest =
                (lookupTemplate
                 ? (new ApplicationRequestImpl
                     (request,
                      contextSrc
                      .getApplicationContextTypes(),
                      template))
                 : (new ApplicationRequestImpl
                     (request,
                      contextSrc.getApplicationContextTypes(),
                      templateSrc.getTemplateLoader())));

            try {
                /*
                 * TODO: I dislike this circular logic of having the context
                 * contain the response and the response containing the
                 * context, changing this will require a redesign of the
                 * response.
                 */
                ((ApplicationResponseImpl)appResponse)
                    .setRequestAndHttpContext(createHttpContext(appRequest,
                                                                appResponse,
                                                                contextSrc),
                                              appRequest);
            }
            catch (Exception e) {
                // Initialize the JDK 1.4+ exception chain,
                //   otherwise ServletException for Servlet 2.3 swallows the cause
                //noinspection ThrowableInstanceNeverThrown
                throw (ServletException) new ServletException(e).initCause(e);
            }

            return new RequestAndResponse(appRequest, appResponse);

        }
        catch (ServletException se) {
            // If we are going to swallow the exception, at least log it.
            getLog().warn(se);
            return null;
        }
    }

    /**
     * Returns the ApplicationDepot, which is used by the admin functions.
     */
    public synchronized ApplicationDepot getApplicationDepot() {
        return mApplicationDepot;
    }

    public synchronized TeaServletTemplateSource getTemplateSource() {
        if (mTemplateSource == null) {
            mTemplateSource =
                createTemplateSource(getApplicationDepot().getContextSource());
        }
        return mTemplateSource;
    }

    /**
     * This method performs the steps necessary to load a new/updated
     * context source.  It does not swap the mTemplateSource member
     * until it is fully initialized (its templates have been
     * compiled).
     */
    public TemplateCompilationResults reloadContextAndTemplates(boolean all)
        throws Exception {

        // Create the new merged context source
        TeaServletContextSource contextSource =
            new TeaServletContextSource(
                mApplicationDepot.getClass().getClassLoader(),
                mApplicationDepot,
                getServletContext(),
                getLog(),
                true,
                mProperties.getBoolean("profiling.enabled", true));

        // Create a new template source using the newly loaded context
        // source from the new application depot
        TeaServletTemplateSource templateSrc =
            createTemplateSource(contextSource);

        // Recompile all templates using the new context source
        TemplateCompilationResults results =
            templateSrc.compileTemplates(null, all);

        // The new context source and template source are ready for
        // use, set the member variables
        synchronized(this) {
            mApplicationDepot.setContextSource(contextSource);
            mTemplateSource = templateSrc;
        }

        // Return the compilation results
        return results;
    }

    /**
     * Destroys the TeaServlet and the user's application.
     */
    public void destroy() {
        if (mApplicationDepot != null) {
            mLog.info("Destroying ApplicationDepot");
            mApplicationDepot.destroy();
        }
        for (Iterator i = getPlugins().values().iterator(); i.hasNext(); )
            ((Plugin) i.next()).destroy();
    }


    /**
     * Returns the lines that have been written to the log file. This is used
     * by the admin functions.
     */
    public LogEvent[] getLogEvents() {
        if (mLogEvents == null) {
            return new LogEvent[0];
        }
        else {
            LogEvent[] events = new LogEvent[mLogEvents.size()];
            return (LogEvent[])mLogEvents.toArray(events);
        }
    }

    private void setLogEvents(List memLog) {
        mLogEvents = memLog;
    }

    public String[] getTemplatePaths() {
        try {
            String[] paths = getTemplateSource().getKnownTemplateNames();
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].replace('.', '/');
            }
            return paths;
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Finds a template based on the given URI. If path ends in a slash, revert
     * to loading default template. If default not found or not specified,
     * return null.
     *
     * @param uri  the URI of the template to find
     * @return the template that maps to the URI or null if no template maps to
     *         the URI
     */
    public Template findTemplate(String uri,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws ServletException, IOException {

        return findTemplate(uri, request, response, getTemplateSource());
    }

    public Template findTemplate(String uri,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 TemplateSource templateSrc)
        throws ServletException, IOException {

        Template template = null;
        try {
            // If path ends in a slash, revert to loading default template. If
            // default not found or not specified, return null.
            boolean useDefault = uri.endsWith("/");

            // Trim slashes and replace with dots.
            while (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            while (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            String name = uri.replace('/', '.');

            // Look up template if not trying to use default.
            if (!useDefault) {
                // Find template that matches the uri
                try {
                    template = templateSrc.getTemplate(name);
                }
                catch (ClassNotFoundException e) {
                    mLog.debug("Can't find template \"" + name + "\": " + e);
                    template = null;
                }
            }

            // Use default if no template found so far.
            if ((template == null)
                && (templateSrc instanceof TeaServletTemplateSource)) {
                TeaServletTemplateSource tsTsrc = (TeaServletTemplateSource)
                    templateSrc;
                if (tsTsrc.getDefaultTemplateName() != null) {
                    if  (name.length() == 0) {
                        name = tsTsrc.getDefaultTemplateName();
                    }
                    else {
                        name = name + '.'
                            + tsTsrc.getDefaultTemplateName();
                    }
                }
                try {
                    template = tsTsrc.getTemplate(name);

                    // Redirect if no slash on end of URI.
                    if (template != null && !useDefault) {
                        StringBuffer location =
                            new StringBuffer(request.getRequestURI());
                        int length = location.length();
                        if (length == 0
                            || location.charAt(length - 1) != '/') {
                            location.append('/');
                        }
                        String query = request.getQueryString();
                        if (query != null) {
                            location.append('?').append(query);
                        }
                        response.setStatus(response.SC_MOVED_PERMANENTLY);
                        response.sendRedirect(location.toString());
                    }
                }
                catch (ClassNotFoundException e) {
                    mLog.debug("Can't find default template \"" +
                               name + "\": " + e);
                }
            }
        }
        catch (NoClassDefFoundError e) {
            // The file system let the class load, but because some file
            // systems support multiple ways in which a file can be found, the
            // class's exact name may not match. Just report the template
            // as not being found.
            return null;
        }
        catch (NoSuchMethodException e) {
            // Initialize the JDK 1.4+ exception chain,
            //   otherwise ServletException for Servlet 2.3 swallows the cause
            //noinspection ThrowableInstanceNeverThrown
            throw (ServletException) new ServletException("Template at \"" + uri
                                       + "\" is invalid", e).initCause(e);
        }
        catch (LinkageError e) {
            // Initialize the JDK 1.4+ exception chain,
            //   otherwise ServletException for Servlet 2.3 swallows the cause
            //noinspection ThrowableInstanceNeverThrown
            throw (ServletException) new ServletException("Template at \"" + uri
                                       + "\" is invalid", e).initCause(e);
        }
        return template;
    }

    /**
     * Lets external classes use the HttpContext for their own, possibly
     * malicious purposes.
     */
    public HttpContext createHttpContext(ApplicationRequest req,
                                         ApplicationResponse resp)
        throws Exception {

        Template template = (Template) req.getTemplate();

        return createHttpContext(req, resp,
                                 template.getTemplateSource().getContextSource());
    }

    private HttpContext createHttpContext(ApplicationRequest req,
                                          ApplicationResponse resp,
                                          ContextSource cs)
        throws Exception {

        return (HttpContext)cs
            .createContext(new RequestAndResponse(req, resp));
    }

    /**
     * Create a template source using the composite context passed to
     * the method.
     *
     * @param contextSource The composite context source for all the
     * applications in the ApplicationDepot.
     *
     * @return A newly created template source.
     */
    private TeaServletTemplateSource createTemplateSource(
                                         ContextSource contextSource) {
        return TeaServletTemplateSource.createTemplateSource(
            (TeaServletContextSource) contextSource,
            getProperties().subMap("template"),
            getLog());
    }
}

