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
 * Base class for all Literals that have a numeric type. 
 *
 * @author Brian S O'Neill
 */
public class NumberLiteral extends Literal {
    private static final long serialVersionUID = 1L;

    private Number mValue;

    public NumberLiteral(SourceInfo info, Number value) {
        super(info);
        if (value == null) {
            throw new IllegalArgumentException
                ("NumberLiterals cannot be null");
        }
        mValue = value;
        super.setType(new Type(value.getClass()).toPrimitive());
    }

    public NumberLiteral(SourceInfo info, int value) {
        this(info, new Integer(value));
    }

    public NumberLiteral(SourceInfo info, float value) {
        this(info, new Float(value));
    }

    public NumberLiteral(SourceInfo info, long value) {
        this(info, new Long(value));
    }

    public NumberLiteral(SourceInfo info, double value) {
        this(info, new Double(value));
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public void convertTo(Type type, boolean preferCast) {
        if (Number.class.isAssignableFrom(type.getObjectClass())) {
            if (type.isPrimitive()) {
                super.setType(type);
            }
            else if (type.hasPrimitivePeer()) {
                super.setType(type.toPrimitive());
                super.convertTo(type.toNonNull(), preferCast);
            }
            else {
                super.convertTo(type, preferCast);
            }
        }
        else {
            super.convertTo(type, preferCast);
        }
    }

    public void setType(Type type) {
        super.setType(type);

        // NumberLiterals never evaluate to null when the value is known.
        if (isValueKnown()) {
            super.setType(getType().toNonNull());
        }
    }

    public Object getValue() {
        return mValue;
    }

    /**
     * Value is known only if type is a number or can be assigned a number.
     */
    public boolean isValueKnown() {
        Type type = getType();
        if (type != null) {
            Class<?> clazz = type.getObjectClass();
            return Number.class.isAssignableFrom(clazz) ||
                clazz.isAssignableFrom(Number.class);
        }
        else {
            return false;
        }
    }
}
