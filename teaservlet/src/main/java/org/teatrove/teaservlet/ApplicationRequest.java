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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.teatrove.tea.runtime.TemplateLoader;

/**
 * An ordinary HttpServletRequest, but with additional operations specific
 * to the TeaServlet.
 * 
 * @author Brian S O'Neill
 */
public interface ApplicationRequest extends HttpServletRequest {
    /**
     * Returns the template that is processing this request, or null if this
     * request was not initiated by a template.
     */
    public TemplateLoader.Template getTemplate();

    /**
     * Returns a TemplateLoader for selecting and executing templates. If this
     * ApplicationRequest was initiated by a template, then its TemplateLoader
     * matches the one returned from this method.
     */
    public TemplateLoader getTemplateLoader();

    /**
     * Returns an object, whose instance uniquely identifies this request.
     */
    public Object getIdentifier();

    /**
     * Returns true if this request will accept a GZIP compressed response.
     */
    public boolean isCompressionAccepted();
    
    /**
     * Returns an unmodifiable mapping of Application instances to the
     * context type Class expected from it, for this request. Applications that
     * do not provide a context will map to null.
     */
    public Map getApplicationContextTypes();
}
