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

package org.teatrove.maven.plugins.teacompiler.contextclassbuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilder;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilderException;
import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilderHelper;
import org.teatrove.tea.runtime.Context;
import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.io.DualOutput;
import org.teatrove.trove.util.ClassInjector;

/**
 * Merges a list of interfaces and outputs the merged interface to the project's output directory
 * for inclusion in the project's final artifact.
 *
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 24, 2008 3:45:51 PM
 */
public class MergedContextClassBuilder implements ContextClassBuilder {

    private String mergedClassName = null;
    private List<String> contexts = new ArrayList<String>();

    public Class<Context> getContextClass(final ContextClassBuilderHelper helper) throws ContextClassBuilderException {
        return getMergedContextClass(contexts, helper);
    }
    
    private Class<Context> getMergedContextClass(final List<String> contexts, final ContextClassBuilderHelper helper) throws ContextClassBuilderException {

        final String mergedClassName = this.mergedClassName != null ? this.mergedClassName : buildClassName(helper, contexts);

        // Figure out location for class
        final File mergedClassLocation = findMergedInterfaceFile(mergedClassName, helper);

        // The standard Context should be always be available
        contexts.add(0, "org.teatrove.tea.runtime.Context");

        List<Class<?>> contextClasses = new ArrayList<Class<?>>(contexts.size());
        for (String context : contexts) {
            try {
                contextClasses.add(Class.forName(context, false, helper.getProjectClassLoader()));
                if (helper.getLog().isDebugEnabled()) {
                    helper.getLog().debug("Loaded: " + context);
                }
            } catch (ClassNotFoundException e) {
                throw new ContextClassBuilderException("Unable to load template context class: '" + context + "'.  classLoader = " + Arrays.asList(((URLClassLoader) helper.getProjectClassLoader()).getURLs()), e);
            }
        }

        final ClassInjector injector = new ClassInjector(helper.getProjectClassLoader(), (File) null, helper.getRootPackage());

        final ClassFile classFile = createMergedInterface(mergedClassName, contextClasses);

        writeMergedInterface(mergedClassLocation, classFile, injector);
        
        return loadMergedInterface(injector, mergedClassName);
    }

    private File findMergedInterfaceFile(String mergedClassName, ContextClassBuilderHelper helper) throws ContextClassBuilderException {
        final String mergedClassPathExpression = "${project.build.outputDirectory}/" + mergedClassName.replaceAll("[.]", "/") + ".class";
        final File mergedClassLocation;
        try {
            mergedClassLocation = new File((String) helper.evaluate(mergedClassPathExpression));
        } catch (ExpressionEvaluationException e) {
            throw new ContextClassBuilderException("There was a problem evaluting: " + mergedClassPathExpression, e);
        }
        return mergedClassLocation;
    }

    @SuppressWarnings("unchecked")
    private Class<Context> loadMergedInterface(ClassInjector injector, String mergedClassName) throws ContextClassBuilderException {
        try {
            //noinspection unchecked
            return (Class<Context>) injector.loadClass(mergedClassName);
        } catch (ClassNotFoundException e) {
            throw new ContextClassBuilderException("The class '" + mergedClassName + "' cannot be found.", e);
        }
    }

    private void writeMergedInterface(File mergedClassLocation, ClassFile classFile, ClassInjector injector) throws ContextClassBuilderException {
        // Write Merged interface to project build output directory so it can be package with jar and loaded by runtime.
        BufferedOutputStream out = null;
        try {
            mergedClassLocation.mkdirs();
            if (mergedClassLocation.exists()) {
                if (!mergedClassLocation.delete()) {
                    throw new ContextClassBuilderException("The file '" + mergedClassLocation + "' was not able to be deleted.");
                }
            }
            mergedClassLocation.createNewFile();
            out = new BufferedOutputStream(new DualOutput(
                new FileOutputStream(mergedClassLocation), 
                injector.getStream(classFile.getClassName()))
            );
            classFile.writeTo(out);
            out.flush();
        } catch (FileNotFoundException e) {
            throw new ContextClassBuilderException("Unable to find file: " + mergedClassLocation, e);
        } catch (IOException e) {
            throw new ContextClassBuilderException("I/O error writing to file: " + mergedClassLocation, e);
        } finally {
            if (out != null) {
                //noinspection EmptyCatchBlock
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private ClassFile createMergedInterface(String mergedClassName, 
                                            List<Class<?>> contextClasses) {
        final ClassFile classFile = new ClassFile(mergedClassName);

        // For the runtime to be able to adapt to this class, make it an interface
        Set<Method> methods = new HashSet<Method>();
        classFile.getModifiers().setInterface(true);
        for (Class<?> contextClass : contextClasses) {
            if (contextClass.isInterface()) {
                classFile.addInterface(contextClass);
            }
            else {
                for (Method method : contextClass.getMethods()) {
                    if (!methods.contains(method) && 
                        !method.getDeclaringClass().equals(Object.class)) {
                        methods.add(method);
                        classFile.addMethod(method);
                    }
                }
            }
        }
        return classFile;
    }

    private static String buildClassName(ContextClassBuilderHelper helper, List<String> classNames) {
        int hashCode = 17;
        for (String className : classNames) {
            hashCode = hashCode * 31 + className.hashCode();
        }
        return helper.getRootPackage() + ".MergedClass$" + (hashCode & 0xffffffffL);
    }

}
