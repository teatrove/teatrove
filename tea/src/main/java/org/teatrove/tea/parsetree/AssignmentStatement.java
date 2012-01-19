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
 * AssignmentStatements can only assign values to variables, and not to
 * array elements. AssignmentStatements are not expressions as they are in
 * C or Java, and thus chaining is not allowed. i.e. a = b = c;
 *
 * @author Brian S O'Neill
 */
public class AssignmentStatement extends Statement {
    private static final long serialVersionUID = 1L;

    private VariableRef mLvalue;
    private Expression mRvalue;

    public AssignmentStatement(SourceInfo info,
                               VariableRef lvalue,
                               Expression rvalue) {
        super(info);

        mLvalue = lvalue;
        mRvalue = rvalue;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        AssignmentStatement as = (AssignmentStatement)super.clone();
        as.mLvalue = (VariableRef)mLvalue.clone();
        as.mRvalue = (Expression)mRvalue.clone();
        return as;
    }

    public VariableRef getLValue() {
        return mLvalue;
    }

    public Expression getRValue() {
        return mRvalue;
    }

    public void setRValue(Expression rvalue) {
        mRvalue = rvalue;
    }
}
