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

package org.teatrove.trove.generics;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 
 *
 * @author Nick Hagan
 */
public class WildcardTypeHandler
    extends AbstractGenericHandler<WildcardType> {

    private GenericType rawType;

    public WildcardTypeHandler(GenericType rootType,
                               WildcardType genericType) {
        super(rootType, genericType);
    }

    @Override
    public GenericType getRawType() {
        if (this.rawType == null) {
            this.rawType = getRawType0();
        }

        return this.rawType;
    }

    @Override
    public int getDimensions() {
        return this.getRawType().getDimensions();
    }

    @Override
    public GenericType getComponentType() {
        return this.getRawType().getComponentType();
    }

    @Override
    public GenericType getRootComponentType() {
        return this.getRawType().getRootComponentType();
    }

    @Override
    public GenericType[] getTypeArguments() {
        return this.getRawType().getTypeArguments();
    }

    protected GenericType getRawType0() {
        WildcardType wildcard = this.getGenericType();

        // check if upper bounds defined
        Type[] ubounds = wildcard.getUpperBounds();
        if (ubounds.length == 1) {
            return new GenericType(this.getRootType(), ubounds[0]);
        }

        // TODO: handle lower bounds??

        // no bounds specified, so default to object
        return new GenericType(Object.class);
    }
}
