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

import java.util.*;

/**
 * Simple event listener that can be added to a Log in order to receive
 * LogEvents.
 *
 * @author Brian S O'Neill
 */
public interface LogListener extends EventListener {
    /**
     * Called for LogEvents that should be logged like an ordinary message.
     */
    public void logMessage(LogEvent e);

    /**
     * Called for LogEvents that should be logged as an exception. The LogEvent
     * object will likely have an Exception object in it.
     */
    public void logException(LogEvent e);
}
