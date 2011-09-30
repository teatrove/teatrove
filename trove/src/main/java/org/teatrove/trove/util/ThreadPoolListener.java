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

import java.util.EventListener;

/**
 * Interface used to receive events from a {@link ThreadPool}.
 *
 * @author Brian S O'Neill
 */
public interface ThreadPoolListener extends EventListener {
    /**
     * Called when a new thread is started.
     */
    public void threadStarted(ThreadPoolEvent e);

    /**
     * Called before a thread exits, usually because the idle timeout expired.
     */
    public void threadExiting(ThreadPoolEvent e);
}
