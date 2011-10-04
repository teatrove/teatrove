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
 * ArithmeticExpression defines addition, subtraction, multiplication,
 * division or remainder operations. The type of an ArithmeticExpression is a
 * Number, set by a type checker if there are no errors.
 *
 * <p>Operator returned is one of Token.PLUS, Token.MINUS,
 * Token.MULT, Token.DIV or Token.MOD.
 *
 * @author Brian S O'Neill
 */
public class ArithmeticExpression extends BinaryExpression {
    private static final long serialVersionUID = 1L;

    public ArithmeticExpression(SourceInfo info,
                                Token operator,
                                Expression left,
                                Expression right) {
        super(info, operator, left, right);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }
}
