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

package org.teatrove.teaservlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.StatusListener;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.ReloadLock;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateError;
import org.teatrove.tea.engine.TemplateErrorListener;
import org.teatrove.tea.engine.TemplateSource;
import org.teatrove.tea.engine.TemplateSourceConfig;
import org.teatrove.tea.engine.TemplateSourceImpl;
import org.teatrove.teaservlet.util.RemoteCompiler;
import org.teatrove.teaservlet.util.ServletContextCompiler;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.PropertyMap;

/**
 *
 * @author Jonathan Colwell
 */
public class TeaServletTemplateSource extends TemplateSourceImpl {

    /** Servlet constant for referencing temporary servlet directory. */
    private final static String SERVLET_TMP_DIR = 
        "javax.servlet.context.tempdir";

    /** Package for templates */
    public final static String TEMPLATE_PACKAGE = 
        "org.teatrove.teaservlet.template";
    
    /** Short package for system templates */
    public final static String SYSTEM_PACKAGE = "system";
    
    /** Full package for system templates */
    public final static String SYSTEM_TEMPLATE_PACKAGE = 
        TEMPLATE_PACKAGE + '.' + SYSTEM_PACKAGE;
    
    private boolean mPreloadTemplates;
    //private boolean mRemoteSuccess;
    //private boolean mDelegatedSuccess;
    private String[] mRemoteTemplateURLs;
    private ServletContext mServletContext;
    private String[] mServletTemplatePaths;
    private String mDefaultTemplateName;
    private String mEncoding;
    private long mPrecompiledTolerance;
    //private ClassInjector mInjector;
    private TemplateSource[] mCustomTemplateSources;
    private ReloadLock mReloadLock;
    private static long mTimeout;

    public static TeaServletTemplateSource createTemplateSource(
        ServletContext servletContext, TeaServletContextSource contextSrc,
            PropertyMap properties, Log log) {

        mTimeout = properties.getNumber("server.timeout",
                Long.valueOf(15000)).longValue();
        
        TemplateSourceConfig tsConfig = 
            new TSConfig(contextSrc, properties, log);

        File tmpDir = (File) servletContext.getAttribute(SERVLET_TMP_DIR);
        if (tmpDir == null) {
            log.error("Servlet container does not provide temporary " +
            		  "directory, defaulting to local file system");
            
            tmpDir = new File(".");  
        }
        
        File destDir = TemplateSourceImpl.createTemplateClassesDir
        (
            tmpDir, properties.getString("classes"), log
        );

        TemplateSource[] customTemplateSources =
                createCustomTemplateSources(tsConfig);

        String sourcePathString = properties.getString("path", "/");
        File[] localDirs = null;
        String[] remoteDirs = null;
        String[] servletDirs = null;

        if (sourcePathString != null) {
            StringTokenizer sourcePathTokenizer =
                    new StringTokenizer(sourcePathString, ",;");

            Vector<String> remoteVec = new Vector<String>();
            Vector<File> localVec = new Vector<File>();
            Vector<String> servletVec = new Vector<String>();

            // Sort out the local directories from those using http.
            while (sourcePathTokenizer.hasMoreTokens()) {
                String nextPath = sourcePathTokenizer.nextToken().trim();
                if (nextPath.startsWith("http://")) {
                    remoteVec.add(nextPath);
                } else if (nextPath.startsWith("/")) {
                	servletVec.add(nextPath);
                } else if (nextPath.startsWith("file:")){
                    localVec.add(new File(nextPath.substring(5)));
                } else {
                	throw new IllegalStateException(
                	    "unsupported template path: " + nextPath);
                }
            }

            localDirs = localVec.toArray(new File[localVec.size()]);
            remoteDirs = remoteVec.toArray(new String[remoteVec.size()]);
            servletDirs = servletVec.toArray(new String[servletVec.size()]);
        }

        return new TeaServletTemplateSource(tsConfig, localDirs,
            remoteDirs, servletContext, servletDirs, destDir, 
            customTemplateSources);
    }

    @SuppressWarnings("unchecked")
    private static TemplateSource[] createCustomTemplateSources(
        final TemplateSourceConfig config) {

        final PropertyMap props = config.getProperties().subMap("sources");
        List<TemplateSource> results = new Vector<TemplateSource>();
        Iterator<String> nameIt = props.subMapKeySet().iterator();
        while (nameIt.hasNext()) {
            try {
                final String name = nameIt.next();
                String className = props.getString(name + ".class");
                Class<?> tsClass = 
                        config.getContextSource().getContextType().
                            getClassLoader().loadClass(className);
                
                TemplateSource tsObj = (TemplateSource) tsClass.newInstance();
                tsObj.init(new TemplateSourceConfig() {

                    private Log mLog = new Log(name, config.getLog());
                    private PropertyMap mProps = props.subMap(name + ".init");

                    public PropertyMap getProperties() {
                        return mProps;
                    }

                    public Log getLog() {
                        return mLog;
                    }

                    public ContextSource getContextSource() {
                        return config.getContextSource();
                    }

                    public String getPackagePrefix() {
                        return config.getPackagePrefix();
                    }

                    public boolean isExceptionGuardianEnabled() {
                        return config.isExceptionGuardianEnabled();
                    }
                });
                results.add(tsObj);
            } catch (Exception e) {
                config.getLog().warn(e);
            }
        }

        TemplateSource[] tSrc = 
            results.toArray(new TemplateSource[results.size()]);
        
        if (tSrc == null) {
            config.getLog().debug("null results array");
            tSrc = new TemplateSource[0];
        }
        
        return tSrc;
    }

    private TeaServletTemplateSource(TemplateSourceConfig config, 
    		File[] localTemplateDirs,
    		String[] remoteTemplateURLs,
    		ServletContext servletContext, 
            String[] servletTemplatePaths,
            File compiledTemplateDir,
            TemplateSource[] customSources) {

        super();

        //since I'm not calling init to parse the config, set things up.
        mConfig = config;
        mLog = config.getLog();
        mProperties = config.getProperties();
        mLog.info("initializing the TeaServletTemplateSource.");

        mReloadLock = new ReloadLock();
        setTemplateRootDirs(localTemplateDirs);
        setDestinationDirectory(compiledTemplateDir);

        if (customSources == null) {
            mLog.debug("No custom TemplateSources configured.");
        } else {
            mLog.info(customSources.length + " custom TemplateSources configured.");
        }

        mCustomTemplateSources = customSources;
        mRemoteTemplateURLs = remoteTemplateURLs;
        mServletContext = servletContext;
        mServletTemplatePaths = servletTemplatePaths;
        mDefaultTemplateName = config.getProperties().getString("default");
        mEncoding = 
            config.getProperties().getString("file.encoding", "ISO-8859-1");
        mPreloadTemplates = config.getProperties().getBoolean("preload", true);
        mPrecompiledTolerance = 
            config.getProperties().getInt("precompiled.tolerance", 1000);

        Set<String> imported = new HashSet<String>();
        String imports = config.getProperties().getString("imports", "");
        StringTokenizer tokenizer = new StringTokenizer(imports, ",;");
        while (tokenizer.hasMoreTokens()) {
            imported.add(tokenizer.nextToken().trim());
        }

        setImports(imported.toArray(new String[imported.size()]));

        if (mCompiledDir == null && !mPreloadTemplates) {
            mLog.warn("Now preloading templates.");
            mPreloadTemplates = true;
        }
    }

    @Override
    public int getKnownTemplateCount() {
        int total = super.getKnownTemplateCount();
        for (int j = 0; j < mCustomTemplateSources.length; j++) {
            total += mCustomTemplateSources[j].getKnownTemplateCount();
        }
        return total;
    }

    @Override
    public String[] getKnownTemplateNames() {
        String[] allNames = new String[getKnownTemplateCount()];
        String[] names = super.getKnownTemplateNames();
        System.arraycopy(names, 0, allNames, 0, names.length);
        int pos = names.length;
        for (int j = 0; j < mCustomTemplateSources.length; j++) {
            names = mCustomTemplateSources[j].getKnownTemplateNames();
            System.arraycopy(names, 0, allNames, pos, names.length);
            pos += names.length;
        }

        if (pos < allNames.length) {
            String[] tmp = new String[pos];
            if (pos > 0) {
                System.arraycopy(allNames, 0, tmp, 0, pos);
            }
            allNames = tmp;
        }
        return allNames;
    }

    @Override
    public TemplateCompilationResults compileTemplates(
            ClassInjector commonInjector,
            boolean all, boolean recurse, StatusListener listener)
        throws Exception {

        return compileTemplates(commonInjector, all, recurse, true, listener);
    }

    @Override
    public TemplateCompilationResults compileTemplates(ClassInjector injector,
    	    StatusListener listener, String[] selectedTemplates) 
        throws Exception {

        return compileTemplates(injector, false, false, true, selectedTemplates,
                                listener);
    }

    @Override
    public TemplateCompilationResults checkTemplates(ClassInjector injector,
            boolean all, String[] selectedTemplates)
        throws Exception {
        
        // TODO should we synch with reloading?
        TemplateCompilationResults results = 
            super.checkTemplates(injector, all, selectedTemplates);
        return results;
    }

    @Override
    protected void createCompilers(List<Compiler> compilers,
                                   ClassInjector injector, 
                                   String packagePrefix) {
        // add parent class compilers
        // TODO: how do we determine order of path between different types:
        //       ie: /WEB-INF/templates;http://remote/templates;/WEB-INF/others
        super.createCompilers(compilers, injector, packagePrefix);
        
        // add remote compiler if available
        Compiler rcompiler = createRemoteCompiler(injector, packagePrefix);
        if (rcompiler != null) {
            compilers.add(rcompiler);
        }
        
        // add servlet context compiler if available
        Compiler scompiler = createServletContextCompiler(injector, packagePrefix);
        if (scompiler != null) {
            compilers.add(scompiler);
        }
    }
    
    protected Compiler createRemoteCompiler(ClassInjector injector,
                                            String packagePrefix) {
        if (mRemoteTemplateURLs != null && mRemoteTemplateURLs.length > 0) {
            RemoteCompiler compiler = new RemoteCompiler(
                mRemoteTemplateURLs, packagePrefix, mCompiledDir, injector,
                mEncoding, mTimeout, mPrecompiledTolerance);
            
            return compiler;
        }
        
        return null;
    }
    
    protected Compiler createServletContextCompiler(ClassInjector injector,
                                                    String packagePrefix) {
        if (mServletTemplatePaths != null && mServletTemplatePaths.length > 0) {
            ServletContextCompiler compiler = new ServletContextCompiler(
                mServletContext, mServletTemplatePaths, packagePrefix,
                mCompiledDir, injector, mEncoding, mPrecompiledTolerance);
            
            return compiler;
        }
        
        return null;
    }
    
    private TemplateCompilationResults compileTemplates(
            ClassInjector commonInjector, boolean all, boolean recurse,
            boolean enforceReloadLock, StatusListener listener)
        throws Exception {
        
        return compileTemplates(commonInjector, all, recurse, enforceReloadLock, 
                                null, listener);
    }

    private TemplateCompilationResults compileTemplates(
            ClassInjector commonInjector, boolean all, boolean recurse,
            boolean enforceReloadLock, String[] selectedTemplates,
            StatusListener listener)
        throws Exception {

        synchronized (mReloadLock) {
            if (mReloadLock.isReloading() && enforceReloadLock) {
                return new TemplateCompilationResults();
            } else {
                mReloadLock.setReloading(true);
            }
        }

        mLog.info("Reloading Templates:");

        try {
            if (commonInjector == null) {
                commonInjector = createClassInjector();
            } else {
                mLog.debug("at this point, the injector should still be null since the template source delegation starts here");
            }

            Results results;
            boolean isSelectiveCompile = 
                (null != selectedTemplates && selectedTemplates.length > 0);
            
            if (isSelectiveCompile) {
                results = actuallyCompileTemplates(commonInjector, listener, 
                                                   selectedTemplates);
            } else {
                results = actuallyCompileTemplates(commonInjector, all, recurse, 
                                                   listener);
            }

            for (int j = 0; j < mCustomTemplateSources.length; j++) {
                TemplateCompilationResults delegateResults =
                    mCustomTemplateSources[j].compileTemplates(commonInjector, 
                                                               all, listener);
                
                if (delegateResults.isAlreadyReloading()) {
                    return delegateResults;
                } 
                else {
                    TemplateCompilationResults transients =
                        results.getTransientResults();
                    transients.appendNames(delegateResults.getReloadedTemplateNames());
                    transients.appendErrors(delegateResults.getTemplateErrors());
                }
            }

            // process results

            TemplateCompilationResults templateCompilationResults =
                results.getTransientResults();

            // remove errors from reloaded
            templateCompilationResults.getReloadedTemplateNames()
                .removeAll(templateCompilationResults.getTemplateErrors().keySet());

            String[] succeeded = 
                templateCompilationResults.getReloadedTemplateNames()
                    .toArray(new String[templateCompilationResults.getReloadedTemplateNames().size()]);

            mResults = results;

            if (mPreloadTemplates) {

                try {
                    if (templateCompilationResults.isSuccessful()) {
                        if (isSelectiveCompile) {
                            preloadTemplates(selectedTemplates);
                        } else {
                            preloadTemplates(this);
                        }
                    } else {
                        preloadTemplates(succeeded);
                    }

                    for (int j = 0; j < mCustomTemplateSources.length; j++) {
                        preloadTemplates(mCustomTemplateSources[j]);
                    }

                } catch (Throwable t) {
                    if (all == false) {
                        return compileTemplates(null, true, true, false, null);
                    }
                    mLog.error(t);
                }
            }

            if ( succeeded.length > 0 ) {

                mLog.info(""+ succeeded.length + " templates successfully reloaded:");

                for (int i = 0; i < succeeded.length; i++) {
                    mLog.info("\t" + succeeded[i]);
                }

            } else {
                mLog.info("No templates needed to be reloaded.");

            }

            if ( ! templateCompilationResults.isSuccessful()) {
                List<TemplateError> errors = 
                    templateCompilationResults.getAllTemplateErrors();
                mLog.warn(errors.size() + " errors encountered.");
                Iterator<TemplateError> errorIt = errors.iterator();
                while (errorIt.hasNext()) {
                    TemplateError error = errorIt.next();
                    mLog.warn(error.getDetailedErrorMessage() + " : " + 
                              error.getSourceLine());
                }
            }

            return templateCompilationResults;
        } finally {
            synchronized (mReloadLock) {
                mReloadLock.setReloading(false);
            }
        }
    }

    public String getDefaultTemplateName() {
        return mDefaultTemplateName;
    }

    @Override
    protected TemplateErrorListener createErrorListener() {
        return new RemoteTemplateErrorRetriever();
    }

    private void preloadTemplates(TemplateSource ts)
            throws Throwable {

        String[] knownTemplateNames = ts.getKnownTemplateNames();
        preloadTemplates(knownTemplateNames);
    }

    private void preloadTemplates(String[] selectedTemplates)
            throws Throwable {

        for (int j = 0; j < selectedTemplates.length; j++) {
            getTemplate(selectedTemplates[j]);
        }
    }

    private static class TSConfig implements TemplateSourceConfig {

        private ContextSource mContextSource;
        private PropertyMap mProperties;
        private Log mLog;

        TSConfig(ContextSource contextSource,
                PropertyMap properties, Log log) {
            mContextSource = contextSource;
            mProperties = properties;
            mLog = log;
        }

        public ContextSource getContextSource() {
            return mContextSource;
        }

        public String getPackagePrefix() {
            return TEMPLATE_PACKAGE;
        }

        public boolean isExceptionGuardianEnabled() {
            return mProperties.getBoolean("exception.guardian", false);
        }

        public PropertyMap getProperties() {
            return mProperties;
        }

        public Log getLog() {
            return mLog;
        }
    }

    class RemoteTemplateErrorRetriever extends ErrorRetriever {

        public void compileError(ErrorEvent event) {
            if(! (event.getCompilationUnit() instanceof RemoteCompiler.Unit)) {
                super.compileError(event);
                return;
            }

            mConfig.getLog().warn("Error in " +
                                  event.getDetailedErrorMessage());

            RemoteCompiler.Unit unit = (RemoteCompiler.Unit) event.getCompilationUnit();
            if (unit == null) {
                return;
            }

            String templateName = unit.getName();

            List<TemplateError> errors = mTemplateErrors.get(templateName);
            if (errors == null) {
                errors = new ArrayList<TemplateError>();
                mTemplateErrors.put(templateName, errors);
            }

            String sourcePath = unit.getSourceFileName();

            TemplateError templateError = createTemplateError(sourcePath, event);

            if (templateError != null) {
                errors.add(templateError);
            }

        }
    }
}


