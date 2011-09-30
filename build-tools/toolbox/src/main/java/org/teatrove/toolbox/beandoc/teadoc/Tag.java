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
public class Tag implements java.io.Serializable {

    protected RootDoc mRootDoc;

    private com.sun.javadoc.Tag mTag;

    public static Tag[] convert(RootDoc root, com.sun.javadoc.Tag[] tags) {
        int length = tags.length;
        Tag[] newTags = new Tag[length];
        for (int i=0; i<length; i++) {
            newTags[i] = new Tag(root, tags[i]);
        }
        return newTags;
    }

    public Tag(RootDoc root, com.sun.javadoc.Tag tag) {
        mRootDoc = root;
        mTag = tag;
    }

    public RootDoc getRootDoc() {
        return mRootDoc;
    }

    public String getName() {
        return mTag.name();
    }

    public String getKind() {
        return mTag.kind();
    }

    public String getText() {
        return mTag.text();
    }

    public String toString() {
        return mTag.toString();
    }

    public Tag[] getInlineTags() {
        return convert(mRootDoc, mTag.inlineTags());
    }

    public Tag[] getFirstSentenceTags() {
        return convert(mRootDoc, mTag.firstSentenceTags());
    }
}
