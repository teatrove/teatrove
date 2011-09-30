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
 * @author Brian S O'Neill
 * @version

 */
public abstract class CallExpression extends Expression {
    private Name mTarget;
    private ExpressionList mParams;
    private Statement mInitializer;
    private Block mSubParam;
    private boolean mVoidPermitted = false;

    public CallExpression(SourceInfo info, 
                          Name target, 
                          ExpressionList params,
                          Block subParam) {
        super(info);

        mTarget = target;
        mParams = params;
        mSubParam = subParam;
    }

    public Object clone() {
        CallExpression ce = (CallExpression)super.clone();
        ce.mParams = (ExpressionList)mParams.clone();
        if (mInitializer != null) {
            ce.mInitializer = (Statement)mInitializer.clone();
        }
        if (mSubParam != null) {
            ce.mSubParam = (Block)mSubParam.clone();
        }
        return ce;
    }

    public boolean isExceptionPossible() {
        return true;
    }

    public Name getTarget() {
        return mTarget;
    }

    public ExpressionList getParams() {
        return mParams;
    }

    /**
     * Initializer is a section of code that executes before the substitution
     * param. By default, it is null. If a CallExpression has a substitution
     * param, a type checker may define an initializer.
     */
    public Statement getInitializer() {
        return mInitializer;
    }

    public Block getSubstitutionParam() {
        return mSubParam;
    }

    /**
     * A CallExpression is permitted to return void only in certain cases.
     * By default this method returns false.
     */
    public boolean isVoidPermitted() {
        return mVoidPermitted;
    }

    public void setParams(ExpressionList params) {
        mParams = params;
    }

    public void setInitializer(Statement stmt) {
        mInitializer = stmt;
    }

    public void setSubstitutionParam(Block subParam) {
        mSubParam = subParam;
    }

    public void setVoidPermitted(boolean b) {
        mVoidPermitted = b;
    }
}
