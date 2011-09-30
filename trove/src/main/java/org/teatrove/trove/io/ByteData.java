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

import java.io.OutputStream;
import java.io.IOException;

/**
 * Simple interface for writing a list of bytes to an OutputStream.
 *
 * @author Brian S O'Neill
 */
public interface ByteData {
    /**
     * Return the amount of bytes that will be written by the writeTo method.
     */
    public long getByteCount() throws IOException;

    /**
     * Writes all the bytes to the given OutputStream.
     */
    public void writeTo(OutputStream out) throws IOException;

    /**
     * Reset any transient data stored in this ByteData. A call to getByteCount
     * or writeTo will force this data to be restored.
     */
    public void reset() throws IOException;
}
