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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.teatrove.trove.util.Depot.Filter;
import org.teatrove.trove.util.DepotTest.TestImmediateResponseFactory;
import org.teatrove.trove.util.tq.TransactionQueue;

public class SimpleKernelTest {
	
	private static final long TEST_TIMEOUT = 60000L;
	private static final int MAX_THREAD_COUNT = 25;
	private static final int MAX_QUEUE_DEPTH = 100000;
	
	private static final int TEST_VALID_CACHE_SIZE = 50;
	private static final int TEST_INVALID_CACHE_SIZE = 100;
	private static final String TEST_TQ_NAME = "Test Transaction Queue";
	private static final boolean TEST_USE_LRU = false;
	
	private ThreadPool mTestThreadPool;
	private TransactionQueue mTestTransactionQueue; 
	private TestImmediateResponseFactory mTestFactory;
	private Depot<Object> mTestDepot;
	
	@Before public void setUp() {
		mTestThreadPool = new ThreadPool(SimpleKernelTest.class.getName(), MAX_THREAD_COUNT);
		mTestTransactionQueue = new TransactionQueue(mTestThreadPool, TEST_TQ_NAME, MAX_QUEUE_DEPTH, MAX_THREAD_COUNT);
		mTestFactory = new TestImmediateResponseFactory();
		mTestDepot = new Depot<Object>(mTestFactory, TEST_VALID_CACHE_SIZE, TEST_INVALID_CACHE_SIZE, mTestTransactionQueue, TEST_TIMEOUT, TEST_USE_LRU);
	}
	
	@Test public void testIsEmptyEmptyDepot() {
		Assert.assertTrue("Unexpectedly non-empty new depot", mTestDepot.isEmpty());
	}

	@Test public void testIsEmptyValidDepot() {
		mTestDepot.get(new Object());
		Assert.assertFalse("Unexpectedly empty depot with item in valid cache", mTestDepot.isEmpty());
	}
	
	@Test public void testIsEmptyInvalidDepot() {
		mTestDepot.get(new Object());
		mTestDepot.invalidateAll();
		Assert.assertFalse("Unexpectedly empty depot with item in invalid cache", mTestDepot.isEmpty());
	}

	@Test public void testSizeEmptyDepot() {
		Assert.assertEquals("Unexpected non-zero size of empty depot", 0, mTestDepot.size());
	}

	@Test public void testSize() {
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			mTestDepot.get(key);
			mTestDepot.invalidate(key);
			mTestDepot.get(new Object());
		}
		
		Assert.assertEquals("Incorrect size of non-empty depot", 2 * testSize, mTestDepot.size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidateAllNullFilter() {
		mTestDepot.get(new Object());
		mTestDepot.invalidateAll(null);
	}
	
	@Test public void testFilteredInvalidateAll() {
		final Set<Object> filtered = new HashSet<Object>();
		
		Filter testFilter = new Filter() {
			@Override public boolean accept(final Object key) {
				return filtered.contains(key);
			}
		};
		
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			mTestDepot.get(key);
			filtered.add(key);
			mTestDepot.get(new Object());
		}
		
		mTestDepot.invalidateAll(testFilter);
		
		Assert.assertEquals("Incorrect size of valid depot after invalidation", testSize, mTestDepot.validSize());
		Assert.assertEquals("Incorrect size of invalid depot after invalidation", testSize, mTestDepot.invalidSize());
	}

	@Test public void testUnfilteredInvalidateAll() {
		
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			mTestDepot.get(new Object());
		}
		
		mTestDepot.invalidateAll();
		
		Assert.assertEquals("Incorrect size of valid depot after invalidation", 0, mTestDepot.validSize());
		Assert.assertEquals("Incorrect size of invalid depot after invalidation", testSize, mTestDepot.invalidSize());
	}

    @Test(expected=IllegalArgumentException.class)
    public void testRemoveAllNullFilter() {
        mTestDepot.get(new Object());
        mTestDepot.removeAll(null);
    }
    
	@Test public void testFilteredRemoveAll() {
		final Set<Object> filtered = new HashSet<Object>();
		
		Filter testFilter = new Filter() {
			@Override public boolean accept(final Object key) {
				return filtered.contains(key);
			}
		};
		
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			// one in the valid cache
			mTestDepot.get(key);
			filtered.add(key);
			
			// one in the invalid cache
			key = new Object();
			mTestDepot.get(key);
			mTestDepot.invalidate(key);
			filtered.add(key);
			
			// one unfiltered in the valid cache
			mTestDepot.get(new Object());
			
			// one unfiltered in the invalid cache
			key = new Object();
			mTestDepot.get(key);
			mTestDepot.invalidate(key);
		}
		
		mTestDepot.removeAll(testFilter);
		
		Assert.assertEquals("Incorrect size of valid depot after removal", testSize, mTestDepot.validSize());
		Assert.assertEquals("Incorrect size of invalid depot after removal", testSize, mTestDepot.invalidSize());
	}

	@Test public void testClear() {
		int testSize = new Random().nextInt(100) + 1;
		for (int i = 0; i < testSize; i++) {
			Object key = new Object();
			// one in the valid cache
			mTestDepot.get(key);
			
			// one in the invalid cache
			key = new Object();
			mTestDepot.get(key);
			mTestDepot.invalidate(key);
		}
		
		mTestDepot.clear();
		
		Assert.assertEquals("Incorrect size after removal", 0, mTestDepot.size());
	}
	
}
