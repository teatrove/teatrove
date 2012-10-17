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

/**
 * A ByteData implementation that wraps an array of bytes.
 * 
 * @author Brian S O'Neill
 * @deprecated Moved to org.teatrove.trove.io package.
 */
public class ArrayByteData extends org.teatrove.trove.io.ArrayByteData
    implements ByteData
{
    private static final long serialVersionUID = 1L;

    public ArrayByteData(byte[] data) {
        super(data);
    }

    public ArrayByteData(byte[] data, int offset, int length) {
        super(data, offset, length);
    }
}
