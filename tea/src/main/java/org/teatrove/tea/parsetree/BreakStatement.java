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
 * 
 * @author Sean T. Treat
 */
public class BreakStatement extends Statement {
    private static final long serialVersionUID = 1L;

    public BreakStatement(SourceInfo info) {
        super(info);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public boolean isBreak() {
        return true;
    }

    public Object clone() {
        return (BreakStatement)super.clone();
    }
}
