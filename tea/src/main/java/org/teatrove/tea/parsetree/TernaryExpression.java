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
 * @author Nick Hagan
 */
public class TernaryExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private Expression mCondition;
    private Expression mThenPart;
    private Expression mElsePart;

    public TernaryExpression(SourceInfo info, Expression condition,
                             Expression thenPart, Expression elsePart) {
        super(info);

        mCondition = condition;
        mThenPart = thenPart;
        mElsePart = elsePart;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        TernaryExpression expr = (TernaryExpression) super.clone();
        expr.mCondition = (Expression) mCondition.clone();
        expr.mThenPart = (Expression) mThenPart.clone();
        expr.mElsePart = (Expression) mElsePart.clone();
        return expr;
    }

    public Expression getCondition() {
        return mCondition;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public Expression getThenPart() {
        return mThenPart;
    }

    public void setThenPart(Expression thenPart) {
        mThenPart = thenPart;
    }

    public Expression getElsePart() {
        return mElsePart;
    }

    public void setElsePart(Expression elsePart) {
        mElsePart = elsePart;
    }
}
