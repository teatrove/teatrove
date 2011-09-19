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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;

/**
 * Template is the main container node for Tea templates.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 27 <!-- $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class Template extends Node {
    private Name mName;
    private Variable[] mParams;
    private boolean mSubParam;
    private Statement mStatement;
    private Type mType;
    private List mDirectiveList;

    public Template(SourceInfo info,
                    Name name, Variable[] params, boolean subParam,
                    Statement statement, List directiveList) {

        super(info);

        mName = name;
        mParams = params;
        mSubParam = subParam;
        mStatement = statement;
        mDirectiveList = directiveList;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        Template t = (Template)super.clone();

        int length = mParams.length;
        Variable[] newParams = new Variable[length];
        for (int i=0; i<length; i++) {
            newParams[i] = (Variable)mParams[i].clone();
        }
        t.mParams = newParams;

        t.mStatement = (Statement)mStatement.clone();
 
        if (mDirectiveList != null)
            t.mDirectiveList = new ArrayList();
            for (Iterator i = mDirectiveList.iterator(); i.hasNext(); )
                t.mDirectiveList.add(((Directive) i.next()).clone());
        return t;
    }

    public Name getName() {
        return mName;
    }

    public Variable[] getParams() {
        return mParams;
    }

    public boolean hasSubstitutionParam() {
        return mSubParam;
    }

    /**
     * Will likely return a StatementList or Block in order to hold many
     * statements.
     *
     * @see StatementList
     * @see Block
     */
    public Statement getStatement() {
        return mStatement;
    }

    public void setStatement(Statement stmt) {
        mStatement = stmt;
    }

    public List getDirectives() { return mDirectiveList; }

    /**
     * The return type is set by a type checker. Returns null if this template
     * returns void, which is the default value.
     */
    public Type getReturnType() {
        return mType;
    }

    public void setReturnType(Type type) {
        mType = type;
    }
}
