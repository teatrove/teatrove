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

package org.teatrove.barista.validate;

import org.teatrove.barista.validate.event.*;

/**
 * @author Sean Treat
 */
public class ConsoleErrorListener implements ValidationListener {

    private int mTotalErrors;
    private int mTotalWarns;

    public ConsoleErrorListener() {
        mTotalErrors = 0;
        mTotalWarns = 0;
    }

    public void error(ValidationEvent e) {     
        int n = e.getLineNumber();
        // log("ERROR: line " + e.getLineNumber() + " - " + e.getMessage());
        log("Error: " + ((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
        ++mTotalErrors;
    }

    public void warn(ValidationEvent e) {
        int n = e.getLineNumber();
        log("Warning: " + ((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
        ++mTotalWarns;
    }

    public void debug(ValidationEvent e) {
        // log("INFO: line " + e.getLineNumber() + " - " + e.getMessage());
        int n = e.getLineNumber();
        log("Debug: " + ((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
    }

    public int getErrorCount() {
        return mTotalErrors;
    }

    public int getWarningCount() {
        return mTotalWarns;
    }

    protected void log(String msg) {
        System.out.println(msg);
    }
}
