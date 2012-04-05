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

/**
 * Custom Tea context that provides access to the JMX data in the running
 * application.
 */
public class JMXContext {
	
    /**
     * Get the {@link ClassLoadingMXBean} MBean in the running application.
     * 
     * @return The associated class loading MBean
     * 
     * @see ManagementFactory#getClassLoadingMXBean()
     */
	public ClassLoadingMXBean getClassLoadingMXBean() {
		return ManagementFactory.getClassLoadingMXBean();
	}
	
	/**
	 * Enable or disable the verbose output of the class loading system.
	 * 
	 * @param value The state of whether to enable or disable verbose output
	 * 
	 * @see ClassLoadingMXBean#setVerbose(boolean)
	 */
	public void setClassLoadingMXBeanVerbose(boolean value) {
		ManagementFactory.getClassLoadingMXBean().setVerbose(value);
	}
	
	/**
     * Get the {@link CompilationMXBean} MBean in the running application.
     * 
     * @return The associated compilation MBean
     * 
     * @see ManagementFactory#getCompilationMXBean()
     */
	public CompilationMXBean getCompilationMXBean() {
		return ManagementFactory.getCompilationMXBean();
	}
	
	/**
     * Get the list of available {@link GarbageCollectorMXBean} MBeans in the 
     * running application.
     * 
     * @return The associated garbage collection MBeans
     * 
     * @see ManagementFactory#getGarbageCollectorMXBeans()
     */
	public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
		return ManagementFactory.getGarbageCollectorMXBeans();
	}
	
	/**
     * Get the list of available {@link MemoryManagerMXBean} MBeans in the 
     * running application.
     * 
     * @return The associated memory manager MBeans
     * 
     * @see ManagementFactory#getMemoryManagerMXBeans()
     */
	public List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
		return ManagementFactory.getMemoryManagerMXBeans();
	}
	
	/**
     * Get the {@link MemoryMXBean} MBean in the running application.
     * 
     * @return The associated memory MBean
     * 
     * @see ManagementFactory#getMemoryMXBean()
     */
	public MemoryMXBean getMemoryMXBean() {
		return ManagementFactory.getMemoryMXBean();
	}
	
	/**
     * Enable or disable the verbose output of the memory system including
     * garbage collection.
     * 
     * @param value The state of whether to enable or disable verbose output
     * 
     * @see MemoryMXBean#setVerbose(boolean)
     */
	public void setMemoryMXBeanVerbose(boolean value) {
		ManagementFactory.getMemoryMXBean().setVerbose(value);
	}
	
	/**
     * Get the list of available {@link MemoryPoolMXBean} MBeans in the 
     * running application.
     * 
     * @return The associated memory pool MBeans
     * 
     * @see ManagementFactory#getMemoryPoolMXBeans()
     */
	public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
		return ManagementFactory.getMemoryPoolMXBeans();
	}
	
	/**
	 * Reset the peak usage of the given memory pool to the current usage.
	 * 
	 * @param bean The associated memory pool bean to reset
	 * 
	 * @see #getMemoryPoolMXBeans()
	 * @see MemoryPoolMXBean#resetPeakUsage()
	 */
	public void resetPeakUsage(MemoryPoolMXBean bean) {
		bean.resetPeakUsage();
	}
	
	/**
	 * Set the collection usage threshold of the given memory pool.
	 * 
	 * @param bean The associated memory pool to configure
	 * @param threshold The threshold to configure, in bytes
	 * 
	 * @see #getMemoryPoolMXBeans()
	 * @see MemoryPoolMXBean#setCollectionUsageThreshold(long)
	 */
	public void setCollectionUsageThreshold(MemoryPoolMXBean bean, long threshold) {
		bean.setCollectionUsageThreshold(threshold);
	}
	
	/**
     * Set the usage threshold of the given memory pool.
     * 
     * @param bean The associated memory pool to configure
     * @param threshold The threshold to configure, in bytes
     * 
     * @see #getMemoryPoolMXBeans()
     * @see MemoryPoolMXBean#setUsageThreshold(long)
     */
	public void setUsageThreshold(MemoryPoolMXBean bean, long threshold) {
		bean.setUsageThreshold(threshold);
	}
	
	/**
     * Get the {@link OperatingSystemMXBean} MBean in the running application.
     * 
     * @return The associated operating system MBean
     * 
     * @see ManagementFactory#getOperatingSystemMXBean()
     */
	public OperatingSystemMXBean getOperatingSystemMXBean() {
		return ManagementFactory.getOperatingSystemMXBean();
	}
	
	/**
	 * Get the platform {@link MBeanServer} instance.
	 * 
	 * @return The platform MBean server instance
	 * 
	 * @see ManagementFactory#getPlatformMBeanServer()
	 */
	public MBeanServer getPlatformMBeanServer() {
		return ManagementFactory.getPlatformMBeanServer();
	}
	
	// TODO add methods for platform server
	
	/**
     * Get the {@link RuntimeMXBean} MBean in the running application.
     * 
     * @return The associated runtime MBean
     * 
     * @see ManagementFactory#getRuntimeMXBean()
     */
	public RuntimeMXBean getRuntimeMXBean() {
		return ManagementFactory.getRuntimeMXBean();
	}
	
	/**
     * Get the {@link ThreadMXBean} MBean in the running application.
     * 
     * @return The associated thread system MBean
     * 
     * @see ManagementFactory#getThreadMXBean()
     */
	public ThreadMXBean getThreadMXBean() {
		return ManagementFactory.getThreadMXBean();
	}
	
	/**
	 * Dump all the thread and their active states. The lockedMonitors and
	 * lockedSynchronizers flags will filter the results accordingly.
	 * 
	 * @param lockedMonitors Include locked monitors in the results
	 * @param lockedSynchronizers Included locked synchronizers in the results
	 * 
	 * @return The list of active threads and their states
	 * 
	 * @see ThreadMXBean#dumpAllThreads(boolean, boolean)
	 */
	public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, 
	                                   boolean lockedSynchronizers) {
		return ManagementFactory.getThreadMXBean()
		    .dumpAllThreads(lockedMonitors, lockedSynchronizers);
	}
	
	/**
	 * Determine if any threads appear to be deadlocked and return the list of
	 * thread ids.
	 * 
	 * @return The array of deadlocked thread ids or <code>null</code> if none
	 * 
	 * @see ThreadMXBean#findDeadlockedThreads()
	 */
	public long[] findDeadlockedThreads() {
		return ManagementFactory.getThreadMXBean().findDeadlockedThreads();
	}
	
	/**
     * Determine if any monitor threads appear to be deadlocked and return the 
     * list of thread ids.
     * 
     * @return The array of deadlocked thread ids or <code>null</code> if none
     * 
     * @see ThreadMXBean#findMonitorDeadlockedThreads()
     */
	public long[] findMonitorDeadlockedThreads() {
		return ManagementFactory.getThreadMXBean().findMonitorDeadlockedThreads();
	}
	
	/**
	 * Enable to disable thread contention monitoring for threads.
	 * 
	 * @param enable The flag to enable or disable thread contention monitoring
	 * 
	 * @see ThreadMXBean#setThreadContentionMonitoringEnabled(boolean)
	 */
	public void setThreadContentionMonitoringEnabled(boolean enable) {
		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(enable);
	}
	
	/**
     * Enable to disable thread CPU time monitoring and calculation for threads.
     * 
     * @param enable The flag to enable or disable thread CPU monitoring
     * 
     * @see ThreadMXBean#setThreadCpuTimeEnabled(boolean)
     */
	public void setThreadCpuTimeEnabled(boolean enable) {
		ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(enable);
	}
}