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
 * RelationalExpression defines seven operations, "==", "!=", "<", ">", "<="
 * ">=" and "isa". The type of a RelationalExpression is a Boolean, set by a
 * type checker if there are no errors.
 *
 * <p>Operator returned is one of Token.EQ, Token.NE, Token.LT, Token.GT,
 * Token.LE, Token.GE or Token.ISA.
 *
 * @author Brian S O'Neill
 */
public class RelationalExpression extends BinaryLogicalExpression {
    private static final long serialVersionUID = 1L;

    private TypeName mTypeName;

    public RelationalExpression(SourceInfo info,
                                Token operator,
                                Expression left,
                                Expression right) {
        super(info, operator, left, right);
    }

    /**
     * Used to construct an "isa" RelationalExpression.
     */
    public RelationalExpression(SourceInfo info,
                                Token operator,
                                Expression left,
                                TypeName typeName) {
        super(info, operator, left, null);
        mTypeName = typeName;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public boolean isExceptionPossible() {
        int id = getOperator().getID();
        if (id != Token.EQ && id != Token.NE && id != Token.ISA) {
            return super.isExceptionPossible();
        }

        Expression left = getLeftExpression();
        Expression right = getLeftExpression();

        return
            (left != null && left.isExceptionPossible()) ||
            (right != null && right.isExceptionPossible());
    }

    /**
     * @return null if an "isa" RelationalExpression
     */
    public Expression getRightExpression() {
        return super.getRightExpression();
    }

    /**
     * Only applies if token is "isa".
     */
    public TypeName getIsaTypeName() {
        return mTypeName;
    }
}
