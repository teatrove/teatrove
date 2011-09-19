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
import org.teatrove.tea.compiler.Type;

/**
 * A Variable represents a variable declaration. A VariableRef is used to
 * reference Variables.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 35 <!-- $-->, <!--$$JustDate:--> 01/05/07 <!-- $-->
 * @see VariableRef
 */
public class Variable extends Node {
    private String mName;
    private TypeName mTypeName;
    private Type mType;

    private boolean mField;
    private boolean mStatic;
    private boolean mTransient;
    private boolean mStaticallyTyped;

    /**
     * Used for variable declarations.
     */
    public Variable(SourceInfo info, String name, TypeName typeName) {
        super(info);

        mName = name;
        mTypeName = typeName;
        mStaticallyTyped = false;
    }

    public Variable(SourceInfo info, String name, TypeName typeName, boolean staticallyTyped) {
        super(info);

        mName = name;
        mTypeName = typeName;
        mStaticallyTyped = staticallyTyped;
    }

    /**
     * Used when creating variables whose type has already been checked.
     */
    public Variable(SourceInfo info, String name, Type type) {
        super(info);

        mName = name;
        mTypeName = new TypeName(info, type);
        mType = type;
    }

    public Variable(SourceInfo info, String name, Type type, boolean staticallyTyped) {
        super(info);

        mName = name;
        mTypeName = new TypeName(info, type);
        mType = type;
        mStaticallyTyped = staticallyTyped;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public TypeName getTypeName() {
        return mTypeName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    /**
     * Returns null if type is unknown.
     */
    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mTypeName = new TypeName(getSourceInfo(), type);
        mType = type;
    }

    /**
     * @return true if this variable is a field instead of a local variable.
     */   
    public boolean isField() {
        return mField;
    }

    /**
     * @return true if this variable is statically typed via the define keyword
     */
    public boolean isStaticallyTyped() {
        return mStaticallyTyped;
    }

    /**
     * @return true if this variable is a field and is static.
     */
    public boolean isStatic() {
        return mStatic;
    }

    /**
     * @return true if this variable is transient.
     */
    public boolean isTransient() {
        return mTransient;
    }

    public void setField(boolean b) {
        mField = b;
        if (!b) {
            mStatic = false;
        }
    }

    public void setStatic(boolean b) {
        mStatic = b;
        if (b) {
            mField = true;
        }
    }

    public void setTransient(boolean b) {
        mTransient = b;
    }

    public int hashCode() {
        return mName.hashCode() + mTypeName.hashCode();
    }

    /**
     * Variables are tested for equality only by their name and type.
     * Field status is ignored.
     */
    public boolean equals(Object other) {
        if (other instanceof Variable) {
            Variable v = (Variable)other;
            return mName.equals(v.mName) && mTypeName.equals(v.mTypeName);
        }
        else {
            return false;
        }
    }
}
