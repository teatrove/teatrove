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

import org.teatrove.trove.util.*;

import java.util.*;

/**
 * A mechanism for making sure the properties specified are valid. Valid 
 * properties are determined either by checking the type of the property, or if
 * specific words or values are required, making sure that those values are 
 * present.
 *
 * @author Jonathan Colwell, Scott Jappinen
 */
public class PropertyMapValidator {
    private static final String DELIMETER = "$";
    
    /**
     * verifies that the properties comply with the structure specified
     * in validator.properties
     */
    public static final void validatePropertyMap(PropertyMap properties,
                                                 PropertyMap specification) 
        throws PropertyException
    {
        PropertyMap propertiesCopy = new PropertyMap(properties);
        PropertyMap specificationCopy = new PropertyMap(specification);
        resolveWildcardKeys(propertiesCopy, specificationCopy);
        
        Iterator specEntryIterator = specificationCopy.entrySet().iterator();
        while (specEntryIterator.hasNext()) {
            Map.Entry entry = (Map.Entry) specEntryIterator.next();
            String specValue = entry.getValue().toString();
            String specKey = entry.getKey().toString();
            
            if (!resolveFreeTrees(specKey, specValue, propertiesCopy)) {
                String propsValue = (String) propertiesCopy.remove(specKey);
                if (propsValue != null) {
                    if (!isSimpleType(specKey, specValue, propsValue)) {
                        if (!isEnumType(specKey, specValue, propsValue)) {
                            throw new PropertyException(specKey + " has an unknown type for its value.");
                        }
                    }
                } else {
                    if (isValueRequired(specValue)) {
                        throw new PropertyException(specKey + " requires a value.");
                    }               
                }
            }
        }
    }
                                                
    private static final boolean isValueRequired(String specValue) {
        return (specValue.indexOf("required") >= 0);
    }
    
    private static final boolean isSimpleType(String specKey, String specValue, String propValue) 
        throws PropertyException 
    {
        boolean result = true;
        if (specValue.indexOf("string") >= 0) ;
        else if (specValue.indexOf("boolean") >= 0) checkForBoolean(propValue, specKey);
        else if (specValue.indexOf("int") >= 0) checkForInt(propValue, specKey);
        else if (specValue.indexOf("short") >= 0) checkForShort(propValue, specKey);      
        else if (specValue.indexOf("long") >= 0) checkForLong(propValue, specKey);
        else if (specValue.indexOf("float") >= 0) checkForFloat(propValue, specKey);
        else if (specValue.indexOf("double") >= 0) checkForDouble(propValue, specKey);
        else result = false;
        return result;
    }
    
    private static final boolean isEnumType(String specKey, String specValue, String propValue)
        throws PropertyException 
    {
        boolean result = false;
        String enumeration = specValue.substring(specValue.lastIndexOf(DELIMETER) + 1);
        if (enumeration != null) {
            StringTokenizer tokenizer = new StringTokenizer(enumeration, "\"|");
            while (tokenizer.hasMoreTokens()) {
                String value = tokenizer.nextToken();
                if (value.equalsIgnoreCase(propValue)){
                    result = true;
                    break;
                }
            }
            if (!result) {
                throw new PropertyException(specKey + " must be of type(s) " + enumeration + ".");
            }
        }
        return result;
    }
    
    private static final void checkForBoolean(String value, String specKey)
        throws PropertyException 
    {
        if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
            throw new PropertyException(specKey + " requires value of type boolean.");
        }       
    }

    private static final void checkForShort(String value, String specKey)
        throws PropertyException 
    {
        try {  
            Short.parseShort(value);
        } catch (NumberFormatException nfe) {
            throw new PropertyException(specKey + " requires a value of type short.");
        }       
    }

    private static final void checkForInt(String value, String specKey)
        throws PropertyException 
    {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw new PropertyException(specKey + " requires a value of type int.");
        }       
    }

    private static final void checkForLong(String value, String specKey)
        throws PropertyException 
    {
        try {           
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            throw new PropertyException(specKey + " requires a value of type long.");
        }       
    }

    private static final void checkForFloat(String value, String specKey)
        throws PropertyException 
    {
        try {
            Float.parseFloat(value);
        } catch (NumberFormatException nfe) {
            throw new PropertyException(specKey + " requires a value of type float.");
        }       
    }

    private static final void checkForDouble(String value, String specKey)
        throws PropertyException 
    {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            throw new PropertyException(specKey + " requires a value of type double.");
        }       
    }

    /* Replaces any instance of anyString in the specification key and replaces it with
     * the actual value that is used in properties. This is so that later validation steps
     * can be performed. */
    private static final void resolveWildcardKeys(PropertyMap properties,
                                                  PropertyMap specification) {
        Iterator keyIterator = specification.keySet().iterator();
        while (keyIterator.hasNext()) {
            String thisKey = keyIterator.next().toString();
            int wildcardIndex = -1;
            if ((wildcardIndex = thisKey.indexOf("anyString")) >= 0) {
                Object specValue = specification.remove(thisKey);
                String prefix = thisKey.substring(0, wildcardIndex);
                String suffix = thisKey.substring(wildcardIndex + "anyString".length());
                PropertyMap substitutionMap = properties.subMap(prefix.substring(0, prefix.length() - 1));
                Set infixSet = substitutionMap.subMapKeySet();
                Iterator iterator = infixSet.iterator();
                while (iterator.hasNext()) {
                    String infix = (String) iterator.next();
                    String resultKey = prefix + infix + suffix;
                    specification.put(resultKey, specValue);                    
                }
            }           
        }
    }
                                                
    private static final boolean resolveFreeTrees(String specKey, String specValue, PropertyMap properties) 
        throws PropertyException
    {
        boolean result;
        if (specValue.equals("freeTree")) {
            PropertyMap subMap = properties.subMap(specKey);
            if (!subMap.isEmpty()) {
                subMap.clear();
                result = true;
            } else {
                throw new PropertyException(specKey + " requires a value.");
            }
        } else {
            result = false;
        }
        return result;
    }
}
