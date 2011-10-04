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

import java.beans.IntrospectionException;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;

/**
 * An expression that evaluates to a new array or Map of values.
 *
 * @author Brian S O'Neill
 */
public class NewArrayExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private ExpressionList mList;
    private boolean mAssociative;

    public NewArrayExpression(SourceInfo info,
                              ExpressionList list,
                              boolean associative) {
        super(info);

        mList = list;
        mAssociative = associative;
    }

    public Object clone() {
        NewArrayExpression nae = (NewArrayExpression)super.clone();
        nae.mList = (ExpressionList)mList.clone();
        return nae;
    }

    public boolean isExceptionPossible() {
        if (mList != null) {
            Expression[] exprs = mList.getExpressions();
            for (int i=0; i<exprs.length; i++) {
                if (exprs[i].isExceptionPossible()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public void convertTo(Type toType, boolean preferCast) {
        super.convertTo(toType, preferCast);

        // If converting to a different array element type, convert all the
        // expressions in the list.

        if (String.class.isAssignableFrom(toType.getNaturalClass())) {
            // Special case to prevent String conversion from setting all
            // internal elements to chars.
            return;
        }

        Type elementType;
        try {
            elementType = toType.getArrayElementType();
        }
        catch (IntrospectionException e) {
            throw new RuntimeException(e.toString());
        }

        if (elementType != null) {
            super.setType(toType);

            Expression[] exprs = getExpressionList().getExpressions();

            int index, increment;
            if (isAssociative()) {
                index = 1;
                increment = 2;
            }
            else {
                index = 0;
                increment = 1;
            }

            for (; index < exprs.length; index += increment) {
                if (exprs[index].getType() != Type.NULL_TYPE) {
                    exprs[index].convertTo(elementType, preferCast);
                }
            }
        }
    }

    public void setType(Type type) {
        super.setType(null);
        if (type != null) {
            // NewArrayExpressions never evaluate to null.
            // Call the overridden convertTo method in order for elements to
            // be converted to the correct type.
            this.convertTo(type.toNonNull(), false);
        }
    }

    public ExpressionList getExpressionList() {
        return mList;
    }

    public boolean isAssociative() {
        return mAssociative;
    }

    public void setExpressionList(ExpressionList list) {
        mList = list;
    }

    /**
     * @return true if this array is composed entirely of constants.
     */
    public boolean isAllConstant() {
        Expression[] exprs = mList.getExpressions();

        int i;
        for (i=0; i<exprs.length; i++) {
            Expression expr = exprs[i];

            if (expr instanceof NewArrayExpression) {
                NewArrayExpression nae = (NewArrayExpression)expr;
                if (!nae.isAllConstant()) {
                    return false;
                }
            }
            else if (!expr.isValueKnown()) {
                return false;
            }
        }

        return true;
    }
}
