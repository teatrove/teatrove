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

import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * A ByteBuffer implementation that can read from an open file or can write
 * to it. This implementation is best suited for temporary byte data that is
 * too large to hold in memory.
 *
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class FileByteBuffer extends org.teatrove.trove.io.FileByteBuffer
    implements ByteBuffer
{
    public FileByteBuffer(RandomAccessFile file) throws IOException {
        super(file);
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
