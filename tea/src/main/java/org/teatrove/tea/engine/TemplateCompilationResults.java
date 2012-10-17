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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teatrove.tea.compiler.CompilationUnit;

/**
 * This contains the results of compiling templates including the templates that
 * were compiled and any errors/warnings for those that failed compilation.
 * 
 * @author Jonathan Colwell
 */
public class TemplateCompilationResults implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** True if the templates are being reloaded */
    boolean mReloadInProgress;

    /** The set of successfully reloaded template names */
    Set<String> mReloaded;
    
    /** 
     * The set of reloaded template units.
     * Note that this is transient so we do not transfer across the wire to
     * remote peers in the cluster.
     */
    transient Map<String, CompilationUnit> mReloadedUnits;

    /** Error map, where the key is the failed template name, and the
        value is a list of TemplateIssue objects for that template */
    Map<String, List<TemplateIssue>> mIssues;

    public TemplateCompilationResults(Set<String> reloaded,
                                      Map<String, List<TemplateIssue>> issues) {
        mIssues = issues;
        mReloaded = reloaded;
    }
    
    public TemplateCompilationResults(Map<String, CompilationUnit> reloaded,
                                      Map<String, List<TemplateIssue>> issues) {
        mIssues = issues;
        mReloadedUnits = reloaded;
        mReloaded = new HashSet<String>(reloaded.keySet());
    }

    public TemplateCompilationResults() {
        mReloadInProgress = true;
    }

    public boolean appendTemplate(String name) {
        if (name == null) {
            return false;
        }
        
        mReloaded.add(name);
        return true;
    }
    
    public boolean appendTemplate(String name, CompilationUnit unit) {
        if (name == null) {
            return false;
        }
        
        mReloaded.add(name);
        
        if (mReloadedUnits != null) {
            mReloadedUnits.put(name, unit);
        }
        
        return true;
    }

    public boolean appendTemplates(Set<String> names) {
        if (names == null) {
            return false;
        }

        mReloaded.addAll(names);
        return true;
    }
    
    public boolean appendTemplates(Map<String, CompilationUnit> names) {
        if (names == null) {
            return false;
        }
        
        mReloaded.addAll(names.keySet());
        
        if (mReloadedUnits != null) {
            mReloadedUnits.putAll(names);
        }
        
        return true;
    }

    public boolean appendIssues(Map<String, List<TemplateIssue>> issues) {
        if (issues == null || issues.isEmpty()) {
            return false;
        }

        if (mIssues == null) {
            mIssues = new Hashtable<String, List<TemplateIssue>>();
        }

        Iterator<String> keyIterator = issues.keySet().iterator();
        while (keyIterator.hasNext()) {
            String name = keyIterator.next();

            List<TemplateIssue> templateIssues = mIssues.get(name);

            if (templateIssues == null) {
                templateIssues = new ArrayList<TemplateIssue>();
                mIssues.put(name, templateIssues);
            }

            List<TemplateIssue> newErrors = issues.get(name);
            templateIssues.addAll(newErrors);
        }

        return true;
    }

    public boolean appendIssue(String templateName, TemplateIssue issue) {
        if (templateName == null || issue == null) {
            return false;
        }

        List<TemplateIssue> templateIssues = mIssues.get(templateName);

        if (templateIssues == null) {
            templateIssues = new ArrayList<TemplateIssue>();
            mIssues.put(templateName, templateIssues);
        }

        return templateIssues.add(issue);
    }

    public boolean appendIssues(String templateName,
                                List<TemplateIssue> issues) {
        if (templateName == null || issues == null || issues.isEmpty()) {
            return false;
        }

        List<TemplateIssue> templateIssues = mIssues.get(templateName);
        if (templateIssues == null) {
            templateIssues = new ArrayList<TemplateIssue>();
            mIssues.put(templateName, templateIssues);
        }

        return templateIssues.addAll(issues);
    }

    public Set<String> getReloadedTemplateNames() {
        return mReloaded;
    }
    
    public Map<String, CompilationUnit> getReloadedTemplates() {
        return mReloadedUnits;
    }
    
    public CompilationUnit getReloadedTemplate(String templateName) {
        if (mReloadedUnits == null) {
            return null;
        }
        
        return mReloadedUnits.get(templateName);
    }

    public Map<String, List<TemplateIssue>> getTemplateIssues() {
        return mIssues;
    }
    
    public Map<String, List<TemplateIssue>> getTemplateErrors() {
        Map<String, List<TemplateIssue>> errors =
            new HashMap<String, List<TemplateIssue>>();
        
        for (Map.Entry<String, List<TemplateIssue>> entry : mIssues.entrySet()) {
            List<TemplateIssue> list = new ArrayList<TemplateIssue>();
            for (TemplateIssue issue : entry.getValue()) {
                if (issue.isError()) { list.add(issue); }
            }
            
            if (!list.isEmpty()) {
                errors.put(entry.getKey(), list);
            }
        }
        
        return errors;
    }
    
    public Map<String, List<TemplateIssue>> getTemplateWarnings() {
        Map<String, List<TemplateIssue>> warnings =
            new HashMap<String, List<TemplateIssue>>();
        
        for (Map.Entry<String, List<TemplateIssue>> entry : mIssues.entrySet()) {
            List<TemplateIssue> list = new ArrayList<TemplateIssue>();
            for (TemplateIssue issue : entry.getValue()) {
                if (issue.isWarning()) { list.add(issue); }
            }
            
            if (!list.isEmpty()) {
                warnings.put(entry.getKey(), list);
            }
        }
        
        return warnings;
    }

    public List<TemplateIssue> getAllTemplateIssues() {
        if (mIssues == null || mIssues.isEmpty())
            return null;

        ArrayList<TemplateIssue> errors = new ArrayList<TemplateIssue>();

        Iterator<List<TemplateIssue>> values = mIssues.values().iterator();
        while (values.hasNext())
            errors.addAll(values.next());

        return errors;
    }

    public List<TemplateIssue> getAllTemplateErrors() {
        if (mIssues == null || mIssues.isEmpty())
            return null;

        List<TemplateIssue> errors = new ArrayList<TemplateIssue>();
        Iterator<List<TemplateIssue>> values = mIssues.values().iterator();
        while (values.hasNext()) {
            for (TemplateIssue issue : values.next()) {
                if (issue.isError()) {
                    errors.add(issue);
                }
            }
        }

        return errors;
    }
    
    public List<TemplateIssue> getAllTemplateWarnings() {
        if (mIssues == null || mIssues.isEmpty())
            return null;

        List<TemplateIssue> warnings = new ArrayList<TemplateIssue>();
        Iterator<List<TemplateIssue>> values = mIssues.values().iterator();
        while (values.hasNext()) {
            for (TemplateIssue issue : values.next()) {
                if (issue.isWarning()) {
                    warnings.add(issue);
                }
            }
        }

        return warnings;
    }
    
    public boolean isSuccessful() {
        for (List<TemplateIssue> issues : mIssues.values()) {
            for (TemplateIssue issue : issues) {
                if (issue.isError()) { return false; }
            }
        }
        
        return true;
    }
   
    public void setAlreadyReloading(boolean inProgress) {
        mReloadInProgress = inProgress;
    }

    public boolean isAlreadyReloading() {
        return mReloadInProgress;
    }
}

