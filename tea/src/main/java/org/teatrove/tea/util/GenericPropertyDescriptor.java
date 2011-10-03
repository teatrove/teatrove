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
import java.beans.PropertyEditor;
import java.lang.reflect.Method;
import java.util.Enumeration;

import org.teatrove.trove.generics.GenericType;

/**
 * 
 * @author Nick Hagan
 */
public class GenericPropertyDescriptor extends PropertyDescriptor {

    private GenericType rootType;
    private GenericType genericType;
    private PropertyDescriptor property;

    public GenericPropertyDescriptor(PropertyDescriptor property)
        throws IntrospectionException {
        super(property.getName(), null, null);
        this.property = property;
    }

    public GenericPropertyDescriptor(GenericType rootType,
                                     PropertyDescriptor property)
        throws IntrospectionException {
        super(property.getName(), null, null);
        this.rootType = rootType;
        this.property = property;
    }

    @Override
    public synchronized Class<?> getPropertyType() {
        resolvePropertyType();
        return this.genericType.getRawType().getType();
    }

    public GenericType getGenericPropertyType() {
        resolvePropertyType();
        return this.genericType;
    }

    protected void resolvePropertyType() {
        if (this.genericType == null) {
            resolvePropertyType0();
        }
    }

    protected void resolvePropertyType0() {
        // get underlying property type to ensure valid
        Class<?> propertyType = this.property.getPropertyType();

        // resolve from read method if available
        Method readMethod = this.property.getReadMethod();
        if (readMethod != null) {
            this.genericType = new GenericType
            (
                this.rootType,
                readMethod.getReturnType(), readMethod.getGenericReturnType()
            );
        }

        // otherwise, resolve from write method if available
        else {
            Method writeMethod = this.property.getWriteMethod();
            if (writeMethod != null) {
                this.genericType = new GenericType
                (
                    this.rootType,
                    writeMethod.getParameterTypes()[0],
                    writeMethod.getGenericParameterTypes()[0]
                );
            }

            // otherwise, default to underlying type
            else {
                this.genericType = new GenericType(this.rootType, propertyType);
            }
        }
    }

    @Override
    public synchronized Method getReadMethod() {
        return this.property.getReadMethod();
    }

    @Override
    public synchronized void setReadMethod(Method readMethod)
        throws IntrospectionException {
        if (this.property != null) {
            this.property.setReadMethod(readMethod);
        }
    }

    @Override
    public synchronized Method getWriteMethod() {
        return this.property.getWriteMethod();
    }

    @Override
    public synchronized void setWriteMethod(Method writeMethod)
        throws IntrospectionException {
        if (this.property != null) {
            this.property.setWriteMethod(writeMethod);
        }
    }

    @Override
    public boolean isBound() {
        return this.property.isBound();
    }

    @Override
    public void setBound(boolean bound) {
        this.property.setBound(bound);
    }

    @Override
    public boolean isConstrained() {
        return this.property.isConstrained();
    }

    @Override
    public void setConstrained(boolean constrained) {
        this.property.setConstrained(constrained);
    }

    @Override
    public void setPropertyEditorClass(Class<?> propertyEditorClass) {
        this.property.setPropertyEditorClass(propertyEditorClass);
    }

    @Override
    public Class<?> getPropertyEditorClass() {
        return this.property.getPropertyEditorClass();
    }

    @Override
    public PropertyEditor createPropertyEditor(Object bean) {
        return this.property.createPropertyEditor(bean);
    }

    @Override
    public String getName() {
        return this.property.getName();
    }

    @Override
    public void setName(String name) {
        if (this.property != null) {
            this.property.setName(name);
        }
    }

    @Override
    public String getDisplayName() {
        return this.property.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        this.property.setDisplayName(displayName);
    }

    @Override
    public boolean isExpert() {
        return this.property.isExpert();
    }

    @Override
    public void setExpert(boolean expert) {
        this.property.setExpert(expert);
    }

    @Override
    public boolean isHidden() {
        return this.property.isHidden();
    }

    @Override
    public void setHidden(boolean hidden) {
        this.property.setHidden(hidden);
    }

    @Override
    public boolean isPreferred() {
        return this.property.isPreferred();
    }

    @Override
    public void setPreferred(boolean preferred) {
        this.property.setPreferred(preferred);
    }

    @Override
    public String getShortDescription() {
        return this.property.getShortDescription();
    }

    @Override
    public void setShortDescription(String text) {
        this.property.setShortDescription(text);
    }

    @Override
    public void setValue(String attributeName, Object value) {
        this.property.setValue(attributeName, value);
    }

    @Override
    public Object getValue(String attributeName) {
        return this.property.getValue(attributeName);
    }

    @Override
    public Enumeration<String> attributeNames() {
        return this.property.attributeNames();
    }

    @Override
    public String toString() {
        return this.property.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this.property.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.property.hashCode();
    }
}
