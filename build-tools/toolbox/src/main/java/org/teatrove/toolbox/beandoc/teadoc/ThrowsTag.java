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
public class ThrowsTag extends Tag {

    private com.sun.javadoc.ThrowsTag mTag;

    public static ThrowsTag[] convert(RootDoc root,
                                      com.sun.javadoc.ThrowsTag[] tags) {
        int length = tags.length;
        ThrowsTag[] newTags = new ThrowsTag[length];
        for (int i=0; i<length; i++) {
            newTags[i] = new ThrowsTag(root, tags[i]);
        }
        return newTags;
    }

    public ThrowsTag(RootDoc root, com.sun.javadoc.ThrowsTag tag) {
        super(root, tag);
        mTag = tag;
    }

    public String getExceptionName() {
        return mTag.exceptionName();
    }

    public String getExceptionComment() {
        return mTag.exceptionComment();
    }

    public ClassDoc getException() {
        return new ClassDoc(mRootDoc, mTag.exception());
    }
}
