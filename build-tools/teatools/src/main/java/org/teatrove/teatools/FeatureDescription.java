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

//import org.teatrove.trove.classfile.Modifiers;

import java.beans.*;
//import java.lang.reflect.*;

/**
 * 
 * @author Mark Masse
 */
public abstract class FeatureDescription {

    private TeaToolsUtils mUtils;

    public FeatureDescription(TeaToolsUtils utils) {
        mUtils = utils;
    }

    public TeaToolsUtils getTeaToolsUtils() {
        return mUtils;
    }

    /**
     * Returns the feature name
     */
    public String getName() {
        FeatureDescriptor fd = getFeatureDescriptor();
        if (fd == null) {
            return null;
        }

        return fd.getName();
    }

    /**
     * Returns the shortDescription or "" if the
     * shortDescription is the same as the displayName.
     */
    public String getDescription() {        
        FeatureDescriptor fd = getFeatureDescriptor();
        if (fd == null) {
            return "";
        }

        return getTeaToolsUtils().getDescription(fd);
    }

    /**
     * Returns the first sentence of the 
     * shortDescription.  Returns "" if the shortDescription is the same as
     * the displayName (the default for reflection-generated 
     * FeatureDescriptors).  
     */
    public String getDescriptionFirstSentence() {
        FeatureDescriptor fd = getFeatureDescriptor();
        if (fd == null) {
            return "";
        }

        return getTeaToolsUtils().getDescriptionFirstSentence(fd);
    }   


    public String toString() {
        return getLongFormat();
    }


    /**
     * Returns the FeatureDescriptor that is wrapped by this 
     * FeatureDescription
     */
    public abstract FeatureDescriptor getFeatureDescriptor();

    /**
     * Returns a short format String for this FeatureDescription
     */
    public abstract String getShortFormat();

    /**
     * Returns a long format String for this FeatureDescription
     */
    public abstract String getLongFormat();


}
