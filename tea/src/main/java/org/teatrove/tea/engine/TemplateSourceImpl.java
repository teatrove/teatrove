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

package org.teatrove.tea.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.StatusEvent;
import org.teatrove.tea.compiler.StatusListener;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.tea.util.FileCompilationProvider;
import org.teatrove.tea.util.JarCompilationProvider;
import org.teatrove.tea.util.ResourceCompilationProvider;
import org.teatrove.trove.io.LinePositionReader;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.PropertyMap;

/**
 * This class should be created using the {@link TemplateSourceFactory}
 *
 * @author Jonathan Colwell
 */
public class TemplateSourceImpl implements TemplateSource {
    /**
     * converts a path to a File for storing compiled template classes.
     * @return the directory or null if the directory is not there or
     * if it cannot be written to.
     */
    public static File createTemplateClassesDir(File tmpDir, String dirPath, 
                                                Log log) {
        File destDir = null;
        if (dirPath != null && !dirPath.isEmpty()) {
            
            // handle file-based paths
            if (dirPath.startsWith("file:")) {
                destDir = new File(dirPath.substring(5));
            }
            
            // otherwise, assume relative path to tmp dir
            else {
                destDir = new File(tmpDir, dirPath);
                try {
                    if (!destDir.getCanonicalPath().startsWith(
                            tmpDir.getCanonicalPath().concat(File.separator))) {
                        throw new IllegalStateException(
                            "invalid template classes directory: " + dirPath
                        );
                    }
                }
                catch (IOException ioe) {
                    throw new IllegalStateException(
                        "invalid template classes directory: " + dirPath, 
                        ioe
                    );
                }
            }

            if (!destDir.isDirectory()) {
                // try creating it but not the parents.
                if (!destDir.mkdir()) {
                    log.warn("Could not create template classes directory: " +
                             destDir.getAbsolutePath());
                    destDir = null;
                }
            }

            if (destDir != null && !destDir.canWrite()) {
                log.warn("Unable to write to template classes directory: " +
                         destDir.getAbsolutePath());
                destDir = null;
            }
        }

        return destDir;
    }

    public static String[] chopString(String target, String delimiters) {
        if (target != null && target.length() != 0) {
            StringTokenizer st = new StringTokenizer(target, delimiters);
            String[] chopped = new String[st.countTokens()];

            for (int j = 0; st.hasMoreTokens(); j++) {
                chopped[j] = st.nextToken().trim();
            }

            return chopped;
        }

        return null;
    }

    // fields that subclasses might need access to
    protected TemplateSourceConfig mConfig;
    protected Log mLog;
    protected PropertyMap mProperties;
    protected File mCompiledDir;
    
    // fields specific to this implementation
    private ReloadLock mReloading;
    private String[] mImports;
    private String mEncoding;
    private long mPrecompiledTolerance;

    // result fields
    protected Results mResults;

    // compiled template source file info field
    protected Map<String, TemplateSourceFileInfo> mTemplateSourceFileInfo;

    protected boolean mLogCompileStatus = true;

    // no arg constructor for dynamic classloading.
    public TemplateSourceImpl() {
        mReloading = new ReloadLock();
    }

    public void init(TemplateSourceConfig config) {
        mConfig = config;
        mLog = config.getLog();
        mProperties = config.getProperties();
        mLog.info("Initializing Template Source...");

        mImports = parseImports(mProperties);

        if (mCompiledDir == null) {
            mCompiledDir = 
            createTemplateClassesDir(new File("."), 
                                     mProperties.getString("classes"), mLog);
        }
        
        mEncoding = mProperties.getString("file.encoding", "ISO-8859-1");
        mPrecompiledTolerance = 
            mProperties.getInt("precompiled.tolerance", 1000);
    }

    public String[] getImports() {
        return this.mImports;
    }

    public void setImports(String[] imports) {
        this.mImports = imports;
    }

    public TemplateCompilationResults compileTemplates(ClassInjector injector,
                                                       boolean all)
        throws Exception {
        return compileTemplates(injector, all, true, null);
    }
    
    public TemplateCompilationResults compileTemplates(ClassInjector injector,
                                                       boolean all,
                                                       StatusListener listener)
        throws Exception {
        return compileTemplates(injector, all, true, listener);
    }

    public TemplateCompilationResults compileTemplates(ClassInjector injector,
                                                       boolean all,
                                                       boolean recurse)
        throws Exception
    {
        return compileTemplates(injector, all, recurse, null);
    }
    
    public TemplateCompilationResults compileTemplates(ClassInjector injector,
                                                       boolean all,
                                                       boolean recurse,
                                                       StatusListener listener)
        throws Exception
    {
        synchronized(mReloading) {
            if (mReloading.isReloading()) {
                return new TemplateCompilationResults();
            }
            else {
                mReloading.setReloading(true);
            }
        }

        try {
            mResults = actuallyCompileTemplates(injector, all, recurse, listener);

            return mResults.getTransientResults();
        }
        finally {
            synchronized(mReloading) {
                mReloading.setReloading(false);
            }
        }
    }

    public TemplateCompilationResults compileTemplates(ClassInjector injector, 
                                                       String[] selectedTemplates) 
        throws Exception 
    {
        return compileTemplates(injector, null, selectedTemplates);
    }
    
    public TemplateCompilationResults compileTemplates(ClassInjector injector, 
                                                       StatusListener listener,
                                                       String[] selectedTemplates) 
        throws Exception 
    {
        synchronized(mReloading) {
            if (mReloading.isReloading()) {
                return new TemplateCompilationResults();
            }
            else {
                mReloading.setReloading(true);
            }
        }

        try {
            mResults = actuallyCompileTemplates(injector, listener, selectedTemplates);

            return mResults.getTransientResults();
        }
        finally {
            synchronized(mReloading) {
                mReloading.setReloading(false);
            }
        }
    }

    public TemplateCompilationResults checkTemplates(ClassInjector injector,
                                                     boolean force,
                                                     String... selectedTemplates)
        throws Exception {
        
        // setup results of checked templates
        TemplateCompilationResults results = new TemplateCompilationResults
        (
            new TreeMap<String, CompilationUnit>(), 
            new TreeMap<String, List<TemplateError>>()
        );

        // setup package prefix and injector
        String packagePrefix = mConfig.getPackagePrefix();
        if (injector == null) {
            injector = createClassInjector();
        }
        
        // create merged compiler
        Compiler compiler = createCompiler(injector, packagePrefix);

        // create error listener
        TemplateErrorListener errorListener = createErrorListener();
        
        // setup compiler
        compiler.setClassLoader(injector);
        compiler.addImportedPackages(getImports());
        compiler.setRuntimeContext(getContextSource().getContextType());
        compiler.setCodeGenerationEnabled(false);
        compiler.addErrorListener(errorListener);
        compiler.setForceCompile(force);
            
        // setup templates to compile
        String[] templates;
        
        // if no selected templates, compile everything
        if (selectedTemplates == null || selectedTemplates.length == 0) {
            templates = compiler.getAllTemplateNames();
        } 
        
        // ensure selected templates are forcefully recompiled
        else {
            force = true;
            templates = selectedTemplates;
            compiler.setForceCompile(true);
        }

        // compile each selected template
        List<TemplateInfo> callerList = new ArrayList<TemplateInfo>();
        templateLoop: for (int i = 0; i < templates.length; i++) {

            CompilationUnit unit = 
                compiler.getCompilationUnit(templates[i], null);
            
            if (unit == null) {
                mLog.warn("selected template not found: " + templates[i]);
                continue templateLoop;
            }

            if ((force || unit.shouldCompile()) && 
                !results.getReloadedTemplateNames().contains(templates[i])) {

                compiler.getParseTree(unit);
                results.appendTemplate(templates[i], unit);
                callerList.addAll(Arrays.asList(
                    TemplateRepository.getInstance().getCallers(unit.getName())
                ));
            }
        }

        // re-compile callers of selected templates
        compiler.setForceCompile(true);
        Iterator<TemplateInfo> it = callerList.iterator();
        callerLoop: while (it.hasNext()) {
            TemplateInfo tInfo = it.next();
            String caller = tInfo.getShortName().replace('/', '.');
            if (results.getReloadedTemplateNames().contains(caller)) {
                continue callerLoop;
            }

            CompilationUnit callingUnit = 
                compiler.getCompilationUnit(caller, null);
            
            if (callingUnit != null) {
                compiler.getParseTree(callingUnit);
            }
        }

        // append all template errors
        results.appendErrors(errorListener.getTemplateErrors());
        errorListener.close();

        // return results
        return results;
    }

    public ContextSource getContextSource() {
        return mConfig.getContextSource();
    }

    public int getKnownTemplateCount() {
        Set<String> names;

        if (mResults == null ||
            (names = mResults.getKnownTemplateNames()) == null) {
            return 0;
        }

        return names.size();
    }

    public String[] getKnownTemplateNames() {
        Set<String> names;

        if (mResults == null ||
            (names = mResults.getKnownTemplateNames()) == null) {
            return new String[0];
        }

        return names.toArray(new String[names.size()]);
    }

    public Date getTimeOfLastReload() {
        if (mResults == null)
            return null;

        return mResults.getLastReloadTime();
    }

    public boolean isExceptionGuardianEnabled() {
        return mConfig.isExceptionGuardianEnabled();
    }

    public TemplateLoader getTemplateLoader() {
        if (mResults != null)
            return mResults.getLoader();

        return null;
    }

    public Template[] getLoadedTemplates() {
        Map<String, Template> templates;

        if (mResults != null &&
            (templates = mResults.getWrappedTemplates()) != null) {
            return templates.values().toArray(new Template[templates.size()]);
        }

        return new Template[0];
    }

    public Template getTemplate(String name)
        throws ClassNotFoundException, NoSuchMethodException
    {
        Template wrapped = null;

        try {
            Map<String, Template> wrappedTemplates =
                mResults.getWrappedTemplates();
            wrapped = wrappedTemplates.get(name);

            if (wrapped == null) {
                TemplateSourceFileInfo sourceFileInfo = null;
                String sourcePath = null;
                long lastModifiedTime = 0;

                if (mTemplateSourceFileInfo != null) {
                    sourceFileInfo = mTemplateSourceFileInfo.get(name);
                }

                if (sourceFileInfo != null) {
                    sourcePath = sourceFileInfo.getSourcePath();
                    lastModifiedTime = sourceFileInfo.getLastModifiedTime();
                }

                wrapped = new TemplateImpl
                    (getTemplateLoader().getTemplate(name), this,
                        sourcePath, lastModifiedTime);
            }

            wrappedTemplates.put(name, wrapped);
        }
        catch (NullPointerException npe) {
            mLog.debug(npe);

            throw new ClassNotFoundException("TemplateLoader not yet available");
        }

        return wrapped;
    }

    protected Compiler createCompiler(ClassInjector injector, 
                                      String packagePrefix) {
        
        Compiler compiler = new Compiler
        (
            injector, packagePrefix, mCompiledDir, mEncoding, 
            mPrecompiledTolerance
        );
        
        CompilationProvider[] providers = parseProviders(mProperties);
        if (providers != null) {
            for (CompilationProvider provider : providers) {
                compiler.addCompilationProvider(provider);
            }
        }

        return compiler;
    }
    
    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               boolean all)
        throws Exception
    {
        return actuallyCompileTemplates(injector, all, null);
    }
    
    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               boolean all,
                                               StatusListener listener)
        throws Exception
    {
        TemplateErrorListener errorListener = createErrorListener();
        try {
            return actuallyCompileTemplates(injector, all, true, errorListener, listener);
        }
        finally {
            errorListener.close();
        }
    }

    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               boolean all,
                                               boolean recurse)
        throws Exception
    {
        return actuallyCompileTemplates(injector, all, recurse, null);
    }
    
    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               boolean all,
                                               boolean recurse,
                                               StatusListener listener)
        throws Exception
    {
        TemplateErrorListener errorListener = createErrorListener();
        try {
            return actuallyCompileTemplates(injector, all, recurse, errorListener, listener, null);
        }
        finally {
            errorListener.close();
        }
    }

    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               boolean all,
                                               boolean recurse,
                                               TemplateErrorListener errorListener,
                                               StatusListener listener)
        throws Exception
    {
        return actuallyCompileTemplates(injector, all, recurse, errorListener, listener, null);
    }

    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               String[] selectedTemplates)
        throws Exception
    {
        return actuallyCompileTemplates(injector, null, selectedTemplates);
    }
    
    protected Results actuallyCompileTemplates(ClassInjector injector,
                                               StatusListener listener,
                                               String[] selectedTemplates)
        throws Exception
    {
        TemplateErrorListener errorListener = createErrorListener();
        try {
            return actuallyCompileTemplates(injector, false, false, 
                errorListener, listener, selectedTemplates);
        }
        finally {
            errorListener.close();
        }
    }

    /**
     * @param selectedTemplates if not null and not empty, the 'all' and 'recurse' flags are ignored
     */
    private Results actuallyCompileTemplates(ClassInjector injector,
                                             boolean all,
                                             boolean recurse,
                                             TemplateErrorListener errorListener,
                                             StatusListener listener,
                                             String[] selectedTemplates)
        throws Exception
    {
        Map<String, CompilationUnit> reloadedTemplates = 
            new TreeMap<String, CompilationUnit>();

        if (injector == null) {
            injector = createClassInjector();
        }

        Set<String> knownTemplateNames = new TreeSet<String>();
        Date lastReloadTime = new Date();

        Class<?> type = mConfig.getContextSource().getContextType();
        boolean exceptionGuardian = mConfig.isExceptionGuardianEnabled();
        String prefix = mConfig.getPackagePrefix();

        // create and setup merged compiler
        Compiler compiler = createCompiler(injector, prefix);
        compiler.addImportedPackages(getImports());
        compiler.setClassLoader(injector);
        compiler.setRuntimeContext(type);
        compiler.setExceptionGuardianEnabled(exceptionGuardian);
        compiler.addErrorListener(errorListener);
            
        if (listener != null) {
            compiler.addStatusListener(listener);
        }

        if (mLogCompileStatus) {
            compiler.addStatusListener(new CompilerStatusLogger(null));
        }
        
        // get list of all known templates
        knownTemplateNames.addAll(Arrays.asList(compiler.getAllTemplateNames()));

        // either forcefully recompile all known templates or just compile
        // selected templates
        if (null == selectedTemplates || selectedTemplates.length == 0) {
            compiler.setForceCompile(all);
            List<String> results = Arrays.asList(compiler.compileAll(recurse));
            
            knownTemplateNames.addAll(results);
            for (String result : results) {
                reloadedTemplates.put(result, 
                                      compiler.getCompilationUnit(result));
            }
        } 
        else {
            List<String> results = 
                Arrays.asList(compiler.compile(selectedTemplates));
            
            knownTemplateNames.addAll(results);
            for (String result : results) {
                reloadedTemplates.put(result, 
                                      compiler.getCompilationUnit(result));
            }
        }

        // get source file info for all templates
        mTemplateSourceFileInfo = 
            createTemplateSourceFileInfo(compiler, knownTemplateNames); 

        // return results
        return new Results(
            new TemplateCompilationResults(reloadedTemplates, 
                                           errorListener.getTemplateErrors()),
            new TemplateAdapter(type, injector, mConfig.getPackagePrefix()),
            lastReloadTime,
            knownTemplateNames,
            new HashMap<String, Template>()
        );
    }

    /**
     * provides subclasses with access to modify the KnownTemplateNames
     */
    protected Set<String> getKnownTemplateNameSet() {
        return mResults.getKnownTemplateNames();
    }

    /**
     * allows a subclass to set directory to write the compiled templates
     * this directory may be overridden if the ClassInjector passed into the
     * compileTemplates() method points to a different location.
     */
    protected void setDestinationDirectory(File  compiledDir) {
        mCompiledDir = compiledDir;
    }

    /**
     * provides a default class injector using the contextType's ClassLoader
     * as a parent.
     */
    protected ClassInjector createClassInjector() throws Exception {
        return new ResolvingInjector
            (mConfig.getContextSource().getContextType().getClassLoader(),
             new File[] {mCompiledDir},
             mConfig.getPackagePrefix(),
             false);
    }

    protected TemplateErrorListener createErrorListener() {
        return new ErrorRetriever();
    }

    private String[] parseImports(PropertyMap properties) {
        return chopString(properties.getString("imports", ""), ";,");
    }

    private CompilationProvider[] parseProviders(PropertyMap properties) {
        CompilationProvider[] providers = null;
        String[] paths = chopString(properties.getString("path", "/"), ";,");
        if (paths != null) {
            providers = new CompilationProvider[paths.length];
            for (int i = 0; i < paths.length; i++) {
                providers[i] = createProvider(paths[i]);
                if (providers[i] == null) {
                    throw new IllegalStateException(
                        "unsupported compilation provider: " + paths[i]);
                }
            }
        }

        return providers;
    }

    protected CompilationProvider createProvider(String path) {
        // handle file-based paths
        if (path.startsWith("file:")) {
            File file = new File(path.substring(5));
            if (file.isDirectory()) {
                return new FileCompilationProvider(file);
            }
            else if (file.isFile() && file.getPath().endsWith(".jar")) {
                return new JarCompilationProvider(file);
            }
        }
        
        // handle jar-based paths
        else if (path.startsWith("jar:")) {
            File file = new File(path.substring(4));
            if (file.isFile() && file.getPath().endsWith(".jar")) {
                return new JarCompilationProvider(file);
            }
        }
        
        // handle classpath-based paths
        else if (path.startsWith("classpath:")) {
            String rootPackage = path.substring(10);
            return new ResourceCompilationProvider(rootPackage);
        }
        
        // unsupported
        return null;
    }
    
    private Map<String, TemplateSourceFileInfo>
    createTemplateSourceFileInfo(Compiler compiler, Set<String> templateNames) {
        if (templateNames.isEmpty()) {
            return null;
        }

        HashMap<String, TemplateSourceFileInfo> sourceFileInfo =
            new HashMap<String, TemplateSourceFileInfo>();

        Iterator<String> iterator = templateNames.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();

            CompilationUnit unit = compiler.getCompilationUnit(name, null);
            if (unit == null) {
                continue;
            }

            String sourcePath = unit.getSourcePath();
            long lastModifiedTime = unit.getLastModified();

            sourceFileInfo.put(
                name, 
                new TemplateSourceFileInfo(name, sourcePath, lastModifiedTime)
            );
        }

        return sourceFileInfo;
    }

    public Map<String, Boolean> listTouchedTemplates() throws Exception {
        // TemplateRepository tRepo = TemplateRepository.getInstance();

        Map<String, Boolean> touchedTemplateMap =
            new HashMap<String, Boolean>();

        // Set<String> reloadedTemplateNames = new TreeSet<String>();
        
        // setup states
        ClassInjector injector = createClassInjector();
        String prefix = mConfig.getPackagePrefix();
        Class<?> type = mConfig.getContextSource().getContextType();
        boolean exceptionGuardian = mConfig.isExceptionGuardianEnabled();
        
        // create and setup merged compiler
        Compiler compiler = createCompiler(injector, prefix);
        compiler.addImportedPackages(getImports());
        compiler.setClassLoader(injector);
        compiler.setRuntimeContext(type);
        compiler.setExceptionGuardianEnabled(exceptionGuardian);
        compiler.setForceCompile(false);

        // check all known templates
        String[] tNames = compiler.getAllTemplateNames();
        for (int i = 0; i < tNames.length; i++) {
            CompilationUnit unit = compiler.getCompilationUnit(tNames[i], null);
            if (unit.shouldCompile()) {
                Boolean sigChanged = Boolean.valueOf(
                    sourceSignatureChanged(unit.getName(), compiler));
                touchedTemplateMap.put(unit.getName(), sigChanged);
            }
        }

        return touchedTemplateMap;
    }

    /**
     *  parses the tea source file and compares the signature to the signature of the current class file
     *      in the TemplateRepository
     *
     *  @return true if tea source signature is different than the class file signature or class file does not exist
     *
     */
    protected boolean sourceSignatureChanged(String tName, Compiler compiler)
        throws IOException {

        TemplateRepository tRepo = TemplateRepository.getInstance();
        TemplateInfo templateInfo = tRepo.getTemplateInfo(tName);
        if(null == templateInfo) {
            return false;
        }

        CompilationUnit unit = compiler.getCompilationUnit(tName, null);
        if (unit == null) {
            return false;
        }
        
        return ! unit.signatureEquals(tName, templateInfo.getParameterTypes(), templateInfo.getReturnType());
    }

    public boolean isLogCompileStatus() {
        return mLogCompileStatus;
    }

    public void setLogCompileStatus(boolean logCompileStatus) {
        this.mLogCompileStatus = logCompileStatus;
    }

    private class ResolvingInjector extends ClassInjector {

        public ResolvingInjector(ClassLoader cl,
                                 File[] classDirs,
                                 String pkg,
                                 boolean keepByteCode) {
            super(cl, classDirs, pkg, keepByteCode);
        }

        public Class<?> loadClass(String className)
            throws ClassNotFoundException {
            return loadClass(className, true);
        }
    }

    protected class ErrorRetriever implements TemplateErrorListener {
        protected Map<String, List<TemplateError>> mTemplateErrors =
            new Hashtable<String, List<TemplateError>>();

        /** Reads error line from template files */
        private LinePositionReader mOpenReader;

        private CompilationUnit mOpenUnit;

        public Map<String, List<TemplateError>> getTemplateErrors() {
            return mTemplateErrors;
        }

        /**
         * This method is called for each error that occurs while compiling
         * templates. The error is reported in the log and in the error list.
         */
        public void compileError(ErrorEvent event) {
            mConfig.getLog().warn("Error in " +
                                  event.getDetailedErrorMessage());

            CompilationUnit unit = event.getCompilationUnit();
            if (unit == null) {
                return;
            }

            String templateName = unit.getName();

            List<TemplateError> errors = mTemplateErrors.get(templateName);
            if (errors == null) {
                errors = new ArrayList<TemplateError>();
                mTemplateErrors.put(templateName, errors);
            }

            String sourcePath = unit.getSourcePath();

            TemplateError templateError = createTemplateError(sourcePath, event);

            if (templateError != null) {
                errors.add(templateError);
            }
        }

        protected TemplateError createTemplateError(String sourcePath,
                                                  ErrorEvent event) {
            SourceInfo info = event.getSourceInfo();
            if (info == null) {
                return null;
            }

            CompilationUnit unit = event.getCompilationUnit();
            if (unit == null) {
                return null;
            }

            Date lastModifiedDate = new Date(new File(sourcePath).lastModified());

            String errorMessage = event.getErrorMessage();
            String detailedErrorMessage = event.getDetailedErrorMessage();
            String sourceInfoMessage = event.getSourceInfoMessage();

            int lineNumber = -1;
            int errorStartPos = -1;
            int errorEndPos = -1;
            int detailPos = -1;

            int linePos = -1;

            String lineStr = null;
            String underline = null;

            try {
                lineNumber = info.getLine();
                errorStartPos = info.getStartPosition();
                errorEndPos = info.getEndPosition();
                detailPos = info.getDetailPosition();

                if (mOpenReader == null ||
                    mOpenUnit != unit ||
                    mOpenReader.getLineNumber() >= lineNumber) {

                    if (mOpenReader != null) {
                        mOpenReader.close();
                    }

                    mOpenUnit = unit;
                    mOpenReader = new LinePositionReader(new BufferedReader(unit.getReader()));
                }

                mOpenReader.skipForwardToLine(lineNumber);
                linePos = mOpenReader.getNextPosition();

                lineStr = mOpenReader.readLine();
                lineStr = LinePositionReader.cleanWhitespace(lineStr);

                int indentSize = errorStartPos - linePos;
                String indent = LinePositionReader.createSequence(' ', indentSize);

                int markerSize = errorEndPos - errorStartPos + 1;
                String marker = LinePositionReader.createSequence('^', markerSize);
                underline = indent + marker;
            }
            catch (IOException ex) {
                mLog.error(ex);
            }

            return new TemplateError
                (sourcePath, lastModifiedDate,
                 errorMessage, detailedErrorMessage, sourceInfoMessage,
                 lineStr, underline, lineNumber,
                 errorStartPos, errorEndPos,
                 errorStartPos - linePos,
                 errorEndPos - linePos + 1,
                 detailPos - linePos);
        }

        public void close() {
            // Close the template error reader.

            if (mOpenReader != null) {
                try {
                    mOpenReader.close();
                }
                catch (IOException e) {
                    mConfig.getLog().error(e);
                }
            }

            mOpenReader = null;
            mOpenUnit = null;
        }

        protected void finalize() {
            close();
        }
    }

    protected class Results {
        private TemplateCompilationResults mTransient;
        private TemplateLoader mLoader;
        private Date mLastReloadTime;
        private Set<String> mKnownTemplateNames;
        private Map<String, Template> mWrappedTemplates;

        public Results(TemplateCompilationResults transientResults,
                       TemplateLoader loader,
                       Date lastReload,
                       Set<String> known,
                       Map<String, Template> wrapped) {
            mTransient = transientResults;
            mLastReloadTime = lastReload;
            mLoader = loader;
            mKnownTemplateNames = known;
            mWrappedTemplates = wrapped;
        }

        public TemplateCompilationResults getTransientResults() {
            return mTransient;
        }

        public TemplateLoader getLoader() {
            return mLoader;
        }

        public Date getLastReloadTime() {
            return mLastReloadTime;
        }

        public Map<String, Template> getWrappedTemplates() {
            return mWrappedTemplates;
        }

        public Set<String> getKnownTemplateNames() {
            return mKnownTemplateNames;
        }
    }

    protected class TemplateSourceFileInfo {
        /** Template name */
        private String mName;

        /** Template source path */
        private String mSourcePath;

        /** Time the template source file last modified */
        private long mLastModifiedTime;

        public TemplateSourceFileInfo(String name,
                                      String sourcePath,
                                      long lastModifiedTime) {
            mName = name;
            mSourcePath = sourcePath;
            mLastModifiedTime = lastModifiedTime;
        }

        public String getName() {
            return mName;
        }

        public String getSourcePath() {
            return mSourcePath;
        }

        public long getLastModifiedTime() {
            return mLastModifiedTime;
        }
    }

    private class TemplateImpl implements Template {
        private TemplateLoader.Template mTemplate;
        private TemplateSource mSource;
        private String mSourcePath;
        private long mLastModifiedTime;

        protected TemplateImpl(TemplateLoader.Template template,
                               TemplateSource source,
                               String sourcePath,
                               long lastModifiedTime) {
            mTemplate = template;
            mSource = source;
            mSourcePath = sourcePath;
            mLastModifiedTime = lastModifiedTime;
        }

        public TemplateSource getTemplateSource() {
            return mSource;
        }

        public String getSourcePath() {
            return mSourcePath;
        }

        public long getLastModifiedTime() {
            return mLastModifiedTime;
        }

        public TemplateLoader getTemplateLoader() {
            return mTemplate.getTemplateLoader();
        }

        public String getName() {
            return mTemplate.getName();
        }

        public Class<?> getTemplateClass() {
            return mTemplate.getTemplateClass();
        }

        public Class<?> getContextType() {
            return mTemplate.getContextType();
        }

        public String[] getParameterNames() {
            return mTemplate.getParameterNames();
        }

        public Class<?>[] getParameterTypes() {
            return mTemplate.getParameterTypes();
        }

        public void execute(Context context, Object[] parameters)
            throws Exception
        {
            if (context == null) {
                throw new Exception("cannot execute against a  null context");
            }

            mTemplate.execute(context, parameters);
        }
    }

    public class CompilerStatusLogger implements StatusListener {
        @SuppressWarnings("unused")
        private String mSrc;

        public CompilerStatusLogger(String src) {
            mSrc = src;
        }

        @Override
        public void statusUpdate(StatusEvent e) {
            mLog.debug("currently compiling "+e.getCurrentName()+" ("+ (1+e.getCurrent()) + " of " + e.getTotal()+")");
        }
    }

}
