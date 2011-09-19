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

package org.teatrove.barista.http;

import java.lang.reflect.*;
import java.io.IOException;
import javax.servlet.*;

/**
 * Permits the sorting of Filters by the order in which they were initialized
 * and configured into Barista. 
 *
 * @author Jonathan Colwell
 */
public class SortableFilter implements Filter, Comparable {
    private Filter mFilter;
    private int mInitIndex;

    public SortableFilter(Filter phil, int orderInitialized) {
        mFilter = phil;
        mInitIndex = orderInitialized;
    }
    
    public void doFilter(ServletRequest req,
                         ServletResponse resp,
                         FilterChain chain) 
        throws IOException, ServletException
    {
        mFilter.doFilter(req, resp, chain);
    }
    
    public void init(FilterConfig conf) throws ServletException {
        try {
            mFilter.init(conf);
        }
        catch (IncompatibleClassChangeError e) {
            callSetFilterConfig(e, conf);
        }
    }

    public void destroy() {
        try {
            mFilter.destroy();
        }
        catch (IncompatibleClassChangeError e) {
            callSetFilterConfig(e, null);
        }
    }

    /**
     * Uses the order of filter initialization to determine placement.
     */
    public int compareTo(Object o) throws ClassCastException {
        return mInitIndex - ((SortableFilter)o).mInitIndex;
    }

    /**
     * Returns true if the underlying filters are the same.
     */
    public boolean equals(Object obj) {
        if (obj instanceof SortableFilter) {
            return mFilter.equals(((SortableFilter)obj).mFilter);
        }
        return false;
    }

    /**
     * Returns the hashcode of the underlying filter.
     */
    public int hashCode() {
        return mFilter.hashCode();
    }

    public String toString() {
        return mFilter.toString();
    }

    // Call setFilterConfig method via reflection for compatibility with
    // filters compiled against the pre-release servlet 2.3 API.
    private void callSetFilterConfig(Error error, FilterConfig conf) {
        try {
            Method m = mFilter.getClass().getMethod
                ("setFilterConfig", new Class[]{FilterConfig.class});
            m.invoke(mFilter, new Object[]{conf});
        }
        catch (NoSuchMethodException e) {
            throw error;
        }
        catch (IllegalAccessException e) {
            throw error;
        }
        catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            if (t instanceof Error) {
                throw (Error)t;
            }
            throw error;
        }
    }
}
