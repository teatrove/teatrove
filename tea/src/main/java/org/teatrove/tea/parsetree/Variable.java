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

import java.lang.reflect.ParameterizedType;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;
import org.teatrove.trove.generics.GenericType;

/**
 * A Variable represents a variable declaration. A VariableRef is used to
 * reference Variables.
 *
 * @author Brian S O'Neill
 * @see VariableRef
 */
public class Variable extends Node {
    private static final long serialVersionUID = 1L;

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
        mTypeName = createTypeName(info, type);
        mType = type;
    }

    public Variable(SourceInfo info, String name, Type type, boolean staticallyTyped) {
        super(info);

        mName = name;
        mTypeName = createTypeName(info, type);
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
        mType = Type.preserveType(mType, type);
        if (mTypeName == null) {
            mTypeName = createTypeName(getSourceInfo(), type);
        } else {
            mTypeName = createTypeName(mTypeName.getSourceInfo(), type);
            // TODO: do we need to check which is better for generics?
            // - if type has generics, createTypeName
            // - else if genericTypes exists, create instnace
            // - else ignore
            //mTypeName = new TypeName(mTypeName.getSourceInfo(),
            //                         mTypeName.getGenericTypes(), type);
        }
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

    private TypeName createTypeName(SourceInfo info, Type type) {
        TypeName[] genericTypes = null;
        // TODO: is there a better to handle this?
        // for whatever reason, the type checker ends up running through
        // nodes twice (once with a TypeName and then once without so that
        // a new one gets created)...this helps to ensure that the generic
        // types are passed through when the type checker re-checks.
        // this mimics the functionality in TypeChecker check(TypeName)
        if (type != null &&
            type.getGenericClass() instanceof ParameterizedType) {
            
            ParameterizedType ptype =
                (ParameterizedType) type.getGenericClass();
            genericTypes = new TypeName[ptype.getActualTypeArguments().length];
            for (int i = 0; i < ptype.getActualTypeArguments().length; i++) {
                java.lang.reflect.Type gtype = ptype.getActualTypeArguments()[i];
                genericTypes[i] = createTypeName(getSourceInfo(), new Type(new GenericType(gtype)));
            }
        }

        return new TypeName(info, genericTypes, type);
    }
    
    public String toString() {
        String className = Object.class.getName();
        Type type = getType();
        if (type != null) {
            className = type.getClassName();
        }
        else {
            TypeName typeName = getTypeName();
            type = typeName.getType();
            if (type != null) {
                className = type.getClassName();
            }
            else {
                String name = typeName.getName();
                if (name != null) {
                    className = name;
                }
            }
        }
        
        return "Variable(" + className + " " + getName() + ")";
    }
}
