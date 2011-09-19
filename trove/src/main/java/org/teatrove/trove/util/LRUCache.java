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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * LRUCache is a specialized Map that is guaranteed to have the most recently 
 * used entries available. Calling "get" or "put" updates the internal MRU list,
 * but calling "containsKey" or "containsValue" will not.
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
 * @author Alex Vigdor
 * @author Greg Katz
 * @author Guy Molinari
 */
public class LRUCache implements Map {

    private Map<Object, Entry> mBackingMap;

    private PriorityBlockingQueue<QueueEntry> mUsageQueue;
    private int mMaxRecent;
    private int mTargetHitRatio = 85;   // RMS smoothed target
    private int mMaxMemoryUsedPercent = 80;
    private LinkedBlockingQueue<Double> mSampleQueue;

    private AtomicLong mHits;
    private AtomicLong mMisses;

    // In-progress removes
    private boolean mRemoving = false;

    // In-progress puts
    private boolean mPutting = false;
    
    // In-progress clears
    private boolean mClearing = false;

    // Manages expireEvent listeners.
    private ArrayList<ExpirationListener> mListeners = new ArrayList();

    /**
     * Construct a cache with an amount of recently used entries that are
     * guaranteed to always be in the cache.
     *
     * @param maxRecent maximum amount of recently used entries guaranteed to
     * be in the Cache.
     * @throws IllegalArgumentException if maxRecent is less than or equal to
     * zero.
     */
    public LRUCache(int maxRecent) {
        this(new ConcurrentHashMap(maxRecent), maxRecent);
    }

    
    public int getTargetHitRatio() {
        return mTargetHitRatio;
    }


    public void setTargetHitRatio(int target) {
        mTargetHitRatio = target;
    }


    public int getMaxMemoryUsedPercent() {
        return mMaxMemoryUsedPercent;
    }


    public void setMaxMemoryUsedPercent(int maxMemory) {
        mMaxMemoryUsedPercent = maxMemory;
    }


    public long getHits() {
        return mHits.get();
    }


    public long getMisses() {
        return mMisses.get();
    }


    public int getHitRatio() {
        if (mHits.get() + mMisses.get() == 0 )
            return 0;
        return (int) ((1 - (mMisses.doubleValue() / (mHits.doubleValue() + mMisses.doubleValue()))) * 100);
    }

    
    /**
     * Calculate the RMS smooted hit ratio.
     */
    public int getRMSHitRatio() {

        if (mSampleQueue.remainingCapacity() != 0)
            return 0;

        int size = mSampleQueue.size();
        double sum = 0;
        for (Double sample : mSampleQueue)
            sum += sample * sample;
        return (int) Math.sqrt(sum / size);

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
    public LRUCache(Map backingMap, int maxRecent) {
        mMaxRecent = maxRecent;
        mBackingMap = backingMap;
        mUsageQueue = new PriorityBlockingQueue();
        mHits = new AtomicLong(0L);
        mMisses = new AtomicLong(0L);
        mSampleQueue = new LinkedBlockingQueue<Double>(100);
    }


    public Object get(Object key) {

        Entry e = mBackingMap.get(key);
        Object value = e != null ? e.getValue() : null;
        
        if (e != null) {
            e.touch();
            mHits.getAndIncrement();
        }
        else
            mMisses.getAndIncrement();

        return value;
    }


    public boolean containsKey(Object key) {
        return mBackingMap.containsKey(key);
    }


    public Object put(Object key, Object value) {
        if (value == null) {
            value = new Null();
        }

        Entry e = new Entry(key, value);
        QueueEntry qe = e.getQueueEntry();

        synchronized(this) {
            while(mPutting || mRemoving || mClearing)
               try { wait(); } catch (InterruptedException ignore) { }
            mPutting = true;
        }

        try {
            Entry old = mBackingMap.put(key, e);
            if (old != null)
                mUsageQueue.remove(old.getQueueEntry());
    
            mUsageQueue.offer(qe);

            // If mMaxRecent <= 0 then autotune is enabled
            if (mMaxRecent <= 0) {
                while (!mSampleQueue.offer(new Double(getHitRatio())))
                    mSampleQueue.poll();

                if (getMemoryUsedPercent() >= mMaxMemoryUsedPercent)
                    mMaxRecent = mBackingMap.size();    // Memory max threshold reached
                if (getRMSHitRatio() >= mTargetHitRatio)
                    mMaxRecent = mBackingMap.size();    // Target hit ratio reached
/*
if (mMaxRecent > 0) {
System.out.println("***** AUTOTUNE TARGET REACHED max = " + mMaxRecent + " hit ratio = " + getRMSHitRatio() + 
    "% Memory = " + getMemoryUsedPercent() + "%");
System.out.println("MAX MEMORY = " + Runtime.getRuntime().maxMemory() + " FREE MEMORY = " + Runtime.getRuntime().freeMemory() +
    " TOTAL MEMORY = " + Runtime.getRuntime().totalMemory());
}
*/
            }
    
            if (mMaxRecent > 0 && mBackingMap.size() > mMaxRecent) {
                QueueEntry a = findLRU(mUsageQueue.poll());

                if (a != null) {
                    Entry expired = mBackingMap.remove(a.getKey());
                    fireExpireEvent(expired);
                }
            }
        }
        finally {
            synchronized(this) {
                mPutting = false;
                notifyAll();
            }
        }

        return value;
    }


    public int size() { return mBackingMap.size(); }


    public boolean isEmpty() { return mBackingMap.isEmpty(); }


    public boolean containsValue(Object value) { return mBackingMap.containsValue(value); }


    public Object remove(Object key) { 

        synchronized(this) {
            while(mRemoving || mPutting || mClearing)
               try { wait(); } catch (InterruptedException ignore) { }
            mRemoving = true;
        }

        Object value = null;
        try {
            Entry e = mBackingMap.remove(key);
            value = e != null ? e.getValue() : null;
            QueueEntry qe = e != null ? e.getQueueEntry() : null;
            if (qe != null)
                mUsageQueue.remove(qe);
        }
        finally {
            synchronized(this) {
                mRemoving = false;
                notifyAll();
            }
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
        synchronized(this) {
            while(mClearing || mRemoving || mPutting)
               try { wait(); } catch (InterruptedException ignore) { }
            mClearing = true;
        }

        try {
            mBackingMap.clear(); 
            mUsageQueue.clear();

        }
        finally {
            synchronized(this) {
                mClearing = false;
                notifyAll();
            }
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
        for (ExpirationListener l : mListeners)
            new Thread(new NotifyBuffer(l, e)).start();
    }


    /**
     * Check to see if the timestamp on the queue entry is the same as the
     * last access timestamp.  If they differ then the item was accessed
     * "touched" and should be enqueued again with the last access 
     * timestamp.
     */
    private QueueEntry findLRU(QueueEntry e) {

        if (e == null)
            throw new IllegalArgumentException("QueueEntry must not be null.");
        if (e.getKey() == null)
            throw new IllegalArgumentException("QueueEntry.getKey() must not be null.");
        if (e.getValue() == null)
            throw new IllegalArgumentException("QueueEntry.getValue() must not be null.");

        Entry check = mBackingMap.get(e.getKey());
        if (check == null)
            throw new IllegalArgumentException("QueueEntry.getKey() not found in backing map.");

        // If the timestamps are equal then expire this entry.
        if (check.getTimeStamp().equals(e.getTimeStamp()))
            return e; 

        // Timestamps differ so re-enqueue the item and look for another entry.
        int len = mUsageQueue.size();
        int count = 0;
        do {
            e.setTimeStamp(check.getTimeStamp());
            mUsageQueue.offer(e);
            e = mUsageQueue.poll();
            check = mBackingMap.get(e.getKey());
            count++;
        } while ((! check.getTimeStamp().equals(e.getTimeStamp())) && count < len);

        return e;
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
     * The Entry class manages a timestamp that initially represents the
     * arrival time of the entry in the queue.  When an item is retrieved
     * via a call to get(), the timestamp is updated (touched) to the
     * last access time.
     */
    class Entry implements Map.Entry {

        private Object mKey;
        private AtomicLong mTimeStamp = null;
        private Object mValue;
        private QueueEntry mQueueEntry = null;

        public Entry(Object key, Object value) {
            mKey = key;
            mValue = value;
            mTimeStamp = new AtomicLong(System.nanoTime());
            mQueueEntry = new QueueEntry(this);
        }

        public QueueEntry getQueueEntry() { return mQueueEntry; }
        public Long getTimeStamp() { return mTimeStamp.get(); }
        public Object getKey() { return mKey; }
        public Object getValue() { return mValue; }
        public Object setValue(Object value) { 
            Object old = mValue;
            mValue = value; 
            return old;
        }
        public void touch() { mTimeStamp.lazySet(System.nanoTime()); }
        public int hashCode() { return mValue != null ? mValue.hashCode() : -1; }
        public boolean equals(Object other) { 
            if (mValue == null && other == null)
                return true;
            return mValue != null ? mValue.equals(other) : false; 
        }
 
    }


    /**
     * The QueueEntry class manages a timestamp that represents the
     * arrival time of the entry in the queue. 
     */
    class QueueEntry implements Comparable {

        Entry mEntry = null;
        private AtomicLong mTimeStamp = null;

        public QueueEntry(Entry e) {
            mEntry = e;
            mTimeStamp = new AtomicLong(e.getTimeStamp());
        }

        public Long getTimeStamp() { return mTimeStamp.get(); }
        public void setTimeStamp(Long timeStamp) { mTimeStamp.set(timeStamp); }
        public Object getKey() { return mEntry.getKey(); }
        public Object getValue() { return mEntry.getValue(); }

        public int compareTo(Object o) {
            QueueEntry a = (QueueEntry) o;
            return getTimeStamp().compareTo(a.getTimeStamp());
        }

        public int hashCode() { return getTimeStamp() != null ? getTimeStamp().hashCode() : -1; }

        public boolean equals(Object other) {
            if (getTimeStamp() == null && other != null && ((QueueEntry) other).getTimeStamp() == null)
                return true;
            return getTimeStamp() != null ? getTimeStamp().equals(((QueueEntry) other).getTimeStamp()) : false;
        }
    }


    public interface ExpirationListener {
        /**
         * This event occurs when an entry expires (maxRecent reached) based upon an LRU policy.
         */
        public void expireEvent(Entry e);
    }

}
