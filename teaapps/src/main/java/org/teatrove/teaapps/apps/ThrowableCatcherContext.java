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

import org.teatrove.tea.runtime.Substitution;

/**
 * Custom context that provides methods to catch throwables within a Tea
 * substitution block. This allows a template to invoke other operations and
 * catch any thrown exceptions including runtime exceptions. Otherwise,
 * exceptions in Tea will propogate to the request resulting in a 
 * {@link ServletException} and HTTP response error.
 * 
 * @author Scott Jappinen
 */
public interface ThrowableCatcherContext {
    
    /**
     * Catch any and all errors within a substitution block. If no errors are
     * thrown during processing the substitution block, then <code>null</code>
     * is returned.
     * 
     * <pre>
     *     errors = catchThrowables() { 'code here' }
     * </pre>
     * 
     * @param substitution The substitution block to invoke
     * 
     * @return The array of thrown errors or <code>null</code>
     */
    public Throwable[] catchThrowables(Substitution substitution);
}
