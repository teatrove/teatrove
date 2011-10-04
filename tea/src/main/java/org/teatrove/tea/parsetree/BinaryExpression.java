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
import org.teatrove.tea.compiler.Type;

/**
 * A BinaryExpression contains a left expression, a right expression and
 * an operator. BinaryExpressions never evaluate to null.
 *
 * @author Brian S O'Neill
 */
public abstract class BinaryExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private Token mOperator;
    private Expression mLeft;
    private Expression mRight;

    public BinaryExpression(SourceInfo info,
                            Token operator,
                            Expression left,
                            Expression right) {
        super(info);

        mOperator = operator;
        mLeft = left;
        mRight = right;
    }

    public Object clone() {
        BinaryExpression be = (BinaryExpression)super.clone();
        be.mLeft = (Expression)mLeft.clone();
        be.mRight = (Expression)mRight.clone();
        return be;
    }

    public boolean isExceptionPossible() {
        if (mLeft != null) {
            if (mLeft.isExceptionPossible()) {
                return true;
            }
            Type type = mLeft.getType();
            if (type != null && type.isNullable()) {
                return true;
            }
        }

        if (mRight != null) {
            if (mRight.isExceptionPossible()) {
                return true;
            }
            Type type = mRight.getType();
            if (type != null && type.isNullable()) {
                return true;
            }
        }

        return false;
    }

    public void setType(Type type) {
        // BinaryExpressions never evaluate to null.
        super.setType(type.toNonNull());
    }

    public Token getOperator() {
        return mOperator;
    }

    public Expression getLeftExpression() {
        return mLeft;
    }

    public Expression getRightExpression() {
        return mRight;
    }

    public void setLeftExpression(Expression left) {
        mLeft = left;
    }

    public void setRightExpression(Expression right) {
        mRight = right;
    }
}
