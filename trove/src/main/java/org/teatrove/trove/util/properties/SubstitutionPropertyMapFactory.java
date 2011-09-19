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

package org.teatrove.trove.util.properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.teatrove.trove.log.Syslog;
import org.teatrove.trove.util.PropertyChangeListener;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.trove.util.PropertyParser;

/*
 * @author: Josh Yockey -- Feb 11, 2005
 */
public class SubstitutionPropertyMapFactory implements PropertyMapFactory {

    private URL m_url;
    private PropertyChangeListener m_listener;
    
    public static final String DEFAULT_KEY_START = "$(";
    public static final String DEFAULT_KEY_END = ")";
    
    public SubstitutionPropertyMapFactory(String sFile) throws MalformedURLException {
        m_url = makeURL(sFile);
    }
    
    public SubstitutionPropertyMapFactory(URL u) {
        m_url = u;
    }
    
    public PropertyMap createProperties() throws IOException {
        PropertyMap properties = new PropertyMap();
        Syslog.debug(getClass().getName() + " loading properties from " + m_url);
        Reader reader = new BufferedReader(new InputStreamReader(m_url.openStream()));
        load(properties, reader);
        return properties;
    }

    public PropertyMap createProperties(PropertyChangeListener listener)
            throws IOException {
        m_listener = listener;
        PropertyMap map = createProperties();
        m_listener = null;
        return map;
    }
    
    private void load(PropertyMap properties, Reader reader) throws IOException
    {
        // first pass - load all properties from the file
        try {
            PropertyParser parser = new PropertyParser(properties);
            if (m_listener != null) {
                parser.addPropertyChangeListener(m_listener);
            }
            parser.parse(reader);
            if (m_listener != null) {
                parser.removePropertyChangeListener(m_listener);
            }
        }
        finally {
            reader.close();
        }
        
        PropertyMap propsSub = properties.subMap("properties.substitution");
        
        SubstitutionFactory subs = initSubstitutionFactory(propsSub);
        
        String sSubStart = propsSub.getString("key.start", DEFAULT_KEY_START);
        String sSubEnd = propsSub.getString("key.end", DEFAULT_KEY_END);
        
        // check for requested substitutions
        
        // this is NOT recursive.  If your substitution is a substitution-formatted string
        // it will be inserted literally, not interpreted.
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            
            String sKey = (String)i.next();
            String sValue = properties.getString(sKey);
            
            if (sValue.startsWith(sSubStart) && sValue.endsWith(sSubEnd)) {
                // we need a substitution!
                String sName = sValue.substring(sSubStart.length(), sValue.length() - sSubEnd.length());
                String sNew = subs.getSubstitution(sName);
                
                if (sNew == null) {
                    Syslog.error("No substitution found for value (" + sName + ") in key " + sKey);
                    continue;
                }
                
                // TODO is there danger of ConcurrentModificationException here?
                properties.put(sKey, sNew);
            }
        }

        
    }
    
    private SubstitutionFactory initSubstitutionFactory(PropertyMap props) {
                
        if (props == null) return null;
        
        String sClass = props.getString("factory.class");
        if (sClass == null) return null;
        
        SubstitutionFactory factory;
        try {
            Class clazz = Class.forName(sClass);
            factory = (SubstitutionFactory)clazz.newInstance();
        } catch (ClassNotFoundException e) {
            Syslog.error("Can't locate SubstitutionMap class: " + sClass);
            Syslog.error(e);
            return null;
        } catch (ClassCastException e) {
            Syslog.error("Class " + sClass + " is not an instance of " + 
                    SubstitutionFactory.class.getName());
            Syslog.error(e);
            return null;
        } catch (InstantiationException e) {
            Syslog.error(e);
            return null;
        } catch (IllegalAccessException e) {
            Syslog.error(e);
            return null;
        }
        factory.init(props.subMap("factory.init"));
        return factory;
    }
    
    private static URL makeURL(String path) throws MalformedURLException {
        path = path.replace(File.separatorChar, '/');

        try {
            return new URL(path);
        }
        catch (MalformedURLException e) {
            try {
                File file = new File(path).getAbsoluteFile();
                try {
                    file = file.getCanonicalFile();
                }
                catch (IOException e2) {
                }
                return new URL("file:" + file.getPath());
            }
            catch (MalformedURLException e2) {
                throw e;
            }
        }
    }

}
