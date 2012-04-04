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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

import org.teatrove.trove.classfile.generics.ClassTypeDesc;
import org.teatrove.trove.classfile.generics.GenericTypeDesc;
import org.teatrove.trove.classfile.generics.ParameterizedTypeDesc;
import org.teatrove.trove.classfile.generics.TypeVariableDesc;
import org.teatrove.trove.util.FlyweightSet;

/**
 * 
 *
 * @author Nick Hagan
 */
public class SignatureDesc {
    // SignatureDesc and TypeDesc can share the same instance cache.
    private final static FlyweightSet mInstances = TypeDesc.cInstances;

    static SignatureDesc intern(SignatureDesc desc) {
        return (SignatureDesc)mInstances.put(desc);
    }

    public static SignatureDesc forClass(TypeVariableDesc[] typeArguments,
                                         GenericTypeDesc superClass,
                                         GenericTypeDesc... interfaces) {
        return intern
        (
            new SignatureDesc
            (
                generateClassDescriptor(typeArguments,
                                        superClass, interfaces)
            )
        );
    }

    public static SignatureDesc forMethod(GenericTypeDesc returnType,
                                          GenericTypeDesc... paramTypes) {
        return forMethod(null, returnType, paramTypes);
    }

    public static SignatureDesc forMethod(TypeVariableDesc[] typeArguments,
                                          GenericTypeDesc returnType,
                                          GenericTypeDesc... paramTypes) {
        return intern
        (
            new SignatureDesc
            (
                generateMethodDescriptor(typeArguments, returnType, paramTypes)
            )
        );
    }

    public static SignatureDesc forMethod(TypeDesc returnType,
                                          TypeDesc... paramTypes) {
        return forMethod(null, returnType, paramTypes);
    }

    public static SignatureDesc forMethod(TypeVariableDesc[] typeArguments,
                                          TypeDesc returnType,
                                          TypeDesc... paramTypes) {
        return intern
        (
            new SignatureDesc
            (
                generateMethodDescriptor(typeArguments, returnType, paramTypes)
            )
        );
    }

    private transient final String mDescriptor;

    private SignatureDesc(String desc) {
        mDescriptor = desc;
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
        if (other instanceof SignatureDesc) {
            return ((SignatureDesc)other).mDescriptor.equals(mDescriptor);
        }
        return false;
    }

    Object writeReplace() throws ObjectStreamException {
        return new External(mDescriptor);
    }

    public static String generateMethodDescriptor(TypeVariableDesc[] typeArguments,
                                                  TypeDesc returnType,
                                                  TypeDesc[] paramTypes) {
        StringBuilder buffer = new StringBuilder(256);

        appendTypeArguments(buffer, typeArguments);
        buffer.append('(');
        appendTypes(buffer, paramTypes);
        buffer.append(')');
        appendType(buffer, returnType);

        return buffer.toString();
    }

    public static String generateMethodDescriptor(TypeVariableDesc[] typeArguments,
                                                  GenericTypeDesc returnType,
                                                  GenericTypeDesc[] paramTypes) {
        StringBuilder buffer = new StringBuilder(256);

        appendTypeArguments(buffer, typeArguments);
        buffer.append('(');
        appendTypes(buffer, paramTypes);
        buffer.append(')');
        appendType(buffer, returnType);

        return buffer.toString();
    }

    public static String generateClassDescriptor(TypeVariableDesc[] typeArguments,
                                                 GenericTypeDesc superClass,
                                                 GenericTypeDesc[] interfaces) {
        if (superClass == null) {
            superClass = ClassTypeDesc.OBJECT_TYPE;
        }
        
        StringBuilder buffer = new StringBuilder(256);

        appendTypeArguments(buffer, typeArguments);
        appendType(buffer, superClass);
        appendTypes(buffer, interfaces);

        return buffer.toString();
    }

    protected static void
    appendTypeArguments(StringBuilder buffer,
                        TypeVariableDesc[] typeArguments) {
        if (typeArguments != null && typeArguments.length > 0) {
            buffer.append('<');
            for (TypeVariableDesc typeArgument : typeArguments) {
                buffer.append(typeArgument.getName()).append(':');

                GenericTypeDesc bounds = typeArgument.getBounds();
                if (bounds == null) {
                    bounds = ClassTypeDesc.forType(Object.class);
                }

                if (bounds instanceof ClassTypeDesc) {
                    ClassTypeDesc desc = ((ClassTypeDesc) bounds);
                    if (desc.isInterface()) {
                        buffer.append(':');
                    }
                }
                else if (bounds instanceof ParameterizedTypeDesc) {
                    ParameterizedTypeDesc desc =
                        ((ParameterizedTypeDesc) bounds);

                    if (desc.getRawType().isInterface()) {
                        buffer.append(':');
                    }
                }

                appendType(buffer, bounds);
            }
            buffer.append('>');
        }
    }

    protected static void appendTypes(StringBuilder buffer,
                                      TypeDesc[] types) {
        if (types != null && types.length > 0) {
            for (TypeDesc type : types) {
                appendType(buffer, type);
            }
        }
    }

    protected static void appendTypes(StringBuilder buffer,
                                      GenericTypeDesc[] types) {
        if (types != null && types.length > 0) {
            for (GenericTypeDesc type : types) {
                appendType(buffer, type);
            }
        }
    }

    protected static void appendType(StringBuilder buffer,
                                     TypeDesc type) {
        buffer.append((type == null ? TypeDesc.VOID : type).getSignature());
    }

    protected static void appendType(StringBuilder buffer,
                                     GenericTypeDesc type) {
        buffer.append(type.getSignature());
    }

    private static class External implements Externalizable {
        private String mDescriptor;

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
            throw new UnsupportedOperationException("SignatureDesc.readResolve");
        }
    }
}
