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

package org.teatrove.tea.runtime;

import java.io.IOException;

/**
 * Defines a simple interface for Tea templates to output information. 
 *
 * @author Brian S O'Neill
 * @see org.teatrove.tea.compiler.Compiler#getRuntimeReceiver
 */
public interface OutputReceiver {
    /**
     * This method receives the output of a template. 
     *
     * NOTE:  This method should <b>not</b> be called directly within a 
     * template.
     *
     * @exclude
     */
    public void print(Object obj) throws Exception;

    /**
     * @hidden
     */
    public void write(int c) throws IOException;

    /**
     * @hidden
     */
    public void write(char[] cbuf) throws IOException;

    /**
     * @hidden
     */
    public void write(char[] cbuf, int off, int len) throws IOException;

    /**
     * @hidden
     */
    public void write(String str) throws IOException;

    /**
     * @hidden
     */
    public void write(String str, int off, int len) throws IOException;
}
