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

import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.TemplateCallExtractor;
import org.teatrove.tea.engine.ReloadLock;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateCompilationStatus;
import org.teatrove.tea.engine.TemplateExecutionResult;
import org.teatrove.tea.engine.TemplateIssue;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.teaservlet.stats.TeaServletRequestStats;
import org.teatrove.teaservlet.stats.TemplateStats;
import org.teatrove.teaservlet.util.NameValuePair;
import org.teatrove.teaservlet.util.RemoteCompilationProvider;
import org.teatrove.teaservlet.util.cluster.Restartable;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.net.HttpClient;
import org.teatrove.trove.net.PlainSocketFactory;
import org.teatrove.trove.net.PooledSocketFactory;
import org.teatrove.trove.net.SocketFactory;
import org.teatrove.trove.util.BeanComparator;

/**
 * The Admin object which contains all administrative information. This object
 * is meant to be used by the Admin page of the TeaServlet.
 *
 * @author Reece Wilton, Brian S O'Neill, Jonathan Colwell
 */
public class TeaServletAdmin implements Restartable {

    // command codes.
    public static final int RELOAD_CONTEXT = -99;
    public static final int RELOAD_TEMPLATE_CHANGES = 4019;
    public static final int RELOAD_ALL_TEMPLATES = -4019;
    public static final int RELOAD_SELECTED_TEMPLATE_CHANGES = 8038;

    private TeaServletEngine mTeaServletEngine;
    private String[] mClusteredServers;
    //private TemplateDepot.TemplateLoadResult mTemplateLoadResult;
    //private String[] mTemplateNamesFromLastSuccessfulReload;
    //private Date mTimeOfLastSuccessfulReload;
    private AppAdminLinks[] mAdminLinks;
    private Comparator<TemplateWrapper> mTemplateOrdering;
    private ReloadLock mLock;
    private TemplateCompilationStatus mStatusListener;

    /**
     * Initializes the Admin object for the specific TeaServletEngine instance.
     * @param teaServlet the TeaServletEngine to administer
     */
    public TeaServletAdmin(TeaServletEngine engine) {
        mTeaServletEngine = engine;
        mLock = new ReloadLock();
    }
    
    public Object restart(Object paramObj)
        throws RemoteException {

        synchronized(mLock) {
            if (mLock.isReloading()) {
                return new TemplateCompilationResults();
            }
            else {
                mLock.setReloading(true);
            }
        }
        try {
            Integer commandCode;
            String[] selectedTemplates = null;
            if(paramObj instanceof Object[]) {
                commandCode = (Integer)((Object[])paramObj)[0];
                if(commandCode.intValue() == RELOAD_SELECTED_TEMPLATE_CHANGES) {
                    selectedTemplates = (String[])((Object[])paramObj)[1];
                    if(selectedTemplates==null) {
                        return new TemplateCompilationResults(new HashMap<String, CompilationUnit>(), new HashMap<String, List<TemplateIssue>>());
                    } 
                    for (int i = 0; i < selectedTemplates.length; i++) {
                        selectedTemplates[i] = selectedTemplates[i].replace('/', '.');
                    }
                }
            } else {
                commandCode = (Integer)paramObj;
            }
            
            
            if (commandCode == null) {
                return mTeaServletEngine.getTemplateSource()
                    .compileTemplates(null, false, mStatusListener);
            }
            else {
                Object result = null;
                switch (commandCode.intValue()) {

                case RELOAD_CONTEXT:
					getLog().debug("DEBUG: restart RELOAD_CONTEXT");
                    return mTeaServletEngine.reloadContextAndTemplates(false);

                case RELOAD_ALL_TEMPLATES:
                    mStatusListener = new TemplateCompilationStatus();
                    getLog().debug("DEBUG: restart RELOAD_ALL_TEMPLATES");
                    result = mTeaServletEngine.getTemplateSource()
                        .compileTemplates(null, true, mStatusListener);
                    mStatusListener = null;
                    return result;

                case RELOAD_SELECTED_TEMPLATE_CHANGES:
                    mStatusListener = new TemplateCompilationStatus();
                    getLog().debug("DEBUG: restart RELOAD_SELECTED_TEMPLATE_CHANGES");
                    result = mTeaServletEngine.getTemplateSource()
                        .compileTemplates(null, mStatusListener, selectedTemplates);
                    mStatusListener = null;
                    return result;

                case RELOAD_TEMPLATE_CHANGES:
                default:
                    mStatusListener = new TemplateCompilationStatus();
                    getLog().debug("DEBUG: restart RELOAD_TEMPLATE_CHANGES");
                    result = mTeaServletEngine.getTemplateSource()
                        .compileTemplates(null, false, mStatusListener);
                    mStatusListener = null;
                    return result;
                }
            }
        }
        catch (Exception e) {
            throw new RemoteException("Restart Error", e);
        }
        finally {
            synchronized(mLock) {
                mLock.setReloading(false);
            }
        }
    }

    public ServletContext getServletContext() {
        return mTeaServletEngine.getServletContext();
    }

    @SuppressWarnings("unchecked")
    public NameValuePair<String>[] getInitParameters() {
        Enumeration<String> e = mTeaServletEngine.getInitParameterNames();
        List<NameValuePair<String>> list = new ArrayList<NameValuePair<String>>();
        while (e.hasMoreElements()) {
            String initName = e.nextElement();
            list.add(new NameValuePair<String>
                     (initName, mTeaServletEngine
                      .getInitParameter(initName)));
        }
        return list.toArray(new NameValuePair[list.size()]);
    }

    @SuppressWarnings("unchecked")
    public NameValuePair<Object>[] getAttributes() {
        ServletContext context = getServletContext();
        Enumeration<String> e = context.getAttributeNames();
        List<NameValuePair<Object>> list = new ArrayList<NameValuePair<Object>>();
        while (e.hasMoreElements()) {
            String initName = e.nextElement();
            list.add(new NameValuePair<Object>
                     (initName, context.getAttribute(initName)));
        }
        return list.toArray(new NameValuePair[list.size()]);
    }

    public Log getLog() {
        return mTeaServletEngine.getLog();
    }

    public LogEvent[] getLogEvents() {
        return mTeaServletEngine.getLogEvents();
    }

    public ApplicationInfo[] getApplications() {

        ApplicationDepot depot = mTeaServletEngine.getApplicationDepot();
        TeaServletContextSource tscs = (TeaServletContextSource)
            mTeaServletEngine.getTemplateSource().getContextSource();
        Map<Application, Class<?>> appContextMap = 
            tscs.getApplicationContextTypes();
        Application[] apps = depot.getApplications();
        String[] names = depot.getApplicationNames();
        String[] prefixes = depot.getContextPrefixNames();

        ApplicationInfo[] infos = new ApplicationInfo[apps.length];

        for (int i=0; i < apps.length; i++) {
            infos[i] = new ApplicationInfo(names[i], apps[i],
                                           appContextMap.get(apps[i]),
                                           prefixes[i]);
        }

        return infos;
    }

    /**
     *  Returns information about all functions available to the templates.
     */
    public FunctionInfo[] getFunctions() {
        // TODO: make this a little more useful by showing more function
        // details.

        ApplicationInfo[] AppInf = getApplications();

        FunctionInfo[] funcArray = null;

        try {
            MethodDescriptor[] methods = Introspector
                .getBeanInfo(HttpContext.class)
                .getMethodDescriptors();
            List<FunctionInfo> funcList = new Vector<FunctionInfo>(50);

            for (int i = 0; i < methods.length; i++) {
                MethodDescriptor m = methods[i];
                if (m.getMethod().getDeclaringClass() != Object.class &&
                    !m.getMethod().getName().equals("print") &&
                    !m.getMethod().getName().equals("toString")) {
                    
                    funcList.add(new FunctionInfo(m, null));
                }
            }
            
            for (int i = 0; i < AppInf.length; i++) {
                FunctionInfo[] ctxFunctions = AppInf[i].getContextFunctions();
                for (int j = 0; j < ctxFunctions.length; j++) {
                    funcList.add(ctxFunctions[j]);
                }
            }

            funcArray = funcList.toArray
                (new FunctionInfo[funcList.size()]);
            Arrays.sort(funcArray);
        }
        catch (Exception ie) {
            ie.printStackTrace();
        }

        return funcArray;
    }

    /*
     * Returns a FunctionInfo for a method descriptor.
     */
    public FunctionInfo getFunction(String methodDesc) {

        methodDesc = methodDesc.replace('.', '$');
        int paramsStartPos = methodDesc.indexOf('(');
        int firstSep = methodDesc.indexOf('$');
        String prefix = firstSep != -1 && firstSep < paramsStartPos ? methodDesc.substring(0, firstSep) : "";
        methodDesc = prefix.length() > 0 ? methodDesc.substring(prefix.length() + 1) : methodDesc;
        FunctionInfo[] f = getFunctions();

        for (int i = 0; i < f.length; i++) {
            String p = f[i].getProvider() != null ? f[i].getProvider().getContextPrefixName().trim() : "";
            p = p.endsWith("$") ? p.substring(0, p.length() - 1) : p;
            if (("".equals(prefix) || prefix.equals(p)) &&
                    new TemplateCallExtractor.AppMethodInfo(f[i].getDescriptor().getMethod()).equals(
                    new TemplateCallExtractor.AppMethodInfo(methodDesc)))
            return f[i];
        }

        return null;

    }

    @SuppressWarnings("unchecked")
    public void setTemplateOrdering(String orderBy) {
       boolean reverse = false;
       if (orderBy != null && orderBy.endsWith("-")) {
           reverse = true;
           orderBy = orderBy.substring(0, orderBy.length() - 1);
       }
       mTemplateOrdering = BeanComparator
           .forClass(TemplateWrapper.class).orderBy(orderBy);
       
       if (reverse)
           mTemplateOrdering = ((BeanComparator) mTemplateOrdering).reverse();
    }

    @SuppressWarnings("unchecked")
    public TemplateLoader.Template[] getTemplates() {
        TemplateLoader.Template[] templates =
            mTeaServletEngine.getTemplateSource()
            .getLoadedTemplates();
        Comparator<TemplateLoader.Template> c = 
            BeanComparator.forClass(TemplateLoader.Template.class)
                .orderBy("name");
        Arrays.sort(templates, c);
        return templates;
    }

    /**
     * Provides an ordered array of available templates using a
     * handy wrapper class.
     */
    @SuppressWarnings("unchecked")
    public TemplateWrapper[] getKnownTemplates() {
        if (mTemplateOrdering == null) {
            setTemplateOrdering("name");
        }

        Comparator<TemplateWrapper> comparator = 
            BeanComparator.forClass(TemplateWrapper.class).orderBy("name");
        Set<TemplateWrapper> known = new TreeSet<TemplateWrapper>(comparator);

        TemplateLoader.Template[] loaded = mTeaServletEngine
            .getTemplateSource().getLoadedTemplates();

        if (loaded != null) {
            for (int j = 0; j < loaded.length; j++) {
                TeaServletAdmin.TemplateWrapper wrapper =
                    new TemplateWrapper(loaded[j], TeaServletInvocationStats.getInstance()
                        .getStatistics(loaded[j].getName(), null), TeaServletInvocationStats.getInstance()
                        .getStatistics(loaded[j].getName(), "__substitution"),
                        TeaServletRequestStats.getInstance().getStats(loaded[j].getName()));

                try {
                    known.add(wrapper);
                } catch (ClassCastException cce) {
                    mTeaServletEngine.getLog().warn(cce);
                }
            }
        }

        String[] allNames = mTeaServletEngine.getTemplateSource()
            .getKnownTemplateNames();
        if (allNames != null) {
            for (int j = 0; j < allNames.length; j++) {
                TeaServletAdmin.TemplateWrapper wrapper =
                    new TemplateWrapper(allNames[j], TeaServletInvocationStats.getInstance()
                        .getStatistics(allNames[j], null), TeaServletInvocationStats.getInstance()
                        .getStatistics(allNames[j], "__substitution"),
                        TeaServletRequestStats.getInstance().getStats(allNames[j]));
                try {
                    known.add(wrapper);
                } catch (ClassCastException cce) {
                    mTeaServletEngine.getLog().warn(cce);
                }
            }
        }

        List<TemplateWrapper> v = new ArrayList<TemplateWrapper>(known);
        
        Collections.sort(v, mTemplateOrdering);
        
//        return (TeaServletAdmin.TemplateWrapper[])known.toArray(new TemplateWrapper[known.size()]);
        return v.toArray(new TemplateWrapper[v.size()]);
    }

    public Date getTimeOfLastReload() {
        return mTeaServletEngine.getTemplateSource()
            .getTimeOfLastReload();
    }
    
    public TemplateCompilationStatus getCompilationStatus() {
        return mStatusListener;
    }

    public Class<?> getTeaServletClass() {
        Object obj = mTeaServletEngine.getServletContext()
            .getAttribute(TeaServlet.class.getName());
        if (obj != null) {
            return obj.getClass();
        }
        return mTeaServletEngine.getClass();
    }

    public String getTeaServletVersion() {
        return org.teatrove.teaservlet.PackageInfo.getImplementationVersion();
    }

    public String getTeaVersion() {
        return org.teatrove.tea.engine.PackageInfo.getImplementationVersion();
    }

    public String[] getClusteredServers() {
        return mClusteredServers;
    }

    protected void setClusteredServers(String[] serverNames) {
        mClusteredServers = serverNames;
    }

    public AppAdminLinks[] getAdminLinks() {
        return mAdminLinks;
    }

    protected void setAdminLinks(AppAdminLinks[] links) {

        mAdminLinks = links;
    }
    
    public String[] getTemplatePaths() {

		String sourcePathString = mTeaServletEngine
 		                                    .getInitParameter("template.path");

		List<String> paths = new ArrayList<String>();

		if (sourcePathString != null) {
			StringTokenizer sourcePathTokenizer =
				                   new StringTokenizer(sourcePathString, ",;");
			while (sourcePathTokenizer.hasMoreTokens()) {
				String nextPath = sourcePathTokenizer.nextToken();
				if (nextPath.startsWith("http://")) {
					paths.add(getRemoteTemplatePath(nextPath));
				}
				else {
					try {
						paths.add(new File(nextPath).getCanonicalPath());
					}
					catch (IOException e) {}
				}
			}
		}
		
    	return paths.toArray(new String[paths.size()]);
    }

	private String getRemoteTemplatePath(String remotePath) {    
	
		String returnValue = null;
		try {
			HttpClient tsClient = getTemplateServerClient(remotePath);
		
			String uri = remotePath.substring(remotePath.indexOf(
		                   "/",RemoteCompilationProvider.TEMPLATE_LOAD_PROTOCOL.length()));
			uri += "?getSourcePath";		                   
	
			HttpClient.Response response = tsClient.setURI(uri)
			                                 .setPersistent(true).getResponse(); 
			if (response != null && response.getStatusCode() == 200) {
				BufferedReader rin = new BufferedReader
				           (new InputStreamReader(response.getInputStream()));
				returnValue = rin.readLine();
				rin.close();								           
			}
		}
		catch (IOException e) {
			getLog().warn("Error connecting to TemplateServer for " + remotePath);
			getLog().warn(e);
		}
		
		return returnValue;
	}
	
	/**
	 * returns a socket connected to a host running the TemplateServerServlet
	 */
	private HttpClient getTemplateServerClient(String remoteSource) throws IOException {

		// TODO: this was copied from the RemoteCompiler class.  This needs
		//       to be moved into a location where both can access it!
		int port = 80;
		String host = remoteSource.substring(RemoteCompilationProvider.TEMPLATE_LOAD_PROTOCOL.length());

		int portIndex = host.indexOf("/");
    
		if (portIndex >= 0) {
			host = host.substring(0,portIndex);                         
		}
		portIndex = host.indexOf(":");
		if (portIndex >= 0) {
			try {
				port = Integer.parseInt(host.substring(portIndex+1));
			}
			catch (NumberFormatException nfe) {
				System.out.println("Invalid port number specified");
			}
			host = host.substring(0,portIndex);
		}
		// TODO: remove the hardcoded 15 second timeout!
		SocketFactory factory = new PooledSocketFactory
			(new PlainSocketFactory(InetAddress.getByName(host), port, 15000));
        
		return new HttpClient(factory);
	}
	
    public ReloadableTemplate[] getReloadableTemplates() throws Exception {

        Map<String, Boolean> tMap = 
            mTeaServletEngine.getTemplateSource().listTouchedTemplates();
        ReloadableTemplate[] arr = new ReloadableTemplate[tMap.size()];

        int j = 0;
        Iterator<Map.Entry<String, Boolean>> it = tMap.entrySet().iterator(); 
        while (it.hasNext()) {
            Map.Entry<String, Boolean> entry = it.next();

            ReloadableTemplate rt = new ReloadableTemplate(entry.getKey());
            rt.setSignatureChanged(entry.getValue().booleanValue());
            arr[j++] = rt;
        }

        return arr;
    }

    public TemplateCompilationResults checkTemplates(boolean forceAll, 
                                                     String[] templateNames) 
        throws Exception {
        
        return mTeaServletEngine.getTemplateSource()
            .checkTemplates(null, forceAll, templateNames);
    }
    
    public TemplateExecutionResult compileSource(String source)
        throws Exception {
        
        TemplateExecutionResult result =
            mTeaServletEngine.getTemplateSource().compileSource(null, source);
        
        return result;
    }
    
    public class TemplateWrapper {

        private TemplateLoader.Template mTemplate;
        private TeaServletInvocationStats.Stats mStats;
        private TeaServletInvocationStats.Stats mBlockStats;
        private TemplateStats mTemplateStats;
        private String mName;

        public TemplateWrapper(TemplateLoader.Template template, 
        		               TeaServletInvocationStats.Stats stats, 
        		               TeaServletInvocationStats.Stats blockStats,
        		               TemplateStats templateStats) {
            mTemplate = template;
            mStats = stats;
            mBlockStats = blockStats;
            mTemplateStats = templateStats;
        }

        public TemplateWrapper(String name, 
        		               TeaServletInvocationStats.Stats stats, 
        		               TeaServletInvocationStats.Stats blockStats,
        		               TemplateStats templateStats) {
            mName = name;
            mStats = stats;
            mBlockStats = blockStats;
            mTemplateStats = templateStats;
        }

        public boolean equals(Object obj) {
            if (obj != null
                && obj instanceof TemplateWrapper) {
                return getName().equals(((TemplateWrapper)obj).getName());
            }
            return false;
        }
        
        public int hashCode() {
            return getName().hashCode();
        }

        public TemplateLoader.Template getLoadedTemplate() {
            return mTemplate;
        }

        public TeaServletInvocationStats.Stats getStatistics() {
            return mStats;
        }
        
        public TemplateStats getTemplateStats() {
        	return mTemplateStats;
        }

        public double getTemplateTime() {
            if (mStats == null)
                return 0.0;
            double blockDuration = mBlockStats != null ? mBlockStats.getAverageServiceDuration() : 0;
            return mStats.getAverageServiceDuration() - blockDuration;
        }

        public long getTemplateInvokes() {
            if (mStats == null)
                return 0L;
            return mStats.getServicedCount();
        }

        public double getWeight() {
            if (mStats == null)
                return 0.0;
            double blockDuration = mBlockStats != null ? mBlockStats.getAverageServiceDuration() : 0;
            return mStats.getServicedCount() * (mStats.getAverageServiceDuration() - blockDuration);
        }

        public String getName() {
            if (mTemplate != null) {
                return mTemplate.getName();
            }
            else {
                return mName;
            }
        }

        public boolean isLoaded() {
            return mTemplate != null;
        }
    }
    
    public class ReloadableTemplate {
        private String mName;
        private boolean mSignatureChanged;

        public ReloadableTemplate(String name) {
            setName(name);
        }
        
        public boolean isSignatureChanged() {
            return mSignatureChanged;
        }

        public void setSignatureChanged(boolean signatureChanged) {
            this.mSignatureChanged = signatureChanged;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            this.mName = name;
            mName = mName.replace('.', '/');
        }

    }

}
