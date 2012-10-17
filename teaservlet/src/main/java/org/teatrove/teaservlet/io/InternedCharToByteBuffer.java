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

package org.teatrove.teaservlet.io;

import java.io.IOException;

/**
 * A CharToByteBuffer that keeps track of interned strings (mainly string
 * literals) and statically caches the results of those strings after applying
 * a byte conversion. This can improve performance if many of the strings being
 * passed to the append method have been converted before.
 *
 * @author Brian S O'Neill
 * deprecated Moved to org.teatrove.trove.io package.
 */
@Deprecated
public class InternedCharToByteBuffer
    extends org.teatrove.trove.io.InternedCharToByteBuffer
    implements CharToByteBuffer
{
    private static final long serialVersionUID = 1L;

    public InternedCharToByteBuffer(org.teatrove.trove.io.CharToByteBuffer buffer)
        throws IOException
    {
        super(buffer);
    }

    public InternedCharToByteBuffer(CharToByteBuffer buffer)
        throws IOException
    {
        super(buffer);
    }

    public void appendSurrogate(ByteData s) throws IOException {
        super.appendSurrogate(s);
    }

    public void addCaptureBuffer(ByteBuffer buffer) throws IOException {
        super.addCaptureBuffer(buffer);
    }

    public void removeCaptureBuffer(ByteBuffer buffer) throws IOException {
        super.removeCaptureBuffer(buffer);
    }
}
