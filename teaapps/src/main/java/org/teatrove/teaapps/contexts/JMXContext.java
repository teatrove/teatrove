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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;
import org.teatrove.trove.util.PropertyMap;

/**
 * Custom Tea context that provides access to the JMX data in the running
 * application.
 */
public class JMXContext implements Context {
	
    private String[] edenSpace = { "Eden Space", "PS Eden Space", "Nursery" };
    private String[] survivorSpace = { "Survivor Space", "PS Survivor Space" };
    private String[] permGen = { "Perm Gen", "PS Perm Gen", "Class Memory" };
    private String[] tenuredGen = { "Tenured Gen", "PS Old Gen", "Old Space" };
    
    /**
     * Default constructor.
     */
    public JMXContext() {
        super();
    }
    
    /**
     * Initialize this context instance.  The following configuration parameters
     * are supported:
     * 
     * <dl>
     *   <dt>edenSpace</dt>
     *   <dd>The name of the eden space memory pool (default: Eden Space)</dd>
     *   
     *   <dt>survivorSpace</dt>
     *   <dd>The name of the survivor space memory pool (default: Survivor Space)</dd>
     *   
     *   <dt>permGen</dt>
     *   <dd>The name of the perm gen memory pool (default: Perm Gen)</dd>
     *   
     *   <dt>tenuredGen</dt>
     *   <dd>The name of the tenured/old gen memory pool (default: Tenured Gen)/dd>
     * </dl>
     */
    public void init(ContextConfig config) {
        PropertyMap properties = config.getProperties();
        String edenSpace = properties.getString("EdenSpace");
        if (edenSpace != null) {
            this.edenSpace = new String[] { edenSpace };
        }
        
        String survivorSpace = properties.getString("SurvivorSpace");
        if (survivorSpace != null) {
            this.survivorSpace = new String[] { survivorSpace };
        }
        
        String tenuredGen = properties.getString("TenuredGen");
        if (tenuredGen != null) {
            this.tenuredGen = new String[] { tenuredGen };
        }
        
        String permGen = properties.getString("PermGen");
        if (permGen != null) {
            this.permGen = new String[] { permGen };
        }
        
        // enable contention
        getThreadMXBean().setThreadCpuTimeEnabled(true);
        getThreadMXBean().setThreadContentionMonitoringEnabled(true);
    }
    
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
     * @see #getGarbageCollectorMXBean(String)
     * @see #getYoungCollectorMXBean()
     * @see #getTenuredCollectorMXBean()
     */
    public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementFactory.getGarbageCollectorMXBeans();
    }
    
    /**
     * Get the {@link GarbageCollectorMXBean} with the given name. If no garbage
     * collector exists for the given name, then <code>null</code> is returned.
     * 
     * @param name The name of the garbage collector to retrieve
     * 
     * @return The associated garbage collector or <code>null</code>
     * 
     * @see #getGarbageCollectorMXBeans()
     */
    public GarbageCollectorMXBean getGarbageCollectorMXBean(String name) {
        for (GarbageCollectorMXBean bean : getGarbageCollectorMXBeans()) {
            if (bean.getName().equals(name)) {
                return bean;
            }
        }
        
        return null;
    }

    /**
     * Get the {@link GarbageCollectorMXBean} that is associated with purely
     * the young generation including the eden and survivor spaces.  This
     * returns the first collector that excludes the tenured generation based
     * on the <code>tenuredGen</code> configuration or <code>Tenured Gen</code>
     * by default.  If no collector is found, then <code>null</code> is 
     * returned.
     * 
     * @return The young generation collector or <code>null</code>
     * 
     * @see #getTenuredMemoryPoolMXBean()
     * @see #getGarbageCollectorMXBean(String)
     * @see #getGarbageCollectorMXBeans()
     */
    public GarbageCollectorMXBean getYoungCollectorMXBean() {
        outer: for (GarbageCollectorMXBean bean : getGarbageCollectorMXBeans()) {
            for (String pool : bean.getMemoryPoolNames()) {
                for (String name : this.tenuredGen) {
                    if (pool.equals(name)) {
                        continue outer;
                    }
                }
            }
            
            return bean;
        }
    
        return getGarbageCollectorMXBeans().get(0);
    }

    /**
     * Get the {@link GarbageCollectorMXBean} that is associated with the old or
     * tenured generations collection.  The same collector may also be
     * associated with young generations. This returns the first collector that 
     * includes the tenured generation based on the <code>tenuredGen</code> 
     * configuration or <code>Tenured Gen</code> by default.  If no collector 
     * is found, then <code>null</code> is returned.
     * 
     * @return The tenured generation collector or <code>null</code>
     * 
     * @see #getTenuredMemoryPoolMXBean()
     * @see #getGarbageCollectorMXBean(String)
     * @see #getGarbageCollectorMXBeans()
     */
    public GarbageCollectorMXBean getTenuredCollectorMXBean() {
        for (GarbageCollectorMXBean bean : getGarbageCollectorMXBeans()) {
            for (String pool : bean.getMemoryPoolNames()) {
                for (String name : this.tenuredGen) {
                    if (pool.equals(name)) {
                        return bean;
                    }
                }
            }
        }
    
        return getGarbageCollectorMXBeans().get(0);
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
     * @see #getMemoryPoolMXBean(String)
     * @see #getEdenMemoryPoolMXBean()
     * @see #getSurvivorMemoryPoolMXBean()
     * @see #getPermGenMemoryPoolMXBean()
     * @see #getTenuredMemoryPoolMXBean()
     */
    public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return ManagementFactory.getMemoryPoolMXBeans();
    }
    
    /**
     * Get the {@link MemoryPoolMXBean} with the given name. If no memory pool
     * exists for the given name, then <code>null</code> is returned.
     * 
     * @param name The name of the memory pool to retrieve
     * 
     * @return The associated memory pool or <code>null</code>
     * 
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getMemoryPoolMXBean(String name) {
        for (MemoryPoolMXBean bean : getMemoryPoolMXBeans()) {
            if (bean.getName().equals(name)) {
                return bean;
            }
        }
        
        return null;
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the eden space in the
     * young generation.  This returns the memory pool that either has the
     * configured <code>edenSpace</code> name or <code>Eden Space</code> by
     * default as is the case with JDK 6+ runtimes.  If the given pool cannot
     * be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the eden space or <code>null</code>
     * 
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getEdenMemoryPoolMXBean() {
        for (String name : this.edenSpace) {
            MemoryPoolMXBean bean = getMemoryPoolMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the survivor space in 
     * the young generation.  This returns the memory pool that either has the
     * configured <code>survivorSpace</code> name or <code>Surivor Space</code> 
     * by default as is the case with JDK 6+ runtimes.  If the given pool cannot
     * be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the survivor space or <code>null</code>
     * 
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getSurvivorMemoryPoolMXBean() {
        for (String name : this.survivorSpace) {
            MemoryPoolMXBean bean = getMemoryPoolMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the perm generation.  
     * This returns the memory pool that either has the configured 
     * <code>permGen</code> name or <code>Perm Gen</code> by default as is the 
     * case with JDK 6+ runtimes.  If the given pool cannot
     * be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the perm gen or <code>null</code>
     * 
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getPermGenMemoryPoolMXBean() {
        for (String name : this.permGen) {
            MemoryPoolMXBean bean = getMemoryPoolMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the tenured gen in the
     * old generation.  This returns the memory pool that either has the
     * configured <code>tenuredGen</code> name or <code>Tenured Gen</code> by
     * default as is the case with JDK 6+ runtimes.  If the given pool cannot
     * be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the tenured gen or <code>null</code>
     * 
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getTenuredMemoryPoolMXBean() {
        for (String name : this.tenuredGen) {
            MemoryPoolMXBean bean = getMemoryPoolMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
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
    
    public Collection<MBeanEntry> getMBeans() {
        Set<ObjectName> mbeans =
            ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
        
        MBeanEntry root = new MBeanEntry(null);
        for (ObjectName mbean : mbeans) {
            String domain = mbean.getDomain();
            MBeanEntry current = root.getOrCreateEntry(domain);
            
            String[] properties = mbean.getKeyPropertyListString().split(",");
            for (int i = 0; i < properties.length - 1; i++) {
                String[] parts = properties[i].split("=");
                String property = (parts.length == 2 ? parts[1] : properties[i]);
                current = current.getOrCreateEntry(property);
            }
            
            String name = properties[properties.length - 1];
            String[] parts = name.split("=");
            String property = (parts.length == 2 ? parts[1] : name);
            
            current.addEntry(name, new MBeanEntry(property, mbean));
        }
        
        return root.getEntries();
    }
    
    public MBeanInfo getMBeanInfo(String name) 
        throws Exception {
        
        return getMBeanInfo(ObjectName.getInstance(name));
    }
    
    public MBeanInfo getMBeanInfo(ObjectName name) 
        throws Exception {
        
        return ManagementFactory.getPlatformMBeanServer().getMBeanInfo(name);
    }
    
    public Object getMBeanAttribute(String name, String attribute)
        throws Exception {
        
        return getMBeanAttribute(ObjectName.getInstance(name), attribute);
    }
    
    public Object getMBeanAttribute(ObjectName name, String attribute)
        throws Exception {
        
        return ManagementFactory.getPlatformMBeanServer()
            .getAttribute(name, attribute);
    }
    
    public static class MBeanEntry {
        private String name;
        private ObjectName objectName;
        private SortedMap<String, MBeanEntry> entries =
            new TreeMap<String, MBeanEntry>();
        
        public MBeanEntry(String name) {
            this.name = name;
        }
        
        public MBeanEntry(String name, ObjectName objectName) {
            this.name = name;
            this.objectName = objectName;
        }
        
        public boolean isMBean() {
            return this.objectName != null;
        }
        
        public String getName() {
            return this.name;
        }
        
        public ObjectName getObjectName() {
            return this.objectName;
        }
        
        public Collection<MBeanEntry> getEntries() {
            return this.entries.values();
        }
        
        public MBeanEntry getOrCreateEntry(String name) {
            MBeanEntry entry = this.entries.get(name);
            if (entry == null) {
                entry = new MBeanEntry(name);
                this.entries.put(name, entry);
            }
            
            return entry;
        }
        
        public void addEntry(String name, MBeanEntry entry) {
            this.entries.put(name, entry);
        }
    }
}
