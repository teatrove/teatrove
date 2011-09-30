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

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ThreadPool contains a collection of re-usable threads. There is a slight
 * performance overhead in creating new threads, and so a ThreadPool can
 * improve performance in systems that create short-lived threads. Pooled
 * threads operate on Runnable targets and return back to the pool when the
 * Runnable.run method exits.
 *
 * @author Brian S O'Neill
 */
public class ThreadPool extends ThreadGroup {
    private static int cThreadID;

    private synchronized static int nextThreadID() {
        return cThreadID++;
    }

    // Fields that use the monitor of this instance.

    private long mTimeout = -1;
    private long mIdleTimeout = -1;

    // Fields that use the mListeners monitor.

    private Collection mListeners = new LinkedList();

    // Fields that use the mPool monitor.

    // Pool is accessed like a stack.
    private LinkedList mPool;
    private int mMax;
    private int mActive;
    private boolean mDaemon;
    private int mPriority;
    private boolean mClosed;

    /**
     * Create a ThreadPool of daemon threads.
     *
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(String name, int max) throws IllegalArgumentException {
        this(name, max, true);
    }

    /**
     * Create a ThreadPool of daemon threads.
     *
     * @param parent Parent ThreadGroup
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(ThreadGroup parent, String name, int max) 
        throws IllegalArgumentException
    {
        this(parent, name, max, true);
    }

    /**
     * Create a ThreadPool.
     *
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     * @param daemon Set to true to create ThreadPool of daemon threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(String name, int max, boolean daemon) 
        throws IllegalArgumentException
    {
        super(name);
        init(max, daemon);
    }

    /**
     * Create a ThreadPool.
     *
     * @param parent Parent ThreadGroup
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     * @param daemon Set to true to create ThreadPool of daemon threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(ThreadGroup parent, String name, int max, boolean daemon)
        throws IllegalArgumentException
    {
        super(parent, name);
        init(max, daemon);
    }

    private void init(int max, boolean daemon)
        throws IllegalArgumentException
    {
        if (max <= 0) {
            throw new IllegalArgumentException
                ("Maximum number of threads must be greater than zero: " +
                 max);
        }

        mMax = max;

        mDaemon = daemon;
        mPriority = Thread.currentThread().getPriority();
        mClosed = false;

        mPool = new LinkedList();
    }

    /**
     * Sets the timeout (in milliseconds) for getting threads from the pool
     * or for closing the pool. A negative value specifies an infinite timeout.
     * Calling the start method that accepts a timeout value will override 
     * this setting.
     */
    public synchronized void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    /**
     * Returns the timeout (in milliseconds) for getting threads from the pool.
     * The default value is negative, which indicates an infinite wait.
     */
    public synchronized long getTimeout() {
        return mTimeout;
    }

    /**
     * Sets the timeout (in milliseconds) for idle threads to exit. A negative
     * value specifies that an idle thread never exits.
     */
    public synchronized void setIdleTimeout(long timeout) {
        mIdleTimeout = timeout;
    }

    /**
     * Returns the idle timeout (in milliseconds) for threads to exit. The
     * default value is negative, which indicates that idle threads never exit.
     */
    public synchronized long getIdleTimeout() {
        return mIdleTimeout;
    }

    public void addThreadPoolListener(ThreadPoolListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeThreadPoolListener(ThreadPoolListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    /**
     * Returns the initial priority given to each thread in the pool. The
     * default value is that of the thread that created the ThreadPool.
     */
    public int getPriority() {
        synchronized (mPool) {
            return mPriority;
        }
    }
    
    /**
     * Sets the priority given to each thread in the pool.
     *
     * @throws IllegalArgumentException if priority is out of range
     */
    public void setPriority(int priority) throws IllegalArgumentException {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException
                ("Priority out of range: " + priority);
        }

        synchronized (mPool) {
            mPriority = priority;
        }
    }

    /**
     * @return The maximum allowed number of threads.
     */
    public int getMaximumAllowed() {
        synchronized (mPool) {
            return mMax;
        }
    }

    /**
     * @return The number of currently available threads in the pool.
     */
    public int getAvailableCount() {
        synchronized (mPool) {
            return mPool.size();
        }
    }

    /**
     * @return The total number of threads in the pool that are either
     * available or in use.
     */
    public int getPooledCount() {
        synchronized (mPool) {
            return mActive;
        }
    }

    /**
     * @return The total number of threads in the ThreadGroup.
     */
    public int getThreadCount() {
        return activeCount();
    }

    /**
     * @return Each thread that is active in the entire ThreadGroup.
     */
    public Thread[] getAllThreads() {
        int count = activeCount();
        Thread[] threads = new Thread[count];
        count = enumerate(threads);
        if (count >= threads.length) {
            return sort(threads);
        }
        else {
            Thread[] newThreads = new Thread[count];
            System.arraycopy(threads, 0, newThreads, 0, count);
            return sort(newThreads);
        }
    }

    private Thread[] sort(Thread[] threads) {
        Comparator c = BeanComparator.forClass(Thread.class)
            .orderBy("threadGroup.name")
            .orderBy("name")
            .orderBy("priority");
        Arrays.sort(threads, c);
        return threads;
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, getTimeout(), null);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param timeout Milliseconds to wait for a thread to become
     * available. If zero, don't wait at all. If negative, wait forever.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, long timeout)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, timeout, null);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }


    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param name The name to give the thread.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, String name)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, getTimeout(), name);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param timeout Milliseconds to wait for a thread to become
     * @param name The name to give the thread.
     * available. If zero, don't wait at all. If negative, wait forever.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, long timeout, String name) 
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, timeout, name);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    private Thread start0(Runnable target, long timeout, String name) 
        throws NoThreadException, InterruptedException
    {
        PooledThread thread;

        while (true) {
            synchronized (mPool) {
                closeCheck();

                // Obtain a thread from the pool if non-empty.
                if (mPool.size() > 0) {
                    thread = (PooledThread)mPool.removeLast();
                }
                else {
                    // Create a new thread if the number of active threads
                    // is less than the maximum allowed.
                    if (mActive < mMax) {
                        return startThread(target, name);
                    }
                    else {
                        break;
                    }
                }
            }

            if (thread.setTarget(target)) {
                return thread;
            }
            
            // Couldn't set the target because the pooled thread is exiting.
            // Wait for it to exit to ensure that the active count is less
            // than the maximum and try to obtain another thread.
            thread.join();
        }
        
        if (timeout == 0) {
            throw new NoThreadException("No thread available from " + this);
        }

        // Wait for a thread to become available in the pool.
        synchronized (mPool) {
            closeCheck();

            if (timeout < 0) {
                while (mPool.size() <= 0) {
                    mPool.wait(0);
                    closeCheck();
                }
            }
            else {
                long expireTime = System.currentTimeMillis() + timeout;
                while (mPool.size() <= 0) {
                    mPool.wait(timeout);
                    closeCheck();

                    // Thread could have been notified, but another thread may
                    // have stolen the thread away.
                    if (mPool.size() <= 0 &&
                        System.currentTimeMillis() > expireTime) {
                        
                        throw new NoThreadException
                            ("No thread available after waiting " + 
                             timeout + " milliseconds: " + this);
                    }
                }
            }
        
            thread = (PooledThread)mPool.removeLast();
            if (thread.setTarget(target)) {
                return thread;
            }
        }
        
        // Couldn't set the target because the pooled thread is exiting.
        // Wait for it to exit to ensure that the active count is less
        // than the maximum and create a new thread.
        thread.join();
        return startThread(target, name);
    }

    public boolean isClosed() {
        return mClosed;
    }

    /**
     * Will close down all the threads in the pool as they become
     * available. This method may block forever if any threads are
     * never returned to the thread pool.
     */
    public void close() throws InterruptedException {
        close(getTimeout());
    }

    /**
     * Will close down all the threads in the pool as they become 
     * available. If all the threads cannot become available within the
     * specified timeout, any active threads not yet returned to the
     * thread pool are interrupted.
     *
     * @param timeout Milliseconds to wait before unavailable threads
     * are interrupted. If zero, don't wait at all. If negative, wait forever.
     */
    public void close(long timeout) throws InterruptedException {
        synchronized (mPool) {
            mClosed = true;
            mPool.notifyAll();
            
            if (timeout != 0) {
                if (timeout < 0) {
                    while (mActive > 0) {
                        // Infinite wait for notification.
                        mPool.wait(0);
                    }
                }
                else {
                    long expireTime = System.currentTimeMillis() + timeout;
                    while (mActive > 0) {
                        mPool.wait(timeout);
                        if (System.currentTimeMillis() > expireTime) {
                            break;
                        }
                    }
                }
            }
        }

        interrupt();
    }

    private PooledThread startThread(Runnable target, String name) {
        PooledThread thread;

        synchronized (mPool) {
            mActive++;
            thread = new PooledThread(getName() + ' ' + nextThreadID());
            thread.setPriority(mPriority);
            thread.setDaemon(mDaemon);
            
            thread.setTarget(target);
            thread.start();
        }

        synchronized (mListeners) {
            if (mListeners.size() > 0) {
                ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
                for (Iterator it = mListeners.iterator(); it.hasNext();) {
                    ((ThreadPoolListener)it.next()).threadStarted(event);
                }
            }
        }

        return thread;
    }

    private void closeCheck() throws NoThreadException {
        if (mClosed) {
            throw new NoThreadException("Thread pool is closed", true);
        }
    }

    void threadAvailable(PooledThread thread) {
        synchronized (mPool) {
            if (thread.getPriority() != mPriority) {
                thread.setPriority(mPriority);
            }
            mPool.addLast(thread);
            mPool.notify();
        }
    }

    void threadExiting(PooledThread thread) {
        synchronized (mPool) {
            mPool.remove(thread);
            mActive--;
            mPool.notify();
        }

        synchronized (mListeners) {
            if (mListeners.size() > 0) {
                ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
                for (Iterator it = mListeners.iterator(); it.hasNext();) {
                    ((ThreadPoolListener)it.next()).threadExiting(event);
                }
            }
        }
    }

    private class PooledThread extends Thread {
        private Runnable mTarget;
        private boolean mExiting;

        public PooledThread(String name) {
            super(ThreadPool.this, name);

            setInheritedAccessControlContextToNull();
        }

        /**
         * As of JDK 1.6 java.util.Thread holds an unused reference to a security context which in turn holds
         * references to all classloaders.  This caused a memory leak as Tea recompiles templates and creates
         * a new class loader each time.
         *
         * @return the truth that inheritedAccessControlContext was set to null
         */
        private boolean setInheritedAccessControlContextToNull() {
            final Logger log = Logger.getLogger(PooledThread.class.getName());
            try {
                final Field field = Thread.class.getDeclaredField("inheritedAccessControlContext");

                if(field != null) {
                    // Override the private keyword,
                    //   this throw an IllegalAccessException if a SecurityManager is installed.
                    field.setAccessible(true);

                    // Null out the field
                    field.set(this, null);

                    return true;
                }
            } catch (IllegalAccessException e) {
                log.logp(Level.WARNING, PooledThread.class.getName(), "<init>", "Unable to nullify inheritedAccessControlContext.", e);
            } catch (NoSuchFieldException e) {
                log.logp(Level.WARNING, PooledThread.class.getName(), "<init>", "Unable to nullify inheritedAccessControlContext.", e);
            }
            return false;
        }


        synchronized boolean setTarget(Runnable target) {
            if (mTarget != null) {
                throw new IllegalStateException
                    ("Target runnable in pooled thread is already set");
            }

            if (mExiting) {
                return false;
            }
            else {
                mTarget = target;
                notify();
                return true;
            }
        }

        private synchronized Runnable waitForTarget() {
            Runnable target;
            
            if ((target = mTarget) == null) {
                long idle = getIdleTimeout();
                
                if ((target = mTarget) == null) {
                    if (idle != 0) {
                        try {
                            if (idle < 0) {
                                wait(0);
                            }
                            else {
                                wait(idle);
                            }
                        }
                        catch (InterruptedException e) {
                        }
                    }
                    
                    if ((target = mTarget) == null) {
                        mExiting = true;
                    }
                }
            }

            return target;
        }

        public void run() {
            try {
                while (!isClosed()) {
                    if (Thread.interrupted()) {
                        continue;
                    }

                    Runnable target;

                    if ((target = waitForTarget()) == null) {
                        break;
                    }

                    try {
                        target.run();
                    }
                    catch (ThreadDeath death) {
                        break;
                    }
                    catch (Throwable e) {
                        uncaughtException(Thread.currentThread(), e);
                        e = null;
                    }

                    // Allow the garbage collector to reclaim target from
                    // stack while we wait for another target.
                    target = null;

                    mTarget = null;
                    threadAvailable(this);
                }
            }
            finally {
                threadExiting(this);
            }
        }
    }
}
