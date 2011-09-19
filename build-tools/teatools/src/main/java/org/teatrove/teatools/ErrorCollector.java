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

import org.teatrove.tea.compiler.ErrorListener;
import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.SourceInfo;

import java.util.*;

/**
 * ErrorListener that stores the errors reported to it.  
 * 
 * @author Mark Masse
 * @version
 * <!--$$Revision:--> 4 <!-- $-->, <!--$$JustDate:--> 11/16/00 <!-- $-->
 */
public class ErrorCollector implements ErrorListener {
    
    protected ArrayList mErrors;
    
    public ErrorCollector() {
        clearErrors();
    }

    /**
     * Implementation of the ErrorListener interface method.
     */
    public void compileError(ErrorEvent error) {            
        mErrors.add(error);
    }        
    
    /**
     * Retrieves the set of errors that were reported via the compileError
     * method.  
     *
     * @return a List of ErrorEvent objects
     */
    public List getErrors() {
        return mErrors;
    }

    /**
     * Clears out the current set of errors.  
     */
    public void clearErrors() {
        mErrors = new ArrayList();
    }
    
}       



