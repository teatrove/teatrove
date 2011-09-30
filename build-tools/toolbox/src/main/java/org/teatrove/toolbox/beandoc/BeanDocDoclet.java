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

import org.teatrove.toolbox.beandoc.teadoc.*;

import java.io.*;

import org.teatrove.tea.runtime.TemplateLoader;

/**
 * The BeanDocDoclet generates "BeanInfo.java" files for each of the
 * classes and interfaces to be documented.  The Java file is created using
 * an accompanying Tea template.
 * <p>
 * BeanDocDoclet makes use of the teadoc sub-package in order to wrap the
 * classes of the doclet API to provide a Tea-friendly JavaBean interface
 * for each class.
 *
 * @author Mark Masse
 */
public class BeanDocDoclet extends com.sun.javadoc.Doclet {

    /** The class name of the Tea template */
    private static final String DEFAULT_TEMPLATE_CLASS_NAME =
        "org.teatrove.toolbox.beandoc.template.BeanInfo";

    //
    // static methods
    //

    /**
     * Starts the BeanDoc doclet.  Called by the javadoc tool.
     */
    public static boolean start(com.sun.javadoc.RootDoc root) {
        try {
            BeanDocDoclet doclet = new BeanDocDoclet(root);
            doclet.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    public static int optionLength(String option) {
        if (option.equals("-d")) {
            return 2;
        }
        else {
            return 0;
        }
    }

    //
    // Instance fields
    //

    private RootDoc mRootDoc;
    private File mDest;
    private String mTemplateClassName;
    private TemplateLoader.Template mTemplate;

    //
    // Instance methods
    //

    /**
     * Creates a new BeanDocDoclet with the specified RootDoc object.
     */
    public BeanDocDoclet(com.sun.javadoc.RootDoc root) throws Exception {

        String[][] options = root.options();
        String dest = ".";

        for (int i = 0; i < options.length; i++) {
            String[] values = options[i];
            String key = values[0];

            if (key.equals("-d") && values.length > 1) {
                dest = values[1];
            }
        }

        // TODO:  Allow template class name to be specified as a param?
        init(root, dest, DEFAULT_TEMPLATE_CLASS_NAME);
    }

    /**
     * Generates BeanInfo.java files for each of the ClassDocs in the
     * RootDoc.
     */
    public void start() {

        ClassDoc[] classDocs = mRootDoc.getClasses();

        for (int i = 0; i < classDocs.length; i++) {

            ClassDoc classDoc = classDocs[i];
            if (!accept(classDoc)) {
                continue;
            }

            try {
                generateBeanInfo(classDoc);
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.toString());
            }
        }
    }

    /**
     * Returns the RootDoc object
     */
    public RootDoc getRootDoc() {
        return mRootDoc;
    }


    /**
     * Prints an error message
     */
    public void printError(String msg) {
        mRootDoc.printError(msg);
    }

    /**
     * Prints an warning message
     */
    public void printWarning(String msg) {
        mRootDoc.printWarning(msg);
    }

    /**
     * Prints an notice message
     */
    public void printNotice(String msg) {
        mRootDoc.printNotice(msg);
    }

    //
    // Non-public methods
    //


    /**
     * Initializes the BeanDocDoclet instance.
     */
    protected void init(com.sun.javadoc.RootDoc root,
                      String dest,
                      String templateClassName) throws Exception {

        mRootDoc = new RootDoc(root);

        mDest = new File(dest);
        mDest.mkdirs();

        mTemplateClassName = templateClassName;
        if (mTemplateClassName == null) {
            printWarning("No template name");
            return;
        }

        String[] templatePath = ClassDoc.parseClassName(mTemplateClassName);

        //
        // Load the specified template class
        //

        TemplateLoader loader =
            new TemplateLoader(getClass().getClassLoader(),
                               templatePath[0]);

        mTemplate = loader.getTemplate(templatePath[1]);

        Class[] params = mTemplate.getParameterTypes();
        if (params.length != 1 || params[0] != ClassDoc.class) {
            printError("Template has incorrect param signature");
        }
    }

    /**
     * Returns true if the ClassDoc should be documented
     */
    protected boolean accept(ClassDoc classDoc) {
        return (classDoc.isPublic() &&
                !classDoc.getTypeName().endsWith("BeanInfo"));
    }


    /**
     * Using a Tea template, generates a "BeanInfo.java" file for the
     * specified ClassDoc.
     */
    private void generateBeanInfo(ClassDoc classDoc) throws Exception {

        String beanInfoJavaFileName =
            classDoc.getTypeNameForFile() + "BeanInfo.java";

        String beanInfoJavaFilePath = beanInfoJavaFileName;

        String packageName = classDoc.getPackageName();

        if (packageName != null) {
            // Create the file path using the package
            beanInfoJavaFilePath =
                packageName.replace('.', '/') + "/" + beanInfoJavaFileName;
        }

        File dest = null;

        if (mDest != null) {
            dest = new File(mDest, beanInfoJavaFilePath);
        }
        else {
            dest = new File(beanInfoJavaFilePath);
        }

        if (dest.exists()) {
            if (dest.canWrite()) {
                //printWarning("File exists: " + beanInfoJavaFileName);
            }
            else {
                // Attempt to overwrite the file via delete
                if (dest.delete()) {
                    //printWarning("File exists: " + beanInfoJavaFileName);

                }
                else {
                    printWarning("File exists and cannot be written: " +
                                 beanInfoJavaFileName);
                    return;
                }
            }
        }

        BeanDocContext context = null;
        try {

            context = new BeanDocContext(this, dest);

            if (mTemplate != null) {
                printNotice("Creating BeanInfo: " + beanInfoJavaFilePath);
                mTemplate.execute(context, new Object[] { classDoc });
            }
            else {
                printWarning("No template");
            }
        }
        finally {
            if (context != null) {
                context.close();
            }
        }
    }

}

