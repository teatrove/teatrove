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

package org.teatrove.barista.util;

import java.io.*;
import java.net.*;
import java.util.*;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.trove.util.PropertyParser;
import org.teatrove.trove.util.PropertyChangeListener;

/**
 * Load properties into a PropertyMap by parsing a file with a
 * {@link PropertyParser}. Understands an optional property, properties.master,
 * which accepts a file path or URL. The master properties file supplies
 * default properties which can be overridden. The master properties can also 
 * have a master, and so on.
 * 
 * @author Brian S O'Neill, Jonathan Colwell
 */
public class URLPropertyMapFactory implements PropertyMapFactory {
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

    private final URL mURL;
    private PropertyChangeListener mListener;

    public URLPropertyMapFactory(String path) throws MalformedURLException {
        mURL = makeURL(path);
    }

    public URLPropertyMapFactory(URL propertyURL) {
        mURL = propertyURL;
    }
    
    public void setPropertyListener(PropertyChangeListener listener) {
        mListener = listener;
    }

    public PropertyMap createProperties(PropertyChangeListener listener) 
        throws java.io.IOException {
        mListener = listener;
        PropertyMap map = createProperties();
        mListener = null;
        return map;
    }

    public PropertyMap createProperties() throws java.io.IOException {
        PropertyMap properties = new PropertyMap();
        Reader reader = null;
        reader = new BufferedReader(new InputStreamReader(mURL.openStream()));
        load(properties, reader, new HashSet());
        return properties;
    }

    private void load(PropertyMap properties, Reader reader, Set URLs)
        throws IOException
    {
        PropertyMap defaultProps = new PropertyMap();
        try {
            PropertyParser parser = new PropertyParser(defaultProps);
            if (mListener != null) {
                parser.addPropertyChangeListener(mListener);
            }
            parser.parse(new BufferedReader(reader));
            if (mListener != null) {
                parser.removePropertyChangeListener(mListener);
            }
        }
        finally {
            reader.close();
        }

        properties.putDefaults(defaultProps);

        String master = defaultProps.getString("properties.master");

        if (master != null && !URLs.contains(master)) {
            // Prevent master properties cycle.
            URLs.add(master);

            URL url = makeURL(master);
            load(properties, new BufferedReader
                 (new InputStreamReader(url.openStream())), URLs);
        }
    }
}
