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

package org.teatrove.trove.util;

import org.teatrove.trove.log.Log;

/**
 * This class provides a default implementation for the Config interface.
 *
 * @author Scott Jappinen
 */
public class ConfigSupport implements Config {
    
    private PropertyMap mProperties;
    private Log mLog;
    
    public ConfigSupport(PropertyMap properties, Log log) {
        mProperties = properties;
        mLog = log;
    }
    
    /**
     * Returns initialization parameters in an easier to use map format.
     */	
    public PropertyMap getProperties() {
        return mProperties;
    }
    
    /**
     * Returns a log object that the target for this config object
     * should use for reporting information pertaining to the 
     * operation of the target.
     */	
    public Log getLog() {
        return mLog;
    }
}

