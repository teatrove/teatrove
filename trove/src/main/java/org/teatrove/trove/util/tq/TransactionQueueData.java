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

import java.util.Date;

/**
 * This class contains a snapshot of data from a {@link TransactionQueue}.
 *
 * @author Brian S O'Neill
 */
public class TransactionQueueData implements java.io.Serializable {
    private transient final TransactionQueue mTransactionQueue;

    private final long mSnapshotStart;
    private final long mSnapshotEnd;
    private final int mQueueSize;
    private final int mThreadCount;
    private final int mServicingCount;
    private final int mPeakQueueSize;
    private final int mPeakThreadCount;
    private final int mPeakServicingCount;
    private final int mTotalEnqueueAttempts;
    private final int mTotalEnqueued;
    private final int mTotalServiced;
    private final int mTotalExpired;
    private final int mTotalServiceExceptions;
    private final int mTotalUncaughtExceptions;
    private final long mTotalQueueDuration;
    private final long mTotalServiceDuration;

    private transient Date mStartDate;
    private transient Date mEndDate;

    public TransactionQueueData(TransactionQueue tq,
                         long snapshotStart,
                         long snapshotEnd,
                         int queueSize,
                         int threadCount,
                         int servicingCount,
                         int peakQueueSize,
                         int peakThreadCount,
                         int peakServicingCount,
                         int totalEnqueueAttempts,
                         int totalEnqueued,
                         int totalServiced,
                         int totalExpired,
                         int totalServiceExceptions,
                         int totalUncaughtExceptions,
                         long totalQueueDuration,
                         long totalServiceDuration) {

        mTransactionQueue = tq;
        mSnapshotStart = snapshotStart;
        mSnapshotEnd = snapshotEnd;
        mQueueSize = queueSize;
        mThreadCount = threadCount;
        mServicingCount = servicingCount;
        mPeakQueueSize = peakQueueSize;
        mPeakThreadCount = peakThreadCount;
        mPeakServicingCount = peakServicingCount;
        mTotalEnqueueAttempts = totalEnqueueAttempts;
        mTotalEnqueued = totalEnqueued;
        mTotalServiced = totalServiced;
        mTotalExpired = totalExpired;
        mTotalServiceExceptions = totalServiceExceptions;
        mTotalUncaughtExceptions = totalUncaughtExceptions;
        mTotalQueueDuration = totalQueueDuration;
        mTotalServiceDuration = totalServiceDuration;
    }

    /**
     * Adds TransactionQueueData to another.
     */
    public TransactionQueueData add(TransactionQueueData data) {
        return new TransactionQueueData
            (null,
             Math.min(mSnapshotStart, data.mSnapshotStart),
             Math.max(mSnapshotEnd, data.mSnapshotEnd),
             mQueueSize + data.mQueueSize,
             mThreadCount + data.mThreadCount,
             mServicingCount + data.mServicingCount,
             Math.max(mPeakQueueSize, data.mPeakQueueSize),
             Math.max(mPeakThreadCount, data.mPeakThreadCount),
             Math.max(mPeakServicingCount, data.mPeakServicingCount),
             mTotalEnqueueAttempts + data.mTotalEnqueueAttempts,
             mTotalEnqueued + data.mTotalEnqueued,
             mTotalServiced + data.mTotalServiced,
             mTotalExpired + data.mTotalExpired,
             mTotalServiceExceptions + data.mTotalServiceExceptions,
             mTotalUncaughtExceptions + data.mTotalUncaughtExceptions,
             mTotalQueueDuration + data.mTotalQueueDuration,
             mTotalServiceDuration + data.mTotalServiceDuration
             );
    }

    /**
     * Returns the TransactionQueue source of this data, or null if not
     * applicable or missing.
     */
    public TransactionQueue getTransactionQueue() {
        return mTransactionQueue;
    }

    /**
     * Returns the date/time for when the snapshot started.
     */
    public Date getSnapshotStart() {
        if (mStartDate == null) {
            mStartDate = new Date(mSnapshotStart);
        }
        return mStartDate;
    }

    /**
     * Returns the date/time for when the snapshot ended.
     */
    public Date getSnapshotEnd() {
        if (mEndDate == null) {
            mEndDate = new Date(mSnapshotEnd);
        }
        return mEndDate;
    }

    /**
     * Returns the number of queued transactions at the snapshot end.
     */
    public int getQueueSize() {
        return mQueueSize;
    }

    /**
     * Returns the amount of worker threads in this TransactionQueue at the
     * snapshot end.
     */
    public int getThreadCount() {
        return mThreadCount;
    }

    /**
     * Returns the amount of transactions currently being serviced at the
     * snapshot end.
     */
    public int getServicingCount() {
        return mServicingCount;
    }

    /**
     * Returns the biggest queue size over the snapshot interval.
     */
    public int getPeakQueueSize() {
        return mPeakQueueSize;
    }

    /**
     * Returns the highest thread count over the snapshot interval.
     */
    public int getPeakThreadCount() {
        return mPeakThreadCount;
    }

    /**
     * Returns the highest servicing count over the snapshot interval.
     */
    public int getPeakServicingCount() {
        return mPeakServicingCount;
    }

    /**
     * Returns the total amount of transactions that were attempted to be
     * enqueued over the snapshot interval.
     */
    public int getTotalEnqueueAttempts() {
        return mTotalEnqueueAttempts;
    }

    /**
     * Returns the total amount of transactions that were enqueued over the
     * snapshot interval.
     */
    public int getTotalEnqueued() {
        return mTotalEnqueued;
    }

    /**
     * Returns the total amount of transactions serviced over the snapshot
     * interval.
     */
    public int getTotalServiced() {
        return mTotalServiced;
    }

    /**
     * Returns the total amount of expired transactions over the snapshot
     * interval.
     */
    public int getTotalExpired() {
        return mTotalExpired;
    }

    /**
     * Returns the number of transactions that were canceled because of an
     * uncaught exception while being serviced.
     */
    public int getTotalServiceExceptions() {
        return mTotalServiceExceptions;
    }

    /**
     * Returns the total number of uncaught exceptions in the TransactionQueue.
     * This value is usually the same as the total number of service
     * exceptions. If it is larger, this does necessarily not indicate that the
     * TransactionQueue has an internal error because exceptions can be
     * generated while attempting to cancel a transaction.
     */
    public int getTotalUncaughtExceptions() {
        return mTotalUncaughtExceptions;
    }

    /**
     * Returns the total time, in milliseconds, that transactions were
     * waiting in the queue.
     */
    public long getTotalQueueDuration() {
        return mTotalQueueDuration;
    }

    /**
     * Returns the total time, in milliseconds, that transactions were being
     * serviced.
     */
    public long getTotalServiceDuration() {
        return mTotalServiceDuration;
    }

    // Calculated data.

    /**
     * Returns the length of the snapshot interval in milliseconds.
     */
    public long getSnapshotDuration() {
        return mSnapshotEnd - mSnapshotStart;
    }

    /**
     * Returns the total amount of enqueue attempts that failed because the
     * queue was full.
     */
    public int getTotalEnqueueFailures() {
        return mTotalEnqueueAttempts - mTotalEnqueued;
    }

    /**
     * Returns the total amount of transactions that weren't serviced because
     * the queue was full, the transaction expired, or an exception was thrown.
     */
    public int getTotalUnserviced() {
        return getTotalEnqueueFailures() +
            mTotalExpired + mTotalServiceExceptions;
    }

    /**
     * Returns the average amount of time, in milliseconds, that a 
     * transaction was in the queue.
     */
    public double getAverageQueueDuration() {
        return ((double)getTotalQueueDuration()) / 
            ((double)getTotalEnqueued());
    }

    /**
     * Returns the average amount of time, in milliseconds, it took servicing 
     * a transaction.
     */
    public double getAverageServiceDuration() {
        return ((double)getTotalServiceDuration()) / 
            ((double)getTotalServiced());
    }

    /**
     * Returns the amount of enqueue attempts per second over the snapshot
     * interval.
     */
    public double getEnqueueAttemptRate() {
        return ((double)getTotalEnqueueAttempts() * 1000) /
            ((double)getSnapshotDuration());
    }

    /**
     * Returns the amount of successful transaction enqueues per second over
     * the snapshot interval.
     */
    public double getEnqueueSuccessRate() {
        return ((double)getTotalEnqueued() * 1000) / 
            ((double)getSnapshotDuration());
    }

    /**
     * Returns the amount of transactions serviced per second over the
     * snapshot interval.
     */
    public double getServiceRate() {
        return ((double)getTotalServiced() * 1000) / 
            ((double)getSnapshotDuration());
    }

    /**
     * Returns zero if no enqueues failed, one if all enqueues failed, or
     * a number in between if some failed.
     */
    public double getEnqueueFailureRatio() {
        return 1.0d - ((double)getTotalEnqueued()) / 
            ((double)getTotalEnqueueAttempts());
    }
}
