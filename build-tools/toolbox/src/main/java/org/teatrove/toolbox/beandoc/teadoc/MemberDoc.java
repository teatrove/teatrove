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
public class MemberDoc extends ProgramElementDoc {

    private com.sun.javadoc.MemberDoc mDoc;

    public MemberDoc(RootDoc root, com.sun.javadoc.MemberDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public PackageDoc getContainingPackage() {
        return new PackageDoc(mRootDoc, mDoc.containingPackage());
    }

    public boolean isSynthetic() {
        return mDoc.isSynthetic();
    }

    public boolean isHidden() {
        return isTagPresent("hidden");
    }

    public boolean isExpert() {
        return isTagPresent("expert");
    }

    public boolean isPreferred() {
        return isTagPresent("preferred");
    }

    public boolean isExcluded() {
        return isTagPresent("exclude") || isTagPresent("excluded");
    }

}




