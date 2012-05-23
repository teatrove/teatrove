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

import java.util.Date;
import java.util.Map;

import org.teatrove.tea.compiler.StatusListener;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.trove.util.ClassInjector;


/**
 * Implementations are responsible for providing compiled templates to an 
 * ApplicationDepot.  The context to compile against must be provided by the 
 * TemplateSourceConfig and a subset of available template sources may be 
 * provided to reduce the number of compilation issues.  Template classes from
 * either a precompiled library or earlier dynamic compilations may also be 
 * provided.
 *
 * @author Jonathan Colwell
 */
public interface TemplateSource {

    public void init(TemplateSourceConfig config) throws Exception;
    
    /**
     * Compile templates recursively
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param all When true, compile all source, even if up-to-date
     */
    public TemplateCompilationResults
        compileTemplates(ClassInjector injector,
                         boolean all) throws Exception;

    /**
     * Compile templates recursively using the specified status listener.
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param all When true, compile all source, even if up-to-date
     * @param listener  The status listener to callback
     */
    public TemplateCompilationResults 
        compileTemplates(ClassInjector injector,
                         boolean all, 
                         StatusListener listener) throws Exception;
    
    /**
     * Compile templates
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param all When true, compile all source, even if up-to-date
     * @param recurse When true, recurse and compile through all
     * subdirectories
     */
    public TemplateCompilationResults
        compileTemplates(ClassInjector injector,
                         boolean all,
                         boolean recurse) throws Exception;

    /**
     * Compile templates
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param all When true, compile all source, even if up-to-date
     * @param recurse When true, recurse and compile through all
     * subdirectories
     * @param listener  The status listener to callback
     */
    public TemplateCompilationResults 
        compileTemplates(ClassInjector injector,
                         boolean all,
                         boolean recurse,
                         StatusListener listener) throws Exception;
    
    /**
     * Compile selected templates
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param selectedTemplates an array of template names to compile
     */
    public TemplateCompilationResults
        compileTemplates(ClassInjector injector,
                         String[] selectedTemplates) throws Exception;


    /**
     * Compile selected templates
     *
     * @param injector Class injector used for template compilation and
     * template loading
     * @param listener  The status listener to callback
     * @param selectedTemplates an array of template names to compile
     */
    public TemplateCompilationResults 
        compileTemplates(ClassInjector injector,
                         StatusListener listener,
                         String[] selectedTemplates) throws Exception;
    
    /**
     *  Returns a Map of templates that need reloading
     *  - the entry key is the String name of the template
     *  - the entry value is a Boolean indicating if the template signature has changed.
     *      TRUE indicates the signature has changed.
     *      FALSE indicates the signature has not changed.
     */
    public Map<String, Boolean> listTouchedTemplates() throws Exception;

    /**
     * Returns number of all templates that are compiled (failed or succeed)
     */
    public int getKnownTemplateCount();

    /**
     * Returns names of all templates that are compiled (failed or succeed)
     */
    public String[] getKnownTemplateNames();

    /**
     * Returns the time the templates are last reloaded
     */
    public Date getTimeOfLastReload();

    public boolean isExceptionGuardianEnabled();

    /**
     * Returns the list of loaded templates
     */
    public Template[] getLoadedTemplates();

    /**
     * Returns the template object of the given template name
     */
    public Template getTemplate(String name)
        throws ClassNotFoundException, NoSuchMethodException;

    /**
     * @return the context source used internally for template compilation.
     */
    public ContextSource getContextSource();

    public TemplateLoader getTemplateLoader();


}

