package org.teatrove.teaapps.contexts;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;

import javax.management.MBeanServer;

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