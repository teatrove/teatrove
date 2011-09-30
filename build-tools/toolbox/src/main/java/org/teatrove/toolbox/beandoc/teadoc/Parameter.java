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

package org.teatrove.toolbox.beandoc.teadoc;

/**
 *
 * @author Brian S O'Neill
 */
public class Parameter implements java.io.Serializable {

    protected RootDoc mRootDoc;

    private com.sun.javadoc.Parameter mParam;

    public static Parameter[] convert(RootDoc root,
                                      com.sun.javadoc.Parameter[] params) {
        int length = params.length;
        Parameter[] newParams = new Parameter[length];
        for (int i=0; i<length; i++) {
            newParams[i] = new Parameter(root, params[i]);
        }
        return newParams;
    }

    public Parameter(RootDoc root, com.sun.javadoc.Parameter param) {
        mRootDoc = root;
        mParam = param;
    }

    public RootDoc getRootDoc() {
        return mRootDoc;
    }

    public Type getType() {
        return new Type(mRootDoc, mParam.type());
    }

    public String getName() {
        return mParam.name();
    }

    public String getTypeName() {
        return mParam.typeName();
    }

    public String toString() {
        return mParam.toString();
    }
}
