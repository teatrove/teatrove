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

import java.beans.Introspector;

import java.util.Vector;
import java.util.HashMap;

/**
 * There is no Doclet equivalent for this class.  It was created from
 * scratch to make documenting JavaBean properties easier.
 *
 * @author Mark Masse
 */
public class PropertyDoc extends MemberDoc {

    /**
     * Creates a set of PropertyDoc objects from the specified MemberDocs.
     * A single PropertyDoc object will be created for each pair of get/set
     * methods.
     */
    public static PropertyDoc[] create(RootDoc root,
                                       MethodDoc[] docs) {

        if (docs == null) {
            return null;
        }

        // Vector of the PropertyDocs to be returned by this method
        Vector propertyDocVector = new Vector();

        // HashMap that maps propertyName to PropertyDoc object.  This is
        // used to ensure that a single PropertyDoc instance contains
        // both the "get" and the "set" MethodDoc (if both exist).
        HashMap propertyMap = new HashMap();

        for (int i = 0; i < docs.length; i++) {

            MethodDoc method = docs[i];

            if (!method.isPublic() ||
                method.isStatic() || method.isExcluded()) {
                continue;
            }

            String methodName = method.getName();
            Parameter[] params = method.getParameters();
            Type returnType = method.getReturnType();
            String typeName = returnType.getQualifiedTypeName();
            if (typeName != null) {
                String dimension = returnType.getDimension();
                if (dimension != null && dimension.length() > 0) {
                    typeName += dimension;
                }
            }

            //
            // Look for JavaBeans naming patterns
            //

            String propertyName = null;
            boolean isReadMethod = true;

            if (params == null || (params.length == 0)) {
                // Accepts no params, might be a "get" method

                if (methodName.startsWith("get") && methodName.length() > 3) {

                    propertyName =
                        Introspector.decapitalize(methodName.substring(3));
                }
                else if (methodName.startsWith("is") &&
                         methodName.length() > 2) {

                    if ("boolean".equalsIgnoreCase(typeName)) {
                        propertyName =
                            Introspector.decapitalize(methodName.substring(2));
                    }
                }
            }
            else if (params != null && (params.length == 1)) {
                // Accepts a single param, might be a "set" method

                if (methodName.startsWith("set") && methodName.length() > 3) {

                    propertyName =
                        Introspector.decapitalize(methodName.substring(3));
                    isReadMethod = false;
                }
            }

            if (propertyName == null) {
                // Was neither a "get" nor a "set"
                continue;
            }

            PropertyDoc pd = (PropertyDoc) propertyMap.get(propertyName);
            if (pd != null) {
                MethodDoc readMethod = pd.getReadMethod();
                if (readMethod == null) {
                    pd.setReadMethod(method);
                }
                else {
                    MethodDoc writeMethod = pd.getWriteMethod();
                    if (writeMethod == null) {
                        pd.setWriteMethod(method);
                    }
                }
            }
            else {
                pd = new PropertyDoc(root, method, isReadMethod);
                pd.setName(propertyName);
                propertyMap.put(propertyName, pd);
                propertyDocVector.addElement(pd);
            }

        }

        PropertyDoc[] propertyDocs = new PropertyDoc[propertyDocVector.size()];
        propertyDocVector.copyInto(propertyDocs);

        return propertyDocs;
    }

    /**
     * Trims off the first word of the specified string and capitalizes the
     * new first word
     */
    private static final String trimFirstWord(String s) {
        s = s.trim();
        char[] chars = s.toCharArray();
        int firstSpace = -1;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isWhitespace(c)) {
                firstSpace = i;
                break;
            }
        }

        if (firstSpace == -1) {
            return s;
        }

        s = s.substring(firstSpace).trim();
        s = capitalize(s);

        return s;
    }

    private static final String capitalize(String s) {

        if (s != null && s.length() > 1) {
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }


    private String mName;
    private MethodDoc mReadMethod;
    private MethodDoc mWriteMethod;
    private String mCommentText;

    public PropertyDoc(RootDoc root,
                       MethodDoc methodDoc, boolean isReadMethod) {

        super(root, (com.sun.javadoc.MethodDoc) methodDoc.getInnerDoc());

        if (isReadMethod) {
            setReadMethod(methodDoc);
        }
        else {
            setWriteMethod(methodDoc);
        }
    }


    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public MethodDoc getReadMethod() {
        return mReadMethod;
    }

    public void setReadMethod(MethodDoc doc) {
        mReadMethod = doc;
    }


    public MethodDoc getWriteMethod() {
        return mWriteMethod;
    }

    public void setWriteMethod(MethodDoc doc) {
        mWriteMethod = doc;
    }


    public String getCommentText() {
        if (mCommentText != null) {
            return mCommentText;
        }

        String text = getTagValue("property");

        if (text == null) {
            text = getMethodCommentText();

            if (text != null) {
                String t = text.toLowerCase();

                if (t.startsWith("return ") ||
                    t.startsWith("returns ") ||
                    t.startsWith("get ") ||
                    t.startsWith("gets ") ||
                    t.startsWith("set ") ||
                    t.startsWith("sets ")) {

                    // Make it appear as if the comment is referring to the
                    // property instead of the method
                    text = trimFirstWord(text);
                }
            }
        }

        if (text != null) {
            text = text.trim();
        }
        mCommentText = capitalize(text);

        return mCommentText;
    }

    public boolean isBound() {
        return isTagPresent("bound");
    }

    public boolean isConstrained() {
        return isTagPresent("constrained");
    }

    public boolean isDefault() {
        return isTagPresent("default");
    }

    public String getPropertyEditorClassName() {
        return getTagValue("editor");
    }

    public boolean isMethod() {
        return false;
    }

    /**
     * Checks to see if the specified tag exists
     */
    public boolean isTagPresent(String tagName) {

        boolean tagPresent = false;

        if (mReadMethod != null) {
            tagPresent = mReadMethod.isTagPresent(tagName);
        }
        if (!tagPresent && mWriteMethod != null) {
            tagPresent = mWriteMethod.isTagPresent(tagName);
        }

        return tagPresent;
    }


    // Overridden to check both the read and write MethodDocs for the tag
    public String getTagValue(String tagName) {
        String value = null;

        if (mReadMethod != null) {
            value = mReadMethod.getTagValue(tagName);
        }
        if (value == null && mWriteMethod != null) {
            value = mWriteMethod.getTagValue(tagName);
        }

        return value;
    }



    //
    // Non-public interface
    //


    // Checks both the read and write MethodDocs for a comment
    private String getMethodCommentText() {

        String text = null;

        // Check the read method first
        if (mReadMethod != null) {
            text = mReadMethod.getCommentText();
        }
        if (text == null && mWriteMethod != null) {
            // Try the write method
            text = mWriteMethod.getCommentText();
        }

        if (text == null || text.trim().length() == 0) {
            // Try the "@return" tag
            text = getTagValue("return");
        }

        return text;
    }


}
