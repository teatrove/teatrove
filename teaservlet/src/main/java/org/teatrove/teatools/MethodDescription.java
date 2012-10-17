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

import org.teatrove.trove.classfile.Modifiers;

import java.beans.*;
import java.lang.reflect.*;

/**
 * Wrapper for a MethodDescriptor object.
 *
 * @author Mark Masse
 */
public class MethodDescription extends FeatureDescription {

    private MethodDescriptor mMethodDescriptor;

    private TypeDescription mReturnType;
    private ParameterDescription[] mParams;

    /**
     * Creates a new MethodDescription
     */
    public MethodDescription(MethodDescriptor md, TeaToolsUtils utils) {
        super(utils);
        mMethodDescriptor = md;
    }

    /**
     * Returns the MethodDescriptor
     */
    public MethodDescriptor getMethodDescriptor() {
        return mMethodDescriptor;
    }

    /**
     * Returns the Method
     */
    public Method getMethod() {
        return getMethodDescriptor().getMethod();
    }

    /**
     * Returns a Modifiers instance that can be used to check the type's
     * modifiers.
     */
    public Modifiers getModifiers() {
        return getTeaToolsUtils().getModifiers(getMethod().getModifiers());
    }
    
    /**
     * Returns whether the given method or any of its super method or interface
     * declarations, recursively, are deprecated.
     * 
     * @return <code>true</code> if the method is deprecated
     * 
     * @see Deprecated
     */
    public boolean isDeprecated() {
        return getTeaToolsUtils().isDeprecated(getMethod());
    }
    
    /**
     * Returns the method's return type
     */
    public TypeDescription getReturnType() {
        if (mReturnType == null) {
            mReturnType = 
                getTeaToolsUtils().createTypeDescription(
                                          getMethod().getReturnType());
        }

        return mReturnType;
    }

    /**
     * Returns the method's parameters
     */
    public ParameterDescription[] getParameters() {
        if (mParams == null) {
            
            mParams = getTeaToolsUtils().createParameterDescriptions(
                                                        getMethodDescriptor());
        }

        return mParams;
    }

    /**
     * Returns true if the specified method accepts a 
     * <code>Substitution</code> as its last parameter.
     */
    public boolean getAcceptsSubstitution() {
        return getTeaToolsUtils().acceptsSubstitution(getMethodDescriptor());
    }

    //
    // FeatureDescription methods
    //

    public FeatureDescriptor getFeatureDescriptor() {
        return getMethodDescriptor();
    }

    public String getShortFormat() {

        StringBuffer format = new StringBuffer();
        format.append(getName());
        format.append('(');
        ParameterDescription[] params = getParameters();
        for (int i = 0; i < params.length; i++) {
            format.append(params[i].getShortFormat());
            if (i < (params.length - 1)) {
                format.append(", ");
            } 
        }

        format.append(')');

        return format.toString();
    }

    public String getLongFormat() {
        StringBuffer format = new StringBuffer();

        format.append(getReturnType().getLongFormat());
        format.append(getName());
        format.append('(');
        ParameterDescription[] params = getParameters();
        for (int i = 0; i < params.length; i++) {
            format.append(params[i].getLongFormat());
            if (i < (params.length - 1)) {
                format.append(", ");
            } 
        }

        format.append(')');

        return format.toString();
    }

    

}


