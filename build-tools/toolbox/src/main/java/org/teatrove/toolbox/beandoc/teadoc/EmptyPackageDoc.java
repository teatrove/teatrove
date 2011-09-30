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
public class EmptyPackageDoc extends PackageDoc {

    private String mName;

    public EmptyPackageDoc(RootDoc root, String name) {
        super(root, null);
        mName = name;
    }

    public String getCommentText() {
        return "";
    }

    public Tag[] getTags() {
        return new Tag[0];
    }

    public SeeTag[] getSeeTags() {
        return new SeeTag[0];
    }

    public Tag[] getInlineTags() {
        return new Tag[0];
    }

    public Tag[] getFirstSentanceTags() {
        return new Tag[0];
    }

    public String getRawCommentText() {
        return "";
    }

    public String getName() {
        return mName;
    }

    public int compareTo(Object obj) {
        if (obj instanceof PackageDoc) {
            return mName.compareTo(((PackageDoc)obj).getName());
        }
        else {
            return 0;
        }
    }

    public boolean isField() {
        return false;
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isConstructor() {
        return false;
    }

    public boolean isInterface() {
        return false;
    }

    public boolean isException() {
        return false;
    }

    public boolean isError() {
        return false;
    }

    public boolean isOrdinaryClass() {
        return false;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isIncluded() {
        return true;
    }

    public ClassDoc[] getAllClasses() {
        return new ClassDoc[0];
    }

    public ClassDoc[] getOrdinaryClasses() {
        return new ClassDoc[0];
    }

    public ClassDoc[] getExceptions() {
        return new ClassDoc[0];
    }

    public ClassDoc[] getErrors() {
        return new ClassDoc[0];
    }

    public ClassDoc[] getInterfaces() {
        return new ClassDoc[0];
    }
}
