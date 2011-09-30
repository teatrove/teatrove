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

package org.teatrove.teaservlet;

import org.teatrove.tea.runtime.OutputReceiver;

/**
 * An interface that pairs an application request and response. With these, any
 * servlet may directly invoke templates and applications.
 *
 * @author Brian S O'Neill
 * @see TeaServletEngine
 */
public interface TeaServletTransaction {
    public ApplicationRequest getRequest();
   
    /**
     * In order to write out the buffered TeaServlet response, the finish
     * method must be called. It will request an OutputStream from the original
     * ServletResponse. An IllegalArgumentException will be thrown if a Writer
     * was already used for writing to the response.
     */
    public ApplicationResponse getResponse();
    
    public OutputReceiver getOutputReceiver();

}
