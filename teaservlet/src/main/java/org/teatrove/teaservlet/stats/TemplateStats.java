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

package org.teatrove.teaservlet.stats;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class keeps track of time series performance data for templates.
 * 
 * All data is stored in memory.
 * 
 * There are two different types of data:
 *   * raw data
 *   * aggregate data
 *   
 * The RawData keeps track of the most recent start and end times for
 * a template invocation in addition to their content lengths. This
 * data is kept in a circular queue of a specified rawWindowSize. Once
 * the rawWindowSize capacity is reached it creates and AggregateInterval
 * which contains a smaller set of descriptive statistics for this template.
 * 
 * In a similar fashion the aggregate intervals are stored in a circular
 * queue.
 * 
 * In both cases for raw data and the aggregate intervals when the window
 * size is reached the newest data overwrites the oldest data.
 * 
 * @author Scott Jappinen
 */
public class TemplateStats {
	
	protected String fullTemplateName;
	protected String templateName;
	protected String templatePath;
	
    private AtomicLong mServicing = new AtomicLong(0L);
	
	protected long mPeakServiceDuration = 0L;
	protected long mCumulativeServiceTime = 0L;
	protected long mServicedCount = 0L;
	
	protected int mRawWindowSize = 0;
	protected int mAggregateWindowSize = 0;
	
	protected int mCurrentRawIndex = 0;
	protected RawData[] mRawData;
	
	protected int mCurrentAggregateIndex = 0;
	protected AggregateInterval[] mAggregateIntervals = null;
	
	protected List<Milestone> mMilestones = null;
	
	public TemplateStats(String fullTemplateName, int rawWindowSize, int aggregateWindowSize) {
		assert(fullTemplateName != null);
		assert(rawWindowSize > 0);
		assert(aggregateWindowSize > 0);
		
		this.fullTemplateName = fullTemplateName;
		int lastPathIndex = fullTemplateName.lastIndexOf('.');
		if (lastPathIndex >= 0) {
			this.templateName = fullTemplateName.substring(lastPathIndex + 1);
			this.templatePath = fullTemplateName.substring(0, lastPathIndex);
		} else {
			this.templateName = fullTemplateName;
			this.templatePath = "";
		}
		this.mRawWindowSize = rawWindowSize;
		mRawData = new RawData[rawWindowSize];
		for (int i=0; i < mRawData.length; i++) 
			mRawData[i] = new RawData();
		
		this.mAggregateWindowSize = aggregateWindowSize;
		mAggregateIntervals = new AggregateInterval[aggregateWindowSize];
		for (int i=0; i < mAggregateIntervals.length; i++) 
			mAggregateIntervals[i] = new AggregateInterval();
		
		mMilestones = new ArrayList<Milestone>();
	}

	/** Log a template request.
	 * 
	 * @param startTime
	 * @param stopTime
	 * @param contentLength
	 * @param params
	 */
	public synchronized void log(long startTime, long stopTime, long contentLength, Object[] params) {
		long elapsedTime = (stopTime - startTime);
		mRawData[mCurrentRawIndex].set(startTime, stopTime, contentLength);
		mCumulativeServiceTime += elapsedTime;
        if (elapsedTime > mPeakServiceDuration) {
        	mPeakServiceDuration = elapsedTime;
        }
        mServicedCount++;
		if (mCurrentRawIndex == mRawWindowSize - 1) {
			long aggregateStartTime = -1;
			if (mCurrentAggregateIndex == 0) {
				if (mAggregateIntervals[mAggregateIntervals.length - 1].getStartTime() != -1) {
					aggregateStartTime = mAggregateIntervals[mAggregateIntervals.length - 1].getEndTime() + 1;
				}
			} else {
				aggregateStartTime = mAggregateIntervals[mCurrentAggregateIndex - 1].getEndTime() + 1;
			}
			mAggregateIntervals[mCurrentAggregateIndex].compute(mRawData, aggregateStartTime, -1);
			if (mCurrentAggregateIndex == mAggregateWindowSize - 1) {
				mCurrentAggregateIndex = 0;
			} else {
				mCurrentAggregateIndex++;
			}
			mCurrentRawIndex = 0;
		} else {
			mCurrentRawIndex++;
		}
	}
	
	/**
	 * Returns the name of the template these stats are for.
	 * 
	 * @return the template name.
	 */
	public String getFullTemplateName() {
		return fullTemplateName;
	}
	
	public String getTemplateName() {
		return templateName;
	}
	
	public String getTemplatePath() {
		return templatePath;
	}
    
    public long getNumberServicing() {
    	return mServicing.get();
    }
    
    public long incrementServicing() {
    	return mServicing.incrementAndGet();
    }
    
    public long decrementServicing() {
    	return mServicing.decrementAndGet();
    }
	
	/**
	 * Returns the total number of times this template has been called
	 * since reset or startup.
	 * 
	 * @return the service count.
	 */
    public long getServicedCount() { 
    	return mServicedCount;
    }

    /**
     * Returns the total time in ms. that this template has been service
     * since system start or reset.
     * 
     * @return the cumulative service time.
     */
    public long getCumulativeServiceTime() {
    	return mCumulativeServiceTime; 
    }
    
    //public long getRecordedDuration() {
	//    if ( stats.aggregateIntervals != null and stats.aggregateIntervals.length > 0 ) {
	//        snapshotDuration = currentTime-stats.aggregateIntervals[0].startTime;
	//    } else {
	//        snapshotDuration = currentTime-stats.aggregateIntervalForRawData.startTime;
	//    }
    //}

    /**
     * Returns the overall average performance of this template since 
     * system start or reset.
     * 
     * @return the average service duration.
     */
    public double getAverageServiceDuration() { 
        return mServicedCount == 0L ? (double) mCumulativeServiceTime : 
            (mCumulativeServiceTime / (mServicedCount * 1.0d));
    }

    /**
     * Returns the peak service duration.
     * 
     * @return the peak service duration as a double.
     */
    public double getPeakServiceDurationAsDouble() {
    	return mPeakServiceDuration * 1.0d;
    }

    /**
     * Returns the peak service duration in ms.
     * 
     * @return the peak service duration
     */
    public long getPeakServiceDuration() {
    	return mPeakServiceDuration;
    }

    /**
     * Returns the number of RawData values to keep before aggregating them.
     * 
     * @return the raw window size.
     */
	public int getRawWindowSize() {
		return mRawWindowSize;
	}
	
	/**
	 * Returns the number of aggregate windows to keep in memory in a 
	 * circular queue type fashion. Once the aggregate window size is
	 * reached then new entries overwrite the oldest entries.
	 * 
	 * @return the aggregate window size
	 */
	public int getAggregateWindowSize() {
		return mAggregateWindowSize;
	}

	/**
	 * Returns a copy of the raw data. Some of this data may have
	 * already been aggregated. The oldest entries are first in the
	 * array and the newest entries are last.
	 * 
	 * @return the raw data
	 */
	public synchronized RawData[] getRawData() {
		//return rawData;
		RawData[] result = null;
		// put things back in order
		//System.out.println("current raw index: " + mCurrentRawIndex + ", " + mRawData);
		if (mCurrentRawIndex == 0) {
			if (mRawData[0].startTime == -1) { // case 1 - empty
				result = null;
			} else { // case 2 full
				result = mRawData.clone();
			}
		} else if (mCurrentRawIndex > 0) {
			// case 2 - not empty - not full
			if (mRawData[mCurrentRawIndex].startTime == -1) {
				result = new RawData[mCurrentRawIndex];
				for (int i=0; i < result.length; i++) {
					result[i] = mRawData[i].clone();
				}
			} else { // case 3 - full & wrapping
				result = new RawData[mRawData.length];
				int j=0;
				for (int i=mCurrentRawIndex; i < mRawData.length; i++) {
					result[j++] = mRawData[i].clone();
				}
				
				for (int i=0; i < mCurrentRawIndex; i++) {
					result[j++] = mRawData[i].clone();
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the duration portion of the raw data as a double array.
	 * 
	 * @see TemplateStats.getRawData()
	 * 
	 * @return the raw durations.
	 */
	public double[] getRawDurations() {
		double[] result = null;
		RawData[] rawData = getRawData();
		if (rawData != null) {
			result = new double[rawData.length];
			for (int i=0; i < result.length; i++) {
				result[i] = rawData[i].getDuration();
			}
		}
		return result;
	}
	
	/**
	 * Returns the content lengths portion of the raw data as a double array.
	 * 
	 * @see TemplateStats.getRawData()
	 * 
	 * @return the raw content lengths.
	 */
	public double[] getRawContentLengths() {
		double[] result = null;
		RawData[] rawData = getRawData();
		if (rawData != null) {
			result = new double[rawData.length];
			for (int i=0; i < result.length; i++) {
				result[i] = rawData[i].getContentLength();
			}
		}
		return result;
	}
	
	/**
	 * Returns durations per content length from the raw data as a double array.
	 * 
	 * @see TemplateStats.getRawData()
	 * 
	 * @return durations per content length.
	 */
	public double[] getRawDurationsPerContentLength() {
		double[] result = null;
		RawData[] rawData = getRawData();
		if (rawData != null) {
			result = new double[rawData.length];
			for (int i=0; i < result.length; i++) {
				result[i] = rawData[i].getDuration()/rawData[i].getContentLength();
			}
		}
		return result;
	}
	
	/**
	 * Returns a histogram of the durations from the raw data.
	 * 
	 * @return a duration histogram
	 */
	public Histogram getRawDurationHistogram() {
		Histogram result = null;
		double[] data = getRawDurations();
		if (data != null) {
			Arrays.sort(data);
			result = new Histogram(data);
		}
		return result;
	}
	
	/**
	 * Returns a histogram of the content lengths from the raw data.
	 * 
	 * @return a content length histogram.
	 */
	public Histogram getRawContentLengthHistogram() {
		Histogram result = null;
		double[] data = getRawContentLengths();
		if (data != null) {
			Arrays.sort(data);
			result = new Histogram(data);
		}
		return result;
	}
	
	/**
	 * Returns a raw durations per content histogram.
	 * 
	 * @return a histogram of the durations per content length
	 */
	public Histogram getRawDurationsPerContentLengthHistogram() {
		Histogram result = null;
		double[] data = getRawDurationsPerContentLength();
		if (data != null) {
			Arrays.sort(data);
			result = new Histogram(data);
		}
		return result;
	}
	
	/**
	 * Returns the latest service time for this template.
	 * 
	 * @return the last time this template was invoked and completed
	 */
	public long getLatestServiceTime() {
	    RawData[] rawData = getRawData();
	    if (rawData != null && rawData.length > 0) {
	        return rawData[rawData.length - 1].getEndTime();
	    }
	    
	    return -1;
	}
	
	/**
     * Returns an AggregateInterval for the current raw data over the last
     * 60 seconds.
     * 
     * The aggregate interval that is created is not stored and
     * is only created for the caller.
     * 
     * @return the aggregate interval for the raw data over 60 seconds.
     */
    public AggregateInterval getLatestAggregateIntervalForRawData() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 60000;
        return getAggregateIntervalForRawData(startTime, endTime);
    }
    
	/**
	 * Returns an AggregateInterval for the current raw data.
	 * 
	 * The aggregate interval that is created is not stored and
	 * is only created for the caller.
	 * 
	 * @return the aggregate interval for the raw data.
	 */
	public AggregateInterval getAggregateIntervalForRawData() {
		AggregateInterval result = null;
		RawData[] rawData = getRawData();
		if (rawData != null) {
			result = new AggregateInterval();
			result.compute(rawData, rawData[0].startTime, -1);
		}
		return result;
	}
	
    /**
	 * Returns an aggregate interval for the raw data filtered 
	 * by start and stop time.
	 * 
	 * @see TemplateStats.getAggregateIntervalForRawData()
	 * 
	 * @param startTime the start time to filter on.
	 * @param stopTime the stop time to filter on.
	 * @return an aggregate interval for the raw data.
	 */
    public AggregateInterval getAggregateIntervalForRawData(long startTime, 
    														long stopTime)
    {
    	AggregateInterval result = null;
		RawData[] rawData = getRawData();
		if (rawData != null) {
			result = new AggregateInterval();
			List<RawData> list = new ArrayList<RawData>();
			for (int i=0; i < rawData.length; i++) {
				if (rawData[i].startTime >= startTime && 
				    rawData[i].endTime <= stopTime) {
				    
					list.add(rawData[i]);
				}
			}
			if (list.size() > 0) {
				result = new AggregateInterval();
				rawData = (RawData[]) list.toArray(new RawData[list.size()]);
				result.compute(rawData, startTime, stopTime);
			}
		}
    	return result;
    }

	/**
	 * Returns a copy of the aggregate intervals in the system
	 * The number of aggregate intervals will be less than or equal
	 * to the aggregate interval window size.
	 * 
	 * The oldest entries are at the beginning and the newest at the end.
	 * 
	 * @return the aggregate intervals for this template.
	 */
	public synchronized AggregateInterval[] getAggregateIntervals() {
		AggregateInterval[] result = null;
		// put things back in order
		if (mCurrentAggregateIndex == 0) {
			if (mAggregateIntervals[0].startTime == -1) { // case 1 - empty
				result = null;
			} else { // case 2 full
				result = mAggregateIntervals.clone();
			}
		} else if (mCurrentAggregateIndex > 0) {
			// case 2 - not empty - not full
			if (mAggregateIntervals[mCurrentAggregateIndex].startTime == -1) {
				result = new AggregateInterval[mCurrentAggregateIndex];
				for (int i=0; i < result.length; i++) {
					result[i] = mAggregateIntervals[i].clone();
				}
			} else { // case 3 - full & wrapping
				result = new AggregateInterval[mAggregateIntervals.length];
				int j=0;
				for (int i=mCurrentAggregateIndex; i < mAggregateIntervals.length; i++) {
					result[j++] = mAggregateIntervals[i].clone();
				}
				
				for (int i=0; i < mCurrentAggregateIndex; i++) {
					result[j++] = mAggregateIntervals[i].clone();
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Returns the aggregate intervals for the specified startTime and stopTime.
	 * Any intervals that contain these two endpoints lie between them will be
	 * included.
	 * 
	 * @param startTime the start time to filter on.
	 * @param stopTime the stop time to filter on.
	 * 
	 * @return aggregate intervals for the specified interval.
	 */
	public AggregateInterval[] getAggregateIntervals(long startTime, long stopTime) {
		assert(stopTime >= startTime);
		AggregateInterval[] result = null;
		AggregateInterval[] allAggregateIntervals = getAggregateIntervals();
		if (allAggregateIntervals != null) {
			// make sure the interval your looking for 
			if (startTime > allAggregateIntervals[allAggregateIntervals.length].getStartTime()) return null;
			if (stopTime < allAggregateIntervals[0].getStartTime()) return null;
			
			int startIndex = search(allAggregateIntervals, startTime);
			int stopIndex = search(allAggregateIntervals, stopTime);
			
			int length = 0;
			
			if (startIndex == -1) { // comes before aggregate starts
				if (stopIndex == -1) { // comes after aggregate ends
					// return everything
					length = allAggregateIntervals.length;
				} else {
					length = stopIndex + 1;
					startIndex = 0;
				}
			} else if (stopIndex == -1) {  // comes after aggregate ends
				length = allAggregateIntervals.length - startIndex;
			} else {
				assert(stopIndex >= startIndex);
				length = (stopIndex + 1) - startIndex;
	
			}
			
			if (length != allAggregateIntervals.length) {
				result =  new AggregateInterval[length];
				System.arraycopy(allAggregateIntervals, startIndex, result, 0, length);
			} else {
				result = allAggregateIntervals;
			}
		}
		return result;
	}

	/**
	 * This method returns the Aggregate interval containing the specified
	 * time stamp.
	 * 
	 * @param intervals the aggregate intervals to search.
	 * @param time aggregate intervals will be found for this time.
	 * @return the aggregate interval containing the specified time.
	 */
	public static int search(AggregateInterval[] intervals, long time) {   	
        return search(intervals, time, 0, intervals.length);
    }
	
    /**
     * Searches for a AggregateInterval which contains the time passed in.
     *
     * @param intervals
     *            Sorted array of AggregateIntervals
     * @param time
     *            Key to search for
     * @param begin
     *            Start posisiton in the index
     * @param end
     *            One past the end position in the index
     * @return Integer index to key. -1 if not found
     */
    public static int search(AggregateInterval[] intervals, long time, int begin, int end) {
        end--;

        while (begin <= end) {
            int mid = (end + begin) >> 1;
            if (intervals[mid].getEndTime() < time)
                begin = mid + 1;
            else if (intervals[mid].getStartTime() > time)
                end = mid - 1;
            else
                return mid;
        }

        return -1;
    }
    
    /**
     * Adds a milestone for this template such as a compile event.
     * 
     * @param milestone
     */
    public void addMilestone(Milestone milestone) {
    	mMilestones.add(milestone);
    }
    
    /**
     * Returns all milestones for this template.
     */
    public Milestone[] getMilestones() {
    	Milestone[] result = null;
    	if (mMilestones.size() > 0) {
    		result = mMilestones.toArray(new Milestone[mMilestones.size()]);
    	}
    	return result;
    }
    
    /**
     * Returns all milestones for this template between the start and stopTime.
     * 
     * @param startTime
     * @param stopTime
     * @return the milestones in the requested interval.
     */
    public Milestone[] getMilestones(long startTime, long stopTime) {
    	Milestone[] result = null;
    	List<Milestone> temp = new ArrayList<Milestone>();
    	for (Milestone milestone: mMilestones) {
    		if (milestone.getTime() >= startTime && milestone.getTime() <= stopTime) {
    			temp.add(milestone);
    		}
    	}
    	if (temp.size() > 0) {
    		result = mMilestones.toArray(new Milestone[temp.size()]);
    	}
    	return result;
    }
    
    public synchronized void reset() {
		mRawData = new RawData[mRawWindowSize];
		for (int i=0; i < mRawData.length; i++) 
			mRawData[i] = new RawData();
		
		mAggregateIntervals = new AggregateInterval[mAggregateWindowSize];
		for (int i=0; i < mAggregateIntervals.length; i++) 
			mAggregateIntervals[i] = new AggregateInterval();
		
		mMilestones = new ArrayList<Milestone>();
		
		mPeakServiceDuration = 0L;
		mCumulativeServiceTime = 0L;
		mServicedCount = 0L;
		
		mCurrentRawIndex = 0;
		mCurrentAggregateIndex = 0;
    }
}
