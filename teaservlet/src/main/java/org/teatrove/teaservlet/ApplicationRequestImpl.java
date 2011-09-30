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

import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.teatrove.tea.runtime.TemplateLoader;

/**
 * 
 * @author Brian S O'Neill
 */
class ApplicationRequestImpl extends HttpServletRequestWrapper
    implements ApplicationRequest
{
    private final Object mTemplateOrLoader;
    private final Map mAppContextMap;
    private Object mID;
    // 0 = don't know, 1 = no, 2 = yes
    private int mCompression;

    public ApplicationRequestImpl(HttpServletRequest request,
                                  Map appContextMap,
                                  TemplateLoader.Template template) {
        super(request);
        mAppContextMap = appContextMap;
        mTemplateOrLoader = template;
    }

    public ApplicationRequestImpl(HttpServletRequest request,
                                  Map appContextMap,
                                  TemplateLoader loader) {
        super(request);
        mAppContextMap = appContextMap;
        mTemplateOrLoader = loader;
    }

    public TemplateLoader.Template getTemplate() {
        if (mTemplateOrLoader instanceof TemplateLoader.Template) {
            return (TemplateLoader.Template)mTemplateOrLoader;
        }
        else {
            return null;
        }
    }

    public TemplateLoader getTemplateLoader() {
        if (mTemplateOrLoader instanceof TemplateLoader.Template) {
            return ((TemplateLoader.Template)mTemplateOrLoader)
                .getTemplateLoader();
        }
        else {
            return (TemplateLoader)mTemplateOrLoader;
        }
    }

    public synchronized Object getIdentifier() {
        if (mID == null) {
            mID = new Object();
        }
        return mID;
    }

    public boolean isCompressionAccepted() {
        if (mCompression != 0) {
            return mCompression == 2;
        }
        
        // MSIE 4.x doesn't support the compression format produced
        // by the TeaServlet, even though it is still a legal GZIP stream.
        String userAgent = getHeader("User-Agent");
        if (userAgent != null) {
            int index = userAgent.indexOf("MSIE ");
            if (index > 0) {
                int index2 = userAgent.indexOf('.', index + 5);
                if (index2 > index) {
                    try {
                        int majorVersion = Integer.parseInt
                            (userAgent.substring(index + 5, index2));
                        if (majorVersion <= 4) {
                            mCompression = 1;
                            return false;
                        }
                    }
                    catch (NumberFormatException e) {
                    }
                }
            }
        }

        String value = getHeader("Accept-Encoding");

        if (value != null) {
            if (value.indexOf("gzip") >= 0) {
                mCompression = 2;
                return true;
            }
            Enumeration e = getHeaders("Accept-Encoding");
            while (e.hasMoreElements()) {
                value = (String)e.nextElement();
                if ("gzip".equals(value)) {
                    mCompression = 2;
                    return true;
                }
            }
        }

        mCompression = 1;
        return false;
    }

    public Map getApplicationContextTypes() {
        return mAppContextMap;
    }
}
