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
import org.teatrove.trove.log.*;

/**
 * @author Sean T. Treat
 */
public class LogErrorListener implements ValidationListener {
    private Log mLog;
    private int mErrors;
    private int mWarns;
    

    public LogErrorListener(Log log) {
        mErrors = 0;
        mWarns = 0;
        mLog = log;
    }

    public LogErrorListener() {
        this(Syslog.log());
    }

    public void error(ValidationEvent e) {
        ++mErrors;
        int n = e.getLineNumber();
        mLog.error(((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
    }

    public void warn(ValidationEvent e) {
        ++mWarns;
        int n = e.getLineNumber();
        mLog.warn(((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
    }

    public void debug(ValidationEvent e) {
        int n = e.getLineNumber();
        mLog.debug(((n!=-1) ? ("line " + n + " - ") : "") + 
            e.getMessage());
    }

    public int getErrorCount() {
        return mErrors;
    }

    public int getWarnCount() {
        return mWarns;
    }
}
