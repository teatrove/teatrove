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
 * VariableRef is used to reference declared Variables.
 *
 * @author Brian S O'Neill
 * @see Variable
 */
public class VariableRef extends Expression implements NullSafe {
    private static final long serialVersionUID = 1L;

    private String mName;
    private Variable mVariable;
    private boolean mNullSafe;

    public VariableRef(SourceInfo info, String name) {
        super(info);

        mName = name;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public String getName() {
        return mName;
    }

    public Variable getVariable() {
        return mVariable;
    }

    /**
     * Setting the variable resets the VariableRef's initial type, but does
     * not clear any type conversions.
     */
    public void setVariable(Variable var) {
        mVariable = var;
        setInitialType(var.getType());
    }
    
    public int hashCode() {
        return mName.hashCode();
    }
    
    public boolean equals(Object other) {
        if (other instanceof VariableRef) {
            VariableRef ref = (VariableRef)other;
            return mName.equals(ref.mName);
        }
        else {
            return false;
        }
    }
    
    public boolean isNullSafe() {
        return mNullSafe;
    }
    
    public void setNullSafe(boolean nullSafe) {
        mNullSafe = nullSafe;
    }
}
