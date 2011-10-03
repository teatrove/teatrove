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

package org.teatrove.teaservlet;

import org.teatrove.trove.util.MergedClass;

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 
 * @author Guy Molinari
 *
 * This class functions as the mediator for handling all profiler statistics
 * gathering and event handling.
 */
public class TeaServletInvocationStats implements MergedClass.InvocationEventObserver {

    private static TeaServletInvocationStats mInstance = null;
    private Map mStatsMap = new ConcurrentHashMap(2000);


    public static TeaServletInvocationStats getInstance() {
        if (mInstance == null)
            mInstance = new TeaServletInvocationStats();
        return mInstance;
    }


    public Map getStatisticsMap() {
        return mStatsMap;
    }

    public void reset() {
        mStatsMap = new ConcurrentHashMap(2000);
    }

    public void reset(int initialMapSize) {
        mStatsMap = new ConcurrentHashMap(initialMapSize);
    }
    
    public void reset(String caller, String callee) {
        Set keySet = mStatsMap.keySet();
        Iterator iter = keySet.iterator();
        while (iter.hasNext()) {
        	Stats stat = (Stats) iter.next();
        	if (stat.getCaller().equals(caller)) {
        		mStatsMap.remove(stat);
        	}
        }
    }

    public Stats getStatistics(String caller, String callee) {
        Stats l = new Stats(caller, callee);
        return (Stats) mStatsMap.get(l);
    }

    public void invokedEvent(String caller, String callee, long elapsedTime) {
        Stats l = new Stats(caller, callee);
        Stats stats = (Stats) mStatsMap.get(l);
        if (stats == null) {
            stats = l;
            mStatsMap.put(stats, stats);
        }
        stats.addCumServiceTime(elapsedTime);
        if (elapsedTime > stats.getPeakServiceDuration())
            stats.setPeakServiceDuration(elapsedTime);
        stats.incServicedCount();
    }


    public long currentTime() {
        return System.nanoTime();
    }


    public class Stats {
        private AtomicLong mPeakServiceDuration = new AtomicLong(0L);
        private AtomicLong mCumServiceTime = new AtomicLong(0L);
        private AtomicLong mServicedCount = new AtomicLong(0L);
        private String mCallee = null;
        private String mCaller = null;
        public Stats(String caller, String callee) {
            mCaller = caller;
            mCallee = callee;
        }
        public String getCaller() { return mCaller; }
        public String getCallee() { return mCallee; }
        public long getServicedCount() { return mServicedCount.get(); }
        public void incServicedCount() { mServicedCount.getAndIncrement(); }
        public void setServicedCount(long servicedCount) { mServicedCount.set(servicedCount); }
        public long getCumServiceTime() { return mCumServiceTime.get(); }
        public long addCumServiceTime(long elapsedTime) { return mCumServiceTime.getAndAdd(elapsedTime); }
        public void setCumServiceTime(long cumServiceTime) { mCumServiceTime.set(cumServiceTime); }
        public double getAverageServiceDuration() { 
            double avg = mServicedCount.get() == 0L ? (double) mCumServiceTime.get() : 
                (double) (mCumServiceTime.get() / mServicedCount.get());
            return avg / 1000000;
        }
        public double getPeakServiceDurationAsDouble() { return mPeakServiceDuration.get() / 1000000; }
        public long getPeakServiceDuration() { return mPeakServiceDuration.get() / 1000000; }
        public void setPeakServiceDuration(long peakServiceDuration) { 
            mPeakServiceDuration.lazySet(peakServiceDuration); 
        }

        public boolean equals(Object o) {
            if (! (o instanceof Stats) || o == null)
                return false;
            Stats a = (Stats) o;
            if (getCaller() == null && a.getCaller() == null && getCallee() == null && a.getCallee() == null)
                return true;
            if (getCaller() == null && a.getCaller() != null)
                return false;
            if (getCallee() == null && a.getCallee() != null)
                return false;
            if (getCaller() != null && ! getCaller().equals(a.getCaller()))
                return false;
            if (getCallee() != null && ! getCallee().equals(a.getCallee()))
                return false;
            return true;
        }

        public int hashCode() {
           int caller = mCaller == null ? -1 : mCaller.hashCode();
           int callee = mCallee == null ? -1 : mCallee.hashCode();
           return caller + callee;
        }

        public int compareTo(Object o) {
            Stats other = (Stats) o;
            if (equals(other))
                return 0;
            if (getCallee() != null && getCallee().equals(other.getCallee())) {
                if (getCaller() == null && other.getCaller() != null)
                    return -1;
                else if (other.getCaller() == null)
                    return 1;
                else
                    return getCaller().compareTo(other.getCaller());
            }
            return -1;
            
        }
    }

}
