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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

import org.teatrove.trove.util.PropertyMap;

/**
 * This class functions as the mediator for handling all http request
 * level statistics gathering and derivation.
 * 
 * This class logs template requests with the TemplateStats object
 * associated with a given template.
 * 
 * @author Scott Jappinen
 */
public class TeaServletRequestStats {

	public static final int DEFAULT_RAW_WINDOW_SIZE = 100;
	public static final int DEFAULT_AGGREGATE_WINDOW_SIZE = 50;

    private static TeaServletRequestStats mInstance = null;
    private Map<String, TemplateStats> mStatsMap = new ConcurrentHashMap<String, TemplateStats>(100);

    private int mRawWindowSize = DEFAULT_RAW_WINDOW_SIZE;
    private int mAggregateWindowSize = DEFAULT_AGGREGATE_WINDOW_SIZE;

    /**
     * Returns a static instance of TeaServletRequestStats.
     * 
     * @return the tea servlet request stats.
     */
    public static TeaServletRequestStats getInstance() {
        if (mInstance == null)
            mInstance = new TeaServletRequestStats();
        return mInstance;
    }
    
    /**
     * This method applies properties from a stats block
     * in the tea servlet config.
     * 
     * @param properties
     */
    public void applyProperties(PropertyMap properties) {
    	if (properties != null) {
	    	mRawWindowSize = properties.getInt
	    		("rawWindowSize", DEFAULT_RAW_WINDOW_SIZE);
	    	mAggregateWindowSize = properties.getInt
	    		("aggregateWindowSize", DEFAULT_AGGREGATE_WINDOW_SIZE);
	    	reset();
    	}
    }
    
    /**
     * Returns the template stats for a given template name.
     * 
     * @param fullTemplateName the full name of the template with '.'s as
     *    delimeters.
     * @return the template stats
     */
    public TemplateStats getStats(String fullTemplateName) {
        TemplateStats stats = (TemplateStats) mStatsMap.get(fullTemplateName);
        if (stats == null) {
            stats = new TemplateStats(fullTemplateName, mRawWindowSize, mAggregateWindowSize);
            mStatsMap.put(fullTemplateName, stats);
        }
        return stats;
    }
    
    /**
     * Returns an array of template stats.
     * 
     * Returns the template raw and aggregate statistics so as to
     * better understand the performance of templates through time.
     * 
     * @return the template stats for this given template.
     */
    public TemplateStats[] getTemplateStats() {
    	Collection<TemplateStats> collection = mStatsMap.values();
    	TemplateStats[] result = null;
    	if (collection.size() > 0) {
    		result = collection.toArray(new TemplateStats[collection.size()]);
    	}
    	return result;
    }

    /**
     * Logs an invokation of an http template invocation.
     * 
     * @param fullTemplateName the name of the template
     * @param startTime the time the template was invoked.
     * @param stopTime the time the template completed.
     * @param contentLength the content length of the result of the template.
     * @param params the parameter values the template was invoked with.
     */
    public void log(String fullTemplateName, long startTime, long stopTime, long contentLength, Object[] params) {
    	//System.out.println(fullTemplateName + ", " + (stopTime-startTime) + ", " + contentLength);
        TemplateStats stats = (TemplateStats) mStatsMap.get(fullTemplateName);
        if (stats == null) {
            stats = new TemplateStats(fullTemplateName, mRawWindowSize, mAggregateWindowSize);
            mStatsMap.put(fullTemplateName, stats);
        }
        stats.log(startTime, stopTime, contentLength, params);
    }

    /**
     * Returns the template name -> template stats map.
     * 
     * @return the mapping of template names to stats.
     */
    public Map<String, TemplateStats> getStatisticsMap() {
        return mStatsMap;
    }
    
    /**
     * Sets the raw window size.
     * 
     * Resets all statistics.
     * 
     * @param rawWindowSize
     */
    public void setRawWindowSize(int rawWindowSize) {
    	mRawWindowSize = rawWindowSize;
    	reset();
    }
    
    /**
     * Returns the raw window size.
     * 
     * This defines the number of raw data entries that will be
     * kept in memory for all templates.
     * 
     * @return the raw window size.
     */
    public int getRawWindowSize() {
    	return mRawWindowSize;
    }
    
    /**
     * Returns the aggregate window size.
     * 
     * This defines the number of aggregate intervals that will be
     * kept in memory for all templates.
     * 
     * @return
     */
    public int getAggregateWindowSize() {
    	return mAggregateWindowSize;
    }
    
    /**
     * Sets the aggregate interval size.
     * 
     * Resets all statistics.
     * 
     * @param aggregateWindowSize
     */
    public void setAggregateWindowSize(int aggregateWindowSize) {
    	mAggregateWindowSize = aggregateWindowSize;
    	reset();
    }

    /**
     * Resets all raw data and aggregate interval statistics.
     */
    public void reset() {
        mStatsMap = new ConcurrentHashMap<String, TemplateStats>(100);
    }

    /**
     * Resets all raw data and aggregate interval statistics.
     * 
     * @param initialMapSize the initial size of the template name / stats map
     */
    public void reset(int initialMapSize) {
        mStatsMap = new ConcurrentHashMap<String, TemplateStats>(initialMapSize);
    }
}
