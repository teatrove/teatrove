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

import java.beans.*;
import java.lang.reflect.Method;

/**
 * Wrapper for a PropertyDescriptor object.
 * 
 * @author Mark Masse
 */
public class PropertyDescription extends FeatureDescription {

    private TypeDescription mType;
    private PropertyDescriptor mPropertyDescriptor;

    /**
     * Creates a new PropertyDescription
     */
    public PropertyDescription(PropertyDescriptor pd,
                               TeaToolsUtils utils) {

        super(utils);

        mPropertyDescriptor = pd;
    }

    /**
     * Returns the property's type
     */
    public TypeDescription getType() {
        if (mType == null) {
            mType = getTeaToolsUtils().createTypeDescription(
                       getPropertyDescriptor().getPropertyType());
        }

        return mType;
    }

    /**
     * Returns the PropertyDescriptor
     */
    public PropertyDescriptor getPropertyDescriptor() {
        return mPropertyDescriptor;
    }
    
    /**
     * Returns whether the given property is deprecated based on whether any
     * of its read methods are deprecated.
     * 
     * @return <code>true</code> if the property is deprecated
     * 
     * @see Deprecated
     */
    public boolean isDeprecated() {
        Method method = getPropertyDescriptor().getReadMethod();
        return (method == null ? false : getTeaToolsUtils().isDeprecated(method));
    }

    //
    // FeatureDescription methods
    //

    public FeatureDescriptor getFeatureDescriptor() {
        return getPropertyDescriptor();
    }

    public String getShortFormat() {
        return getName();
    }

    public String getLongFormat() {
        return getType().getLongFormat() + " " + getName();
    }


    

}
