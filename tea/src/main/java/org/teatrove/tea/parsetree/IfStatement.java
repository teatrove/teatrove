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
 * An IfStatement consists of a condition, a "then" part and an "else" part.
 * Both the then and else parts are optional, but a parser should never
 * create an IfStatement without a then part. An optimizer may detect that
 * the then part never executes, and so eliminates it.
 *
 * @author Brian S O'Neill
 */
public class IfStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private Expression mCondition;
    private Block mThenPart;
    private Block mElsePart;
    private Variable[] mMergedVariables;

    public IfStatement(SourceInfo info,
                       Expression condition,
                       Block thenPart) {
        this(info, condition, thenPart, null);
    }

    public IfStatement(SourceInfo info,
                       Expression condition,
                       Block thenPart,
                       Block elsePart) {
        super(info);

        mCondition = condition;
        mThenPart = thenPart;
        mElsePart = elsePart;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        IfStatement is = (IfStatement)super.clone();
        is.mCondition = (Expression)mCondition.clone();
        if (mThenPart != null) {
            is.mThenPart = (Block)mThenPart.clone();
        }
        if (mElsePart != null) {
            is.mElsePart = (Block)mElsePart.clone();
        }
        return is;
    }

    public boolean isReturn() {
        return
            mThenPart != null && mThenPart.isReturn() &&
            mElsePart != null && mElsePart.isReturn();
    }

    public boolean isBreak() {
        return
            mThenPart != null && mThenPart.isBreak() &&
            mElsePart != null && mElsePart.isBreak();
    }

    public Expression getCondition() {
        return mCondition;
    }

    /**
     * @return Null if no then part.
     */
    public Block getThenPart() {
        return mThenPart;
    }

    /**
     * @return Null if no else part.
     */
    public Block getElsePart() {
        return mElsePart;
    }

    public void setCondition(Expression condition) {
        mCondition = condition;
    }

    public void setThenPart(Block block) {
        mThenPart = block;
    }

    public void setElsePart(Block block) {
        mElsePart = block;
    }

    /**
     * Returns the variables that were commonly assigned in both the "then"
     * and "else" parts of the if statement, were merged together and moved
     * into the parent scope. Returns null if not set.
     */
    public Variable[] getMergedVariables() {
        return mMergedVariables;
    }

    public void setMergedVariables(Variable[] vars) {
        mMergedVariables = vars;
    }
}
