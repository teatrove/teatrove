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
 * A Statement represents a stand-alone unit of code. The default Statement
 * implements the empty statement, represented by a semi-colon ';' in a
 * source file. A Parser usually strips most empty statements out.
 *
 * @author Brian S O'Neill
 */
public class Statement extends Node {
    private static final long serialVersionUID = 1L;

    public Statement(SourceInfo info) {
        super(info);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    /**
     * Returns true if Statement definitely returns from its method either
     * from a return statement or a throw statement.
     */
    public boolean isReturn() {
        return false;
    }

    /**
     * Returns true if Statement 'breaks' during its execution.
     */
    public boolean isBreak() {
        return false;
    }
}
