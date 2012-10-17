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

package org.teatrove.teaservlet;

import java.io.IOException;

/**
 * This class only provides a compatibility bridge between the original
 * TeaServlet I/O classes and the Trove replacements.
 *
 * @author Brian S O'Neill
 */
@Deprecated
class FastCharToByteBuffer extends org.teatrove.trove.io.FastCharToByteBuffer
    implements org.teatrove.teaservlet.io.CharToByteBuffer
{
    private static final long serialVersionUID = 1L;

    public FastCharToByteBuffer(org.teatrove.trove.io.ByteBuffer buffer) {
        super(buffer);
    }

    public FastCharToByteBuffer(org.teatrove.trove.io.ByteBuffer buffer, String enc) 
	{
        super(buffer, enc);
    }

    public void appendSurrogate(org.teatrove.teaservlet.io.ByteData s)
		throws IOException
	{
        super.appendSurrogate(s);
    }

    public void addCaptureBuffer(org.teatrove.teaservlet.io.ByteBuffer buffer)
		throws IOException
	{
        super.addCaptureBuffer(buffer);
    }

    public void removeCaptureBuffer(org.teatrove.teaservlet.io.ByteBuffer buffer)
		throws IOException
	{
        super.removeCaptureBuffer(buffer);
    }
}
