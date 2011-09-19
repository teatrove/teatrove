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

import org.teatrove.trove.util.tq.TransactionQueueData;

/**
 * The TQTeaServletTransactionQueueData class adds support for tracking
 * template execution time.  This data will be displayed in the Barista
 * administration pages.
 * 
 * @author Reece Wilton
 */
public class TQTeaServletTransactionQueueData extends TransactionQueueData {

	private final long mTotalTemplateDuration;
	private final long mTotalTemplateExecutions;
        private int mQueueWarnThreshold = 0;

	public TQTeaServletTransactionQueueData(TransactionQueueData queueData,
	                                        long totalTemplateDuration,
	                                        long totalTemplateExecutions) {
	                       	
		super(queueData.getTransactionQueue(),
		      queueData.getSnapshotStart().getTime(),
		      queueData.getSnapshotEnd().getTime(),
		      queueData.getQueueSize(),
			  queueData.getThreadCount(),
			  queueData.getServicingCount(),
			  queueData.getPeakQueueSize(),
			  queueData.getPeakThreadCount(),
			  queueData.getPeakServicingCount(),
			  queueData.getTotalEnqueueAttempts(),
			  queueData.getTotalEnqueued(),
			  queueData.getTotalServiced(),
			  queueData.getTotalExpired(),
			  queueData.getTotalServiceExceptions(),
			  queueData.getTotalUncaughtExceptions(),
			  queueData.getTotalQueueDuration(),
			  queueData.getTotalServiceDuration());	
			  
		mTotalTemplateDuration = totalTemplateDuration;
		mTotalTemplateExecutions = totalTemplateExecutions;			
	}
	
	public TQTeaServletTransactionQueueData(TransactionQueueData queueData,
	                                        long totalTemplateDuration,
	                                        long totalTemplateExecutions,
	                                        int queueWarnThreshold) {
            this(queueData, totalTemplateDuration, totalTemplateExecutions);
            mQueueWarnThreshold = queueWarnThreshold;
        }

	/**
	 * Returns the total time, in milliseconds, that templates were being
	 * executed.
	 */
	public long getTotalTemplateDuration() {
		return mTotalTemplateDuration;
	}

	/**
	 * Returns the total number of template executions.
	 */
	public long getTotalTemplateExecutions() {
		return mTotalTemplateExecutions;
	}

	/**
	 * Returns the average amount of time, in milliseconds, it took executing 
	 * a template.
	 */
	public double getAverageTemplateDuration() {
		return getTotalTemplateExecutions() > 0 ?
			((double)getTotalTemplateDuration()) / 
			((double)getTotalTemplateExecutions()) : 0;
	}

	/**
	 * Returns a number between zero and one that is the ratio between template 
	 * duration and service duration.  Lower numbers mean more time is spent on
	 * things other than template execution.
	 */
	public double getTemplateDurationRatio() {
		return this.getAverageServiceDuration() > 0 ? 
                	((double)getAverageTemplateDuration()) / 
			((double)this.getAverageServiceDuration()) : 0;
	}

	/**
	 * Returns true if the template service duration exceeds the warning
         * threshold.
	 */
	public boolean getServiceDurationThresholdExceeded() {
		return mQueueWarnThreshold > 0 && 
                     getAverageTemplateDuration() > mQueueWarnThreshold;
	}
}
