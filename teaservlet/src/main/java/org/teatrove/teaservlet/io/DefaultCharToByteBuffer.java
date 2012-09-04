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
 * A CharToByteBuffer implementation that wraps a ByteBuffer for storage.
 * 
 * @author Brian S O'Neill
 * deprecated Moved to org.teatrove.trove.io package.
 */
@Deprecated
public class DefaultCharToByteBuffer
    extends org.teatrove.trove.io.DefaultCharToByteBuffer
    implements CharToByteBuffer
{
    private static final long serialVersionUID = 1L;

    public DefaultCharToByteBuffer(org.teatrove.trove.io.ByteBuffer buffer) {
        super(buffer);
    }

    public DefaultCharToByteBuffer(ByteBuffer buffer) {
        super(buffer);
    }

    public DefaultCharToByteBuffer(org.teatrove.trove.io.ByteBuffer buffer,
                                   String defaultEncoding) {
        super(buffer, defaultEncoding);
    }

    public DefaultCharToByteBuffer(ByteBuffer buffer, String defaultEncoding) {
        super(buffer, defaultEncoding);
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
