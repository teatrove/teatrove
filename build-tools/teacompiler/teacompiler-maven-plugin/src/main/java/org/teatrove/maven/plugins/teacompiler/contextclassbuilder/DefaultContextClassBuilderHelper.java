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

import org.teatrove.maven.teacompiler.contextclassbuilder.api.ContextClassBuilderHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 25, 2008 2:08:05 PM
 */
public class DefaultContextClassBuilderHelper implements ContextClassBuilderHelper {

    private Log log;

    private ExpressionEvaluator evaluator;

    private PlexusContainer container;


    private ClassLoader projectClassLoader;
    private String rootPackage;


    public DefaultContextClassBuilderHelper(MavenSession session, ExpressionEvaluator evaluator, Log log,
                                            PlexusContainer container, ClassLoader projectClassLoader, String rootPackage) {
        this.log = log;
        this.evaluator = evaluator;

        if (container != null) {
            this.container = container;
        } else {
            this.container = session.getContainer();
        }

        this.projectClassLoader = projectClassLoader;
        this.rootPackage = rootPackage;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public ClassLoader getProjectClassLoader() {
        return projectClassLoader;
    }

    public void setProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
    }

    public String getRootPackage() {
        return rootPackage;
    }

    public void setRootPackage(String rootPackage) {
        this.rootPackage = rootPackage;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultContextClassBuilderHelper that = (DefaultContextClassBuilderHelper) o;

        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        if (projectClassLoader != null ? !projectClassLoader.equals(that.projectClassLoader) : that.projectClassLoader != null)
            return false;
        //noinspection RedundantIfStatement
        if (rootPackage != null ? !rootPackage.equals(that.rootPackage) : that.rootPackage != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (log != null ? log.hashCode() : 0);
        result = 31 * result + (projectClassLoader != null ? projectClassLoader.hashCode() : 0);
        result = 31 * result + (rootPackage != null ? rootPackage.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "DefaultContextClassBuilderHelper{" +
                "log=" + log +
                ", projectClassLoader=" + projectClassLoader +
                ", rootPackage='" + rootPackage + '\'' +
                '}';
    }


    /*
    * (non-Javadoc)
    *
    * @see org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper#getRuntimeInformation()
    */
    /**
     * Gets the component.
     *
     * @param clazz the clazz
     * @return the component
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *          the component lookup exception
     */
    public Object getComponent(Class clazz) throws ComponentLookupException {
        return getComponent(clazz.getName());
    }

    /**
     * Gets the component.
     *
     * @param componentKey the component key
     * @return the component
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *          the component lookup exception
     */
    public Object getComponent(String componentKey) throws ComponentLookupException {
        return container.lookup(componentKey);
    }

    /**
     * Gets the component.
     *
     * @param role     the role
     * @param roleHint the role hint
     * @return the component
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *          the component lookup exception
     */
    public Object getComponent(String role, String roleHint) throws ComponentLookupException {
        return container.lookup(role, roleHint);
    }

    /**
     * Gets the component map.
     *
     * @param role the role
     * @return the component map
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *          the component lookup exception
     */
    public Map getComponentMap(String role) throws ComponentLookupException {
        return container.lookupMap(role);
    }

    /**
     * Gets the component list.
     *
     * @param role the role
     * @return the component list
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *          the component lookup exception
     */
    public List getComponentList(String role) throws ComponentLookupException {
        return container.lookupList(role);
    }

    /**
     * Gets the container.
     *
     * @return the container
     */
    public PlexusContainer getContainer() {
        return container;
    }

    /**
     * Evaluate an expression.
     *
     * @param expression the expression
     * @return the value of the expression
     */
    public Object evaluate(String expression) throws ExpressionEvaluationException {
        return evaluator.evaluate(expression);
    }

    /**
     * Align a given path to the base directory that can be evaluated by this expression evaluator, if known.
     *
     * @param file the file
     * @return the aligned file
     */
    public File alignToBaseDirectory(File file) {
        return evaluator.alignToBaseDirectory(file);
    }
}
