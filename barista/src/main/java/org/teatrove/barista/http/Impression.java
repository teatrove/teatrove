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

import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;

/**
 * A special LogEvent used to record "hits" or impressions made to a web
 * server.
 * 
 * @author Brian S O'Neill
 * @see HttpHandler
 * @see HttpHandlerStage
 */
public class Impression extends LogEvent {
    private transient HttpServerConnection mCon;
    private long mStartTime;

    public Impression(Log log,
                      HttpServerConnection connection,
                      long startTime) {
        super(log, INFO_TYPE, connection.getRequestURI());
        mCon = connection;
        mStartTime = startTime;
    }
    
    public HttpServerConnection getConnection() {
        return mCon;
    }
    
    public long getStartTime() {
        return mStartTime;
    }
}

