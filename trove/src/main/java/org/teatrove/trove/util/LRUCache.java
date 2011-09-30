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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LRUCache is a specialized Map that is guaranteed to have the most recently 
 * used entries available. Calling "get" or "put" updates the internal ordered
 * MRU list (LRU expiry), but calling "containsKey" or "containsValue" will not.
 * <p>
 * Unlike Cache, when least recently used items are expired, they are eligible 
 * for garbage collection and NOT backed by a soft reference.
 * <p>
 * The LRUCache class is thread safe and utilizes a combination of classes
 * in java.util.concurrent package and light weight guards around code that 
 * mutates them (where threads should not be interleaved).   It is engineered
 * for maximum "liveness". 
 * <p>
 * LRUCache also allows the registration of interest for LRU expire events via
 * the LRUCache.ExpirationListener interface.
 * <p>
 * Briefly stated, LRUCache can be used as a simple bounded Map.
 * <p>
 *
 * @author Guy Molinari
 */
public class LRUCache implements Map {

    public static final int DEFAULT_TARGET_HIT_RATIO = 97;
    public static final int DEFAULT_MAX_MEMORY_USED_PERCENT = 90;
    public static final int DEFAULT_WINDOW_SIZE = 5;

    public static final Executor expireExecutor = Executors.newFixedThreadPool(10, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("LRUCache fireExpireEvent".concat(thread.getName()));
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });

    private Map<Object, Entry> mBackingMap;

    private ConcurrentLinkedList<Entry> mUsageQueue;
    private int mMaxRecent;

    // RMS smoothed target for cache equilibrium
    private int mTargetHitRatio;
    
    // Re-enable autotune if hit ratio falls below this threshold.
    private int mMaxMemoryUsedPercent;
    private boolean mWasAutoTuned = false;
    private boolean mWasMemorySizeThresholdReached = false;
    private LinkedBlockingQueue<MutablePair> mSampleQueue;
    private double mCumTangent = 0;

    private AtomicLong mHits;
    private AtomicLong mMisses;

    // In-progress puts
    private ReentrantLock mPutLock = new ReentrantLock();

    // Manages expireEvent listeners.
    private ArrayList<ExpirationListener> mListeners = new ArrayList();

    public int getTargetHitRatio() { return mTargetHitRatio; }
    public void setTargetHitRatio(int target) {
        mTargetHitRatio = target;
    }


    public int getMaxMemoryUsedPercent() { return mMaxMemoryUsedPercent; }
    public void setMaxMemoryUsedPercent(int maxMemory) {
        mMaxMemoryUsedPercent = maxMemory;
    }


    public boolean wasMemorySizeThresholdReached() { return mWasMemorySizeThresholdReached; }

    public boolean wasAutoTuned() { return mWasAutoTuned; }

    public long getHits() {
        return mHits.get();
    }


    public long getMisses() {
        return mMisses.get();
    }


    public double getHitRatio() {
        if (mHits.get() + mMisses.get() == 0)
            return 0;
        return (1 - (mMisses.doubleValue() / (mHits.doubleValue() + mMisses.doubleValue()))) * 100;
    }

    
    /**
     * Calculate first order derivative over sliding window.
     *
     * deltaX = sampled cache size, deltaY = sampled cumulative hit ratio.
     */
    public double getTangent() {
    
        MutablePair firstPair = mSampleQueue.size() > 0 ? mSampleQueue.peek() : new MutablePair(0, 0);
        MutablePair lastPair = null;
        for (MutablePair p : mSampleQueue)
            lastPair = p;
        if (lastPair == null)
            lastPair = new MutablePair(0, 1);

        double deltaY = Math.abs(lastPair.getHitRatio() - firstPair.getHitRatio());
        int deltaX = Math.abs(lastPair.getCacheSize() - firstPair.getCacheSize());

        // deltaX can't be zero because getTangent() is called inside put()
        return deltaY / deltaX;
        
    }


    public double getHitRatioTargetDelta() {
        return getTargetHitRatio() - getHitRatio();
    }

   
    /**
     * Calculate cache estimate (Proportional feedback control using derivative).
     */
    public int estimateCacheSize(double tangent) {
        return (int) (mBackingMap.size() + .9 * getHitRatioTargetDelta() / tangent);
    }
    

    public int getMemoryUsedPercent() {
        double maxMemory = (double) Runtime.getRuntime().maxMemory();
        double totalMemory = (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        return (int) (totalMemory / maxMemory * 100);
    }


    public int getMaxRecent() {
        return mMaxRecent;
    }


    public void setMaxRecent(int maxRecent) {
        mMaxRecent = maxRecent;
    }


    /**
     * Construct a cache with an amount of recently used entries that are
     * guaranteed to always be in the cache.
     *
     * @param Map The Map used for underlying storage.  Must be thread-safe.
     * @param maxRecent maximum amount of recently used entries guaranteed to
     * be in the Cache.
     * @throws IllegalArgumentException if maxRecent is less than or equal to
     * zero.
     */
    public LRUCache(Map backingMap, int maxRecent, int targetHitRatio, 
            int maxMemoryUsedPercent) {

        mMaxRecent = maxRecent;
        mBackingMap = backingMap;
        mUsageQueue = new ConcurrentLinkedList();
        mHits = new AtomicLong(0L);
        mMisses = new AtomicLong(0L);
        mSampleQueue = new LinkedBlockingQueue(DEFAULT_WINDOW_SIZE);
        mTargetHitRatio = targetHitRatio;
        mMaxMemoryUsedPercent = maxMemoryUsedPercent;
    }


    public LRUCache(Map backingMap, int maxRecent) {
        this(backingMap, maxRecent, DEFAULT_TARGET_HIT_RATIO,
            DEFAULT_MAX_MEMORY_USED_PERCENT);
    }


    /**
     * Construct a cache with an amount of recently used entries that are
     * guaranteed to always be in the cache.
     *
     * @param maxRecent maximum amount of recently used entries guaranteed to
     * be in the Cache.  A value of -2 enables auto-tune.
     */
    public LRUCache(int maxRecent) {
        this(new ConcurrentHashMap(), maxRecent);
    }

    
    public void integrityCheck() {
        mUsageQueue.integrityCheck();
        if (mUsageQueue.size() != mBackingMap.size())
            throw new IllegalArgumentException("Backing map and LRU queue sizes differ! " + 
                mBackingMap.size() + "/" + mUsageQueue.size());
    }


    public Object get(Object key) {
        Entry e = mBackingMap.get(key);
      
        if (e != null) {
            e.touch();
            mHits.getAndIncrement();
            return e.getValue();
        }
        else{
            mMisses.getAndIncrement();
            return null;
        }
    }


    public boolean containsKey(Object key) {
        return mBackingMap.containsKey(key);
    }


    public Object put(Object key, Object value) {
        
        if (value == null)
            value = new Null();

        mPutLock.lock();
        try {
    
            Entry e = new Entry(key, value);
            Entry old = mBackingMap.put(key, e);
            if (old != null) {
                old.dequeue();
            }
            e.enqueue();

            while (mMaxRecent > 0 && mBackingMap.size() > mMaxRecent) {
                Entry a = mUsageQueue.poll();           // get the LRU item.
                if (a == null){
                    break;
                }
                Entry expired = mBackingMap.remove(a.getKey());
                if (expired != null){
                    fireExpireEvent(expired);
                }
            } 

            // If mMaxRecent <= 0 then autotune is enabled
            if ((mMaxRecent <= 0 || mWasAutoTuned) && !mWasMemorySizeThresholdReached) {
                mWasAutoTuned = true;

                MutablePair sample = null;

                if (mSampleQueue.remainingCapacity() == 0)
                    sample = mSampleQueue.poll();
                else
                    sample = new MutablePair();

                sample.setHitRatio(getHitRatio());
                sample.setCacheSize(mBackingMap.size());
                mSampleQueue.offer(sample);

                double tangent = getTangent();
                if (tangent != Double.POSITIVE_INFINITY) {
                    int estimatedSize = estimateCacheSize(tangent);
                    if (estimatedSize < mBackingMap.size()) {
                        double sizeDelta = mBackingMap.size() - estimatedSize;
                        double sizeDeltaPct = sizeDelta / mBackingMap.size() * 100;
                        if (sizeDeltaPct < 100 - mTargetHitRatio) {  // Must be small downward change.
                            mMaxRecent = estimateCacheSize(tangent); 
                        }
          
                    }
                    else
                        mMaxRecent = estimatedSize;
                }

                if (getMemoryUsedPercent() >= mMaxMemoryUsedPercent) {
                    mMaxRecent = mBackingMap.size();    // Memory max threshold reached
                    mWasMemorySizeThresholdReached = true;
                }

            }
            
            
        }
        finally {
            mPutLock.unlock();
        }

        return value;
    }


    public int size() { return mBackingMap.size(); }


    public boolean isEmpty() { return mBackingMap.isEmpty(); }


    public boolean containsValue(Object value) { return mBackingMap.containsValue(value); }


    public Object remove(Object key) { 

        Object value = null;
        mPutLock.lock();
        try {
            Entry e = mBackingMap.remove(key);
            if (e != null){
                e.dequeue();
            }
        }
        finally {
            mPutLock.unlock();
        }
        return value;
    }

    
    public void putAll(Map m) { 
        for (Iterator<Map.Entry> i = m.entrySet().iterator(); i.hasNext(); )  {
           Map.Entry e = i.next();
           put(e.getKey(), e.getValue());
        }
    }


    public void clear() { 

        mPutLock.lock();
        try {
            mBackingMap.clear(); 
            mUsageQueue.clear();
        }
        finally {
            mPutLock.unlock();
        }
    }


    public Set keySet() { return mBackingMap.keySet(); }


    public Collection values() { 
        ArrayList l = new ArrayList();
        for (Map.Entry e : mBackingMap.entrySet()) 
            l.add(((Entry) e.getValue()).getValue());
        return l;
    }


    public Set entrySet() { 
        HashSet s = new HashSet(mBackingMap.size());
        for (Map.Entry e : mBackingMap.entrySet()) 
            s.add(new Entry(e.getKey(), ((Entry) e.getValue()).getValue()));
        return s; 
    }

    
    public boolean equals(Object o) { return mBackingMap.equals(o); }


    public int hashCode() { return mBackingMap.hashCode(); }


    public boolean addListener(ExpirationListener l) { return mListeners.add(l); }


    public boolean removeListener(ExpirationListener l) { return mListeners.remove(l); }


    protected void fireExpireEvent(Entry e) {
      for(int i = 0;i < mListeners.size(); i++)
            expireExecutor.execute(new NotifyBuffer(mListeners.get(i), e));
    }


    private class NotifyBuffer implements Runnable {
        private Entry mEntry = null;
        private ExpirationListener mListener = null;
        NotifyBuffer(ExpirationListener l, Entry e) { mEntry = e; mListener = l; }
        public void run() {
            mListener.expireEvent(mEntry);
        }
    }


    static class Null {
        public boolean equals(Object other) {
            return other == null || other instanceof Null;
        }

        public String toString() {
            return "null";
        }
    }
    

    /**
     * The Entry class encapsulates a link between an item in the backing map
     * and the LRU ordered queue.  When an entry is accessed via get() it is removed
     * and placed on the tail of the queue via the touch() method.
     */
    class Entry implements Map.Entry {

        private Object mKey;
        private Object mValue;
        private volatile ConcurrentLinkedList.Node mQueueEntry = null;

        public Entry(Object key, Object value) {
            mKey = key;
            mValue = value;
        }


        public ConcurrentLinkedList.Node getQueueEntry() { return mQueueEntry; }
        public Object getKey() { return mKey; }
        public Object getValue() { return mValue; }
        public Object setValue(Object value) { 
            Object old = mValue;
            mValue = value; 
            return old;
        }


        public void enqueue() {
            mQueueEntry = mUsageQueue.offerAndGetNode(this);
        }


        public boolean dequeue() {
            return mUsageQueue.remove(mQueueEntry);
        }


        /**
         * Place this entry on the tail of the LRU.
         */
        public void touch() { 
            mUsageQueue.moveToTail(mQueueEntry);
        }
        public int hashCode() { return mValue != null ? mValue.hashCode() : -1; }
        public boolean equals(Object other) { 
            if (mValue == null && other == null)
                return true;
            return mValue != null ? mValue.equals(other) : false; 
        }
 
    }


    public interface ExpirationListener {
        /**
         * This event occurs when an entry expires (maxRecent reached) based upon an LRU policy.
         */
        public void expireEvent(Entry e);
    }


    // the purpose of this class is to help reduce heap churn when auto-tune is enabled.
    static class MutablePair {
        
        double mHitRatio;
        int mCacheSize;

        MutablePair () { 
            mHitRatio = 0; 
            mCacheSize = 0;
        }

        MutablePair(double hitRatio, int cacheSize) { 
            mHitRatio = hitRatio; 
            mCacheSize = cacheSize;
        }

        double getHitRatio() { return mHitRatio; }
        void setHitRatio(double hitRatio) { mHitRatio = hitRatio; }

        int getCacheSize() { return mCacheSize; }
        void setCacheSize(int cacheSize) { mCacheSize = cacheSize; }

    }

}
