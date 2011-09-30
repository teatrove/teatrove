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
 * This class keeps track of the raw data for a template
 * invocation.
 * 
 * @author Scott Jappinen
 */
public class RawData implements Cloneable {

	protected long startTime = -1;
	protected long endTime = -1;
	protected long contentLength = -1;
	
	/**
	 * This methods sets the raw data values.
	 * 
	 * @param startTime the time the template was invoked.
	 * @param stopTime the time the template completed.
	 * @param contentLength the content length of the template result.
	 */
	public void set(long startTime, long stopTime, long contentLength) {
		this.startTime = startTime;
		this.endTime = stopTime;
		this.contentLength = contentLength;
	}
	
	/**
	 * The start time of the tempalate.
	 * @return the time the template was invoked.
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the time halfway in between the start and stop times.
	 * 
	 * @return the time halfway in between the start and stop times.
	 */
	public long getMidPointTime() {
		return (startTime + endTime) >> 1;
	}

	/**
	 * Returns the template end time.
	 * 
	 * @return the time the template completed.
	 */
	public long getEndTime() {
		return endTime;
	}
	
	/**
	 * Returns the duration of this template invocation.
	 * 
	 * @return the duration of the template,.
	 */
	public long getDuration() {
		return endTime - startTime;
	}

	/**
	 * Returns the content length of the result of this template.
	 * 
	 * @return the content length of this template.
	 */
	public long getContentLength() {
		return contentLength;
	}
	
	/**
	 * Returns a deep copy of this raw data.
	 */
	public RawData clone() {
		RawData result = new RawData();
		result.contentLength = this.contentLength;
		result.startTime = this.startTime;
		result.endTime = this.endTime;
		return result;
	}
}
