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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    public URL getResource(String path)
        throws MalformedURLException {
        
        // support any protocol (file:, http:, etc)
        // support classpath: protocol
        // support web: protocol
        File file = new File(path);
        if (file.exists()) {
            return file.toURI().toURL();
        }
        else {
            return DefaultResourceFactory.class.getResource(path);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        try { return new FileInputStream(path); }
        catch (FileNotFoundException fnfe) {
            return DefaultResourceFactory.class.getResourceAsStream(path);
        }
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
