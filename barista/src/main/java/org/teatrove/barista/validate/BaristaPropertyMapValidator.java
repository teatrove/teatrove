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

import org.teatrove.barista.validate.event.*;
import org.teatrove.trove.io.SourceInfo;
import org.teatrove.trove.util.PropertyMap;
import java.util.*;

/**
 * Validates a property file using a rules property file.
 *
 * @author Sean T. Treat
 */
public class BaristaPropertyMapValidator implements Validator {

    private final static String ROOT_KEY = "httpServer";

    private StringStack mKeyStack;

    private PropertyMap mMap;
    private PropertyMap mRules;
    private List mListeners;
    private Map mLineNumberMap;

    /** list that indicates whether to search for a custom rules file. */
    private List mLoadedList;

    /** list of items/sections that can legally appear anywhere in the file */
    private List mOptionalList;

    /** map of subItem names to Lists of the indivually names items */
    private Map mSubItemMap = new HashMap(16);  

    public BaristaPropertyMapValidator(PropertyMap map, PropertyMap rules, 
                                Map lineNumbers) {
        mListeners =  new ArrayList();
        mMap = map;
        mRules = rules;
        mLineNumberMap = lineNumbers;
        mLoadedList = new ArrayList(5);
        mOptionalList = RulesEvaluator.getOptionalItemList(mRules);
    }

    public BaristaPropertyMapValidator(PropertyMap map, PropertyMap rules) {
        this(map, rules, new HashMap());
    }

    /**
     * Sets a map that will be used for mapping key names to line numbers.
     */
    public void setLineNumbers(Map lineNumbers) {
        mLineNumberMap = lineNumbers;
    }

    /**
     * Validator interface method
     */
    public void addValidationListener(ValidationListener listener) {
        mListeners.add(listener);
    }

    /**
     * Validator interface method
     */
    public void removeValidationListener(ValidationListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);      
        }
    }

    /**
     * Validates the candidate PropertyMap against the rules specified in
     * the constructor.
     */
    public void validate() {
        if (mMap != null && mRules != null) {
            mKeyStack = new StringStack(ROOT_KEY);

            validate(mMap, (PropertyMap)mRules);
            mKeyStack = new StringStack(ROOT_KEY);
        }
    }

    private void dispatchAdminApplicationWarning(String className) {
        if (!mMap.containsValue(className)) {
            AdminPageWarning w = 
                new AdminPageWarning(this, getSourceInfo(mKeyStack.current()),
                                     className + " is not properly configured"
                                     + " in this property file");
            dispatchWarn(w);                
        }
    }

    private void validate(PropertyMap map, PropertyMap rules) {

        if(rules == null || map == null) {
            return;
        }

        PropertyMap customMap = null;
        PropertyMap unchangedRules = new PropertyMap();
        unchangedRules.putAll(rules);
        RulesEvaluator rEval = new RulesEvaluator(rules);
        Set rulesSet = rules.subKeySet();
        Iterator rulesIterator = rulesSet.iterator();
        Set mapSet = map.subKeySet();

        //
        // Recognize and flag any extra settings in the file.
        //
        if (rEval.isStrict()) {
            Iterator mi = mapSet.iterator();
            while (mi.hasNext()) {
                String next = (String)mi.next();
                if (!rulesSet.contains(next)) {
                    // if item is optional, insert a copy of its rules setings.
                    if (mOptionalList.contains(next)) {
                        // place this subMap into the rules file
                        rules.put(next, "dummy");
                        PropertyMap sm = mRules.subMap(next);
                        rules.subMap(next).putAll(sm);                  
                        rulesSet = rules.subKeySet();
                        rulesIterator = rulesSet.iterator();
                        continue;
                    }
                    mKeyStack.push(next);
                    String curKey = mKeyStack.current();
                    dispatchError(new InvalidPropertyError(this, 
                                  (SourceInfo)mLineNumberMap.get(curKey), 
                                  next + " is not allowed in section: " + 
                                  curKey));
                    mKeyStack.pop();
                }
            }
        }

        //
        // The first time through this loop, we don't want to pop the current
        // key because we are at the root.
        //
        boolean first = true;

        while (rulesIterator.hasNext()) {
            if (first) {
                first = false;
            } else {
                mKeyStack.pop();
            }

            // get the next subKey
            String subKey = (String)rulesIterator.next();
            mKeyStack.push(subKey);
            
            if (subKey.startsWith(RulesEvaluator.KEY_PREFIX)) { 
                continue;
            }

            String subItem = rEval.getSubItemName(subKey);

            //
            // Identify items that are designated as "<mappingItem>"s, and 
            // check them against the specified field.
            //
            String mappingItem = rEval.getMappingItem(subKey);          
            if (mappingItem != null) {
                // call method that will check all subitems against a
                // list of mappable items.
                checkMappingsSection(map.subMap(subKey), mappingItem);
                continue;
            }

            // check for required items
            if (rEval.isRequired(subKey) && !mapSet.contains(subKey)) {
                String curKey = mKeyStack.current();
                dispatchError(new MissingPropertyError(this, 
                                                       getSourceInfo(curKey), 
                                                       curKey +
                                                       " is not present"));
                continue;
            }
            else if (mapSet.contains(subKey) && subItem == null) {
                String type = rEval.getType(subKey);
                // check type here
                if (!rEval.checkType(type, map.get(subKey))) {
                    String curKey = mKeyStack.current();
                    dispatchError(new IncorrectTypeError(this, 
                                  (SourceInfo)getSourceInfo(curKey), 
                                  curKey + " is the wrong type"));
                }

                checkBounds(type, subKey, map.get(subKey), rEval);

                //
                // Try to load custom rules files here. 
                //
                String rulesField = rEval.getCustomRulesFieldName();
                if (rulesField != null) {
                    String rField = (String)map.get(rulesField);
                    if (rField != null && rField.length() != 0) {
                        // convert to a valid file path
                        String n = rField.replace('.', '/');
                        if (!mLoadedList.contains(n)) {
                            customMap = PropertyMapUtils.
                                loadPropertyMapFromURL(RulesEvaluator.
                                                       getRulesURLForClass(n));
                            if (customMap != null) {
                                mLoadedList.add(n);
                                rules.putAll(customMap);
                            }
                        }
                    }
                }

                validate(map.subMap(subKey), rules.subMap(subKey));
            }
            else if (subItem != null) {
                PropertyMap subItemRuleMap = rules.subMap(subKey).
                    subMap(subItem);
                PropertyMap subItemMap = map.subMap(subKey);

                List subItems = (List)mSubItemMap.get(subItem);
                if (subItems == null) {
                    subItems = new LinkedList();
                }
                subItems = checkSpecialSection(subItemMap, subItemRuleMap, 
                                               subItems);
                mSubItemMap.put(subItem, subItems);
            }
        }

        //
        // Remove the custom map if it was loaded.
        //
        if (customMap != null) {
            String removed = (String)mLoadedList.remove(mLoadedList.size()-1);
            Iterator it = customMap.subKeySet().iterator();
            while (it.hasNext()) {
                rules.subMap((String)it.next()).clear();
            }
            rules.putAll(unchangedRules);
        }
        
        mKeyStack.pop();
    }        

    /**
     * Checks that all the keys in the mapping section are mapped to valid
     * mapping items. For example:
     * filterMap {
     *     * = "Error Forwarding"
     *     /filteredItems/ = "My Filter"
     * }
     *
     * In both cases above, the values "Error Forwarding", and "My Filter" must
     * be filters that are defined elsewher in the Barista Property File.
     */
    private void checkMappingsSection(PropertyMap map, String mappingItem) {
        Set keys = map.subKeySet();
        Iterator i = keys.iterator();

        List mappingItems = (List)mSubItemMap.get(mappingItem);
        if (mappingItems == null) {
            String msg = "No sub items loaded for: " + mappingItem + 
                ". Check the rules files";
            dispatchError(new ValidationError(this, 
                                              getSourceInfo(mKeyStack.
                                                            current())
                                              , msg));
            return;
        }
        
        String value, key;
        while (i.hasNext()) {
            key = (String)i.next();
            mKeyStack.push(key);
            value = map.getString(key);
            // The filtermap has the special ability to contain
            // multiple assignments to the same path via a comma
            // delimited list. This hack addresses that.
            if (mappingItem.equalsIgnoreCase("filter")) {
                StringTokenizer tok = new StringTokenizer(value, ",");
                while (tok.hasMoreTokens()) {
                    String next = ((String)tok.nextToken()).trim();
                    if (!mappingItems.contains(next)) {
                        String msg = "'" + next + "' is not a valid " + 
                            mappingItem + " mapping for " + key;
                        dispatchError(new InvalidPropertyError(this, 
                            getSourceInfo(mKeyStack.current()), msg));
                    }
                }
            }
            else {
                if (!mappingItems.contains(value)) {
                    String msg = value + " is not a valid " + 
                        mappingItem + " mapping for " + key;
                    dispatchError(new InvalidPropertyError(this,
                        getSourceInfo(mKeyStack.current()), msg));
                }
            }
            mKeyStack.pop();
        }
    }

    /**
     * All custom loaded sections wille be validated through this method.
     */
    private List checkSpecialSection(PropertyMap map, PropertyMap rules, 
                                     List subItems) {
        Iterator it = map.subKeySet().iterator();
        while(it.hasNext()) {                   
            String next = (String)it.next();            
            subItems.add(next);
            mKeyStack.push(next);
            validate(map.subMap(next), rules); 
            mKeyStack.pop();
        }

        return subItems;
    }

    /**
     * Checks that <b>item</b> is in the valid range defined by 
     * <max> and <min>, or a valid String choice.
     */
    private void checkBounds(String type, String subKey, Object item, 
                             RulesEvaluator eval) {
        // don't check boolean types
        if (type.compareTo(RulesEvaluator.TYPE_BOOLEAN) == 0) {
            return;
        }

        if (type.compareTo(RulesEvaluator.TYPE_NUMBER) == 0) {
            int min = eval.getMin(subKey);
            int max = eval.getMax(subKey);           
            int num = Integer.parseInt((String)item);

            if (num <= min || num >= max) {
                String deflt = eval.getDefault(subKey);
                int def = 0;
                if (deflt != null && deflt.length() > 0) {
                    def = Integer.parseInt(deflt);                   
                }
                String curKey = mKeyStack.current();
                dispatchError(new OutOfRangeError(this, 
                                                  getSourceInfo(curKey), 
                                                  curKey + " is out of bounds."
                                                  + " Value must be between " 
                                                  + min + " and " + max + "." +
                                                  ((deflt!= null) ? 
                                                   (" The Default value is " + 
                                                    def) : "")));
            }
        }
        else if (type.equals(RulesEvaluator.TYPE_STRING)) {
            String[] choices = eval.getChoices(subKey);
            if (choices != null) {
                boolean isValid = false;
                String c = new String();

                for (int i=0; i<choices.length; i++) {
                    if (choices[i].equals((String)item)) {
                        isValid = true;
                    }
                    c += choices[i];
                    if ((i+1) < choices.length) {
                        c += ", ";
                    }                       
                }
                
                if (!isValid) {
                    String curKey = mKeyStack.current();
                    String msg = curKey + 
                        " must be one of the following values: " + c;
                    dispatchError(new OutOfRangeError(this, 
                                  getSourceInfo(curKey), 
                                  msg));
                }
            }
        }
    }
    
    /**
     * Retrieves the SourceInfo object (if exists) that matches the specified 
     * key.
     */
    private SourceInfo getSourceInfo(String key) {
        try {
            return (SourceInfo)mLineNumberMap.get(key);
        }
        catch (NullPointerException npe) {
            return null;
        }
    }

    private void dispatchError(ValidationEvent e) {
        Iterator i = mListeners.iterator();

        while (i.hasNext()) {
            ((ValidationListener)i.next()).error(e);
        }
    }

    private void dispatchWarn(ValidationEvent e) {
        Iterator i = mListeners.iterator();

        while (i.hasNext()) {
            ((ValidationListener)i.next()).warn(e);
        }
    }

    private void dispatchDebug(ValidationEvent e) {
        Iterator i = mListeners.iterator();

        while (i.hasNext()) {
            ((ValidationListener)i.next()).debug(e);
        }
    }

}
