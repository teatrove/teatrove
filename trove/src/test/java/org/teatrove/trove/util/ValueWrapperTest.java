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
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.teatrove.trove.util.Depot.ValueWrapper;

public class ValueWrapperTest {
	
	private static final double TIME_EPSILON = 200.0d;
	private static final int THREAD_COUNT = 500;
	private static final int OPERATION_COUNT = 50000;
	
	private Object mTestObject;
	private ValueWrapper<Object> mTestWrapper;
	
	@Before public void setUp() {
		mTestObject = new Object();
		mTestWrapper = new ValueWrapper<Object>(mTestObject);
	}
	
	
	@Test public void testConstructor() {
		long time = System.currentTimeMillis();
		ValueWrapper<Object> wrapper = new ValueWrapper<Object>(mTestObject);
		
		Assert.assertTrue("Incorrect valid state after proper construction", wrapper.isValid());
		Assert.assertEquals("Incorrect object retuned from wrapper", mTestObject, wrapper.getValue());
		Assert.assertEquals("Incorrect time recorded for arrival time", (double) time, wrapper.getArrivalTime(), TIME_EPSILON);
		Assert.assertEquals("Incorrect default time for last access", 0, wrapper.getLastAccessTime());
		Assert.assertEquals("Incorrect default time for last update", 0, wrapper.getLastUpdateTime());
		Assert.assertEquals("Incorrect default expiration time", 0, wrapper.getExpirationTime());
		Assert.assertEquals("Incorrect default number of hits", 0, wrapper.getHits());
		Assert.assertEquals("Incorrect default version", 1L, wrapper.getVersion());
		Assert.assertEquals("Incorrect default retrieval time", 0, wrapper.getRetrievalElapsedTime());
		Assert.assertFalse("Incorrectly marked as cached by default", wrapper.wasCached());
	}
	
	@Test public void testSetValueSingleThread() {
		Object newValue = new Object();
		Object oldValue = mTestWrapper.setValue(newValue);
		
		Assert.assertEquals("Incorrect old object retrieved from setValue() call", mTestObject, oldValue);
		Assert.assertEquals("Failed to set value", newValue, mTestWrapper.getValue());
	}

	@Test public void testSetValueNullAllowed() {
		Object newValue = null;
		Object oldValue = mTestWrapper.setValue(newValue);
		
		Assert.assertEquals("Incorrect old object retrieved from setValue() call", mTestObject, oldValue);
		Assert.assertEquals("Failed to set value", newValue, mTestWrapper.getValue());
	}
	
	@Test public void testSetValueMultipleThreads() throws Exception {
		final ValueWrapper<Object> testTarget = mTestWrapper;
		final AtomicBoolean foundNonNull = new AtomicBoolean(false);
		mTestWrapper.setValue(null);
		
		runMultipleThreadTest(new Callable<Void>() {
			@Override public Void call() throws Exception {
				Object oldValue = testTarget.setValue(new Object());
				
				if (foundNonNull.get()) {
					Assert.assertNotNull("Found null value after a non-null had been observed.", oldValue);
				}
				else if (oldValue != null) {
					foundNonNull.set(true);
				}
				
				return null;
			}
		});
		
		Assert.assertTrue("Eventually found non-null value", foundNonNull.get());
		Assert.assertNotNull("Found null value after all threads have completed", mTestWrapper.getValue());
	}

	@Test public void testSetArrivalTimeSingleThread() {
		long newTime = Double.doubleToLongBits(Math.random());
		mTestWrapper.setArrivalTime(newTime);
		
		Assert.assertEquals("Failed to set arrival time", newTime, mTestWrapper.getArrivalTime());
	}

	@Test public void testSetRetrievalElapsedTimeSingleThread() {
		int newTime = new Random().nextInt();
		mTestWrapper.setRetrievalElapsedTime(newTime);
		
		Assert.assertEquals("Failed to set elapsed time", newTime, mTestWrapper.getRetrievalElapsedTime());
	}

	@Test public void testNoArgSetLastAccessTimeSingleThread() {
		long newTime = System.currentTimeMillis();
		mTestWrapper.setLastAccessTime();
		
		Assert.assertEquals("Failed to set access time", (double) newTime, mTestWrapper.getLastAccessTime(), TIME_EPSILON);
		Assert.assertEquals("Failed to increment hit counter", 1, mTestWrapper.getHits());
	}
	
	@Test public void testSetLastUpdateTimeSingleThread() {
		long newTime = System.currentTimeMillis();
		mTestWrapper.setLastUpdateTime();
		
		Assert.assertEquals("Failed to set update time", (double) newTime, mTestWrapper.getLastUpdateTime(), TIME_EPSILON);
		Assert.assertEquals("Failed to increment version", 2, mTestWrapper.getVersion());
	}

	@Test public void testSetExpirationTimeSingleThread() {
		long newTime = Double.doubleToLongBits(Math.random());
		mTestWrapper.setExpirationTime(newTime);
		
		Assert.assertEquals("Failed to set expiration time", newTime, mTestWrapper.getExpirationTime());
	}

	@Test public void testSetValidSingleThread() {
		mTestWrapper.setValid(false);
		Assert.assertFalse("Failed to set validity to false", mTestWrapper.isValid());
		
		mTestWrapper.setValid(true);
		Assert.assertTrue("Failed to set validity to true", mTestWrapper.isValid());
	}

	@Test public void testWasCached() {
		mTestWrapper.setLastAccessTime();
		Assert.assertTrue("Incorrect cache state with a non-zero last access time", mTestWrapper.wasCached());
	}
	
	@Test public void testHashCodeValidObject() {
		Assert.assertEquals("Failed to match duplicate object's hash code", ValueWrapper.wrap(mTestObject).hashCode(), mTestWrapper.hashCode());
	}
	
	@Test public void testHashCodeNullReferent() {
		mTestWrapper.setValue(null);
		Assert.assertEquals("Incorrect hash code when wrapped object is null", ValueWrapper.<Object>wrap(null).hashCode(), mTestWrapper.hashCode());
	}
	
	@Test public void testEqualsNull() {
		Assert.assertFalse("Unexpectedly equal to null", mTestWrapper.equals(null));
	}

	@Test public void testEqualsSelf() {
		Assert.assertTrue("Unexpectedly not equal to self", mTestWrapper.equals(mTestWrapper));
	}

	// The object wrapper shouldn't equal the underlying object because that breaks the symmetry of equals;
	// calling wrappedObject.equals(object) would return true but object.equals(wrappedObject) would
	// be false.
	
	@Test public void testEqualsWrappedObject() {
		Assert.assertFalse("Unexpectedly equal to wrapped object", mTestWrapper.equals(mTestObject));
	}

	@Test public void testEqualsSeparateWrapperSameObject() {
		ValueWrapper<Object> other = new ValueWrapper<Object>(mTestObject);
		Assert.assertTrue("Unexpectedly not equal to another ValueWrapper for the same object", mTestWrapper.equals(other));
	}

	@Test public void testEqualsSeparateWrapperDifferentObject() {
		ValueWrapper<Object> other = new ValueWrapper<Object>(new Object());
		Assert.assertFalse("Unexpectedly equal to another ValueWrapper for a different object", mTestWrapper.equals(other));
	}

	@Test public void testEqualsSeparateWrapperNullObject() {
		ValueWrapper<Object> other = new ValueWrapper<Object>(null);
		Assert.assertFalse("Unexpectedly equal to another ValueWrapper for null", mTestWrapper.equals(other));
	}

	@Test public void testEqualsTwoWrappersNullObject() {
		mTestWrapper.setValue(null);
		ValueWrapper<Object> other = new ValueWrapper<Object>(null);
		Assert.assertTrue("Unexpectedly not equal to another ValueWrapper when both wrap null", mTestWrapper.equals(other));
	}


	static void runMultipleThreadTest(final Callable<?> test) throws Exception {
		final CountDownLatch closingGate = new CountDownLatch(OPERATION_COUNT);
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		
		for (int i = 0; i < OPERATION_COUNT; i++) {
			futures.add(executor.submit(new Callable<Void>() {
					@Override public Void call() throws Exception {
						try {
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
}
