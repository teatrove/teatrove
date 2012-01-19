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
package org.teatrove.teaapps.contexts;

import java.util.Date;

import org.joda.time.DateTime;

/**
 * @author Scott Jappinen
 */
public class CastContext {

    public Integer toInt(Object value) {
    	if (value != null) return (Integer) value;
    	else return null;
    }
    
    public Long toLong(Object value) {
    	if (value != null) return (Long) value;
    	else return null;
    }
    
    public Float toFloat(Object value) {
    	if (value != null) return (Float) value;
    	else return null;
    }
    
    public Double toDouble(Object value) {
    	if (value != null) return (Double) value;
    	else return null;
    }
    
    public String toString(Object value) {
    	if (value != null) return (String) value;
    	else return null;
    }
    
    public Boolean toBoolean(Object value) {
    	if (value != null) return (Boolean) value;
    	else return null;
    }
    
    public DateTime toDateTime(Object value) {
    	if (value != null) return (DateTime) value;
    	else return null;
    }
    
    public Date toDate(Object value) {
    	if (value != null) return (Date) value;
    	else return null;
    }
    
    public int[] toIntArray(Object value) {
    	if (value != null) return (int[]) value;
    	else return null;
    }
    
    public long[] toLongArray(Object value) {
    	if (value != null) return (long[]) value;
    	else return null;
    }
    
    public float[] toFloatArray(Object value) {
    	if (value != null) return (float[]) value;
    	else return null;
    }
    
    public double[] toDoubleArray(Object value) {
    	if (value != null) return (double[]) value;
    	else return null;
    }
    
    public String[] toStringArray(Object value) {
    	if (value != null) return (String[]) value;
    	else return null;
    }
}
