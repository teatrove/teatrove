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

package org.teatrove.trove.persist;

import java.io.*;
import java.util.*;
import java.lang.ref.*;
import org.teatrove.trove.file.FileBuffer;
import org.teatrove.trove.file.FileBufferInputStream;
import org.teatrove.trove.file.FileBufferOutputStream;
import org.teatrove.trove.file.FileRepository;
import org.teatrove.trove.io.FastBufferedOutputStream;
import org.teatrove.trove.io.FastBufferedInputStream;
import org.teatrove.trove.util.SoftHashMap;
import org.teatrove.trove.util.IdentityMap;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 */
public class BasicObjectRepository implements ObjectRepository {
    // How many milleseconds to wait on ReferenceQueue before exiting the
    // deferred deletion thread.
    private static final long QUEUE_IDLE = 60000;

    private static ReferenceQueue cRefQueue;
    private static Thread cDeferredThread;

    private static synchronized ReferenceQueue getRefQueue() {
        if (cRefQueue == null) {
            cRefQueue = new ReferenceQueue();
        }
        if (cDeferredThread == null) {
            cDeferredThread = new DeferredThread(cRefQueue);
            cDeferredThread.setDaemon(true);
            cDeferredThread.start();
        }
        return cRefQueue;
    }

    private static synchronized void exitDeferredThread() {
        cDeferredThread = null;
    }

    private final FileRepository mRepository;

    private final ObjectStreamBuilder mOSBuilder;

    // Maps Long ids to Objects.
    private Map mIdToObjectCache;
    // Maps Objects to long ids.
    private Map mObjectToIdMap;

    // Maps ids to DeferredDelete objects.
    private Map mDeferredDeletes;

    private boolean mClosed;

    public BasicObjectRepository(FileRepository repository)
        throws IOException
    {
        this(repository, null);
    }

    public BasicObjectRepository(FileRepository repository,
                                 ObjectStreamBuilder builder)
        throws IOException
    {
        mRepository = repository;
        if (builder == null) {
            builder = new ObjectStreamBuilder();
        }
        mOSBuilder = builder;
        mIdToObjectCache = new SoftHashMap();
        mObjectToIdMap = new IdentityMap();
        mDeferredDeletes = new HashMap();
    }
    
    public long fileCount() throws IOException {
        synchronized (this) {
            checkClosed();
        }
        return mRepository.fileCount();
    }

    public FileRepository.Iterator fileIds() throws IOException {
        synchronized (this) {
            checkClosed();
        }
        return mRepository.fileIds();
    }

    public boolean fileExists(long id) throws IOException {
        synchronized (this) {
            checkClosed();
        }
        return mRepository.fileExists(id);
    }

    public FileBuffer openFile(long id)
        throws IOException, FileNotFoundException
    {
        synchronized (this) {
            checkClosed();
        }
        return mRepository.openFile(id);
    }

    public long createFile() throws IOException {
        synchronized (this) {
            checkClosed();
        }
        return mRepository.createFile();
    }

    public boolean deleteFile(long id) throws IOException {
        synchronized (this) {
            checkClosed();
        }
        return deleteObject(new Long(id));
    }

    public ReadWriteLock lock() {
        return mRepository.lock();
    }

    public Object retrieveObject(long id)
        throws IOException, FileNotFoundException, ClassNotFoundException
    {
        return retrieveObject(id, 0);
    }

    public Object retrieveObject(long id, int reserved)
        throws IOException, FileNotFoundException, ClassNotFoundException
    {
        Long Id = new Long(id);
        Object obj;
        synchronized (this) {
            checkClosed();
            obj = mIdToObjectCache.get(Id);
            if (obj != null) {
                return obj;
            }
        }
        
        FileBuffer file;

        try {
            // Acquire this lock so that file can be opened and locked to
            // prevent another thread from deleting it.
            lock().acquireReadLock();

            file = mRepository.openFile(id);
            file.lock().acquireReadLock();
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }

        ObjectRepository old = local.set(this);
        try {
            InputStream in = new FileBufferInputStream(file, reserved, true);
            int bufSize = in.available();
            if (bufSize > 1) {
                if (bufSize > 2048) {
                    bufSize = 2048;
                }
                in = new FastBufferedInputStream(in, bufSize);
            }

            ObjectInputStream oin = mOSBuilder.createInputStream(in);
            obj = oin.readObject();
            oin.close();
        }
        finally {
            local.set(old);
            file.lock().releaseLock();
        }
        
        synchronized (this) {
            Object existing = mIdToObjectCache.get(Id);
            if (existing != null) {
                return existing;
            }
            mIdToObjectCache.put(Id, obj);
            mObjectToIdMap.put(obj, Id);
        }
        
        return obj;
    }

    public long saveObject(Object obj) throws IOException {
        return saveObject(obj, 0);
    }

    public long saveObject(Object obj, int reserved) throws IOException {
        Long Id;
        synchronized (this) {
            checkClosed();
            Id = (Long)mObjectToIdMap.get(obj);
            if (Id != null && cancelDeferredDelete(Id)) {
                return Id.longValue();
            }
        }
        
        FileBuffer file;

        try {
            // Acquire this lock so that file can be created and locked to
            // prevent another thread from deleting it.
            lock().acquireWriteLock();

            long id = mRepository.createFile();
            file = mRepository.openFile(id);
            file.lock().acquireWriteLock();
            Id = new Long(id);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }

        ObjectRepository old = local.set(this);
        try {
            FileBufferOutputStream out =
                new FileBufferOutputStream(file, reserved, true);
            ObjectOutputStream oout = mOSBuilder.createOutputStream
                (new FastBufferedOutputStream(out));
            oout.writeObject(obj);
            oout.close();
        }
        finally {
            local.set(old);
            file.lock().releaseLock();
        }

        synchronized (this) {
            mIdToObjectCache.put(Id, obj);
            mObjectToIdMap.put(obj, Id);
        }
        
        return Id.longValue();
    }

    public long replaceObject(Object obj) throws IOException {
        return replaceObject(obj, 0, 0);
    }

    public long replaceObject(Object obj, int reserved) throws IOException {
        return replaceObject(obj, reserved, 0);
    }

    public long replaceObject(Object obj, int reserved, long id)
        throws IOException
    {
        Long Id;
        synchronized (this) {
            checkClosed();
            Id = (Long)mObjectToIdMap.get(obj);
            if (Id == null || !cancelDeferredDelete(Id)) {
                if (id > 0) {
                    Id = new Long(id);
                    if (!cancelDeferredDelete(Id)) {
                        Id = null;
                    }
                }
                else {
                    Id = null;
                }
            }
        }

        FileBuffer file;

        try {
            // Acquire this lock so that file can be opened and locked to
            // prevent another thread from deleting it.
            lock().acquireUpgradableLock();

            if (Id == null) {
                file = null;
            }
            else {
                try {
                    file = mRepository.openFile(Id.longValue());
                    file.lock().acquireWriteLock();
                }
                catch (FileNotFoundException e) {
                    file = null;
                }
            }

            if (file == null) {
                try {
                    lock().acquireWriteLock();
                    
                    id = mRepository.createFile();
                    file = mRepository.openFile(id);
                    file.lock().acquireWriteLock();
                    Id = new Long(id);
                }
                finally {
                    lock().releaseLock();
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            lock().releaseLock();
        }

        ObjectRepository old = local.set(this);
        try {
            FileBufferOutputStream out =
                new FileBufferOutputStream(file, reserved, true);
            ObjectOutputStream oout = mOSBuilder.createOutputStream
                (new FastBufferedOutputStream(out));
            oout.writeObject(obj);
            oout.flush();
            // If re-using file, make sure old data is trimmed off.
            file.truncate(out.position());
            oout.close();
        }
        finally {
            local.set(old);
            file.lock().releaseLock();
        }

        synchronized (this) {
            mIdToObjectCache.put(Id, obj);
            mObjectToIdMap.put(obj, Id);
        }

        return Id.longValue();
    }

    public boolean removeObject(long id) throws IOException {
        Long Id = new Long(id);
        Object obj;
        synchronized (this) {
            checkClosed();
            obj = mIdToObjectCache.get(Id);
            if (obj != null) {
                if (!mDeferredDeletes.containsKey(Id)) {
                    mDeferredDeletes.put
                        (Id, new DeferredDelete(obj, getRefQueue(), Id));
                }
                return false;
            }
        }

        // No in memory instance, so delete it right away.
        return deleteObject(Id);
    }

    public synchronized void close() throws IOException {
        if (!mClosed) {
            Collection deferred = mDeferredDeletes.values();
            // Clone it to prevent concurrent modification exceptions during
            // iteration.
            deferred = new ArrayList(deferred);
            java.util.Iterator it = deferred.iterator();
            while (it.hasNext()) {
                ((DeferredDelete)it.next()).delete();
            }
            mClosed = true;
        }
    }

    boolean deleteObject(Long Id) throws IOException {
        synchronized (this) {
            cancelDeferredDelete(Id);
            Object old = mIdToObjectCache.remove(Id);
            if (old != null) {
                mObjectToIdMap.remove(old);
            }
        }
        return mRepository.deleteFile(Id.longValue());
    }

    private void checkClosed() throws IOException {
        if (mClosed) {
            throw new IOException("ObjectRepository closed");
        }
    }

    // Returns false if a deferred delete couldn't be cancelled.
    private synchronized boolean cancelDeferredDelete(Long Id) {
        DeferredDelete deferred = (DeferredDelete)mDeferredDeletes.remove(Id);
        if (deferred != null) {
            return deferred.cancel();
        }
        return true;
    }
    
    private class DeferredDelete extends PhantomReference {
        ThreadGroup mGroup;
        Long mId;

        DeferredDelete(Object obj, ReferenceQueue queue, Long Id) {
            super(obj, queue);
            mGroup = Thread.currentThread().getThreadGroup();
            mId = Id;
        }

        void delete() {
            Long Id;
            synchronized (this) {
                if ((Id = mId) == null) {
                    return;
                }
                mId = null;
                clear();
            }

            try {
                BasicObjectRepository.this.deleteObject(Id);
            }
            catch (IOException e) {
                mGroup.uncaughtException(Thread.currentThread(), e);
            }
        }

        synchronized boolean cancel() {
            if (mId != null) {
                mId = null;
                clear();
                return true;
            }
            return false;
        }
    }

    private static class DeferredThread extends Thread {
        private ReferenceQueue mQueue;

        DeferredThread(ReferenceQueue queue) {
            super("ObjectRepository deferred deletion");
            mQueue = queue;
        }

        public void run() {
            try {
                while (true) {
                    try {
                        DeferredDelete deferred = 
                            (DeferredDelete)mQueue.remove(QUEUE_IDLE);
                        if (deferred == null) {
                            break;
                        }
                        deferred.delete();
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                    catch (Throwable e) {
                        Thread t = Thread.currentThread();
                        t.getThreadGroup().uncaughtException(t, e);
                    }
                }
            }
            finally {
                exitDeferredThread();
            }
        }
    }
}
