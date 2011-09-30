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

/**
 *
 * @author Brian S O'Neill
 */
public class ClassDoc extends ProgramElementDoc {

    private com.sun.javadoc.ClassDoc mDoc;
    private String[] mPath;

    public static ClassDoc[] convert(RootDoc root,
                                     com.sun.javadoc.ClassDoc[] docs) {
        int length = docs.length;
        ClassDoc[] newDocs = new ClassDoc[length];
        for (int i=0; i<length; i++) {
            newDocs[i] = new ClassDoc(root, docs[i]);
        }
        return newDocs;
    }

    /**
     * Splits a class name into two strings.
     * <br>
     * [0] = package name (or null if the class is unpackaged) <br>
     * [1] = class name
     */
    public static String[] parseClassName(String fullClassName) {

        int dotIndex = fullClassName.lastIndexOf(".");
        String packageName = null;
        String className = fullClassName;

        if (dotIndex > 0) {
            packageName = fullClassName.substring(0, dotIndex);
            className = fullClassName.substring(dotIndex + 1);
        }

        return new String[] { packageName, className };
    }




    public ClassDoc(RootDoc root, com.sun.javadoc.ClassDoc doc) {
        super(root, doc);
        mDoc = doc;
    }

    public boolean isAbstract() {
        return mDoc.isAbstract();
    }

    public boolean isSerializable() {
        return mDoc.isSerializable();
    }

    public boolean isExternalizable() {
        return mDoc.isExternalizable();
    }

    public MethodDoc[] getSerializationMethods() {
        return MethodDoc.convert(mRootDoc, mDoc.serializationMethods());
    }

    public FieldDoc[] getSerializableFields() {
        return FieldDoc.convert(mRootDoc, mDoc.serializableFields());
    }

    public boolean getDefinesSerializableFields() {
        return mDoc.definesSerializableFields();
    }

    public PackageDoc getContainingPackage() {
        return new PackageDoc(mRootDoc, mDoc.containingPackage());
    }

    public String[] getPath() {
        if (mPath == null) {
            ClassDoc parent = getContainingClass();
            if (parent != null) {
                String[] parentPath = parent.getPath();
                int length = parentPath.length;
                mPath = new String[length];
                System.arraycopy(parentPath, 0, mPath, 0, length);
                mPath[length - 1] = getName();
            }
            else {
                mPath = parseName(getQualifiedName());
            }
        }

        return mPath;
    }

    public ClassDoc getSuperclass() {
        com.sun.javadoc.ClassDoc superclass = mDoc.superclass();
        if (superclass == null) {
            return null;
        }

        return new ClassDoc(mRootDoc, superclass);
    }

    public ClassDoc[] getInterfaces() {
        return convert(mRootDoc, mDoc.interfaces());
    }

    public FieldDoc[] getFields() {
        return FieldDoc.convert(mRootDoc, mDoc.fields());
    }

    public FieldDoc[] getSortedFields() {
        FieldDoc[] docs = (FieldDoc[])getFields().clone();
        Arrays.sort(docs);
        return docs;
    }

    public MethodDoc[] getMethods() {
        return MethodDoc.convert(mRootDoc, mDoc.methods());
    }

    public MethodDoc[] getSortedMethods() {
        MethodDoc[] docs = (MethodDoc[])getMethods().clone();
        Arrays.sort(docs);
        return docs;
    }

    public PropertyDoc[] getProperties() {
        return PropertyDoc.create(mRootDoc, getMethods());
    }

    public PropertyDoc[] getSortedProperties() {
        return PropertyDoc.create(mRootDoc, getSortedMethods());
    }

    public ConstructorDoc[] getConstructors() {
        return ConstructorDoc.convert(mRootDoc, mDoc.constructors());
    }

    public ConstructorDoc[] getSortedConstructors() {
        ConstructorDoc[] docs = (ConstructorDoc[])getConstructors().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getInnerClasses() {
        return convert(mRootDoc, mDoc.innerClasses());
    }

    public ClassDoc[] getSortedInnerClasses() {
        ClassDoc[] docs = (ClassDoc[])getInnerClasses().clone();
        Arrays.sort(docs);
        return docs;
    }

    public ClassDoc[] getImportedClasses() {
        return convert(mRootDoc, mDoc.importedClasses());
    }

    public PackageDoc[] getImportedPackages() {
        return PackageDoc.convert(mRootDoc, mDoc.importedPackages());
    }

    public String getTypeName() {
        return mDoc.typeName();
    }

    public String getQualifiedTypeName() {
        return mDoc.qualifiedTypeName();
    }

    /**
     * Converts inner class names (replaces '.' with '$').
     */
    public String getTypeNameForFile() {
        return mDoc.typeName().replace('.', '$');
    }

    /**
     * Converts inner class names.
     */
    public String getQualifiedTypeNameForFile() {

        String typeName = getTypeNameForFile();
        String qualifiedTypeName = mDoc.qualifiedTypeName();

        int packageLength = qualifiedTypeName.length() - typeName.length();
        if (packageLength <= 0) {
            return typeName;
        }

        String packagePath = qualifiedTypeName.substring(0, packageLength);

        return packagePath + typeName;
    }

    /**
     * Returns the package name. Returns null if the class is unpackaged.
     */
    public String getPackageName() {
        return parseClassName(getQualifiedTypeNameForFile())[0];
    }

    public String getDimension() {
        return mDoc.dimension();
    }

    public String toString() {
        return mDoc.toString();
    }

    public ClassDoc getAsClassDoc() {
        return new ClassDoc(mRootDoc, mDoc.asClassDoc());
    }

    /**
     * Get a MethodDoc in this ClassDoc with a name and signature
     * matching that of the specified MethodDoc
     */
    public MethodDoc getMatchingMethod(MethodDoc method) {

        MethodDoc[] methods = getMethods();
        for (int i = 0; i < methods.length; i++) {

            if (method.getName().equals(methods[i].getName()) &&
                method.getSignature().equals(methods[i].getSignature())) {

                return methods[i];
            }
        }
        return null;
    }

    /**
     * Get a MethodDoc in this ClassDoc with a name and signature
     * matching that of the specified MethodDoc and accepted by the
     * specified MethodFinder
     */
    public MethodDoc getMatchingMethod(MethodDoc method, MethodFinder mf) {

        MethodDoc md = getMatchingMethod(method);
        if (md != null) {
            if (mf.checkMethod(md)) {
                return md;
            }
        }

        return null;
    }

    /**
     * Find a MethodDoc with a name and signature
     * matching that of the specified MethodDoc and accepted by the
     * specified MethodFinder.  This method searches the interfaces and
     * super class ancestry of the class represented by this ClassDoc for
     * a matching method.
     */
    public MethodDoc findMatchingMethod(MethodDoc method, MethodFinder mf) {

        // Look in this class's interface set
        MethodDoc md = findMatchingInterfaceMethod(method, mf);

        if (md != null) {
            return md;
        }

        // Look in this class's superclass ancestry
        ClassDoc superClass = getSuperclass();
        if (superClass != null) {

            md = superClass.getMatchingMethod(method, mf);
            if (md != null) {
                return md;
            }

            return superClass.findMatchingMethod(method, mf);
        }

        return null;
    }

    private MethodDoc findMatchingInterfaceMethod(MethodDoc method,
                                                  MethodFinder mf) {

        ClassDoc[] interfaces = getInterfaces();
        if (interfaces == null || interfaces.length == 0) {
            return null;
        }

        // Look at each interface's methods
        for (int i = 0; i < interfaces.length; i++) {

            MethodDoc m =
                interfaces[i].getMatchingMethod(method, mf);

            if (m != null) {
                return m;
            }
        }

        // Look for the method in the interface's interfaces
        for (int i = 0; i < interfaces.length; i++) {

            MethodDoc m =
                interfaces[i].findMatchingInterfaceMethod(method, mf);

            if (m != null) {
                return m;
            }
        }

        return null;
    }

}
