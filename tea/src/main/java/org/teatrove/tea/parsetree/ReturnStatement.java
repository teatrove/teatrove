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
 * A ReturnStatement allows the values of Expressions to be returned from an
 * execution scope. If the expression is null, then void is returned.
 *
 * @author Brian S O'Neill
 */
public class ReturnStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;

    public ReturnStatement(SourceInfo info, Expression expr) {
        super(info);
        mExpr = expr;
    }

    /**
     * Construct a ReturnStatement from just an expression and its SourceInfo.
     */
    public ReturnStatement(Expression expr) {
        this(expr.getSourceInfo(), expr);
    }

    /**
     * Construct a ReturnStatement that returns void.
     */
    public ReturnStatement(SourceInfo info) {
        this(info, null);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ReturnStatement rs = (ReturnStatement)super.clone();
        rs.mExpr = (Expression)mExpr.clone();
        return rs;
    }

    public boolean isReturn() {
        return true;
    }

    public boolean isBreak() {
        return true;
    }

    /**
     * Returns the expression to return or null if void is returned.
     */
    public Expression getExpression() {
        return mExpr;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }
}
