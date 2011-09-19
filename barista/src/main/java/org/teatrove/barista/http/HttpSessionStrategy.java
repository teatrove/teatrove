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

import org.teatrove.trove.util.PropertyMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * HttpSessionStrategy allows any kind of HTTP session implementation to be
 * provided for a HttpServletRequest.
 *
 * @author Brian S O'Neill
 * @see HttpServletDispatcher
 */
public interface HttpSessionStrategy {

    public void init(PropertyMap properties);

    public SessionInfo[] getSessionInfos();

    public Support createSupport
        (HttpServletRequest request, HttpServletResponse response);

    public interface Support {
        /**
         * @see HttpServletRequest#getSession(boolean)
         */
        public HttpSession getSession(ServletContext context, boolean create);

        /**
         * @see HttpServletRequest#getSession()
         */
        public HttpSession getSession(ServletContext context);

        /**
         * @see HttpServletRequest#getRequestedSessionId()
         */
        public String getRequestedSessionId();

        /**
         * @see HttpServletRequest#isRequestedSessionIdValid()
         */
        public boolean isRequestedSessionIdValid();

        /**
         * @see HttpServletRequest#isRequestedSessionIdFromCookie()
         */
        public boolean isRequestedSessionIdFromCookie();

        /**
         * @see HttpServletRequest#isRequestedSessionIdFromURL()
         */
        public boolean isRequestedSessionIdFromURL();
    }
}
