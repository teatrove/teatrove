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
public class FieldDoc extends MemberDoc {

    private com.sun.javadoc.FieldDoc mDoc;

    public static FieldDoc[] convert(RootDoc root,
                                     com.sun.javadoc.FieldDoc[] docs) {
        int length = docs.length;
        FieldDoc[] newDocs = new FieldDoc[length];
        for (int i=0; i<length; i++) {
            newDocs[i] = new FieldDoc(root, docs[i]);
        }
        return newDocs;
    }

    public FieldDoc(RootDoc root, com.sun.javadoc.FieldDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public Type getType() {
        return new Type(mRootDoc, mDoc.type());
    }

    public boolean isTransient() {
        return mDoc.isTransient();
    }

    public boolean isVolatile() {
        return mDoc.isVolatile();
    }

    public SerialFieldTag[] getSerialFieldTags() {
        return SerialFieldTag.convert(mRootDoc, mDoc.serialFieldTags());
    }
}
