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

package org.teatrove.trove.classfile;

import org.teatrove.trove.util.FlyweightSet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to build method descriptor strings as 
 * defined in <i>The Java Virtual Machine Specification</i>, section 4.3.3.
 * MethodDesc instances are canonicalized and therefore "==" comparable.
 *
 * @author Brian S O'Neill
 */
public class MethodDesc extends Descriptor implements Serializable {
    private static final TypeDesc[] EMPTY_PARAMS = new TypeDesc[0];
    private static final String[] EMPTY_PARAM_NAMES = new String[0];

    // MethodDesc and TypeDesc can share the same instance cache.
    private final static FlyweightSet mInstances = TypeDesc.cInstances;

    static MethodDesc intern(MethodDesc desc) {
        return (MethodDesc)mInstances.put(desc);
    }

    /**
     * Acquire a MethodDesc from a set of arguments.
     * @param ret return type of method; null implies void
     * @param params parameters to method; null implies none
     */
    public static MethodDesc forArguments(TypeDesc ret, TypeDesc... params) {
        final String[] paramNames = createGenericParameterNames(params);
        return forArguments(ret, params, paramNames);
    }

    static String[] createGenericParameterNames(TypeDesc... params) {
        String[] paramNames = null;
        if(params != null) {
            paramNames = new String[params.length];
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = "param$" + i;
            }
        }
        return paramNames;
    }

    /**
     * Acquire a MethodDesc from a set of arguments.
     * @param ret return type of method; null implies void
     * @param params parameters to method; null implies none
     * @param paramNames parameter names to method; null implies none
     */
    public static MethodDesc forArguments(TypeDesc ret, TypeDesc[] params, String[] paramNames) {
        if (ret == null) {
            ret = TypeDesc.VOID;
        }
        if (params == null || params.length == 0) {
            params = EMPTY_PARAMS;
        }
        if (paramNames == null || paramNames.length == 0) {
            paramNames = EMPTY_PARAM_NAMES;
        }
        return intern(new MethodDesc(ret, params, paramNames));
    }

    /**
     * Acquire a MethodDesc from a type descriptor. This syntax is described in
     * section 4.3.3, Method Descriptors.
     */
    public static MethodDesc forDescriptor(String desc) 
        throws IllegalArgumentException
    {
        try {
            int cursor = 0;
            char c;

            if ((c = desc.charAt(cursor++)) != '(') {
                throw invalidDescriptor(desc);
            }

            StringBuffer buf = new StringBuffer();
            List list = new ArrayList();

            while ((c = desc.charAt(cursor++)) != ')') {
                switch (c) {
                case 'V':
                case 'I':
                case 'C':
                case 'Z':
                case 'D':
                case 'F':
                case 'J':
                case 'B':
                case 'S':
                    buf.append(c);
                    break;
                case '[':
                    buf.append(c);
                    continue;
                case 'L':
                    while (true) {
                        buf.append(c);
                        if (c == ';') {
                            break;
                        }
                        c = desc.charAt(cursor++);
                    }
                    break;
                default:
                    throw invalidDescriptor(desc);
                }

                list.add(TypeDesc.forDescriptor(buf.toString()));
                buf.setLength(0);
            }

            TypeDesc ret = TypeDesc.forDescriptor(desc.substring(cursor));

            TypeDesc[] tds = new TypeDesc[list.size()];
            tds = (TypeDesc[])list.toArray(tds);

            return intern(new MethodDesc(desc, ret, tds, new String[list.size()]));
        }
        catch (NullPointerException e) {
            throw invalidDescriptor(desc);
        }
        catch (IndexOutOfBoundsException e) {
            throw invalidDescriptor(desc);
        }
    }

    private static IllegalArgumentException invalidDescriptor(String desc) {
        return new IllegalArgumentException("Invalid descriptor: " + desc);
    }

    private transient final String mDescriptor;
    private transient final TypeDesc mRetType;
    private transient final TypeDesc[] mParams;
    private transient final String[] mParamNames;

    private MethodDesc(TypeDesc ret, TypeDesc[] params, String[] paramNames) {
        this(generateDescriptor(ret, params), ret, params, paramNames);
    }

    private MethodDesc(String desc, TypeDesc ret, TypeDesc[] params, String[] paramNames) {
        mDescriptor = desc;
        mRetType = ret;
        mParams = params;
        mParamNames = paramNames;
    }

    /**
     * Returns the described return type, which is TypeDesc.VOID if void.
     */
    public TypeDesc getReturnType() {
        return mRetType;
    }

    public int getParameterCount() {
        return mParams.length;
    }

    public TypeDesc[] getParameterTypes() {
        TypeDesc[] params = mParams;
        return (params != EMPTY_PARAMS) ? (TypeDesc[])params.clone() : params;
    }

    public String[] getParameterNames() {
        String[] paramNames = mParamNames;
        return (paramNames != EMPTY_PARAM_NAMES) ? (String[])paramNames.clone() : paramNames;
    }

    /**
     * Returns this in Java method signature syntax.
     * @param name method name
     */
    public String toMethodSignature(String name) {
        StringBuffer buf = new StringBuffer();
        buf.append(mRetType.getFullName());
        buf.append(' ');
        buf.append(name);
        buf.append('(');

        TypeDesc[] params = mParams;
        for (int i=0; i<params.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(params[i].getFullName());
        }

        return buf.append(')').toString();
    }

    /**
     * Returns this in method descriptor syntax.
     */
    public String toString() {
        return mDescriptor;
    }

    public int hashCode() {
        return mDescriptor.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof MethodDesc) {
            return ((MethodDesc)other).mDescriptor.equals(mDescriptor);
        }
        return false;
    }

    Object writeReplace() throws ObjectStreamException {
        return new External(mDescriptor);
    }

    private static String generateDescriptor(TypeDesc ret, TypeDesc[] params) {
        int length = ret.toString().length() + 2;
        int paramsLength = params.length;
        for (int i=paramsLength; --i >=0; ) {
            length += params[i].toString().length();
        }
        char[] buf = new char[length];
        buf[0] = '(';
        int index = 1;
        String paramDesc;
        for (int i=0; i<paramsLength; i++) {
            paramDesc = params[i].toString();
            int paramDescLength = paramDesc.length();
            paramDesc.getChars(0, paramDescLength, buf, index);
            index += paramDescLength;
        }
        buf[index++] = ')';
        paramDesc = ret.toString();
        paramDesc.getChars(0, paramDesc.length(), buf, index);
        return new String(buf);
    }

    private static class External implements Externalizable {
        private String mDescriptor;

        public External() {
        }

        public External(String desc) {
            mDescriptor = desc;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(mDescriptor);
        }

        public void readExternal(ObjectInput in) throws IOException {
            mDescriptor = in.readUTF();
        }

        public Object readResolve() throws ObjectStreamException {
            return forDescriptor(mDescriptor);
        }
    }
}
