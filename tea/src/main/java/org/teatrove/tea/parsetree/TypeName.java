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
 * @author Brian S O'Neill
 * @version

 */
public class TypeName extends Name {
    private int mDimensions;
    private Type mType;

    public TypeName(SourceInfo info, String name) {
        super(info, name);
    }

    public TypeName(SourceInfo info, String name, int dimensions) {
        super(info, name);
        mDimensions = dimensions;
    }

    public TypeName(SourceInfo info, Name name) {
        super(info, name.getName());
    }

    public TypeName(SourceInfo info, Name name, int dimensions) {
        super(info, name.getName());
        mDimensions = dimensions;
    }

    public TypeName(SourceInfo info, Type type) {
        super(info, type == null ? "" : type.getNaturalClass().getName());
        mType = type;
        mDimensions = -1;
    }
    
    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public int getDimensions() {
        if (mDimensions < 0 && mType != null) {
            Class clazz = mType.getNaturalClass();
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
                if (mType == null) {
                    return null == ((TypeName)other).getType();
                }
                else {
                    return mType.equals(((TypeName)other).getType());
                }
            }
        }

        return false;
    }
}
