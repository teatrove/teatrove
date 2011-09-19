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

package org.teatrove.barista.servlet;

import org.teatrove.trove.util.ThreadPool;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.TransactionQueueData;

/**
 * The TQTeaServletTransactionQueue class adds support for tracking
 * template execution time.  This data will be displayed in the Barista
 * administration pages.
 * 
 * @author Reece Wilton
 */
public class TQTeaServletTransactionQueue extends TransactionQueue {

	private long mTotalTemplateDuration = 0;
	private long mTotalTemplateExecutions = 0;
        private int mQueueWarnThreshold = 0;

	public TQTeaServletTransactionQueue(ThreadPool tp, int maxSize,	
	                                    int maxThreads) {
		super(tp, maxSize, maxThreads);
	}

	public TQTeaServletTransactionQueue(ThreadPool tp, String name,
	                                    int maxSize, int maxThreads) {
		super(tp, name, maxSize, maxThreads);
	}

	/**
	 * Returns a snapshot of the statistics on this TransactionQueue.
	 */
	public synchronized TransactionQueueData getStatistics() {
	
		TransactionQueueData data = super.getStatistics();
		return new TQTeaServletTransactionQueueData(data,
		                                            mTotalTemplateDuration,
		                                            mTotalTemplateExecutions,
                                                            mQueueWarnThreshold);
	}
	
	/**
	 * Resets all time lapse statistics.
	 */
	public synchronized void resetStatistics() {
	
		super.resetStatistics();
		
		mTotalTemplateDuration = 0;
		mTotalTemplateExecutions = 0;
	}
	
	public synchronized void addTemplateDuration(long templateDuration) {
		
		mTotalTemplateDuration += templateDuration;
		mTotalTemplateExecutions++;		
	}

        public synchronized void setQueueWarnThreshold(int value) {
                mQueueWarnThreshold = value;
        }
}
