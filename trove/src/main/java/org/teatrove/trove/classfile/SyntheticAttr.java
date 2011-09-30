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

import java.io.DataInput;
import java.io.IOException;

/**
 * This class corresponds to the Synthetic_attribute structure introduced in
 * JDK1.1. It is not defined in the first edition of 
 * <i>The Java Virual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
class SyntheticAttr extends Attribute {
    public SyntheticAttr(ConstantPool cp) {
        super(cp, SYNTHETIC);
    }

    public int getLength() {
        return 0;
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        if (length > 0) {
            din.skipBytes(length);
        }

        return new SyntheticAttr(cp);
    }
}

