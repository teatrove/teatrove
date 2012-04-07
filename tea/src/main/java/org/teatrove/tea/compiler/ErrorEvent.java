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

package org.teatrove.tea.compiler;

/**
 * 
 * @author Brian S O'Neill
 */
public class ErrorEvent extends java.util.EventObject {

    private static final long serialVersionUID = 1L;

    private String mErrorMsg;
    private Token mCulprit;
    private SourceInfo mInfo;
    private CompilationUnit mUnit;

    ErrorEvent(Object source, String errorMsg, Token culprit) {
        this(source, errorMsg, culprit, null);
    }

    ErrorEvent(Object source, String errorMsg, SourceInfo info) {
        this(source, errorMsg, info, null);
    }

    ErrorEvent(Object source, String errorMsg, Token culprit,
               CompilationUnit unit) {
        super(source);
        mErrorMsg = errorMsg;
        mCulprit = culprit;
        if (culprit != null) {
            mInfo = culprit.getSourceInfo();
        }
        mUnit = unit;
    }

    ErrorEvent(Object source, String errorMsg, SourceInfo info,
               CompilationUnit unit) {
        super(source);
        mErrorMsg = errorMsg;
        mInfo = info;
        mUnit = unit;
    }

    public String getErrorMessage() {
        return mErrorMsg;
    }

    /**
     * Returns the error message prepended with source file information.
     */
    public String getDetailedErrorMessage() {
        String prepend = getSourceInfoMessage();
        if (prepend == null || prepend.length() == 0) {
            return mErrorMsg;
        }
        else {
            return prepend + ": " + mErrorMsg;
        }
    }

    public String getSourceInfoMessage() {
        String msg;
        if (mUnit == null) {
            if (mInfo == null) {
                msg = "";
            }
            else {
                msg = String.valueOf(mInfo.getLine());
            }
        }
        else {
            if (mInfo == null) {
                msg = mUnit.getName();
            }
            else {
                msg =
                    mUnit.getName() + ':' + mInfo.getLine();
            }
        }

        return msg;
    }

    /**
     * This method reports on where in the source code an error was found.
     *
     * @return Source information on this error or null if not known.
     */
    public SourceInfo getSourceInfo() {
        return mInfo;
    }

    /**
     * @return Null if there was no offending token
     */
    public Token getCulpritToken() {
        return mCulprit;
    }

    /**
     * @return Null if there was no CompilationUnit
     */
    public CompilationUnit getCompilationUnit() {
        return mUnit;
    }
}
