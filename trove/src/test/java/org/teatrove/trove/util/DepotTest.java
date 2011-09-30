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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.teatrove.trove.util.Depot.Factory;
import org.teatrove.trove.util.Depot.Filter;
import org.teatrove.trove.util.Depot.InvalidationListener;
import org.teatrove.trove.util.Depot.Kernel;
import org.teatrove.trove.util.Depot.Perishable;
import org.teatrove.trove.util.Depot.ValueWrapper;
import org.teatrove.trove.util.tq.TransactionQueue;

public class DepotTest {

	private static final long TEST_TIMEOUT = 60000L;
	private static final long SHORT_TIMEOUT = 200L;
	private static final long TEST_CLEANUP_INTERVAL = 50L;
	private static final int MAX_THREAD_COUNT = 25;
	private static final int MAX_QUEUE_DEPTH = 100000;
	private static final boolean TEST_INVALID_NO_WAIT = false;
	private static final boolean TEST_SYNCHRONIZE = false;
	private static final int TEST_LRU_CACHE_SIZE = 100;
	
	private static final int TEST_VALID_CACHE_SIZE = 50;
	private static final int TEST_INVALID_CACHE_SIZE = 100;
	private static final int TEST_ALTERNATE_NO_VALID_CACHE_SIZE = -100;
	private static final int TEST_NO_VALID_CACHE_SIZE = -1;
	private static final String TEST_TQ_NAME = "Test Transaction Queue";
	private static final boolean TEST_USE_LRU = false;
	
	private static final int EXPECTED_HIT_COUNT_NEW_OBJECT = 0;
	private static final int EXPECTED_HIT_COUNT_CACHED_OBJECT = 1;
	private static final int EXPECTED_VERSION_NEW_OBJECT = 1;
	private static final int EXPECTED_VERSION_CACHED_OBJECT = 1;
	private static final int EXPECTED_VERSION_INVALIDATED_OBJECT = 2;

	private Map<ValueWrapper<Object>, ValueWrapper<Object>> mTestValidCache;
	private Map<ValueWrapper<Object>, ValueWrapper<Object>> mTestInvalidCache;
	private ThreadPool mTestThreadPool;
	private TransactionQueue mTestTransactionQueue; 
	private TestImmediateResponseFactory mTestFactory;
	private TestKernel mTestKernel;
	private Depot<Object> mTestDepot;
	
	private static final Object CONSTANT_VALUE = new Object();
	private static final Factory<Object> CONSTANT_FACTORY = new Factory<Object>() {
		@Override public Object create(final Object key) throws InterruptedException {
			return CONSTANT_VALUE;
		}
	};
	
	private static final long TEST_DELAY = 2000L;
	
	static interface RequestCountingFactory<T> extends Factory<T> {
	    int getRequestCount();
	}
	
	static final class DelayedRequestCountingFactory implements RequestCountingFactory<Integer> {
		private final AtomicInteger mRequestCount = new AtomicInteger();
		
		@Override public Integer create(final Object key) throws InterruptedException {
			int result = mRequestCount.incrementAndGet();
			Thread.sleep(TEST_DELAY);
			return result;
		}
		
		public int getRequestCount() {
			return mRequestCount.get();
		}
	}		
	
	private RequestCountingFactory<Integer> mDelayedRequestCountingFactory;
	
	private static final int SMALL_MULTITHREADED_TEST_THREAD_COUNT = 200;
	
	@Before public void setUp() {
		mTestValidCache = new ConcurrentHashMap<ValueWrapper<Object>, ValueWrapper<Object>>();
		mTestInvalidCache = new ConcurrentHashMap<ValueWrapper<Object>, ValueWrapper<Object>>();
		mTestThreadPool = new ThreadPool(DepotTest.class.getName(), MAX_THREAD_COUNT);
		mTestTransactionQueue = new TransactionQueue(mTestThreadPool, TEST_TQ_NAME, MAX_QUEUE_DEPTH, MAX_THREAD_COUNT);
		mTestFactory = new TestImmediateResponseFactory();
		mTestKernel = new TestKernel();
		mTestDepot = new Depot<Object>(mTestFactory, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);

		mDelayedRequestCountingFactory = new DelayedRequestCountingFactory();

	}
	
	static final class TestImmediateResponseFactory implements RequestCountingFactory<Integer> {
		private final AtomicInteger ID = new AtomicInteger(0);
		
		@Override public Integer create(final Object key) throws InterruptedException {
			return ID.getAndIncrement();
		}
		
		@Override public int getRequestCount() {
			return ID.get();
		}
		
	}
	
	private static final class TestKernel implements Kernel<Object> {
		
		private final Map<String, Integer> mCallCountByMethod;
		private final Map<ValueWrapper<Object>, ValueWrapper<Object>> mValidCache;
		private final Map<ValueWrapper<Object>, ValueWrapper<Object>> mInvalidCache;
		
		TestKernel() {
			mValidCache = new HashMap<ValueWrapper<Object>, ValueWrapper<Object>>();
			mInvalidCache = new HashMap<ValueWrapper<Object>, ValueWrapper<Object>>();
			
			mCallCountByMethod = new HashMap<String, Integer>();
			mCallCountByMethod.put("isEmpty", 0);
			mCallCountByMethod.put("clear", 0);
			mCallCountByMethod.put("invalidCache", 0);
			mCallCountByMethod.put("invalidateAll_filter", 0);
			mCallCountByMethod.put("invalidateAll", 0);
			mCallCountByMethod.put("removeAll_filter", 0);
			mCallCountByMethod.put("size", 0);
			mCallCountByMethod.put("validCache", 0);
		}
		
		private void increment(final String name) {
			mCallCountByMethod.put(name, mCallCountByMethod.get(name) + 1);
		}
		
		int getCallCountForMethod(final String name) {
			return mCallCountByMethod.get(name);
		}

		@Override public boolean isEmpty() {
			increment("isEmpty");
			return mValidCache.isEmpty() && mInvalidCache.isEmpty();
		}

		@Override public void clear() {
			increment("clear");
			mValidCache.clear();
			mInvalidCache.clear();
		}

		@Override public Map<ValueWrapper<Object>, ValueWrapper<Object>> invalidCache() {
			increment("invalidCache");
			return mInvalidCache;
		}

		@Override public void invalidateAll(final Filter filter) {
			increment("invalidateAll_filter");
			Iterator<Map.Entry<ValueWrapper<Object>, ValueWrapper<Object>>> i = mValidCache.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<ValueWrapper<Object>, ValueWrapper<Object>> entry = i.next();
				if (filter.accept(entry.getKey().getValue())) {
					mInvalidCache.put(entry.getKey(), entry.getValue());
					i.remove();
				}
			}
			
		}

		@Override public void invalidateAll() {
			increment("invalidateAll");
			mInvalidCache.putAll(mValidCache);
			mValidCache.clear();
		}

		@Override public void removeAll(final Filter filter) {
			increment("removeAll_filter");
            Iterator<Map.Entry<ValueWrapper<Object>, ValueWrapper<Object>>> i = mValidCache.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<ValueWrapper<Object>, ValueWrapper<Object>> entry = i.next();
				if (filter.accept(entry.getKey().getValue())) {
					i.remove();
				}
			}
		}

		@Override public int size() {
			increment("size");
			return mValidCache.size() + mInvalidCache.size();
		}

		@Override
		public Map<ValueWrapper<Object>, ValueWrapper<Object>> validCache() {
			increment("validCache");
			return mValidCache;
		}
	}
	
	private static final class TestInvalidationListener implements InvalidationListener {

		private int mInvalidObjectCount = 0;
		
		int getInvalidationCalls() {
			return mInvalidObjectCount;
		}
		
		@Override public void invalidated(final Object key) {
			mInvalidObjectCount++;
		}
		
	}
	
	// Constructor tests
	
	@Test public void test8ArgConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  		  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE, TEST_CLEANUP_INTERVAL);
		
		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test8ArgConstructorNullValidCacheSynchronized() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, true, TEST_CLEANUP_INTERVAL);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test8ArgConstructorNullInvalidCacheSynchronized() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, true, TEST_CLEANUP_INTERVAL);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test8ArgConstructorNullValidCache() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, false, TEST_CLEANUP_INTERVAL);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test8ArgConstructorNullInvalidCache() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, false, TEST_CLEANUP_INTERVAL);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test8ArgConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, null,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE, TEST_CLEANUP_INTERVAL);
	}
	
	@Test public void test7ArgConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test7ArgConstructorNullValidCacheSynchronized() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, true);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test7ArgConstructorNullInvalidCacheSynchronized() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, true);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test7ArgConstructorNullValidCache() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, false);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test7ArgConstructorNullInvalidCache() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, false);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test7ArgConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, null,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE);
	}
	
	@Test public void test7ArgSizedConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_USE_LRU, TEST_CLEANUP_INTERVAL);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test7ArgSizedConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, null,
				  TEST_TIMEOUT, TEST_USE_LRU, TEST_CLEANUP_INTERVAL);
	}
	
	@Test public void test7ArgSizedInvalidNoWaitConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_USE_LRU);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test7ArgSizedInvalidNoWaitConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, null,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_USE_LRU);
	}
	
	@Test public void test6ArgConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test6ArgConstructorNullValidCache() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test6ArgConstructorNullInvalidCache() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test6ArgConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, null,
				  TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}
	
	@Test public void test6ArgSizedConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_USE_LRU);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test6ArgSizedConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, null,
				  TEST_TIMEOUT, TEST_USE_LRU);
	}
	
	@Test public void test5ArgConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestValidCache, mTestInvalidCache, mTestTransactionQueue, TEST_TIMEOUT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test5ArgConstructorNullValidCache() {
		new Depot<Object>(mTestFactory, null, mTestInvalidCache, mTestTransactionQueue, TEST_TIMEOUT);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test5ArgConstructorNullInvalidCache() {
		new Depot<Object>(mTestFactory, mTestValidCache, null, mTestTransactionQueue, TEST_TIMEOUT);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test5ArgConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, null, TEST_TIMEOUT);
	}
	
	@Test public void test5ArgSizedConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, TEST_VALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test5ArgSizedConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, null, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}
	
	@Test public void test5ArgKernelConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test5ArgKernelConstructorNullKernel() {
		new Depot<Object>(mTestFactory, null, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test5ArgKernelConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestKernel, null, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
	}
	
	@Test public void test4ArgSizedConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, TEST_VALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test4ArgSizedConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, null, TEST_TIMEOUT);
	}
	
	@Test public void test4ArgKernelConstructorNullFactory() {
		Depot<Object> testDepot = new Depot<Object>(null, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT);

		Assert.assertNull("Unexpected non-null default factory", testDepot.getDefaultFactory());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test4ArgKernelConstructorNullKernel() {
		new Depot<Object>(mTestFactory, null, mTestTransactionQueue, TEST_TIMEOUT);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test4ArgKernelConstructorNullTransactionQueue() {
		new Depot<Object>(mTestFactory, mTestKernel, null, TEST_TIMEOUT);
	}
	
	@Test public void test8ArgConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE, TEST_CLEANUP_INTERVAL);
		assertEmptyDepot(testDepot);
	}

    @SuppressWarnings("unchecked")
	@Test public void test8ArgConstructorLRUCaches() {
		LRUCache testValidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		LRUCache testInvalidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, testValidLRU, testInvalidLRU, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE, TEST_CLEANUP_INTERVAL);
		
		Assert.assertTrue("Depot not found in valid LRU listener list", testValidLRU.removeListener(testDepot));
		Assert.assertFalse("Depot unexpectedly found in valid LRU listener list", testInvalidLRU.removeListener(testDepot));
	}

	@Test public void test7ArgConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE);
		assertEmptyDepot(testDepot);
	}

	@SuppressWarnings("unchecked")
	@Test public void test7ArgConstructorLRUCaches() {
		LRUCache testValidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		LRUCache testInvalidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, testValidLRU, testInvalidLRU, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, TEST_SYNCHRONIZE);
		
		Assert.assertTrue("Depot not found in valid LRU listener list", testValidLRU.removeListener(testDepot));
		Assert.assertFalse("Depot unexpectedly found in valid LRU listener list", testInvalidLRU.removeListener(testDepot));
	}
	
	@Test public void test7ArgSizedConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, false, TEST_CLEANUP_INTERVAL);
		assertEmptyDepot(testDepot);
	}

	@Test public void test7ArgSizedConstructorLRUCaches() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, true, TEST_CLEANUP_INTERVAL);
		assertEmptyDepot(testDepot);
	}

	@Test public void test7ArgSizedInvalidNoWaitConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, false);
		assertEmptyDepot(testDepot);
	}

	@Test public void test7ArgSizedInvalidNoWaitConstructorLRUCaches() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT, true);
		assertEmptyDepot(testDepot);
	}

	@Test public void test7ArgSizedConstructorNoCache() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_NO_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, false, TEST_CLEANUP_INTERVAL);
		assertEmptyDepot(testDepot);
		Object key = new Object();
		Object value = new Object();
		testDepot.put(key, value);
		Object result = testDepot.get(key);
		Assert.assertEquals("Unexpected use of cache", 1, mTestFactory.getRequestCount());
		Assert.assertNotSame("Unexpected retrieval of stored object with cache disabled", value, result);
	}

	@Test public void test7ArgSizedConstructorNoCacheAlternateSpecification() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_ALTERNATE_NO_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, false, TEST_CLEANUP_INTERVAL);
		assertEmptyDepot(testDepot);
		Object key = new Object();
		Object value = new Object();
		testDepot.put(key, value);
		Object result = testDepot.get(key);
		Assert.assertEquals("Unexpected use of cache", 1, mTestFactory.getRequestCount());
		Assert.assertNotSame("Unexpected retrieval of stored object with cache disabled", value, result);
	}

	@Test public void test6ArgConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		assertEmptyDepot(testDepot);
	}

    @SuppressWarnings("unchecked")
	@Test public void test6ArgConstructorLRUCaches() {
		LRUCache testValidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		LRUCache testInvalidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, testValidLRU, testInvalidLRU, mTestTransactionQueue,
				  					TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Depot not found in valid LRU listener list", testValidLRU.removeListener(testDepot));
		Assert.assertFalse("Depot unexpectedly found in valid LRU listener list", testInvalidLRU.removeListener(testDepot));
	}

	@Test public void test6ArgSizedConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, false);
		assertEmptyDepot(testDepot);
	}

	@Test public void test6ArgSizedConstructorLRUCaches() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue,
				  					TEST_TIMEOUT, true);
		assertEmptyDepot(testDepot);
	}

	@Test public void test5ArgConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, mTestValidCache, mTestInvalidCache, mTestTransactionQueue, TEST_TIMEOUT);
		assertEmptyDepot(testDepot);
	}

	@Test public void test5ArgSizedConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		assertEmptyDepot(testDepot);
	}

	@Test public void test5ArgKernelConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		assertEmptyDepot(testDepot);
		Assert.assertEquals("isEmpty failed to call through to kernel", 1, mTestKernel.getCallCountForMethod("isEmpty"));
	}

	@Test public void test4ArgSizedConstructor() {
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT);
		assertEmptyDepot(testDepot);
	}

	private void assertEmptyDepot(final Depot<Object> testDepot) {
		Assert.assertEquals("Incorrect factory set in constructor", mTestFactory, testDepot.getDefaultFactory());
		assertCacheState(testDepot, 0, 0);
		Assert.assertTrue("Unexpected non-empty Depot at creation", testDepot.isEmpty());
	}

    @SuppressWarnings("unchecked")
	@Test public void test5ArgConstructorLRUCaches() {
		LRUCache testValidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		LRUCache testInvalidLRU = new LRUCache(TEST_LRU_CACHE_SIZE);
		Depot<Object> testDepot = new Depot<Object>(mTestFactory, testValidLRU, testInvalidLRU, mTestTransactionQueue, TEST_TIMEOUT);
		
		Assert.assertTrue("Depot not found in valid LRU listener list", testValidLRU.removeListener(testDepot));
		Assert.assertFalse("Depot unexpectedly found in valid LRU listener list", testInvalidLRU.removeListener(testDepot));
	}
	
	@Test public void testInvalidNoWaitWhenFalse() {
		Object key = new Object();
		Object invalidValue = new Object();
		mTestDepot.put(key, invalidValue);
		mTestDepot.invalidate(key);
		Object result = mTestDepot.get(key);
		
		Assert.assertNotNull("Failed to get a non-null object from Depot", result);
		Assert.assertNotSame("Incorrectly got cached version from Depot when invalidNoWait is false", invalidValue, result);
		Assert.assertEquals("Incorrect constructed object count", 1, mTestFactory.getRequestCount());
	}

	@Test public void testInvalidNoWaitWhenTrue() {
		Object key = new Object();
		Object invalidValue = new Object();
		mTestDepot.setReturnInvalidNoWait(true);
		mTestDepot.put(key, invalidValue);
		mTestDepot.invalidate(key);
		Object result = mTestDepot.get(key);
		
		Assert.assertNotNull("Failed to get a non-null object from Depot", result);
		Assert.assertSame("Incorrectly got a new version from Depot when invalidNoWait is true", invalidValue, result);
		Assert.assertEquals("Incorrectly constructed an object unneccessarily", 0, mTestFactory.getRequestCount());
	}
	
	@Test(timeout=TEST_DELAY / 2)
	public void testZeroTimeout() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		Object result = mTestDepot.get(mDelayedRequestCountingFactory, key, 0L);
		Assert.assertSame("Unexpected got a new object with zero timeout and an item in the cache", value, result);
	}
	
	@Test public void testInvalidNoWaitNoEffectIfNoObjectWhenTrue() {
		Object key = new Object();
		mTestDepot.setReturnInvalidNoWait(true);
		Object result = mTestDepot.get(key);
		
		Assert.assertNotNull("Failed to get a non-null object from Depot", result);
		Assert.assertEquals("Failed to construct an object", 1, mTestFactory.getRequestCount());
	}
	
	@Test public void testToString() {
		Assert.assertEquals("Incorrect string returned from toString()", "Depot " + TEST_TQ_NAME, mTestDepot.toString());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddNullInvalidationListener() {
		mTestDepot.addInvalidationListener(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testRemoveNullInvalidationListener() {
		mTestDepot.removeInvalidationListener(null);
	}
	
	@Test public void testInvalidationListeners() {
		Object key = new Object();
		Object invalidValue = new Object();
		TestInvalidationListener listener = new TestInvalidationListener();
		
		mTestDepot.addInvalidationListener(listener);
		mTestDepot.put(key, invalidValue);
		mTestDepot.invalidate(key);
		
		Assert.assertEquals("Failed to register invalidation", 1, listener.getInvalidationCalls());
		
		mTestDepot.remove(listener);
		mTestDepot.invalidate(key);

		Assert.assertEquals("Failed to remove invalidation listener", 1, listener.getInvalidationCalls());

		// Make sure that removing an already-removed listener doesn't hurt anything
		mTestDepot.remove(listener);
		mTestDepot.invalidate(key);

		Assert.assertEquals("Failed to remove invalidation listener", 1, listener.getInvalidationCalls());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testExpireNullEvent() {
		mTestDepot.expireEvent(null);
	}
	
	@Test public void testExpireEvent() {
		Object key = new Object();
		Object expiredValue = new Object();
		TestInvalidationListener listener = new TestInvalidationListener();
		LRUCache testLRUCache = new LRUCache(TEST_LRU_CACHE_SIZE);
		
		mTestDepot.addInvalidationListener(listener);
		mTestDepot.put(key, expiredValue);
		mTestDepot.expireEvent(testLRUCache.new Entry(key, expiredValue));
		
		Assert.assertEquals("Failed to receieve notification of expiration", 1, listener.getInvalidationCalls());
	}
	
	@Test public void testSize() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		Assert.assertEquals("Incorrect size detected", testSize, mTestDepot.size());
		Assert.assertEquals("Failed to pass size() to kernel", 1, mTestKernel.getCallCountForMethod("size"));
	}

	@Test public void testIsEmpty() {
		Assert.assertTrue("Unexpected non-empty depot to start", mTestDepot.isEmpty());
		
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		Assert.assertFalse("Unexpected empty depot after adding objects", mTestDepot.isEmpty());
		
		mTestDepot.clear();
		
		Assert.assertTrue("Unepxected non-empty depot after clear called", mTestDepot.isEmpty());
		Assert.assertEquals("Failed to pass isEmpty() to kernel", 3, mTestKernel.getCallCountForMethod("isEmpty"));
	}

	@Test public void testGetTQStatistics() {
		Assert.assertNotNull("Unexpected null response to TQStat request", mTestDepot.getTQStatistics());
	}

	@Test public void testValidAndInvalidSize() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
			Object invalidObjectKey = new Object();
			mTestDepot.get(invalidObjectKey);
			mTestDepot.invalidate(invalidObjectKey);

			invalidObjectKey = new Object();
			mTestDepot.get(invalidObjectKey);
			mTestDepot.invalidate(invalidObjectKey);
		}
		
		Assert.assertEquals("Incorrect size detected", testSize * 3, mTestDepot.size());
		Assert.assertEquals("Incorrect valid size detected", testSize, mTestDepot.validSize());
		Assert.assertEquals("Incorrect invalid size detected", testSize * 2, mTestDepot.invalidSize());
		Assert.assertEquals("Failed to pass size() to kernel", 1, mTestKernel.getCallCountForMethod("size"));
	}

	@Test public void testCacheCounters() {
		Object key1 = new Object();
		Object key2 = new Object();
		Object key3 = new Object();
		
		int hits = 0;
		int misses = 0;
		
		assertEmptyDepot(mTestDepot);
		
		mTestDepot.get(key1);
		misses++;
		assertCacheState(mTestDepot, hits, misses);
		
		mTestDepot.get(key2);
		misses++;
		assertCacheState(mTestDepot, hits, misses);
		
		mTestDepot.get(key1);
		hits++;
		assertCacheState(mTestDepot, hits, misses);
		
		mTestDepot.reset();
		hits = 0;
		misses = 0;
		assertCacheState(mTestDepot, hits, misses);

		mTestDepot.get(key2);
		hits++;
		assertCacheState(mTestDepot, hits, misses);

		mTestDepot.get(key3);
		misses++;
		assertCacheState(mTestDepot, hits, misses);
	}
	
	private void assertCacheState(final Depot<Object> depot, final long hits, final long misses) {
		Assert.assertEquals("Incorrect number of cache hits", hits, depot.getCacheHits());
		Assert.assertEquals("Incorrect number of cache misses", misses, depot.getCacheMisses());
		Assert.assertEquals("Incorrect number of cache gets", hits + misses, depot.getCacheGets());
	}
	
	@Test public void testInvalidContainsKey() {
		Object key = new Object();
		
		Assert.assertFalse("Valid contains key before it's added", mTestDepot.validContainsKey(key));
		mTestDepot.get(key);
		Assert.assertTrue("Valid failed to contain key after it's added", mTestDepot.validContainsKey(key));
		mTestDepot.invalidate(key);
		Assert.assertFalse("Valid contains key after it's invalidated", mTestDepot.validContainsKey(key));
	}
	
	// Applications depend upon being able to use null as both a key and a value in a Depot.
	@Test public void testInvalidContainsNullKey() {
		Object key = null;
		
		Assert.assertFalse("Valid contains key before it's added", mTestDepot.validContainsKey(key));
		mTestDepot.get(key);
		Assert.assertTrue("Valid failed to contain key after it's added", mTestDepot.validContainsKey(key));
		mTestDepot.invalidate(key);
		Assert.assertFalse("Valid contains key after it's invalidated", mTestDepot.validContainsKey(key));
	}
	
	@Test public void testGetObjectFromCache() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		Assert.assertSame("Retrieved incorrect object from cache", value, mTestDepot.get(key));
	}

	@Test public void testGetObjectFromCacheNullKey() {
		Object key = null;
		Object value = new Object();
		
		mTestDepot.put(key, value);
		Assert.assertSame("Retrieved incorrect object from cache", value, mTestDepot.get(key));
	}

	@Test(timeout=TEST_TIMEOUT)
	public void testGetObjectFromCacheWithTimeout() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		Factory<Object> slowFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				Thread.sleep(TEST_TIMEOUT);
				return new Object();
			}
		};

		Depot<Object> slowDepot = new Depot<Object>(slowFactory, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT);
		
		Assert.assertSame("Retrieved incorrect object from cache", value, slowDepot.get(key, SHORT_TIMEOUT));
		
		// Try it using the original depot and specifying the factory; it should behave the same.
		Assert.assertSame("Retrieved incorrect object from cache", value, mTestDepot.get(slowFactory, key, SHORT_TIMEOUT));
	}

	@Test public void testGetObjectAlternateFactory() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		Assert.assertSame("Retrieved incorrect object from factory", CONSTANT_VALUE, mTestDepot.get(CONSTANT_FACTORY, key));
	}
	
	@Test public void testGetObjectNullResult() {
		Factory<Object> nullFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				return null;
			}
		};

		Assert.assertNull("Successfully retrieved null from factory", mTestDepot.get(nullFactory, new Object()));
		
	}
	
	@Test public void testGetObjectFactoryTimedOut() {
		Factory<Object> interruptingFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				throw new InterruptedException();
			}
		};

		Assert.assertNull("Successfully retrieved null from factory due to interruption", mTestDepot.get(interruptingFactory, new Object()));
	}
	
	@Test public void testGetWrappedObjectFromCache() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		ValueWrapper<? extends Object> wrapper = mTestDepot.getWrappedValue(key);
		Assert.assertSame("Retrieved incorrect object from cache", value, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_CACHED_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_CACHED_OBJECT, wrapper.getVersion());
	}

	@Test public void testGetWrappedObjectFromCacheNullKey() {
		Object key = null;
		Object value = new Object();
		
		mTestDepot.put(key, value);
		ValueWrapper<? extends Object> wrapper = mTestDepot.getWrappedValue(key);
		Assert.assertSame("Retrieved incorrect object from cache", value, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_CACHED_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_CACHED_OBJECT, wrapper.getVersion());
	}

	@Test(timeout=TEST_TIMEOUT)
	public void testGetWrappedObjectFromCacheWithTimeout() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		Factory<Object> slowFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				Thread.sleep(TEST_TIMEOUT);
				return new Object();
			}
		};

		Depot<Object> slowDepot = new Depot<Object>(slowFactory, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT);
		
		ValueWrapper<? extends Object> wrapper = slowDepot.getWrappedValue(key, SHORT_TIMEOUT);
		Assert.assertSame("Retrieved incorrect object from cache", value, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_NEW_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_NEW_OBJECT, wrapper.getVersion());
		
		// Try it using the original depot and specifying the factory; it should behave the same.
		wrapper = mTestDepot.getWrappedValue(slowFactory, key, SHORT_TIMEOUT);
		Assert.assertSame("Retrieved incorrect object from cache", value, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_NEW_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_NEW_OBJECT, wrapper.getVersion());
	}

	@Test public void testWrappedGetObjectAlternateFactory() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		ValueWrapper<? extends Object> wrapper = mTestDepot.getWrappedValue(CONSTANT_FACTORY, key);
		Assert.assertSame("Retrieved incorrect object from cache", CONSTANT_VALUE, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_NEW_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_INVALIDATED_OBJECT, wrapper.getVersion());
	}
	
	@Test public void testGetWrappedObjectNullResult() {
		Factory<Object> nullFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				return null;
			}
		};

		ValueWrapper<? extends Object> wrapper = mTestDepot.getWrappedValue(nullFactory, new Object());
		Assert.assertSame("Retrieved incorrect object from cache", null, wrapper.getValue());
		Assert.assertEquals("Incorrect number of hits on wrapped object", EXPECTED_HIT_COUNT_NEW_OBJECT, wrapper.getHits());
		Assert.assertEquals("Incorrect version of wrapped object", EXPECTED_VERSION_INVALIDATED_OBJECT, wrapper.getVersion());
		
	}
	
	@Test public void testGetWrappedObjectFactoryTimedOut() {
		Factory<Object> interruptingFactory = new Factory<Object>() {
			@Override public Object create(final Object key) throws InterruptedException {
				throw new InterruptedException();
			}
		};

		ValueWrapper<? extends Object> wrapper = mTestDepot.getWrappedValue(interruptingFactory, new Object());
		Assert.assertNull("Retrieved unexpected non-null wrapper object after timeout", wrapper);
	}
	
	@Test public void testGetPerishableObjectStillValid() {
		Perishable validObject = new Perishable() {
			@Override public boolean isValid() {
				return true;
			}
		};
		
		Object key = new Object();
		mTestDepot.put(key, validObject);
		
		Object result = mTestDepot.get(key);
		Assert.assertSame("Failed to get cached object for valid Perishable value", validObject, result);
	}

	@Test public void testGetPerishableObjectNoLongerValid() {
		Perishable validObject = new Perishable() {
			@Override public boolean isValid() {
				return false;
			}
		};
		
		Object key = new Object();
		mTestDepot.put(key, validObject);
		Object result = mTestDepot.get(CONSTANT_FACTORY, key);
		
		Assert.assertSame("Retrieved incorrect object from factory", CONSTANT_VALUE, result);
	}
	
	@Test public void testGetExpiredObject() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.setExpiration(key, -1L);

		Object result = mTestDepot.get(CONSTANT_FACTORY, key);
		
		Assert.assertSame("Retrieved incorrect object from factory", CONSTANT_VALUE, result);
	}

	@Test public void testGetUnexpiredObject() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.setExpiration(new Object(), -1L);

		Object result = mTestDepot.get(CONSTANT_FACTORY, key);
		
		Assert.assertSame("Retrieved incorrect object from cache", value, result);
	}

	@Test public void testGetNotYetExpiredObject() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.setExpiration(key, TEST_TIMEOUT);

		Object result = mTestDepot.get(CONSTANT_FACTORY, key);
		
		Assert.assertSame("Retrieved incorrect object from cache", value, result);
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void testGetMultipleThreads() throws Exception {
		
		final Object key = new Object();
		
		ValueWrapperTest.runMultipleThreadTest(new Callable<Void>() {
			@Override public Void call() throws Exception {
				Assert.assertNotNull("Unexpected null value retrieved from depot", mTestDepot.get(mDelayedRequestCountingFactory, key));
				return null;
			}
		});
		
		Assert.assertEquals("Too many requests to the factory in multi-threaded retriever test", 1, mDelayedRequestCountingFactory.getRequestCount());
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void testGetRetrieverProvidesExpiredItems() throws Exception {
		final Perishable expiredObject = new Perishable() {
			@Override public boolean isValid() {
				return false;
			}
		};
		
		final RequestCountingFactory<Object> expiredRQF = new RequestCountingFactory<Object>() {
			
			private final AtomicInteger mRequestCount = new AtomicInteger(0);
			
			@Override public Object create(final Object key) throws InterruptedException {
				mRequestCount.incrementAndGet();
				return expiredObject;
			}
			
			@Override public int getRequestCount() {
				return mRequestCount.get();
			}
		}; 

		final Object key = new Object();
		
		runSmallMultipleThreadTest(new Callable<Void>() {

			@Override public Void call() throws Exception {
				Assert.assertNotNull("Unexpected null result from depot retriever", mTestDepot.get(expiredRQF, key));
				return null;
			}
		});
		
		// Because the lock on the retriever is released while the transaction queue is waiting
		// to service the request, any number of threads can end up with cache misses, not just the
		// first, so the best we can say is that the number of requests should be greater than 1,
		// assuming that the number of threads in the test is greater than the number of cores.
		
		Assert.assertTrue("Failed to re-fetch perished object", expiredRQF.getRequestCount() > 1);
	}
	
	static void runSmallMultipleThreadTest(final Callable<?> test) throws Exception {
		final CountDownLatch startingGate = new CountDownLatch(SMALL_MULTITHREADED_TEST_THREAD_COUNT);
		final CountDownLatch closingGate = new CountDownLatch(SMALL_MULTITHREADED_TEST_THREAD_COUNT);
		ExecutorService executor = Executors.newFixedThreadPool(SMALL_MULTITHREADED_TEST_THREAD_COUNT);
		
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		
		for (int i = 0; i < SMALL_MULTITHREADED_TEST_THREAD_COUNT; i++) {
			futures.add(executor.submit(new Callable<Void>() {
					@Override public Void call() throws Exception {
						try {
							startingGate.countDown();
							startingGate.await();
							
							test.call();
						}
						finally {
							closingGate.countDown();
						}
						
						return null;
					}
				}));
		}
		
		closingGate.await();
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		
		for (Future<Void> future : futures) {
			future.get();
		}
	}
	
	@Test public void testEvictNoExpirations() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		mTestDepot.evict();
		
		Assert.assertEquals("Objects evicted despite lack of expirations", testSize, mTestDepot.validSize());
	}
	
	@Test public void testEvictAllFutureExpirations() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			mTestDepot.get(key);
			mTestDepot.setExpiration(key, TEST_TIMEOUT);
		}
		
		mTestDepot.evict();
		
		Assert.assertEquals("Objects evicted despite future expirations", testSize, mTestDepot.validSize());
	}

	@Test public void testEvictSomeCurrentExpirations() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			mTestDepot.get(key);
			mTestDepot.setExpiration(key, TEST_TIMEOUT);
			
			key = new Object();
			mTestDepot.get(key);
			mTestDepot.setExpiration(key, -1);
		}
		
		mTestDepot.evict();
		
		Assert.assertEquals("Incorrect number of objects evicted", testSize, mTestDepot.validSize());
		Assert.assertEquals("Incorrect number of objects evicted", testSize, mTestDepot.invalidSize());
	}
	
	@Test public void testInvalidateAll() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		mTestDepot.invalidateAll();
		Assert.assertEquals("Valid depot should be empty after invalidateAll", 0, mTestDepot.validSize());
		Assert.assertEquals("Invalid depot size incorrect after invaldiateAll", testSize, mTestDepot.invalidSize());
		Assert.assertEquals("Failed to pass invalidateAll to kernel", 1, mTestKernel.getCallCountForMethod("invalidateAll"));
	}

	@Test public void testFilteredInvalidateAll() {
		final Set<Object> invalidKeys = new HashSet<Object>();

		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
			
			Object invalidKey = new Object();
			mTestDepot.get(invalidKey);
			invalidKeys.add(invalidKey);
		}
		
		Filter filter = new Filter() {
			@Override public boolean accept(final Object key) {
				return invalidKeys.contains(key);
			}
		};
		
		mTestDepot.invalidateAll(filter);
		Assert.assertEquals("Valid depot size incorrect after invalidateAll", testSize, mTestDepot.validSize());
		Assert.assertEquals("Invalid depot size incorrect after invaldiateAll", testSize, mTestDepot.invalidSize());
		Assert.assertEquals("Failed to pass invalidateAll to kernel", 1, mTestKernel.getCallCountForMethod("invalidateAll_filter"));
	}
	
	@Test public void testPutIntoEmptyDepot() {
		Object key = new Object();
		Object value = new Object();
		
		Object result = mTestDepot.put(key, value);
		Assert.assertNull("Unexpected non-null result from putting into an empty depot", result);
		Assert.assertTrue("Valid failed to contain key after put", mTestDepot.validContainsKey(key));
		Assert.assertFalse("Invalid incorrectly contained key after put", mTestKernel.mInvalidCache.containsKey(key));
		Assert.assertSame("Incorrect result retrieved from Depot after put", value, mTestDepot.get(key));
	}

	@Test public void testPutReplaceValid() {
		Object key = new Object();
		Object firstValue = new Object();
		
		mTestDepot.put(key, firstValue);
		
		Object secondValue = new Object();
		Object result = mTestDepot.put(key, secondValue);
		
		Assert.assertSame("Incorrect result from putting into a depot with an entry in valid", firstValue, result);
		Assert.assertTrue("Valid failed to contain key after put", mTestDepot.validContainsKey(key));
		Assert.assertFalse("Invalid incorrectly contained key after put", mTestKernel.mInvalidCache.containsKey(key));
		Assert.assertSame("Incorrect result retrieved from Depot after put", secondValue, mTestDepot.get(key));
	}

	@Test public void testPutReplaceInvalid() {
		Object key = new Object();
		Object firstValue = new Object();
		
		mTestDepot.put(key, firstValue);
		mTestDepot.invalidate(key);
		
		Object secondValue = new Object();
		doInvalidPutTest(key, firstValue, secondValue);
	}

	private void doInvalidPutTest(Object key, Object firstValue,
			Object secondValue) {
		Object result = mTestDepot.put(key, secondValue);
		
		Assert.assertSame("Incorrect result from putting into a depot with an entry in invalid", firstValue, result);
		Assert.assertTrue("Valid failed to contain key after put", mTestDepot.validContainsKey(key));
		Assert.assertFalse("Invalid incorrectly contained key after put", mTestKernel.mInvalidCache.containsKey(key));
		Assert.assertSame("Incorrect result retrieved from Depot after put", secondValue, mTestDepot.get(key));
	}
	
	@Test public void testPutNullKey() {
		Object key = null;
		Object firstValue = new Object();
		
		mTestDepot.put(key, firstValue);
		mTestDepot.invalidate(key);
		
		Object secondValue = new Object();
		doInvalidPutTest(key, firstValue, secondValue);
	}
	
	@Test public void testPutNullFirstValue() {
		Object key = new Object();
		Object firstValue = null;
		
		mTestDepot.put(key, firstValue);
		mTestDepot.invalidate(key);
		
		Object secondValue = new Object();
		doInvalidPutTest(key, firstValue, secondValue);
	}
	
	@Test public void testPutNullSecondValue() {
		Object key = new Object();
		Object firstValue = new Object();
		
		mTestDepot.put(key, firstValue);
		mTestDepot.invalidate(key);
		
		Object secondValue = null;
		doInvalidPutTest(key, firstValue, secondValue);
	}

	@Test public void testRemoveObjectNotInDepot() {
		Object key = new Object();
		Object value = new Object();
		mTestDepot.put(key, value);
		
		Object result = mTestDepot.remove(new Object());
		
		Assert.assertNull("Unexpected non-null result from removing an object that wasn't there", result);
	}

	@Test public void testRemoveObjectFromValid() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		
		doRemoveTest(key, value);
	}

	@Test public void testRemoveReplaceInvalid() {
		Object key = new Object();
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		doRemoveTest(key, value);
	}
	
	@Test public void testRemoveNullKey() {
		Object key = null;
		Object value = new Object();
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		doRemoveTest(key, value);
	}

	private void doRemoveTest(Object key, Object value) {
		Object result = mTestDepot.remove(key);
		
		Assert.assertSame("Incorrect result from removing an item from a depot", value, result);
		Assert.assertFalse("Valid incorrectly contained key after remove", mTestDepot.validContainsKey(key));
		Assert.assertFalse("Invalid incorrectly contained key after remove", mTestKernel.mInvalidCache.containsKey(key));
	}
	
	@Test public void testRemoveNullValue() {
		Object key = new Object();
		Object value = null;
		
		mTestDepot.put(key, value);
		mTestDepot.invalidate(key);
		
		doRemoveTest(key, value);
	}

	@Test public void testClear() {
		int testSize = new Random().nextInt(100) + 1; 
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		mTestDepot.clear();
		Assert.assertEquals("Valid depot should be empty after clear", 0, mTestDepot.validSize());
		Assert.assertEquals("Invalid depot should be empty after clear", 0, mTestDepot.invalidSize());
		Assert.assertEquals("Failed to pass clear to kernel", 1, mTestKernel.getCallCountForMethod("clear"));
	}

	@Test public void testFilteredRemoveAll() {
		final Set<Object> removedKeys = new HashSet<Object>();

		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
			
			Object removedKey = new Object();
			mTestDepot.get(removedKey);
			removedKeys.add(removedKey);
		}
		
		Filter filter = new Filter() {
			@Override public boolean accept(final Object key) {
				return removedKeys.contains(key);
			}
		};
		
		mTestDepot.removeAll(filter);
		Assert.assertEquals("Valid depot size incorrect after removeAll", testSize, mTestDepot.validSize());
		Assert.assertEquals("Invalid depot size incorrect after removeAll", 0, mTestDepot.invalidSize());
		Assert.assertEquals("Failed to pass removeAll to kernel", 1, mTestKernel.getCallCountForMethod("removeAll_filter"));
	}

	@Test public void testContainsKeyNotContained() {
		mTestDepot.put(new Object(), new Object());
		Assert.assertFalse("Unexpectedly found non-existent key in depot", mTestDepot.containsKey(new Object()));
	}
	
	@Test public void testContainsKeyValid() {
		Object key = new Object();
		mTestDepot.get(key);
		Assert.assertTrue("Failed to find key in valid depot", mTestDepot.containsKey(key));
	}
	
	@Test public void testContainsKeyInvalid() {
		Object key = new Object();
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		Assert.assertTrue("Failed to find key in invalid depot", mTestDepot.containsKey(key));
	}
	
	@Test public void testContainsNullKey() {
		Object key = null;
		mTestDepot.get(key);
		Assert.assertTrue("Failed to find null key in depot", mTestDepot.containsKey(key));
	}

	@Test public void testEmptyDepotDoesNotContainNullKey() {
		Assert.assertFalse("Unexpectedly found null key in empty depot", mTestDepot.containsKey(null));
	}

	@Test public void testContainsValueNotContained() {
		mTestDepot.put(new Object(), new Object());
		Assert.assertFalse("Unexpectedly found non-existent value in depot", mTestDepot.containsValue(new Object()));
	}
	
	@Test public void testContainsValueValid() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		Assert.assertTrue("Failed to find value in valid depot", mTestDepot.containsValue(value));
	}
	
	@Test public void testContainsValueInvalid() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		Assert.assertTrue("Failed to find value in invalid depot", mTestDepot.containsValue(value));
	}
	
	@Test public void testContainsNullValue() {
		mTestDepot.put(new Object(), null);
		Assert.assertTrue("Failed to find null value in depot", mTestDepot.containsValue(null));
	}

	@Test public void testEmptyDepotDoesNotContainNullValue() {
		Assert.assertFalse("Unexpectedly found null value in empty depot", mTestDepot.containsValue(null));
	}
	
	@Test public void testPeekNotFound() {
		mTestDepot.get(new Object());
		Assert.assertNull("Unexpected found non-existent object", mTestDepot.peek(new Object()));
	}
	
	@Test public void testPeekValid() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		Assert.assertSame("Failed to find object in valid depot", value, mTestDepot.peek(key));
	}
	
	@Test public void testPeekInvalid() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		Assert.assertSame("Failed to find object in invalid depot", value, mTestDepot.peek(key));
	}
	
	@Test public void testPeekNullKey() {
		Object value = mTestDepot.get(null);
		Assert.assertSame("Failed to find object in valid depot with null key", value, mTestDepot.peek(null));
	}
	
	@Test public void testPeekNullValue() {
		Object key = new Object();
		mTestDepot.put(key, null);
		Assert.assertNull("Failed to get 'null' value from depot via peek", mTestDepot.peek(key));
	}
	
	@Test public void testEntrySetEmptyDepot() {
		Assert.assertTrue("Unexpected non-empty entry set in empty depot", mTestDepot.entrySet().isEmpty());
	}
	
	@Test public void testEntrySetOneValidEntry() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		
        Set<Map.Entry<Object, Object>> result = mTestDepot.entrySet();
		Assert.assertEquals("Incorrect entrySet size", 1, result.size());
		Map.Entry<?, ?> entry = result.iterator().next();
		Assert.assertSame("Incorrect key in entrySet entry", key, entry.getKey());
		Assert.assertSame("Incorrect value in entrySet entry", value, entry.getValue());
	}

	@Test public void testEntrySetOneInvalidEntry() {
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		
		Set<Map.Entry<Object, Object>> result = mTestDepot.entrySet();
		Assert.assertEquals("Incorrect entrySet size", 1, result.size());
		Map.Entry<?, ?> entry = result.iterator().next();
		Assert.assertSame("Incorrect key in entrySet entry", key, entry.getKey());
		Assert.assertSame("Incorrect value in entrySet entry", value, entry.getValue());
	}

	@Test public void testEntrySetOneValidAndOneInvalidEntry() {
		Map<Object, Object> expectedMap = new HashMap<Object, Object>();
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		expectedMap.put(key, value);
		
		key = new Object();
		value = mTestDepot.get(key);
		expectedMap.put(key, value);
		
        Set<Map.Entry<Object, Object>> result = mTestDepot.entrySet();
		Assert.assertEquals("Incorrect entrySet size", 2, result.size());
		for (Map.Entry<?, ?> entry : result) {
			Assert.assertSame("Incorrect mapping in entrySet entry", expectedMap.get(entry.getKey()), entry.getValue());
		}
	}

	@Test public void testEntrySetOneValidAndOneNullKey() {
		Map<Object, Object> expectedMap = new HashMap<Object, Object>();
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		expectedMap.put(key, value);
		
		key = null;
		value = mTestDepot.get(key);
		expectedMap.put(key, value);
		
        Set<Map.Entry<Object, Object>> result = mTestDepot.entrySet();
		Assert.assertEquals("Incorrect entrySet size", 2, result.size());
		for (Map.Entry<?, ?> entry : result) {
			Assert.assertSame("Incorrect mapping in entrySet entry", expectedMap.get(entry.getKey()), entry.getValue());
		}
	}

	@Test public void testEntrySetOneValidAndOneNullValue() {
		Map<Object, Object> expectedMap = new HashMap<Object, Object>();
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		expectedMap.put(key, value);
		
		key = new Object();
		value = null;
		mTestDepot.put(key, value);
		expectedMap.put(key, value);
		
        Set<Map.Entry<Object, Object>> result = mTestDepot.entrySet();
		Assert.assertEquals("Incorrect entrySet size", 2, result.size());
		for (Map.Entry<?, ?> entry : result) {
			Assert.assertSame("Incorrect mapping in entrySet entry", expectedMap.get(entry.getKey()), entry.getValue());
		}
	}

	@Test public void testKeySetEmptyDepot() {
		Assert.assertTrue("Unexpected non-empty key set in empty depot", mTestDepot.keySet().isEmpty());
	}
	
	@Test public void testKeySetOneValidKey() {
		Object key = new Object();
		mTestDepot.get(key);
		
		Set<?> result = mTestDepot.keySet();
		Assert.assertEquals("Incorrect keySet size", 1, result.size());
		Object foundKey = result.iterator().next();
		Assert.assertSame("Incorrect key in keySet", key, foundKey);
	}

	@Test public void testKeySetOneInvalidKey() {
		Object key = new Object();
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		
		Set<?> result = mTestDepot.keySet();
		Assert.assertEquals("Incorrect keySet size", 1, result.size());
		Object foundKey = result.iterator().next();
		Assert.assertSame("Incorrect key in keySet", key, foundKey);
	}

	@Test public void testKeySetOneValidAndOneInvalidKey() {
		Set<Object> expectedSet = new HashSet<Object>();
		Object key = new Object();
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		expectedSet.add(key);
		
		key = new Object();
		mTestDepot.get(key);
		expectedSet.add(key);
		
		Set<?> result = mTestDepot.keySet();
		Assert.assertEquals("Incorrect keySet size", 2, result.size());
		for (Object foundKey : result) {
			Assert.assertTrue("Incorrect key in keySet", expectedSet.contains(foundKey));
		}
	}

	@Test public void testKeySetOneValidAndOneNullKey() {
		Set<Object> expectedSet = new HashSet<Object>();
		Object key = new Object();
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		expectedSet.add(key);
		
		key = null;
		mTestDepot.get(key);
		expectedSet.add(key);
		
		Set<?> result = mTestDepot.keySet();
		Assert.assertEquals("Incorrect keySet size", 2, result.size());
		for (Object foundKey : result) {
			Assert.assertTrue("Incorrect key in keySet", expectedSet.contains(foundKey));
		}
	}
	
	// In the following tests, I've made the assertion that equals() for a Depot should behave 
	// according to the specification for Map, which is to say that two Depots are equal if they
	// contain the same contents.  No attempt is made to determine that the objects would behave
	// identically under future stimuli (e.g., a call to get() which invokes the factory).
	
	// Note that in order to match the behaviour of the other Map interfaces (values(), keySet(), etc.),
	// the contents of both the valid and invalid caches are used and are not distinguished from one
	// another.
	
	@Test public void testEqualsNull() {
		Assert.assertFalse("Unexpected equality with null", mTestDepot.equals(null));
	}
	
	@Test public void testEmptyDepotEqualsSelf() {
		Assert.assertTrue("Unexpected failure to equal self", mTestDepot.equals(mTestDepot));
	}

	@Test public void testNonEmptyDepotEqualsSelf() {
		mTestDepot.get(new Object());
		Assert.assertTrue("Unexpected failure to equal self", mTestDepot.equals(mTestDepot));
	}
	
	@Test public void testInvalidDepotEqualsSelf() {
		Object key = new Object();
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		Assert.assertTrue("Unexpected failure to equal self", mTestDepot.equals(mTestDepot));
	}

	@Test public void testTwoDepotsWithSameContentsEqualEachOther() {
		Object key = new Object();
		Object value = new Object();
		
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		mTestDepot.put(key, value);
		secondDepot.put(key, value);
		Assert.assertTrue("Unexpected failure to equal depot with identical contents", mTestDepot.equals(secondDepot));
		Assert.assertTrue("Unexpected failure to equal depot with identical contents", secondDepot.equals(mTestDepot));
	}

	@Test public void testEqualsTwoDepotsWithSameValidContents() {
		Object key = new Object();
		Object value = new Object();
		
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		mTestDepot.get(key);
		mTestDepot.invalidate(key);
		
		key = new Object();
		secondDepot.get(key);
		secondDepot.invalidate(key);
		
		key = new Object();
		mTestDepot.put(key, value);
		secondDepot.put(key, value);
		Assert.assertFalse("Unexpectedly equal to depot with identical valid contents but different invalid contents", mTestDepot.equals(secondDepot));
		Assert.assertFalse("Unexpectedly equal to depot with identical valid contents but different invalid contents", secondDepot.equals(mTestDepot));
	}

	@Test public void testEqualsValidInvalidSymmetry() {
        Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
        
		Object key = new Object();
		Object value = mTestDepot.get(key);
		mTestDepot.invalidate(key);
		
		secondDepot.put(key, value);
		
		Assert.assertTrue("Unexpected failure to equal depot with valid and invalid swapped", mTestDepot.equals(secondDepot));
		Assert.assertTrue("Unexpected failure to equal depot with valid and invalid swapped", secondDepot.equals(mTestDepot));
	}
	
	@Test public void testEqualsDifferentFactory() {
		Depot<Object> secondDepot = new Depot<Object>(CONSTANT_FACTORY, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Unexpected failure to equal depot with different factory", mTestDepot.equals(secondDepot));
	}

	@Test public void testEqualsDifferentKernel() {
		Kernel<Object> testKernel = new Kernel<Object>() {
			
			
			@Override public Map<ValueWrapper<Object>, ValueWrapper<Object>> validCache() {
				return Collections.emptyMap();
			}
			
			@Override public int size() {
				return 0;
			}
			
			@Override public void removeAll(final Filter filter) {
				return;
			}
			
			@Override public boolean isEmpty() {
				return true;
			}
			
			@Override public void invalidateAll() {
				return;
			}
			
			@Override public void invalidateAll(final Filter filter) {
				return;
			}
			
			@Override public Map<ValueWrapper<Object>, ValueWrapper<Object>> invalidCache() {
				return Collections.emptyMap();
			}
			
			@Override public void clear() {
				return;
			}
		};
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, testKernel, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Unexpected failure to equal depot with different kernel", mTestDepot.equals(secondDepot));
	}

	@Test public void testEqualsDifferentTransactionQueue() {
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, mTestKernel, new TransactionQueue(mTestThreadPool, MAX_QUEUE_DEPTH - 1, MAX_QUEUE_DEPTH - 1), TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Unexpected failure to equal depot with different TQ", mTestDepot.equals(secondDepot));
	}

	@Test public void testEqualsDifferentTimeout() {
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, mTestKernel, mTestTransactionQueue, -1, TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Unexpected failure to equal depot with different timeout", mTestDepot.equals(secondDepot));
	}

	@Test public void testEqualsDifferentInvalidNoWait() {
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, mTestKernel, mTestTransactionQueue, TEST_TIMEOUT, !TEST_INVALID_NO_WAIT);
		
		Assert.assertTrue("Unexpected failure to equal depot with different invalid no wait", mTestDepot.equals(secondDepot));
	}
	
	@Test public void testHashCodeConstant() {
		Assert.assertEquals("Unexpected change in hash code between two successive invocations", mTestDepot.hashCode(), mTestDepot.hashCode());
	}
	
	@Test public void testHashCodeSameContents() {
		Object key = new Object();
		Object value = new Object();
		
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		mTestDepot.put(key, value);
		secondDepot.put(key, value);
		
		Assert.assertEquals("Unexpected change in hash code between objects with same contents", mTestDepot.hashCode(), secondDepot.hashCode());
	}
	
	@Test public void testHashCodeValidInvalidSwap() {
		Object key = new Object();
		Object value = new Object();
		
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		mTestDepot.put(key, value);
		secondDepot.put(key, value);
		secondDepot.invalidate(key);
		
		Assert.assertEquals("Unexpected change in hash code between objects with same contents and valid/invalid swap", mTestDepot.hashCode(), secondDepot.hashCode());
	}

	@Test public void testHashCodeDifferentInvalid() {
		Object key = new Object();
		Object value = new Object();
		
		Depot<Object> secondDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_INVALID_NO_WAIT);
		
		mTestDepot.put(key, value);
		secondDepot.put(key, value);
		
		key = new Object();
		secondDepot.get(key);
		secondDepot.invalidate(key);
		
		Assert.assertFalse("Unexpected identical hash code between objects with same valid contents and different invalid contents", mTestDepot.hashCode() == secondDepot.hashCode());
	}
	
	// The NPE is required by the Map specification: http://download.oracle.com/javase/6/docs/api/java/util/Map.html
	@Test(expected=NullPointerException.class)
	public void testPutAllNullMap() {
		mTestDepot.putAll(null);
	}
	
	@Test public void testPutAllEmptyMap() {
		mTestDepot.get(new Object());
		mTestDepot.putAll(Collections.emptyMap());
		Assert.assertEquals("Incorrect size of depot after adding nothing", 1, mTestDepot.size());
	}
	
	@Test public void testPutAllNoOverlap() {
		mTestDepot.get(new Object());
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(new Object(), new Object());
		map.put(new Object(), new Object());
		mTestDepot.putAll(map);
		Assert.assertEquals("Incorrect size of depot after adding two nonoverlapping pairs", 3, mTestDepot.size());
	}

	@Test public void testPutAllWithOverlap() {
		Object key = new Object();
		mTestDepot.get(key);
		Object newValue = new Object();
		
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(key, newValue);
		mTestDepot.putAll(map);
		
		Assert.assertEquals("Incorrect size of depot after adding an overlapping pair", 1, mTestDepot.size());
		Assert.assertSame("Incorrect value retrieved from depot after putAll", newValue, mTestDepot.get(key));
	}

	@Test public void testValuesEmptyDepot() {
		Collection<?> values = mTestDepot.values();
		Assert.assertTrue("Unexpected non-empty collection retrieved from empty depot", values.isEmpty());
	}
	
	@Test public void testSingleValidValue() {
		Object value = mTestDepot.get(new Object());
		Collection<?> values = mTestDepot.values();
		Assert.assertEquals("Incorrect size of values collection with one valid object", 1, values.size());
		Assert.assertSame("Incorrect value in values collection", value, values.iterator().next());
	}

	@Test public void testSingleInvalidValue() {
		Object value = mTestDepot.get(new Object());
		mTestDepot.invalidateAll();
		Collection<?> values = mTestDepot.values();
		Assert.assertEquals("Incorrect size of values collection with one invalid object", 1, values.size());
		Assert.assertSame("Incorrect value in values collection", value, values.iterator().next());
	}

	@Test public void testValidAndInvalidValues() {
		Set<Object> expected = new HashSet<Object>();
		Object value = mTestDepot.get(new Object());
		expected.add(value);
		mTestDepot.invalidateAll();
		
		value = mTestDepot.get(new Object());
		expected.add(value);
		
		Collection<?> values = mTestDepot.values();
		Assert.assertEquals("Incorrect size of values collection with one valid and one invalid object", 2, values.size());
		for (Object found : values) {
			Assert.assertTrue("Incorrect value in values collection", expected.contains(found));
		}
	}
	
	@Test public void testMultipleIdenticalValues() {
		Object canonicalValue = new Object();
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			mTestDepot.put(new Object(), canonicalValue);
		}
		
		Collection<?> values = mTestDepot.values();
		Assert.assertEquals("Incorrect size of values collection with many identical values", testSize, values.size());
		for (Object found : values) {
			Assert.assertSame("Incorrect value in values collection", canonicalValue, found);
		}
	}
	
	@Test public void testAverageEntrySizeWhenEmpty() {
		Assert.assertEquals("Unexpected non-zero average entry size with no elements", 0, mTestDepot.calculateAvgPerEntrySize());
	}
	
    // There appears to be an 8-byte difference when using Eclipse or TC to run the tests.  While
    // that makes no sense, the fact is that this calculation is an estimate anyway and 8
    // bytes is certainly close enough.
    
	//@Test 
	public void testAverageEntrySizeOneBigItem() {
		Random rng = new Random();
		Integer [] value = new Integer[rng.nextInt(50000) + 1];
		for (int i = 0; i < value.length; i++) {
			value[i] = rng.nextInt();
		}
		
		mTestDepot.put(new Object(), value);
		Assert.assertEquals("Incorrect average size with one element", 10 * value.length + 116, mTestDepot.calculateAvgPerEntrySize(), 8);
	}

	//@Test 
	public void testAverageEntrySizeDateFields() {
		final Random rng = new Random();
		int testSize = rng.nextInt(50000) + 75; // A minimum of 75 is necessary to amortise the serialisation
		// overhead enough to make it negligible.
		for (int i = 0; i < testSize; i++) {
			mTestDepot.put(new Object(), new TestObject(rng));
		}
		
		Assert.assertEquals("Incorrect average size with date fields", 129, mTestDepot.calculateAvgPerEntrySize(), 8);
	}
	
	private static class TestObject implements Serializable {
        private static final long serialVersionUID = 1L;

        Date mDate;
        java.sql.Date mSQLDate;
        
        @SuppressWarnings("deprecation")
        public TestObject(Random rng) {
            mDate = new Date(rng.nextInt(10000), rng.nextInt(12), rng.nextInt(28), rng.nextInt(24), rng.nextInt(60), rng.nextInt(60));
            mSQLDate = new java.sql.Date(mDate.getTime());
        }
        
        private void writeObject(final ObjectOutputStream out) throws IOException {
            out.writeObject(mDate);
            out.writeObject(mSQLDate);
        }
	}
}
