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
public class ExecutableMemberDoc extends MemberDoc {

    private com.sun.javadoc.ExecutableMemberDoc mDoc;

    public ExecutableMemberDoc(RootDoc root,
                               com.sun.javadoc.ExecutableMemberDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public ClassDoc[] getThrownExceptions() {
        return ClassDoc.convert(mRootDoc, mDoc.thrownExceptions());
    }

    public boolean isNative() {
        return mDoc.isNative();
    }

    public boolean isSynchronized() {
        return mDoc.isSynchronized();
    }

    public Parameter[] getParameters() {
        return Parameter.convert(mRootDoc, mDoc.parameters());
    }

    public ThrowsTag[] getThrowsTags() {
        return ThrowsTag.convert(mRootDoc, mDoc.throwsTags());
    }

    public ParamTag[] getParamTags() {
        return ParamTag.convert(mRootDoc, mDoc.paramTags());
    }

    public String getSignature() {
        return mDoc.signature();
    }

    public String getFlatSignature() {
        return mDoc.flatSignature();
    }
}
