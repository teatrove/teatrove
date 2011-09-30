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

package org.teatrove.trove.io;

/**
 * Provides information on where an object (like a token) appeared in the
 * the source file.
 *
 * @author Brian S O'Neill
 */
public class SourceInfo implements Cloneable, java.io.Serializable {
    private int mLine;
    private int mStartPosition;
    private int mEndPosition;

    public SourceInfo(int line, int startPos, int endPos) {
        mLine = line;
        mStartPosition = startPos;
        mEndPosition = endPos;
    }

    /**
     * @return The line in the source file. The first line is one.
     */
    public int getLine() {
        return mLine;
    }
    
    /**
     * @return The character position in the source file where this object 
     * started. The first position of the source file is zero.
     */
    public int getStartPosition() {
        return mStartPosition;
    }

    /**
     * @return The character position in the source file where this object 
     * ended. The first position of the source file is zero.
     */
    public int getEndPosition() {
        return mEndPosition;
    }

    /**
     * @return A character position detailing this object. Usually is the same 
     * as the start position.
     */
    public int getDetailPosition() {
        return mStartPosition;
    }

    private SourceInfo copy() {
        try {
            return (SourceInfo)super.clone();
        }
        catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * @return A clone of this SourceInfo, but with a different end position
     */
    public SourceInfo setEndPosition(int endPos) {
        SourceInfo infoCopy = copy();
        infoCopy.mEndPosition = endPos;
        return infoCopy;
    }

    /**
     * @return A clone of this SourceInfo, but with a different end position
     */
    public SourceInfo setEndPosition(SourceInfo info) {
        return setEndPosition(info.getEndPosition());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(60);

        buf.append("line=");
        buf.append(getLine());
        buf.append(',');
        buf.append("start=");
        buf.append(getStartPosition());
        buf.append(',');
        buf.append("end=");
        buf.append(getEndPosition());
        buf.append(',');
        buf.append("detail=");
        buf.append(getDetailPosition());

        return buf.toString();
    }
}
