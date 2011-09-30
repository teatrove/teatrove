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

/**
 * 
 * @author Mark Masse
 */
public class ParameterDescription extends FeatureDescription {

    private TypeDescription mType;
    private ParameterDescriptor mParameterDescriptor;

    /**
     * Creates a new ParameterDescription
     */
    public ParameterDescription(TypeDescription type, 
                                ParameterDescriptor pd,
                                TeaToolsUtils utils) {

        super(utils);

        mType = type;
        mParameterDescriptor = pd;
    }

    /**
     * Returns the parameter's type
     */
    public TypeDescription getType() {
        return mType;
    }

    /**
     * Returns the ParameterDescriptor
     */
    public ParameterDescriptor getParameterDescriptor() {
        return mParameterDescriptor;
    }

    /**
     * Returns the formal param name, or null if the formal name is not
     * available.
     */
    public String getName() {
        String name = super.getName();
        if (name != null) {
            if (name.equals(getParameterDescriptor().getDisplayName()) ||
                name.length() == 0) {
                name = null;
            }
        }
        
        return name;
    }


    //
    // FeatureDescription methods
    //

    public FeatureDescriptor getFeatureDescriptor() {
        return getParameterDescriptor();
    }

    public String getShortFormat() {
        return getType().getShortFormat();
    }

    public String getLongFormat() {
        String name = getName();
        String longFormat = getType().getLongFormat();
        if (name != null) {
            longFormat = longFormat + " " + name;
        }

        return longFormat;
    }


    

}
