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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A ByteBuffer that accepts characters and Strings as well.
 *
 * @author Brian S O'Neill
 */
public interface CharToByteBuffer extends ByteBuffer {
    /**
     * Set the encoding for converting characters to bytes. Calling getEncoding
     * will return the canonical encoding name and may differ from the
     * encoding name provided to this method.
     */
    public void setEncoding(String enc)
        throws IOException, UnsupportedEncodingException;

    /**
     * Returns the current encoding that is being used to convert characters
     * to bytes or null if no encoding has been set yet. The encoding name
     * that is returned is canonical and may differ from the name passed into
     * setEncoding.
     */
    public String getEncoding() throws IOException;

    /**
     * Add one character to the end of this buffer.
     */
    public void append(char c) throws IOException;

    /**
     * Copy the given characters to the end of this buffer.
     */
    public void append(char[] chars) throws IOException;

    /**
     * Copy the given characters to the end of this buffer, starting at the
     * offset, using the length provided.
     */
    public void append(char[] chars, int offset, int length)
        throws IOException;

    /**
     * Copy the given String to the end of this buffer.
     */
    public void append(String str) throws IOException;

    /**
     * Copy the given String to the end of this buffer, starting at the offset,
     * using the length provided.
     */
    public void append(String str, int offset, int length) throws IOException;

    /**
     * Force any buffered characters to be immediately converted to bytes.
     */
    public void drain() throws IOException;
}
