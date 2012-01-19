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

import java.util.Map;
import java.util.Hashtable;

import javax.servlet.ServletContext;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.MergedClass;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.DynamicContextSource;
import org.teatrove.tea.engine.MergedContextSource;
import org.teatrove.tea.engine.ContextCreationException;
import org.teatrove.teaservlet.management.HttpContextManagement;

/**
 * 
 * @author Jonathan Colwell
 */
public class TeaServletContextSource extends MergedContextSource {

    private Map mPastContextTypes;
    private Application[] mApplications;
    private Map mContextTypeMap;
    private DynamicContextSource[] mDynSources;
    private Log mLog;
    private boolean mProfilingEnabled;

    TeaServletContextSource(ClassLoader loader, 
                            ApplicationDepot appDepot,
                            ServletContext servletContext,
                            Log log,
                            boolean prependWithHttpContext,
                            boolean httpContextMBeanEnabled,
                            int httpContextMBeanCacheSize,
                            boolean profilingEnabled) throws Exception {
        
        int len = prependWithHttpContext ? 1 : 0;

        mLog = log;
        mProfilingEnabled = profilingEnabled;
        Application[] applications = appDepot.getApplications();
        String[] prefixes = appDepot.getContextPrefixNames();       
        DynamicContextSource[] contextSources =  
            new DynamicContextSource[applications.length + len];

        mApplications = applications;       
        mDynSources = contextSources;

        String[] mungedPrefixes = new String[contextSources.length];

        if (prependWithHttpContext) {
            // Create the Http context MBean if it is configured to do so.
            HttpContextManagement httpContextMBean = null;
            if (httpContextMBeanEnabled) {
                httpContextMBean = HttpContextManagement.create(httpContextMBeanCacheSize);                
            }
            
            contextSources[0] = new HttpContextSource(servletContext, log, httpContextMBean);
            mungedPrefixes[0] = "HttpContext$";
        }

        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];

            if (prefix != null && !prefix.endsWith("$")) {
                prefix += "$";
            }

            mungedPrefixes[i + len] = prefix;
        }

        for (int j = 0; j < applications.length; j++) {
            contextSources[j+len] =
                new ApplicationContextSource(applications[j]);
        }

        init(loader, contextSources, mungedPrefixes, profilingEnabled);
    }


    public final Map getApplicationContextTypes() {
        if (mContextTypeMap == null) {
            int tableSize = mApplications.length;
            Class[] contextTypes = getContextsInOrder();
            mContextTypeMap = new Hashtable(tableSize);
            int prependAdjustment = 
                ((tableSize == contextTypes.length) ? 0 : 1);

            for (int j = 0; j < tableSize; j++) {
                mContextTypeMap.put(mApplications[j], 
                                    contextTypes[j + prependAdjustment]);
            }
        }
        return mContextTypeMap;
    }

    /**
     * a generic method to create context instances 
     */
    public Object createContext(Object param) throws Exception {
        try {
            if(mProfilingEnabled) {
                return getConstructor().newInstance(new Object[] {
                    new TSContextFactory(param), TeaServletInvocationStats.getInstance()
                });
            }
            else {
                return getConstructor().newInstance(new Object[] {
                    new TSContextFactory(param), 
                    new MergedClass.InvocationEventObserver() {
                        public void invokedEvent(String caller, String callee, long elapsedTime) { }
                        public long currentTime() { return 0L; }

                    }});
            }
        }
        catch (Exception ex) {
            mLog.error(ex);
            throw ex;
        }
    }

    private class TSContextFactory implements MergedClass.InstanceFactory {
        private final Object mContextParameter;

        TSContextFactory(Object contextParam) {
            mContextParameter = contextParam;            
        }

        public Object getInstance(int i) {
            try {
                return mDynSources[i].createContext(getContextsInOrder()[i], 
                                                    mContextParameter);
            }
            catch (Exception e) {
                throw new ContextCreationException(e);
            }
        }
    }
}

