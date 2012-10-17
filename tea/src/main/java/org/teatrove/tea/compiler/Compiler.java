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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.teatrove.tea.parsetree.Template;
import org.teatrove.trove.io.SourceReader;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.StatusEvent;
import org.teatrove.trove.util.StatusListener;

/**
 * The Tea compiler. This class is abstract, and a few concrete
 * implementations can be found in the org.teatrove.tea.util package.
 *
 * <p>A Compiler instance should be used for only one "build" because
 * some information is cached internally like parse trees and error count.
 *
 * @author Brian S O'Neill
 * @see org.teatrove.tea.util.FileCompilationProvider
 * @see org.teatrove.tea.util.ResourceCompilationProvider
 */
public class Compiler {
    
    protected static final String TEMPLATE_PKG = "org.teatrove.tea.templates";
    
    // Maps qualified names to CompilationProviders
    private Map<String, CompilationProvider> mTemplateProviderMap;
    
    // Maps qualified names to ParseTrees.
    private final Map<String, Template> mParseTreeMap;

    // Maps qualified names to CompilationUnits.
    private final Map<String, CompilationUnit> mCompilationUnitMap =
        new HashMap<String, CompilationUnit>();

    // Set of names for CompilationUnits that have already been compiled.
    private final Set<String> mCompiled = new HashSet<String>();

    // List of compilation providers
    private final List<CompilationProvider> mCompilationProviders = 
        new ArrayList<CompilationProvider>();
    
    protected boolean mForce;
    private Set<String> mPreserveTree;

    private Class<?> mContextClass = 
        org.teatrove.tea.runtime.Context.class;
    
    private Method[] mRuntimeMethods;
    private Method[] mStringConverters;

    private CompileListener mCompileListener;

    private Vector<CompileListener> mCompileListeners =
        new Vector<CompileListener>(4);

    private int mErrorCount = 0;
    private int mWarningCount = 0;

    private Vector<StatusListener> mStatusListeners =
        new Vector<StatusListener>();

    protected String mRootPackage;
    protected File mRootDestDir;
    protected ClassInjector mInjector;
    protected String mEncoding;
    protected long mPrecompiledTolerance;
    
    private boolean mGenerateCode = true;
    private boolean mExceptionGuardian = false;

    private ClassLoader mClassLoader;

    private MessageFormatter mFormatter;

    private Set<String> mImports = new HashSet<String>();
    { mImports.add("java.lang"); mImports.add("java.util"); }

    public Compiler() {
        this(ClassInjector.getInstance());
    }
    
    public Compiler(ClassInjector injector) {
        this(injector, TEMPLATE_PKG);
    }
    
    public Compiler(String rootPackage) {
        this(ClassInjector.getInstance(), rootPackage);
    }
    
    public Compiler(ClassInjector injector, String rootPackage) {
        this(ClassInjector.getInstance(), rootPackage, null);
    }
    
    public Compiler(String rootPackage, File rootDestDir) {
        this(ClassInjector.getInstance(), rootPackage, rootDestDir);
    }
    
    public Compiler(ClassInjector injector,
                    String rootPackage, File rootDestDir) {
        this(injector, rootPackage, rootDestDir, "UTF-8", 0);
    }
    
    public Compiler(String rootPackage, File rootDestDir, String encoding,
                    long precompiledTolerance) {
        this(ClassInjector.getInstance(), rootPackage, rootDestDir, 
             encoding, precompiledTolerance);
    }
    
    public Compiler(ClassInjector injector, String rootPackage, 
                    File rootDestDir, String encoding,
                    long precompiledTolerance) {
        this(injector, rootPackage, rootDestDir, encoding, precompiledTolerance,
             Collections.synchronizedMap(new HashMap<String, Template>()));
    }

    /**
     * This constructor allows template signatures to be shared among compiler
     * instances. This is useful in interactive environments, where compilation
     * is occurring on a regular basis, but most called templates are not
     * being modified. The Compiler will map qualified template names to
     * ParseTree objects that have their code removed. Removing a template
     * entry from the map will force the compiler to re-parse the template if
     * it is called. Any template passed into the compile method will always be
     * re-parsed, even if its parse tree is already present in the map.
     *
     * @param rootDestDir The optional directory to write class-files to
     * @param rootPackage The root package to store generate classes to
     * @param parseTreeMap map should be thread-safe
     */
    public Compiler(ClassInjector injector, String rootPackage, 
                    File rootDestDir, String encoding,
                    long precompiledTolerance,
                    Map<String, Template> parseTreeMap) {
        mInjector = injector;
        mRootPackage = rootPackage;
        mRootDestDir = rootDestDir;
        mEncoding = encoding;
        mParseTreeMap = parseTreeMap;
        mPrecompiledTolerance = precompiledTolerance;
        
        mCompileListener = new CompileListener() {
            public void compileError(CompileEvent e) {
                dispatchCompileError(e);
            }
            
            public void compileWarning(CompileEvent e) {
                dispatchCompileWarning(e);
            }
        };
        
        mFormatter = MessageFormatter.lookup(this);
        
        if (!TemplateRepository.isInitialized()) {
            TemplateRepository.init(rootDestDir, rootPackage);
        }
    }
    
    /**
     * Get the root destination directory where class files should be written
     * to. If no root destination directory is provided, <code>null</code> will
     * be returned.
     * 
     * @return The root destination directory or <code>null</code>
     */
    public File getRootDestDir() {
        return mRootDestDir;
    }

    /**
     * Get the root package that all templates should be packaged under.
     * 
     * @return The root package of templates
     */
    public String getRootPackage() {
        return mRootPackage;
    }
    
    /**
     * Get the class injector to write classes to in order to generate and load
     * class structures.
     * 
     * @return The class injector
     */
    public ClassInjector getInjector() {
        return mInjector;
    }
    
    /**
     * Get the encoding of the source files to use.
     * 
     * @return The encoding of the source files to use
     */
    public String getEncoding() {
        return mEncoding;
    }
    
    /**
     * Get the precompiled tolerance to use in detecting last modifications.
     * 
     * @return The precompiled tolerance in milliseconds
     */
    public long getPrecompiledTolerance() {
        return mPrecompiledTolerance;
    }

    /**
     * Add the specified compilation provider as a supported provider for
     * finding and compiling templates.
     * 
     * @param provider  The compilation provider to add
     */
    public void addCompilationProvider(CompilationProvider provider) {
        this.mTemplateProviderMap = null;
        this.mCompilationProviders.add(provider);
    }
    
    /**
     * Add the specified compilation providers as supported providers for
     * finding and compiling templates.
     * 
     * @param provider  The compilation providers to add
     */
    public void addCompilationProviders(Collection<CompilationProvider> providers) {
        this.mTemplateProviderMap = null;
        this.mCompilationProviders.addAll(providers);
    }
    
    /**
     * Add an CompileListener in order receive events of compile-time events.
     * @see org.teatrove.tea.util.ConsoleReporter
     */
    public void addCompileListener(CompileListener listener) {
        mCompileListeners.addElement(listener);
    }

    public void removeCompileListener(CompileListener listener) {
        mCompileListeners.removeElement(listener);
    }

    private void dispatchCompileError(CompileEvent e) {
        mErrorCount++;

        synchronized (mCompileListeners) {
            for (int i = 0; i < mCompileListeners.size(); i++) {
                mCompileListeners.elementAt(i).compileError(e);
            }
        }
    }

    private void dispatchCompileWarning(CompileEvent e) {
        mWarningCount++;

        synchronized (mCompileListeners) {
            for (int i = 0; i < mCompileListeners.size(); i++) {
                mCompileListeners.elementAt(i).compileWarning(e);
            }
        }
    }
    
    /**
     * Add a StatusListener in order to receive events of compilation progress.
     */
    public void addStatusListener(StatusListener listener) {
        mStatusListeners.addElement(listener);
    }

    public void removeStatusListener(StatusListener listener) {
        mStatusListeners.removeElement(listener);
    }

    private void dispatchCompileStatus(StatusEvent e) {
        synchronized (mStatusListeners) {
            for(int i = 0; i < mStatusListeners.size(); i++) {
                mStatusListeners.elementAt(i).statusUpdate(e);
            }
        }
    }

    private void uncaughtException(Throwable e) {
        Thread t = Thread.currentThread();
        t.getThreadGroup().uncaughtException(t, e);
    }

    /**
     * By default, code generation is enabled. Passing false disables the
     * code generation phase of the compiler.
     */
    public void setCodeGenerationEnabled(boolean flag) {
        mGenerateCode = flag;
    }

    /**
     * Returns true if code generation is enabled. The default setting is true.
     */
    public boolean isCodeGenerationEnabled() {
        return mGenerateCode;
    }

    public void setExceptionGuardianEnabled(boolean flag) {
        mExceptionGuardian = flag;
    }

    /**
     * Returns true if the exception guardian is enabled. The default setting
     * is false.
     */
    public boolean isExceptionGuardianEnabled() {
        return mExceptionGuardian;
    }

    /**
     * Sets the ClassLoader to use to load classes with. If set to null,
     * then classes are loaded using Class.forName.
     */
    public void setClassLoader(ClassLoader loader) {
        mClassLoader = loader;
    }

    /**
     * Returns the ClassLoader used by the Compiler, or null if none set.
     */
    public ClassLoader getClassLoader() {
        return mClassLoader;
    }

    /**
     * Loads and returns a class by the fully qualified name given. If a
     * ClassLoader is specified, it is used to load the class. Otherwise,
     * the class is loaded via Class.forName.
     *
     * @see #setClassLoader(ClassLoader)
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        while (true) {
            try {
                if (mClassLoader == null) {
                    return Class.forName(name);
                } else {
                    return mClassLoader.loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                int index = name.lastIndexOf('.');
                if (index < 0) {
                    throw e;
                }

                // Search for inner class.
                name = name.substring(0, index) + '$' +
                        name.substring(index + 1);
            }
        }
    }

    /**
     * After a template is compiled, all but the root node of its parse tree
     * is clipped, in order to save memory. Applications that wish to traverse
     * CompilationUnit parse trees should call this method to preserve them.
     * This method must be called prior to compilation and prior to requesting
     * a parse tree from a CompilationUnit.
     *
     * @param name fully qualified name of template whose parse tree is to be
     * preserved.
     */
    public void preserveParseTree(String name) {
        if (mPreserveTree == null) {
            mPreserveTree = new HashSet<String>();
        }
        mPreserveTree.add(name);
    }

    /**
     * Get the flag of whether to force all templates to be compiled even if
     * up-to-date.
     * 
     * @return true to compile all source, even if up-to-date
     */
    public boolean isForceCompile() {
        return mForce;
    }
    
    /**
     * Set the flag of whether to force all templates to be compiled even if
     * up-to-date.
     * 
     * @param force When true, compile all source, even if up-to-date
     */
    public void setForceCompile(boolean force) {
        mForce = force;
    }
    
    protected void loadTemplates() {
        if (mTemplateProviderMap == null) {
            try { getAllTemplateNames(true); }
            catch (IOException ioe) { 
                mTemplateProviderMap = new HashMap<String, CompilationProvider>();
            }
        }
    }
    
    /**
     * Get the list of all known templates to this compiler.  If the list of
     * known templates cannot be easily resolved, then <code>null</code> should
     * be returned.
     * 
     * @return The list of all known templates.
     */
    public String[] getAllTemplateNames() throws IOException {
        return getAllTemplateNames(true);
    }
    
    /**
     * Get the list of all known templates to this compiler.  If the list of
     * known templates cannot be easily resolved, then <code>null</code> should
     * be returned.
     * 
     * @param recurse The flag of whether to recurse into sub-directories
     * 
     * @return The list of all known templates.
     */
    public String[] getAllTemplateNames(boolean recurse) 
        throws IOException {
        
        if (mTemplateProviderMap == null) {
            synchronized (this) {
                mTemplateProviderMap = new HashMap<String, CompilationProvider>();
                for (CompilationProvider provider : mCompilationProviders) {
                    String[] templates = provider.getKnownTemplateNames(recurse);
                    for (String template : templates) {
                        if (!mTemplateProviderMap.containsKey(template)) {
                            mTemplateProviderMap.put(template, provider);
                        }
                    }
                }
            }
        }
        
        return mTemplateProviderMap.keySet().toArray(
            new String[mTemplateProviderMap.size()]);
    }
    
    /**
     * Recursively compiles all files in the source directory.
     *
     * @return The names of all the compiled sources
     */
    public String[] compileAll() throws IOException {
        return compileAll(true);
    }
    
    /**
     * Compiles all files in the source directory recursively or not.
     *
     * @param recurse The flag of whether to recurse all sub-directories
     * 
     * @return The names of all the compiled sources
     */
    public String[] compileAll(boolean recurse) throws IOException {
        return compile(getAllTemplateNames(recurse));
    }
    
    /**
     * Compile a single compilation unit. This method can be called multiple
     * times, but it will not compile compilation units that have already been
     * compiled.
     *
     * @param name the fully qualified template name
     *
     * @return The names of all the sources compiled by this compiler
     * @exception IOException
     */
    public String[] compile(String name) throws IOException {
        return compile(new String[] {name});
    }

    /**
     * Compile a list of compilation units. This method can be called multiple
     * times, but it will not compile compilation units that have already been
     * compiled.
     *
     * @param names an array of fully qualified template names
     *
     * @return The names of all the sources compiled by this compiler
     * @exception IOException
     */
    public String[] compile(String[] names) throws IOException {
        if (!TemplateRepository.isInitialized()) {
            return compile0(names);
        }
        
        String[] compNames = compile0(names);
        ArrayList<String> compList =
            new ArrayList<String>(Arrays.asList(compNames));

        TemplateRepository rep = TemplateRepository.getInstance();
        String[] callers = rep.getCallersNeedingRecompile(compNames, this);
        if (callers.length > 0)
            compList.addAll(Arrays.asList(compile0(callers)));
        String[] compiled = compList.toArray(new String[compList.size()]);

        // JoshY - There's a VM bug in JVM 1.4.2 that can cause the repository 
        // update to throw a NullPointerException when it shouldn't.  There's a 
        // workaround in place and we also put a catch here, to allow the 
        // TeaServlet init to finish just in case
        try { rep.update(compiled); } catch (Exception e) {
            System.err.println("Unable to update repository");
            e.printStackTrace(System.err);
        }
        
        return compiled;
    }
    
    protected String[] compile0(String[] names) throws IOException {
        synchronized (mParseTreeMap) {
            for (int i=0; i<names.length; i++) {
                if(Thread.interrupted()) {
                    break;
                }
                dispatchCompileStatus(new StatusEvent(this, i, names.length,
                        names[i]));
                CompilationUnit unit = getCompilationUnit(names[i], null);
                if (unit == null) {
                    String msg = mFormatter.format("not.found", names[i]);
                    dispatchCompileError(new CompileEvent(this, 
                        CompileEvent.Type.ERROR, msg, (SourceInfo) null, null));
                } else if (!mCompiled.contains(names[i]) &&
                        unit.shouldCompile()) {
                    mParseTreeMap.remove(names[i]);
                    getParseTree(unit);
                }
            }
        }

        names = new String[mCompiled.size()];
        Iterator<String> it = mCompiled.iterator();
        int i = 0;
        while (it.hasNext()) {
            names[i++] = it.next();
        }

        return names;
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    public int getWarningCount() {
        return mWarningCount;
    }
    
    /**
     * Returns a compilation unit associated with the given name, or null if
     * not found.
     *
     * @param name the requested name
     */
    public CompilationUnit getCompilationUnit(String name) {
        return getCompilationUnit(name, null);
    }
    
    /**
     * Returns a compilation unit associated with the given name, or null if
     * not found.
     *
     * @param name the requested name
     * @param from optional CompilationUnit is passed because requested name
     * should be found relative to it.
     */
    public CompilationUnit getCompilationUnit(String name,
            CompilationUnit from) {
        String fqName = determineQualifiedName(name, from);

        // Templates with source will always override compiled templates
        boolean compiled = false;
        if (fqName == null && (compiled = CompiledTemplate.exists(this, name, from))) {
            fqName = CompiledTemplate.getFullyQualifiedName(name);
        }
        if (fqName == null)
            return null;

        CompilationUnit unit = mCompilationUnitMap.get(fqName);
        if (unit == null) {
            if (!compiled) {
                unit = createCompilationUnit(fqName);
                if (unit != null)
                    mCompilationUnitMap.put(fqName, unit);
            } else {
                unit = new CompiledTemplate(name, this, from);
                // if the CompiledTemplate class was precompiled (is valid) return the unit, otherwise return null to signify 'not found'
                if( ((CompiledTemplate)unit).isValid() ) {
                    mCompilationUnitMap.put(fqName, unit);
                } else {
                    // TODO:  flag the template class for removal
                    unit = null;
                }
            }
        }

        return unit;
    }

    /**
     * Returns the list of imported packages that all templates have. Template
     * parameters can abbreviate the names of all classes in these packages.
     */
    public String[] getImportedPackages() {
        return mImports.toArray(new String[mImports.size()]);
    }

    /**
     * Add an imported package that all templates will have.
     *
     * @param imported  The fully-qualified package name
     */
    public void addImportedPackage(String imported) {
        this.mImports.add(imported);
    }

    /**
     * Add all imported packages that all templates will have.
     *
     * @param imports  The fully-qualified package name
     */
    public void addImportedPackages(String[] imports) {
        if (imports == null) { return; }

        for (String imported : imports) {
            this.mImports.add(imported);
        }
    }

    /**
     * Return a class that defines a template's runtime context. The runtime
     * context contains methods that are callable by templates. A template
     * is compiled such that the first parameter of its execute method must
     * be an instance of the runtime context.
     *
     * <p>Default implementation returns org.teatrove.tea.runtime.Context.</p>
     *
     * @see org.teatrove.tea.runtime.Context
     */
    public Class<?> getRuntimeContext() {
        return mContextClass;
    }

    /**
     * Call to override the default runtime context class that a template is
     * compiled to use.
     *
     * @see org.teatrove.tea.runtime.Context
     */
    public void setRuntimeContext(Class<?> contextClass) {
        mContextClass = contextClass;
        mRuntimeMethods = null;
        mStringConverters = null;
    }

    /**
     * Returns all the methods available in the runtime context.
     */
    public final Method[] getRuntimeContextMethods() {
        if (mRuntimeMethods == null) {
            mRuntimeMethods = getRuntimeContext().getMethods();
        }

        return mRuntimeMethods.clone();
    }

    /**
     * Return the name of a method in the runtime context to bind to for
     * receiving objects emitted by templates. The compiler will bind to the
     * closest matching public method based on the type of its single
     * parameter.
     *
     * <p>Default implementation returns "print".
     */
    public String getRuntimeReceiver() {
        return "print";
    }

    /**
     * Return the name of a method in the runtime context to bind to for
     * converting objects and primitives to strings. The compiler will bind to
     * the closest matching public method based on the type of its single
     * parameter.
     *
     * <p>Default implementation returns "toString". Returning null indicates
     * that a static String.valueOf method should be invoked.
     */
    public String getRuntimeStringConverter() {
        return "toString";
    }

    /**
     * Returns the set of methods that are used to perform conversion to
     * strings. The compiler will bind to the closest matching method based
     * on its parameter type.
     */
    public final Method[] getStringConverterMethods() {
        if (mStringConverters == null) {
            String name = getRuntimeStringConverter();

            Vector<Method> methods = new Vector<Method>();

            if (name != null) {
                Method[] contextMethods = getRuntimeContextMethods();
                for (int i=0; i<contextMethods.length; i++) {
                    Method m = contextMethods[i];
                    if (m.getName().equals(name) &&
                            m.getReturnType() == String.class &&
                            m.getParameterTypes().length == 1) {

                        methods.addElement(m);
                    }
                }
            }

            int customSize = methods.size();

            Method[] stringMethods = String.class.getMethods();
            for (int i=0; i<stringMethods.length; i++) {
                Method m = stringMethods[i];
                if (m.getName().equals("valueOf") &&
                        m.getReturnType() == String.class &&
                        m.getParameterTypes().length == 1 &&
                        Modifier.isStatic(m.getModifiers())) {

                    // Don't add to list if a custom converter already handles
                    // this method's parameter type.
                    Class<?> type = m.getParameterTypes()[0];
                    int j;
                    for (j=0; j<customSize; j++) {
                        Method cm = methods.elementAt(j);
                        if (cm.getParameterTypes()[0] == type) {
                            break;
                        }
                    }

                    if (j == customSize) {
                        methods.addElement(m);
                    }
                }
            }

            mStringConverters = new Method[methods.size()];
            methods.copyInto(mStringConverters);
        }

        return mStringConverters.clone();
    }

    /**
     * Given a name, as requested by the given CompilationUnit, return a
     * fully qualified name or null if the name could not be found.
     *
     * @param name requested name
     * @param from optional CompilationUnit
     */
    private String determineQualifiedName(String name, CompilationUnit from) {
        if (from != null) {
            // Determine qualified name as being relative to "from"

            String fromName = from.getName();
            int index = fromName.lastIndexOf('.');
            if (index >= 0) {
                String qual = fromName.substring(0, index + 1) + name;
                if (sourceExists(qual)) {
                    return qual;
                }
            }
        }

        if (sourceExists(name)) {
            return name;
        }

        return null;
    }

    /**
     * @return true if source exists for the given qualified name
     */
    public boolean sourceExists(String name) {
        loadTemplates();
        
        // check if already known template and delegate to provider
        CompilationProvider provider = mTemplateProviderMap.get(name);
        if (provider != null) {
            return provider.sourceExists(name);
        }
        
        // search for a suitable provider and delegate
        for (CompilationProvider provider2 : mCompilationProviders) {
            if (provider2.sourceExists(name)) {
                mTemplateProviderMap.put(name, provider2);
                return true;
            }
        }
        
        // none found
        return false;
    }

    protected CompilationUnit createCompilationUnit(String name) {
        loadTemplates();
        
        // check if source exists and delegate to provider
        if (sourceExists(name)) {
            CompilationProvider provider = mTemplateProviderMap.get(name);
            if (provider != null) {
                CompilationSource source = provider.createCompilationSource(name);
                return (source == null ? null : 
                    new CompilationUnit(name, source, this));
            }
        }
        
        // none found
        return null;
    }

    /**
     * Default implementation returns a SourceReader that uses "<%" and "%>"
     * as code delimiters.
     */
    protected SourceReader createSourceReader(CompilationUnit unit)
    throws IOException {

        Reader r = new BufferedReader(unit.getReader());
        return new SourceReader(r, "<%", "%>");
    }

    protected Scanner createScanner(SourceReader reader, CompilationUnit unit)
    throws IOException {

        return new Scanner(reader, unit);
    }

    protected Parser createParser(Scanner scanner, CompilationUnit unit)
    throws IOException {

        return new Parser(scanner, unit);
    }

    protected TypeChecker createTypeChecker(CompilationUnit unit) {
        TypeChecker tc = new TypeChecker(unit);
        tc.setClassLoader(getClassLoader());
        tc.setExceptionGuardianEnabled(isExceptionGuardianEnabled());
        return tc;
    }

    /**
     * Default implementation returns a new JavaClassGenerator.
     *
     * @see JavaClassGenerator
     */
    protected CodeGenerator createCodeGenerator(CompilationUnit unit)
    throws IOException {

        return new JavaClassGenerator(unit);
    }

    /**
     * Called by the Compiler or by a CompilationUnit when its parse tree is
     * requested. Requesting a parse tree may cause template code to be
     * generated.
     */
    public Template getParseTree(CompilationUnit unit) {
        synchronized (mParseTreeMap) {
            return getParseTree0(unit);
        }
    }

    private Template getParseTree0(CompilationUnit unit) {
        String name = unit.getName();
        Template tree = mParseTreeMap.get(name);
        if (tree != null) {
            return tree;
        }

        try {
            // Parse and type check the parse tree.

            // Direct all compile events into the CompilationUnit.
            // Remove the unit as an CompileListener in the finally block
            // at the end of this method.
            addCompileListener(unit);

            try {
                Scanner s = createScanner(createSourceReader(unit), unit);
                s.addCompileListener(mCompileListener);
                Parser p = createParser(s, unit);
                p.addCompileListener(mCompileListener);
                tree = p.parse();
                mParseTreeMap.put(name, tree);
                s.close();
            } catch (IOException e) {
                uncaughtException(e);
                String msg = mFormatter.format("read.error", e.toString());
                dispatchCompileError(new CompileEvent(this, 
                    CompileEvent.Type.ERROR, msg, (SourceInfo) null, unit));
                return tree;
            }

            TypeChecker tc = createTypeChecker(unit);
            tc.setClassLoader(getClassLoader());
            tc.addCompileListener(mCompileListener);
            tc.typeCheck();

            if (mCompiled.contains(name) || !unit.shouldCompile()) {
                return tree;
            } else {
                mCompiled.add(name);
            }

            // Code generate the CompilationUnit only if no errors and
            // the code generate option is enabled.

            if (unit.getErrorCount() == 0 && mGenerateCode) {
                OutputStream out = null;
                try {
                    out = unit.getOutputStream();
                    if (out != null) {
                        tree = (Template)new BasicOptimizer(tree).optimize();
                        mParseTreeMap.put(name, tree);

                        CodeGenerator codegen = createCodeGenerator(unit);
                        codegen.addCompileListener(mCompileListener);
                        codegen.writeTo(out);
                        out.flush();
                        out.close();
                        
                        // sync times so class file matches last modified of
                        // source file to ensure times are in sync
                        unit.syncTimes();
                    }
                } catch (Throwable e) {
                    // attempt to close stream
                    // NOTE: we must call this here as well as in the try block
                    //       above rather than solely in a finally block since
                    //       the unit.resetOutputStream expects the stream to
                    //       already be closed.  For example, if the unit uses
                    //       ClassInjector.getStream, then close must be called
                    //       on that stream to ensure it is defined so that
                    //       the reset method can undefine it.
                    if (out != null) {
                        try { out.close(); }
                        catch (Throwable err) { uncaughtException(err); }
                    }

                    // reset the output stream
                    unit.resetOutputStream();

                    // output error
                    uncaughtException(e);
                    String msg = mFormatter.format
                            ("write.error", e.toString());
                    dispatchCompileError(new CompileEvent(this, 
                        CompileEvent.Type.ERROR, msg, (SourceInfo) null, unit));
                    return tree;
                }
            }
        } catch (Throwable e) {
            uncaughtException(e);
            String msg = mFormatter.format("internal.error", e.toString());
            dispatchCompileError(new CompileEvent(this, 
                CompileEvent.Type.ERROR, msg, (SourceInfo) null, unit));
        } finally {
            removeCompileListener(unit);
            // Conserve memory by removing the bulk of the parse tree after
            // compilation. This preserves the signature for templates that
            // may need to call this one.
            if (tree != null &&
                    (mPreserveTree == null || !mPreserveTree.contains(name))) {
                tree.setStatement(null);
            }
        }

        return tree;
    }

}
