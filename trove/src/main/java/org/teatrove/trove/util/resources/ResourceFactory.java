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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactoryProvider;
import org.teatrove.trove.util.SubstitutionFactory;

public interface ResourceFactory {
    
    /**
     * Get a associated resource per the specified path.  The default method
     * will first check for a local file and then load from the class path.  If
     * no file is found, <code>null</code> is returned.
     * 
     * @param path  The name or path of the resource
     * 
     * @return  The associated URL of the resource
     */
    URL getResource(String path);
    
    /**
     * Get a associated input stream per the specified path.  The default method
     * will first check for a local file and then load from the class path.  If
     * no file is found, <code>null</code> is returned.
     * 
     * @param path  The name or path of the resource
     * 
     * @return  The associated input stream of the resource
     */
    InputStream getResourceAsStream(String path);
    
    /**
     * Get the associated resource as a {@link PropertyMap} per the specified
     * path.  This will load the resource per {@link #getResource(String)} and
     * if the resource is not valid, then <code>null</code> is returned.  By
     * default, any properties will be auto-substituted by the default
     * substitutions per {@link SubstitutionFactory#getDefaults()}.  The type of
     * resource is determined by the path.  For examples, 'properties' files
     * use a properties resource loader whereas 'xml' files use a XML resource
     * loader.  For more information, see {@link PropertyMapFactoryProvider}.
     * 
     * @param path  The name or path of the resource
     * 
     * @return  The associated list of properties
     * 
     * @throws IOException   If an error occurs loading or parsing the resource
     */
    PropertyMap getResourceAsProperties(String path) 
        throws IOException;
    
    /**
     * Get the associated resource as a {@link PropertyMap} per the specified
     * path.  This will load the resource per the given input stream and
     * if the resource is not valid, then <code>null</code> is returned.  By
     * default, any properties will be auto-substituted by the default
     * substitutions per {@link SubstitutionFactory#getDefaults()}.  The type of
     * resource is determined by the path.  For examples, 'properties' files
     * use a properties resource loader whereas 'xml' files use a XML resource
     * loader.  For more information, see {@link PropertyMapFactoryProvider}.
     * 
     * @param path  The name or path of the resource
     * @param input  The associated stream to load
     * 
     * @return  The associated list of properties
     * 
     * @throws IOException   If an error occurs loading or parsing the resource
     */
    PropertyMap getResourceAsProperties(String path, InputStream input) 
        throws IOException;
    
    /**
     * Get the associated resource as a {@link PropertyMap} per the specified
     * path.  This will load the resource per {@link #getResource(String)} and
     * if the resource is not valid, then <code>null</code> is returned.  Any 
     * properties will be auto-substituted by the specified substitutions.  The
     * type of resource is determined by the path.  For examples, 'properties' 
     * files use a properties resource loader whereas 'xml' files use a XML 
     * resource loader.  For more information, see 
     * {@link PropertyMapFactoryProvider}.
     * 
     * @param path  The name or path of the resource
     * @param substitutions  The map of substitutions to replace properties with
     * 
     * @return  The associated list of properties
     * 
     * @throws IOException   If an error occurs loading or parsing the resource
     */
    PropertyMap getResourceAsProperties(String path, PropertyMap substitutions)
        throws IOException;
    
    /**
     * Get the associated resource as a {@link PropertyMap} per the specified
     * path.  This will load the resource per the given input stream and
     * if the resource is not valid, then <code>null</code> is returned.  Any 
     * properties will be auto-substituted by the specified substitutions.  The
     * type of resource is determined by the path.  For examples, 'properties' 
     * files use a properties resource loader whereas 'xml' files use a XML 
     * resource loader.  For more information, see 
     * {@link PropertyMapFactoryProvider}.
     * 
     * @param path  The name or path of the resource
     * @param substitutions  The map of substitutions to replace properties with
     * 
     * @return  The associated list of properties
     * 
     * @throws IOException   If an error occurs loading or parsing the resource
     */
    PropertyMap getResourceAsProperties(String path, InputStream input,
                                        PropertyMap substitutions)
        throws IOException;
}
