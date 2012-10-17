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

/**
 * ByteBuffer extends ByteData such that bytes can be added to it.
 *
 * @author Brian S O'Neill
 */
public interface ByteBuffer extends ByteData {
    
    /**
     * Clears any bytes added to this buffer.
     */
    public void clear() throws IOException;
    
    /**
     * Returns the base byte count, which excludes surrogates.
     */
    public long getBaseByteCount() throws IOException;

    /**
     * Add one byte to the end of this buffer.
     */
    public void append(byte b) throws IOException;

    /**
     * Copy the given bytes to the end of this buffer.
     */
    public void append(byte[] bytes) throws IOException;

    /**
     * Copy the given bytes to the end of this buffer, starting at the offset,
     * using the length provided.
     */
    public void append(byte[] bytes, int offset, int length)
        throws IOException;

    /**
     * Append ByteData that will not be touched until this ByteBuffer needs
     * to calculate its byte count, or it needs to write out. A null surrogate
     * is not appended.
     */
    public void appendSurrogate(ByteData s) throws IOException;

    /**
     * Add a ByteBuffer that will receive a copy of all the data appended to
     * this ByteBuffer.
     */
    public void addCaptureBuffer(ByteBuffer buffer) throws IOException;

    /**
     * Remove a capture buffer that was previously added by addCaptureBuffer.
     */
    public void removeCaptureBuffer(ByteBuffer buffer) throws IOException;
}
