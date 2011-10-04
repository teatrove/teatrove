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
import org.teatrove.tea.compiler.Token;
import org.teatrove.tea.compiler.Type;

/**
 * 
 * @author Brian S O'Neill
 */
public abstract class BinaryLogicalExpression extends BinaryExpression
    implements Logical
{
    private static final long serialVersionUID = 1L;

    public BinaryLogicalExpression(SourceInfo info,
                                   Token operator,
                                   Expression left,
                                   Expression right) {
        super(info, operator, left, right);
    }

    public void convertTo(Type type, boolean preferCast) {
        Class<?> clazz = type.getObjectClass();

        // For these types, a Logical can simply generate a literal, skipping
        // a conversion.
        if (Boolean.class.isAssignableFrom(clazz) ||
            String.class.isAssignableFrom(clazz)) {

            setType(type);
        }
        else {
            super.convertTo(type, preferCast);
        }
    }
}
