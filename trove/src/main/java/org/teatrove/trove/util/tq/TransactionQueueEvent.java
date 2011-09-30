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

import java.util.EventObject;
import java.util.Date;

/**
 * An event that contains information from a {@link TransactionQueue}.
 * TransactionQueueEvents can be received by implementing a
 * {@link TransactionQueueListener}.
 *
 * @author Brian S O'Neill
 */
public class TransactionQueueEvent extends EventObject {
    private Transaction mTransaction;
    private Throwable mThrowable;
    private long mTimestampMillis;
    private long mStageDuration;

    private transient Date mTimestamp;

    public TransactionQueueEvent(TransactionQueue source, 
                                 Transaction transaction) {
        super(source);
        mTransaction = transaction;
        mTimestampMillis = System.currentTimeMillis();
    }

    public TransactionQueueEvent(TransactionQueue source, 
                                 Transaction transaction,
                                 Throwable throwable) {
        super(source);
        mTransaction = transaction;
        mThrowable = throwable;
        mTimestampMillis = System.currentTimeMillis();
    }

    /**
     * Copies the given event, but gives it a new timestamp and computes a
     * stage duration.
     */
    public TransactionQueueEvent(TransactionQueueEvent event) {
        super(event.getSource());
        mTransaction = event.mTransaction;
        mThrowable = event.mThrowable;
        mStageDuration = (mTimestampMillis = System.currentTimeMillis()) -
            event.mTimestampMillis;
    }

    /**
     * Copies the given event, but gives it a different throwable, a new
     * timestamp and computes a stage duration.
     */
    public TransactionQueueEvent(TransactionQueueEvent event,
                                 Throwable throwable) {
        super(event.getSource());
        mTransaction = event.mTransaction;
        mThrowable = throwable;
        mStageDuration = (mTimestampMillis = System.currentTimeMillis()) -
            event.mTimestampMillis;
    }

    public TransactionQueue getTransactionQueue() {
        return (TransactionQueue)getSource();
    }

    /**
     * Returns the date and time of this event.
     */
    public Date getTimestamp() {
        if (mTimestamp == null) {
            mTimestamp = new Date(mTimestampMillis);
        }
        return mTimestamp;
    }

    /**
     * Returns the date and time of this event as milliseconds since 1970.
     */
    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    /**
     * Returns the amount of time, in milliseconds, that the transaction was
     * in a TransactionQueue stage. The implied meaning of this value depends
     * on how this event was passed to a listener.
     *
     * @see TransactionQueueListener
     */
    public long getStageDuration() {
        return mStageDuration;
    }

    /**
     * Returns the transaction that was being processed by the
     * TransactionQueue.
     */
    public Transaction getTransaction() {
        return mTransaction;
    }

    /**
     * Returns the exception that occurred while servicing the transaction, if
     * applicable.
     */
    public Throwable getThrowable() {
        return mThrowable;
    }
}
