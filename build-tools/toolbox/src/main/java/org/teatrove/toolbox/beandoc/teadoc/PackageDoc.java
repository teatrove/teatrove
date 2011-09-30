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

import java.util.Arrays;
import java.util.ArrayList;

/**
 *
 * @author Brian S O'Neill
 */
public class PackageDoc extends Doc {

    private com.sun.javadoc.PackageDoc mDoc;
    private PackageDoc[] mPackages;

    public static PackageDoc[] convert(RootDoc root,
                                       com.sun.javadoc.PackageDoc[] docs) {
        int length = docs.length;
        PackageDoc[] newDocs = new PackageDoc[length];
        for (int i=0; i<length; i++) {
            newDocs[i] = new PackageDoc(root, docs[i]);
        }
        return newDocs;
    }

    public PackageDoc(RootDoc root, com.sun.javadoc.PackageDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public ClassDoc[] getAllClasses() {
        return ClassDoc.convert(mRootDoc, mDoc.allClasses());
    }

    public ClassDoc[] getSortedAllClasses() {
        ClassDoc[] docs = (ClassDoc[])getAllClasses().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getOrdinaryClasses() {
        return ClassDoc.convert(mRootDoc, mDoc.ordinaryClasses());
    }

    public ClassDoc[] getSortedOrdinaryClasses() {
        ClassDoc[] docs = (ClassDoc[])getOrdinaryClasses().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getExceptions() {
        return ClassDoc.convert(mRootDoc, mDoc.exceptions());
    }

    public ClassDoc[] getSortedExceptions() {
        ClassDoc[] docs = (ClassDoc[])getExceptions().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getErrors() {
        return ClassDoc.convert(mRootDoc, mDoc.errors());
    }

    public ClassDoc[] getSortedErrors() {
        ClassDoc[] docs = (ClassDoc[])getErrors().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getInterfaces() {
        return ClassDoc.convert(mRootDoc, mDoc.interfaces());
    }

    public ClassDoc[] getSortedInterfaces() {
        ClassDoc[] docs = (ClassDoc[])getInterfaces().clone();
        Arrays.sort(docs);
        return docs;
    }

    public PackageDoc[] getPackages() {
        if (mPackages == null) {
            ArrayList list = new ArrayList();
            PackageDoc[] allPackages = mRootDoc.getPackages();
            String thisName = getName();

            for (int i=0; i<allPackages.length; i++) {
                PackageDoc doc = allPackages[i];
                String name = doc.getName();
                if (name.startsWith(thisName) && doc != this) {
                    name = name.substring(thisName.length());
                    if (name.lastIndexOf('.') <= 0) {
                        list.add(doc);
                    }
                }
            }

            mPackages =
                (PackageDoc[])list.toArray(new PackageDoc[list.size()]);
        }

        return mPackages;
    }

    public PackageDoc[] getSortedPackages() {
        PackageDoc[] docs = (PackageDoc[])getPackages().clone();
        Arrays.sort(docs);
        return docs;
    }

    public int hashCode() {
        if (mDoc != null) {
            return getName().hashCode();
        }
        else {
            return super.hashCode();
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof PackageDoc) {
            PackageDoc doc = (PackageDoc)other;
            return getName().equals(doc.getName());
        }

        return false;
    }

    public int compareTo(Object obj) {
        if (obj instanceof PackageDoc) {
            return getName().compareTo(((PackageDoc)obj).getName());
        }
        else {
            return 0;
        }
    }
}
