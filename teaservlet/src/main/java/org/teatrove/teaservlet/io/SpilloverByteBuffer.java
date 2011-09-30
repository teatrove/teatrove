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
 * A ByteBuffer implementation that initially stores its data in a
 * DefaultByteBuffer, but after a certain threshold is reached, spills over
 * into a FileByteBuffer.
 *
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class SpilloverByteBuffer extends org.teatrove.trove.io.SpilloverByteBuffer
    implements ByteBuffer
{
    public SpilloverByteBuffer(Group group) {
        super(group);
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

    public static abstract class Group
        extends org.teatrove.trove.io.SpilloverByteBuffer.Group
    {
        public Group(long threshold) {
            super(threshold);
        }
    }
}
