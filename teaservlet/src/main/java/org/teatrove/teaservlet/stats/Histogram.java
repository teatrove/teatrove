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

import java.text.DecimalFormat;

/**
 * <p>Histogram class.</p>
 *
 * @author Scott Jappinen
 */
public class Histogram {
	
	/* Constant <code>OPTIMAL_NUMERATOR=24.0d*Math.sqrt(Math.PI)</code> */
	protected static final double OPTIMAL_NUMERATOR = 24.0d*Math.sqrt(Math.PI);
	/* Constant <code>ONE_THIRD=1.0d/3.0d</code> */
	protected static final double ONE_THIRD = 1.0d/3.0d;
	
	protected double[] data = null;
	
	protected int numBins = 0;
	
	protected double xMinRangeLimit;
	protected double xMaxRangeLimit;
	
	protected double[] ranges = null;
	protected double[] bins = null;
	protected double[] logs = null;
	protected double[] normedL1 = null;
	protected double[] normedL2 = null;
	
	protected int minBin;
	protected int maxBin;
	
	protected double minBinValue = Integer.MAX_VALUE;
	protected double maxBinValue = Integer.MIN_VALUE;
	
	protected int numNull = 0;
	protected int numOutOfBoundsLowerLimit = 0;
	protected int numOutOfBoundsUpperLimit = 0;
	
	protected double percentNull = 0.0d;
	protected double percentOutOfBoundsLowerLimit = 0.0d;
	protected double percentOutOfBoundsUpperLimit = 0.0d;
	
	/**
	 * <p>Constructor for Histogram.</p>
	 * 
	 * @param data the data.
	 */
	public Histogram(final double[] data)
	{
		this(data, calculateOptimalBinWidth(data));
	}
	
	/**
	 * <p>Constructor for Histogram.</p>
	 *
	 * @param data the data.
	 * @param binWidth a double.
	 */
	public Histogram(final double[] data, final double binWidth)
	{	
		this.data = data;
		// add a small padding so extends can fall into bins
		System.out.println("data.length: " + data.length);
		System.out.println("binWidth: " + binWidth);

		double padding = binWidth / 1000;
		System.out.println("binWidth: " + binWidth);
		double min = data[0];
		System.out.println("min: " + min);
		double max = data[data.length - 1];
		System.out.println("max: " + max);
		double histRange = (max + padding) - (min);
		System.out.println("histRange: " + histRange);
		numBins = ((Double) Math.ceil(histRange/binWidth)).intValue();
		System.out.println("numBins: " + numBins);
		initSetup(numBins);
		initUniformRange(min, max + padding);	
		initStatsAndBinify();
	}
	
	/**
	 * <p>Constructor for Histogram.</p>
	 *
	 * @param data a double array containing the ata..
	 * @param binWidth a double.
	 * @param xMinRangeLimit a double.
	 * @param xMaxRangeLimit a double.
	 */
	public Histogram(final double[] data, 
					 final double binWidth,
					 final double xMinRangeLimit, 
					 final double xMaxRangeLimit)
	{	
		this.data = data;
		double histRange = (xMaxRangeLimit) - xMinRangeLimit;
		numBins = ((Double) Math.ceil(histRange/binWidth)).intValue();
		initSetup(numBins);
		initUniformRange(xMinRangeLimit, xMaxRangeLimit);	
		initStatsAndBinify();
	}
	
	/**
	 * <p>Constructor for Histogram.</p>
	 *
	 * @param data the data under observation.
	 * @param numBins a int.
	 */
	public Histogram(final double[] data, final int numBins)
	{	
		this.data = data;
		double min = data[0];
		double max = data[data.length - 1];
		initSetup(numBins);
		initUniformRange(min, max + 1.0);	
		initStatsAndBinify();
	}
	
	/**
	 * <p>Constructor for Histogram.</p>
	 *
	 * @param data the data under observation.
	 * @param numBins a int.
	 * @param xMinRangeLimit a double.
	 * @param xMaxRangeLimit a double.
	 */
	public Histogram(final double[] data,
					 final int numBins, 
					 final double xMinRangeLimit, 
					 final double xMaxRangeLimit)
	{
		this.data = data;
		initSetup(numBins);
		initUniformRange(xMinRangeLimit, xMaxRangeLimit);	
		initStatsAndBinify();
	}

	/**
	 * <p>Constructor for Histogram.</p>
	 *
	 * @param data the data under observation.
	 * @param numBins a int.
	 * @param ranges an array of double.
	 */
	public Histogram(final double[] data,
				   	 final int numBins, 
					 final double[] ranges)
	{
		this.data = data;
		initSetup(numBins);
		initDynamicRange(ranges);
		initStatsAndBinify();
	}
    
	/**
	 * Returns the data underlying this histogram.
	 * 
	 * @return the data the histogram is based on.
	 */
	public double[] getData() {
		return this.data;
	}
	
	/**
	 * Returns the number of bins in the histogram.
	 * 
	 * @return the number of bins.
	 */
	public int getNumBins() {
		return numBins;
	}
	
	/**
	 * Returns the minimum range limit or lower bound for this Histogram.
	 * @return the minimum range limit
	 */
	public double getMinRangeLimit() {
		return xMinRangeLimit;
	}
	
	/**
	 * Returns the maximum range limit or upper bound for this Histogram.
	 * @return the maximum range limit
	 */
	public double getMaxRangeLimit() {
		return xMaxRangeLimit;
	}

	/**
	 * Returns the index of the minimum bin.
	 * @return the index of the minimum bin
	 */
	public int getMinBin() {
		return minBin;
	}

	/**
	 * Returns the index of the max bin.
	 * @return
	 */
	public int getMaxBin() {
		return maxBin;
	}
	
	/**
	 * Returns the value contained in the min bin.
	 * @return
	 */
	public double getMinBinValue() {
		return minBinValue;
	}
	
	/**
	 * Returns the value contained in the max bin.
	 * @return
	 */
	public double getMaxBinValue() {
		return maxBinValue;
	}
	
	/**
	 * Returns the range of the bin with the given index.
	 * 
	 * @param index the index of the bin
	 * @return the range for the specified bin.
	 */
	public double getRange(int index) {
		return ranges[index];
	}
	
	/**
	 * Returns the set of ranges for all bins.
	 * 
	 * @return the ranges for all bins.
	 */
	public double[] getRanges() {
		return ranges;
	}
	
	/**
	 * Returns the values of all bins.
	 * @return the values of the bins.
	 */
	public double[] getBins() {
		return bins;
	}
	
	/**
	 * Returns the natural log of the values in the bins.
	 * 
	 * @return the natural log of the values in the bins.
	 */
	public double[] getLogOfBins() {
		return logs;
	}
	
	/**
	 * Returns the l1 norm of the values in the bins.
	 * 
	 * @return the l1 norm of the values in the bins.
	 */
	public double[] getL1NormOfBins() {
		return normedL1;
	}
	
	/**
	 * Returns the l2 norm of the values in the bins.
	 * 
	 * @return the l2 norm of the values in the bins.
	 */
	public double[] getL2NormOfBins() {
		return normedL2;
	}
	
	/**
	 * Returns the value of the bin at the specified index.
	 * 
	 * @param index the index of the bin
	 * @return the value in the specified bin.
	 */
	public double getBin(int index) {
		if (index >= 0 && index < bins.length) return bins[index];
		else return 0.0d;
	}
	
	/** 
	 * Returns the range for the bin at the specified index.
	 * @param binIndex the index of the bin.
	 * @return a double array of length 2 where [0] is the lower bound for the
	 *    range and [1] is the upper bound for the range.
	 */
	public double[] getRangeForBin(int binIndex) {
		if (binIndex >=0 && binIndex < ranges.length) 
			return new double[] { ranges[binIndex], ranges[binIndex + 1] };
		return null;
	}

	/**
	 * Returns the index of the bin containing the specified coordinate.
	 * 
	 * @param x the coordinate data value
	 * @return the index of the bin containing the coordinate value.
	 */
	public int find(double x) {
		if (x < xMinRangeLimit) return Integer.MIN_VALUE;
		else if (x >= xMaxRangeLimit) return Integer.MAX_VALUE;
		return findSmaller(ranges, x);
	}
	
	protected void print(StringBuilder builder) {
		
		builder.append("-----------------\n\n");
		
		DecimalFormat formatter = new DecimalFormat("0.0");
		for (int i=0; i < bins.length; i++) {
			builder.append(formatter.format(ranges[i])).append('-');
			builder.append(formatter.format(ranges[i+1])).append(":");
			for (int j=0; j < bins[i]; j++) builder.append('*');
			builder.append('\n');
		}
		
		builder.append("-----------------\n\n");

		builder.append("numBins: ").append(numBins).append("\n");
		builder.append("xMinRangeLimit: ").append(xMinRangeLimit).append("\n");
		builder.append("xMaxRangeLimit: ").append(xMaxRangeLimit).append("\n");
		builder.append("minBin: ").append(minBin).append("\n");
		builder.append("maxBin: ").append(maxBin).append("\n");
		builder.append("minBinValue: ").append(minBinValue).append("\n");
		builder.append("maxBinValue: ").append(maxBinValue).append("\n\n");
		
		builder.append("numNull: ").append(numNull).append("\n");
		builder.append("percentNull: ").append(percentNull).append("\n");
		builder.append("numOutOfBoundsLowerLimit: ").append(numOutOfBoundsLowerLimit).append("\n");
		builder.append("numOutOfBoundsUpperLimit: ").append(numOutOfBoundsUpperLimit).append("\n");
		builder.append("percentOutOfBoundsLowerLimit: ").append(percentOutOfBoundsLowerLimit).append("\n");
		builder.append("percentOutOfBoundsUpperLimit: ").append(percentOutOfBoundsUpperLimit).append("\n\n");
		
		builder.append("-----------------\n\n");
	}
	
	/**
	 * <p>toString</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		print(builder);
		return builder.toString();
	}
	
	/* private and protected methods */
	
	/*
	 * <p>calculateOptimalBinWidth</p>
	 *
	 * @param data the data under observation.
	 * @return the optimal width of a bin based on the data.
	 */
	protected static double calculateOptimalBinWidth(final double[] data) {
		return Math.pow(OPTIMAL_NUMERATOR/(double) data.length, ONE_THIRD);
	}
	
	private void initSetup(int numBins) {
		this.numBins = numBins;
		assert(this.numBins > 0);
		this.bins = new double[this.numBins];
	}

	private void initUniformRange(double xMinRangeLimit, double xMaxRangeLimit) {
		assert(xMinRangeLimit < xMinRangeLimit);
		this.xMinRangeLimit = xMinRangeLimit;
		this.xMaxRangeLimit = xMaxRangeLimit;
		double totalRange = this.xMaxRangeLimit - this.xMinRangeLimit;
		double binSpacing = totalRange/this.numBins;
		ranges = new double[this.numBins + 1]; // num bins + 1
		
		double lowerBound = this.xMinRangeLimit;
		for (int i=0; i < bins.length; i++) {
			bins[i] = 0;
			ranges[i] = lowerBound;
			lowerBound += binSpacing;
		}
		ranges[ranges.length - 1] = this.xMaxRangeLimit;
	}
	
	private void initDynamicRange(double[] ranges) {
		assert(ranges != null);
		assert(ranges.length > 1);
		assert(numBins == ranges.length - 1);
		this.ranges = ranges;
		this.xMinRangeLimit = this.ranges[0];
		this.xMaxRangeLimit = this.ranges[ranges.length - 1];
		assert(this.xMinRangeLimit < this.xMinRangeLimit);
	}

	private void initStatsAndBinify() {
		for (int i=0; i < data.length; i++) {
			if (xMaxRangeLimit <= data[i]) {
				numOutOfBoundsUpperLimit++;
			} else if (data[i] < xMinRangeLimit) {
				numOutOfBoundsLowerLimit++;
			} else {
				int index = findSmaller(ranges, data[i]);
				if (index >= 0 && index < ranges.length) {
					bins[index]++;
					if (bins[index] > maxBinValue) {
						maxBinValue = bins[index];
						maxBin = index;
					}
					
					if (bins[index] < minBinValue) {
						minBinValue = bins[index];
						minBin = index;
					}
				}
			}
		}
		
		double sumOfSquares = 0.0d;
		logs = new double[bins.length];
		normedL1 = new double[bins.length];
		normedL2 = new double[bins.length];
		for (int i=0; i < bins.length; i++) {
			logs[i] = Math.log(bins[i]);
			normedL1[i] = bins[i]/data.length;
			sumOfSquares += bins[i] * bins[i];
		}
		
		double L2norm = Math.sqrt(sumOfSquares);
		for (int i=0; i < bins.length; i++) {
			normedL2[i] = bins[i]/L2norm;
		}
	}
	
    /*
     * Searches for a key in a sorted array, and returns an index to an element
     * which is smaller than or equal key.
     *
     * @param index
     *            Sorted array of integers
     * @param key
     *            Search for something equal or greater
     * @return -1 if nothing smaller or equal was found, else an index
     *         satisfying the search criteria
     */
    protected static int findSmaller(double[] index, double key) {
        return binarySearch(index, key, 0, index.length, false);
    }

    protected static int binarySearch(double[] index, 
    		                          double key,
    		                          int begin,
                                      int end,
                                      boolean greater) {
        if (begin == end) {
            if (greater) {
                return end;
            } else {
                return begin - 1;
            }
        }
        
        end--;
        int mid = (end + begin) >> 1;

        while (begin <= end) {
            mid = (end + begin) >> 1;

            if (index[mid] < key) {
                begin = mid + 1;
            } else if (index[mid] > key) {
                end = mid - 1;
            } else {
                return mid;
            }
        }
        
        if (greater) {
        	if (index[mid] >= key) {
        		return mid;
        	} else {
        		return mid + 1;
        	}
        } else if (index[mid] <= key) {
        	return mid;
        } else {
        	return mid - 1;
        }
    }
}
