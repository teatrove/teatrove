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

package org.teatrove.barista.validate;

import org.teatrove.trove.util.PropertyMap;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/** 
 * @author Sean T. Treat
 */
public class RulesEvaluator {

    //
    // static fields and methods
    //
    private final static String RULES_SUFFIX = ".properties";
    public final static String KEY_PREFIX = "<";
    public final static String TYPE_BOOLEAN = "Boolean";
    public final static String TYPE_NUMBER = "Number";
    public final static String TYPE_STRING = "String";
    public final static String DELIMITER = "&";
    
    /**
     * Returns an URL that points to the property files containing the
     * rules for the specified class. If the rules file doesn't exit, then
     * null is returned. NOTE: The 
     */
    public static URL getRulesURLForClass(Class clazz) {
        String name = clazz.getName() + RULES_SUFFIX;
        return clazz.getClassLoader().getResource(name);
    }

    public static URL getRulesURLForClass(String className) {
        String name = className + RULES_SUFFIX;
        return ClassLoader.getSystemClassLoader().getResource(name);
    }

    public static List getOptionalItemList(PropertyMap rules) {
        List l = null;
        if (rules != null) {
            String optionals = rules.getString("<optionalItems>");
            l = parseMultiString(optionals);
        }
        
        return ((l != null) ? l : new ArrayList());
    }

    /**
     * Parses a string delimited by DELIMITER and returns a List containing
     * each String in multi.
     */
    protected static List parseMultiString(String multi) {
        try {
            StringTokenizer tok = new StringTokenizer(multi, DELIMITER);
            List items = new ArrayList(tok.countTokens());
            while (tok.hasMoreTokens()) {
                items.add(tok.nextToken());
            }
            return items;
        }
        catch (NullPointerException npe) {
            return null;
        }
    }

    //
    // instance fields and methods
    //
    private PropertyMap mMap;

    public RulesEvaluator(PropertyMap map) {
        mMap = map;
    }

    /**
     * Returns true if this item is required.
     * default: false
     */
    public boolean isRequired(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<required>";
        return mMap.getBoolean(key, false);
    }

    public boolean isRequired() {
        return isRequired(null);
    }


    /**
     * Returns true if this item has been declared strict. Strict means that no
     * extra parameters may be inserted into this map item.
     *
     * default: false
     */
    public boolean isStrict(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<strict>";
        return mMap.getBoolean(key, false);
    }

    public boolean isStrict() {
        return isStrict(null);
    }

    /**
     * Returns a long indicating minimum number
     * default: 0
     */
    public int getMin(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<min>";
        return mMap.getInt(key, Integer.MIN_VALUE);
    }

    public int getMin() {
        return getMin(null);
    }

    /**
     * Returns a  long indicating a maximum number.
     * default: 0
     */
    public int getMax(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<max>";
        return mMap.getInt(key, Integer.MAX_VALUE);
    }

    public int getMax() {
        return getMax(null);
    }

    /**
     * Returns a  long indicating a maximum number.
     * default: 0
     */
    public String getDefault(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<default>";
        return mMap.getString(key);
    }

    public String getDefault() {
        return getDefault(null);
    }

    /**
     * Returns the name of the data type represented by keyName
     * default: string
     */
    public String getType(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<type>";
        return mMap.getString(key, "String");
    }

    public String getType() {
        return getType(null);
    }

    /**
     * Returns the available choices for keyName. 
     * default: null
     */
    public String[] getChoices(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<choices>";
        String choices =  mMap.getString(key);

        try {
            List l = parseMultiString(choices);
            Iterator it = l.iterator();
            String[] c = new String[l.size()];
            int i = 0;

            while (it.hasNext()) {
                c[i] = (String)it.next();
                ++i;
            }
            return c;
        }
        catch (NullPointerException npe) {
            // not found
            return null;
        }
    }

    public String[] getChoices() {
        return getChoices(null);
    }

    public String getMappingItem(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<mappingItem>";
        return  mMap.getString(key);
    }

    public String getMappingItem() {
        return getMappingItem(null);
    }

    /**
     * Returns the name of the rules section to compare subitems to.For 
     * example:
     * servlets {
     *     "hello world" {
     *     }
     *
     *    "foobar" {
     *    }
     * }
     *
     * Since each servlet has a unique name, we need to define a subitem 
     * type to compare these servlets to.
     * default: null
     */
    public String getSubItemName(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<subItem>";
        return  mMap.getString(key);
    }

    public String getSubItemName() {
        return getSubItemName(null);
    }

    /**
     * 
     */
    public String getCustomRulesFieldName(String keyName) {
        String key = ((keyName != null) ? (keyName + mMap.getSeparator()) : "")
            + "<rules>";
        return mMap.getString(key);
    }

    public String getCustomRulesFieldName() {
        return getCustomRulesFieldName(null);
    }

    public boolean checkType(String type, Object item) {
        if(item != null) {
            // System.out.println("item = " + item);
            // System.out.println("type = " + type);
            if (type.equalsIgnoreCase(TYPE_BOOLEAN)) {
                return isBoolean((String)item);
            }
            else if (type.equalsIgnoreCase(TYPE_NUMBER)) {
                try {
                    int i = Integer.parseInt((String)item);
                    return true;
                }
                catch (NumberFormatException nfe) {
                    return false;
                }
            }
            else {
                return (item instanceof String);
            }
        }

        return true;
    }

    protected boolean isBoolean(String str) {
        return (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false"));
    }
}
