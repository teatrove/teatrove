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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.teatrove.trove.util.resources.DefaultResourceFactory;
import org.teatrove.trove.util.resources.ResourceFactory;

public class SubstitutionFactory {
    private static final Pattern PARSER_PATTERN =
        Pattern.compile("\\$\\{([a-zA-Z0-9_-]+)(:([^}]+))?\\}");
    
    private SubstitutionFactory() {
        super();
    }
    
    @SuppressWarnings("unchecked")
    public static PropertyMap getDefaults() {
        PropertyMap vars = new PropertyMap();
        vars.putAll(System.getenv());
        for (Map.Entry<?, ?> entry : System.getProperties().entrySet()) {
            vars.put(entry.getKey().toString(), entry.getValue().toString());
        }
        
        return vars;
    }
    
    public static PropertyMap getSubstitutions(PropertyMap properties)
        throws IOException {
        
        return getSubstitutions(properties, 
                                DefaultResourceFactory.getInstance());
    }
    
    @SuppressWarnings("unchecked")
    public static PropertyMap getSubstitutions(PropertyMap properties,
                                               ResourceFactory resourceFactory)
        throws IOException {
        
        PropertyMap subs = new PropertyMap();
        
        // add environment variables
        if (properties == null || properties.getBoolean("env", true)) {
            subs.putAll(System.getenv());
        }
        
        // add system properties
        if (properties == null || properties.getBoolean("system", true)) {
            for (Map.Entry<?, ?> entry : System.getProperties().entrySet()) {
                subs.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        // add file
        if (properties != null) {
            String file = properties.getString("file");
            if (file != null) {
                PropertyMap props = 
                    resourceFactory.getResourceAsProperties(file);
                if (props != null) {
                    subs.putAll(props);
                }
            }
        }
        
        // add factory class
        if (properties != null && resourceFactory != null) {
            PropertyMap factoryMap = properties.subMap("factory");
            if (factoryMap != null && factoryMap.size() > 0) {
                try { subs.putAll(loadProperties(factoryMap)); }
                catch (Exception exception) {
                    throw new IOException("unable to load substitution factory", 
                                          exception);
                }
            }
        }
        
        // add provided properties
        if (properties != null) {
            PropertyMap props = properties.subMap("properties");
            if (props != null) {
                subs.putAll(props);
            }
        }
        
        // return subs
        return subs;
    }
    
    public static String substitute(String value) {
        return substitute(value, getDefaults());
    }

    public static String substitute(String value, PropertyMap vars) {
        // ignore if empty string
        if (value == null || value.isEmpty()) { return value; }

        // iterate while we have variables
        int idx = 0, current = 0, marker = 0;
        StringBuilder sb = new StringBuilder(value.length() + 32);

        while (idx >= 0) {
            idx = value.indexOf("${", current);
            if (idx >= 0) {
                // append prefix up to marker
                sb.append(value.substring(current, idx));

                // update marker to end of variable
                current = value.indexOf('}', idx) + 1;
                marker = value.indexOf(':', idx) + 1;
                if (marker >= current) {
                    marker = -1;
                }

                // lookup the variable
                String type = null, dflt = null;
                if (marker > 0) {
                    dflt = value.substring(marker, current - 1);
                    type = value.substring(idx + 2, marker - 1);
                }
                else {
                    type = value.substring(idx + 2, current - 1);
                }
                
                // attempt to match the value or set the default
                String match = null;
                if (vars.containsKey(type)) {
                    match = (String) vars.get(type);
                }
                else if (dflt != null) {
                    match = dflt;
                }
                
                // append value if valid, otherwise append match
                if (match == null) {
                    sb.append("${").append(type).append('}');
                }
                else { sb.append(match); }
            }
        }

        // append remaining content if applicable
        if (current < value.length()) {
            sb.append(value.substring(current));
        }

        // return replaced string
        return sb.toString();
    }

    protected String _substitute(String value, Map<String, String> vars) {

        // ignore if empty string
        if (value == null || value.isEmpty()) { return value; }

        // create matcher for url
        Matcher matcher = PARSER_PATTERN.matcher(value);

        // loop through matches, substituting as necessary
        int idx = 0;
        StringBuilder sb = new StringBuilder(value.length() + 32);
        while (matcher.find()) {
            // append non-matching portion
            sb.append(value.substring(idx, matcher.start()));

            // lookup type via variables
            String type = matcher.group(1);
            String match = vars.get(type);

            // TODO: add support for default value via colon matching per group2
            
            // append value if valid, otherwise append match
            if (match == null) { sb.append(matcher.group(0)); }
            else { sb.append(match); }

            // update index
            idx = matcher.end();
        }

        // append remaining unmatched portion
        if (idx < value.length()) {
            sb.append(value.substring(idx));
        }

        // return parsed result
        return sb.toString();
    }
    
    private static PropertyMap loadProperties(PropertyMap factoryProps) 
        throws Exception {
        
        String className = null;
        PropertyMapFactory factory = null;
        if (factoryProps != null && factoryProps.size() > 0
            && (className = factoryProps.getString("class")) != null) {
        
            // Load and use custom PropertyMapFactory.
            Class<?> factoryClass = Class.forName(className);
            java.lang.reflect.Constructor<?> ctor =
                factoryClass.getConstructor(new Class[]{Map.class});
            factory = (PropertyMapFactory)
                ctor.newInstance(new Object[]{factoryProps.subMap("init")});
        }
        
        // return properties
        return (factory == null ? null : factory.createProperties());
    }
}
