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
 * An ExpressionList wraps a list of expressions that may appear in a
 * call statement or new array expression.
 *
 * @author Brian S O'Neill
 */
public class ExpressionList extends Node {
    private static final long serialVersionUID = 1L;

    private Expression[] mElements;

    public ExpressionList(SourceInfo info, Expression[] elements) {
        super(info);

        mElements = elements;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ExpressionList el = (ExpressionList)super.clone();

        int length = mElements.length;
        Expression[] newElements = new Expression[length];
        for (int i=0; i<length; i++) {
            newElements[i] = (Expression)mElements[i].clone();
        }
        el.mElements = newElements;

        return el;
    }

    public Expression[] getExpressions() {
        return mElements;
    }
}
