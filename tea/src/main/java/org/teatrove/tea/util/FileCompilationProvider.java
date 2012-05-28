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
import java.util.Collection;
import java.util.TreeSet;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;

/**
 * FileCompilationProvider provides access to tea source files by reading them 
 * from a directory. When given a directory, all files within the directory with 
 * the extension ".tea" will be provided. 
 *
 * @author Brian S O'Neill
 */
public class FileCompilationProvider implements CompilationProvider {
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
    /*
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
            context = org.teatrove.tea.runtime.Context.class;
        }

        if (destDir == null) {
            destDir = rootDir;
        }

        FileCompilationProvider compiler =
            new FileCompilationProvider(rootDir, rootPackage, destDir, null, encoding);

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
        System.out.print(FileCompilationProvider.class.getName());
        System.out.println(" {options} <template root directory> {templates}");
        System.out.println();
        System.out.println("where {options} includes:");
        System.out.println(usageDetail);
    }
    */

    private File mRootSourceDir;
    
    /**
     * Create a compilation provider that loads source files from the given
     * root source directory.
     * 
     * @param rootSourceDir Required root source directory
     */
    public FileCompilationProvider(File rootSourceDir) {
        if (rootSourceDir == null) {
            throw new IllegalArgumentException("rootSourceDir");
        }
        
        mRootSourceDir = rootSourceDir;
    }

    @Override
    public String[] getKnownTemplateNames(boolean recurse) throws IOException {
        Collection<String> sources = new TreeSet<String>();
        gatherSources(sources, mRootSourceDir, recurse);
        return sources.toArray(new String[sources.size()]);
    }

    @Override
    public boolean sourceExists(String name) {
        return getSourceFile(name).exists();
    }

    /**
     * Always returns an instance of FileCompiler.Unit. Any errors reported
     * by the compiler that have a reference to a CompilationUnit will have
     * been created by this factory method. Casting this to FileCompiler.Unit
     * allows error reporters to access the source file via the getSourceFile
     * method.
     *
     * @see FileCompilationProvider.Unit#getSourceFile
     */
    @Override
    public CompilationSource createCompilationSource(String name) {
        File sourceFile = getSourceFile(name);
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }
        
        return new FileSource(name, sourceFile);
    }
    
    /**
     * Get the source file relative to the root source directory for the given
     * template.
     * 
     * @param name The fully-qualified name of the template
     * 
     * @return The file reference to the source file
     */
    protected File getSourceFile(String name) {
        String fileName = name.replace('.', File.separatorChar) + ".tea";

        File file = new File(mRootSourceDir, fileName);
        return file;
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
    protected void gatherSources(Collection<String> templateNames,
                                 File sourceDir, boolean recurse)
        throws IOException {
        gatherSources(templateNames, sourceDir, null, recurse);
    }

    protected void gatherSources(Collection<String> toCompile,
                                 File sourceDir, String parentName, 
                                 boolean recurse)
        throws IOException {
        
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

    public static class FileSource implements CompilationSource {
        
        private final File mSourceFile;

        public FileSource(String name, File sourceFile) {
            mSourceFile = sourceFile;
        }
        
        public File getSourceFile() {
            return mSourceFile;
        }
        
        @Override
        public String getSourcePath() {
            return mSourceFile.getAbsolutePath();
        }

        @Override
        public InputStream getSource() 
            throws IOException {
         
            return new FileInputStream(mSourceFile);
        }

        @Override
        public long getLastModified() {
            return mSourceFile.lastModified();            
        }
    }
}
