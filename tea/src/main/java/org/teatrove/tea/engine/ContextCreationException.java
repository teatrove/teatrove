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

import java.lang.reflect.UndeclaredThrowableException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Intended to wrap any exceptions thrown by createContext when used in the 
 * MergingContextSource so exceptions can pass through the getInstance() call
 * of MergedClass.InstanceFactory.  Calls are passed to the wrapped Exception
 * leaving this class as a transparent wrapper.
 *
 * @author Jonathan Colwell
 */
public class ContextCreationException extends UndeclaredThrowableException {

    private static final long serialVersionUID = 1L;

    public ContextCreationException(Exception e) {
        super(e);
    }

    public ContextCreationException(Exception e, String str) {
        super(e, str);
    }

    // overridden Throwable methods
    public String getMessage() {
        Throwable rock = getUndeclaredThrowable();
        if (rock != null) {
            return rock.getMessage();
        }
        else {
            return super.getMessage();
        }
    }

    // overridden UndeclaredThrowableException methods

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            Throwable rock = getUndeclaredThrowable();
            if (rock != null) {
                rock.printStackTrace(ps);
            }
            else {
                super.printStackTrace(ps);
            }
        }
    }

    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            Throwable rock = getUndeclaredThrowable();
            if (rock != null) {
                rock.printStackTrace(pw);
            }
            else {
                super.printStackTrace(pw);
            }
        }
    }
}
