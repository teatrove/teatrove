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
public class ProgramElementDoc extends Doc {

    private com.sun.javadoc.ProgramElementDoc mDoc;

    public ProgramElementDoc(RootDoc root,
                             com.sun.javadoc.ProgramElementDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public ClassDoc getContainingClass() {
        if (mDoc.containingClass() != null) {
            return new ClassDoc(mRootDoc, mDoc.containingClass());
        }
        else {
            return null;
        }
    }

    public String getQualifiedName() {
        return mDoc.qualifiedName();
    }

    public int getModifierSpecifier() {
        return mDoc.modifierSpecifier();
    }

    public String getModifiers() {
        return mDoc.modifiers();
    }

    public boolean isPublic() {
        return mDoc.isPublic();
    }

    public boolean isProtected() {
        return mDoc.isProtected();
    }

    public boolean isPrivate() {
        return mDoc.isPrivate();
    }

    public boolean isPackagePrivate() {
        return mDoc.isPackagePrivate();
    }

    public boolean isStatic() {
        return mDoc.isStatic();
    }

    public boolean isFinal() {
        return mDoc.isFinal();
    }
}
