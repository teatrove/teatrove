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
 * A literal is an expression that has a constant value. 
 *
 * @author Brian S O'Neill
 */
public abstract class Literal extends Expression {
    private static final long serialVersionUID = 1L;

    protected Literal(SourceInfo info) {
        super(info);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    /**
     * Usually returns true, but some literals may have an unknown value if
     * they are converted to another type.
     */
    public boolean isValueKnown() {
        return true;
    }
}
