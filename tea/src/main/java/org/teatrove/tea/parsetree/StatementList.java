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
 * A StatementList wraps a list of statements as if they were a single
 * statement.
 * 
 * @author Brian S O'Neill
 * @see Block
 * @see Template#getStatement()
 */
public class StatementList extends Statement {
    private static final long serialVersionUID = 1L;

    private Statement[] mStatements;

    public StatementList(SourceInfo info, Statement[] statements) {
        super(info);

        mStatements = statements;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        StatementList sl = (StatementList)super.clone();

        int length = mStatements.length;
        Statement[] newStatements = new Statement[length];
        for (int i=0; i<length; i++) {
            newStatements[i] = (Statement)mStatements[i].clone();
        }
        sl.mStatements = newStatements;

        return sl;
    }

    public boolean isReturn() {
        if (mStatements != null) {
            for (int i=0; i<mStatements.length; i++) {
                if (mStatements[i].isReturn()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Statement[] getStatements() {
        return mStatements;
    }
    
    public void setStatements(Statement[] stmts) {
        mStatements = stmts;
    }
}
