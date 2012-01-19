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
 * 
 * @author Brian S O'Neill, Nick Hagan
 */
public class TypeName extends Name {
    private static final long serialVersionUID = 1L;

    private int mDimensions;
    private Type mType;
    private TypeName[] mGenericTypes;

    public TypeName(SourceInfo info, String name) {
        super(info, name);
    }

    public TypeName(SourceInfo info, String name, TypeName[] genericTypes) {
        super(info, name);
        this.mGenericTypes = genericTypes;
    }

    public TypeName(SourceInfo info, String name, int dimensions) {
        super(info, name);
        mDimensions = dimensions;
    }

    public TypeName(SourceInfo info, String name,
                    TypeName[] genericTypes, int dimensions) {
        super(info, name);
        mGenericTypes = genericTypes;
        mDimensions = dimensions;
    }

    public TypeName(SourceInfo info, Name name) {
        super(info, name.getName());
    }

    public TypeName(SourceInfo info, Name name, TypeName[] genericTypes) {
        super(info, name.getName());
        mGenericTypes = genericTypes;
    }

    public TypeName(SourceInfo info, Name name, int dimensions) {
        super(info, name.getName());
        mDimensions = dimensions;
    }

    public TypeName(SourceInfo info, Name name,
                    TypeName[] genericTypes, int dimensions) {
        super(info, name.getName());
        mDimensions = dimensions;
        mGenericTypes = genericTypes;
    }

    public TypeName(SourceInfo info, Type type) {
        super(info, type == null ? "" : type.getNaturalClass().getName());
        mType = type;
        mDimensions = -1;
    }

    public TypeName(SourceInfo info, TypeName[] genericTypes, Type type) {
        super(info, type == null ? "" : type.getNaturalClass().getName());
        mType = type;
        mDimensions = -1;
        mGenericTypes = genericTypes;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public TypeName[] getGenericTypes() {
        return mGenericTypes;
    }

    public int getDimensions() {
        if (mDimensions < 0 && mType != null) {
            Class<?> clazz = mType.getNaturalClass();
            int dim = 0;
            while (clazz.isArray()) {
                dim++;
                clazz = clazz.getComponentType();
            }
            mDimensions = dim;
        }

        return mDimensions;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

    public int hashCode() {
        return super.hashCode() + getDimensions();
    }

    public boolean equals(Object other) {
        if (other instanceof TypeName) {
            if (super.equals(other)) {
                // check types
                boolean valid = false;
                if (mType == null) {
                    valid = (null == ((TypeName)other).getType());
                }
                else {
                    valid = mType.equals(((TypeName)other).getType());
                }

                if (!valid) { return false; }

                // check generics
                TypeName[] genericTypes = ((TypeName)other).getGenericTypes();
                if (mGenericTypes == null) {
                    valid = (null == genericTypes);
                }
                else if (genericTypes == null) {
                    valid = false;
                }
                else if (genericTypes.length != mGenericTypes.length) {
                    valid = false;
                }
                else {
                    valid = true;
                    for (int i = 0; i < genericTypes.length; i++) {
                        valid = mGenericTypes[i].equals(genericTypes[i]);
                        if (!valid) { break; }
                    }
                }

                // return state
                return valid;
            }
        }

        return false;
    }
}
