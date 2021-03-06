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
package org.teatrove.teaapps.apps;

import org.teatrove.trove.log.Log;
import org.teatrove.teaapps.ContextConfig;

/**
 * Support implementation of {@link ThrowableHandler} that provides stub
 * methods and returns <code>null</code> for handled errors.  The expectation
 * is that overriding classes will override the 
 * {@link #handleThrowables(Throwable)} method.
 * 
 * @author Scott Jappinen
 */
public class ThrowableHandlerSupport implements ThrowableHandler {
    private Log mLog;

    /*
     * (non-Javadoc)
     * @see org.teatrove.teaapps.apps.ThrowableHandler#init(org.teatrove.teaapps.ContextConfig)
     */
    public void init(ContextConfig config) {
        mLog = config.getLog();
    }

    /*
     * (non-Javadoc)
     * @see org.teatrove.teaapps.apps.ThrowableHandler#handleThrowables(java.lang.Throwable)
     */
    public Throwable[] handleThrowables(Throwable throwable) {
        mLog.error(throwable);
        return null;
    }
}
