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

package org.teatrove.trove.util.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.trove.util.PropertyMapFactoryProvider;
import org.teatrove.trove.util.SubstitutionFactory;

public class DefaultResourceFactory implements ResourceFactory {
    
    /** Singleton instance. */
    private static final DefaultResourceFactory INSTANCE = 
        new DefaultResourceFactory();
    
    private PropertyMap substitutions;
    
    /**
     * Default constructor that uses default substitutions.
     * 
     * #see SubstitutionFactory#getDefaults()
     */
    public DefaultResourceFactory() {
        this(SubstitutionFactory.getDefaults());
    }
    
    /**
     * Create a resource factory that defaults to the given substitutions
     * unless explicitly stated otherwise.
     * 
     * @param substitutions  The default substitutions
     */
    public DefaultResourceFactory(PropertyMap substitutions) {
        this.substitutions = substitutions;
    }
    
    /**
     * Get the singleton instance of the factory.
     * 
     * @return  The default resource factory
     */
    public static DefaultResourceFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(String path) {
        
        // check if file-based path
        if (path.startsWith("file:")) {
            File file = new File(path.substring(5));
            if (!file.exists()) { return null; }
            
            try { return file.toURI().toURL(); }
            catch (MalformedURLException exception) { return null; }
        }
        
        // check if classpath-based path
        else if (path.startsWith("classpath:")) {
            int idx = 10;
            while (path.charAt(idx) == '/') { idx++; }
            return Thread.currentThread().getContextClassLoader()
                .getResource(path.substring(idx));
        }

        // check if URL-based path
        else {
            try { return new URL(path); }
            catch (MalformedURLException e1) {
                // default to file or class path
                File file = new File(path);
                if (file.exists()) { 
                    try { return file.toURI().toURL(); }
                    catch (MalformedURLException e2) { return null; }
                }
                else { 
                    int idx = 0;
                    while (path.charAt(idx) == '/') { idx++; }
                    return Thread.currentThread().getContextClassLoader()
                        .getResource(path.substring(idx)); 
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        URL url = getResource(path);
        if (url != null) {
            try { return url.openStream(); }
            catch (IOException ioe) { return null; }
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path) 
        throws IOException {
        
        return getResourceAsProperties(path, this.substitutions);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, InputStream input) 
        throws IOException {
    
        return getResourceAsProperties(path, input, this.substitutions);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, 
                                               PropertyMap substitutions)
        throws IOException {
        
        // get associated resource
        InputStream input = getResourceAsStream(path);
        if (input == null) { return null; }
        
        // lookup properties
        return getResourceAsProperties(path, input, substitutions);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, InputStream input,
                                               PropertyMap substitutions)
        throws IOException {
        
        if (input == null) { return null; }
        Reader reader = new InputStreamReader(input);
        
        // get associated factory
        PropertyMapFactory factory = 
            PropertyMapFactoryProvider.createPropertyMapFactory(path, reader,
                                                                substitutions);
        
        // return properties
        return factory.createProperties();
    }
}
