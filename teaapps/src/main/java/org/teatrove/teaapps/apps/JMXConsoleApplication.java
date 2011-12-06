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

import javax.servlet.ServletException;

import org.teatrove.teaapps.contexts.JMXContext;
import org.teatrove.teaservlet.AdminApp;
import org.teatrove.teaservlet.AppAdminLinks;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.teaservlet.RegionCachingContext;
import org.teatrove.trove.log.Log;


/*
 * @author Scott Jappinen
 */
public class JMXConsoleApplication implements AdminApp {

	private Log mLog;
	
	private JMXContext mContext = null;
	
	public void init(ApplicationConfig config) throws ServletException {
		mLog = config.getLog();
		mContext = new JMXContext();
	}
	
    public void destroy() {}

    /**
     * Returns an instance of {@link RegionCachingContext}.
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new JMXContext();
    }

    /**
     * Returns {@link RegionCachingContext}.class.
     */
    public Class<JMXContext> getContextType() {
        return JMXContext.class;
    }
	
    /*
     * Retrieves the administrative links for this application.
     */
    public AppAdminLinks getAdminLinks() {
    	AppAdminLinks links = new AppAdminLinks(mLog.getName());
        links.addAdminLink("JMX Console","system.teaservlet.JMXConsole");
        return links;
    }
	
}
