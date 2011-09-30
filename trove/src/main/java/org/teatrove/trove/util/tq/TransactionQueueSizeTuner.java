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

/**
 * Automatically tunes a TransactionQueue by dynamically adjusting the maximum
 * size of the queue. TransactionQueueSizeTuner only works for 
 * TransactionQueues that have a transaction timeout, and a
 * TransactionQueueSizeTuner instance should only be added to one
 * TransactionQueue.
 * 
 * @author Brian S O'Neill
 */
public class TransactionQueueSizeTuner extends TransactionQueueAdapter {
    public void transactionQueueFull(TransactionQueueEvent e) {
        TransactionQueue queue = e.getTransactionQueue();
        long timeout = queue.getTransactionTimeout();
        if (timeout >= 0) {
            queue.setMaximumSize(queue.getMaximumSize() + 1);
        }
    }

    public void transactionExpired(TransactionQueueEvent e) {
        TransactionQueue queue = e.getTransactionQueue();
        int currentSize = queue.getMaximumSize();
        if (currentSize > 1) {
            queue.setMaximumSize(currentSize - 1);
        }
    }
}
