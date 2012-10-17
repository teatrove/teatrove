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

package org.teatrove.toolbox.beandoc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.teatrove.toolbox.beandoc.teadoc.ClassDoc;
import org.teatrove.toolbox.beandoc.teadoc.MethodDoc;
import org.teatrove.toolbox.beandoc.teadoc.MethodFinder;
import org.teatrove.toolbox.beandoc.teadoc.ParamTag;

/**
 * Context class used by the beandoc Tea templates.
 *
 * @author Mark Masse
 */
public class BeanDocContext extends org.teatrove.tea.runtime.DefaultContext {

    /** The BeanDocDoclet instance */
    private BeanDocDoclet mBeanDocDoclet;

    /** The Writer that writes the file */
    private Writer mOut;

    private MethodFinder mCommentedMethodFinder;
    private CommentedParamFinder mCommentedParamFinder;

    /**
     * Creates a new BeanDocContext for use with a Tea template.
     *
     * @param doclet the BeanDocDoclet instance
     * @param dest the "BeanInfo.java" file to write to.
     * @throws java.io.IOException If there was a problem with the I/O
     */
    public BeanDocContext(BeanDocDoclet doclet, File dest)
    throws IOException {

        mBeanDocDoclet = doclet;

        String parentDir = dest.getParent();
        if (parentDir != null) {
            File dir = new File(parentDir);
            if (!dir.exists() || !dir.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
        }

        mOut = new BufferedWriter(new FileWriter(dest));

        mCommentedMethodFinder = new MethodFinder() {
            public boolean checkMethod(MethodDoc md) {
                return (md != null && md.getCommentText() != null);
            }
        };

        mCommentedParamFinder = new CommentedParamFinder();

    }

    /**
     * The standard context method, implemented to write to the file.
     */
    public void print(Object obj) throws IOException {

        if (mOut != null) {
            // static Tea!
            mOut.write(toString(obj));
        }
    }

    /**
     * Closes the FileWriter
     */
    public void close() throws IOException {

        if (mOut != null) {
            mOut.close();
            mOut = null;
        }
    }

    /**
     * Get the current date.
     */
    public Date currentDate() {
        return new Date();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getMethodComment(ClassDoc classDoc, MethodDoc md) {

        String comment = md.getCommentText();
        if (comment == null) {
            md = classDoc.findMatchingMethod(md, mCommentedMethodFinder);
            if (md != null) {
                comment = md.getCommentText();
            }
        }

        if (comment != null) {
            comment = formatForLiteral(comment.trim());
        }

        return comment;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getMethodParamComment(ClassDoc classDoc,
                                        MethodDoc md,
                                        String paramName) {

        if (paramName == null) {
            return null;
        }

        mCommentedParamFinder.paramName = paramName;

        classDoc.getMatchingMethod(md, mCommentedParamFinder);
        String comment = mCommentedParamFinder.comment;

        if (comment == null) {
            classDoc.findMatchingMethod(md, mCommentedParamFinder);
            comment = mCommentedParamFinder.comment;
        }

        if (comment != null) {
            comment = formatForLiteral(comment.trim());
        }

        return comment;
    }

    /**
     * Returns a String that is same as the specified String except that
     * all special/escape characters are formatted as if they are escaped.
     * <p>
     * So, for example a tab character ('\t') becomes the string "\\t"
     *
     * @param s unescaped value
     * @return escaped value
     */
    public String formatForLiteral(String s) {

        if (s == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer(s.length());
        char[] chars = s.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\'':
                    sb.append("\\\'");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        return sb.toString();
    }

    /**
     * Returns the BeanDoc version number
     * @return the BeanDoc version number
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String getBeanDocVersion() {
        return PackageInfo.getProductVersion();
    }

    /**
     * Prints an error message.  Can be used to print an error message from
     * the Tea template.
     * @param msg The error message to print
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void printError(String msg) {
        mBeanDocDoclet.printError(msg);
    }

    /**
     * Prints an warning message.  Can be used to print an warning message
     * from the Tea template.
     * @param msg The warning message to print
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void printWarning(String msg) {
        mBeanDocDoclet.printWarning(msg);
    }

    /**
     * Prints an notice message.  Can be used to print an notice message from
     * the Tea template.
     * @param msg The notice to print
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void printNotice(String msg) {
        mBeanDocDoclet.printNotice(msg);
    }


    private static class CommentedParamFinder implements MethodFinder {

        public String paramName;
        public String comment;

        public boolean checkMethod(MethodDoc md) {
            comment = null;

            ParamTag[] paramTags = md.getParamTags();
            if (paramTags != null) {
                for (ParamTag paramTag : paramTags) {
                    if (paramName.equals(paramTag.getParameterName())) {
                        comment = paramTag.getParameterComment();
                        if (comment != null) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }
}

