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
 * The ReadWriteLock interface provides a more flexible locking mechanism than
 * Java's monitors. If there are many threads that wish to only read a
 * resource, then they may get a read lock which only blocks writers. A
 * resource can be shared more efficiently in this way.
 *
 * <p>When using the locking mechanisms, its a good idea to release the lock 
 * inside a finally statement to ensure the lock is always released. Example:
 * 
 * <pre>
 *     private Lock fileLock = new ReadWriteLock();
 *
 *     public String readFile() {
 *         try {
 *             fileLock.acquireReadLock();
 *             ...
 *         }
 *         finally {
 *             fileLock.releaseLock();
 *         }
 *     }
 *
 *     public void writeFile(String str) {
 *         try {
 *             fileLock.acquireWriteLock();
 *             ...
 *         }
 *         finally {
 *             fileLock.releaseLock();
 *         }
 *     }
 *
 *     public void deleteFile(String name) {
 *         try {
 *             fileLock.acquireUpgradableLock();
 *             if (exists(name)) {
 *                 try {
 *                     fileLock.acquireWriteLock();
 *                     delete(name);
 *                 }
 *                 finally {
 *                     fileLock.releaseLock();
 *                 }
 *             }
 *         }
 *         finally {
 *             fileLock.releaseLock();
 *         }
 *     }
 * </pre>
 *
 * @author Brian S O'Neill
 */
public interface ReadWriteLock {
    public final int NONE = 0, READ = 1, UPGRADABLE = 2, WRITE = 3;

    /**
     * Same as calling acquireReadLock(getDefaultTimeout()).
     */
    public void acquireReadLock() throws InterruptedException;

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
    public boolean acquireReadLock(long timeout) throws InterruptedException;

    /**
     * Same as calling acquireUpgradableLock(getDefaultTimeout()).
     */
    public void acquireUpgradableLock()
        throws InterruptedException, IllegalStateException;

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
        throws InterruptedException, IllegalStateException;

    /**
     * Same as calling acquireWriteLock(getDefaultTimeout()).
     */
    public void acquireWriteLock()
        throws InterruptedException, IllegalStateException;

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
        throws InterruptedException, IllegalStateException;

    /**
     * Release the lock held by the current thread.
     *
     * @return false if this thread doesn't hold a lock.
     */
    public boolean releaseLock();

    /**
     * Returns the default timeout used for acquiring locks. If negative,
     * the timeout is infinite.
     */
    public long getDefaultTimeout();
}
