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
public class SourceDetailedInfo extends SourceInfo {
    private static final long serialVersionUID = 1L;

    private int mDetail;

    public SourceDetailedInfo(int line,
                              int startPos,
                              int endPos,
                              int detailPos) {
        super(line, startPos, endPos);

        mDetail = detailPos;
    }

    public SourceDetailedInfo(SourceInfo info, int detailPos) {
        super(info.getLine(), info.getStartPosition(), info.getEndPosition());

        mDetail = detailPos;
    }

    public int getDetailPosition() {
        return mDetail;
    }
}
