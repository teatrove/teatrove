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

package org.teatrove.tea.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.teatrove.trove.generics.GenericType;

/**
 * A special kind of PropertyDescriptor provided by {@link BeanAnaliyzer}.
 * Keyed properties are named "[]" and may accept any type of key. A keyed
 * property is usually signified by the following method signature:
 * <tt>public&nbsp;&lt;propertyType&gt;&nbsp;get(&lt;keyType&gt;);</tt>.
 * Arrays, Strings and Lists always have keyed properties with a key
 * type of int.
 *
 * @author Brian S O'Neill
 */
public class KeyedPropertyDescriptor extends PropertyDescriptor {
    private GenericType mPropertyType;
    private Method[] mKeyedGetters;

    KeyedPropertyDescriptor() throws IntrospectionException {
        super(BeanAnalyzer.KEYED_PROPERTY_NAME, null, null);
    }

    /**
     * A null element indicates that an array lookup should be performed.
     */
    public Method[] getKeyedReadMethods() {
        return (Method[])mKeyedGetters.clone();
    }

    void setKeyedReadMethods(Method[] keyedGetters) {
        mKeyedGetters = keyedGetters;
    }

    public GenericType getKeyedPropertyType() {
        return mPropertyType;
    }

    void setKeyedPropertyType(GenericType propertyType) {
        mPropertyType = propertyType;
    }
}
