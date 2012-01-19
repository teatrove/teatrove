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
class StringToken extends Token {
    private static final long serialVersionUID = 1L;

    private String mValue;

    StringToken(int sourceLine,
                int sourceStartPos,
                int sourceEndPos,
                int tokenID,
                String value)
    {
        super(sourceLine, sourceStartPos, sourceEndPos, tokenID);
        mValue = value;
    }

    StringToken(int sourceLine,
                int sourceStartPos,
                int sourceEndPos,
                int sourceDetailPos,
                int tokenID,
                String value)
    {
        super(sourceLine, sourceStartPos, sourceEndPos, sourceDetailPos,
              tokenID);
        mValue = value;
    }

    public String getStringValue() {
        return mValue;
    }
}
