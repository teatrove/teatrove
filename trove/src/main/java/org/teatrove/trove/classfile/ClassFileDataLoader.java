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

package org.teatrove.trove.classfile;

import java.io.*;

/**
 * Loads data for class files from qualified class names.
 *
 * @author Brian S O'Neill
 */
public interface ClassFileDataLoader {
    /**
     * Returns null if class data not found. The given name must use '.'
     * characters to separate packages and top-level classes. A '$' character
     * separates inner class names.
     *
     * @param name fully qualified class name
     */
    public InputStream getClassData(String name) throws IOException;
}
