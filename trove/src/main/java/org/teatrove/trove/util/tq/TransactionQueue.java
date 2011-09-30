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

package org.teatrove.trove.util.tq;

import java.util.*;
import org.teatrove.trove.util.*;

/**
 * TransactionQueue processes {@link Transaction Transactions} concurrently
 * using threads obtained from a {@link ThreadPool}. When a transaction is
 * enqueued, it goes into a waiting queue, and it is serviced as soon as a
 * thread is available.
 *
 * @author Brian S O'Neill
 */
public class TransactionQueue {
    private ThreadPool mThreadPool;
    private String mName;
    private int mMaxSize;
    private int mMaxThreads;
    private long mIdleTimeout;
    private long mTransactionTimeout;

    private LinkedList mQueue = new LinkedList();
    private int mThreadCount;
    private int mServicingCount;
    private int mThreadId;
    private boolean mSuspended;

    private Worker mWorker = new Worker();

    private Collection mListeners = new LinkedList();
    private Collection mExceptionListeners = new LinkedList();

    // Used to gather time lapse statistics.
    private long mTimeLapseStart;
    private int mPeakQueueSize;
    private int mPeakThreadCount;
    private int mPeakServicingCount;
    private int mTotalEnqueueAttempts;
    private int mTotalEnqueued;
    private int mTotalServiced;
    private int mTotalExpired;
    private int mTotalServiceExceptions;
    private int mTotalUncaughtExceptions;
    private long mTotalQueueDuration;
    private long mTotalServiceDuration;

    public TransactionQueue(ThreadPool tp, int maxSize, int maxThreads) {
        this(tp, "TransactionQueue", maxSize, maxThreads);
    }

    public TransactionQueue(ThreadPool tp, String name,
                            int maxSize, int maxThreads) {
        mThreadPool = tp;
        mName = name;

        setMaximumSize(maxSize);
        setMaximumThreads(maxThreads);

        setIdleTimeout(tp.getIdleTimeout());
        setTransactionTimeout(-1);

        resetStatistics();
    }

    /**
     * Sets the timeout (in milliseconds) for the TransactionQueue to wait
     * inactive before going into an idle state. When the TransactionQueue is
     * idle, there are no internal worker threads. A negative value specifies
     * that the TransactionQueue should never automatically go into idle mode.
     *
     * @see #idle()
     */
    public synchronized void setIdleTimeout(long timeout) {
        mIdleTimeout = timeout;
    }

    /**
     * Returns the timeout (in milliseconds) that the TransactionQueue will
     * wait inactive before going into an idle state. The default value is
     * the same as the ThreadPool's idle timeout.
     *
     * @see #idle()
     * @see ThreadPool#getIdleTimeout
     */
    public synchronized long getIdleTimeout() {
        return mIdleTimeout;
    }

    /**
     * Sets the timeout (in milliseconds) to wait before an enqueued
     * transaction expires. If a worker receives an expired transaction, it
     * is cancelled. A negative timeout specifies that enqueued transactions
     * never expire.
     */
    public synchronized void setTransactionTimeout(long timeout) {
        mTransactionTimeout = timeout;
    }

    /**
     * Returns the timeout (in milliseconds) to wait before an enqueued
     * transaction expires. The default value is -1, indicating that enqueued
     * transactions never expire.
     */
    public synchronized long getTransactionTimeout() {
        return mTransactionTimeout;
    }

    /**
     * Returns the name of this TransactionQueue.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the maximum allowed number of queued transactions.
     */
    public synchronized int getMaximumSize() {
        return mMaxSize;
    }

    /**
     * Setting the max size to zero disables enqueueing.
     */
    public synchronized void setMaximumSize(int max) {
        if (max < 0) {
            throw new IllegalArgumentException
                ("TransactionQueue max size must be positive: " + max);
        }

        mMaxSize = max;
    }

    /**
     * Returns the maximum allowed number of worker threads.
     */
    public synchronized int getMaximumThreads() {
        return mMaxThreads;
    }

    public synchronized void setMaximumThreads(int max) {
        if (max < 1) {
            throw new IllegalArgumentException
                ("TransactionQueue must have at least one thread: " + max);
        }

        mMaxThreads = max;
    }

    /**
     * Enqueues a transaction that will be serviced when a worker is
     * available. If the queue is full or cannot accept new transactions, the
     * transaction is not enqueued, and false is returned.
     *
     * @return true if enqueued, false if queue is full or cannot accept new
     * transactions.
     */
    public synchronized boolean enqueue(Transaction transaction) {
        mTotalEnqueueAttempts++;

        if (transaction == null || mThreadPool.isClosed()) {
            return false;
        }

        int queueSize;
        if ((queueSize = mQueue.size()) >= mMaxSize) {
            if (mListeners.size() > 0) {
                TransactionQueueEvent event =
                    new TransactionQueueEvent(this, transaction);

                Iterator it = mListeners.iterator();
                while (it.hasNext()) {
                    ((TransactionQueueListener)it.next())
                        .transactionQueueFull(event);
                }
            }
            return false;
        }

        if (!mSuspended) {
            if (!ensureWaitingThread()) {
                return false;
            }
        }

        mTotalEnqueued++;

        TransactionQueueEvent event =
            new TransactionQueueEvent(this, transaction);

        mQueue.addLast(event);

        if (++queueSize > mPeakQueueSize) {
            mPeakQueueSize = queueSize;
        }

        notify();

        if (mListeners.size() > 0) {
            Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                ((TransactionQueueListener)it.next())
                    .transactionEnqueued(event);
            }
        }

        return true;
    }

    /**
     * Suspends processing of transactions in the queue until resume is called.
     * If suspend is called on a TransactionQueue that is already suspended,
     * the call has no effect.
     */
    public synchronized void suspend() {
        if (!mSuspended) {
            mQueue.addFirst(null);
            notify();
            mSuspended = true;
        }
    }

    /**
     * Resumes processing of transactions in the queue if suspend was called.
     * If resume is called on a TransactionQueue that is already running, the
     * call has no effect, but true is still returned.
     *
     * @return false if couldn't resume because no threads available from pool.
     */
    public synchronized boolean resume() {
        if (mSuspended) {
            mSuspended = false;
        }
        return ensureWaitingThread();
    }

    /**
     * Make this TransactionQueue go into idle mode and allow it to be
     * reclaimed by the garbage collector if it is no longer used. Any pending
     * transactions will be serviced, and any servicing transactions will
     * finish. If any transactions are added to the TransactionQueue while
     * idle, it will reactivate itself. New TransactionQueues start out in an
     * idle state.
     *
     * @see #setIdleTimeout
     * @see #getIdleTimeout
     */
    public synchronized void idle() {
        mQueue.addLast(null);
        notify();
    }

    public synchronized void addTransactionQueueListener
        (TransactionQueueListener listener) {

        mListeners.add(listener);
    }

    public synchronized void removeTransactionQueueListener
        (TransactionQueueListener listener) {

        mListeners.remove(listener);
    }

    public synchronized void addUncaughtExceptionListener
        (UncaughtExceptionListener listener) {

        mExceptionListeners.add(listener);
    }

    public synchronized void removeUncaughtExceptionListener
        (UncaughtExceptionListener listener) {

        mExceptionListeners.remove(listener);
    }

    /**
     * Returns the number of currently queued transactions.
     */
    public synchronized int getQueueSize() {
        return mQueue.size();
    }

    /**
     * Returns the current amount of worker threads.
     */
    public synchronized int getThreadCount() {
        return mThreadCount;
    }

    /**
     * Returns a snapshot of the statistics on this TransactionQueue.
     */
    public synchronized TransactionQueueData getStatistics() {
        return new TransactionQueueData(this,
                                        mTimeLapseStart,
                                        System.currentTimeMillis(),
                                        mQueue.size(),
                                        mThreadCount,
                                        mServicingCount,
                                        mPeakQueueSize,
                                        mPeakThreadCount,
                                        mPeakServicingCount,
                                        mTotalEnqueueAttempts,
                                        mTotalEnqueued,
                                        mTotalServiced,
                                        mTotalExpired,
                                        mTotalServiceExceptions,
                                        mTotalUncaughtExceptions,
                                        mTotalQueueDuration,
                                        mTotalServiceDuration);
    }

    /**
     * Resets all time lapse statistics.
     */
    public synchronized void resetStatistics() {
        mPeakQueueSize = 0;
        mPeakThreadCount = 0;
        mPeakServicingCount = 0;
        mTotalEnqueueAttempts = 0;
        mTotalEnqueued = 0;
        mTotalServiced = 0;
        mTotalExpired = 0;
        mTotalServiceExceptions = 0;
        mTotalUncaughtExceptions = 0;
        mTotalQueueDuration = 0;
        mTotalServiceDuration = 0;

        mTimeLapseStart = System.currentTimeMillis();
    }

    /**
     * Understands and applies the following integer properties.
     *
     * <ul>
     * <li>max.size - setMaximumSize
     * <li>max.threads - setMaximumThreads
     * <li>timeout.idle - setIdleTimeout
     * <li>timeout.transaction - setTransactionTimeout
     * <li>tune.size - Automatically tunes queue size when "true" and
     *                 transaction timeout set.
     * <li>tune.threads - Automatically tunes maximum thread count.
     * </ul>
     */
    public synchronized void applyProperties(PropertyMap properties) {
        if (properties.containsKey("max.size")) {
            setMaximumSize(properties.getInt("max.size"));
        }

        if (properties.containsKey("max.threads")) {
            setMaximumThreads(properties.getInt("max.threads"));
        }

        if (properties.containsKey("timeout.idle")) {
            setIdleTimeout(properties.getNumber("timeout.idle").longValue());
        }

        if (properties.containsKey("timeout.transaction")) {
            setTransactionTimeout
                (properties.getNumber("timeout.transaction").longValue());
        }

        if ("true".equalsIgnoreCase(properties.getString("tune.size"))) {
            addTransactionQueueListener(new TransactionQueueSizeTuner());
        }

        if ("true".equalsIgnoreCase(properties.getString("tune.threads"))) {
            addTransactionQueueListener(new TransactionQueueThreadTuner());
        }
    }

    synchronized void startThread(boolean canwait)
        throws InterruptedException {

        if (mThreadCount < mMaxThreads) {
            String threadName = getName() + ' ' + (mThreadId++);
            if (canwait) {
                mThreadPool.start(mWorker, threadName);
            }
            else {
                mThreadPool.start(mWorker, 0, threadName);
            }

            if (++mThreadCount > mPeakThreadCount) {
                mPeakThreadCount = mThreadCount;
            }
        }
    }

    /**
     * Returns null when the TransactionQueue should go idle.
     */
    synchronized TransactionQueueEvent nextTransactionEvent()
        throws InterruptedException {

        if (mQueue.isEmpty()) {
            if (mIdleTimeout != 0) {
                if (mIdleTimeout < 0) {
                    wait();
                }
                else {
                    wait(mIdleTimeout);
                }
            }
        }

        if (mQueue.isEmpty()) {
            return null;
        }

        return (TransactionQueueEvent)mQueue.removeFirst();
    }

    synchronized TransactionQueueEvent transactionDequeued
        (TransactionQueueEvent event) {

        if (++mServicingCount > mPeakServicingCount) {
            mPeakServicingCount = mServicingCount;
        }

        TransactionQueueEvent deqEvent = new TransactionQueueEvent(event);

        mTotalQueueDuration +=
            (deqEvent.getTimestampMillis() - event.getTimestampMillis());

        if (mListeners.size() > 0) {
            Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                ((TransactionQueueListener)it.next())
                    .transactionDequeued(deqEvent);
            }
        }

        return deqEvent;
    }

    synchronized void transactionServiced(TransactionQueueEvent event) {
        TransactionQueueEvent svcEvent = new TransactionQueueEvent(event);

        mTotalServiceDuration +=
            (svcEvent.getTimestampMillis() - event.getTimestampMillis());

        if (mListeners.size() > 0) {
            Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                ((TransactionQueueListener)it.next())
                    .transactionServiced(svcEvent);
            }
        }

        // Adjust counters at end in case a listener threw an exception and let
        // the call to transactionException adjust the counters instead.
        mServicingCount--;
        mTotalServiced++;
    }

    synchronized void transactionExpired(TransactionQueueEvent event) {
        mServicingCount--;
        mTotalExpired++;

        if (mListeners.size() > 0) {
            event = new TransactionQueueEvent(event);

            Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                ((TransactionQueueListener)it.next())
                    .transactionExpired(event);
            }
        }
    }

    synchronized void transactionException(TransactionQueueEvent event,
                                           Throwable e) {
        mServicingCount--;
        mTotalServiceExceptions++;

        if (mListeners.size() > 0) {
            event = new TransactionQueueEvent(event, e);

            Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                ((TransactionQueueListener)it.next())
                    .transactionException(event);
            }
        }
    }

    synchronized void uncaughtException(Throwable e) {
        mTotalUncaughtExceptions++;

        if (mExceptionListeners.size() > 0) {
            UncaughtExceptionEvent event =
                new UncaughtExceptionEvent(this, e);

            Iterator it = mExceptionListeners.iterator();
            while (it.hasNext()) {
                ((UncaughtExceptionListener)it.next())
                    .uncaughtException(event);
            }
        }
        else {
            Thread current = Thread.currentThread();
            current.getThreadGroup().uncaughtException(current, e);
        }
    }

    synchronized boolean exitThread(boolean force) {
        if (!force && (mThreadCount - mServicingCount) <= 1 &&
            mQueue.size() > 0 && !mSuspended) {

            // Can't exit thread because transactions are waiting to
            // be serviced, and no thread is waiting on the queue.
            return false;
        }
        else {
            mThreadCount--;
            return true;
        }
    }

    private synchronized boolean ensureWaitingThread() {
        if (mThreadCount <= mServicingCount) {
            try {
                // Only wait if no threads. Otherwise the lock on this object
                // will prevent threads from entering the exitThread method.
                startThread(mThreadCount == 0);
            }
            catch (NoThreadException e) {
                if (!e.isThreadPoolClosed()) {
                    if (mThreadCount == 0) {
                        uncaughtException(e);
                        return false;
                    }
                }
            }
            catch (InterruptedException e) {
                return false;
            }
            catch (Throwable e) {
                uncaughtException(e);
                return false;
            }
        }
        return true;
    }

    private class Worker implements Runnable {
        public void run() {
            boolean forceExit = false;
            TransactionQueueEvent event;

            while (true) {
                try {
                	// allow event to be GC'd in case we wait() on next event
                	event = null;
                	
                    // Phase 1: wait for a transaction
                    try {
                        if ((event = nextTransactionEvent()) == null) {
                            // Go into idle mode.
                            continue;
                        }
                    }
                    catch (InterruptedException e) {
                        forceExit = true;
                        continue;
                    }

                    long enqueueTimestamp = event.getTimestampMillis();

                    // Phase 2: spawn off a replacement thread
                    try {
                        startThread(false);
                    }
                    catch (NoThreadException e) {
                        if (e.isThreadPoolClosed()) {
                            forceExit = true;
                            // Don't "continue" because the transaction must
                            // still be serviced first.
                        }
                    }
                    catch (InterruptedException e) {
                        forceExit = true;
                        // Don't "continue" because the transaction must
                        // still be serviced first.
                    }
                    catch (Throwable e) {
                        uncaughtException(e);
                    }
                    finally {
                        // Only indicate that transaction has been dequeued
                        // after a replacement thread has been created.
                        // Queue time is more accurate this way because time
                        // spent waiting for a thread is time spent not being
                        // serviced.
                        try {
                            event = transactionDequeued(event);
                        }
                        catch (Throwable e) {
                            uncaughtException(e);
                        }
                    }

                    long serviceTimestamp = event.getTimestampMillis();

                    // Phase 3: service the transaction
                    long timeout = getTransactionTimeout();
                    if (timeout >= 0 &&
                        (serviceTimestamp - enqueueTimestamp) >= timeout) {
                        try {
                            event.getTransaction().cancel();
                        }
                        finally {
                            transactionExpired(event);
                        }
                    }
                    else {
                        try {
                            event.getTransaction().service();
                            transactionServiced(event);
                        }
                        catch (Throwable e) {
                            uncaughtException(e);

                            try {
                                event.getTransaction().cancel();
                            }
                            catch (Throwable e2) {
                                uncaughtException(e2);
                            }

                            transactionException(event, e);
                        }
                    }
                }
                catch (Throwable e) {
                    try {
                        uncaughtException(e);
                    }
                    catch (Throwable e2) {
                        // If another error is thrown while trying to log the
                        // first error, ignore it. This ensures that the
                        // exitThread method is called, even if
                        // OutOfMemoryErrors are being thrown around.
                    }
                }
                finally {
                    if (exitThread(forceExit)) {
                        break;
                    }
                }
            }
        }
    }
}
