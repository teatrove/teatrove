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

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.trove.util.*;

/**
 * @author Jonathan Colwell
 */
public class ErrorForwardingFilter implements Filter {

    /** HTTP status code parameter received by error forwarding URI.*/
    public static final String STATUS_CODE_PARAM = "statusCode";

    /** Message parameter received by error forwarding URI. */
    public static final String MESSAGE_PARAM = "message";

    /** Path of original request, passed by error forwarding URI. */
    public static final String PATH_PARAM = "path";

    private static final String ATTR_NAME =
        "org.teatrove.barista.http.ErrorForwardingFilter";
    private static final Object ATTR_VALUE = new Object();
    
    protected PatternMatcher mErrorForwardingMatcher;
    protected FilterConfig mConfig;

    public void init(FilterConfig config) {
        mConfig = config;
        Map patterns = new HashMap();
        Enumeration names = config.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement().toString();
            patterns.put(name,config.getInitParameter(name));
        }
        mErrorForwardingMatcher = PatternMatcher.forPatterns(patterns);
    }

    public void destroy() {
        mConfig = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
        throws IOException, ServletException
    {
        // Create a response wrapper to pass up the chain to allow error
        // forwarding on the way back out. If the response is not an
        // HttpServletResponse don't wrap but still call on up the chain.
        // Use a special attribute to prevent multiple wrapping of the
        // response.

        if (request instanceof HttpServletRequest &&
            response instanceof HttpServletResponse &&
            request.getAttribute(ATTR_NAME) != ATTR_VALUE) {

            request.setAttribute(ATTR_NAME, ATTR_VALUE);

            response = new ErrorForwardingWrapper
                ((HttpServletRequest)request, (HttpServletResponse)response);
        }

        chain.doFilter(request, response);
    }

    /**
     * @deprecated
     */
    public void setFilterConfig(FilterConfig config) {
        if (config == null) {
            destroy();
        }
        else {
            init(config);
        }
    }

    /**
     * @deprecated
     */
    public FilterConfig getFilterConfig() {
        return mConfig;
    }
    
    private class ErrorForwardingWrapper extends HttpServletResponseWrapper {
        HttpServletRequest mRequest;
        HttpServletResponse mResponse;
        
        public ErrorForwardingWrapper(HttpServletRequest req,
                                      HttpServletResponse response) {
            super(response);
            mRequest = req;
            mResponse = response;
        }

        public void sendError(int sc) 
            throws IOException,IllegalStateException {
            sendError(sc, null);
        }

        public void sendError(int sc, String msg) 
            throws IOException, IllegalStateException 
        {
            if (isCommitted()) {
                throw new IllegalStateException
                    ("Response has already been committed");
            }

            PatternMatcher.Result result = mErrorForwardingMatcher
                .getMatch(mRequest.getRequestURI());

            String forward;
            if (result == null ||
                (forward = (String)result.getValue()) == null) {
                super.sendError(sc, msg);
                return;
            }

            StringBuffer buf = new StringBuffer(200);

            String query = null;
            int index = forward.indexOf('?');
            if (index < 0) {
                buf.append(forward);
            }
            else {
                buf.append(forward.substring(0, index));
                if (index + 1 < forward.length()) {
                    query = forward.substring(index + 1);
                }
            }

            buf.append('?');
            buf.append(STATUS_CODE_PARAM);
            buf.append('=');
            buf.append(sc);
            buf.append('&');
            buf.append(MESSAGE_PARAM);
            buf.append('=');
            if (msg != null) {
                buf.append(encodeURL(msg));
            }
            buf.append('&');
            buf.append(PATH_PARAM);
            buf.append('=');
            buf.append(encodeURL(mRequest.getRequestURI()));
            if (query != null) {
                buf.append('&');
                buf.append(query);
            }
    
            RequestDispatcher disp = 
                mRequest.getRequestDispatcher(buf.toString());

            if (disp == null) {
                super.sendError(sc, msg);
                return;
            }

            try {
                disp.forward(mRequest, mResponse);
            }
            catch (Exception e) {
                e.printStackTrace();
                super.sendError(sc, msg);
            }
        }
    }
}

