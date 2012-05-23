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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teatrove.tea.compiler.CompilationUnit;

/**
 *
 * @author Jonathan Colwell
 */
public class TemplateCompilationResults implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** True if the templates are being reloaded */
    boolean mReloadInProgress;

    /** The set of successfully reloaded template names */
    Map<String, CompilationUnit> mReloaded;

    /** Error map, where the key is the failed template name, and the
        value is a list of TemplateError objects for that template */
    Map<String, List<TemplateError>> mErrors;

    public TemplateCompilationResults(Map<String, CompilationUnit> reloaded,
                                      Map<String, List<TemplateError>> errors) {
        mErrors = errors;
        mReloaded = reloaded;
    }

    public TemplateCompilationResults() {
        mReloadInProgress = true;
    }

    public boolean appendTemplate(String name, CompilationUnit unit) {
        if (name == null) {
            return false;
        }
        mReloaded.put(name, unit);
        return true;
    }

    public boolean appendTemplates(Map<String, CompilationUnit> names) {
        if (names == null) {
            return false;
        }
        mReloaded.putAll(names);
        return true;
    }

    public boolean appendErrors(Map<String, List<TemplateError>> errors) {
        if (errors == null || errors.isEmpty())
            return false;

        if (mErrors == null)
            mErrors = new Hashtable<String, List<TemplateError>>();

        Iterator<String> keyIterator = errors.keySet().iterator();
        while (keyIterator.hasNext()) {
            String name = keyIterator.next();

            List<TemplateError> templateErrors = mErrors.get(name);

            if (templateErrors == null) {
                templateErrors = new ArrayList<TemplateError>();
                mErrors.put(name, templateErrors);
            }

            List<TemplateError> newErrors = errors.get(name);
            templateErrors.addAll(newErrors);
        }

        return true;
    }

    public boolean appendError(String templateName, TemplateError error) {
        if (templateName == null || error == null) {
            return false;
        }

        List<TemplateError> templateErrors = mErrors.get(templateName);

        if (templateErrors == null) {
            templateErrors = new ArrayList<TemplateError>();
            mErrors.put(templateName, templateErrors);
        }

        return templateErrors.add(error);
    }

    public boolean appendErrors(String templateName,
                                List<TemplateError> errors) {
        if (templateName == null || errors == null || errors.isEmpty())
            return false;

        List<TemplateError> templateErrors = mErrors.get(templateName);
        if (templateErrors == null) {
            templateErrors = new ArrayList<TemplateError>();
            mErrors.put(templateName, templateErrors);
        }

        return templateErrors.addAll(errors);
    }

    public Set<String> getReloadedTemplateNames() {
        return mReloaded.keySet();
    }
    
    public Map<String, CompilationUnit> getReloadedTemplates() {
        return mReloaded;
    }
    
    public CompilationUnit getReloadedTemplate(String templateName) {
        return mReloaded.get(templateName);
    }

    public Map<String, List<TemplateError>> getTemplateErrors() {
        return mErrors;
    }

    public List<TemplateError> getAllTemplateErrors() {
        if (mErrors == null || mErrors.isEmpty())
            return null;

        ArrayList<TemplateError> errors = new ArrayList<TemplateError>();

        Iterator<List<TemplateError>> values = mErrors.values().iterator();
        while (values.hasNext())
            errors.addAll(values.next());

        return errors;
    }

    public boolean isSuccessful() {
        return (mErrors == null || mErrors.size() == 0);
    }

    public void setAlreadyReloading(boolean inProgress) {
        mReloadInProgress = inProgress;
    }

    public boolean isAlreadyReloading() {
        return mReloadInProgress;
    }
}

