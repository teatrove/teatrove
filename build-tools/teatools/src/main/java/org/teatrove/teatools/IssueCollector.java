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

package org.teatrove.teatools;

import java.util.ArrayList;
import java.util.List;

import org.teatrove.tea.compiler.CompileEvent;
import org.teatrove.tea.compiler.CompileListener;

/**
 * CompileListener that stores the issues reported to it.  
 * 
 * @author Mark Masse
 */
public class IssueCollector implements CompileListener {
    
    protected ArrayList<CompileEvent> mIssues;
    
    public IssueCollector() {
        clearIssues();
    }

    /**
     * Implementation of the ErrorListener interface method.
     */
    public void compileError(CompileEvent error) {            
        mIssues.add(error);
    }
    
    /**
     * Implementation of the ErrorListener interface method.
     */
    public void compileWarning(CompileEvent error) {            
        mIssues.add(error);
    }
    
    /**
     * Retrieves the set of issues that were reported via the compile event
     * methods.  
     *
     * @return a List of CompileEvent objects
     */
    public List<CompileEvent> getIssues() {
        return mIssues;
    }

    /**
     * Retrieves the set of errors that were reported via the compileError
     * method.  
     *
     * @return a List of CompileEvent error objects
     */
    public List<CompileEvent> getErrors() {
        List<CompileEvent> errors = new ArrayList<CompileEvent>();
        for (CompileEvent event : mIssues) {
            if (event.isError()) { errors.add(event); }
        }
        
        return errors;
    }
    
    /**
     * Retrieves the set of warnings that were reported via the compileWarning
     * method.  
     *
     * @return a List of CompileEvent warning objects
     */
    public List<CompileEvent> getWarnings() {
        List<CompileEvent> warnings = new ArrayList<CompileEvent>();
        for (CompileEvent event : mIssues) {
            if (event.isWarning()) { warnings.add(event); }
        }
        
        return warnings;
    }
    
    /**
     * Clears out the current set of errors.  
     */
    public void clearIssues() {
        mIssues = new ArrayList<CompileEvent>();
    }
    
}       
