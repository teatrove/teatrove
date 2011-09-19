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

import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class XMLMapFactory {

	public static final Document createDocument(Reader reader) throws Exception {
		Document result = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			result = builder.build(reader);
		} catch(Exception e) {
			throw e;
		}
		return result;
	}

	public static final PropertyMap getPropertyMapFromElement(Element element) {
		PropertyMap result = null;
		if (element != null) {
			result = new PropertyMap();
			String name = element.getName();
			String value = element.getTextTrim();                 
			if (name != null) {
				if (value != null && !value.equals("")) {
					result.put(name, value);
				} else {
					List childList = element.getChildren();
					PropertyMap childMap = getChildPropertyMap(childList);
					if (childMap != null && !childMap.isEmpty()) {
						PropertyMap subMap = result.subMap(name);
						subMap.putAll(childMap);
					}
				}
			}
		}
		return result;
	}
    
    private static final PropertyMap getChildPropertyMap(List list) {
        PropertyMap result = null;
        if (list != null) {
            result = new PropertyMap();
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                Object child = iterator.next();
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    String childName = childElement.getName();
                    String childValue = childElement.getTextTrim();                 
                    if (childName != null) {
                        if (childValue != null && !childValue.equals("")) {
                            result.put(childName, childValue);
                        } else {
                            List childList = childElement.getChildren();
                            PropertyMap childMap = getChildPropertyMap(childList);
                            if (childMap != null && !childMap.isEmpty()) {
                                PropertyMap subMap = result.subMap(childName);
                                subMap.putAll(childMap);
                            }
                        }
                    }
                }
            }           
        }
        return result;
    }
}
