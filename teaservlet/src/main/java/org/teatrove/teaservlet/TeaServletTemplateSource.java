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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.StatusListener;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.ReloadLock;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateError;
import org.teatrove.tea.engine.TemplateSource;
import org.teatrove.tea.engine.TemplateSourceConfig;
import org.teatrove.tea.engine.TemplateSourceImpl;
import org.teatrove.teaservlet.util.RemoteCompilationProvider;
import org.teatrove.teaservlet.util.ServletContextCompilationProvider;
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
    private ServletContext mServletContext;
    private String mDefaultTemplateName;
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

        TeaServletTemplateSource source = new TeaServletTemplateSource
        (
            servletContext, destDir, customTemplateSources
        );
        source.init(tsConfig);
        return source;
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

    private TeaServletTemplateSource(ServletContext servletContext, 
        File compiledTemplateDir, TemplateSource[] customSources) {

        mServletContext = servletContext;
        setDestinationDirectory(compiledTemplateDir);
        mCustomTemplateSources = customSources;
    }
    
    @Override
    public void init(TemplateSourceConfig config) {
        super.init(config);
        
        mLog.info("Initializing the TeaServletTemplateSource.");

        mReloadLock = new ReloadLock();
        mDefaultTemplateName = config.getProperties().getString("default");
        mPreloadTemplates = config.getProperties().getBoolean("preload", true);

        if (mCustomTemplateSources == null) {
            mLog.debug("No custom TemplateSources configured.");
        } else {
            mLog.info(mCustomTemplateSources.length + 
                      " custom TemplateSources configured.");
        }
        
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
            boolean force, String... selectedTemplates)
        throws Exception {
        
        // TODO should we synch with reloading?
        TemplateCompilationResults results = 
            super.checkTemplates(injector, force, selectedTemplates);
        return results;
    }

    @Override
    protected CompilationProvider createProvider(String path) {
        CompilationProvider provider = super.createProvider(path);
        if (provider == null) {
            if (path.startsWith("http:")) {
                provider = new RemoteCompilationProvider(path, mTimeout);
            }
            else if (path.startsWith("web:")) {
                provider = new ServletContextCompilationProvider(
                    mServletContext, path.substring(4));
            }
            else {
                provider = new ServletContextCompilationProvider(
                    mServletContext, path);
            }
        }
        
        return provider;
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
                    transients.appendTemplates(delegateResults.getReloadedTemplates());
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
}


