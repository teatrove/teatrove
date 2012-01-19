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
 * BooleanLiterals can only have one of two values, true or false. 
 *
 * @author Brian S O'Neill
 */
public class BooleanLiteral extends Literal {
    private static final long serialVersionUID = 1L;

    private Boolean mValue;

    public BooleanLiteral(SourceInfo info, boolean value) {
        super(info);
        mValue = value ? Boolean.TRUE : Boolean.FALSE;
        setType(Type.BOOLEAN_TYPE);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public void convertTo(Type type, boolean preferCast) {
        // No conversion need be applied for BooleanLiterals.
        setType(type);
    }

    public void setType(Type type) {
        // BooleanLiterals never evaluate to null.
        super.setType(type.toNonNull());
    }

    public Object getValue() {
        return mValue;
    }
}
