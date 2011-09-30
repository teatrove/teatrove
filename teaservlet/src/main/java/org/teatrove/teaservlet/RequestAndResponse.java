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
 * 
 * @author Jonathan Colwell
 */
public class RequestAndResponse implements TeaServletTransaction {

    ApplicationRequest mReq;
    ApplicationResponse mResp;
    OutputReceiver mOutRec;
        
    public RequestAndResponse() {
        mResp = null;
        mReq = null;
        mOutRec = null;
    }
        
    public RequestAndResponse(ApplicationRequest req, ApplicationResponse resp) {
        mResp = resp;
        mReq = req;
    }

    
    public RequestAndResponse(OutputReceiver outRec) {
        mOutRec = outRec;
    }
     
    public ApplicationRequest getRequest() {
        return mReq;
    }

    public ApplicationResponse getResponse() {
        return mResp;
    }

    public OutputReceiver getOutputReceiver() {
         return mOutRec;
    }
}
