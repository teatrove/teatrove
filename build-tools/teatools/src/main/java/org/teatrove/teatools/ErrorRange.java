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

import org.teatrove.tea.compiler.CompileEvent;
import org.teatrove.tea.compiler.SourceInfo;

/**
 * Simple structure for holding a error information. 
 *
 * @author Mark Masse
 */
public class ErrorRange extends Range {

    public String errorMessage;

    public ErrorRange(CompileEvent error) {

        String message = error.getMessage();       
        setErrorMessage(message);      

        SourceInfo sourceInfo = error.getSourceInfo();
        if (sourceInfo != null) {            
            setStart(sourceInfo.getStartPosition());
            setEnd(sourceInfo.getEndPosition()); 
        }

    }

    public ErrorRange(int start, int end, String errorMessage) {
        super(start, end);
        setErrorMessage(errorMessage);      
    }

    /**
     * Gets the error message associated with the error
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message associated with the error
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
