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
public class SeeTag extends Tag {

    private com.sun.javadoc.SeeTag mTag;

    public static SeeTag[] convert(RootDoc root,
                                   com.sun.javadoc.SeeTag[] tags) {
        int length = tags.length;
        SeeTag[] newTags = new SeeTag[length];
        for (int i=0; i<length; i++) {
            newTags[i] = new SeeTag(root, tags[i]);
        }
        return newTags;
    }

    public SeeTag(RootDoc root, com.sun.javadoc.SeeTag tag) {
        super(root, tag);
        mTag = tag;
    }

    public String getLabel() {
        return mTag.label();
    }

    public String getReferencedClassName() {
        return mTag.referencedClassName();
    }

    public ClassDoc getReferencedClass() {
        return new ClassDoc(mRootDoc, mTag.referencedClass());
    }

    public String getReferencedMemberName() {
        return mTag.referencedMemberName();
    }

    public MemberDoc getReferencedMember() {
        return new MemberDoc(mRootDoc, mTag.referencedMember());
    }
}
