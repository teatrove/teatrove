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
import org.teatrove.tea.compiler.Type;

/**
 * NegateExpression is a simple unary expression that calculates the negative
 * value of an expression. The expression it operates on must return a
 * Number type.
 *
 * @author Brian S O'Neill
 */
public class NegateExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;

    public NegateExpression(SourceInfo info,
                            Expression expr) {
        super(info);
        mExpr = expr;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        NegateExpression ne = (NegateExpression)super.clone();
        ne.mExpr = (Expression)mExpr.clone();
        return ne;
    }

    public boolean isExceptionPossible() {
        if (mExpr != null) {
            if (mExpr.isExceptionPossible()) {
                return true;
            }
            Type type = mExpr.getType();
            if (type != null && type.isNullable()) {
                return true;
            }
        }
        return false;
    }

    public void setType(Type type) {
        // NegateExpressions never evaluate to null.
        super.setType(type.toNonNull());
    }

    public Expression getExpression() {
        return mExpr;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }
}
