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

/**
 * This exception is thrown by a {@link ThreadPool} when no thread is
 * available.
 *
 * @author Brian S O'Neill
 */
public class NoThreadException extends InterruptedException {
    private boolean mIsClosed;

    public NoThreadException(String message) {
        super(message);
    }

    public NoThreadException(String message, boolean isClosed) {
        super(message);
        mIsClosed = isClosed;
    }

    public boolean isThreadPoolClosed() {
        return mIsClosed;
    }
}
