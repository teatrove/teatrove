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

package org.teatrove.barista.util;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.ThreadPool;
import org.teatrove.trove.log.Log;

/**
 * @author Brian S O'Neill
 */
public class DefaultConfig implements Config {
    private final PropertyMap mProperties;
    private final Log mLog;
    private final ThreadPool mThreadPool;

    public DefaultConfig(PropertyMap properties,
                         Log log,
                         ThreadPool threadPool) {
        mProperties = properties;
        mLog = log;
        mThreadPool = threadPool;
    }

    public PropertyMap getProperties() {
        return mProperties;
    }
    
    public Log getLog() {
        return mLog;
    }

    public ThreadPool getThreadPool() {
        return mThreadPool;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        buf.append(super.toString());
        buf.append(":\n");
        buf.append("Properties: " + getProperties());
        buf.append('\n');
        buf.append("Log: " + getLog());
        buf.append('\n');
        buf.append("ThreadPool: " + getThreadPool());
        buf.append('\n');
        return buf.toString();
    }
}
