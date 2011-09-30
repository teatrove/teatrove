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
public class AggregateStats implements Cloneable {
	
	protected long sum = 0;
	protected long min = Integer.MAX_VALUE;
	protected long max = Integer.MIN_VALUE;
	protected float arithmeticMean = -1.0f;
	protected float median = -1.0f;
	protected float variance = -1.0f;
	protected float upperQuartile = -1.0f;
	protected float lowerQuartile = -1.0f;
	protected float upperWhisker = -1.0f;
	protected float lowerWhisker = -1.0f;

	/**
	 * <p>
	 * Returns the minimum value found in the data.
	 * </p>
	 *
	 * <p>
	 * \(\min x\)
	 * </p>
	 *
	 * @return the min value.
	 */
	public long getMin() {
		return min;
	}
	
	/**
	 * Returns the maximum value found in the data.
	 *
	 * <p>
	 * \(\max x\)
	 * </p>
	 *
	 * @return the max value.
	 */
	public long getMax() {
		return max;
	}
	
	/**
	 * <p>
	 * Returns the sum of the data.
	 * </p>
	 *
	 * <p>
	 * \(\text{sum} = \sum_{i=0}^{n-1}x_i\)
	 * </p>
	 *
	 * @return the sum of the data.
	 */
	public long getSum() {
		return sum;
	}
	
	/**
	 * <p>
	 * Returns the arithmethic mean of the data.
	 * </p>
	 *
	 * <p>
	 * \(\text{arithmethic mean} = E(X) = \mu = \frac{1}{n}\sum_{i=0}^{n-1}x_i\)
	 * </p>
	 *
	 * <p>
	 * <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">From Wikipedia</a> -
	 * The arithmetic mean, often referred to as simply the mean or
	 * average when the context is clear, is a method to derive the central
	 * tendency of a sample space.
	 * </p>
	 *
	 * @return the arithmetic mean for this set of data.
	 */
	public float getArithmeticMean() {
		return arithmeticMean;
	}
	
	/**
	 * <p>
	 * Returns the median of all values.
	 * </p>
	 *
	 * <p>
	 * <a href="http://en.wikipedia.org/wiki/Median">From Wikipedia</a> -
	 * In probability theory and statistics, a median is described as the numeric
	 * value separating the higher half of a sample, a topics, or a probability
	 * distribution, from the lower half. The median of a finite list of numbers
	 * can be found by arranging all the observations from lowest value to highest
	 * value and picking the middle one. If there is an even number of observations,
	 * then there is no single middle value; the median is then usually defined to
	 * be the mean of the two middle values.
	 * </p>
	 *
	 * <p>
	 * Efficient computation of the sample median
	 * </p>
	 *
	 * </p>
	 * Even though sorting n items requires \(O(n \log n)\) operations, selection algorithms
	 * can compute the kth-smallest of n items (e.g., the median) with only \(O(n)\) operations.
	 * </p>
	 *
	 * <p>
	 * The default implementation for this method uses a selection operation that computes
	 * the median in worst case \(O(n)\).
	 * </p>
	 *
	 * <p>
	 * For odd number of items then the \(((n+1)/2)th\) item is to be selected. For an even
	 * number of items the \((n/2)th\) item is to be averaged with the \((n/2 + 1)th\) item.
	 * </p>
	 *
	 * @return the median value of the set of data.
	 */
	public float getMedian() {
		return median;
	}
	
	/**
	 * <p>
	 * Returns the variance.
	 * </p>
	 *
	 * <p>
	 * <a href="http://en.wikipedia.org/wiki/Variance">From Wikipedia</a> -
	 * The variance of a random variable or distribution is the expectation,
	 * or mean, of the squared deviation of that variable from its expected
	 * value or mean. Thus the variance is a measure of the amount of variation
	 * of the values of that variable, taking account of all possible values
	 * and their probabilities or weightings (not just the extremes which
	 * give the range).
	 * </p>
	 *
	 * <p>
	 * In general, the topics variance of a finite topics of size N is given by:
	 * </p>
	 *
	 * <p>
	 * \(\text{variance} = \sigma^{2} = \frac{1}{n}\sum_{i=0}^{n-1} (x_i-\mu)^{2}\)
	 * </p>
	 *
	 * <p>
	 * where
	 * </p>
	 *
	 * <p>
	 * \(\mu = \frac{1}{n}\sum_{i=0}^{n-1}x_i\)
	 * </p>
	 *
	 * <p>
	 * is the topics mean.
	 * </p>
	 *
	 * @return the variance for this set of data.
	 */
	public float getVariance() {
		return variance;
	}
	
	/**
	 * <p>
	 * Returns the standard deviation of all values.
	 * </p>
	 *
	 * <p>
	 * <a href="http://en.wikipedia.org/wiki/Standard_Deviation">From Wikipedia</a> -
	 * Standard deviation is a widely used measurement of variability or diversity
	 * used in statistics and probability theory. It shows how much variation or
	 * "dispersion" there is from the "average" (mean, or expected/budgeted value).
	 * A low standard deviation indicates that the data points tend to be very close
	 * to the mean, whereas high standard deviation indicates that the data are
	 * spread out over a large range of values.
	 * </p>
	 *
	 * <p>
	 * In general, the standard deviation defined by:
	 * </p>
	 *
	 * <p>
	 * \(\text{standard deviation}=\sigma = \sqrt{\frac{1}{n}\sum_{i=0}^{n-1} (x_i-\mu)^{2}}\)
	 * </p>
	 *
	 * <p>
	 * where
	 * </p>
	 *
	 * <p>
	 * \(\mu = \frac{1}{n}\sum_{i=0}^{n-1}x_i\)
	 * </p>
	 *
	 * @return the standard deviation for this set of data.
	 */
	public double getStandardDeviation() {
		return Math.sqrt(variance);
	}
	
	/**
	 * <p>
	 * Returns the upper whisker for a boxplot.
	 * </p>
	 *
	 * <p>
	 * \(
	 * \text{upper whisker} = F_U + 1.5d_F
	 * \)
	 * </p>
	 *
	 * @return a double.
	 */
	public long getAbsoluteRange() {
		return max - min;
	}
	
	/**
	 * <p>
	 * Return the upper quartile (forth).
	 * </p>
	 *
	 * <p>
	 * \(
	 * F_U=\frac{1}{2}\{x_{(j)} + x_{(x_{j+1})}\}
	 * \;\; \text{where} \;\;
	 * j = \text{[depth of median]} +\text{[depth of forth]}
	 * \)
	 * </p>
	 *
	 * @return a double.
	 */
	public float getUpperQuartile() {
		return upperQuartile;
	}

	/**
	 * <p>
	 * Return the lower quartile (forth).
	 * </p>
	 *
	 * <p>
	 * \(
	 * F_L=\frac{1}{2}\{x_{(\text{j})} + x_{(\text{j+1})}\}
	 * \;\; \text{where} \;\;
	 * j = \text{[depth of forth]}
	 * \)
	 * </p>
	 *
	 * @return a double.
	 */
	public float getLowerQuartile() {
		return lowerQuartile;
	}
	
	/**
	 * <p>
	 * Returns the upper whisker for a boxplot.
	 * </p>
	 *
	 * <p>
	 * \(
	 * \text{upper whisker} = F_U + 1.5d_F
	 * \)
	 * </p>
	 *
	 * @return a double.
	 */
	public float getUpperWhisker() {
		return upperWhisker;
	}

	/**
	 * <p>
	 * Returns the lower outside whisker for a boxplot.
	 * </p>
	 *
	 * <p>
	 * \(
	 * \text{lower whisker} = F_L - 1.5d_F
	 * \)
	 * </p>
	 *
	 * @return a double.
	 */
	public float getLowerWhisker() {
		return lowerWhisker;
	}

	/**
	 * Returns a deep clone of this object.
	 */
	public AggregateStats clone() {
		AggregateStats result = new AggregateStats();
		result.sum = this.sum;
		result.min = this.min;
		result.max = this.max;
		result.arithmeticMean = this.arithmeticMean;
		result.median = this.median;
		result.variance = this.variance;
		result.upperQuartile = this.upperQuartile;
		result.lowerQuartile = this.lowerQuartile;
		result.upperWhisker = this.upperWhisker;
		result.lowerWhisker = this.lowerWhisker;
		return result;
	}
	
	/**
	 * Resets the statistics in this aggregate interval.
	 */
	public void reset() {
		sum = 0;
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		arithmeticMean = -1.0f;
		median = -1.0f;
		variance = -1.0f;
		upperQuartile = -1.0f;
		lowerQuartile = -1.0f;
		upperWhisker = -1.0f;
		lowerWhisker = -1.0f;
	}
}
