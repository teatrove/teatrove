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
 * A statement that catches instances of Exception, calls
 * ThreadGroup.uncaughtException, and moves on. This statement is inserted
 * by a TypeChecker, and does not appear directly in Tea templates.
 * 
 * @author Brian S O'Neill
 */
public class ExceptionGuardStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private Statement mGuarded;
    private Statement mReplacement;

    /**
     * @param guarded Statement to guard.
     * @param replacement Optional Statement to execute in catch handler.
     */
    public ExceptionGuardStatement(Statement guarded,
                                   Statement replacement) {
        this(guarded.getSourceInfo(), guarded, replacement);
    }

    /**
     * @param guarded Statement to guard.
     * @param replacement Optional Statement to execute in catch handler.
     */
    public ExceptionGuardStatement(SourceInfo info,
                                   Statement guarded,
                                   Statement replacement) {
        super(info);
        mGuarded = guarded;
        mReplacement = replacement;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ExceptionGuardStatement egs = (ExceptionGuardStatement)super.clone();
        egs.mGuarded = (Statement)mGuarded.clone();
        if (mReplacement != null) {
            egs.mReplacement = (Statement)mReplacement.clone();
        }
        return egs;
    }

    public boolean isReturn() {
        return
            mGuarded != null && mGuarded.isReturn() &&
            mReplacement != null && mReplacement.isReturn();
    }

    public boolean isBreak() {
        return
            mGuarded != null && mGuarded.isBreak() &&
            mReplacement != null && mReplacement.isBreak();
    }

    /**
     * May return null if removed.
     */
    public Statement getGuarded() {
        return mGuarded;
    }

    /**
     * May return null since this is optional.
     */
    public Statement getReplacement() {
        return mReplacement;
    }

    public void setGuarded(Statement guarded) {
        mGuarded = guarded;
    }

    public void setReplacement(Statement replacement) {
        mReplacement = replacement;
    }
}
