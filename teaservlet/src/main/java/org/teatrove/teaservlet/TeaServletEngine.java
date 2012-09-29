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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.teatrove.tea.engine.Template;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateSource;
import org.teatrove.teaservlet.assets.AssetEngine;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.util.PropertyMap;

/**
 * This interface allows other servlets to create
 * {@link TeaServletTransaction TeaServletTransactions}. When the TeaServlet is
 * initialized, it adds an attribute to its ServletContext named
 * "org.teatrove.teaservlet.TeaServletEngine". The attribute's value is a
 * TeaServletEngine array. The number of array elements matches the number of
 * times a TeaServlet is configured in. Use TeaServletEngine's name to
 * distinguish between different instances.
 * <p>
 * Servlets that request a TeaServletTransaction should generally let all
 * output be handled by it. This is because it will try to set headers and use
 * a servlet output stream.
 *
 * @author Brian S O'Neill
 */
public interface TeaServletEngine extends ApplicationConfig {
    /**
     * Creates a TeaServletTransaction instance for the given request/response
     * pair and returns it.
     *
     * @param request HttpServletRequest used for building ApplicationRequest.
     * @param response HttpServletResponse used for building
     * ApplicationResponse.
     */
    public TeaServletTransaction createTransaction
        (HttpServletRequest request, HttpServletResponse response)
        throws IOException;

    public TeaServletTransaction createTransaction
        (HttpServletRequest request, HttpServletResponse response,
         boolean lookupTemplate)
        throws IOException;


    public Template findTemplate(String uri,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws IOException, ServletException;

    public Template findTemplate(String uri,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 TemplateSource templateSrc)
        throws IOException, ServletException;

    public ApplicationDepot getApplicationDepot();

    public AssetEngine getAssetEngine();
    
    public TeaServletTemplateSource getTemplateSource();

    public TemplateCompilationResults reloadContextAndTemplates(boolean all)
        throws Exception;

    public String[] getTemplatePaths();

    public PropertyMap getProperties();

    public boolean isProfilingEnabled();
    
    public LogEvent[] getLogEvents();

    public void destroy();
}
