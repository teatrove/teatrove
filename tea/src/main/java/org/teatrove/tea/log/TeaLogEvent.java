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

package org.teatrove.tea.log;

import org.teatrove.tea.log.TeaLog.TeaStackTraceLine;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;

/**
 * The TeaLogEvent class works with the TeaLog and the TeaLogListener to
 * simplify stack traces for Tea templates.
 * 
 * @author Reece Wilton
 */
public class TeaLogEvent extends LogEvent {

    private static final long serialVersionUID = 1L;

    private TeaStackTraceLine[] mLines;

    public TeaLogEvent(Log log, int type, TeaStackTraceLine[] lines) {
        super(log, type, null, null, null, null);
        mLines = lines;
    }

    public TeaStackTraceLine[] getLines() {
        return mLines;
    }
}
