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
public class ParamTag extends Tag {

    private com.sun.javadoc.ParamTag mTag;

    public static ParamTag[] convert(RootDoc root,
                                     com.sun.javadoc.ParamTag[] tags) {
        int length = tags.length;
        ParamTag[] newTags = new ParamTag[length];
        for (int i=0; i<length; i++) {
            newTags[i] = new ParamTag(root, tags[i]);
        }
        return newTags;
    }

    public ParamTag(RootDoc root, com.sun.javadoc.ParamTag tag) {
        super(root, tag);
        mTag = tag;
    }

    public String getParameterName() {
        return mTag.parameterName();
    }

    public String getParameterComment() {
        return mTag.parameterComment();
    }
}
