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

import java.util.LinkedList;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;

/**
 * A ParenExpression is a thin wrapper around an Expression that was
 * delimited by parenthesis in the source code. A parse tree does not
 * necessarily need a ParenExpression, but it is useful when reconstructing
 * something that resembles the source code.
 *
 * @author Brian S O'Neill
 */
public class ParenExpression extends Expression implements NullSafe {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private boolean mNullSafe;

    public ParenExpression(SourceInfo info, Expression expr) {
        super(info);
        mExpr = expr;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ParenExpression pe = (ParenExpression)super.clone();
        pe.mExpr = (Expression)mExpr.clone();
        return pe;
    }

    public boolean isExceptionPossible() {
        return super.isExceptionPossible() ||
            (mExpr != null && mExpr.isExceptionPossible());
    }

    public Type getType() {
        return mExpr.getType();
    }

    public Type getInitialType() {
        return mExpr.getInitialType();
    }

    public void convertTo(Type toType, boolean preferCast) {
        mExpr.convertTo(toType, preferCast);
    }

    public LinkedList<Conversion> getConversionChain() {
        return mExpr.getConversionChain();
    }

    public void setType(Type type) {
        mExpr.setType(type);
    }

    public void setInitialType(Type type) {
        mExpr.setInitialType(type);
    }

    public boolean isValueKnown() {
        return mExpr.isValueKnown();
    }

    public Object getValue() {
        return mExpr.getValue();
    }

    public Expression getExpression() {
        return mExpr;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }
    
    public boolean isNullSafe() {
        return mNullSafe;
    }
    
    public void setNullSafe(boolean nullSafe) {
        mNullSafe = nullSafe;
    }
}
