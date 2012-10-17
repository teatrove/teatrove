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
public class TemplateIssue implements java.io.Serializable {

    public static final int ERROR = 0;
    public static final int WARNING = 1;
    
    private static final long serialVersionUID = 1L;

    //
    // Private instance members
    //

    /** Source path of the template */
    private String mSourcePath;

    /** Last modified date of the template file */
    private Date mLastModifiedDate;
    
    private int mState;

    /** Short description of the compilation error */
    private String mMessage;

    /** Description of the compilation error that includes template name
        and line number */
    private String mDetailedMessage;

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
    private int mStartPosition;

    /** The character position on the error line where the error ends */
    private int mEndPosition;

    /** The character position on the error line where the detail starts */
    private int mDetailPos;

    /** The character position in the template file where the error starts */
    private int mFileStartPosition;

    /** The character position in the template file where the error ends */
    private int mFileEndPosition;

    public TemplateIssue() {
        mSourcePath = null;
        mLastModifiedDate = null;
        mState = ERROR;
        
        mMessage = "";
        mDetailedMessage = "";
        mSourceInfoMessage = "";

        mSourceLine = "";
        mUnderline = "";
        mLineNumber = 0;

        mFileStartPosition = 0;
        mFileEndPosition = 0;
        mStartPosition = 0;
        mEndPosition = 0;
        mDetailPos = 0;
    }

    public TemplateIssue(String sourcePath,
                         Date lastModifiedDate,
                         int state,
                         String message,
                         String detailedMessage,
                         String sourceInfoMessage,
                         String sourceLine,
                         String underline,
                         int lineNumber,
                         int fileStartPos,
                         int fileEndPos,
                         int startPos,
                         int endPos,
                         int detailPos) {
        mSourcePath = sourcePath;
        mLastModifiedDate = lastModifiedDate;
        mState = state;
        
        mMessage = message;
        mDetailedMessage = detailedMessage;
        mSourceInfoMessage = sourceInfoMessage;

        mSourceLine = sourceLine;
        mUnderline = underline;
        mLineNumber = lineNumber;

        mFileStartPosition = fileStartPos;
        mFileEndPosition = fileEndPos;

        int length = sourceLine.length();
        if ((mStartPosition = startPos) > length)
            mStartPosition = length - 1;
        if ((mEndPosition = endPos) > length)
            mEndPosition = length - 1;
        if ((mDetailPos = detailPos) > length)
            mDetailPos = length - 1;
    }

    public TemplateIssue(int state, String message) {
        mSourcePath = null;
        mLastModifiedDate = null;
        mState = state;

        mMessage = message;
        mDetailedMessage = message;
        mSourceInfoMessage = "custom error: no source involved";

        mSourceLine = "";
        mUnderline = "";
        mLineNumber = 0;

        mFileStartPosition = 0;
        mFileEndPosition = 0;
        mStartPosition = 0;
        mEndPosition = 0;
        mDetailPos = 0;
    }

    public int getState() { return mState; }
    public boolean isError() { return mState == ERROR; }
    public boolean isWarning() { return mState == WARNING; }
    
    public String getSourcePath() {
        return mSourcePath;
    }

    public Date getLastModifiedDate() {
        return mLastModifiedDate;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getDetailedMessage() {
        return mDetailedMessage;
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

    public int getStartPosition() {
        return mStartPosition;
    }

    public int getEndPosition() {
        return mEndPosition;
    }

    public int getFileStartPosition() {
        return mFileStartPosition;
    }

    public int getFileEndPosition() {
        return mFileEndPosition;
    }

    public String getStartOfLine() {
        try {
            return mSourceLine.substring(0, mStartPosition);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getStartOfIssue() {
        try {
            return mSourceLine.substring(mStartPosition, mDetailPos);
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

    public String getEndOfIssue() {
        try {
            return mSourceLine.substring(mDetailPos + 1, mEndPosition + 1);
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public String getEndOfLine() {
        try {
            return mSourceLine.substring
                (mEndPosition + 1, mSourceLine.length());
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}

