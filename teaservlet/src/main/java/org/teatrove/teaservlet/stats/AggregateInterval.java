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

/**
 * This class represents a compressed statistical representation
 * of many requests to a template for a given interval.
 * 
 * @author Scott Jappinen
 */
public class AggregateInterval implements Cloneable {
	
	protected static final double oor2pi = 1 / (Math.sqrt(2 * 3.14159265358979323846));

	protected long startTime = -1;
	protected long endTime = -1;
	
	protected int sampleSize = 0;
	
	protected AggregateStats durationStats = new AggregateStats();
	protected AggregateStats contentLengthStats = new AggregateStats();
	
	protected Object[] outlierParams = null;
	
	public AggregateInterval() {}
	
	protected void compute(RawData[] rawData, long startTime, long endTime) {
		reset();
		
		boolean findStartTime = false;
		boolean findEndTime = false;
		this.startTime = startTime;
		this.endTime = endTime;
		if (this.startTime == -1) findStartTime = true;
		if (this.endTime == -1) findEndTime = true;
		
		sampleSize = rawData.length;
		
		for (int i = 0; i < rawData.length; i++) {
			if (findStartTime) {
				if (this.startTime == -1 && rawData[i].getStartTime() > this.startTime) {
					this.startTime = rawData[i].getStartTime();
				} else if (this.startTime > -1 && rawData[i].getStartTime() < this.startTime) {
					this.startTime = rawData[i].getStartTime();
				}
			}
			
			if (findEndTime) {
				if (rawData[i].getEndTime() > endTime) {
					this.endTime = rawData[i].getEndTime();
				}
			}
		}
		
		long[] durations = calculateBasicStats(durationStats, rawData, true);
		long[] contentLengths = calculateBasicStats(contentLengthStats, rawData, false);
		
		calculateVariance(durationStats, durations);
		calculateVariance(contentLengthStats, contentLengths);

		calculatePercentile(durationStats, durations);
		calculatePercentile(contentLengthStats, contentLengths);
	}

	protected long[] calculateBasicStats(AggregateStats stats, RawData[] rawData, boolean isDuration) {
		long[] data = new long[rawData.length];
		int sum = 0;
		for (int i = 0; i < rawData.length; i++) {	
			sum += (data[i] = (isDuration ? rawData[i].getDuration() : rawData[i].getContentLength()));
			
			if (data[i] < stats.min)
				stats.min = data[i];
			if (data[i] > stats.max)
				stats.max = data[i];
		}
		stats.sum = sum;
		stats.arithmeticMean = sum / data.length;
		return data;
	}
	
	protected void calculateVariance(AggregateStats stats, long[] data) {
		float ds = 0.0f, dep = 0.0f;
		for (int i = 0; i < data.length; i++) {
			dep += ds;
			ds = data[i] - stats.arithmeticMean;
			stats.variance += (ds * ds);
		}
		ds = stats.variance - dep * dep / data.length;
		stats.variance = ds / data.length;
	}

	protected void calculatePercentile(AggregateStats stats, long[] data) {
		int depthOfMedian = 0;
		
		Arrays.sort(data);
		if ((data.length & 1) == 0) { // even
			depthOfMedian = (data.length / 2) - 1;
			stats.median = (data[depthOfMedian] + data[depthOfMedian + 1]) / 2.0f;
		} else {
			if (data.length >= 3) {
				depthOfMedian = ((data.length + 1) / 2) - 1;
				stats.median = data[depthOfMedian];
			} else if (data.length == 1) {
				stats.median = data[0];
			}
		}

		//System.out.println("data.length: " + data.length);
		//System.out.println("depthOfMedian: " + depthOfMedian);
		
		if (depthOfMedian > 0) {
			if ((depthOfMedian & 1) == 0) {
				int depthOfForth = (depthOfMedian + 1) / 2;
				int upperQuartileDepth = depthOfMedian + depthOfForth;
				//System.out.println("depthOfForth: " + depthOfForth);
				//System.out.println("upperQuartileDepth: " + upperQuartileDepth);
				
				stats.lowerQuartile = 0.5f * (data[depthOfForth] + data[depthOfForth + 1]);
				stats.upperQuartile = 0.5f * (data[upperQuartileDepth] + data[upperQuartileDepth + 1]);	
				
				float fourthSpread = stats.upperQuartile - stats.lowerQuartile;
				stats.lowerWhisker = stats.lowerQuartile - (1.5f * fourthSpread);
				if (stats.lowerWhisker < stats.min) stats.lowerWhisker = stats.min;
				stats.upperWhisker = stats.upperQuartile + (1.5f * fourthSpread);
				if (stats.upperWhisker > stats.max) stats.upperWhisker = stats.max;
			} else {
				int depthOfForth = ((depthOfMedian + 1) / 2);
				int upperQuartileDepth = depthOfMedian + depthOfForth;
				//System.out.println("depthOfForth: " + depthOfForth);
				//System.out.println("upperQuartileDepth: " + upperQuartileDepth);
				
				stats.lowerQuartile = 0.5f * (data[depthOfForth - 1] + data[depthOfForth]);
				stats.upperQuartile = 0.5f * (data[upperQuartileDepth - 1] + data[upperQuartileDepth]);	
				
				float fourthSpread = stats.upperQuartile - stats.lowerQuartile;
				stats.lowerWhisker = stats.lowerQuartile - (1.5f * fourthSpread);
				if (stats.lowerWhisker < stats.min) stats.lowerWhisker = stats.min;
				stats.upperWhisker = stats.upperQuartile + (1.5f * fourthSpread);
				if (stats.upperWhisker > stats.max) stats.upperWhisker = stats.max;
			}
		}
	}
	
    public double getDurationStressMeasure() {
    	double result = 0.0d;
    	double sigma = durationStats.getStandardDeviation();
    	if (sampleSize > 0 && sigma != 0.0d) {
	    	double timeDelta = endTime - startTime;
	    	double mean = durationStats.arithmeticMean;
	    	double numServiced = (double) sampleSize;
	    	double stdDevAdj = Math.max((mean-4.0d*sigma)/4.0d, -1.0d*sigma+1);
	    	sigma += stdDevAdj;
	    	double x = timeDelta/numServiced;
	    	result = 1.0d - AggregateInterval.normDist(x, mean, sigma);
    	}
    	return result;
    }
    
    public static double normDist(double X, double mean, double sigma) {
        double result = 0;
        final double x = (X - mean) / sigma;
        if (x == 0) {
        	result = 0.5;
        } else {
        	double denom = 1 + 0.2316419 * Math.abs(x);
        	if (denom != 0.0d) {
	            double t = 1 / (denom);
	            t *= oor2pi * Math.exp(-0.5 * x * x) *
	                (0.31938153 + t * (-0.356563782 + t *
	                (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
	            if (x >= 0) {
	            	result = 1 - t;
	            } else {
	            	result = t;
	            }
        	}
        }
        return result;
    }
	
	/**
	 * Returns the number of samples this aggregate represents.
	 * 
	 * @return the sample size
	 */
	public int getSampleSize() {
		return sampleSize;
	}
	
	/**
	 * Returns the startTime of this aggregate.
	 * 
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the midpoint time of this aggregate
	 * @return the time halfway between the start and end time.
	 */
	public long getMidPointTime() {
		return (startTime + endTime) >> 1;
	}

	/**
	 * Returns the endTime of this aggregate.
	 * 
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * Returns the set of statistics for the duration of this
	 * template during this aggregate interval.
	 * 
	 * @return the duration stats
	 */
	public AggregateStats getDurationStats() {
		return durationStats;
	}

	/**
	 * Returns the set of statistics for the content lengths of 
	 * this template during this aggregate interval.
	 * 
	 * @return the content length stats
	 */
	public AggregateStats getContentLengthStats() {
		return contentLengthStats;
	}
	
	/**
	 * Clones this aggregate interval
	 */
	public AggregateInterval clone() {
		AggregateInterval result = new AggregateInterval();
		result.sampleSize = this.sampleSize;
		result.startTime = this.startTime;
		result.endTime = this.endTime;
		result.durationStats = this.durationStats.clone();
		result.contentLengthStats = this.contentLengthStats.clone();
		return result;
	}
	
	/**
	 * Resets all raw and aggregate statistics for this template.
	 */
	public void reset() {
		sampleSize = 0;
		durationStats.reset();
		contentLengthStats.reset();
	}
}
