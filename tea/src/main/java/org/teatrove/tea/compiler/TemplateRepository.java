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

package org.teatrove.tea.compiler;

import java.beans.MethodDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.log.Syslog;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyParser;

/**
 *
 * @author Guy A. Molinari
 *
 * The TemplateRepository class manages metadata pertaining to the
 * caller/callee hierarchy.  This information is derived from the template
 * class files and does not rely on tea source code.  In order to reduce the
 * repository spin-up time, this data is stored in a file named <b>.templates.info</b>
 * and is updated as templates are successfully compiled.  This file is located
 * in the root directory where template classes are stored.
 *
 */
public class TemplateRepository {

    private File mRootClassesDir;
    // private File mRepositoryFile;
    private String mRootPackage;

    private HashMap<String, TemplateInfo> mTemplateInfoMap =
        new HashMap<String, TemplateInfo>();

    private HashMap<String, Map<String, TemplateInfo>> mAncestorMap = null;
    private HashMap<String, Map<String, TemplateInfo>> mFunctionMap = null;
    private static final SimpleDateFormat mDateFmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss(S) a z");
    private static TemplateRepository mInstance = null;

    public static final String REPOSITORY_FILENAME = ".templates.info";

    private TemplateRepository(File rootClassesDir, String rootPackage) {
        mRootClassesDir = rootClassesDir;
        mRootPackage = rootPackage.replace('.', '/');
        mRootPackage = !mRootPackage.endsWith("/") ? mRootPackage += "/" : mRootPackage;
    }

    /**
     * Initalizes and loads the repository.  Note:  call this method instead
     * of a constructor to initialize the singleton instance.
     *
     * @param rootClassesDir The root template classes directory.
     * @param rootPackage The root package name (org.teatrove.teaservlet.template).
     */
    public static synchronized void init(File rootClassesDir, String rootPackage) {
        if (mInstance == null) {
            _init(rootClassesDir, rootPackage);
        }
    }
    
    private static void _init(File rootClassesDir, String rootPackage) {
        if (rootClassesDir == null || rootPackage == null)
            return;
        if (! rootClassesDir.isDirectory())
            throw new IllegalArgumentException("Root classes parameter not a directory.");
        mInstance = new TemplateRepository(rootClassesDir, rootPackage);
        try {
            long start = System.currentTimeMillis();
            mInstance.loadRepositoryFile();
            Syslog.info("Repository initialized.  Elapsed time " +
                (System.currentTimeMillis() - start) + " ms.");
        }
        catch (Exception ex) {
            long start = System.currentTimeMillis();
            Syslog.info("Generating repository...");
            try {
                mInstance.getTemplateInfoForAllFiles();
                mInstance.createRepositoryFile();
            }
            catch (IOException ix) {
                ix.printStackTrace();
            }
            Syslog.info("Repository created.  Elapsed time " +
                 (((double) (System.currentTimeMillis() - start)) / 1000) + " sec.");
        }
    }

    /**
     * Check to see if the repository was initialized (singleton instance
     * create).
     */
    public static boolean isInitialized() {
        return mInstance != null;
    }

    /**
     * Get repository instance. Accessor method (GoF Singleton pattern).
     */
    public static TemplateRepository getInstance() {
        if (mInstance == null)
            throw new RuntimeException("Repository not initialized.");
        return mInstance;
    }

    /**
     * Retrieves template metadata for a class file.
     *
     * @param f The class file object.
     * @return TemplateInfo The template metadata.
     */
    public TemplateInfo getTemplateInfoForClassFile(File f) throws IOException {

        if (!f.exists())
            return null;
        MethodInfo mi = TemplateCallExtractor.getTemplateExecuteMethod(
            new FileInputStream(f));
        return mi != null ? new TemplateInfo(mi, f.lastModified(), mRootClassesDir) : null;
    }

    /**
     * Template metadata container object.
     */
    public class TemplateInfo {
        String mName;
        String mSourceFile;
        long mLastModified;
        TypeDesc mReturnType;
        TypeDesc[] mParameterTypes;
        String[] mDependents;
        TemplateCallExtractor.AppMethodInfo[] mMethodsCalled;
        boolean mPrecompiled = false;

        /**
         * Construct a TemplateInfo from MethodInfo.
         *
         * @param mi MethodInfo instance.
         * @param lastModified Last file modification timestamp.
         * @param rootClassesDir Root template classes location.
         * @param precompiled true if pre-compiled.
         */
        TemplateInfo(MethodInfo mi, long lastModified, File rootClassesDir, boolean precompiled) {
            mLastModified = lastModified;
            mName = mi.getClassFile().getClassName().replace('.', '/');
            mSourceFile = mi.getClassFile().getSourceFile();
            mReturnType = mi.getMethodDescriptor().getReturnType();
            mParameterTypes = mi.getMethodDescriptor().getParameterTypes();
            mDependents = TemplateCallExtractor.getTemplatesCalled(
                rootClassesDir.getAbsolutePath(), mi.getClassFile().getClassName());
            if (mParameterTypes.length > 0) {
                mMethodsCalled = TemplateCallExtractor.getAppMethodsCalled(
                    rootClassesDir.getAbsolutePath(), mi.getClassFile().getClassName(),
                    mParameterTypes[0].toString());
            }
            else
                mMethodsCalled = new TemplateCallExtractor.AppMethodInfo[0];
            mPrecompiled = precompiled;
        }

        /**
         * Construct a TemplateInfo from a PropertyMap extracting a single
         * name from the map.
         *
         * @param name The template name.
         * @param p The property map.
         */
        TemplateInfo(String name, PropertyMap p) {
            mName = name;
            try {
                mLastModified = mDateFmt.parse((String) p.get("lastModified")).
                    getTime();
            }
            catch (ParseException ignore) { }
            mReturnType = TypeDesc.forDescriptor((String) p.get("returnType"));
            ArrayList<TypeDesc> paramList = new ArrayList<TypeDesc>();
            if (p.get("parameterTypes") != null) {
                StringTokenizer params = new StringTokenizer(
                    (String) p.get("parameterTypes"), ",", false);
                while(params.hasMoreTokens())
                    paramList.add(TypeDesc.forDescriptor(
                        (params.nextToken()).trim()));
            }
            mParameterTypes = paramList.toArray(
                new TypeDesc[paramList.size()]);
            ArrayList<String> depList = new ArrayList<String>();
            if (p.get("dependents") != null) {
                StringTokenizer deps = new StringTokenizer(
                    (String) p.get("dependents"), ",", false);
                while(deps.hasMoreTokens())
                    depList.add(deps.nextToken().trim());
            }
            mDependents = depList.toArray(new String[depList.size()]);
            ArrayList<TemplateCallExtractor.AppMethodInfo> methodList =
                new ArrayList<TemplateCallExtractor.AppMethodInfo>();
            if (p.get("methodsCalled") != null) {
                StringTokenizer meths = new StringTokenizer(
                    (String) p.get("methodsCalled"), ",", false);
                while(meths.hasMoreTokens())
                    methodList.add(new TemplateCallExtractor.AppMethodInfo(meths.nextToken().trim()));
            }
            mMethodsCalled = 
                methodList.toArray(new TemplateCallExtractor.AppMethodInfo[methodList.size()]);
            mPrecompiled = "true".equals(p.get("precompiled"));
        }


        /**
         * Construct a TemplateInfo.
         *
         * @param mi MethodInfo instance.
         * @param lastModified Last file modification timestamp.
         * @param rootClassesDir Root template classes location.
         */
        TemplateInfo(MethodInfo mi, long lastModified, File rootClassesDir) {
            this(mi, lastModified, rootClassesDir, false);
        }


        /**
         * Returns false if the template signatures do not match.  The
         * template modification date and other info are ignored.
         */
        public boolean equals(TemplateInfo t) {
            if (t == null || !this.getName().equals(t.getName()) ||
                !this.getReturnType().equals(t.getReturnType()) ||
                this.getParameterTypes().length != t.getParameterTypes().length)
                return false;

            for (int i = 0; i < this.getParameterTypes().length; i++) {
                if (!this.getParameterTypes()[i].equals(
                        t.getParameterTypes()[i]))
                    return false;
            }
            return true;
        }

        public String getName() { return mName; }
        
        public String getSourceFile() { return mSourceFile; }

        public TypeDesc getReturnType() { return mReturnType; }

        public TypeDesc[] getParameterTypes() { return mParameterTypes; }

        public String[] getDependents() { return mDependents; }

        public long getLastModified() { return mLastModified; }

        public Date getLastModifiedDate() { return new Date(mLastModified); }

        public boolean isPrecompiled() { return mPrecompiled; }

        public String getShortName() {
            return mName.substring(mRootPackage.length());
        }

        public TemplateCallExtractor.AppMethodInfo[] getAppMethodsCalled() {
            return mMethodsCalled;
        }

        /**
         * Returns a String in 'PropertyMap' format.
         */
        public String toString() {
            String indent = "        ";
            StringBuffer buf = new StringBuffer();
            buf.append("    \"").append(mName).append("\" {\n");
            buf.append(indent).append("lastModified = ").append(
                mDateFmt.format(new Date(mLastModified))).append("\n");
            buf.append(indent).append("returnType = ").append(mReturnType).
                append("\n");
            if (mParameterTypes.length > 0) {
                buf.append(indent).append("parameterTypes = ");
                for (int i = 0; i < mParameterTypes.length; i++) {
                    buf.append(mParameterTypes[i]);
                    if (i < mParameterTypes.length - 1)
                        buf.append(", ");
                }
                buf.append("\n");
            }
            if (mDependents.length > 0) {
                buf.append(indent).append("dependents = ");
                for (int i = 0; i < mDependents.length; i++) {
                    buf.append(mDependents[i]);
                    if (i < mDependents.length - 1)
                        buf.append(", ");
                }
                buf.append("\n");
            }
            if (mMethodsCalled.length > 0) {
                buf.append(indent).append("methodsCalled = ");
                for (int i = 0; i < mMethodsCalled.length; i++) {
                    buf.append(mMethodsCalled[i]);
                    if (i < mMethodsCalled.length - 1)
                        buf.append(", ");
                }
                buf.append("\n");
            }
            buf.append(indent).append("precompiled = " + mPrecompiled + "\n");
            buf.append("    }\n");
            return buf.toString();
        }
    }

    /**
     * This method is called to begin searching the classes directory
     * and all subdirectories. Jar files are also processed.
     */
    private void getTemplateInfoForAllFiles() throws IOException {
        getTemplateInfoForAllFiles(mRootClassesDir);
        getTemplateInfoForPrecompiledTemplates();
        buildAncestorMap();
    }

    /**
     * Perform a depth first recursion of subdirectories, then process
     * all class files in the current directory.
     */
    private void getTemplateInfoForAllFiles(File startingFileContext) throws IOException {

        // Do a depth first recursion
        File[] dirList = startingFileContext.listFiles(
                new FilenameFilter() {
            public boolean accept(File f, String name) {
                return f.isDirectory();
            }
        });

        for (int i = 0; dirList != null && i < dirList.length; i++)
            getTemplateInfoForAllFiles(dirList[i]);

        // Process class files in current context
        File[] classList = startingFileContext.listFiles(
                new FilenameFilter() {
            public boolean accept(File f, String name) {
                return f.canRead() && name.endsWith(".class");
            }
        });

        for (int i = 0; classList != null && i < classList.length; i++) {
            TemplateInfo t = getTemplateInfoForClassFile(classList[i]);
            if (t != null)
                mTemplateInfoMap.put(t.getName(), t);
        }
    }

    /**
     * Rip apart all jar files in the classpath looking for pre-compiled templates.
     */
    private void getTemplateInfoForPrecompiledTemplates() {

        StringTokenizer paths = new StringTokenizer(
            System.getProperty("java.class.path"), File.pathSeparator, false);
        while(paths.hasMoreTokens()) {
            String name = paths.nextToken();
            if (!name.endsWith(".jar"))
                continue;
            try {
                JarFile jar = null;
                try {
                    jar = new JarFile(name);
                }
                catch (ZipException zx) {
                    Syslog.warn("The jar file " + name + " is in the classpath but does not exist or is unreadable.");
                    continue;
                }
                for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    if (! entry.getName().startsWith(TemplateCallExtractor.TEMPLATE_PACKAGE.replace('.', '/')) ||
                            ! entry.getName().endsWith(".class"))
                        continue;

                    MethodInfo mi = TemplateCallExtractor.getTemplateExecuteMethod(jar.getInputStream(entry));
                    if (mi != null) {
                        TemplateInfo t =  new TemplateInfo(mi, entry.getTime(), mRootClassesDir, true);
                        mTemplateInfoMap.put(t.getName(), t);
                    }
                }
            }
            catch (JarException jx) {
                Syslog.error(jx);
            }
            catch (IOException ix) {
                Syslog.error(ix);
            }
        }


    }



    /**
     * Persist the current state of the repository to disk.
     */
    public synchronized void createRepositoryFile() throws IOException {
        StringBuffer buf = new StringBuffer();
        long lastModified = System.currentTimeMillis();
        buf.append("lastModified = ").append(
            mDateFmt.format(new Date(lastModified))).append("\n");
        buf.append("templates {\n");
        for (Iterator<String> i = mTemplateInfoMap.keySet().iterator(); i.hasNext(); )
            buf.append((mTemplateInfoMap.get(i.next())).toString());
        buf.append("}\n");
        File repositoryFile = new File(mRootClassesDir,
            REPOSITORY_FILENAME);
        FileOutputStream out = new FileOutputStream(repositoryFile);
        out.write(buf.toString().getBytes());
        out.flush();
        out.close();
        repositoryFile.setLastModified(lastModified);
    }

    /**
     * Loads the current repository state from disk.
     */
    private void loadRepositoryFile() throws IOException, ParseException {
        PropertyMap m = new PropertyMap();
        PropertyParser p = new PropertyParser(m);
        File repositoryFile = new File(mRootClassesDir,
            REPOSITORY_FILENAME);
        p.parse(new FileReader(repositoryFile));
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        long lastModified = mDateFmt.parse((String) m.get("lastModified")).
            getTime();
        if ((isWindows && repositoryFile.lastModified() != lastModified) ||
                (!isWindows && Math.max(repositoryFile.lastModified(), lastModified) -
                Math.min(repositoryFile.lastModified(), lastModified) >= 1000L)) {
            String corruptMsg = "Repository corrupt.  Rebuild Needed.";
            Syslog.error(corruptMsg);
            throw new RuntimeException(corruptMsg);
        }
        PropertyMap templateMap = m.subMap("templates");

        // Merge the TemplateInfos into existing mTemplateInfoMap
        for (Iterator<?> i = templateMap.subMapKeySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            mTemplateInfoMap.put(key, new TemplateInfo(key,
                templateMap.subMap(key)));
        }
        buildAncestorMap();
    }

    /**
     * Create caller hierarchy.
     *
     * JoshY 12/28/04 - Due to a bug in the 1.4.2_05 server VM, it's important that you not
     * use the TemplateCallExtractor.AppMethodInfo as a map key.  We're using a String
     * representation here as a workaround.
     */
    private void buildAncestorMap() {
        mAncestorMap = new HashMap<String, Map<String, TemplateInfo>>();
        mFunctionMap = new HashMap<String, Map<String, TemplateInfo>>();
        for (Iterator<TemplateInfo> i = mTemplateInfoMap.values().iterator(); i.hasNext();) {
            TemplateInfo t = i.next();
            for (int j = 0; j < t.getDependents().length; j++) {
                Map<String, TemplateInfo> parentMap =
                    mAncestorMap.get(t.getDependents()[j]);
                if (parentMap == null)
                    parentMap = new HashMap<String, TemplateInfo>();
                if (!parentMap.containsKey(t.getName()))
                    parentMap.put(t.getName(), t);
                mAncestorMap.put(t.getDependents()[j], parentMap);
            }
            for (int j = 0; j < t.getAppMethodsCalled().length; j++) {
                String sKey = t.getAppMethodsCalled()[j].toString();
                Map<String, TemplateInfo> callMap = mFunctionMap.get(sKey);
                if (callMap == null) {
                    callMap = new HashMap<String, TemplateInfo>();
                    mFunctionMap.put(sKey, callMap);
                }
                if (!callMap.containsKey(t.getName()))
                    callMap.put(t.getName(), t);
            }
        }
    }

    /**
     * Retrieve the metadata for all templates that call the named
     * template.  The template name can be the short form, long form
     * and use either '.' or '/' separators.
     *
     * @param templateName The name of the template.
     * @return TemplateInfo[] The callers (consumers).
     */
    public TemplateInfo[] getCallers(String templateName) {
        templateName = !templateName.startsWith(mRootPackage) ?
            getFullyQualifiedTemplateName(templateName) : templateName;
        if (mAncestorMap.containsKey(templateName)) {
            Collection<TemplateInfo> parents = mAncestorMap.get(templateName).values();
            return parents.toArray(new TemplateInfo[parents.size()]);
        }
        else
            return new TemplateInfo[0];
    }

    /**
     * Retrieve the metadata for all templates that call the specified
     * method.
     *
     * @param methodDesc The method descriptor to find.
     * @return TemplateInfo[] The callers (consumers).
     */
    public TemplateInfo[] getMethodCallers(MethodDescriptor methodDesc) {
        Method method = methodDesc.getMethod();
        Class<?>[] paramClasses = method.getParameterTypes();
        java.lang.reflect.Type[] paramTypes = method.getGenericParameterTypes();

        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i = 0; i < params.length; i++)
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
        TemplateCallExtractor.AppMethodInfo mi =
            new TemplateCallExtractor.AppMethodInfo(method.getName(), params);

        if (mFunctionMap.containsKey(mi)) {
            Collection<TemplateInfo> callers = mFunctionMap.get(mi).values();
            return callers.toArray(new TemplateInfo[callers.size()]);
        }
        else
            return new TemplateInfo[0];
    }

    /**
     * Retrieve the metadata for a given template.  The template name
     * can be the short form, long form and use either '.' or '/'
     * separators.
     *
     * @param templateName The name of the template.
     * @return TemplateInfo The metadata.
     */
    public TemplateInfo getTemplateInfo(String templateName) {
        templateName = !templateName.startsWith(mRootPackage) ?
            getFullyQualifiedTemplateName(templateName) : templateName;
        return mTemplateInfoMap.get(templateName);
    }

    /**
     * Retrieve the metadata for all templates.
     * @return TemplateInfo[] The metadata.
     */
    public TemplateInfo[] getTemplateInfos() {
    	Collection<TemplateInfo> collection = mTemplateInfoMap.values();
    	TemplateInfo[] result = null;
        if (collection.size() > 0) {
        	result = collection.toArray(new TemplateInfo[collection.size()]);
        }
        return result;
    }

    /**
     * Retrieve the callers of a list of given template names
     * where the source file can be located (delegated to the compiler),
     * and the signature of given template(s) has changed since the last
     * call to <b>update</b>.  The class file(s) of the corresponding
     * template names returned are removed.
     *
     * @param names The names of the templates to check.
     * @param compiler The compiler instance.
     * @return String[] The caller templates needing recompilation.
     */
    public String[] getCallersNeedingRecompile(String[] names, Compiler compiler) throws IOException {
        HashMap<String, TemplateInfo> needsCompile =
            new HashMap<String, TemplateInfo>();

        for (int i = 0; i < names.length; i++) {
            String templateName = getFullyQualifiedTemplateName(names[i]);
            TemplateInfo tNew = getTemplateInfoForClassFile(getClassFileForName(templateName));
            if (tNew == null || !mTemplateInfoMap.containsKey(templateName))
                continue;
            TemplateInfo tOld = mTemplateInfoMap.get(templateName);
            // Don't bother if template signature hasn't changed.
            if (tNew.equals(tOld))
                continue;
            TemplateInfo[] callers = getCallers(templateName);
            for (int j = 0; j < callers.length; j++) {
                String shortPath = callers[j].getName().
                    substring(mRootPackage.length()).replace('/','.');
                if (!needsCompile.containsKey(shortPath) &&
                        compiler.sourceExists(shortPath)) {
                    needsCompile.put(shortPath, callers[j]);
                    // Nuke the caller's class file to force a recompile.
                    getClassFileForName(callers[j].getName()).delete();
                }
            }
        }
        return needsCompile.keySet().toArray(new String[needsCompile.size()]);
    }

    /**
     * Synchronize the repository state with respect to the named template
     * class file(s).  The dependency lists will be updated an the new
     * repository file will be written to disk.
     *
     * @param templatesChanged The templates to update.
     */
    public synchronized void update(String[] templatesChanged) throws IOException {
        HashMap<String, String> updated = new HashMap<String, String>();
        for (int i = 0; i < templatesChanged.length; i++) {
            String templateName = getFullyQualifiedTemplateName(templatesChanged[i]);
            if (!updated.containsKey(templateName)) {
                TemplateInfo tUpdated = getTemplateInfoForClassFile(getClassFileForName(templateName));
                if (tUpdated != null)
                    mTemplateInfoMap.put(templateName, tUpdated);
                else
                    mTemplateInfoMap.remove(templateName);
                updated.put(templateName, templateName);
            }
        }
        buildAncestorMap();
        createRepositoryFile();
    }

    /**
     * Canonicalize the template name.
     */
    private String getFullyQualifiedTemplateName(String name) {
        String s = name.replace('.', '/');
        return !s.startsWith(mRootPackage) ? mRootPackage + s : s;
    }

    /**
     * Get the class file for a template name.
     */
    private File getClassFileForName(String name) throws IOException {
        File templateFile = null;
        if (name.startsWith(mRootPackage))
            templateFile = new File(mRootClassesDir,
                name.substring(mRootPackage.length()) + ".class");

        if (templateFile == null || !templateFile.exists())
            templateFile = new File(mRootClassesDir, name + ".class");

        return templateFile;
    }

    /**
     * Type desc format beautifier
     */
    public static String formatTypeDesc(TypeDesc d) {
        StringBuffer name = new StringBuffer(d.getFullName());
        if (d.isPrimitive()) {
            switch (d.getTypeCode()) {
                case TypeDesc.VOID_CODE:
                    return "Void";
                case TypeDesc.BOOLEAN_CODE:
                    name = new StringBuffer("boolean");
                    break;
                case TypeDesc.BYTE_CODE:
                    name = new StringBuffer("byte");
                    break;
                case TypeDesc.CHAR_CODE:
                    name = new StringBuffer("char");
                    break;
                case TypeDesc.DOUBLE_CODE:
                    name = new StringBuffer("double");
                    break;
                case TypeDesc.FLOAT_CODE:
                    name = new StringBuffer("float");
                    break;
                case TypeDesc.INT_CODE:
                    name = new StringBuffer("int");
                    break;
                case TypeDesc.LONG_CODE:
                    name = new StringBuffer("long");
                    break;
                case TypeDesc.SHORT_CODE:
                    name = new StringBuffer("short");
                    break;
                default:
                    name = new StringBuffer("unknown");
                    break;
            }
        }
        int i = name.toString().indexOf("java.lang.");
        if (i != -1) {
            name.replace(i, 10, "");
        }
        if (d.isArray()) {
            for (int j = 0; j < d.getDimensions(); j++)
                name.append("[]");
        }
        return name.toString();
    }

}
