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

package org.teatrove.trove.jcache;

import javax.cache.CacheFactory;
import javax.cache.CacheException;
import javax.cache.Cache;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.ThreadPool;
import org.teatrove.trove.util.Depot;
import org.teatrove.trove.util.tq.*;

/**
 * This class is an adapter that maps the Depot API to the
 * JCache interfaces.<p>
 *
 * For further info on the JCache API, see:<br>
 * https://jsr-107-interest.dev.java.net/javadoc/javax/cache/package-summary.html</p>
 *
 * This class can either be configured as a bean, or via an implementation
 * of PropertyMap.   Also, it can be used as a factory for plain old Depot
 * instances and not just for JCache.<p>
 *
 * For details on how to configure the factory, review the source for this class.
 * It is pretty straightforward.
 *
 * @author Guy A. Molinari
 *
 */
public class DepotFactory implements CacheFactory {

    private Log mLog;                        // The Log object 
    private Depot mDepot;                    // Constructed Depot instance.
    private Depot.Factory mFactory;          // The Depot retriever factory.
    private TransactionQueue mDepotTQ;       // The TQ for the Depot.
    private int mDepotValidCacheSize = 0;    // The Depot valid cache size.
    private int mDepotInvalidCacheSize = 0;  // The Depot invalid cache size.
    private int mDepotTimeout = -1;          // The Depot timeout.
    private Map mValidCache;                 // The valid items cache.
    private Map mInvalidCache;               // The invalid items cache.
    private String mThreadPoolName;          // Name of the ThreadPool.
    private int mThreadPoolMaxThreads;       // Max number of threads in the thread pool.
    private int mThreadPoolTimeout;          // Thread timeout, in ms.
    
    // Flag to have the Depot return data immediately from invalid cache.
    private boolean mDepotReturnImmediately = false;
    
    // Specifies whether the caches use hard-limit LRU behavior or the legacy soft-reference behavior.
    private boolean mCachesUseLRU = false;
    
    // Constant defines.
    public final static String LOGGER_INSTANCE = "logger.instance";
    public final static String DEPOT_FACTORY_INSTANCE = "depot.factory.instance";
    public final static String LRU_ENABLED = "caches.useLRU";
    public final static String THREAD_POOL_NAME = "name";
    public final static String THREAD_POOL_MAX_THREADS = "max.threads";
    public final static String THREAD_POOL_TIMEOUT = "threadpool.timeout";
    public final static String DEFAULT_THREAD_POOL_NAME = "DepotThreadPool";
    public final static int DEFAULT_THREAD_POOL_MAX_THREADS = 20;
    public final static int DEFAULT_THREAD_POOL_TIMEOUT = 5000;
    public final static String VALID_CACHE_SIZE = "validCache.size";
    public final static String INVALID_CACHE_SIZE = "invalidCache.size";
    public final static String CACHE_TIMEOUT = "cache.timeout";
    public final static String RETURN_INVALID_IMMEDIATELY = "return.invalid.immediately";
    public final static String TQ_SUBMAP_NAME = "TQ.config";


    /**
     * Create a new implementation specific Cache object using the env parameters.
     * If null is passed instead of a property map, then this method will attempt
     * to initialize based upon the member variable contents (via setX).
     */
    public Cache createCache(Map env) throws CacheException {

        PropertyMap tqProperties = null;

        if (env != null) {
            if (! (env instanceof PropertyMap))
                throw new CacheException("The environment map parameter must be an instance of PropertyMap.");

            PropertyMap properties = (PropertyMap) env;
            mLog = (Log) env.get(LOGGER_INSTANCE);
            mFactory = (Depot.Factory) env.get(DEPOT_FACTORY_INSTANCE);
            mThreadPoolName = properties.getString(THREAD_POOL_NAME, DEFAULT_THREAD_POOL_NAME);
            mThreadPoolMaxThreads = properties.getInt(THREAD_POOL_MAX_THREADS, DEFAULT_THREAD_POOL_MAX_THREADS);
            mThreadPoolTimeout = properties.getInt(THREAD_POOL_TIMEOUT, DEFAULT_THREAD_POOL_TIMEOUT);
            mCachesUseLRU = properties.getBoolean(LRU_ENABLED, false);
            mDepotValidCacheSize = properties.getInt(VALID_CACHE_SIZE);
            mDepotInvalidCacheSize = properties.getInt(INVALID_CACHE_SIZE);
            mDepotTimeout = properties.getInt(CACHE_TIMEOUT);
            mDepotReturnImmediately = properties.getBoolean(RETURN_INVALID_IMMEDIATELY);
    
            // TQ Properties
            tqProperties = properties.subMap(TQ_SUBMAP_NAME);
            if (tqProperties == null) {
                String errMsg = "TransactionQueue (TQ.config) property map is null.";
                mLog.error(errMsg);
                throw new CacheException(errMsg);
            }
        }

        validate();

        if (mDepotTQ == null) {
            // Set up the thread pool.
            ThreadPool pool = new ThreadPool(mThreadPoolName, mThreadPoolMaxThreads);
            pool.setTimeout((long) mThreadPoolTimeout);
    
            mDepotTQ = new TransactionQueue(pool, 0, 1);
            mDepotTQ.applyProperties(tqProperties);
        }

        if (mCachesUseLRU) {
            mValidCache = new ConcurrentHashMap(mDepotValidCacheSize);
            mInvalidCache = new ConcurrentHashMap(mDepotInvalidCacheSize);
        }
        else {
            mValidCache = new HashMap(mDepotValidCacheSize);
            mInvalidCache = new HashMap(mDepotInvalidCacheSize);
        }

        // The Depot supports optionally wrapping its constructor-supplied caches in synchronized maps. Alternatively,
        // the user of the Depot can choose to synchronize the caches themselves. When our caches have been configured
        // to be LRUCaches, we construct the LRUCaches with ConcurrentHashMap (see CacheUtils.createMap), so we don't
        // need the Depot to synchronize the caches. When our caches have been configured to be non-LRU Caches, we do
        // need the Depot to synchronize the caches. In other words, mCachesUseLRU == !synchronize.
        
        mDepot = new Depot(mFactory, mValidCache, mInvalidCache, mDepotTQ, mDepotTimeout,
            mDepotReturnImmediately, !mCachesUseLRU);
        
        return new DepotAdapter(mDepot);

    }


    protected void validate() throws CacheException {

        if (mLog == null)
            throw new CacheException(LOGGER_INSTANCE + " is null.");

        if (mFactory == null)
            throw new CacheException(DEPOT_FACTORY_INSTANCE + " is null.");

        if (mThreadPoolName == null)
            mThreadPoolName = DEFAULT_THREAD_POOL_NAME;
        mLog.debug("Name: " + mThreadPoolName);

        if (mThreadPoolMaxThreads == 0)
            mThreadPoolMaxThreads = DEFAULT_THREAD_POOL_MAX_THREADS;
        mLog.debug("Max threads: " + mThreadPoolMaxThreads);

        if (mThreadPoolTimeout == 0)
            mThreadPoolTimeout = DEFAULT_THREAD_POOL_TIMEOUT;
        mLog.debug("Timeout: " + mThreadPoolTimeout + " ms");

        mLog.debug("Use LRUCache = " + mCachesUseLRU);
        mLog.debug("Cache timeout = " + mDepotTimeout);
        mLog.debug("Return invalid items immediately = " + mDepotReturnImmediately);

        if (mDepotValidCacheSize <= 0)
            throw new CacheException(VALID_CACHE_SIZE + " must be > 0.");
        mLog.debug("Valid cache size = " + mDepotValidCacheSize);

        if (mDepotInvalidCacheSize <= 0)
            throw new CacheException(INVALID_CACHE_SIZE + " must be > 0.");
        mLog.debug("Valid cache size = " + mDepotInvalidCacheSize);
    }


    public Log getLogger() { return mLog; }
    public void setLogger(Log logger) { mLog = logger; }

    public Depot.Factory getFactory() { return mFactory; }
    public void setFactory(Depot.Factory factory) { mFactory = factory; }

    public Depot getDepot() { return mDepot; }

    public TransactionQueue getTransactionQueue() { return mDepotTQ; }
    public void setTransactionQueue(TransactionQueue tq) { mDepotTQ = tq; }

    public int getValidCacheSize() { return mDepotValidCacheSize; }
    public void setValidCacheSize(int size) { mDepotValidCacheSize = size; }

    public int getInvalidCacheSize() { return mDepotInvalidCacheSize; }
    public void setInvalidCacheSize(int size) { mDepotInvalidCacheSize = size; }

    public int getDepotTimeout() { return mDepotTimeout; }
    public void setDepotTimeout(int timeout) { mDepotTimeout = timeout; }

    public String getThreadPoolName() { return mThreadPoolName; }
    public void setThreadPoolName(String name) { mThreadPoolName = name; }

    public int getThreadPoolMaxThreads() { return mThreadPoolMaxThreads; }
    public void setThreadPoolMaxThreads(int maxThreads) { mThreadPoolMaxThreads = maxThreads; }

    public int getThreadPoolTimeout() { return mThreadPoolTimeout; }
    public void setThreadPoolTimeout(int timeout) { mThreadPoolTimeout = timeout; }

    public boolean getDepotReturnImmediately() { return mDepotReturnImmediately; }
    public void setDepotReturnImmediately(boolean flag) { mDepotReturnImmediately = flag; }
   
    public boolean getCachesUseLRU() { return mCachesUseLRU; }
    public void setCachesUseLRU(boolean flag) { mCachesUseLRU = flag; }
    
}
