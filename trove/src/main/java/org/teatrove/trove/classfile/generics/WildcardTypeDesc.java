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

package org.teatrove.trove.classfile.generics;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 
 *
 * @author Nick Hagan
 */
public class WildcardTypeDesc
    extends AbstractGenericTypeDesc<WildcardType>
{
    private final GenericTypeDesc lowerBounds;
    private final GenericTypeDesc upperBounds;

    public static WildcardTypeDesc forType(GenericTypeDesc lowerBounds,
                                           GenericTypeDesc upperBounds) {
        return InternFactory.intern(new WildcardTypeDesc(lowerBounds, upperBounds));
    }

    public static WildcardTypeDesc forType(WildcardType type) {
        return InternFactory.intern(new WildcardTypeDesc(type));
    }

    protected WildcardTypeDesc(GenericTypeDesc lowerBounds,
                               GenericTypeDesc upperBounds) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
    }

    protected WildcardTypeDesc(WildcardType type) {
        Type[] lbounds = type.getLowerBounds();
        if (lbounds != null && lbounds.length > 0) {
            this.lowerBounds = GenericTypeFactory.fromType(lbounds[0]);
        } else { this.lowerBounds = null; }

        Type[] ubounds = type.getUpperBounds();
        if (ubounds != null && ubounds.length > 0) {
            this.upperBounds = GenericTypeFactory.fromType(ubounds[0]);
        } else { this.upperBounds = null; }
    }

    public GenericTypeDesc getLowerBounds() {
        return this.lowerBounds;
    }

    public GenericTypeDesc getUpperBounds() {
        return this.upperBounds;
    }

    public String getSignature() {
        StringBuilder buffer = new StringBuilder(256);

        boolean found = false;
        if (this.lowerBounds != null) {
            found = true;
            if (this.lowerBounds instanceof ClassTypeDesc) {
                ClassTypeDesc clazz = (ClassTypeDesc) this.lowerBounds;
                String name = clazz.getClassName();
                if (Object.class.getName().equals(name)) {
                    found = false;
                }
            }

            if (found) {
                buffer.append('-').append(this.lowerBounds.getSignature());
            }
        }

        else if (this.upperBounds != null) {
            found = true;
            if (this.upperBounds instanceof ClassTypeDesc) {
                ClassTypeDesc clazz = (ClassTypeDesc) this.upperBounds;
                String name = clazz.getClassName();
                if (Object.class.getName().equals(name)) {
                    found = false;
                }
            }

            if (found) {
                buffer.append('+').append(this.upperBounds.getSignature());
            }
        }

        if (!found) {
            buffer.append('*');
        }

        return buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (!(other instanceof WildcardTypeDesc)) { return false; }

        WildcardTypeDesc type = (WildcardTypeDesc) other;
        return ((this.lowerBounds == null && type.lowerBounds == null) ||
                (this.lowerBounds != null &&
                    this.lowerBounds.equals(type.lowerBounds))) &&
               ((this.upperBounds == null && type.upperBounds == null) ||
                (this.upperBounds != null &&
                    this.upperBounds.equals(type.upperBounds)));
    }

    @Override
    public int hashCode() {
        return (this.lowerBounds == null ? 0 : this.lowerBounds.hashCode() * 11) +
               (this.upperBounds == null ? 0 : this.upperBounds.hashCode() * 17);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(256);

        buffer.append("?");
        if (this.lowerBounds != null) {
            buffer.append(" super ").append(this.lowerBounds.toString());
        }

        else if (this.upperBounds != null) {
            buffer.append(" extends ").append(this.upperBounds.toString());
        }

        return buffer.toString();
    }
}
