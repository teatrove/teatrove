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

package org.teatrove.tea.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.trove.util.ClassInjector;

/**
 * FileCompiler compiles tea source files by reading them from a file or a
 * directory. The compiled code can be written as class files to a given
 * destination directory, they can be passed to a ClassInjector, or they
 * can be sent to both.
 *
 * <p>When given a directory, FileCompiler compiles all files with the
 * extension ".tea". If a destination directory is used, tea files that have a
 * matching class file that is more up-to-date will not be compiled, unless
 * they are forced to be re-compiled.
 *
 * @author Brian S O'Neill
 * @see ClassInjector
 */
public class FileCompiler extends AbstractCompiler {
    /**
     * Entry point for a command-line tool suitable for compiling Tea
     * templates to be bundled with a product. Templates are read from files
     * that must have the extension ".tea", and any compilation error messages
     * are sent to standard out.
     *
     * <pre>
     * Usage: java org.teatrove.tea.util.FileCompiler {options} 
     * &lt;template root directory&gt; {templates}
     *
     * where {options} includes:
     * -context &lt;class&gt;     Specify a runtime context class to compile against.
     * -dest &lt;directory&gt;    Specify where to place generated class files.
     * -force               Compile all templates, even if up-to-date.
     * -package &lt;package&gt;   Root package to compile templates into.
     * -encoding &lt;encoding&gt; Specify character encoding used by source files.
     * -guardian            Enable the exception guardian.
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            usage();
            return;
        }

        Class<?> context = null;
        File destDir = null;
        boolean force = false;
        String rootPackage = null;
        String encoding = null;
        boolean guardian = false;
        File rootDir = null;
        Collection<String> templates = new ArrayList<String>(args.length);

        try {
            boolean parsingOptions = true;
            for (int i=0; i<args.length;) {
                String arg = args[i++];
                if (arg.startsWith("-") && parsingOptions) {
                    if (arg.equals("-context") && context == null) {
                        context = Class.forName(args[i++]);
                        continue;
                    }
                    else if (arg.equals("-dest") && destDir == null) {
                        destDir = new File(args[i++]);
                        continue;
                    }
                    else if (arg.equals("-force") && !force) {
                        force = true;
                        continue;
                    }
                    else if (arg.equals("-package") && rootPackage == null) {
                        rootPackage = args[i++];
                        continue;
                    }
                    else if (arg.equals("-encoding") && encoding == null) {
                        encoding = args[i++];
                        continue;
                    }
                    else if (arg.equals("-guardian") && !guardian) {
                        guardian = true;
                        continue;
                    }
                }
                else {
                    if (parsingOptions) {
                        parsingOptions = false;
                        rootDir = new File(arg);
                        continue;
                    }

                    arg = arg.replace('/', '.');
                    arg = arg.replace(File.separatorChar, '.');
                    while (arg.startsWith(".")) {
                        arg = arg.substring(1);
                    }
                    while (arg.endsWith(".")) {
                        arg = arg.substring(0, arg.length() - 1);
                    }
                    templates.add(arg);
                    continue;
                }
                
                usage();
                return;
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            usage();
            return;
        }

        if (rootDir == null) {
            usage();
            return;
        }

        if (context == null) {
            context = org.teatrove.tea.runtime.UtilityContext.class;
        }

        if (destDir == null) {
            destDir = rootDir;
        }

        FileCompiler compiler =
            new FileCompiler(rootDir, rootPackage, destDir, null, encoding);

        compiler.setRuntimeContext(context);
        compiler.setForceCompile(force);
        compiler.addErrorListener(new ConsoleErrorReporter(System.out));
        compiler.setExceptionGuardianEnabled(guardian);

        String[] names;
        if (templates.size() == 0) {
            names = compiler.compileAll(true);
        }
        else {
            names = templates.toArray(new String[templates.size()]);
            names = compiler.compile(names);
        }

        int errorCount = compiler.getErrorCount();

        if (errorCount > 0) {
            String msg = String.valueOf(errorCount) + " error";
            if (errorCount != 1) {
                msg += 's';
            }
            System.out.println(msg);
            System.exit(1);
        }
    }

    private static void usage() {
        String usageDetail =
            " -context <class>     Specify a runtime context class to compile against.\n" +
            " -dest <directory>    Specify where to place generated class files.\n" +
            " -force               Compile all templates, even if up-to-date.\n" +
            " -package <package>   Root package to compile templates into.\n" +
            " -encoding <encoding> Specify character encoding used by source files.\n" +
            " -guardian            Enable the exception guardian.";

        System.out.print("\nUsage: ");
        System.out.print("java ");
        System.out.print(FileCompiler.class.getName());
        System.out.println(" {options} <template root directory> {templates}");
        System.out.println();
        System.out.println("where {options} includes:");
        System.out.println(usageDetail);
    }

    private File[] mRootSourceDirs;
    private Map<File, JarOfTemplates> mSrcJars;
    
    /**
     * @param rootSourceDir Required root source directory
     * @param rootPackage Optional root package to compile source to
     * @param rootDestDir Optional directory to place generated class files
     * @param injector Optional ClassInjector to feed generated classes into
     */
    public FileCompiler(File rootSourceDir,
                        String rootPackage,
                        File rootDestDir,
                        ClassInjector injector) {
        this(new File[]{rootSourceDir}, rootPackage, rootDestDir, injector, null);
    }

    /**
     * @param rootSourceDir Required root source directory
     * @param rootPackage Optional root package to compile source to
     * @param rootDestDir Optional directory to place generated class files
     * @param injector Optional ClassInjector to feed generated classes into
     * @param encoding Optional character encoding used by source files
     */
    public FileCompiler(File rootSourceDir,
                        String rootPackage,
                        File rootDestDir,
                        ClassInjector injector,
                        String encoding) {
        this(new File[]{rootSourceDir}, rootPackage, rootDestDir, injector, encoding);
    }

    /**
     * @param rootSourceDirs Required root source directories
     * @param rootPackage Optional root package to compile source to
     * @param rootDestDir Optional directory to place generated class files
     * @param injector Optional ClassInjector to feed generated classes into
     */
    public FileCompiler(File[] rootSourceDirs,
                        String rootPackage,
                        File rootDestDir,
                        ClassInjector injector) {
        this(rootSourceDirs, rootPackage, rootDestDir, injector, null);
    }

    /**
     * @param rootSourceDirs Required root source directories
     * @param rootPackage Optional root package to compile source to
     * @param rootDestDir Optional directory to place generated class files
     * @param injector Optional ClassInjector to feed generated classes into
     * @param encoding Optional character encoding used by source files
     */
    public FileCompiler(File[] rootSourceDirs,
                        String rootPackage,
                        File rootDestDir,
                        ClassInjector injector,
                        String encoding) {
        super(injector, rootPackage);
        init(rootSourceDirs, rootDestDir, encoding);
    }

    /**
     * @param rootSourceDirs Required root source directories
     * @param rootPackage Optional root package to compile source to
     * @param rootDestDir Optional directory to place generated class files
     * @param injector Optional ClassInjector to feed generated classes into
     * @param encoding Optional character encoding used by source files
     * @param parseTreeMap Optional map should be thread-safe. See
     * {@link Compiler} for details.
     */
    public FileCompiler(File[] rootSourceDirs,
                        String rootPackage,
                        File rootDestDir,
                        ClassInjector injector,
                        String encoding,
                        Map<String, Template> parseTreeMap) {
        super(injector, rootPackage, parseTreeMap);
        init(rootSourceDirs, rootDestDir, encoding);
    }

    private JarOfTemplates getJarOfTemplates(File file) throws IOException {

        if(mSrcJars==null) {
            mSrcJars = Collections.synchronizedMap(new HashMap<File, JarOfTemplates>());
        }

        //TODO change the value of the map to a weak ref
        JarOfTemplates j = mSrcJars.get(file);

        if(j==null) {
            j = new JarOfTemplates(file);
            mSrcJars.put(file, j);
        }

        return j;
    }

    private void init(File[] rootSourceDirs,
                      File rootDestDir,
                      String encoding) {
        mRootSourceDirs = rootSourceDirs.clone();
        mRootDestDir = rootDestDir;
        mEncoding = encoding;

        rootSourceLoop:for (int i=0; i<rootSourceDirs.length; i++) {

            if(rootSourceDirs[i].isDirectory()) continue rootSourceLoop;

            if( isJarUrl( rootSourceDirs[i] ) ) continue rootSourceLoop;

            throw new IllegalArgumentException
                    ("Source location is not a directory or a jar URL: " +
                     rootSourceDirs[i]);
        }

        if (rootDestDir != null && 
            !rootDestDir.isDirectory()) {
            throw new IllegalArgumentException
                ("Destination is not a directory: " + rootDestDir);
        }

        if (! TemplateRepository.isInitialized())
            TemplateRepository.init(rootDestDir, getRootPackage());
    }

    /**
     * @param force When true, compile all source, even if up-to-date
     */
    public void setForceCompile(boolean force) {
        mForce = force;
    }

    /**
     * Compiles all files in the source directory.
     *
     * @param recurse When true, recursively compiles all files and directories
     *
     * @return The names of all the compiled sources
     */
    public String[] compileAll(boolean recurse) throws IOException {
        return compile(getAllTemplateNames(recurse));
    }

    public String[] compile(String[] names) throws IOException {

        try {
        ArrayList<String> nameList = new ArrayList<String>();
            String[] allNames = getAllTemplateNames();
            Arrays.sort(allNames);
            for (int i = 0; i < names.length; i++) {
                if(Arrays.binarySearch(allNames, names[i]) >= 0) {
                    nameList.add(names[i]);
                }
            }
        return super.compile(nameList.toArray(new String[nameList.size()]));
        } finally {
            if(mSrcJars!=null) for(File f:mSrcJars.keySet()) {
                mSrcJars.remove(f).close();
            }
        }
    }
   
    public String[] getAllTemplateNames() throws IOException {
        return getAllTemplateNames(true);
    }

    public String[] getAllTemplateNames(boolean recurse) throws IOException {
        // Using a Set to prevent duplicate template names.
        Collection<String> sources = new TreeSet<String>();

        for (int i=0; i<mRootSourceDirs.length; i++) {
            if( isJarUrl(mRootSourceDirs[i] ) ) {
                JarOfTemplates j = getJarOfTemplates(mRootSourceDirs[i]);
                gatherJarSources(sources, j);
            } else {
                gatherSources(sources, mRootSourceDirs[i], recurse);
            }
        }

        return sources.toArray(new String[sources.size()]);
    }
    
    public boolean sourceExists(String name) {
        return findRootSourceDir(name) != null || findJarSrc(name)!=null;
    }

    private void gatherJarSources(Collection<String> sources, JarOfTemplates j) 
        throws IOException {

        for(Enumeration<JarEntry> entries = j.getEntries(); entries.hasMoreElements(); ) {
            String name = entries.nextElement().getName();
            if(name.endsWith(".tea")) {
                name = name.substring(0, name.lastIndexOf(".tea"));
                name = name.replace('/', '.');
                if(!sources.contains(name)) sources.add(name);
            }
        }
    }

    /**
     * Gathers all sources (template names) in the source directory.
     *
     * @param templateNames Collection of Strings. The gatherSources method 
     * will add the template names to this Collection.  
     * @param sourceDir the root source directory
     * @param recurse When true, recursively gathers all sources in 
     * sub-directories.
     */
    private void gatherSources(Collection<String> templateNames,
                               File sourceDir, 
                               boolean recurse)
        throws IOException
    {
        gatherSources(templateNames, sourceDir, null, recurse);
    }


    private void gatherSources(Collection<String> toCompile,
                               File sourceDir, 
                               String parentName, 
                               boolean recurse)
        throws IOException
    {
        String[] list = sourceDir.list();
        if (list != null) {
            srcDirLoop:for (int i=0; i<list.length; i++) {
                File file = new File(sourceDir, list[i]);
                
                // ignore hidden files 
                if(file.isHidden()) {
                    continue srcDirLoop;
                }
                
                if (file.isDirectory()) {
                    if (recurse) {
                        String name = file.getName();

                        if (parentName != null) {
                            name = parentName + '.' + name;
                        }

                        gatherSources(toCompile, file, name, recurse);
                    }
                }
                else if (file.getName().endsWith(".tea")) {
                    String name = file.getName();
                    int index = name.lastIndexOf('.');
                    name = name.substring(0, index);
                    
                    if (parentName != null) {
                        name = parentName + '.' + name;
                    }

                    toCompile.add(name);
                }
            }
        }

        return;
    }

    /**
     * Always returns an instance of FileCompiler.Unit. Any errors reported
     * by the compiler that have a reference to a CompilationUnit will have
     * been created by this factory method. Casting this to FileCompiler.Unit
     * allows error reporters to access the source file via the getSourceFile
     * method.
     *
     * @see FileCompiler.Unit#getSourceFile
     */
    protected CompilationUnit createCompilationUnit(String name) {
        File jarFile = findJarSrc(name);
        try {
            if(jarFile!=null) {
                return new JarredUnit(jarFile, name, this);
            }
        } catch (IOException ex) {
            // if error default to normal file loading behavoir
            throw new RuntimeException(ex);
        }
        return new Unit(name, this);
    }

    private File findRootSourceDir(String name) {
        String fileName = name.replace('.', File.separatorChar) + ".tea";

        for (int i=0; i<mRootSourceDirs.length; i++) {
            if(mRootSourceDirs[i].isFile() && mRootSourceDirs[i].getPath().endsWith(".jar")) {
                continue;
            }
            File file = new File(mRootSourceDirs[i], fileName);
            if (file.exists()) {
                return mRootSourceDirs[i];
            }
        }

        return null;
    }

    private File findJarSrc(String name) {
        String fileName = name.replace('.', '/') + ".tea";

        for (File file : mRootSourceDirs) {
            if(isJarUrl(file)) {
                try {
                    JarOfTemplates jot = getJarOfTemplates(file);
                    if(jot.getEntry(fileName)!=null) {
                        return file;
                    }
                } catch(IOException ex) {
                    throw new RuntimeException("opening jar file: "+file, ex);
                }
            }
        }

        return null;
    }

    private boolean isJarUrl(File file) {
        String path = file.toString();
        return (path.startsWith("jar:") && path.endsWith("!"));
    }

    public abstract class FileUnit extends AbstractUnit {
        FileUnit(String name, Compiler compiler) {
            super(name, compiler);
        }
    }
    
    public class Unit extends FileUnit {
        
        private final File mSourceFile;

        Unit(String name, Compiler compiler) {
            super(name, compiler);

            File rootSourceDir = findRootSourceDir(name);
            if (rootSourceDir == null) {
                // File isn't found, but set to a valid directory so that error
                // is produced later when attempting to get a Reader.
                rootSourceDir = mRootSourceDirs[0];
            }

            mSourceFile = new File(rootSourceDir, mSourceFileName);
        }
        
        public File getSourceFile() {
            return mSourceFile;
        }

        @Override
        protected InputStream getTemplateSource(String templateSourceName) 
            throws IOException {
         
            return new FileInputStream(mSourceFile);
        }

        protected long getLastModified() {
            return mSourceFile.lastModified();            
        }
    }
    
/////////////////////////////////
    public class JarredUnit extends FileUnit {

        private JarOfTemplates mJarOfTemplates;
        private URL mUrl;

        public JarredUnit(File file, String name, Compiler compiler) 
            throws IOException {
            
            super(name, compiler);

            mJarOfTemplates = getJarOfTemplates(file);
            mUrl = new URL(mJarOfTemplates.getUrl(), 
                           mJarOfTemplates.getEntry(mSourceFileName).toString());
        }

        @Override
        public String getSourceFileName() {
            return getJarEntry().getName();
        }

        public JarEntry getJarEntry() {
            return mJarOfTemplates.getEntry(mSourceFileName);
        }

        public URL getSourceUrl() {
            return mUrl;
        }
        
        @Override
        protected InputStream getTemplateSource(String templateSourceName) 
            throws IOException {
            
            return mJarOfTemplates.getInputStream(getJarEntry());
        }
        
        protected long getLastModified() {
            if (getJarEntry() == null) {
                return -1;
            }
            
            return getJarEntry().getTime();            
        }

        public void syncTimes() {
            if(mDestFile!=null) {
                mDestFile.setLastModified(getJarEntry().getTime());
            }
        }
    }

    class JarOfTemplates {
        private JarFile mJarFile;
        private JarURLConnection mConn;
        private URL mUrl;

        public JarOfTemplates(File file) throws IOException {
            mUrl = makeJarUrlFromFile(file);
            mConn = (JarURLConnection)mUrl.openConnection();
            mJarFile = mConn.getJarFile();
        }

        public JarEntry getEntry(JarEntry jarEntry) {
            return getEntry(jarEntry.getName());
        }

        public JarEntry getEntry(String name) {
            return mJarFile.getJarEntry(name);
        }

        public Enumeration<JarEntry> getEntries() {
            // TODO cache with weak ref and check url headers for changes
            return mJarFile.entries();
        }

        public InputStream getInputStream(JarEntry jarEntry) throws IOException {
            return mJarFile.getInputStream(jarEntry);
        }

        public URL getUrl() {
            return mUrl;
        }

        public void close() throws IOException {
            if(mJarFile!=null) mJarFile.close();
            mJarFile = null;
            mConn = null;
        }

        URL makeJarUrlFromFile(File path) {
            String urlStr = path.toString();

            urlStr = urlStr.replace("\\", "/");
            if(urlStr.startsWith("jar:file:")) {
                urlStr = urlStr.replaceFirst("/", "///");
            } else {
                if(urlStr.indexOf("//")<0) {
                    urlStr = urlStr.replaceFirst("/", "//");
                }
            }

            if(!urlStr.endsWith("/")) urlStr = urlStr+ "/";

            try {
                return new URL(urlStr);
            } catch(MalformedURLException ex) {
                throw new RuntimeException("not a jar url: "+urlStr, ex);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            this.close();
            super.finalize();
        }


    }
}
