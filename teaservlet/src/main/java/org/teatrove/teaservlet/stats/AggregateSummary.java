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

/**
 * 
 * @author Scott Jappinen
 */
public class AggregateSummary {

	protected int n = 0;
	protected long sum = 0;
	protected long min = Integer.MAX_VALUE;
	protected long max = Integer.MIN_VALUE;
	protected float arithmeticMean = -1.0f;
	protected float averageMedian = -1.0f;
	protected float averageVariance = -1.0f;
	protected float averageUpperQuartile = -1.0f;
	protected float averageLowerQuartile = -1.0f;
	protected float averageUpperWhisker = -1.0f;
	protected float averageLowerWhisker = -1.0f;
	
	public long getN() {
		return n;
	}
	
	public long getMin() {
		return min;
	}
	
	public long getMax() {
		return max;
	}
	
	public long getSum() {
		return sum;
	}
	
	public float getArithmeticMean() {
		return arithmeticMean;
	}
	

	public float getAverageMedian() {
		return averageMedian;
	}
	
	public float getAverageVariance() {
		return averageVariance;
	}
	
	public double getAverageStandardDeviation() {
		return Math.sqrt(averageVariance);
	}
	
	public long getAbsoluteRange() {
		return max - min;
	}
	
	public float getAverageUpperQuartile() {
		return averageUpperQuartile;
	}

	public float getAverageLowerQuartile() {
		return averageLowerQuartile;
	}
	
	public float getAverageUpperWhisker() {
		return averageUpperWhisker;
	}

	public float getAverageLowerWhisker() {
		return averageLowerWhisker;
	}
	
	public static AggregateSummary getDurationAggregateSummary(AggregateInterval[] intervals) {
		AggregateSummary result = null;
		if (intervals != null && intervals.length > 0) {
			result = new AggregateSummary();
			for (AggregateInterval interval: intervals) {
				result.n += interval.sampleSize;
				AggregateStats stats = interval.durationStats;
				if (stats.min < result.min) result.min = stats.min;
				if (stats.max > result.max) result.max = stats.max;
				result.arithmeticMean += (stats.arithmeticMean * interval.sampleSize);
				result.averageMedian += stats.median;
				result.averageVariance += stats.variance;
				result.averageUpperQuartile += stats.upperQuartile;
				result.averageLowerQuartile += stats.lowerQuartile;
				result.averageUpperWhisker += stats.upperWhisker;
				result.averageLowerWhisker += stats.lowerWhisker;
			}
			result.arithmeticMean /= result.n;
			result.averageMedian /= intervals.length;
			result.averageVariance /= intervals.length;
			result.averageUpperQuartile /= intervals.length;
			result.averageLowerQuartile /= intervals.length;
			result.averageUpperWhisker /= intervals.length;
			result.averageLowerWhisker /= intervals.length;
		}
		return result;
	}

	public AggregateSummary clone() {
		AggregateSummary result = new AggregateSummary();
		result.sum = this.sum;
		result.min = this.min;
		result.max = this.max;
		result.arithmeticMean = this.arithmeticMean;
		result.averageMedian = this.averageMedian;
		result.averageVariance = this.averageVariance;
		result.averageUpperQuartile = this.averageUpperQuartile;
		result.averageLowerQuartile = this.averageLowerQuartile;
		result.averageUpperWhisker = this.averageUpperWhisker;
		result.averageLowerWhisker = this.averageLowerWhisker;
		return result;
	}
	
	public void reset() {
		n = 0;
		sum = 0;
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		arithmeticMean = -1.0f;
		averageMedian = -1.0f;
		averageVariance = -1.0f;
		averageUpperQuartile = -1.0f;
		averageLowerQuartile = -1.0f;
		averageUpperWhisker = -1.0f;
		averageLowerWhisker = -1.0f;
	}
	
}
