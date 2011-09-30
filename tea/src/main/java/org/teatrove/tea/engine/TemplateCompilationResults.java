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

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jonathan Colwell
 * @version

 */
public class TemplateCompilationResults implements java.io.Serializable {

    /** True if the templates are being reloaded */
    boolean mReloadInProgress;

    /** The set of successfully reloaded template names */
    Set mReloaded;

    /** Error map, where the key is the failed template name, and the
        value is a list of TemplateError objects for that template */
    Map mErrors;

    public TemplateCompilationResults(Set reloaded,
                                      Map errors) {
        mErrors = errors;
        mReloaded = reloaded;
    }

    public TemplateCompilationResults() {
        mReloadInProgress = true;
    }

    public boolean appendName(String name) {
        if (name == null) {
            return false;
        }
        return mReloaded.add(name);
    }

    public boolean appendNames(Collection names) {
        if (names == null) {
            return false;
        }
        return mReloaded.addAll(names);
    }

    public boolean appendErrors(Map errors) {
        if (errors == null || errors.isEmpty())
            return false;

        if (mErrors == null)
            mErrors = new Hashtable();

        Iterator keyIterator = errors.keySet().iterator();
        while (keyIterator.hasNext()) {
            String name = (String) keyIterator.next();

            ArrayList templateErrors = (ArrayList) mErrors.get(name);

            if (templateErrors == null) {
                templateErrors = new ArrayList();
                mErrors.put(name, templateErrors);
            }

            List newErrors = (List) errors.get(name);

            templateErrors.addAll(newErrors);
        }

        return true;
    }

    public boolean appendError(String templateName, TemplateError error) {
        if (templateName == null || error == null) {
            return false;
        }

        ArrayList templateErrors = (ArrayList) mErrors.get(templateName);

        if (templateErrors == null) {
            templateErrors = new ArrayList();
            mErrors.put(templateName, templateErrors);
        }

        return templateErrors.add(error);
    }

    public boolean appendErrors(String templateName, List errors) {
        if (templateName == null || errors == null || errors.isEmpty())
            return false;

        ArrayList templateErrors = (ArrayList) mErrors.get(templateName);

        if (templateErrors == null) {
            templateErrors = new ArrayList();
            mErrors.put(templateName, templateErrors);
        }

        return templateErrors.addAll(errors);
    }

    public Set getReloadedTemplateNames() {
        return mReloaded;
    }
        
    public Map getTemplateErrors() {
        return mErrors;
    }
        
    public List getAllTemplateErrors() {
        if (mErrors == null || mErrors.isEmpty())
            return null;

        ArrayList errors = new ArrayList();

        Iterator values = mErrors.values().iterator();
        while (values.hasNext())
            errors.addAll((ArrayList) values.next());

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

