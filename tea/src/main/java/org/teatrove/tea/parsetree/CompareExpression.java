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
import org.teatrove.tea.compiler.Token;

/**
 * A CompareExpression is otherwise known as a spaceship operation or compare
 * operation and contains a left expression and a right expression to which
 * a comparison is done between the two returning -1 if less then, 0 if equal,
 * or 1 if greater than.
 * 
 * @author Nick Hagan
 */
public class CompareExpression extends BinaryExpression {
    private static final long serialVersionUID = 1L;

    public CompareExpression(SourceInfo info, Token operator,
                             Expression left, Expression right) {
        super(info, operator, left, right);
    }

    public boolean isExceptionPossible() {
        if (getLeftExpression().isExceptionPossible()) {
            return true;
        }
            
        if (getRightExpression().isExceptionPossible()) {
            return true;
        }

        return false;
    }
    
    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }
}
