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

/**
 * 
 *
 * @author Nick Hagan
 */
public class ClassHandler
    extends AbstractGenericHandler<Class<?>> {

    private boolean resolved;
    private GenericType rawType;
    private int dimensions;
    private GenericType componentType;
    private GenericType rootComponentType;

    public ClassHandler(GenericType rootType,
                        Class<?> genericType) {
        super(rootType, genericType);
    }

    @Override
    public GenericType getRawType() {
        resolve();
        return this.rawType;
    }

    @Override
    public int getDimensions() {
        resolve();
        return this.dimensions;
    }

    @Override
    public GenericType getComponentType() {
        resolve();
        return this.componentType;
    }

    @Override
    public GenericType getRootComponentType() {
        resolve();
        return this.rootComponentType;
    }

    protected void resolve() {
        if (!this.resolved) {
            this.resolve0();
            this.resolved = true;
        }
    }

    protected void resolve0() {
        // resolve component type
        this.componentType = new GenericType
        (
            this.getGenericType().getComponentType()
        );

        // save raw type
        this.rawType = new GenericType(this.getGenericType());

        // find root component type and number of dimensions
        this.dimensions = 0;
        Class<?> compType = this.getGenericType();
        while (compType.isArray()) {
            this.dimensions++;
            compType = compType.getComponentType();
        }

        // save root component type
        GenericType rootType = this.getRootType();
        this.rootComponentType = new GenericType(rootType, compType);
    }
}
