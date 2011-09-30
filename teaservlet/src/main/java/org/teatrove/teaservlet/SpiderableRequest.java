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

package org.teatrove.teaservlet;

import java.util.*;
import javax.servlet.http.*;

/**
 * Allows HTTP requests to be 'spiderable', that is, support parameters
 * without using '?', '&' and '=' characters so that search engines will
 * index the request URL. The request URI is broken down, providing additional
 * parameters, and possibly altering the PathInfo. If the URL contains the
 * normal query separator, '?', then any parameters specified after it are
 * preserved.
 *
 * @author Brian S O'Neill
 */
class SpiderableRequest extends HttpServletRequestWrapper {
    private final String mQuerySeparator;
    private final String mParameterSeparator;
    private final String mValueSeparator;

    private final int mQuerySepLen;
    private final int mParamSepLen;
    private final int mValSepLen;

    private String mPathInfo;
    private Map mParameters;

    public SpiderableRequest(HttpServletRequest request,
                             String querySeparator,
                             String parameterSeparator,
                             String valueSeparator) {
        super(request);
        mQuerySeparator = querySeparator;
        mParameterSeparator = parameterSeparator;
        mValueSeparator = valueSeparator;

        mQuerySepLen = querySeparator.length();
        mParamSepLen = parameterSeparator.length();
        mValSepLen = valueSeparator.length();

        mPathInfo = request.getPathInfo();
        mParameters = new HashMap(37);
        
        fillParameters();
        fillDefaultParameters();
    }

    public String getPathInfo() {
        return mPathInfo;
    }
    
    public String getParameter(String name) {
        String[] values = getParameterValues(name);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    public String[] getParameterValues(String name) {
        return (String[])mParameters.get(name);
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(mParameters.keySet());
    }

    private void fillParameters() {
        String str;
        int startIndex, midIndex, endIndex;
        
        if ("?".equals(mQuerySeparator)) {
            if ((str = super.getQueryString()) == null) {
                return;
            }
            startIndex = 0;
        }
        else {
            if (mPathInfo != null) {
                startIndex = mPathInfo.indexOf(mQuerySeparator);
                if (startIndex >= 0) {
                    mPathInfo = mPathInfo.substring(0, startIndex);
                }
            }

            str = super.getRequestURI();
            startIndex = str.indexOf(mQuerySeparator);
            if (startIndex < 0) {
                return;
            }
            startIndex += mQuerySepLen;
        }
        
        int length = str.length();
        
        for (; startIndex < length; startIndex = endIndex + mParamSepLen) {
            endIndex = str.indexOf(mParameterSeparator, startIndex);
            
            if (endIndex < 0) {
                endIndex = length;
            }
            
            midIndex = str.indexOf(mValueSeparator, startIndex);
            
            String key;
            String value;
            
            if (midIndex < 0 || midIndex > endIndex) {
                if (endIndex - startIndex > 1) {
                    key = str.substring(startIndex, endIndex);
                    value = "";
                }
                else {
                    continue;
                }
            }
            else if (midIndex - startIndex > 1) {
                key = str.substring(startIndex, midIndex);
                value = str.substring(midIndex + mValSepLen, endIndex);
            }
            else {
                continue;
            }
            
            putParameter(key, value);
        }
    }

    private void fillDefaultParameters() {
        Enumeration names = super.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            putParameters(name, super.getParameterValues(name));
        }
    }

    private void putParameter(String key, String value) {
        String[] currentValues = (String[])mParameters.get(key);
        if (currentValues == null) {
            currentValues = new String[] {value};
        }
        else {
            String[] newValues = new String[currentValues.length + 1];
            int i;
            for (i=0; i<currentValues.length; i++) {
                newValues[i] = currentValues[i];
            }
            currentValues = newValues;
            currentValues[i] = value;
        }

        mParameters.put(key, currentValues);
    }


    private void putParameters(String key, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        else if (values.length == 1) {
            putParameter(key, values[0]);
            return;
        }

        String[] currentValues = (String[])mParameters.get(key);
        if (currentValues == null) {
            currentValues = values;
        }
        else {
            String[] newValues =
                new String[currentValues.length + values.length];
            int i, j;
            for (i=0; i<currentValues.length; i++) {
                newValues[i] = currentValues[i];
            }
            for (j=0; j<values.length; j++) {
                newValues[i + j] = values[j];
            }
            currentValues = newValues;
        }
        
        mParameters.put(key, currentValues);
    }
}
