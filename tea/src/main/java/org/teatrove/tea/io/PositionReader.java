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

package org.teatrove.tea.io;

import java.io.*;

/**
 * The PositionReader tracks the postion in the stream of the next character 
 * to be read. PositionReaders chain together such that the position is
 * read from the earliest PositionReader in the chain.
 *
 * <p>Position readers automatically close the underlying input stream when
 * the end of file is reached. Ordinary input streams don't do this.
 * 
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class PositionReader extends org.teatrove.trove.io.PositionReader {
    public PositionReader(Reader reader) {
        super(reader);
    }
}
