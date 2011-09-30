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
 * allowed number of worker threads. A TransactionQueueThreadTuner instance
 * should only be added to one TransactionQueue.
 * <p>
 * NOTE: The current implementation of TransactionQueueThreadTuner is
 * experimental and should not be used in production systems.
 * 
 * @author Brian S O'Neill
 */
public class TransactionQueueThreadTuner extends TransactionQueueAdapter {
    private long mLastQueueTime;
    private long mLastServiceTime;

    private long mTotalQueueDelta;
    private long mTotalServiceDelta;

    private boolean mBaton;

    public synchronized void transactionDequeued(TransactionQueueEvent e) {
        long queueTime = e.getStageDuration();
        mTotalQueueDelta += queueTime - mLastQueueTime;
        mLastQueueTime = queueTime;

        if (!mBaton) {
            mBaton = true;
            tune(e);
        }
    }

    public synchronized void transactionServiced(TransactionQueueEvent e) {
        long serviceTime = e.getStageDuration();
        mTotalServiceDelta += serviceTime - mLastServiceTime;
        mLastServiceTime = serviceTime;

        if (mBaton) {
            mBaton = false;
            tune(e);
        }
    }

    private void tune(TransactionQueueEvent e) {
        if (mTotalQueueDelta > mTotalServiceDelta) {
            TransactionQueue queue = e.getTransactionQueue();
            int maxThreads = queue.getMaximumThreads();
            if (maxThreads <= queue.getThreadCount() * 2) {
                // Increase the maximum amount of threads.
                queue.setMaximumThreads(maxThreads + 1);
            }
        }
        else if (mTotalServiceDelta > mTotalQueueDelta) {
            TransactionQueue queue = e.getTransactionQueue();
            int maxThreads = queue.getMaximumThreads();
            if (maxThreads > 1) {
                // Decrease the maximum amount of threads.
                queue.setMaximumThreads(maxThreads - 1);
            }
        }
    }
}
