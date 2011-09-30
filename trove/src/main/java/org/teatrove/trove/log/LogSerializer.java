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

package org.teatrove.trove.log;

import java.io.*;

/**
 * LogListener serializes LogEvents to an ObjectOutputStream. LogSerializer
 * does not distinguish between logged messages and logged exceptions. They
 * are both treated as messages.
 *
 * @author Brian S O'Neill
 */
public class LogSerializer implements LogListener {
    private ObjectOutput mOut;
    private LogEvent mCurrentEvent;

    public LogSerializer(ObjectOutput out) {
        mOut = out;
    }

    public void logMessage(LogEvent event) {
        if (event == mCurrentEvent) {
            // Guard against infinite recursion in case uncaughtException is
            // redirected to this LogListener.
            mCurrentEvent = null;
            return;
        }
        
        try {
            mCurrentEvent = event;
            mOut.writeObject(event);
            mOut.flush();
            mCurrentEvent = null;
        }
        catch (IOException e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
        }
    }

    public void logException(LogEvent event) {
        logMessage(event);
    }
}
