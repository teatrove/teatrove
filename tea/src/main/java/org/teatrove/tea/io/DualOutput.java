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

package org.teatrove.tea.io;

import java.io.OutputStream;

/**
 * DualOutput wraps two OutputStreams so they can be written to simultaneously.
 * This is handy for writing to a file and a class injector at the same time.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 6 <!-- $--> 21 <!-- $$JustDate:--> 11/14/03 <!-- $-->
 * @deprecated Moved to org.teatrove.trove.io package.
 * @see ClassInjector
 */
public class DualOutput extends org.teatrove.trove.io.DualOutput {
    public DualOutput(OutputStream out1, OutputStream out2) {
        super(out1, out2);
    }
}        
