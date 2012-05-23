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

import java.beans.*;
import java.io.*;
import java.util.*;
import java.net.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.teatrove.trove.log.Log;

import org.teatrove.teaservlet.util.ClassDB;
import org.teatrove.trove.util.BeanComparator;
import org.teatrove.trove.util.PropertyMap;

import org.teatrove.trove.io.ByteBuffer;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.tea.engine.Template;
import org.teatrove.tea.engine.TemplateCompilationResults;

import org.teatrove.tea.compiler.TemplateRepository;

import org.teatrove.teatools.*;
import org.teatrove.teaservlet.util.ServerNote;
//REMOTE stuff
import java.rmi.RemoteException;

import org.teatrove.teaservlet.stats.*;
import org.teatrove.teaservlet.util.cluster.Restartable;
import org.teatrove.teaservlet.util.cluster.Clustered;
import org.teatrove.teaservlet.util.cluster.ClusterManager;

/**
 * The Admin application defines functions for administering the TeaServlet.
 *
 * @author Reece Wilton, Brian S O'Neill, Jonathan Colwell
 */
public class AdminApplication implements AdminApp {
    protected ApplicationConfig mConfig;
    protected Log mLog;
    protected TeaServlet mTeaServlet;
    protected String mAdminKey;
    protected String mAdminValue;
    protected AppAdminLinks[] mAdminLinks;
    protected Map mNotes;
    protected int mMaxNotes;
    protected int mNoteAge;
    protected ClassDB mClassDB;

    // REMOTE stuff
    TeaServletAdmin mAdmin;
    String mClusterName;
    int mRmiPort,mMulticastPort;
    InetAddress mMulticastGroup;
    ClusterManager mClusterManager;

    /**
     * Initializes the Application. Accepts the following initialization
     * parameters:
     * <pre>
     * admin.key - the security parameter key
     * admin.value - the security parameter value
     * notes.max - number of notes to store for each group
     * notes.age - how long to hang on to a note, in seconds
     * </pre>
     *
     * @param config the application's configuration object
     */
    public void init(ApplicationConfig config) {
        mConfig = config;
        mLog = config.getLog();
        PropertyMap props = config.getProperties();
        // Get the admin key/value.
        mAdminKey = props.getString("admin.key");
        mAdminValue = props.getString("admin.value");
        mMaxNotes = props.getInt("notes.max", 20);
        mNoteAge = props.getInt("notes.age", 60 * 60 * 24 * 7);

        mClassDB = null;
        if (props.getBoolean("classdb.enabled")) {
            mClassDB = new ClassDB();

            String ignoredPackages = props.getString("classdb.ignored");
            if (ignoredPackages != null) {
                String[] pkgs = ignoredPackages.split("[\\s,;\r\n]+");
                for (String pkg : pkgs) {
                    mClassDB.addIgnoredPackages(pkg.trim());
                }
            }

            String allowedPackages = props.getString("classdb.allowed");
            if (allowedPackages != null) {
                String[] pkgs = allowedPackages.split("[\\s,;\r\n]+");
                for (String pkg : pkgs) {
                    mClassDB.addAllowedPackages(pkg.trim());
                }
            }

            try { mClassDB.scanAll(config.getServletContext()); }
            catch (IOException ioe) {
                mClassDB = null;
                mLog.error("unable to scan class index");
                mLog.error(ioe);
            }
        }

        List serverList = new ArrayList();

        /* OLD CLUSTER stuff
           // Get the server list.
           String clusterServers = config.getInitParameter("cluster.servers");
           if (clusterServers != null) {
           StringTokenizer st = new StringTokenizer
           (clusterServers, ",;");

           while (st.hasMoreTokens()) {
           serverList.add(st.nextToken().trim());
           }

           if (serverList.isEmpty()) {
           mLog.warn("No servers specified for this cluster");
           }
           }

           mClusteredServers = (String[])serverList.toArray(new String[0]);
        */

        // Save the TeaServlet reference. The TeaServlet places an instance of
        // itself in the config in this hidden way because applications don't
        // need direct access to the TeaServlet.
        ServletContext context = config.getServletContext();
        mTeaServlet =
            (TeaServlet)context.getAttribute(TeaServlet.class.getName());
        if (mTeaServlet == null) {
            mLog.warn("TeaServlet attribute not found");
        }
        else {

            //REMOTE stuff
            try {
                String servers = config
                    .getInitParameter("cluster.servers");
                String clusterName = config
                    .getInitParameter("cluster.name");
                int rmiPort = 1099;
                int multicastPort = 1099;
                InetAddress multicastGroup = null;
                String netInterface = config
                    .getInitParameter("cluster.localNet");

                try {
                    String rmiPortStr = 
                        config.getInitParameter("cluster.rmi.port");
                    if (rmiPortStr != null) {
                        rmiPort = Integer.parseInt(rmiPortStr);
                    }

                    String multicastPortStr = 
                        config.getInitParameter("cluster.multicast.port");
                    if (multicastPortStr != null) {
                        multicastPort = Integer.parseInt(multicastPortStr);
                    }

                    String multicastGroupStr =
                        config.getInitParameter("cluster.multicast.group");
                    if (multicastGroupStr != null) {
                        multicastGroup = InetAddress.getByName(multicastGroupStr);
                    }
                }
                catch (NumberFormatException nfe) { mLog.debug(nfe); }
                catch (UnknownHostException uhe) { mLog.warn(uhe); }
                if (multicastGroup != null) {

                    int multicastTtl = 1;
                    String ttlStr = config.getInitParameter("cluster.multicast.ttl");
                    if(ttlStr!=null && ttlStr.trim().length()>0) {
                        try {
                            multicastTtl = Integer.parseInt(ttlStr);
                        } catch(NumberFormatException ex) {
                            mLog.warn("using default for cluster.multicast.ttl: "+multicastTtl
                                    +" because of error parsing configured value: "+ttlStr);
                            mLog.warn(ex);
                        }
                    }

                    mClusterManager = new ClusterManager(getAdmin(),
                                                         clusterName,
                                                         null,
                                                         multicastGroup,
                                                         multicastPort,
                                                         rmiPort,
                                                         netInterface,
                                                         servers);

                    mLog.info("setting multicast ttl to: "+multicastTtl);
                    mClusterManager.setMulticastTtl(multicastTtl);

                    mClusterManager.joinCluster();
                    mClusterManager.launchAuto();

                }
                else if (servers != null) {
                    mClusterManager = new ClusterManager(getAdmin(),
                                                         clusterName,
                                                         null,
                                                         rmiPort,
                                                         netInterface,
                                                         servers);
                }
            }
            catch (Exception e) {
                mLog.warn(e);
            }
        }
    }

    public void destroy() {
        if (mClusterManager != null) {
            mClusterManager.killAuto();
        }
    }

    /**
     * Returns an instance of {@link AdminContext}.
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new ContextImpl(request, response);
    }

    /**
     * Returns {@link AdminContext}.class.
     */
    public Class getContextType() {
        return AdminContext.class;
    }

    void adminCheck(ApplicationRequest request, ApplicationResponse response)
        throws AbortTemplateException
    {
        if (mAdminKey == null) {
            return;
        }

        // Check for admin key.
        String adminParam = request.getParameter(mAdminKey);

        // Look in cookie for admin param.
        if (adminParam == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie cookie = cookies[i];
                    if (cookie.getName().equals(mAdminKey)) {
                        adminParam = cookie.getValue();
                    }
                }
            }
        }

        if (adminParam != null && adminParam.equals(mAdminValue)) {
            // Set the admin param in the cookie.
            Cookie c = new Cookie(mAdminKey, adminParam);
            // Save cookie for 7 days.
            c.setMaxAge(24 * 60 * 60 * 7);
            c.setPath("/");
            response.addCookie(c);
        }
        else {
            // User is unauthorized.

            mLog.warn("Unauthorized Admin access to " +
                      request.getRequestURI() +
                      " from " + request.getRemoteAddr() +
                      " - " + request.getRemoteHost() +
                      " - " + request.getRemoteUser());

            try {
                response.sendError
                    (response.SC_NOT_FOUND, request.getRequestURI());
            }
            catch (IOException e) {
            }

            throw new AbortTemplateException();
        }
    }

    /**
     * This implementation uses hard coded link information, but other
     * applications can dynamically determine their admin links.
     */
    public AppAdminLinks getAdminLinks() {

        AppAdminLinks links = new AppAdminLinks(mConfig.getName());
        //links.addAdminLink("Instrumentation","/system/console?page=instrumentation");
        //links.addAdminLink("Dashboard","/system/console?page=dashboard");
        //links.addAdminLink("Janitor","/system/console?page=janitor");
        //links.addAdminLink("Compile","/system/console?page=compile");
        //links.addAdminLink("Templates","/system/console?page=templates");
        //links.addAdminLink("Functions","/system/console?page=functions");
        //links.addAdminLink("Applications", "/system/console?page=applications");
        //links.addAdminLink("Logs","/system/console?page=logs");
        //links.addAdminLink("Servlet Engine", "/system/console?page=servlet_engine");
        //links.addAdminLink("Echo Request", "/system/console?page=echo_request");
        
        links.addAdminLink("Templates","/system/teaservlet/AdminTemplates");
        links.addAdminLink("Functions","/system/teaservlet/AdminFunctions");
        links.addAdminLink("Applications",
                           "/system/teaservlet/AdminApplications");
        links.addAdminLink("Logs","/system/teaservlet/LogViewer");
        links.addAdminLink("Servlet Engine",
                           "/system/teaservlet/AdminServletEngine");
        return links;
    }

    private TeaServletAdmin getAdmin() {
        if (mAdmin == null) {
            mAdmin = new TeaServletAdmin(mTeaServlet.getEngine());
        }
        return mAdmin;
    }


    public class ContextImpl extends TeaToolsUtils implements AdminContext {
        protected ApplicationRequest mRequest;
        protected ApplicationResponse mResponse;
        private TeaServletAdmin mTSAdmin;
        private TemplateCompilationResults mCompilationResults;
        private List mServerStatus;

        protected ContextImpl(ApplicationRequest request,
                              ApplicationResponse response) {
            mRequest = request;
            mResponse = response;
        }

        public TemplateCompilationResults getCompilationResults() {
            return mCompilationResults;
        }

        public FunctionInfo getFunction(String methodName) {
            try {
                return getTeaServletAdmin().getFunction(methodName);
            }
            catch (ServletException se) { return null; }
        }

        public void setTemplateOrdering(String orderBy) {
            if (mTSAdmin != null)
                mTSAdmin.setTemplateOrdering(orderBy);
        }

        public TeaServletAdmin getTeaServletAdmin() throws ServletException {
            if (mTSAdmin != null) {
                return mTSAdmin;
            }
            adminCheck(mRequest, mResponse);
            mTSAdmin = getAdmin();
            // Cluster related Stuff

            if (mClusterManager != null) {
                mTSAdmin.setClusteredServers(mClusterManager
                                             .resolveServerNames());
            }

            // Admin Link Stuff
            if (mTSAdmin.getAdminLinks() == null) {

                // add the Teaservlet links first.
                List links = new ArrayList();
                links.add(getAdminLinks());

                // go through the apps looking for other AdminApps
                Iterator it = mRequest.getApplicationContextTypes()
                    .keySet().iterator();

                while (it.hasNext()) {
                    Application app = (Application)it.next();
                    if (app instanceof AdminApp
                        && !AdminApplication.this.equals(app)) {
                        links.add(((AdminApp)app).getAdminLinks());
                    }
                }
                mTSAdmin
                    .setAdminLinks((AppAdminLinks[])links
                                   .toArray(new AppAdminLinks[links.size()]));
            }


            // Does the user want to reload templates?
            String param = mRequest.getParameter("reloadTemplates");
            if (param != null) {
                int command = TeaServletAdmin.RELOAD_TEMPLATE_CHANGES;
                if (contextChanged()) {
                    command = TeaServletAdmin.RELOAD_CONTEXT;
                }
                else if ("all".equals(param)) {
                    command = TeaServletAdmin.RELOAD_ALL_TEMPLATES;
                } else if("selected".equals(param)) {
                    command = TeaServletAdmin.RELOAD_SELECTED_TEMPLATE_CHANGES;
                }

                Integer all = new Integer(command);

                try {
                    if (mRequest.getParameter("cluster") != null
                        && mClusterManager != null) {

                        if(command==TeaServletAdmin.RELOAD_SELECTED_TEMPLATE_CHANGES) {
                            String[] selectedTemplates = mRequest.getParameterValues("selectedTemplates");
                            mCompilationResults = clusterReload(new Object[] { all, selectedTemplates });
                        }
                        else {
                            mCompilationResults = clusterReload(all);
                        }
                    }
                    else {
                        if(command==TeaServletAdmin.RELOAD_SELECTED_TEMPLATE_CHANGES) {
                            String[] selectedTemplates = mRequest.getParameterValues("selectedTemplates");
                            mCompilationResults =
                                (TemplateCompilationResults)mTSAdmin.restart(new Object[] { all, selectedTemplates });
                        }
                        else {
                            mCompilationResults =
                                (TemplateCompilationResults)mTSAdmin.restart(all);
                        }
                    }
                }
                catch (RemoteException re) {
                    mLog.warn(re);
                }
            }

            // Does the user want to reset statistics?
            param = mRequest.getParameter("resetStatistics");
            if (param != null)
                TeaServletInvocationStats.getInstance().reset();

            return mTSAdmin;
        }

        public Class getClassForName(String classname) {
            try {
                ClassLoader cl = mTeaServlet.getEngine().getApplicationDepot()
                    .getContextType().getClassLoader();
                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                return cl.loadClass(classname);
            }
            catch (Exception cpe) {
                mLog.warn(cpe);
                return null;
            }
        }

        /**
         * Returns a list of class objects for each known subclass.
         */
        public String[] getSubclassesForName(String classname) {
            if (mClassDB == null) {
                return null;
            }

            Set<String> subclasses = mClassDB.getSubclasses(classname);
            return (subclasses == null
                        ? new String[0]
                        : subclasses.toArray(new String[subclasses.size()]));
        }

        /**
         * Streams the structural bytes of the named class via the
         * HttpResponse.
         */
        public void streamClassBytes(String className)
            throws AbortTemplateException
        {
            adminCheck(mRequest, mResponse);

            if (className == null) {
                return;
            }

            String classResource = className.replace('.', '/') + ".class";

            ClassLoader cl;

            try {
                cl = mTeaServlet.getEngine().getApplicationDepot()
                    .getContextSource().getContextType().getClassLoader();
            }
            catch (Exception e) {
                cl = ClassLoader.getSystemClassLoader();
            }

            InputStream in = cl.getResourceAsStream(classResource);

            if (in != null) {
                int len = 4000;
                byte[] b = new byte[len];
                int bytesRead = 0;
                ByteBuffer buffer = mResponse.getResponseBuffer();

                try {
                    while ((bytesRead = in.read(b, 0, len)) > 0) {
                        buffer.append(b, 0, bytesRead);
                    }

                    in.close();
                }
                catch (Exception e) {
                    mResponse.setStatus(mResponse.SC_NOT_FOUND);
                    mLog.debug(e);
                }
            }
            else {
                mResponse.setStatus(mResponse.SC_NOT_FOUND);
            }

            mResponse.setContentType("application/java");
        }

        public TemplateRepository.TemplateInfo 
                getTemplateInfo(String templateName) {
            return TemplateRepository.getInstance().
                getTemplateInfo(templateName);
        }
        
        public TemplateRepository.TemplateInfo[] getTemplateInfos() {
        	return TemplateRepository.getInstance().getTemplateInfos();
		}

        public TemplateRepository.TemplateInfo[]
                getCallers(String templateName) {
            return TemplateRepository.getInstance().
                getCallers(templateName);
        }

        public TemplateRepository.TemplateInfo[]
                getMethodCallers(MethodDescriptor methodDesc) {
            return TemplateRepository.getInstance().
                getMethodCallers(methodDesc);
        }

        public boolean isTemplateRepositoryEnabled() {
            return TemplateRepository.isInitialized();
        }

        public String formatTypeDesc(TypeDesc type) {
            return TemplateRepository.formatTypeDesc(type);
        }

        public TeaServletInvocationStats.Stats getStatistics(String caller, String callee) {
            return TeaServletInvocationStats.getInstance().getStatistics(caller, callee);
        }

        public void resetStatistics() {
            TeaServletInvocationStats.getInstance().reset();
        }
        
        public void resetStatistics(String caller, String callee) {
            TeaServletInvocationStats.getInstance().reset(caller, callee);
        }

        /**
         * allows a template to dynamically call another template
         */
        public void dynamicTemplateCall(String templateName)
            throws Exception {
            dynamicTemplateCall(templateName,new Object[0]);
        }

        /**
         * allows a template to dynamically call another template
         * this time with parameters.
         */
        public void dynamicTemplateCall(String templateName, Object[] params)
            throws Exception
        {
            org.teatrove.tea.runtime.Context context = mResponse
                .getHttpContext();

            Template currentTemplate =
                (Template)mRequest.getTemplate();

            org.teatrove.tea.runtime.TemplateLoader.Template td =
                mTeaServlet.getEngine().findTemplate(templateName,
                                                     mRequest,
                                                     mResponse,
                                                     (TeaServletTemplateSource)
                                                     currentTemplate
                                                     .getTemplateSource());

            if (td==null) {
                throw new ServletException("dynamicTemplateCall() could not find template: " + templateName
                        +" called from template: " + currentTemplate.getSourcePath());
            }

            // make sure we have the right number and types of parameters
            String[] paramNames = td.getParameterNames();
            Object[] oldParams = params;
            if (oldParams == null
                || oldParams.length != paramNames.length) {
                params = new Object[paramNames.length];

                /*
                 * if the provided parameters don't match up with the
                 * required parameters first try to fill in the params
                 * from the request then fill the rest with nulls.
                 * NOTE: if the parameters explicitly passed in do not
                 * match the template signature, none of those parameters
                 * will be used
                 */

                for (int j=0;j<paramNames.length;j++) {
                    params[j] = mRequest.getParameter(paramNames[j]);
                    if (params[j] == null && oldParams.length > j) {
                        params[j] = oldParams[j];
                    }
                }
            }

            td.execute(context, params);
        }

        public Object obtainContextByName(String appName)
            throws ServletException {
            ApplicationInfo[] applications =
                getTeaServletAdmin().getApplications();
            for (int j=0;j<applications.length;j++) {
                if (appName.equals(applications[j].getName())) {
                    return ((Application)applications[j].getValue())
                        .createContext(mRequest,mResponse);
                }
            }
            return null;
        }


        public ServerStatus[] getReloadStatusOfServers() {
            ServerStatus[] statusArray;

            if (mServerStatus == null) {
                statusArray = new ServerStatus[0];
            }
            else {
                statusArray = (ServerStatus[])mServerStatus
                    .toArray(new ServerStatus[mServerStatus.size()]);

                Comparator c = BeanComparator.forClass(ServerStatus.class)
                    .orderBy("statusCode").reverse()
                    .orderBy("serverName");
                Arrays.sort(statusArray, c);
            }
            return statusArray;
        }

        protected void setServerReloadStatus(String name,
                                          int statusCode, String message) {
            if (mServerStatus == null) {
                mServerStatus = new Vector();
            }
            mServerStatus.add(new ServerStatus(name, statusCode, message));
        }


        public Set addNote(String ID, String contents, int lifespan) {

            Set noteSet = null;
            if (mNotes == null) {
                mNotes = Collections.synchronizedSortedMap(new TreeMap());
            }
            if (ID != null) {
                if ((noteSet = (Set)mNotes.get(ID)) == null) {
                    Comparator comp = BeanComparator.forClass(ServerNote.class)
                        .orderBy("timestamp").orderBy("contents");
                    noteSet = Collections
                        .synchronizedSortedSet(new TreeSet(comp));
                    mNotes.put(ID, noteSet);
                }
                else {
                    Date now = new Date();
                    synchronized (noteSet) {
                        Iterator expireIt = noteSet.iterator();
                        while (expireIt.hasNext()) {
                            ServerNote nextNote = (ServerNote)expireIt.next();
                            if (now.after(nextNote.getExpiration())) {
                                expireIt.remove();
                            }
                        }
                    }
                }
                if (contents != null) {
                    if (lifespan == 0) {
                        lifespan = mNoteAge;
                    }
                    ServerNote note = new ServerNote(contents,
                                                     lifespan);
                    noteSet.add(note);
                }
                return noteSet;
            }
            return mNotes.keySet();
        }
        
        /* new template raw and aggregate statistics */
        
        /**
         * Returns the template raw and aggregate statistics so as to
         * better understand the performance of this template through time.
         * 
         * @param fullTemplateName the name of the template with '.' as a seperator.
         * 
         * @return the template stats for this given template.
         */
        public TemplateStats getTemplateStats(String fullTemplateName) {
            return TeaServletRequestStats.getInstance().getStats(fullTemplateName);
        }
        
        /**
         * Returns an array of template stats.
         * 
         * Returns the template raw and aggregate statistics so as to
         * better understand the performance of templates through time.
         * 
         * @return the template stats for this given template.
         */
        public TemplateStats[] getTemplateStats() {
        	return TeaServletRequestStats.getInstance().getTemplateStats();
        }
        
        /**
         * Returns object that manages template raw and aggregate statistics.
         */
        public TeaServletRequestStats getTeaServletRequestStats() {
            return TeaServletRequestStats.getInstance();
        }
        
        /**
         * Sets the raw window size. The rawWindowSize defines how many
         * raw statistics are going to be kept in a circular queue.
         * 
         * Resets all statistics.
         * 
         * @param rawWindowSize
         */
        public void setRawWindowSize(int rawWindowSize) {
            TeaServletRequestStats.getInstance().setRawWindowSize(rawWindowSize);
        }
        
        /**
         * Sets the aggregate interval size. The aggregateWindowSize defines how many
         * aggregate intervals are going to be kept in a circular queue.
         * 
         * Resets all statistics.
         * 
         * @param aggregateWindowSize
         */
        public void setAggregateWindowSize(int aggregateWindowSize) {
            TeaServletRequestStats.getInstance().setAggregateWindowSize(aggregateWindowSize);
        }
        
        /**
    	 * Returns the aggregate intervals for the specified startTime and stopTime.
    	 * Any intervals that contain these two endpoints lie between them will be
    	 * included.
    	 * 
    	 * @param templateStats the template stats object to query.
    	 * @param startTime the start time to filter on.
    	 * @param stopTime the stop time to filter on.
    	 * 
    	 * @return aggregate intervals for the specified interval.
    	 */
        public AggregateInterval[] getAggregateIntervals(TemplateStats templateStats, 
        		                                         long startTime, 
        		                                         long stopTime) {
       		return templateStats.getAggregateIntervals(startTime, stopTime);
        }   
        
        /**
    	 * Returns an aggregate interval for the raw data filtered 
    	 * by start and stop time.
    	 * 
    	 * @see TemplateStats.getAggregateIntervalForRawData()
    	 * 
    	 * @param startTime the start time to filter on.
    	 * @param stopTime the stop time to filter on.
    	 * @return an aggregate interval for the raw data.
    	 */
        public AggregateInterval getAggregateIntervalForRawData(TemplateStats templateStats, 
        														long startTime, long stopTime) {
        	return templateStats.getAggregateIntervalForRawData(startTime, stopTime);
        }
        
        /**
         * Returns a statistical summary of a set of AggregateIntervals.
         * 
         * @param intervals the AggregateIntervals to summarize.
         * @return a statistical summary of AggregateIntervals.
         */
        public AggregateSummary getDurationAggregateSummary(AggregateInterval[] intervals) {
        	return AggregateSummary.getDurationAggregateSummary(intervals);
        }
        
        /**
    	 * This method returns the Aggregate interval containing the specified
    	 * time stamp.
    	 * 
    	 * @param intervals the aggregate intervals to search.
    	 * @param time aggregate intervals will be found for this time.
    	 * @return the aggregate interval containing the specified time.
    	 */
    	public int search(AggregateInterval[] intervals, long time) {
    		return TemplateStats.search(intervals, time);
    	}
    	
        /**
         * Searches for a AggregateInterval which contains the time passed in.
         *
         * @param intervals
         *            sorted array of AggregateIntervals
         * @param time
         *            key to search for
         * @param begin
         *            start position in the index
         * @param end
         *            one past the end position in the index
         * @return Integer index to key. -1 if not found
         */
        public int search(AggregateInterval[] intervals, long time, int begin, int end) {
        	return TemplateStats.search(intervals, time, begin, end);
        }
        
        /**
         * Adds a milestone for this template such as a compile event.
         * 
         * @param templateStats the template stats object to query.
         * @param description a description of the milestone
         * @param time the time of the milestone
         */
        public void addMilestone(TemplateStats templateStats, String description, long time) {
        	templateStats.addMilestone(new Milestone(description, time));
        }
        
        /**
         * Returns all milestones for this template.
         * @param templateStats the template stats object to query.
         * @return all milestones
         */
        public Milestone[] getMilestones(TemplateStats templateStats) {
        	return templateStats.getMilestones();
        }
        
        /**
         * Returns all milestones for this template between the start and stopTime.
         * 
         * @param templateStats the template stats object to query.
         * @param startTime
         * @param stopTime
         * @return the milestones in the requested interval.
         */
        public Milestone[] getMilestones(TemplateStats templateStats, long startTime, long stopTime) {
        	return templateStats.getMilestones(startTime, stopTime);
        }
        
        /**
         * Resets the raw and aggregate template statistics.
         */
        public void resetTemplateStats() {
        	TeaServletRequestStats.getInstance().reset();
        }
        
        /**
         * Resets the raw and aggregate template statistics. If null is passed in all templates
         * statistics will be reset.
         * 
         * @param templateName the name of the template to reset statistics for.
         */
        public void resetTemplateStats(String templateName) {
        	if (templateName != null) {
        		TemplateStats templateStats = getTemplateStats(templateName);
        		if (templateStats != null) {
        			templateStats.reset();
        		}
        	} else {
        		resetTemplateStats();
        	}
        }

        /* not currently used. TODO: delete these when sure we won't use again

        public FeatureDescription[] sort(FeatureDescription[] fds) {
            return sortDescriptions(fds);
        }

        public FeatureDescriptor[] sort(FeatureDescriptor[] fds) {
            return sortDescriptors(fds);
        }

        public Object[] sort(Object[] objArray) {
            Object[] dolly = (Object[])objArray.clone();
            Arrays.sort(dolly);
            return dolly;
        }

        public PropertyDescriptor[] getBeanProperties(Class beanClass)
            throws IntrospectionException
        {
            if (beanClass == null) {
                return null;
            }

            PropertyDescriptor[] pdarray = new PropertyDescriptor[0];

            Collection props =
                BeanAnalyzer.getAllProperties(beanClass).values();
            pdarray = (PropertyDescriptor[])props.toArray(pdarray);
            Comparator pdcomp = BeanComparator
                .forClass(PropertyDescriptor.class).orderBy("name");
            java.util.Arrays.sort(pdarray, pdcomp);

            return pdarray;
        }
        */

        /**
         * @return true if the context type has changed making a context
         * reload necessary.
         */
        private boolean contextChanged() {

            TeaServletAdmin admin = getAdmin();
            ApplicationInfo[] apps = admin.getApplications();
            Map expectedTypes = mRequest.getApplicationContextTypes();

            for (int j = 0; j < apps.length; j++) {
                Application app = (Application)apps[j].getValue();
                Class currentContextType = app.getContextType();
                Class expectedContextType = (Class)expectedTypes.get(app);
                if (currentContextType != expectedContextType) {
                    return true;
                }
            }
            return false;
        }

        private TemplateCompilationResults clusterReload(Object all)
            throws ServletException, RemoteException {
            /* OLD Cluster Stuff
               StringBuffer uri = new StringBuffer();

               uri.append(mRequest.getContextPath());
               uri.append(mRequest.getServletPath());
               uri.append("/system/teaservlet/ClusterReload?reloadTemplates");

               if (all) {
               uri.append("=all");
               }

               if (mAdminKey != null && mAdminValue != null) {
               uri.append('&');
               uri.append(mAdminKey);
               uri.append('=');
               uri.append(mAdminValue);
               }

               int port = mRequest.getServerPort();
            */
            if (mClusterManager != null) {

                //mClusterManager.resolveServerNames();

                TemplateCompilationResults results =
                    new TemplateCompilationResults
                        (Collections.synchronizedMap(new TreeMap()),
                         new Hashtable());

                Clustered[] peers =
                    (Clustered[])mClusterManager.getCluster()
                    .getKnownPeers();

                final ClusterThread[] ct = new ClusterThread[peers.length];

                for (int i=0; i<peers.length; i++) {
                    Clustered peer = peers[i];
                    ct[i] = new ClusterThread(this, results,
                                              peer, all);
                    ct[i].start();
                }

                // collect the launched threads.

                for (int i=0; i<ct.length; i++) {
                    if (ct[i] != null) {
                        try {
                            ct[i].join();
                        }
                        catch (InterruptedException e) {
                            mLog.warn(e);
                        }
                    }
                }

                return results;
            }
            throw new RemoteException("kinda hard to reload across a cluster without a ClusterManager");
        }

        public TeaToolsContext.HandyClassInfo getHandyClassInfo(String fullClassName) {
            if (fullClassName != null) {
                if (fullClassName.charAt(0) == '[' && fullClassName.charAt(fullClassName.length() - 1) != ';') {
                    char typeChar = fullClassName.charAt(fullClassName.lastIndexOf('[') + 1);
                    switch (typeChar) {
                        case 'B':
                            return getHandyClassInfo(Boolean.TYPE);
                        case 'I':
                            return getHandyClassInfo(Integer.TYPE);
                        case 'D':
                            return getHandyClassInfo(Double.TYPE);
                        case 'L':
                            return getHandyClassInfo(Long.TYPE);
                        case 'S':
                            return getHandyClassInfo(Short.TYPE);
                    }
                }
                else {
                    StringBuffer fb = new StringBuffer(fullClassName);
                    // strip array descriptors from class names, we know its an array from the type node.
                    if (fullClassName.charAt(0) == '[') {
                        fb.deleteCharAt(fullClassName.lastIndexOf('[') + 1);
                        int i = 0;
                        while ((i = fb.indexOf("[")) != -1)
                            fb.deleteCharAt(i);
                        if ((i = fb.indexOf(";")) != -1)
                            fb.deleteCharAt(i);
                    }

                    Class clazz = getClassForName(fb.toString());
                    if (clazz != null) {
                        return getHandyClassInfo(clazz);
                    }
                }
            }
            return null;
        }

        public TemplateCompilationResults checkTemplates(boolean forceAll) throws Exception {
            return mTSAdmin.checkTemplates(forceAll, null);
        }

        public TemplateCompilationResults checkTemplates(String[] templateNames) throws Exception {
            return mTSAdmin.checkTemplates(false, templateNames);
        }

        public TeaToolsContext.HandyClassInfo getHandyClassInfo(Class clazz) {
            if (clazz != null) {
                return new HandyClassInfoImpl(clazz);
            }
            return null;
        }

        public class HandyClassInfoImpl extends TypeDescription
            implements TeaToolsContext.HandyClassInfo
        {

            HandyClassInfoImpl(Class clazz) {
                super(clazz,ContextImpl.this);
            }
        }
    }


    public class ServerStatus {
        private String mServerName;
        private String mMessage;
        private int mStatusCode;

        public ServerStatus(String name, int statusCode, String message) {
            mServerName = name;
            mMessage = message;
            mStatusCode = statusCode;
        }

        public String getServerName() {
            return mServerName;
        }

        public String getMessage() {
            return mMessage;
        }

        public int getStatusCode() {
            return mStatusCode;
        }
    }


    private class ClusterThread extends Thread {
        private TemplateCompilationResults mResults;
        private Clustered mClusterPeer;
        private Object mAll;
        private ContextImpl mContext;

        public ClusterThread(ContextImpl cont,
                             TemplateCompilationResults res,
                             Clustered peer,
                             Integer all) {
            this(cont, res, peer, (Object)all);
        }

        public ClusterThread(ContextImpl cont,
                             TemplateCompilationResults res,
                             Clustered peer,
                             Object all) {
            mContext = cont;
            mResults = res;
            mClusterPeer = peer;
            mAll = all;

        }

        public void run() {
            try {

                TemplateCompilationResults res = (TemplateCompilationResults)
                    ((Restartable)mClusterPeer).restart(mAll);

                if (res != null) {
                    if (res.isAlreadyReloading()) {
                        mResults.setAlreadyReloading(true);
                        mContext.setServerReloadStatus(mClusterPeer
                                                   .getServerName(),
                                                   299,
                                                   "template reload already in progress");
                    }
                    else {
                        mResults.appendTemplates(res.getReloadedTemplates());
                        mResults.appendErrors(res.getTemplateErrors());
                    }
                }
                else {
                    mContext.setServerReloadStatus(mClusterPeer.getServerName(),
                                               299,
                                               "error encountered while reloading templates");
                }
            }
            catch (RemoteException re) {
                try {
                    mContext.setServerReloadStatus(mClusterPeer.getServerName(),
                                               299,
                                               "error encountered while reloading templates");
                    mLog.warn(re);
                    mClusterManager.getCluster().removePeer(mClusterPeer);
                    mLog.warn("removing " + mClusterPeer.getServerName()
                              + " from the " + mClusterPeer.getClusterName()
                              + " cluster.");
                }
                catch (RemoteException re2) {
                    mLog.warn(re2);
                }
            }
        }
    }
}
