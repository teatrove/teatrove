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

import java.util.*;
import javax.servlet.*;
import org.teatrove.trove.log.*;
import org.teatrove.trove.util.*;

/**
 * Provides configuration for filters.
 * @author Jonathan Colwell
 */
public class FilterConfigImpl implements FilterConfig {
    private Log mLog;
    private PropertyMap mParameters;
    private ServletContext mContext;

    public FilterConfigImpl(Log log,
                            PropertyMap initParameters,
                            ServletContext context) {
        mLog = log;
        mParameters = initParameters;
        mContext = new LogContext(context, log);
    }
    
    public String getFilterName() {
        return mLog.getName();
    }
    
    public String getInitParameter(String name) {
        return mParameters.getString(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(mParameters.keySet());
    }    

    public ServletContext getServletContext() {
        return mContext;
    }
}















