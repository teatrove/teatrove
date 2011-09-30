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

/**
 * SecureReadWriteLock prevents threads from accidentally releasing locks on
 * behalf of other threads. A thread can only release as many locks as it has
 * acquired. In addition, IllegalStateExceptions are thrown when a thread
 * attempts to acquire a lock in a fashion that is deadlock prone.
 * <p>
 * Threads requesting write locks are given priority over those requesting
 * weaker locks. Without this priority, many read threads can easily starve the
 * threads requesting write locks.
 * <p>
 * SecureReadWriteLock is fully re-entrant. A thread may request a lock of
 * equal or lesser strength as many times as it likes, provided that a release
 * is performed as many times.
 *
 * <pre>
 * lock.acquireWriteLock();
 * lock.acquireReadLock();  // works because thread already has stronger lock
 * ...
 * lock.releaseLock(); // read lock released, original write lock still kept
 * lock.releaseLock(); // release original write lock
 * </pre>
 *
 * @author Brian S O'Neill
 */
public final class SecureReadWriteLock implements ReadWriteLock {
    private final long mTimeout;

    // The count of threads holding read locks.
    private int mReadLocks;

    // The thread (if any) that holds the upgradable lock.
    private Thread mUpgradableLockHeld;

    // The thread (if any) that holds the write lock.
    private Thread mWriteLockHeld;

    // The count of threads attempting to acquire write locks.
    private int mWriteLockAttempts;

    private final ThreadLocal mLockInfoRef = new LockInfoRef();

    /**
     * Constructs a SecureReadWriteLock with an infinite default timeout.
     */
    public SecureReadWriteLock() {
        mTimeout = -1;
    }

    /**
     * Constructs a SecureReadWriteLock with the given default timeout. If
     * negative, the timeout is infinite.
     */
    public SecureReadWriteLock(long timeout) {
        mTimeout = timeout;
    }

    /**
     * Same as calling acquireReadLock(getDefaultTimeout()).
     */
    public void acquireReadLock() throws InterruptedException {
        acquireReadLock(mTimeout);
    }

    /**
     * A read lock is obtained when no threads currently hold a write lock.
     * When a thread has a read lock, it only blocks threads that wish to
     * acquire a write lock.
     * <p>
     * A read lock is the weakest form of lock.
     *
     * @param timeout milliseconds to wait for lock acquisition. If negative,
     * timeout is infinite.
     * @return true if the lock was acquired.
     */
    public boolean acquireReadLock(long timeout) throws InterruptedException {
        LockInfo info = (LockInfo)mLockInfoRef.get();

        if (info.mType == NONE) {
            synchronized (this) {
                if (!readLockAvailable()) {
                    if (timeout < 0) {
                        while (true) {
                            wait();
                            if (readLockAvailable()) {
                                break;
                            }
                        }
                    }
                    else if (timeout > 0) {
                        long expire = System.currentTimeMillis() + timeout;
                        while (true) {
                            wait(timeout);
                            if (readLockAvailable()) {
                                break;
                            }
                            timeout = expire - System.currentTimeMillis();
                            if (timeout <= 0) {
                                return false;
                            }
                        };
                    }
                    else {
                        return false;
                    }
                }
                mReadLocks++;
            }
            info.mType = READ;
        }     
        
        info.mCount++;
        return true;
    }

    /**
     * Same as calling acquireUpgradableLock(getDefaultTimeout()).
     */
    public void acquireUpgradableLock()
        throws InterruptedException, IllegalStateException
    {
        acquireUpgradableLock(mTimeout);
    }

    /**
     * An upgradable lock is obtained when no threads currently hold write or
     * upgradable locks. When a thread has an upgradable lock, it blocks 
     * threads that wish to acquire upgradable or write locks.
     * <p>
     * Upgradable locks are to be used when a thread needs a read lock, but
     * while that read lock is held, it may need to be upgraded to a write
     * lock. If a thread acquired a read lock and then attempted to
     * acquire a write lock while the first lock was held, the thread
     * would be deadlocked with itself.
     * <p>
     * To prevent deadlock, threads that may need to upgrade a read lock
     * to a write lock should acquire an upgradable lock instead of a read 
     * lock. Upgradable locks will not block threads that wish to only read.
     * <p>
     * To perform an upgrade, call acquireWriteLock while the upgradable 
     * lock is still held.
     *
     * @param timeout milliseconds to wait for lock acquisition. If negative,
     * timeout is infinite.
     * @return true if the lock was acquired.
     * @throws IllegalStateException if thread holds a read lock.
     */
    public boolean acquireUpgradableLock(long timeout)
        throws InterruptedException, IllegalStateException
    {
        LockInfo info = (LockInfo)mLockInfoRef.get();
        int type = info.mType;

        if (type == READ) {
            throw new IllegalStateException
                ("Cannot acquire an upgradable lock while thread holds " +
                 "only a read lock.");
        }

        if (type == NONE) {
            synchronized (this) {
                if (!upgradableLockAvailable()) {
                    if (timeout < 0) {
                        while (true) {
                            wait();
                            if (upgradableLockAvailable()) {
                                break;
                            }
                        }
                    }
                    else if (timeout > 0) {
                        long expire = System.currentTimeMillis() + timeout;
                        while (true) {
                            wait(timeout);
                            if (upgradableLockAvailable()) {
                                break;
                            }
                            timeout = expire - System.currentTimeMillis();
                            if (timeout <= 0) {
                                return false;
                            }
                        };
                    }
                    else {
                        return false;
                    }
                }
                mUpgradableLockHeld = Thread.currentThread();
            }
            info.mType = UPGRADABLE;
        }     

        info.mCount++;
        return true;
    }

    /**
     * Same as calling acquireWriteLock(getDefaultTimeout()).
     */
    public void acquireWriteLock()
        throws InterruptedException, IllegalStateException
    {
        acquireWriteLock(mTimeout);
    }

    /**
     * A write lock is obtained only when there are no read, upgradable or 
     * write locks held by any other thread. When a thread has a write lock,
     * it blocks any thread that wishes to acquire any kind of lock.
     * <p>
     * A write lock is the strongest form of lock. When a write lock is
     * held by a thread, then that thread alone has a lock. Requests for
     * write locks are granted the highest priority.
     *
     * @param timeout milliseconds to wait for lock acquisition. If negative,
     * timeout is infinite.
     * @return true if the lock was acquired.
     * @throws IllegalStateException if thread holds a read lock.
     */
    public boolean acquireWriteLock(long timeout)
        throws InterruptedException, IllegalStateException
    {
        LockInfo info = (LockInfo)mLockInfoRef.get();
        int type = info.mType;

        if (type == READ) {
            throw new IllegalStateException
                ("Cannot acquire a write lock while thread holds " + 
                 "only a read lock. " +
                 "Use an upgradable lock instead of a read lock.");
        }

        if (type != WRITE) {
            synchronized (this) {
                if (type == UPGRADABLE) {
                    // Save the count before upgrade, which will be used when
                    // the lock is released.
                    info.mUpgradeCount = info.mCount;
                }
                
                if (!writeLockAvailable(type)) {
                    if (timeout < 0) {
                        mWriteLockAttempts++;
                        try {
                            while (true) {
                                wait();
                                if (writeLockAvailable(type)) {
                                    break;
                                }
                            }
                        }
                        catch (InterruptedException e) {
                            if (type == UPGRADABLE) {
                                info.mUpgradeCount = 0;
                            }
                            throw e;
                        }
                        finally {
                            mWriteLockAttempts--;
                        }
                    }
                    else if (timeout > 0) {
                        mWriteLockAttempts++;
                        long expire = System.currentTimeMillis() + timeout;
                        try {
                            while (true) {
                                wait(timeout);
                                if (writeLockAvailable(type)) {
                                    break;
                                }
                            }
                            timeout = expire - System.currentTimeMillis();
                            if (timeout <= 0) {
                                return false;
                            }
                        }
                        catch (InterruptedException e) {
                            if (type == UPGRADABLE) {
                                info.mUpgradeCount = 0;
                            }
                            throw e;
                        }
                        finally {
                            mWriteLockAttempts--;
                        }
                    }
                    else {
                        return false;
                    }
                }
                mWriteLockHeld = Thread.currentThread();
            }
            info.mType = WRITE;
        }
        
        info.mCount++;
        return true;
    }

    /**
     * Release the lock held by the current thread.
     *
     * @return false if this thread doesn't hold a lock.
     */
    public boolean releaseLock() {
        LockInfo info = (LockInfo)mLockInfoRef.get();
        int type = info.mType;
        int count = info.mCount;
        if (count > 0) {
            count--;
        }
        else {
            info.mType = type = NONE;
            count = 0;
        }

        if ((info.mCount = count) == 0) {
            switch (type) {
            case NONE: default:
                return false;

            case READ:
                synchronized (this) {
                    mReadLocks--;
                    notifyAll();
                }
                break;

            case UPGRADABLE:
                synchronized (this) {
                    mUpgradableLockHeld = null;
                    notifyAll();
                }
                break;

            case WRITE:
                synchronized (this) {
                    mWriteLockHeld = null;
                    notifyAll();
                }
                break;
            }

            info.mType = NONE;
        }
        else if (type == WRITE && info.mUpgradeCount == count) {
            // Convert write lock back into an upgradable lock.
            info.mType = UPGRADABLE;
            info.mUpgradeCount = 0;
            synchronized (this) {
                mWriteLockHeld = null;
                notifyAll();
            }
        }

        return true;
    }

    /**
     * Returns the default timeout used for acquiring locks. If negative,
     * the timeout is infinite.
     */
    public long getDefaultTimeout() {
        return mTimeout;
    }

    /**
     * Returns the lock type held by the calling thread, which is
     * NONE, READ, UPGRADABLE, or WRITE.
     */
    public int getLockType() {
        return ((LockInfo)mLockInfoRef.get()).mType;
    }

    /**
     * Returns the number of times this thread has acquired a lock. The
     * releaseLock method would need be called this many times to release all
     * the locks.
     */
    public int getLockAcquisitions() {
        return ((LockInfo)mLockInfoRef.get()).mCount;
    }

    /**
     * Returns the number of threads that hold read locks.
     */
    public synchronized int getReadLocksHeld() {
        return mReadLocks;
    }

    /**
     * Returns the thread (if any) that is holding the upgradable lock. It
     * can be interrupted to stop a deadlock.
     */
    public synchronized Thread getUpgradableLockHeld() {
        return mUpgradableLockHeld;
    }

    /**
     * Returns the thread (if any) that is holding the write lock. It can be
     * interrupted to stop a deadlock.
     */
    public synchronized Thread getWriteLockHeld() {
        return mWriteLockHeld;
    }

    public synchronized String toString() {
        return super.toString() + '[' + mReadLocks + ',' +
            mUpgradableLockHeld + ',' + mWriteLockHeld + ']';
    }

    // Caller must be synchronized.
    private boolean readLockAvailable() {
        return mWriteLockHeld == null && mWriteLockAttempts == 0;
    }

    // Caller must be synchronized.
    private boolean upgradableLockAvailable() {
        return readLockAvailable() && mUpgradableLockHeld == null;
    }

    // Caller must be synchronized.
    private boolean writeLockAvailable(int type) {
        if (mReadLocks == 0 && mWriteLockHeld == null) {
            return (type == UPGRADABLE) ? true : mUpgradableLockHeld == null;
        }
        return false;
    }

    private static final class LockInfo {
        int mType = NONE;
        int mCount;
        // The lock count when the lock was upgraded to a write lock.
        int mUpgradeCount;
    }

    private static final class LockInfoRef extends ThreadLocal {
        protected Object initialValue() {
            return new LockInfo();
        }
    }
}
