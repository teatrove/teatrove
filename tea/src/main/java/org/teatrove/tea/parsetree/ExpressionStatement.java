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

import java.lang.reflect.Method;

/**
 * An ExpressionStatement allows an Expression to be converted to a Statement.
 * What happens to the value returned by the Expression is determined by
 * a code generator.
 *
 * @author Brian S O'Neill
 */
public class ExpressionStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private Method mReceiver;

    public ExpressionStatement(Expression expr) {
        super(expr.getSourceInfo());
        mExpr = expr;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ExpressionStatement es = (ExpressionStatement)super.clone();
        es.mExpr = (Expression)mExpr.clone();
        return es;
    }

    public Expression getExpression() {
        return mExpr;
    }

    public Method getReceiverMethod() {
        return mReceiver;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }

    public void setReceiverMethod(Method m) {
        mReceiver = m;
    }
}
