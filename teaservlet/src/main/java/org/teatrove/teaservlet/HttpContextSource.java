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

import javax.servlet.ServletContext;

import org.teatrove.tea.runtime.OutputReceiver;
import org.teatrove.tea.engine.DynamicContextSource;
import org.teatrove.teaservlet.management.HttpContextManagement;
import org.teatrove.trove.io.CharToByteBuffer;
import org.teatrove.trove.log.Log;

/**
 * 
 * @author Jonathan Colwell
 */
public class HttpContextSource implements DynamicContextSource {

    private ServletContext mServletContext;
    private Log mLog;
    private HttpContextManagement mHttpContextMBean;    

    HttpContextSource(ServletContext servletContext, Log log, HttpContextManagement contextMBean) {
        mLog = log;
        mServletContext = servletContext;
        mHttpContextMBean = contextMBean;
    }

    public Class getContextType() {
        return HttpContext.class;
    }

    public Object createContext(Class expected, Object obj) 
        throws ClassNotFoundException {
        
        if (expected.isAssignableFrom(HttpContextImpl.class)) {
            return createContext(obj);
        }
        else {
            throw new ClassNotFoundException
                (expected + " is not available from this context source.");
        }
    }

    public Object createContext(Object obj) {

        ApplicationRequest req = null;
        ApplicationResponse resp = null;
        CharToByteBuffer buf = null;
        OutputReceiver outRec = null;

        if (obj instanceof TeaServletTransaction) {
            TeaServletTransaction trans = (TeaServletTransaction)obj;
            req = trans.getRequest();
            resp = trans.getResponse();
            buf = resp.getResponseBuffer();
            outRec = trans.getOutputReceiver();
        }
        else if (obj instanceof OutputReceiver) {
            outRec = (OutputReceiver)obj;
        }
        return new HttpContextImpl(mServletContext, mLog, req, resp, buf, outRec, mHttpContextMBean);
    }
}
