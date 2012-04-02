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

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.SubstitutionFactory;
import org.teatrove.trove.util.resources.DefaultResourceFactory;

public class TeaServletResourceFactory extends DefaultResourceFactory {

    private ServletContext servletContext;
    
    public TeaServletResourceFactory(ServletContext servletContext) {
        this(servletContext, SubstitutionFactory.getDefaults());
    }
    
    public TeaServletResourceFactory(ServletContext servletContext,
                                     PropertyMap substitutions) {
        super(substitutions);
        this.servletContext = servletContext;
    }
    
    public ServletContext getServletContext() {
        return this.servletContext;
    }
    
    public URL getResource(String path) {
        URL url = null;
        try { url = this.servletContext.getResource(path); }
        catch (MalformedURLException exception) { /* ignore */ }
        
        if (url == null) {
            url = super.getResource(path);
        }
        
        return url;
    }
}
