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
package org.teatrove.teaapps.apps;

import javax.servlet.ServletException;

import java.lang.management.*; 
import javax.management.*;

import org.teatrove.teaservlet.AdminApp;
import org.teatrove.teaservlet.AppAdminLinks;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.teaservlet.RegionCachingContext;
import org.teatrove.trove.log.Log;

import java.util.List;

/*
 * @author Scott Jappinen
 */
public class JMXConsoleApplication implements AdminApp {

	private Log mLog;
	
	private JMXContext mContext = null;
	
	public void init(ApplicationConfig config) throws ServletException {
		mLog = config.getLog();
		mContext = new JMXContext();
	}
	
    public void destroy() {}

    /**
     * Returns an instance of {@link RegionCachingContext}.
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new JMXContext();
    }

    /**
     * Returns {@link RegionCachingContext}.class.
     */
    public Class<JMXContext> getContextType() {
        return JMXContext.class;
    }
	
    /*
     * Retrieves the administrative links for this application.
     */
    public AppAdminLinks getAdminLinks() {
    	AppAdminLinks links = new AppAdminLinks(mLog.getName());
        links.addAdminLink("JMX Console","system.teaservlet.JMXConsole");
        return links;
    }
    
    public class JMXContext {
    	
    	public ClassLoadingMXBean getClassLoadingMXBean() {
    		return ManagementFactory.getClassLoadingMXBean();
    	}
    	
    	public void setClassLoadingMXBeanVerbose(boolean value) {
    		ManagementFactory.getClassLoadingMXBean().setVerbose(value);
    	}
    	
    	public CompilationMXBean getCompilationMXBean() {
    		return ManagementFactory.getCompilationMXBean();
    	}
    	
    	public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
    		return ManagementFactory.getGarbageCollectorMXBeans();
    	}
    	
    	public List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
    		return ManagementFactory.getMemoryManagerMXBeans();
    	}
    	
    	public MemoryMXBean getMemoryMXBean() {
    		return ManagementFactory.getMemoryMXBean();
    	}
    	
    	public void setMemoryMXBeanVerbose(boolean value) {
    		ManagementFactory.getMemoryMXBean().setVerbose(value);
    	}
    	
    	public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
    		return ManagementFactory.getMemoryPoolMXBeans();
    	}
    	
    	public void resetPeakUsage(MemoryPoolMXBean bean) {
    		bean.resetPeakUsage();
    	}
    	
    	public void setCollectionUsageThreshold(MemoryPoolMXBean bean, long threshold) {
    		bean.setCollectionUsageThreshold(threshold);
    	}
    	
    	public void setUsageThreshold(MemoryPoolMXBean bean, long threshold) {
    		bean.setUsageThreshold(threshold);
    	}
    	
    	public OperatingSystemMXBean getOperatingSystemMXBean() {
    		return ManagementFactory.getOperatingSystemMXBean();
    	}
    	
    	public MBeanServer getPlatformMBeanServer() {
    		return ManagementFactory.getPlatformMBeanServer();
    	}
    	
    	// TODO add methods for platform server
    	
    	public RuntimeMXBean getRuntimeMXBean() {
    		return ManagementFactory.getRuntimeMXBean();
    	}
    	
    	public ThreadMXBean getThreadMXBean() {
    		return ManagementFactory.getThreadMXBean();
    	}
    	
    	public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
    		return ManagementFactory.getThreadMXBean().dumpAllThreads(lockedMonitors, lockedSynchronizers);
    	}
    	
    	public long[] findDeadlockedThreads() {
    		return ManagementFactory.getThreadMXBean().findDeadlockedThreads();
    	}
    	
    	public long[] findMonitorDeadlockedThreads() {
    		return ManagementFactory.getThreadMXBean().findMonitorDeadlockedThreads();
    	}
    	
    	public void setThreadContentionMonitoringEnabled(boolean enable) {
    		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(enable);
    	}
    	
    	public void setThreadCpuTimeEnabled(boolean enable) {
    		ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(enable);
    	}
    }
	
}
