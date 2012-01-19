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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.SubstitutionFactory;
import org.teatrove.trove.util.resources.DefaultResourceFactory;

public class TeaServletResourceFactory extends DefaultResourceFactory {

    private PropertyMap substitutions;
    private ServletContext servletContext;
    
    public TeaServletResourceFactory(ServletContext servletContext) {
        this(servletContext, SubstitutionFactory.getDefaults());
    }
    
    public TeaServletResourceFactory(ServletContext servletContext,
                                     PropertyMap substitutions) {
        super();
        this.servletContext = servletContext;
        this.substitutions = substitutions;
    }
    
    public ServletContext getServletContext() {
        return this.servletContext;
    }
    
    public URL getResource(String path)
        throws MalformedURLException {
        
        URL url = this.servletContext.getResource(path);
        if (url == null) {
            url = super.getResource(path);
        }
        
        return url;
    }
    
    public InputStream getResourceAsStream(String path) {
        InputStream input = this.servletContext.getResourceAsStream(path);
        if (input == null) {
            input = super.getResourceAsStream(path);
        }
        
        return input;
    }
    
    public PropertyMap getResourceAsProperties(String path) 
        throws IOException {
        
        return this.getResourceAsProperties(path, substitutions);
    }
}
