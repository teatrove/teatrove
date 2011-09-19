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

package org.teatrove.trove.persist;

import java.io.*;

/**
 * Creates object streams that wrap plain streams. Override one or both of
 * the methods to control how the streams are constructed and which options to
 * apply to it.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 3 <!-- $-->, <!--$$JustDate:--> 02/02/19 <!-- $-->
 */
public class ObjectStreamBuilder {
    public ObjectInputStream createInputStream(InputStream in)
        throws IOException
    {
        return new ObjectInputStream(in);
    }

    public ObjectOutputStream createOutputStream(OutputStream out)
        throws IOException
    {
        return new ObjectOutputStream(out);
    }
}
