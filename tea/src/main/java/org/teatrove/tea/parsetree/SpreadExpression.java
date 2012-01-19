package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;

/**
 * Expression that handles spread operators which is responsiblef or taking an
 * expression as either a collection or array and generating a new collection
 * or array by invoking another expression against each item in the collection.
 */
public class SpreadExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private Expression mOperation;
    
    public SpreadExpression(SourceInfo info, Expression expr,
                            Expression operation) {
        super(info);

        mExpr = expr;
        mOperation = operation;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        SpreadExpression spread = (SpreadExpression)super.clone();
        spread.mExpr = (Expression)mExpr.clone();
        spread.mOperation = (Expression)mOperation.clone();
        return spread;
    }

    public boolean isExceptionPossible() {
        if (super.isExceptionPossible()) {
            return true;
        }

        if (mExpr != null) {
            if (mExpr.isExceptionPossible()) {
                return true;
            }
        }
        
        if (mOperation != null) {
            if (mOperation.isExceptionPossible()) {
                return true;
            }
        }

        return false;
    }

    public Expression getExpression() {
        return mExpr;
    }

    public Expression getOperation() {
        return mOperation;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }

    public void setOperation(Expression operation) {
        mOperation = operation;
    }
}
