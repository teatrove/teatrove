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
public class MethodDoc extends ExecutableMemberDoc {

    private com.sun.javadoc.MethodDoc mDoc;

    public static MethodDoc[] convert(RootDoc root,
                                      com.sun.javadoc.MethodDoc[] docs) {
        int length = docs.length;
        MethodDoc[] newDocs = new MethodDoc[length];
        for (int i=0; i<length; i++) {
            newDocs[i] = new MethodDoc(root, docs[i]);
        }
        return newDocs;
    }

    public MethodDoc(RootDoc root, com.sun.javadoc.MethodDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public boolean isAbstract() {
        return mDoc.isAbstract();
    }

    public Type getReturnType() {
        return new Type(mRootDoc, mDoc.returnType());
    }

    public ClassDoc getOverriddenClass() {
        return new ClassDoc(mRootDoc, mDoc.overriddenClass());
    }

}
