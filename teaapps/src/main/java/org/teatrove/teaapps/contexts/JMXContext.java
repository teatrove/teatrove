package org.teatrove.teaapps.contexts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;

/**
 * Custom Tea context that provides access to the JMX data in the running
 * application.
 */
public class JMXContext implements Context {
	
    public static final String[] EDEN_SPACE_DEFAULTS = { 
        "Eden Space", "PS Eden Space", "Par Eden Space", "Nursery" 
    };
    
    public static final String[] SURVIVOR_SPACE_DEFAULTS = {
        "Survivor Space", "PS Survivor Space", "Par Survivor Space"
    };
    
    public static final String[] PERM_GEN_DEFAULTS = {
        "Perm Gen", "PS Perm Gen", "Par Perm Gen", "CMS Perm Gen", "Class Memory"
    };
    
    public static final String[] TENURED_GEN_DEFAULTS = {
        "Tenured Gen", "PS Old Gen", "Par Old Gen", "CMS Old Gen", "Old Space"
    };
    
    private ContextConfig config;
    
    private String[] edenSpace = EDEN_SPACE_DEFAULTS;
    private String[] survivorSpace = SURVIVOR_SPACE_DEFAULTS;
    private String[] permGen = PERM_GEN_DEFAULTS;
    private String[] tenuredGen = TENURED_GEN_DEFAULTS;
    
    private String[] youngCollector = new String[0];
    private String[] tenuredCollector = new String[0];
    
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
     *   <dt>EdenSpace</dt>
     *   <dd>The name of the eden space memory pool</dd>
     *   
     *   <dt>SurvivorSpace</dt>
     *   <dd>The name of the survivor space memory pool</dd>
     *   
     *   <dt>PermGen</dt>
     *   <dd>The name of the perm gen memory pool</dd>
     *   
     *   <dt>TenuredGen</dt>
     *   <dd>The name of the tenured/old gen memory pool</dd>
     *   
     *   <dt>YoungCollector</dt>
     *   <dd>The name of the young garbage collector</dd>
     *   
     *   <dt>TenuredCollector</dt>
     *   <dd>The name of the tenured garbage collector</dd>
     * </dl>
     * 
     * @see #EDEN_SPACE_DEFAULTS
     * @see #SURVIVOR_SPACE_DEFAULTS
     * @see #PERM_GEN_DEFAULTS
     * @see #TENURED_GEN_DEFAULTS
     */
    public void init(ContextConfig config) {
        this.config = config;
        
        PropertyMap properties = config.getProperties();
        String edenSpace = properties.getString("EdenSpace");
        if (edenSpace != null) {
            this.setEdenMemoryPool(edenSpace);
        }
        
        String survivorSpace = properties.getString("SurvivorSpace");
        if (survivorSpace != null) {
            this.setSurvivorMemoryPool(survivorSpace);
        }
        
        String tenuredGen = properties.getString("TenuredGen");
        if (tenuredGen != null) {
            this.setTenuredMemoryPool(tenuredGen);
        }
        
        String permGen = properties.getString("PermGen");
        if (permGen != null) {
            this.setPermGenMemoryPool(permGen);
        }
        
        String youngCollector = properties.getString("YoungCollector");
        if (youngCollector != null) {
            this.setYoungCollector(youngCollector);
        }
        
        String tenuredCollector = properties.getString("TenuredCollector");
        if (tenuredCollector != null) {
            this.setTenuredCollector(tenuredCollector);
        }
        
        // enable contention
        getThreadMXBean().setThreadCpuTimeEnabled(true);
        getThreadMXBean().setThreadContentionMonitoringEnabled(true);
        
        // load the settings if possible
        loadJMXSettings();
    }
    
    /**
     * Check whether or not the context is properly configured with valid
     * memory pools and collectors, including eden, survivor, and tenured
     * generations.
     * 
     * @return <code>true</code> if properly configured, otherwise false
     */
    public boolean validateJMX() {
        if (getEdenMemoryPoolMXBean() == null ||
            getSurvivorMemoryPoolMXBean() == null ||
            getTenuredMemoryPoolMXBean() == null ||
            getPermGenMemoryPoolMXBean() == null ||
            getYoungCollectorMXBean() == null ||
            getTenuredCollectorMXBean() == null) {
            
            return false;
        }
        
        return true;
    }
    
    protected File getSettingsFile() {
        Log log = config.getLog();
        
        // get the temp dir
        final String TEMP_DIR = "javax.servlet.context.tempdir";
        File tempDir = (File) config.getServletContext().getAttribute(TEMP_DIR);
        
        // validate the dir exists
        if (tempDir == null) {
            tempDir = new File(".");
            if (!tempDir.exists()) {
                log.warn("No temporary directory to save JMX settings");
                return null;
            }
        }
        
        // create and validate file
        File settingsFile = new File(tempDir, "jmx.properties");
        return settingsFile;
    }
    
    protected void saveSetting(Properties properties, 
                               String name, String[] array) {
        if (array != null && array.length == 1) {
            properties.put(name, array[0]);
        }
    }

    /**
     * Load the previously saved and configured JMX settings for memory pools 
     * and collectors.  The settings are stored in the temporary/working 
     * directory of the servlet. 
     * 
     * @return true if successfully saved, false otherwise
     * 
     * @see #saveJMXSettings()
     */
    public boolean loadJMXSettings() {
        Log log = config.getLog();
        
        // load settings file
        // if non-existant, nothing to load so return success
        File settingsFile = getSettingsFile();
        if (!settingsFile.exists()) { return true; }
        
        // validate file is readable
        if (!settingsFile.canRead()) {
            log.warn("Temporary directory not readable for JMX settings");
            return false;
        }
        
        // create the properties
        Properties properties = new Properties();
        
        // load the properties
        FileInputStream in = null;
        try { 
            in = new FileInputStream(settingsFile);
            properties.load(in);
        }
        catch (IOException ioe) {
            log.error("Unable to load JMX settings: " + ioe.getMessage());
            log.error(ioe);
            return false;
        }
        finally {
            try { if (in != null) { in.close(); } }
            catch (IOException e) { /* ignore */ }
        }
        
        // load the settings
        String value;
        
        value = properties.getProperty("EdenSpace");
        if (value != null) { edenSpace = new String[] { value }; }
        
        value = properties.getProperty("SurvivorSpace");
        if (value != null) { survivorSpace = new String[] { value }; }
        
        value = properties.getProperty("PermGen");
        if (value != null) { permGen = new String[] { value }; }
        
        value = properties.getProperty("TenuredGen");
        if (value != null) { tenuredGen = new String[] { value }; }
        
        value = properties.getProperty("YoungCollector");
        if (value != null) { youngCollector = new String[] { value }; }
        
        value = properties.getProperty("TenuredCollector");
        if (value != null) { tenuredCollector = new String[] { value }; }
        
        // success
        return true;
    }
    
    /**
     * Save the configured JMX settings for memory pools and collectors so that
     * the customized settings will be persisted across restarts.  The settings
     * are stored in the temporary/working directory of the servlet. 
     * 
     * @return true if successfully saved, false otherwise
     */
    public boolean saveJMXSettings() {
        Log log = config.getLog();
        
        // create and validate file
        File settingsFile = getSettingsFile();
        
        // create the jmx properties state
        Properties properties  = new Properties();
        
        saveSetting(properties, "EdenSpace", edenSpace);
        saveSetting(properties, "SurvivorSpace", survivorSpace);
        saveSetting(properties, "PermGen", permGen);
        saveSetting(properties, "TenuredGen", tenuredGen);
        
        saveSetting(properties, "YoungCollector", youngCollector);
        saveSetting(properties, "TenuredCollector", tenuredCollector);
        
        // save the properties
        String comment = "Saved JMX settings on " + new Date();
        FileOutputStream out = null;
        
        try {
            out = new FileOutputStream(settingsFile);
            properties.store(out, comment);
        }
        catch (IOException ioe) {
            log.error("Unable to save JMX settings: " + ioe.getMessage());
            log.error(ioe);
            return false;
        }
        finally {
            try { if (out != null) { out.close(); } }
            catch (IOException e) { /* ignore */ }
        }
        
        // success
        return true;
    }
    
    /**
     * Set the name of the eden space region.  This should reflect the name of
     * the actual {@link MemoryPoolMXBean} containing the eden space stats.
     * 
     * @param edenSpace The name of the eden space
     */
    public void setEdenMemoryPool(String edenSpace) {
        this.edenSpace = new String[] { edenSpace };
    }
    
    /**
     * Set the name of the survivor space region.  This should reflect the name
     * of the actual {@link MemoryPoolMXBean} containing the survivor space 
     * stats.
     * 
     * @param survivorSpace The name of the survivor space
     */
    public void setSurvivorMemoryPool(String survivorSpace) {
        this.survivorSpace = new String[] { survivorSpace };
    }
    
    /**
     * Set the name of the tenured, or old, generation.  This should reflect the
     * name of the actual {@link MemoryPoolMXBean} containing the tenured
     * generation stats.
     * 
     * @param tenuredGen The name of the tenured generation
     */
    public void setTenuredMemoryPool(String tenuredGen) {
        this.tenuredGen = new String[] { tenuredGen };
    }
    
    /**
     * Set the name of the perm generation.  This should reflect the name of the 
     * actual {@link MemoryPoolMXBean} containing the perm generation stats.
     * 
     * @param permGen The name of the perm generation
     */
    public void setPermGenMemoryPool(String permGen) {
        this.permGen = new String[] { permGen };
    }
    
    /**
     * Set the name of the young garbage collector.  This should reflect the
     * name of the actual {@link GarbageCollectorMXBean} containing the young
     * collector stats.
     * 
     * @param youngCollector The name of the young collector
     */
    public void setYoungCollector(String youngCollector) {
        this.youngCollector = new String[] { youngCollector };
    }
    
    /**
     * Set the name of the tenured garbage collector.  This should reflect the
     * name of the actual {@link GarbageCollectorMXBean} containing the tenured
     * collector stats.
     * 
     * @param tenuredCollector The name of the tenured collector
     */
    public void setTenuredCollector(String tenuredCollector) {
        this.tenuredCollector = new String[] { tenuredCollector };
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
     * Get the {@link GarbageCollectorMXBean} with one of the given names.  The
     * first matching collector is returned. If no garbage collector exists for 
     * the given name, then <code>null</code> is returned.
     * 
     * @param names The names of the garbage collector to search for
     * 
     * @return The associated garbage collector or <code>null</code>
     * 
     * @see #getGarbageCollectorMXBeans()
     */
    public GarbageCollectorMXBean getGarbageCollectorMXBean(String[] names) {
        for (String name : names) {
            GarbageCollectorMXBean bean = this.getGarbageCollectorMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
    }
    
    /**
     * Get the {@link GarbageCollectorMXBean} that is associated with purely
     * the young generation including the eden and survivor spaces.  This 
     * returns the collector that has the configured <code>YoungCollector</code> 
     * name.  If no collector has been configured, then this attempts to return
     * the first collector that excludes the tenured generation based on the 
     * <code>TenuredGen</code> configuration or one of the known defaults.  If 
     * no collector is found, then <code>null</code> is returned.
     * 
     * @return The young generation collector or <code>null</code>
     *
     * @see #setYoungCollector(String)
     * @see #getTenuredMemoryPoolMXBean()
     * @see #getGarbageCollectorMXBean(String)
     * @see #getGarbageCollectorMXBeans()
     * 
     * @see #TENURED_GENERATION_POOLS
     */
    public GarbageCollectorMXBean getYoungCollectorMXBean() {
        GarbageCollectorMXBean bean = 
            this.getGarbageCollectorMXBean(this.youngCollector);
        if (bean != null) { return bean; }
        
        outer: for (GarbageCollectorMXBean bean2 : getGarbageCollectorMXBeans()) {
            for (String pool : bean2.getMemoryPoolNames()) {
                for (String name : this.tenuredGen) {
                    if (pool.equals(name)) {
                        continue outer;
                    }
                }
            }
            
            return bean2;
        }

        return null;
    }

    /**
     * Get the {@link GarbageCollectorMXBean} that is associated with the old or
     * tenured generations collection.  The same collector may also be
     * associated with young generations. This  returns the collector that has 
     * the configured <code>TenuredCollector</code> name.  If no collector has 
     * been configured, then this attempts to return the first collector that 
     * includes the tenured generation based on the <code>TenuredGen</code> 
     * configuration or one of the known defaults.  If no collector is found, 
     * then <code>null</code> is returned.
     * 
     * @return The tenured generation collector or <code>null</code>
     * 
     * @see #setTenuredCollector(String)
     * @see #getTenuredMemoryPoolMXBean()
     * @see #getGarbageCollectorMXBean(String)
     * @see #getGarbageCollectorMXBeans()
     * 
     * @see #TENURED_GENERATION_POOLS
     */
    public GarbageCollectorMXBean getTenuredCollectorMXBean() {
        GarbageCollectorMXBean bean = 
            this.getGarbageCollectorMXBean(this.tenuredCollector);
        if (bean != null) { return bean; }
        
        for (GarbageCollectorMXBean bean2 : getGarbageCollectorMXBeans()) {
            for (String pool : bean2.getMemoryPoolNames()) {
                for (String name : this.tenuredGen) {
                    if (pool.equals(name)) {
                        return bean2;
                    }
                }
            }
        }
    
        return null;
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
     * Get the {@link MemoryPoolMXBean} with one of the given names. The first 
     * matching memory pool is returned. If no memory pool exists for the given 
     * names, then <code>null</code> is returned.
     * 
     * @param names The names of the memory pools to search
     * 
     * @return The associated memory pool or <code>null</code>
     * 
     * @see #getMemoryPoolMXBeans()
     */
    public MemoryPoolMXBean getMemoryPoolMXBean(String[] names) {
        for (String name : names) {
            MemoryPoolMXBean bean = getMemoryPoolMXBean(name);
            if (bean != null) { return bean; }
        }
        
        return null;
    }
    
    /**
     * Get the {@link MemoryPoolMXBean} associated with the eden space in the
     * young generation.  This returns the memory pool that either has the
     * configured <code>EdenSpace</code> name or one of the known defaults.  If 
     * the given pool cannot be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the eden space or <code>null</code>
     * 
     * @see #setEdenMemoryPool(String)
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     * @see #EDEN_SPACE_DEFAULTS
     */
    public MemoryPoolMXBean getEdenMemoryPoolMXBean() {
        return this.getMemoryPoolMXBean(this.edenSpace);
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the survivor space in 
     * the young generation.  This returns the memory pool that either has the
     * configured <code>SurvivorSpace</code> name or one of the known defaults.  
     * If the given pool cannot be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the survivor space or <code>null</code>
     * 
     * @see #setSurvivorMemoryPool(String)
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     * @see #SURVIVOR_SPACE_DEFAULTS
     */
    public MemoryPoolMXBean getSurvivorMemoryPoolMXBean() {
        return this.getMemoryPoolMXBean(this.survivorSpace);
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the perm generation.  
     * This returns the memory pool that either has the configured 
     * <code>PermGen</code> name or one of the known defaults.  If the given 
     * pool cannot be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the perm gen or <code>null</code>
     * 
     * @see #setPermGenMemoryPool(String)
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     * @see #PERM_GEN_DEFAULTS
     */
    public MemoryPoolMXBean getPermGenMemoryPoolMXBean() {
        return this.getMemoryPoolMXBean(this.permGen);
    }

    /**
     * Get the {@link MemoryPoolMXBean} associated with the tenured gen in the
     * old generation.  This returns the memory pool that either has the
     * configured <code>TenuredGen</code> name or one of the known defaults.  If 
     * the given pool cannot be found, then <code>null</code> is returned.
     * 
     * @return The memory pool for the tenured gen or <code>null</code>
     * 
     * @see #setTenuredMemoryPool(String)
     * @see #getMemoryPoolMXBean(String)
     * @see #getMemoryPoolMXBeans()
     * @see #TENURED_GEN_DEFAULTS
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
