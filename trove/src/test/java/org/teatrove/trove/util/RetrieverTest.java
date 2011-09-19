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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.teatrove.trove.util.Depot.PerishablesFactory;
import org.teatrove.trove.util.Depot.ValueWrapper;
import org.teatrove.trove.util.DepotTest.DelayedRequestCountingFactory;
import org.teatrove.trove.util.DepotTest.TestImmediateResponseFactory;
import org.teatrove.trove.util.tq.Transaction;
import org.teatrove.trove.util.tq.TransactionQueue;

public class RetrieverTest {
	
	private static final long TEST_TIMEOUT = 60000L;
	private static final long SHORT_TIMEOUT = 5000L;
	private static final int MAX_THREAD_COUNT = 25;
	private static final int MAX_QUEUE_DEPTH = 100;
	
	private static final int TEST_VALID_CACHE_SIZE = 50;
	private static final int TEST_INVALID_CACHE_SIZE = 100;
	private static final String TEST_TQ_NAME = "Test Transaction Queue";
	private static final boolean TEST_USE_LRU = false;
	
	private ThreadPool mTestThreadPool;
	private TransactionQueue mTestTransactionQueue; 
	private TestImmediateResponseFactory mFastFactory;
	private DelayedRequestCountingFactory mSlowFactory;
	
	private Depot<Integer> mTestDepot;
	private Depot<Integer>.Retriever mTestRetriever;
	
	private ValueWrapper<Object> mKey;
	
	@Before public void setUp() {
		mTestThreadPool = new ThreadPool(SimpleKernelTest.class.getName(), MAX_THREAD_COUNT);
		mTestTransactionQueue = new TransactionQueue(mTestThreadPool, TEST_TQ_NAME, MAX_QUEUE_DEPTH, MAX_THREAD_COUNT);
		mFastFactory = new TestImmediateResponseFactory();
		mSlowFactory = new DelayedRequestCountingFactory();
		mTestDepot = new Depot<Integer>(mFastFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_USE_LRU);
		
		mKey = ValueWrapper.wrap((Object) "Denard");
	
		mTestRetriever = mTestDepot.new Retriever(mKey);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testRetrieveNullFactory() {
		mTestRetriever.retrieve(null, TEST_TIMEOUT, false);
	}
	
	@Test public void testRetrieveStickyFactory() throws Exception {
		final CountDownLatch latch = new CountDownLatch(2);
		new Thread() {
			@Override public void run() {
				mTestRetriever.retrieve(mSlowFactory, TEST_TIMEOUT, false);
				latch.countDown();
			}
		}.start();
		
		new Thread() {
			@Override public void run() {
				try {
					Thread.sleep(100L);
				}
				catch (InterruptedException ie) {
					// ignore and continue
				}
				
				mTestRetriever.retrieve(mFastFactory, TEST_TIMEOUT, false);
				latch.countDown();
			}
		}.start();
		
		latch.await();
		Assert.assertEquals("Incorrect request count for slow factory", 1, mSlowFactory.getRequestCount());
		Assert.assertEquals("Incorrect request count for fast factory", 0, mFastFactory.getRequestCount());
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void testRetrieveFullThreadPoolNotPriority() {
		for (int i = 0; i < MAX_QUEUE_DEPTH * 2; i++) {
			mTestTransactionQueue.enqueue(new Transaction() {
				
				@Override public void service() throws Exception {
					Thread.sleep(SHORT_TIMEOUT * 5);
				}
				
				@Override public void cancel() throws Exception {
				}
			});
		}
		
		ValueWrapper<? extends Integer> wrapper = mTestRetriever.retrieve(mFastFactory, TEST_TIMEOUT, false);
		Assert.assertFalse("Unexpected set wrapped value", wrapper.isValueSet());
		Assert.assertEquals("Incorrect request count for fast factory", 0, mFastFactory.getRequestCount());
	}

	@Test(timeout=TEST_TIMEOUT)
	public void testRetrieveFullThreadPoolPriority() {
		for (int i = 0; i < MAX_QUEUE_DEPTH * 2; i++) {
			mTestTransactionQueue.enqueue(new Transaction() {
				
				@Override public void service() throws Exception {
					Thread.sleep(SHORT_TIMEOUT);
				}
				
				@Override public void cancel() throws Exception {
				}
			});
		}
		
		ValueWrapper<? extends Integer> wrapper = mTestRetriever.retrieve(mFastFactory, TEST_TIMEOUT, true);
        Assert.assertTrue("Unexpected unset wrapped value", wrapper.isValueSet());
		Assert.assertEquals("Incorrect request count for fast factory", 1, mFastFactory.getRequestCount());
	}
	
	@Test(timeout=1000L)
	public void testBypassValueWithNoWrapper() throws Exception {
		Integer value = Integer.MAX_VALUE;

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ValueWrapper<? extends Integer>> result = new AtomicReference<ValueWrapper<? extends Integer>>(null);
		new Thread() {
			@Override public void run() {
				result.set(mTestRetriever.retrieve(mSlowFactory, TEST_TIMEOUT, false));
				latch.countDown();
			}
		}.start();

		Thread.sleep(100L);
		mTestRetriever.bypassValue(value);
		latch.await();
		Assert.assertEquals("Incorrect request count for slow factory", 1, mSlowFactory.getRequestCount());
		Assert.assertSame("Incorrect value retrieved", value, result.get().getValue());
	}

	@Test(timeout=1000L)
	public void testBypassValueWithNullNoWrapper() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ValueWrapper<? extends Integer>> result = new AtomicReference<ValueWrapper<? extends Integer>>(null);
		new Thread() {
			@Override public void run() {
				result.set(mTestRetriever.retrieve(mSlowFactory, TEST_TIMEOUT, false));
				latch.countDown();
			}
		}.start();

		Thread.sleep(100L);
		mTestRetriever.bypassValue(null);
		latch.await();
		Assert.assertEquals("Incorrect request count for slow factory", 1, mSlowFactory.getRequestCount());
		Assert.assertNull("Incorrect value retrieved", result.get().getValue());
	}
	
	// NOTE: the common path of service() is tested implicitly by just about every other test; these tests
	// will only cover uncommon paths.
	
	@Test public void testImmediatelyPerishableObject() {
		PerishablesFactory<Integer> factory = new PerishablesFactory<Integer>() {
			@Override public Integer create(final Object key) throws InterruptedException {
				return mFastFactory.create(key);
			}
			
			@Override public long getValidDuration() {
				return 0;
			}
		};
		
		ValueWrapper<? extends Integer> wrappedObject = mTestRetriever.retrieve(factory, TEST_TIMEOUT, false);
		Assert.assertFalse("Unexpected appearance in valid cache of automatically-perished object", mTestDepot.validContainsKey(mKey.getValue()));
		Assert.assertFalse("Unexpectedly valid wrapped value", wrappedObject.isValid());
	}
	
	@Test public void testSoonToExpireObject() throws Exception {
		PerishablesFactory<Integer> factory = new PerishablesFactory<Integer>() {
			@Override public Integer create(final Object key) throws InterruptedException {
				return mFastFactory.create(key);
			}
			
			@Override public long getValidDuration() {
				return 400L;
			}
		};
		
		ValueWrapper<? extends Integer> wrappedObject = mTestRetriever.retrieve(factory, TEST_TIMEOUT, false);
		Assert.assertTrue("Unexpected non-appearance in valid cache of not-yet-perished object", mTestDepot.validContainsKey(mKey.getValue()));
		Assert.assertTrue("Unexpectedly invalid wrapped value", wrappedObject.isValid());
		Thread.sleep(450L);
		mTestDepot.evict();
		Assert.assertFalse("Unexpected re-appearance in valid cache of now-perished object", mTestDepot.validContainsKey(mKey.getValue()));
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void testCancel() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ValueWrapper<? extends Integer>> result = new AtomicReference<ValueWrapper<? extends Integer>>(null);
		new Thread() {
			@Override public void run() {
				result.set(mTestRetriever.retrieve(mSlowFactory, TEST_TIMEOUT, false));
				latch.countDown();
			}
		}.start();

		Thread.sleep(100L);
		mTestRetriever.cancel();
		latch.await();
		Assert.assertFalse("Unexpectedly set wrapped value", result.get().isValueSet());
		Assert.assertEquals("Incorrect request count for slow factory", 1, mSlowFactory.getRequestCount());
		
	}

	@Test public void testSpuriousNotification() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ValueWrapper<? extends Integer>> result = new AtomicReference<ValueWrapper<? extends Integer>>();
		new Thread() {
			@Override public void run() {
				result.set(mTestRetriever.retrieve(mSlowFactory, TEST_TIMEOUT, false));
				latch.countDown();
			}
		}.start();
		
		Thread.sleep(100L);
		synchronized (mTestRetriever) {
			mTestRetriever.notifyAll();
		}
		latch.await();
		
        Assert.assertTrue("Unexpectedlyt unset wrapped value", result.get().isValueSet());
		Assert.assertEquals("Incorrect request count for slow factory", 1, mSlowFactory.getRequestCount());
	}

}

