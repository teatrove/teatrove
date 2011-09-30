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

import java.beans.*;
import java.util.*;
import java.lang.ref.*;

/**
 * A JavaBean Introspector that ensures interface properties are properly
 * discovered.
 * 
 * @author Brian S O'Neill
 */
public class CompleteIntrospector {
    // Weakly maps Class objects to softly referenced PropertyDescriptor maps.
    private static Map cPropertiesCache;

    static {
        cPropertiesCache = new IdentityMap();

        // The Introspector has a poor design in that a special GLOBAL setting
        // is used. The default setting has a negative affect because the
        // BeanInfo search path disregards packages. By default, the search
        // path provides a BeanInfo for Component, which is for
        // java.awt.Component. However, any class with this name, in any
        // package, will be given the visible properties of java.awt.Component.
        Introspector.setBeanInfoSearchPath(new String[0]);
    }

    /**
     * Test program.
     */
    public static void main(String[] args) throws Exception {
        Map map = getAllProperties(Class.forName(args[0]));
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            PropertyDescriptor desc = (PropertyDescriptor)map.get(key);
            System.out.println(key + " = " + desc);
        }
    }

    /**
     * A function that returns a Map of all the available properties on
     * a given class including write-only properties. The properties returned
     * is mostly a superset of those returned from the standard JavaBeans 
     * Introspector except more properties are made available to interfaces.
     * 
     * @return an unmodifiable mapping of property names (Strings) to
     * PropertyDescriptor objects.
     *
     */
    public static Map getAllProperties(Class clazz)
        throws IntrospectionException {
        
        synchronized (cPropertiesCache) {
            Map properties;

            Reference ref = (Reference)cPropertiesCache.get(clazz);
            if (ref != null) {
                properties = (Map)ref.get();
                if (properties != null) {
                    return properties;
                }
                else {
                    // Clean up cleared reference.
                    cPropertiesCache.remove(clazz);
                }
            }

            properties = Collections.unmodifiableMap(createProperties(clazz));
            cPropertiesCache.put(clazz, new SoftReference(properties));
            return properties;
        }
    }

    private static Map createProperties(Class clazz)
        throws IntrospectionException {

        Map properties = new HashMap();

        if (clazz == null || clazz.isPrimitive()) {
            return properties;
        }
        
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(clazz);
        }
        catch (LinkageError e) {
            throw new IntrospectionException(e.toString());
        }

        if (info != null) {
            PropertyDescriptor[] pdArray = info.getPropertyDescriptors();
            
            // Standard properties.
            int length = pdArray.length;
            for (int i=0; i<length; i++) {
                properties.put(pdArray[i].getName(), pdArray[i]);
            }
        }

        // Properties defined in Object are also available to interfaces.
        if (clazz.isInterface()) {
            properties.putAll(getAllProperties(Object.class));
        }

        // Ensure that all implemented interfaces are properly analyzed.
        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            properties.putAll(getAllProperties(interfaces[i]));
        }

        // Filter out properties with names that contain '$' characters.
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()) {
            String propertyName = (String)it.next();
            if (propertyName.indexOf('$') >= 0) {
                it.remove();
            }
        }

        return properties;
    }
}
