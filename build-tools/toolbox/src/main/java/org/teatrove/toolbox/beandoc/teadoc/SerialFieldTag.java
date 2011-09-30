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
public class SerialFieldTag extends Tag implements Comparable {

    private com.sun.javadoc.SerialFieldTag mTag;

    public static SerialFieldTag[] convert(RootDoc root,
                                           com.sun.javadoc.SerialFieldTag[]
                                           tags) {
        int length = tags.length;
        SerialFieldTag[] newTags = new SerialFieldTag[length];
        for (int i=0; i<length; i++) {
            newTags[i] = new SerialFieldTag(root, tags[i]);
        }
        return newTags;
    }

    public SerialFieldTag(RootDoc root, com.sun.javadoc.SerialFieldTag tag) {
        super(root, tag);
        mTag = tag;
    }

    public String getFieldName() {
        return mTag.fieldName();
    }

    public String getFieldType() {
        return mTag.fieldType();
    }

    public ClassDoc getFieldTypeDoc() {
        return new ClassDoc(mRootDoc, mTag.fieldTypeDoc());
    }

    public String getDescription() {
        return mTag.description();
    }

    public int compareTo(Object obj) {
        return mTag.compareTo(obj);
    }
}
