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

package org.teatrove.teatools;

/**
 * Data structure that contains a Context's class name and prefix name.
 * This class can be used in conjuntion with the 
 * <code>TeaToolsUtils.createContextClass</code> method.
 * 
 * @author Mark Masse
 */
public class ContextClassEntry {
    
    private String mContextClassName;
    private String mPrefixName;
    
    /**
     * Creates a new ContextClassEntry
     */
    public ContextClassEntry(String contextClassName) {
        setContextClassName(contextClassName);
    }

    /**
     * Sets the class name of this ContextClassEntry
     */
    public void setContextClassName(String contextClassName) {
        mContextClassName = contextClassName;
    }
    
    /**
     * Gets the class name of this ContextClassEntry
     */        
    public String getContextClassName() {
        return mContextClassName;
    }
    

    /**
     * Sets the prefix name of this ContextClassEntry
     */
    public void setPrefixName(String prefixName) {
        mPrefixName = prefixName;
    }
    
    /**
     * Gets the prefix name of this ContextClassEntry
     */
    public String getPrefixName() {
        return mPrefixName;
    }
        
    /**
     * Returns the class name of this ContextClassEntry
     */
    public String toString() {
        return getContextClassName();
    }
}
