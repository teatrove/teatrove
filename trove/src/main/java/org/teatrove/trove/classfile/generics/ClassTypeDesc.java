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

/**
 * 
 *
 * @author Nick Hagan
 */
public class ClassTypeDesc
    extends AbstractGenericTypeDesc<Class<?>>
{
    public static ClassTypeDesc OBJECT_TYPE = 
        ClassTypeDesc.forType(Object.class);
    
    private final String className;
    private boolean isInterface;

    public static ClassTypeDesc forType(Class<?> type) {
        return InternFactory.intern(new ClassTypeDesc(type));
    }

    public static ClassTypeDesc forType(String className) {
        return InternFactory.intern(new ClassTypeDesc(className));
    }

    public static ClassTypeDesc forType(String className, boolean isInterface) {
        return InternFactory.intern(new ClassTypeDesc(className, isInterface));
    }

    protected ClassTypeDesc(String className) {
        this.className = className;
        try { this.isInterface = Class.forName(className).isInterface(); }
        catch (Exception e) {
            // class not found, so assument non-interface
            this.isInterface = false;
        }
    }

    protected ClassTypeDesc(Class<?> type) {
        this(type.getName(), type.isInterface());
    }

    protected ClassTypeDesc(String className, boolean isInterface) {
        this.className = className;
        this.isInterface = isInterface;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean isInterface() {
        return this.isInterface;
    }

    public String getSignature() {
        return getSignature(true);
    }

    protected String getSignature(boolean terminate) {
        String name = this.getClassName();

        if ("byte".equals(name) || "B".equals(name)) { return "B"; }
        else if ("char".equals(name) || "C".equals(name)) { return "C"; }
        else if ("double".equals(name) || "D".equals(name)) { return "D"; }
        else if ("float".equals(name) || "F".equals(name)) { return "F"; }
        else if ("int".equals(name) || "I".equals(name)) { return "I"; }
        else if ("long".equals(name) || "J".equals(name)) { return "J"; }
        else if ("short".equals(name) || "S".equals(name)) { return "S"; }
        else if ("boolean".equals(name) || "Z".equals(name)) { return "Z"; }
        else if ("void".equals(name) || "V".equals(name)) { return "V"; }

        StringBuilder buffer = new StringBuilder(128);
        buffer.append('L')
              .append(this.getClassName().replace('.', '/'));

        if (terminate) { buffer.append(';'); }
        return buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (!(other instanceof ClassTypeDesc)) { return false; }

        ClassTypeDesc type = (ClassTypeDesc) other;
        return this.className.equals(type.className);
    }

    @Override
    public int hashCode() {
        return this.className.hashCode();
    }

    @Override
    public String toString() {
        return (this.isInterface() ? "interface " : "class ")
            .concat(this.getClassName());
    }
}
