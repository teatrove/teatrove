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

import java.util.HashMap;
import java.util.Iterator;

import com.sun.javadoc.SourcePosition;

/**
 *
 * @author Brian S O'Neill
 */
public class RootDoc extends Doc implements com.sun.javadoc.DocErrorReporter {

    private com.sun.javadoc.RootDoc mDoc;
    private PackageDoc[] mPackages;

    public RootDoc(com.sun.javadoc.RootDoc doc) {
        super(null, doc);
        mRootDoc = this;
        mDoc = doc;
    }

    public String[][] getOptions() {
        return mDoc.options();
    }

    public PackageDoc[] getSpecifiedPackages() {
        return PackageDoc.convert(mRootDoc, mDoc.specifiedPackages());
    }

    public ClassDoc[] getSpecifiedClasses() {
        return ClassDoc.convert(mRootDoc, mDoc.specifiedClasses());
    }

    public ClassDoc[] getClasses() {
        return ClassDoc.convert(mRootDoc, mDoc.classes());
    }

    public PackageDoc[] getPackages() {
        if (mPackages == null) {
            ClassDoc[] classes = getClasses();
            HashMap map = new HashMap();

            for (int i=0; i<classes.length; i++) {
                PackageDoc doc = classes[i].getContainingPackage();
                map.put(doc.getName(), doc);
            }

            for (int i=0; i<classes.length; i++) {
                PackageDoc doc = classes[i].getContainingPackage();

                String[] path = doc.getPath();
                StringBuffer buf = new StringBuffer();
                String name = buf.toString();
                if (map.get(name) == null) {
                    map.put(name, new EmptyPackageDoc(mRootDoc, name));
                }

                for (int j=0; j<path.length; j++) {
                    if (j > 0) {
                        buf.append('.');
                    }
                    buf.append(path[j]);
                    name = buf.toString();
                    if (map.get(name) == null) {
                        map.put(name, new EmptyPackageDoc(mRootDoc, name));
                    }
                }
            }

            mPackages = new PackageDoc[map.size()];
            Iterator it = map.keySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                mPackages[i++] = (PackageDoc)map.get(it.next());
            }
        }

        return mPackages;
    }

    public void printError(String msg) {
        mDoc.printError(msg);
    }

    public void printWarning(String msg) {
        mDoc.printWarning(msg);
    }

    public void printNotice(String msg) {
        mDoc.printNotice(msg);
    }

    public void printError(SourcePosition position, String message) {
        printError(message);
    }

    public void printNotice(SourcePosition position, String message) {
        printNotice(message);
    }

    public void printWarning(SourcePosition position, String message) {
        printWarning(message);
    }
}
