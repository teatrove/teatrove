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

package org.teatrove.teaservlet.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.teatrove.trove.util.Cache;

/**
 * 
 *
 * @author tliaw
 */
public class HttpContextManagement implements HttpContextManagementMBean {
    
    //
    // Private static fields
    //
    
    private static HttpContextManagement __Instance; 
    private static Cache __UrlMap;
    
    //
    // Private fields
    //
    
    private boolean readUrlLoggingEnabled;
    
    //
    // Public static methods
    //
    
    public static synchronized HttpContextManagement create(int cacheSize) throws Exception {
        if (__Instance == null) {
            __Instance = new HttpContextManagement(cacheSize);
        
        } else {
            if (__UrlMap.getMaxRecent() != cacheSize) {
                // TODO: __UrlMap.setMaxRecent(cacheSize);
            }
        }
    
        return __Instance;
    }

    //
    // Private constructors
    //
    
    private HttpContextManagement(int cacheSize) {
        __UrlMap = new Cache(cacheSize);
        readUrlLoggingEnabled = true;
    }
    
    //
    // Public methods
    //
    
    public boolean isReadUrlLoggingEnabled() {
        return readUrlLoggingEnabled;
    }
    
    public void setReadUrlLoggingEnabled(boolean enabled) {
        readUrlLoggingEnabled = enabled;
    }
    
    public void clearReadUrlLog() {
        __UrlMap.clear();        
    }
    
    public String[] listRequestedUrls() {
        ArrayList result = new ArrayList();
        Set urlKeySet = __UrlMap.keySet();
        
        for (Iterator it = urlKeySet.iterator(); it.hasNext();) {
            String url = (String)it.next();
            AtomicLong count = (AtomicLong)__UrlMap.get(url);
            
            String displayString = url + " : " + count.toString(); 
            result.add(displayString);
        }
        
        return (String[]) result.toArray(new String[result.size()]);
    }
    
    /**
     * Adds the URL to the list if it doesn't already exist. Or increment the hit count otherwise.
     *
     * @param url The URL requested.
     */
    public void addReadUrl(String url) {        
        
        //
        // Only cache the url data if logging is enabled.
        //
        
        if (readUrlLoggingEnabled == false) {
            return;
        }
        
        //
        // Strip off the query parameters.
        //
        
        int paramIndex = url.indexOf('?');
        String filteredUrl = url;
        if (paramIndex != -1) {
            filteredUrl = url.substring(0, paramIndex);
        }

        //
        // Because Cache doesn't take advantage of the underlying concurrentMap, this code either has to (1)introduce 
        // sync block to synchronize addReadUrl requests which would negatively impact all templates calling addReadUrl
        // or (2) accept potential race condition during the first add call on the same Url. This implementation takes
        // (2) approach as this feature is more for reporting and hence not worth the performance degradation . Also, 
        // once the first record is added to the map, the race condition no longer exists due to the use of atomic 
        // variable.
        //        
        
        AtomicLong count = (AtomicLong)__UrlMap.get(filteredUrl);
        if (count == null) {
            count = new AtomicLong(1);
            __UrlMap.put(filteredUrl, count);
        }
        else {
            count.incrementAndGet();
        }        
    }
}
