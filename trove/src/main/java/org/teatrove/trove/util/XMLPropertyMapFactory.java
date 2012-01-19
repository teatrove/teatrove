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

package org.teatrove.trove.util;

import java.io.IOException;
import java.io.Reader;

import org.jdom.Document;
import org.jdom.Element;

public class XMLPropertyMapFactory implements PropertyMapFactory {

    private Reader reader;
    private boolean stripRoot;
    
    public XMLPropertyMapFactory(Reader reader) {
        this(reader, false);
    }
    
    public XMLPropertyMapFactory(Reader reader, boolean stripRoot) {
        this.reader = reader;
        this.stripRoot = stripRoot;
    }
    
    @Override
    public PropertyMap createProperties() 
        throws IOException {
        
        return createProperties(null);
    }

    @Override
    public PropertyMap createProperties(PropertyChangeListener listener)
        throws IOException {
        
        try {
            Document document = XMLMapFactory.createDocument(reader);
            
            Element element = document.getRootElement();
            PropertyMap properties =
                XMLMapFactory.getPropertyMapFromElement(element);
            
            if (this.stripRoot && properties.size() > 0) {
                properties = properties.subMap(element.getName());
            }
            
            return properties;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
}
