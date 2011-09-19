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

package org.teatrove.barista.validate.event;

import org.teatrove.trove.io.SourceInfo;

/**
 * @author Sean T. Treat
 */
public class ValidationError extends java.util.EventObject 
    implements ValidationEvent {

    private SourceInfo mInfo;
    private String mMessage;

    public ValidationError(Object source, SourceInfo info, String msg) {
        super(source);
        mInfo = info;
        mMessage = msg;
    }

    /**
     * Returns the message for this error.
     */
    public String getMessage() {
        try {
            return (getClass().getName() + ": " + mMessage);
        }
        catch (NullPointerException npe) {
            return "no message defined";
        }
    }

    /**
     * Returns the SourceInfo associated with this error.
     */
    public SourceInfo getSourceInfo() {
        return mInfo;
    }

    /**
     * Returns the line number for this error. If there is no SourceInfo 
     * associated with this error, then -1 is returned.
     */
    public int getLineNumber() {
        try {
            return mInfo.getLine();
        }
        catch (NullPointerException npe) {
            // no SourceInfo is present
            return -1;
        }
    }
}

