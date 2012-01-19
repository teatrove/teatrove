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
 * A StringLiteral is a constant string of characters, usually delimited
 * by quotes in a source file.
 *
 * @author Brian S O'Neill
 */
public class StringLiteral extends Literal {
    private static final long serialVersionUID = 1L;

    private String mValue;

    public StringLiteral(SourceInfo info, String value) {
        super(info);
        if (value == null) {
            throw new IllegalArgumentException
                ("StringLiterals cannot be null");
        }
        mValue = value;
        setType(Type.NON_NULL_STRING_TYPE);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public void setType(Type type) {
        // StringLiterals never evaluate to null.
        super.setType(type.toNonNull());
    }

    public Object getValue() {
        return mValue;
    }

    public boolean equals(Object another) {
        if (another instanceof StringLiteral) {
            return mValue.equals(((StringLiteral)another).mValue);
        }
        return false;
    }
}
