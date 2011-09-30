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
public class Type implements java.io.Serializable {

    protected RootDoc mRootDoc;

    private com.sun.javadoc.Type mType;

    public Type(RootDoc root, com.sun.javadoc.Type type) {
        mRootDoc = root;
        mType = type;
    }

    public RootDoc getRootDoc() {
        return mRootDoc;
    }

    public String getTypeName() {
        return mType.typeName();
    }

    public String getQualifiedTypeName() {
        return mType.qualifiedTypeName();
    }

    public String getDimension() {
        return mType.dimension();
    }

    public String toString() {
        return mType.toString();
    }

    public ClassDoc getAsClassDoc() {
        return new ClassDoc(mRootDoc, mType.asClassDoc());
    }
}
