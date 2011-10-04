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
 * A ForeachStatement iterates over the values of an array or a Collection, 
 * storing each value in a variable, allowing a statement or statements to 
 * operate on each. Reverse looping is supported for arrays and Lists.
 *
 * <p>Because Collections don't know the type of elements they 
 * contain (they only know that they are Objects), the only operations allowed
 * on the loop variable are those that are defined for Object. 
 *
 * <p>Collection class can be subclassed to contain a special
 * field that defines the element type. The field must have the following
 * signature: <tt>public static final Class ELEMENT_TYPE</tt>
 *
 * @author Brian S O'Neill
 */
public class ForeachStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private VariableRef mLoopVar;
    private Expression mRange;
    private Expression mEndRange;
    private boolean mReverse;
    private Statement mInitializer;
    private Block mBody;

    public ForeachStatement(SourceInfo info,
                            VariableRef loopVar,
                            Expression range,
                            Expression endRange,
                            boolean reverse,
                            Block body) {
        super(info);

        mLoopVar = loopVar;
        mRange = range;
        mEndRange = endRange;
        mReverse = reverse;
        mBody = body;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ForeachStatement fs = (ForeachStatement)super.clone();
        fs.mLoopVar = (VariableRef)mLoopVar.clone();
        fs.mRange = (Expression)mRange.clone();
        fs.mEndRange = (Expression)mEndRange.clone();
        if (mInitializer != null) {
            fs.mInitializer = (Statement)mInitializer.clone();
        }
        fs.mBody = (Block)mBody.clone();
        return fs;
    }

    public VariableRef getLoopVariable() {
        return mLoopVar;
    }

    public Expression getRange() {
        return mRange;
    }

    /**
     * Returns null if this foreach statement iterates over an array/collection
     * instead of an integer range of values.
     */
    public Expression getEndRange() {
        return mEndRange;
    }

    public boolean isReverse() {
        return mReverse;
    }

    /**
     * Initializer is a section of code that executes before the loop is
     * entered. By default, it is null. A type checker may define an
     * initializer.
     */
    public Statement getInitializer() {
        return mInitializer;
    }

    public Block getBody() {
        return mBody;
    }

    public void setRange(Expression range) {
        mRange = range;
    }

    public void setEndRange(Expression endRange) {
        mEndRange = endRange;
    }

    public void setInitializer(Statement stmt) {
        mInitializer = stmt;
    }

    public void setBody(Block body) {
        mBody = body;
    }
}
