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

package org.teatrove.trove.util;

/**
 * Applies the filter pattern to Exceptions.
 *
 * @author Scott Jappinen
 */
public class FilterException extends Exception {

    public FilterException() {
        super();
    }
    
    public FilterException(String message) {
        super(message);
    }
    
    protected FilterException(Throwable rootCause) {
        super(rootCause);
    }

    protected FilterException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public Throwable getRootCause() {
        return super.getCause();
    }
}

