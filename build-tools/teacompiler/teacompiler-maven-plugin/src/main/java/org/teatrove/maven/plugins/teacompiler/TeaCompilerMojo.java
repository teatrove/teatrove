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

package org.teatrove.maven.plugins.teacompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.teatrove.maven.plugins.teacompiler.contextclassbuilder.DefaultContextClassBuilderHelper;
import org.teatrove.maven.plugins.teacompiler.contextclassbuilder.TeaCompilerExpressionEvaluator;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilder;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilderException;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilderHelper;
import org.teatrove.tea.compiler.CompileEvent;
import org.teatrove.tea.compiler.CompileListener;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.util.FileCompilationProvider;

/**
 * A Maven 2 goal responsible for compiling Tea templates.
 *
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @phase compile
 * @goal compile
 * @requiresDependencyResolution
 * @configurator include-project-dependencies
 * @since Jul 23, 2008 1:41:16 PM
 */
public class TeaCompilerMojo extends AbstractMojo implements Contextualizable {


    /**
     * Path Translator needed by the ExpressionEvaluator
     *
     * @component role="org.apache.maven.project.path.PathTranslator"
     */
    protected PathTranslator translator;

    /**
     * The MavenSession
     *
     * @parameter expression="${session}"
     */
    protected MavenSession session;

    /**
     * POM
     *
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;


    /**
     * The configuration for a custom ContextClassBuilder.
     *
     * @parameter
     */
    private ContextClassBuilder contextClassBuilder;

    /**
     * The class name to use as the Tea context, if not set
     * The contextClassBuilder is used.
     *
     * @parameter
     */
    private String context;

    /**
     * The default directory to use to find Tea templates if none are specified.
     *
     * @parameter default-value="src/main/tea"
     * @readonly
     */
    private File defaultSourceDirectory;

    /**
     * A list of directories where Tea templates are located.
     *
     * @parameter
     */
    private File[] sourceDirectories;

    /**
     * The directory the compiled Tea templates should be written to.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * @parameter default-value="false"
     */
    private boolean force;

    /**
     * The root package the Tea templates should be compiled under.
     *
     * @parameter default-value="org.teatrove.teaservlet.template"
     */
    private String rootPackage;

    /**
     * @parameter
     */
    private String encoding;

    /**
     * @parameter default-value="false"
     */
    private boolean guardian;

    /**
     * @parameter
     */
    private String[] includes;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnError;

    /**
     * @parameter default-value="false"
     */
    private boolean failOnWarning;

    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    public void contextualize(Context context)
            throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }


    public void execute() throws MojoExecutionException, MojoFailureException {

        final Log logger = getLog();

        // Create the Helper for the Context Class Builder
        ContextClassBuilderHelper helper =
                new DefaultContextClassBuilderHelper(
                        session,
                        new TeaCompilerExpressionEvaluator(session, translator, project),
                        logger, container, this.getClass().getClassLoader(), rootPackage
                );

        // Merge the contexts
        final Class<?> contextClass;
        try {
            if(this.context == null) {
                if(contextClassBuilder == null) {
                    throw new MojoExecutionException("Either context or contextClassBuilder parameter is required.");
                }
                contextClass = contextClassBuilder.getContextClass(helper);
            } else {
                contextClass = Class.forName(this.context);
            }
        } catch (ContextClassBuilderException e) {
            throw new MojoExecutionException("Unable to find or create the Context.", e);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Unable to load the Context.", e);
        }

        final File realOutputDirectory = new File(outputDirectory, rootPackage.replace('.', File.separatorChar));

        realOutputDirectory.mkdirs();

        if (sourceDirectories == null || sourceDirectories.length == 0) {
            sourceDirectories = new File[]{defaultSourceDirectory};
        } else {
            // Filter out any that don't exist
            List<File> existing = new ArrayList<File>(sourceDirectories.length);
            for (File sourceDirectory : sourceDirectories) {
                if (sourceDirectory.exists()) {
                    existing.add(sourceDirectory);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Removing source directory because it does not exist. [" + sourceDirectory + "].");
                }
            }
            sourceDirectories = existing.toArray(new File[existing.size()]);
        }

        final Compiler compiler = 
            new Compiler(rootPackage, realOutputDirectory, encoding, 0);
        
        for (File sourceDirectory : sourceDirectories) {
            compiler.addCompilationProvider(
                new FileCompilationProvider(sourceDirectory)
            );
        }

        compiler.setClassLoader(contextClass.getClassLoader());
        compiler.setRuntimeContext(contextClass);
        compiler.setForceCompile(force);
        compiler.addCompileListener(new CompileListener() {
            public void compileError(CompileEvent e) {
                logger.error(e.getDetailedMessage());
            }
            
            public void compileWarning(CompileEvent e) {
                logger.warn(e.getDetailedMessage());
            }
        });
        compiler.setExceptionGuardianEnabled(guardian);

        final String[] names;
        try {
            if (includes == null || includes.length == 0) {
                names = compiler.compileAll(true);
            } else {
                names = compiler.compile(includes);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("I/O error while compiling templates.", e);
        }

        final int errorCount = compiler.getErrorCount();
        if (errorCount > 0) {
            String msg = errorCount + " error" + (errorCount != 1 ? "s" : "");
            if(failOnError) {
                throw new MojoFailureException(msg);
            } else if(logger.isWarnEnabled()){
                logger.warn(msg);
            }
        }
        
        final int warningCount = compiler.getWarningCount();
        if (warningCount > 0) {
            String msg = warningCount + " warning" + (warningCount != 1 ? "s" : "");
            if(failOnWarning) {
                throw new MojoFailureException(msg);
            } else if(logger.isWarnEnabled()){
                logger.warn(msg);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Compiled the following templates:");
            Arrays.sort(names);
            for (String name : names) {
                logger.info(name);
            }
        }
    }

}
