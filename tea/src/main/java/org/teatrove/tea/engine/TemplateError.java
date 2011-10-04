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

/**
 * 
 * @author Reece Wilton
 */
public class TemplateError implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    //
    // Private instance members
    //

    /** Source path of the template */
    private String mSourcePath;

    /** Last modified date of the template file */
    private Date mLastModifiedDate;

    /** Short description of the compilation error */
    private String mErrorMessage;

    /** Description of the compilation error that includes template name
        and line number */
    private String mDetailedErrorMessage;

    /** Description of the compilation error that includes template source
        file information and line number */
    private String mSourceInfoMessage;

    /** The line where the compilation error occured */
    private String mSourceLine;

    /** The line number where the compilation error occured */
    private int mLineNumber;

    /** A line that can be printed below the error line to underline
        the specific location of the error. */
    private String mUnderline;

    /** The character position on the error line where the error starts */
    private int mErrorStartPos;

    /** The character position on the error line where the error ends */
    private int mErrorEndPos;

    /** The character position on the error line where the detail starts */
    private int mDetailPos;

    /** The character position in the template file where the error starts */
    private int mErrorFileStartPos;

    /** The character position in the template file where the error ends */
    private int mErrorFileEndPos;

    public TemplateError() {
        mSourcePath = null;
        mLastModifiedDate = null;

        mErrorMessage = "";
        mDetailedErrorMessage = "";
        mSourceInfoMessage = "";

        mSourceLine = "";
        mUnderline = "";
        mLineNumber = 0;

        mErrorFileStartPos = 0;
        mErrorFileEndPos = 0;
        mErrorStartPos = 0;
        mErrorEndPos = 0;
        mDetailPos = 0;
    }

    public TemplateError(String sourcePath,
                         Date lastModifiedDate,
                         String errorMessage,
                         String detailedErrorMessage,
                         String sourceInfoMessage,
                         String sourceLine,
                         String underline,
                         int lineNumber,
                         int errorFileStartPos,
                         int errorFileEndPos,
                         int errorStartPos,
                         int errorEndPos,
                         int detailPos) {
        mSourcePath = sourcePath;
        mLastModifiedDate = lastModifiedDate;

        mErrorMessage = errorMessage;
        mDetailedErrorMessage = detailedErrorMessage;
        mSourceInfoMessage = sourceInfoMessage;

        mSourceLine = sourceLine;
        mUnderline = underline;
        mLineNumber = lineNumber;

        mErrorFileStartPos = errorFileStartPos;
        mErrorFileEndPos = errorFileEndPos;

        int length = sourceLine.length();
        if ((mErrorStartPos = errorStartPos) > length)
            mErrorStartPos = length - 1;
        if ((mErrorEndPos = errorEndPos) > length)
            mErrorEndPos = length - 1;
        if ((mDetailPos = detailPos) > length)
            mDetailPos = length - 1;
    }

    public TemplateError(String message) {
        mSourcePath = null;
        mLastModifiedDate = null;

        mErrorMessage = message;
        mDetailedErrorMessage = message;
        mSourceInfoMessage = "custom error: no source involved";

        mSourceLine = "";
        mUnderline = "";
        mLineNumber = 0;

        mErrorFileStartPos = 0;
        mErrorFileEndPos = 0;
        mErrorStartPos = 0;
        mErrorEndPos = 0;
        mDetailPos = 0;
    }

    public String getSourcePath() {
        return mSourcePath;
    }

    public Date getLastModifiedDate() {
        return mLastModifiedDate;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public String getDetailedErrorMessage() {
        return mDetailedErrorMessage;
    }

    public String getSourceInfoMessage() {
        return mSourceInfoMessage;
    }

    public String getSourceLine() {
        return mSourceLine;
    }

    public String getUnderline() {
        return mUnderline;
    }

    public int getLineNumber() {
        return mLineNumber;
    }

    public int getErrorStartPos() {
        return mErrorStartPos;
    }

    public int getErrorEndPos() {
        return mErrorEndPos;
    }

    public int getErrorFileStartPos() {
        return mErrorFileStartPos;
    }

    public int getErrorFileEndPos() {
        return mErrorFileEndPos;
    }

    public String getLineStart() {
        try {
            return mSourceLine.substring(0, mErrorStartPos);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getErrorStart() {
        try {
            return mSourceLine.substring(mErrorStartPos, mDetailPos);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getDetail() {
        try {
            return mSourceLine.substring(mDetailPos, mDetailPos + 1);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getErrorEnd() {
        try {
            return mSourceLine.substring(mDetailPos + 1, mErrorEndPos + 1);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getLineEnd() {
        try {
            return mSourceLine.substring
                (mErrorEndPos + 1, mSourceLine.length());
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}

