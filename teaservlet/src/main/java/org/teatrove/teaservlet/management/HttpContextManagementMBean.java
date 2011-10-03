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

public interface HttpContextManagementMBean  {
    
    /**
     * Returns a list of URLs which have been requested by readURL.
     */
    public String[] listRequestedUrls();
    
    /**
     * Accessor methods for turning on/off readRrl logging.
     */
    public boolean isReadUrlLoggingEnabled();
    public void setReadUrlLoggingEnabled(boolean enabled);
    
    /**
     * Clear the readUrl log.
     */
    public void clearReadUrlLog();
}
