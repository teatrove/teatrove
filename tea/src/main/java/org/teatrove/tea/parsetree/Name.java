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

package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;

/**
 * A Name just associates a String with a SourceInfo object. Names are
 * usually restricted to only containing Java identifier characters and
 * '.' characters.
 *
 * @author Brian S O'Neill
 */
public class Name extends Node {
    private static final long serialVersionUID = 1L;

    private String mName;

    public Name(SourceInfo info, String name) {
        super(info);
        mName = name;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public String getName() {
        return mName;
    }

    public int hashCode() {
        return mName.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof Name) {
            return ((Name)other).mName == mName;
        }
        else {
            return false;
        }
    }
}
