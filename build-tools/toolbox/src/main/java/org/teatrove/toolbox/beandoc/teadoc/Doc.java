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

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *
 * @author Brian S O'Neill
 */
public class Doc implements java.io.Serializable, Comparable {

    protected RootDoc mRootDoc;

    private com.sun.javadoc.Doc mDoc;
    private String[] mPath;

    public Doc(RootDoc root, com.sun.javadoc.Doc doc) {
        mRootDoc = root;
        mDoc = doc;
    }

    protected String[] parseName(String name) {
        ArrayList list = new ArrayList();
        StringTokenizer st = new StringTokenizer(name, ".");
        while (st.hasMoreElements()) {
            list.add(st.nextElement());
        }

        return (String[])list.toArray(new String[list.size()]);
    }

    public com.sun.javadoc.Doc getInnerDoc() {
        return mDoc;
    }

    public RootDoc getRootDoc() {
        return mRootDoc;
    }

    public String getCommentText() {

        String commentText = mDoc.commentText();
        if (commentText != null && commentText.trim().length() > 0) {
            return commentText;
        }

        return null;
    }

    /**
     * Checks to see if the specified tag exists
     */
    public boolean isTagPresent(String tagName) {

        Tag[] tags = getTagMap().get(tagName);
        return (tags != null && tags.length > 0);
    }


    /**
     * Gets the text value of the first tag in doc that matches tagName
     */
    public String getTagValue(String tagName) {

        Tag[] tags = getTagMap().get(tagName);
        if (tags == null || tags.length == 0) {
            return null;
        }

        return tags[tags.length - 1].getText();
    }

    public Tag[] getTags() {
        return Tag.convert(mRootDoc, mDoc.tags());
    }

    public TagMap getTagMap() {
        return new TagMap();
    }

    public SeeTag[] getSeeTags() {
        return SeeTag.convert(mRootDoc, mDoc.seeTags());
    }

    public Tag[] getInlineTags() {
        return Tag.convert(mRootDoc, mDoc.inlineTags());
    }

    public Tag[] getFirstSentenceTags() {
        return Tag.convert(mRootDoc, mDoc.firstSentenceTags());
    }

    public String getRawCommentText() {
        return mDoc.getRawCommentText();
    }

    public String getName() {
        return mDoc.name();
    }

    public String[] getPath() {
        if (mPath == null) {
            mPath = parseName(getName());
        }

        return mPath;
    }

    public int compareTo(Object obj) {
        if (obj instanceof Doc) {
            return mDoc.compareTo(((Doc)obj).mDoc);
        }
        else {
            return 0;
        }
    }

    public boolean isField() {
        return mDoc.isField();
    }

    public boolean isMethod() {
        return mDoc.isMethod();
    }

    public boolean isConstructor() {
        return mDoc.isConstructor();
    }

    public boolean isInterface() {
        return mDoc.isInterface();
    }

    public boolean isException() {
        return mDoc.isException();
    }

    public boolean isError() {
        return mDoc.isError();
    }

    public boolean isOrdinaryClass() {
        return mDoc.isOrdinaryClass();
    }

    public boolean isClass() {
        return mDoc.isClass();
    }

    public boolean isIncluded() {
        return mDoc.isIncluded();
    }

    public boolean equals(Object obj) {
        return obj instanceof Doc && mDoc.equals(((Doc)obj).mDoc);
    }

    public class TagMap {

        public TagMap() {
        }

        public Tag[] get(String tagName) {
            return Tag.convert(mRootDoc, getInnerDoc().tags(tagName));
        }

    }

}
