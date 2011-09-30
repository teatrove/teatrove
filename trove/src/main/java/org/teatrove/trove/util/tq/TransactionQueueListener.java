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

import java.util.EventListener;

/**
 * Interface used to receive events from a {@link TransactionQueue}.
 *
 * @author Brian S O'Neill
 * @see TransactionQueueAdapter
 */
public interface TransactionQueueListener extends EventListener {
    /**
     * Called if the transaction couldn't be enqueued because the queue is
     * full. The transaction is not cancelled.
     *
     * <p>The stage duration value in the event is zero.
     */
    public void transactionQueueFull(TransactionQueueEvent e);

    /**
     * Called when the transaction is successfully enqueued.
     *
     * <p>The stage duration value in the event is zero.
     */
    public void transactionEnqueued(TransactionQueueEvent e);

    /**
     * Called when the transaction is dequeued and ready to be serviced.
     *
     * <p>The stage duration value in the event represents the amount of time
     * the transaction was queued.
     */
    public void transactionDequeued(TransactionQueueEvent e);

    /**
     * Called after the transaction has been serviced, unless an exception
     * was thrown while trying to service the transaction.
     *
     * <p>The stage duration value in the event represents the amount of time
     * the transaction took to service.
     */
    public void transactionServiced(TransactionQueueEvent e);

    /**
     * Called if a transaction couldn't be serviced because it expired.
     * The transaction is cancelled.
     *
     * <p>The stage duration value in the event represents the age of the
     * expired transaction.
     */
    public void transactionExpired(TransactionQueueEvent e);

    /**
     * Called if an exception was thrown while trying to service the
     * transaction. The transaction is cancelled.
     *
     * <p>The stage duration value in the event represents the amount of time
     * that passed servicing the transaction before an exception was thrown and
     * after the transaction is cancelled.
     */
    public void transactionException(TransactionQueueEvent e);
}
