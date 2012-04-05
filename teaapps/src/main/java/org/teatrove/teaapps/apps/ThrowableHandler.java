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

import org.teatrove.teaapps.ContextConfig;

/**
 * Handler class that handles thrown exceptions from the
 * {@link ThrowableCatcherApplication} and provides the array of associated
 * errors.
 * 
 * @author Scott Jappinen
 */
public interface ThrowableHandler {
    /**
     * Initialize the handler with the provided configuration.
     * 
     * @param config The provided configuration
     */
    public void init(ContextConfig config);
    
    /**
     * Handle the given throwable and return the array of associated errors.
     * The handler can either return <code>null</code> specifying no errors
     * should be handled, an array with the single given throwable specifying
     * that just the one error was handled, or an array of multiple errors from
     * determining multiple distinct errors within the given error.
     * 
     * @param throwable The error to handle
     * 
     * @return The array of errors that were and should be handled
     */
    public Throwable[] handleThrowables(Throwable throwable);
}