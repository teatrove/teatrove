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

package org.teatrove.barista.http;

import java.util.Map;
import org.teatrove.trove.net.SocketFace;
import org.teatrove.trove.net.HttpHeaderMap;

/**
 * @author Brian S O'Neill
 */
public class HttpServerConnectionWrapper implements HttpServerConnection {
    private final HttpServerConnection mCon;

    public HttpServerConnectionWrapper(HttpServerConnection con) {
        mCon = con;
    }

    public String getRequestScheme() {
        return mCon.getRequestScheme();
    }

    public String getRequestMethod() {
        return mCon.getRequestMethod();
    }

    public String getRequestProtocol() {
        return mCon.getRequestProtocol();
    }
    
    public String getRequestURI() {
        return mCon.getRequestURI();
    }

    public String getRequestPath() {
        return mCon.getRequestPath();
    }

    public String getRequestQueryString() {
        return mCon.getRequestQueryString();
    }

    public Map getRequestParameters() {
        return mCon.getRequestParameters();
    }
    
    public HttpHeaderMap getRequestHeaders() {
        return mCon.getRequestHeaders();
    }

    public Map getRequestCookies() {
        return mCon.getRequestCookies();
    }

    public boolean isResponseCommitted() {
        return mCon.isResponseCommitted();
    }

    public HttpHeaderMap getResponseHeaders() {
        return mCon.getResponseHeaders();
    }

    public Map getResponseCookies() {
        return mCon.getResponseCookies();
    }

    public void setResponseStatus(int code, String message) {
        mCon.setResponseStatus(code, message);
    }

    public String getResponseProtocol() {
        return mCon.getResponseProtocol();
    }
    
    public int getResponseStatusCode() {
        return mCon.getResponseStatusCode();
    }

    public String getResponseStatusMessage() {
        return mCon.getResponseStatusMessage();
    }

    public SocketFace getSocket() {
        return mCon.getSocket();
    }

    public long getBytesRead() {
        return mCon.getBytesRead();
    }

    public long getBytesWritten() {
        return mCon.getBytesWritten();
    }

    public Map getAttributeMap() {
        return mCon.getAttributeMap();
    }
}
