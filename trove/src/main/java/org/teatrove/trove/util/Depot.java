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

package org.teatrove.trove.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import org.teatrove.trove.util.tq.Transaction;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.TransactionQueueData;
import com.javabi.sizeof.MemoryUtil;

/**
 * Depot implements a simple and efficient caching strategy. It is thread-safe,
 * and it allows requests of different objects to occur concurrently. Depot
 * is best suited as a front-end for accessing objects from a remote device,
 * like a database. If the remote device is not responding, the Depot will
 * continue to serve invalidated objects so that the requester may continue
 * as normal.
 * <p>
 * Depot is designed as a cache in front of an object {@link Factory factory}.
 * Objects may be invalidated, but they are not explicitly removed from the
 * cache until a replacement has been provided by the factory. The factory is
 * invoked from another thread, allowing for the requester to timeout and use
 * an invalidated object. When the factory eventually finishes, its object will
 * be cached.
 * <p>
 * By allowing for eventual completion of the factory, Depot enables
 * applications to dynamically adjust to the varying performance and
 * reliability of remote data providers.
 * <p>
 * Depot will never return an object or null that did not originate from the
 * factory. When retrieving an object that wasn't found cached, a call to the
 * factory will block until it is finished.
 * <p>
 * Objects may be invalided from the Depot
 * {@link PerishablesFactory automatically}. This approach is based on a fixed
 * time expiration and is somewhat inflexible. An ideal invalidation strategy
 * requires asynchronous notification from the actual data providers.
 *
 * @author Brian S O'Neill
 * @see MultiKey
 */

public class Depot<V> implements Map<Object, V>, LRUCache.ExpirationListener {

    private static final long CLEANUP_DELAY = 60L;
    protected static final long DEFAULT_CLEANUP_INTERVAL = 10L;

    private final Factory<? extends V> mDefaultFactory;
    final Map<ValueWrapper<Object>, ValueWrapper<V>> mValidCache;
    final Map<ValueWrapper<Object>, ValueWrapper<V>> mInvalidCache;
    final Map<ValueWrapper<Object>, ValueWrapper<V>> mBackingValidCache;
    final Map<ValueWrapper<Object>, ValueWrapper<V>> mBackingInvalidCache;
    private final Kernel<V> mKernel;
    final TransactionQueue mQueue;
    private final long mTimeout;
    private boolean mReturnInvalidNoWait = false;
    private final AtomicLong mCacheGets = new AtomicLong();
    private final AtomicLong mCacheHits = new AtomicLong();
    private final AtomicLong mCacheMisses = new AtomicLong();
    private ConcurrentMap<Object, Long> mExpirations;
    private List<InvalidationListener> mListeners = new ArrayList<InvalidationListener>();
    private ScheduledExecutorService mCleanupScheduler = null;
    private long mCleanupInterval = DEFAULT_CLEANUP_INTERVAL;
    private final ConcurrentMap<ValueWrapper<Object>, Retriever> mRetrievers = 
        new ConcurrentHashMap<ValueWrapper<Object>, Retriever>();

    /**
     * Indicates whether or not the current thread should be renamed while this {@link Depot} is fetching
     * an object.  This is currently only done for the {@link Depot} class itself, and not its subclasses,
     * as the authors of the subclasses found that this had a serious performance impact for them.
     */

    private final boolean mSetThreadName;

    /**
     * @param factory Default factory from which objects are obtained
     * @param validCache Map to use for caching valid objects
     * @param invalidCache Map to use for caching invalid objects
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     * @param synchronize If set to 'true' tells the Depot to
     * call Collections.synchronizedMap to wrap the Map implementations
     * method.
     * @param cleanupInterval Number of seconds between TTL cleanup thread runs.  A nonpositive
     * value disables the cleanup thread.
     */

    public Depot(Factory<? extends V> factory, Map<ValueWrapper<Object>, ValueWrapper<V>> validCache, Map<ValueWrapper<Object>, ValueWrapper<V>> invalidCache,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait,
                 boolean synchronize, long cleanupInterval) {
        Validate.notNull(validCache, "Valid cache cannot be null when creating a Depot.");
        Validate.notNull(invalidCache, "Invalid cache cannot be null when creating a Depot.");
        Validate.notNull(tq, "Transaction queue cannot be null when creating a Depot.");

        mDefaultFactory = factory;
        mCleanupInterval = cleanupInterval;
        if (synchronize) {
            mValidCache = Collections.synchronizedMap(validCache);
            mInvalidCache = Collections.synchronizedMap(invalidCache);
        }
        else {
            mValidCache = validCache;
            mInvalidCache = invalidCache;
        }
        mKernel = new SimpleKernel();
        mQueue = tq;
        mTimeout = timeout;
        mReturnInvalidNoWait = returnInvalidNoWait;
        if (validCache instanceof LRUCache) {
            ((LRUCache) validCache).addListener(this);
        }
        mBackingValidCache = validCache;
        mBackingInvalidCache = invalidCache;
        mSetThreadName = getClass() == Depot.class;
    }

    /**
     * @param factory Default factory from which objects are obtained
     * @param validCache Map to use for caching valid objects
     * @param invalidCache Map to use for caching invalid objects
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     * @param synchronize If set to 'true' tells the Depot to
     * call Collections.synchronizedMap to wrap the Map implementations
     * method.
     */
    public Depot(Factory<? extends V> factory, Map<ValueWrapper<Object>, ValueWrapper<V>> validCache, Map<ValueWrapper<Object>, ValueWrapper<V>> invalidCache,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait,
                 boolean synchronize) {
        this(factory, validCache, invalidCache, tq, timeout, returnInvalidNoWait,
            synchronize, DEFAULT_CLEANUP_INTERVAL);
    }

    /**
     * @param factory Default factory from which objects are obtained
     * @param validCache Map to use for caching valid objects
     * @param invalidCache Map to use for caching invalid objects
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     */
    public Depot(Factory<? extends V> factory, Map<ValueWrapper<Object>, ValueWrapper<V>> validCache, Map<ValueWrapper<Object>, ValueWrapper<V>> invalidCache,
                 TransactionQueue tq, long timeout) {
        this(factory, validCache, invalidCache, tq, timeout, false, true, DEFAULT_CLEANUP_INTERVAL);
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param validCache Map to use for caching valid objects
     * @param invalidCache Map to use for caching invalid objects
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     */
    public Depot(Factory<? extends V> factory, Map<ValueWrapper<Object>, ValueWrapper<V>> validCache, Map<ValueWrapper<Object>, ValueWrapper<V>> invalidCache,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait) {
        this(factory, validCache, invalidCache, tq, timeout, returnInvalidNoWait, true,
            DEFAULT_CLEANUP_INTERVAL);
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param cacheSize Number of items guaranteed to be in cache, if negative,
     * cache is completely disabled.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     */
    public Depot(Factory<? extends V> factory, int cacheSize,
                 TransactionQueue tq, long timeout) {
        this(factory, cacheSize, tq, timeout, false);
    }

    /**
     * @param factory Default factory from which objects are obtained
     * @param validCacheSize Number of items guaranteed to be in cache, if negative,
     * cache is completely disabled.
     * @param invalidCacheSize Number of items guaranteed to be in the invalid cache.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param useLRUCache use LRUCache instead of soft reference Cache.
     * LRUCache does not use soft references.  Items that "expire" will be
     * eligible for garbage collection.
     *
     */
    public Depot(Factory<? extends V> factory, int validCacheSize, int invalidCacheSize,
                 TransactionQueue tq, long timeout, boolean useLRUCache) {
        this(factory, validCacheSize, invalidCacheSize, tq, timeout, useLRUCache, DEFAULT_CLEANUP_INTERVAL);
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param validCacheSize Number of items guaranteed to be in cache, if negative,
     * cache is completely disabled.
     * @param invalidCacheSize Number of items guaranteed to be in the invalid cache.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param useLRUCache use LRUCache instead of soft reference Cache.
     * LRUCache does not use soft references.  Items that "expire" will be
     * eligible for garbage collection.
     * @param cleanupInterval Number of seconds between TTL cleanup thread runs.  A nonpositive
     * value disables the cleanup thread.
     *
     */
    @SuppressWarnings("unchecked")
    protected Depot(Factory<? extends V> factory, int validCacheSize, int invalidCacheSize,
                    CacheProvider<V> cacheProvider, TransactionQueue tq, long timeout, boolean useLRUCache, long cleanupInterval) {
        Validate.notNull(tq, "Transaction queue cannot be null when creating a Depot.");

        Map<ValueWrapper<Object>, ValueWrapper<V>> valid, invalid;

        mCleanupInterval = cleanupInterval;

        if (validCacheSize < 0) {
            valid = Utils.voidMap();
            invalid = Utils.voidMap();
        }
        else {
            if (useLRUCache) {
                valid = new LRUCache(validCacheSize);
                ((LRUCache) valid).addListener(this);
                invalid = new LRUCache(invalidCacheSize);
            }
            else if (validCacheSize > 0) {
                valid = cacheProvider.createValidCache();
                invalid = cacheProvider.createInvalidCache(valid);
            }
            else {
                valid = cacheProvider.createValidHashMap();
                invalid = cacheProvider.createInvalidHashMap();
            }
        }

        mDefaultFactory = factory;
        mBackingValidCache = valid;
        mBackingInvalidCache = invalid;
        mValidCache = Collections.synchronizedMap(valid);
        mInvalidCache = Collections.synchronizedMap(invalid);
        mKernel = new SimpleKernel();
        mQueue = tq;
        mTimeout = timeout;

        mSetThreadName = getClass() == Depot.class;
    }

    protected interface CacheProvider<V> {
        Map<ValueWrapper<Object>, ValueWrapper<V>> createValidCache();
        Map<ValueWrapper<Object>, ValueWrapper<V>> createInvalidCache(final Map<ValueWrapper<Object>, ValueWrapper<V>> validCache);
        Map<ValueWrapper<Object>, ValueWrapper<V>> createValidHashMap();
        Map<ValueWrapper<Object>, ValueWrapper<V>> createInvalidHashMap();
    }

    @SuppressWarnings("unchecked")
    public Depot(Factory<? extends V> factory, final int validCacheSize, final int invalidCacheSize,
            TransactionQueue tq, long timeout, boolean useLRUCache, long cleanupInterval) {
        this(factory, validCacheSize, invalidCacheSize,
             new CacheProvider<V>() {

                public Map<ValueWrapper<Object>, ValueWrapper<V>> createValidCache() {
                    return new Cache(validCacheSize);
                }

                public Map<ValueWrapper<Object>, ValueWrapper<V>> createInvalidCache(final Map<ValueWrapper<Object>, ValueWrapper<V>> validCache) {
                    return new Cache((Cache) validCache);
                }

                public Map<ValueWrapper<Object>, ValueWrapper<V>> createValidHashMap() {
                    return new SoftHashMap();
                }

                public Map<ValueWrapper<Object>, ValueWrapper<V>> createInvalidHashMap() {
                    return new SoftHashMap();
                }
             }, tq, timeout, useLRUCache, cleanupInterval);
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param validCacheSize Number of items guaranteed to be in the valid cache.
     * @param invalidCacheSize Number of items guaranteed to be in the invalid cache.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     * @param useLRUCache use LRUCache instead of soft reference Cache.
     * LRUCache does not use soft references.  Items that "expire" will be
     * eligible for garbage collection.
     */
    public Depot(Factory<? extends V> factory, int validCacheSize, int invalidCacheSize,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait,
                 boolean useLRUCache) {
        this(factory, validCacheSize, invalidCacheSize, tq, timeout, useLRUCache, DEFAULT_CLEANUP_INTERVAL);
        mReturnInvalidNoWait = returnInvalidNoWait;
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param cacheSize Number of items guaranteed to be in cache, if negative,
     * cache is completely disabled.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     */
    public Depot(Factory<? extends V> factory, int cacheSize,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait) {
        this(factory, cacheSize, cacheSize, tq, timeout, false, DEFAULT_CLEANUP_INTERVAL);
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param kernel Kernel for supporting more advanced Depots, such as
     * persistent ones.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     */
    public Depot(Factory<? extends V> factory, Kernel<V> kernel,
                 TransactionQueue tq, long timeout) {
        Validate.notNull(kernel, "Kernel cannot be null when creating a Depot.");
        Validate.notNull(tq, "Transaction queue cannot be null when creating a Depot.");

        mDefaultFactory = factory;
        mBackingValidCache = mValidCache = kernel.validCache();
        mBackingInvalidCache = mInvalidCache = kernel.invalidCache();
        mKernel = kernel;
        mQueue = tq;
        mTimeout = timeout;

        mSetThreadName = getClass() == Depot.class;
    }


    /**
     * @param factory Default factory from which objects are obtained
     * @param kernel Kernel for supporting more advanced Depots, such as
     * persistent ones.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     * @param returnInvalidNoWait If set to 'true' tells the Depot to
     * return data from the invalid cache (if exists) and continue
     * depot retrieval.
     */
    public Depot(Factory<? extends V> factory, Kernel<V> kernel,
                 TransactionQueue tq, long timeout, boolean returnInvalidNoWait) {
        this(factory, kernel, tq, timeout);
        mReturnInvalidNoWait = returnInvalidNoWait;

    }

    public void setReturnInvalidNoWait(boolean flag) {
        mReturnInvalidNoWait = flag;
    }


    @Override protected void finalize() {
        if (mValidCache instanceof LRUCache)
            ((LRUCache) mValidCache).removeListener(this);
    }


    @Override public String toString() {
        return "Depot " + mQueue.getName();
    }


    /**
     * Register for invalidation event interest.
     */
    public void addInvalidationListener(final InvalidationListener l) {
        Validate.notNull(l, "Listener cannot be null when being added to a depot");

        mListeners.add(l);
    }


    /**
     * De-register interest in invalidation events.
     */
    public void removeInvalidationListener(InvalidationListener l) {
        Validate.notNull(l, "Listener cannot be null when being removed from a depot");

        mListeners.remove(l);
    }


    /*
     * Implement ExpirationListener to handle LRUCache expiry events.
     * (If applicable.)
     */
    public void expireEvent(final LRUCache.Entry entry) {
        Validate.notNull(entry, "Cannot expire null event.");

        fireInvalidationEvent(entry.getKey());
    }


    /**
     * Returns the total number objects in the Depot.
     */
    public int size() {
        return mKernel.size();
    }

    public boolean isEmpty() {
        return mKernel.isEmpty();
    }

    /**
     * Provides access to the Default Factory or returns null if no
     * DefaultFactory is present.
     */
    public Factory<? extends V> getDefaultFactory() {
        return mDefaultFactory;
    }

    /**
     * Provides access to the TransactionQueue statistics of the
     * Depot.
     */
    public TransactionQueueData getTQStatistics() {
        return mQueue.getStatistics();
    }

    /**
     * Returns the number of valid objects in the Depot.
     */
    public int validSize() {
        return mValidCache.size();
    }

    public int getValidMaxRecent() {
        if (mBackingValidCache instanceof LRUCache)
            return ((LRUCache) mBackingValidCache).getMaxRecent();
        if (mBackingValidCache instanceof Cache)
            return ((Cache) mBackingValidCache).getMaxRecent();
        return -1;
    }

    /**
     * Returns the number of cache reads
     */
    public long getCacheGets() {
        return mCacheGets.get();
    }

    /**
     * Returns the number of times an item has been found in the
     * cache while attempting a read operation.
     */
    public long getCacheHits() {
        return mCacheHits.get();
    }

    /**
     * Returns the number of times an item has not been found in the
     * cache necessitating a back end read.
     */
    public long getCacheMisses() {
        return mCacheMisses.get();
    }

    /**
     * Reset counters.
     */
    public void reset() {
        mCacheGets.lazySet(0L);
        mCacheHits.lazySet(0L);
        mCacheMisses.lazySet(0L);
    }

    /**
     * Returns the number of invalid objects in the Depot.
     */
    public int invalidSize() {
        return mInvalidCache.size();
    }

    public int getInvalidMaxRecent() {
        if (mBackingInvalidCache instanceof LRUCache)
            return ((LRUCache) mBackingInvalidCache).getMaxRecent();
        if (mBackingInvalidCache instanceof Cache)
            return ((Cache) mBackingInvalidCache).getMaxRecent();
        return -1;
    }

    /**
     * Determines whether a key is in the valid cache.
     */
    public boolean validContainsKey(Object inKey) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        return mValidCache.containsKey(key);
    }


    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param key key of object to retrieve
     */
    public V get(Object key) {
        return get(mDefaultFactory, key, mTimeout);
    }


    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param key key of object to retrieve
     * @param timeout max time (in milliseconds) to wait for an invalid value
     * to be replaced from the factory, if negative, wait forever. Ignored if
     * no cached value exists at all.
     */
    public V get(Object key, long timeout) {
        return get(mDefaultFactory, key, timeout);
    }


    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param factory factory to use to retrieve object if not cached
     * @param key key of object to retrieve
     */
    public V get(Factory<? extends V> factory, Object key) {
        return get(factory, key, mTimeout);
    }


    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param factory factory to use to retrieve object if not cached
     * @param key key of object to retrieve
     * @param timeout max time (in milliseconds) to wait for an invalid value
     * to be replaced from the factory, if negative, wait forever. Ignored if
     * no cached value exists at all.
     */
    public V get(Factory<? extends V> factory, Object key, long timeout) {
        ValueWrapper<? extends V> value = getWrappedValue(factory, key, timeout);
        if (value != null && value.isValueSet())
            return value.getValue();
        return null;
    }


    /**
     * Return an value contained within a wrapper that tracks per instance
     * stats such as timestamps, validity, etc.  See the corresponding
     * get(Object key, long timeout) method for details.
     */
    public ValueWrapper<? extends V> getWrappedValue(Object key, long timeout) {
        return getWrappedValue(mDefaultFactory, key, timeout);
    }


    /**
     * Return an value contained within a wrapper that tracks per instance
     * stats such as timestamps, validity, etc.  See the corresponding
     * get(Object key) method for details.
     */
    public ValueWrapper<? extends V> getWrappedValue(Object key) {
        return getWrappedValue(mDefaultFactory, key, mTimeout);
    }


    /**
     * Return an value contained within a wrapper that tracks per instance
     * stats such as timestamps, validity, etc.  See the corresponding
     * get(Factory factory, Object key) method for details.
     */
    public ValueWrapper<? extends V> getWrappedValue(Factory<V> factory, Object key) {
        return getWrappedValue(factory, key, mTimeout);
    }


    /**
     * Return an value contained within a wrapper that tracks per instance
     * stats such as timestamps, validity, etc.  See the corresponding
     * get(Factory factory, Object key, long timeout) method for details.
     */
    public ValueWrapper<? extends V> getWrappedValue(Factory<? extends V> factory, Object inKey, long timeout) {
        // Do quick check first.
        mCacheGets.getAndIncrement();
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        ValueWrapper<V> value = mValidCache.get(key);
        if (value != null) {
            validTest: {
                if (value.getValue() instanceof Perishable) {
                    if (!((Perishable)value.getValue()).isValid()) {
                        break validTest;
                    }
                }

                if (mExpirations == null) {
                    mCacheHits.getAndIncrement();
                    value.setLastAccessTime();
                    return value;
                }

                Long expire = (Long)mExpirations.get(inKey);
                if (expire == null ||
                    System.currentTimeMillis() <= expire.longValue()) {
                    // Value is still valid.
                    mCacheHits.getAndIncrement();
                    value.setLastAccessTime();
                    return value;
                }
            }
        }

        Retriever r = getRetriever(key);
        try {
            synchronized (r) {
                if (value != null) {
                    validTest: {
                        if (value.getValue() instanceof Perishable) {
                            if (!((Perishable)value.getValue()).isValid()) {
                                break validTest;
                            }
                        }

                        if (mExpirations == null) {
                            mCacheHits.getAndIncrement();
                            value.setLastAccessTime();
                            return value;
                        }
                        Long expire = (Long)mExpirations.get(inKey);
                        if (expire == null ||
                            System.currentTimeMillis() <= expire.longValue()) {
                            // Value is still valid.
                            mCacheHits.getAndIncrement();
                            value.setLastAccessTime();
                            return value;
                        }

                        // Value has expired.
                        mExpirations.remove(inKey);
                        invalidate(inKey);
                    }

                }
                else {
                    value = mInvalidCache.get(key);
                    if (value == null) {
                        // Wait forever since not even an invalid value exists.
                        timeout = -1;
                    }
                }

                ValueWrapper<? extends V> newValue = r.retrieve(factory, timeout, false);
                mCacheMisses.getAndIncrement();
                return (newValue.isValueSet()) ? newValue : value;
            }
        }
        finally {
            releaseRetriever(key, r);
        }
    }


    /**
     * Items are usually expired during calls to get.  The evict() method will
     * actively expire any qualifying items in the expiration list.
     */
    public void evict() {
        if (mExpirations != null) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Object, Long>> i = mExpirations.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Object, Long> e = i.next();
                if (now >= e.getValue()) {
                    i.remove();
                    invalidate(e.getKey());
                }
            }
        }
    }


    /**
     * Invalidate the object referenced by the given key, if it is already
     * cached in this Depot. Invalidated objects are not removed from the
     * Depot until a replacement object has been successfully created from the
     * factory.
     *
     * @param key key of object to invalidate
     */
    public void invalidate(Object inKey) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        if (mValidCache.containsKey(key)) {
            ValueWrapper<V> value = mValidCache.get(key);
            if (value == null)
                return;
            synchronized (value) {
                mValidCache.remove(key);
                value.setValid(false);
                fireInvalidationEvent(inKey);
                mInvalidCache.put(key, value);
            }
        }
    }


    private void fireInvalidationEvent(Object key) {
        for (InvalidationListener l : mListeners)
            l.invalidated(key);
    }


    /**
     * Invalidates objects in the Depot, using a filter. Each key that the
     * filter accepts is invalidated.
     */
    public void invalidateAll(Filter filter) {
        mKernel.invalidateAll(filter);
    }


    /**
     * Invalidates all the objects in the Depot.
     */
    public void invalidateAll() {
        mKernel.invalidateAll();
    }


    /**
     * Put a value into the Depot, bypassing the factory. Invalidating an
     * object and relying on the factory to produce a new value is generally
     * preferred. This method will notify any threads waiting on a factory to
     * produce a value, but it will not disrupt the behavior of the factory.
     *
     * @param key key with which to associate the value.
     * @param value value to be associated with key.
     */

    public V put(final Object inKey, final V value) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        Retriever r = getRetriever(key);
        try {
            synchronized (r) {
                ValueWrapper<V> oldInvalid = mInvalidCache.remove(key);
                ValueWrapper<V> wrappedValue = ValueWrapper.wrap(value);
                ValueWrapper<V> oldValid = mValidCache.put(key, wrappedValue);
                // Bypass the factory produced value so that any waiting
                // threads are notified.
                r.bypassValue(value);

                if (oldValid != null)
                    return oldValid.getValue();
                else if (oldInvalid != null)
                    return oldInvalid.getValue();
                else
                    return null;
            }
        }
        finally {
            releaseRetriever(key, r);
        }
    }


    /**
     * Completely removes an item from the Depot's caches. Invalidating an
     * object is preferred, and remove should be called only if the object
     * should absolutely never be used again.
     */
    public V remove(Object inKey) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        Retriever r = getRetriever(key);
        try {
            synchronized (r) {
                ValueWrapper<V> old;
                if (mValidCache.containsKey(key)) {
                    old = mValidCache.remove(key);
                    mInvalidCache.remove(key);
                }
                else {
                    mValidCache.remove(key);
                    old = mInvalidCache.remove(key);
                }
                r.clearValue();
                return old != null ? old.getValue() : null;
            }
        }
        finally {
            releaseRetriever(key, r);
        }
    }


    /**
     * Completely removes all the items from the Depot that the given filter
     * accepts.
     */
    public void removeAll(Filter filter) {
        mKernel.removeAll(filter);
    }


    /**
     * Completely removes all items from the Depot's caches. Invalidating all
     * the objects is preferred, and clear should be called only if all the
     * cached objects should absolutely never be used again.
     */
    public synchronized void clear() {
        mKernel.clear();
    }


    /**
     * Returns true if either the valid or invalid cache contain the specified key.
     */
    public boolean containsKey(Object inKey) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        return mValidCache.containsKey(key) || mInvalidCache.containsKey(key);
    }


    /**
     * Returns true if either the valid or invalid cache maps this value to one or
     * more keys.
     */
    public boolean containsValue(Object inValue) {
        ValueWrapper<Object> value = ValueWrapper.wrap(inValue);
        return mValidCache.containsValue(value) || mInvalidCache.containsValue(value);
    }


    /**
     * Returns the cached value for a given key if it exists in either the valid or
     * invalid maps.  No stats are updated and retrievers are not utilized.
     */
    public V peek(Object inKey) {
        ValueWrapper<Object> key = ValueWrapper.wrap(inKey);
        if (mValidCache.containsKey(key))
            return mValidCache.get(key).getValue();
        if (mInvalidCache.containsKey(key))
            return mInvalidCache.get(key).getValue();
        return null;
    }


    /**
     * Returns a set view of all items in both valid and invalid caches.
     */
    public Set<Map.Entry<Object, V>> entrySet() {
        CopyOnWriteArraySet<Map.Entry<Object, V>> result = 
            new CopyOnWriteArraySet<Map.Entry<Object, V>>();
        for (Map.Entry<ValueWrapper<Object>, ValueWrapper<V>> o : mValidCache.entrySet()) {
            result.add(new Entry<V>(o.getKey().getValue(), o.getValue().getValue()));
        }

        for (Map.Entry<ValueWrapper<Object>, ValueWrapper<V>> o : mInvalidCache.entrySet()) {
            result.add(new Entry<V>(o.getKey().getValue(), o.getValue().getValue()));
        }

        return result;
    }

    /**
     * Returns a set view of all keys in both valid and invalid caches.
     */

    public Set<Object> keySet() {
        CopyOnWriteArraySet<Object> result = 
            new CopyOnWriteArraySet<Object>();
        
        for (ValueWrapper<Object> key : mValidCache.keySet()) {
            result.add(key.getValue());
        }

        for (ValueWrapper<Object> key : mInvalidCache.keySet()) {
            result.add(key.getValue());
        }

        return result;
    }

    @Override public boolean equals(final Object o) {
        boolean result = o == this;

        if (!result && o instanceof Map<?, ?>) {
            Map<?, ?> other = (Map<?, ?>) o;
            if (other.size() == size()) {
                if (o instanceof Depot<?>) {
                    Depot<?> otherDepot = (Depot<?>) o;
                    Map<Object, ValueWrapper<V>> thisMap = new HashMap<Object, ValueWrapper<V>>(mValidCache);
                    thisMap.putAll(mInvalidCache);

                    Map<Object, Object> otherMap = new HashMap<Object, Object>(otherDepot.mValidCache);
                    otherMap.putAll(otherDepot.mInvalidCache);

                    result = thisMap.equals(otherMap);
                }
                else {
                    result = o.equals(this);
                }
            }
            else {
                result = false;
            }
        }

        return result;
    }


    @Override public int hashCode() {
        int validHash = mValidCache.hashCode();
        int invalidHash = mInvalidCache.hashCode();
        return validHash + invalidHash;
    }


    /**
     * Copies all of the specified mappings to the Depot via its put() method.
     */
    public void putAll(Map<? extends Object, ? extends V> m) {
        for (Map.Entry<? extends Object, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }


    /**
     * Returns a collection view of all items in the Depot.
     */
    public Collection<V> values() {
        List<V> result = new ArrayList<V>(mValidCache.size() + mInvalidCache.size());

        for (ValueWrapper<V> wrappedValue : mValidCache.values()) {
            result.add(wrappedValue.getValue());
        }

        for (ValueWrapper<V> wrappedValue : mInvalidCache.values()) {
            result.add(wrappedValue.getValue());
        }

        return result;
    }


    void setExpiration(final Object key, final long duration) {
        Long expire = new Long(System.currentTimeMillis() + duration);
        if (mExpirations == null) {
            initCleanupThread();
            mExpirations = new ConcurrentHashMap<Object, Long>();
        }

        mExpirations.put(key, expire);
        ValueWrapper<V> value = mValidCache.get(key);
        if (value != null) {
            value.setExpirationTime(expire);
        }
    }


    private Retriever getRetriever(final ValueWrapper<Object> key) {
        Retriever result = mRetrievers.get(key);
        if (result == null) {
            result = new Retriever(key);

            // Attempt to store the new retriever in the map. If one has been added since we last
            // checked, use that instead.

            Retriever tempResult = mRetrievers.putIfAbsent(key, result);
            if (tempResult != null) {
                result = tempResult;
            }
        }

        return result;
    }

    private void releaseRetriever(final ValueWrapper<Object> key, final Retriever retriever) {
        mRetrievers.remove(key, retriever);
    }


    /**
     * Implement this interface in order for Depot to retrieve objects when
     * needed, often in a thread other than the requester's.
     *
     * @see PerishablesFactory
     */
    public interface Factory<T> {
        /**
         * Create an object that is mapped by the given key. This method must
         * be thread-safe, but simply making it synchronized may severely
         * impact the Depot's support of concurrent activity.
         * <p>
         * Create may abort its operation by throwing an InterruptedException.
         * This forces an invalid object to be used or null if none. If an
         * InterruptedException is thrown, nether the invalid object or null
         * will be cached. Null is cached only if the factory returns it
         * directly.
         *
         * @throws InterruptedException explicitly throwing this exception
         * allows the factory to abort creating an object.
         */
        public T create(Object key) throws InterruptedException;
    }


    /**
     * A special kind of Factory that creates objects that are considered
     * invalid after a specific amount of time has elapsed.
     */
    public interface PerishablesFactory<T> extends Factory<T> {
        /**
         * Returns the maximum amount of time (in milliseconds) that objects
         * from this factory should remain valid. Returning a value less than
         * or equal to zero causes objects to be immediately invalidated.
         */
        public long getValidDuration();
    }


    /**
     * Values returned from the Factories may implement this interface if they
     * manually handle expiration.
     */
    public interface Perishable {
        /**
         * If this Perishable is still valid, but it came from a
         * PerishablesFactory, it may be considered invalid if the valid
         * duration has elapsed.
         */
        public boolean isValid();
    }


    public interface Filter {
        /**
         * Returns true if the given key should be included in an operation,
         * such as invalidation.
         */
        public boolean accept(Object key);
    }


    /**
     * Interface provides basic data structures and additional services in
     * order for the Depot to function. The Kernel must perform operations in a
     * thread-safe manner.
     */
    public interface Kernel<T> {
        /**
         * Returns the map to cache valid Depot entries.
         */
        public Map<ValueWrapper<Object>, ValueWrapper<T>> validCache();

        /**
         * Returns the map to cache invalid Depot entries.
         */
        public Map<ValueWrapper<Object>, ValueWrapper<T>> invalidCache();

        /**
         * Returns the combined size of the valid and invalid cache.
         */
        public int size();

        /**
         * Returns true if both the valid and invalid caches are empty.
         */
        public boolean isEmpty();

        /**
         * Moves all entries in the valid cache to the invalid cache, if the
         * filter accepts it.
         */
        public void invalidateAll(Filter filter);

        /**
         * Moves all entries in the valid cache to the invalid cache.
         */
        public void invalidateAll();

        /**
         * Removes all entries from the valid cache and invalid cache, if the
         * filter accepts it.
         */
        public void removeAll(Filter filter);

        /**
         * Removes all entries from the valid cache and invalid cache.
         */
        public void clear();

    }


    /**
     * Used in place of a user supplied kernel.
     */
    private class SimpleKernel implements Kernel<V> {
        public Map<ValueWrapper<Object>, ValueWrapper<V>> validCache() {
            return null;
        }

        public Map<ValueWrapper<Object>, ValueWrapper<V>> invalidCache() {
            return null;
        }

        public int size() {
            synchronized (mValidCache) {
                return mValidCache.size() + mInvalidCache.size();
            }
        }

        public boolean isEmpty() {
            synchronized (mValidCache) {
                return mValidCache.isEmpty() && mInvalidCache.isEmpty();
            }
        }

        public void invalidateAll(final Filter filter) {
            Validate.notNull(filter, "Filter may not be null when invalidating objects");

            Map<ValueWrapper<Object>, ValueWrapper<V>> valid = mValidCache;
            Map<ValueWrapper<Object>, ValueWrapper<V>> invalid = mInvalidCache;
            synchronized (valid) {
                Iterator<Map.Entry<ValueWrapper<Object>, ValueWrapper<V>>> it = valid.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<ValueWrapper<Object>, ValueWrapper<V>> entry = it.next();
                    ValueWrapper<Object> key = entry.getKey();
                    if (filter.accept(key.getValue())) {
                        it.remove();
                        invalid.put(key, entry.getValue());
                    }
                }
            }
        }

        public void invalidateAll() {
            synchronized (mValidCache) {
                synchronized (mInvalidCache) {
                    mInvalidCache.putAll(mValidCache);
                    mValidCache.clear();
                }
            }
        }

        public void removeAll(final Filter filter) {
            Validate.notNull(filter, "Filter may not be null when removing objects");

            Map<ValueWrapper<Object>, ValueWrapper<V>> valid = mValidCache;
            Map<ValueWrapper<Object>, ValueWrapper<V>> invalid = mInvalidCache;
            synchronized (valid) {
                synchronized (invalid) {
                    Iterator<Map.Entry<ValueWrapper<Object>, ValueWrapper<V>>> it = valid.entrySet().iterator();
                    while (it.hasNext()) {
                        ValueWrapper<Object> key = it.next().getKey();
                        if (filter.accept(key.getValue())) {
                            it.remove();
                        }
                    }

                    it = invalid.entrySet().iterator();
                    while (it.hasNext()) {
                        ValueWrapper<Object> key = it.next().getKey();
                        if (filter.accept(key.getValue())) {
                            it.remove();
                        }
                    }
                }
            }
        }

        public void clear() {
            synchronized (mValidCache) {
                synchronized (mInvalidCache) {
                    mValidCache.clear();
                    mInvalidCache.clear();
                }
            }
        }
    }


    final class Retriever implements Transaction {
        private final ValueWrapper<Object> mKey;
        // All access to mFactory, mValue, or mCancelled must be done while holding the object's lock
        private Factory<? extends V> mFactory;
        private ValueWrapper<V> mValue;
        private boolean mCancelled;

        Retriever(final ValueWrapper<Object> key) {
            mKey = key;
            mValue = new ValueWrapper<V>();
            mFactory = null;
            mCancelled = false;
        }

        /**
         * Returns sentinel value NOTHING if object couldn't be retrieved.
         */
        public synchronized ValueWrapper<? extends V> retrieve(final Factory<? extends V> factory, final long timeout, final boolean priority) {
            if (mFactory != null) {
                // Work in progress to retrieve new value.
                return waitForValue(timeout);
            }

            if ((mFactory = factory) == null) {
                throw new IllegalArgumentException("Factory may not be null when retrieving objects");
            }

            if (mQueue.enqueue(this)) {
                return waitForValue(timeout);
            }

            // No threads available in TQ to retrieve new value.

            if (priority) {
                // Skip the queue, service it in this thread.
                service();
                return mValue;
            }
            else {
                // Unset it so that we'll know we're not working on it.
                mFactory = null;
            }

            mValue = new ValueWrapper<V>();
            return mValue;
        }

        public synchronized void bypassValue(final V value) {
            if (mFactory != null) {
                mValue = ValueWrapper.wrap(value);
                notifyAll();
            }
        }

        synchronized void clearValue() {
            mValue = new ValueWrapper<V>();
        }

        public void service() {
            Factory<? extends V> factory;
            synchronized (this) {
                factory = mFactory;
                if (factory == null) {
                    return;
                }
            }

            int elapsedTime = 0;
            fetch: try {
                Thread t = null;
                String originalName = null;
                if (mSetThreadName) {
                    t = Thread.currentThread();
                    originalName = t.getName();
                    t.setName(originalName + ' ' + mKey.getValue());
                }
                V value = null;
                Throwable error = null;
                try {
                    long startTime = System.currentTimeMillis();
                    value = factory.create(mKey.getValue());
                    elapsedTime = (int) (System.currentTimeMillis() - startTime);
                }
                catch (InterruptedException e) {
                    synchronized (this) {
                        mCancelled = true;
                    }

                    break fetch;
                }
                catch (Throwable e) {
                    error = e;
                }
                finally {
                    if (t != null) {
                        t.setName(originalName);
                    }
                }
                synchronized (this) {
                    mValue = ValueWrapper.wrap(value);
                    if (error != null) {
                        mValue.setError(error);
                        mValue.setRetrievalElapsedTime(-1);
                        throw new RuntimeException(error);
                    }
                    else {
                        mValue.setRetrievalElapsedTime(elapsedTime);
                    }
                    if (factory instanceof PerishablesFactory<?>) {
                        long duration =
                            ((PerishablesFactory<? extends V>)factory).getValidDuration();
                        if (duration <= 0) {
                            mInvalidCache.put(mKey, mValue);
                            mValidCache.remove(mKey);
                            mValue.setValid(false);
                        }
                        else {
                            moveInvalidToValid(mKey, mValue);
                            setExpiration(mKey.getValue(), duration);
                        }
                    }
                    else {
                        moveInvalidToValid(mKey, mValue);
                    }
                    mFactory = null;
                    notifyAll();
                }
            }
            finally {
                done();
            }
        }


        private void moveInvalidToValid(final ValueWrapper<Object> key, final ValueWrapper<V> wrappedValue) {
            ValueWrapper<V> oldInvalid = mInvalidCache.remove(key);
            if (oldInvalid != null) {
                wrappedValue.setArrivalTime(oldInvalid.getArrivalTime());
            }

            ValueWrapper<V> oldValid = mValidCache.put(key, wrappedValue);
            wrappedValue.setValid(true);
            if (!wrappedValue.equals(oldValid)) {
                wrappedValue.setLastUpdateTime();
            }
        }


        public synchronized void cancel() {
            mCancelled = true;
            done();
        }

        private synchronized void done() {
            if (mFactory != null) {
                mFactory = null;
                notifyAll();
            }
        }

        private static final long TIMER_RESOLUTION = 10;

        private ValueWrapper<? extends V> waitForValue(final long inTimeout) {
            if (mReturnInvalidNoWait && mInvalidCache.containsKey(mKey)) {
                return mInvalidCache.get(mKey);
            }

            synchronized (this) {
                if (inTimeout != 0) {
                    long timeout = inTimeout > 0 ? inTimeout : 0;
                    long wakeup = timeout > 0 ? (System.currentTimeMillis() + timeout) : Long.MAX_VALUE;

                    try {
                        boolean timedOut = false;
                        while (!mValue.isValueSet() && !timedOut && !mCancelled) {
                            wait(timeout);

                            if (!mValue.isValueSet()) {
                                long now = System.currentTimeMillis();
                                if (now >= (wakeup - TIMER_RESOLUTION)) {
                                    timedOut = true;
                                    mValue.setRetrievalElapsedTime(-1);
                                }
                                else {
                                    timeout = wakeup - now;
                                }
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        // If we're interrupted, allow processing to continue and return whatever
                        // the current state of the value is (likely NOTHING).
                    }
                }

                return mValue;
            }
        }
    }


    /**
     * This method will calculate the average size of a cache entry
     * by serializing the value objects.  This average does not include
     * the key, which in many cases is duplicated within the object itself.
     * This method will return -1 if the average cannot be calculated, or
     * the size in bytes.  Use this number to estimate cache size as appropriate.
     * Note that only the first 10000 items (or less if fewer) in the cache
     * will be sampled.
     *
     */
    public int calculateAvgPerEntrySize() {

        Object[] keys = mValidCache.keySet().toArray();
        int cumulativeSize = 0;
        int sampled = 0;
        for (int i = 0; i < keys.length && i < 10000; i++) {
            Object k = keys[i];
            ValueWrapper<V> w = mValidCache.get(k);
            if (w == null)
                continue;
            Object v = w.getValue();
            if (v == null)
                continue;
            sampled++;
            try {
                // TODO: this fails with anonymous classes because MemoryUtil
                // checks each instance variable which will include the parent
                // class which will end up in an endless loop possibly.
                cumulativeSize += MemoryUtil.sizeOf(v);
            }
            catch (IllegalAccessException ignore) { }
        }
        return sampled > 0 ? cumulativeSize / sampled : 0;
    }

    public static final class ValueWrapper<T> {
        private final AtomicLong mArrivalTime;
        private final AtomicLong mLastAccessTime = new AtomicLong(0L);
        private final AtomicLong mLastUpdateTime = new AtomicLong(0L);
        private final AtomicLong mExpirationTime = new AtomicLong(0L);
        private final AtomicReference<T> mValue;
        private final AtomicBoolean mValid;
        private final AtomicInteger mRetrievalElapsedTime = new AtomicInteger(0);
        private final AtomicInteger mHits = new AtomicInteger(0);
        private final AtomicLong mVersion = new AtomicLong(1L);
        private final AtomicBoolean mValueSet;
        private Throwable mError = null;
        ValueWrapper() {
            mValue = new AtomicReference<T>(null);
            mArrivalTime = new AtomicLong(System.currentTimeMillis());
            mValid = new AtomicBoolean(true);
            mValueSet = new AtomicBoolean(false);
        }

        ValueWrapper(final T value) {
            this();
            mValue.set(value);
            mValueSet.set(true);
        }

        static <V> ValueWrapper<V> wrap(final V value) {
            return new ValueWrapper<V>(value);
        }

        public boolean isValueSet() {
            return mValueSet.get();
        }

        public T getValue() { return mValue.get(); }
        public synchronized T setValue(final T value) {
            mValueSet.set(true);
            return mValue.getAndSet(value);
        }

        public long getArrivalTime() { return mArrivalTime.get(); }
        void setArrivalTime(final long arrivalTime) { mArrivalTime.set(arrivalTime); }

        public long getLastAccessTime() { return mLastAccessTime.get(); }
        void setLastAccessTime() {
            mLastAccessTime.set(System.currentTimeMillis());
            mHits.getAndIncrement();
        }

        public long getLastUpdateTime() { return mLastUpdateTime.get(); }
        void setLastUpdateTime() {
            mLastUpdateTime.lazySet(System.currentTimeMillis());
            mVersion.getAndIncrement();
        }

        public long getExpirationTime() { return mExpirationTime.get(); }
        void setExpirationTime(final long expirationTime) { mExpirationTime.set(expirationTime); }

        public boolean isValid() { return mValid.get(); }
        void setValid(final boolean valid) { mValid.set(valid); }

        public boolean wasCached() { return mLastAccessTime.get() != 0L; }

        @Override public synchronized int hashCode() {
            return new HashCodeBuilder(37, 113).append(mValue.get()).append(mValueSet.get()).toHashCode();
        }

        @Override public synchronized boolean equals(final Object o) {
            boolean result = false;

            if (o instanceof ValueWrapper<?>) {
                ValueWrapper<?> other = (ValueWrapper<?>) o;
                result = new EqualsBuilder().append(mValue.get(), other.mValue.get())
                                            .append(mValueSet.get(), other.mValueSet.get()).isEquals();
            }

            return result;
        }

        public int getRetrievalElapsedTime() { return mRetrievalElapsedTime.get(); }
        void setRetrievalElapsedTime(final int elapsedTime) { mRetrievalElapsedTime.set(elapsedTime); }

        public int getHits() { return mHits.get(); }

        public long getVersion() { return mVersion.get(); }
        public Throwable getError() { return mError; }
        public void setError(Throwable error) { mError = error; }
        public boolean isError() { return mError != null; }
    }


    private static final class Entry<T> implements Map.Entry<Object, T> {

        private final Object mKey;
        private final AtomicReference<T> mValue;

        Entry(final Object key, final T value) {
            mKey = key;
            mValue = new AtomicReference<T>(value);
        }

        public Object getKey() { return mKey; }
        public T getValue() { return mValue.get(); }
        public T setValue(final T value) { return mValue.getAndSet(value); }

        @Override public boolean equals(final Object o) {
            boolean result = false;
            if (o instanceof Entry<?>) {
                Entry<?> other = (Entry<?>) o;
                result = new EqualsBuilder().append(mKey, other.mKey).append(mValue.get(), other.mValue.get()).isEquals();
            }

            return result;
        }


        @Override public int hashCode() { return mValue != null ? mValue.hashCode() : -1; }
    }

    public interface InvalidationListener {
        /**
         * This event occurs when an entry is invalidated.
         */
        public void invalidated(Object key);
    }


    private void initCleanupThread() {
        if (mCleanupInterval <= 0)
            return;
        mCleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        mCleanupScheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                evict();
            }
        }, CLEANUP_DELAY, mCleanupInterval, TimeUnit.SECONDS);
    }
}
