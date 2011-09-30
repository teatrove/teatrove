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

package org.teatrove.trove.util.tq;

import java.util.EventObject;

/**
 * An event that represents an uncaught exception from a
 * {@link TransactionQueue}. UncaughtExceptionEvents can be received by
 * implementing a {@link UncaughtExceptionListener}.
 *
 * @author Brian S O'Neill
 */
public class UncaughtExceptionEvent extends EventObject {
    private Thread mThread;
    private Throwable mThrowable;

    public UncaughtExceptionEvent(Object source, Throwable e) {
        super(source);

        mThread = Thread.currentThread();
        mThrowable = e;
    }

    /**
     * Returns the uncaught exception.
     */
    public Throwable getException() {
        return mThrowable;
    }

    /**
     * Returns the thread that created this UncaughtExceptionEvent.
     */
    public Thread getThread() {
        return mThread;
    }
}
