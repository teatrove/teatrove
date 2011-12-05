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
import org.teatrove.tea.runtime.OutputReceiver;

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.teaservlet.HttpContext;
import org.teatrove.trove.log.Log;

import javax.servlet.ServletException;

/**
 * 
 * @author Scott Jappinen
 */
public class CaptureApplication implements Application {
    
    private Log mLog;
    private String mOutputPath = null;
    
    /**
     * Initializes the application.
     * @param config an ApplicationConfig object containing config. info.
     */
    public void init(ApplicationConfig config) throws ServletException {     
        mLog = config.getLog();
    }
    
    /**
     * Creates a context for the templates.
     * @param request The user's http request
     * @param response The user's http response
     * @return the context for the templates
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new CaptureContext(response);
    }
    
    /**
     * Returns the ContextType for this application.
     */
    public Class getContextType() {
        return CaptureContext.class;
    }
    
    /**
     * Called before the application is closed down.
     */
    public void destroy() {}
    
    public class CaptureContext {
        private ApplicationResponse mResponse;
        
        public CaptureContext(ApplicationResponse response) {
            mResponse = response;
        }
        
        public String capture(Substitution substitution) {
            BodyReceiver receiver = new BodyReceiver(mResponse.getHttpContext());
            String result = null;
            try {
                mResponse.stealOutput(substitution, receiver);
                result = receiver.getBody();
            } catch (Exception e) {
                mLog.error(e);
            }
            return result;
        }        
    }

    class BodyReceiver implements OutputReceiver {
        StringBuffer mBuffer = new StringBuffer(256);
        HttpContext mHttpContext = null;

        public BodyReceiver(HttpContext context) {
            mHttpContext = context;
        }
        
        public void print(Object obj) {
            mBuffer.append(mHttpContext.toString(obj));
        }

        public void write(char[] cbuf) {
            mBuffer.append(cbuf);
        }
            
        public void write(char[] cbuf, int off, int len) {
            mBuffer.append(cbuf, off, len);
        }
            
        public void write(int c) {
            mBuffer.append((char) c);
        }
            
        public void write(String str) {
            mBuffer.append(str);
        }
            
        public void write(String str, int off, int len) {
            mBuffer.append(str.substring(off, off + len));
        }   
        
        public String getBody() {
            return mBuffer.toString();
        }
    }
}
