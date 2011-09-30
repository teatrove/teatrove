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

import java.util.*;

/**
 * 
 * @author Brian S O'Neill
 */
public class CodeDisassembler {
    private final MethodInfo mMethod;
    private final String mEnclosingClassName;
    private final String mSuperClassName;
    private final CodeAttr mCode;
    private final ConstantPool mCp;
    private final byte[] mByteCodes;
    private final ExceptionHandler[] mExceptionHandlers;

    // Current CodeAssembler in use for disassembly.
    private CodeAssembler mAssembler;

    // List of all the LocalVariable objects in use.
    private Vector mLocals;

    // True if the method being decompiled still has a "this" reference.
    private boolean mHasThis;

    // Maps Integer address keys to itself, but to Label objects after first
    // needed.
    private Map mLabels;

    // Maps Integer catch locations to Lists of ExceptionHandler objects.
    private Map mCatchLocations;

    // Current address being decompiled.
    int mAddress;

    public CodeDisassembler(MethodInfo method) {
        mMethod = method;
        mEnclosingClassName = method.getClassFile().getClassName();
        mSuperClassName = method.getClassFile().getSuperClassName();
        mCode = method.getCodeAttr();
        mCp = mCode.getConstantPool();
        CodeBuffer buffer = mCode.getCodeBuffer();
        mByteCodes = buffer.getByteCodes();
        mExceptionHandlers = buffer.getExceptionHandlers();
    }

    /**
     * Disassemble the MethodInfo into the given assembler.
     *
     * @see CodeAssemblerPrinter
     */
    public synchronized void disassemble(CodeAssembler assembler) {
        mAssembler = assembler;
        mLocals = new Vector();
        mHasThis = !mMethod.getModifiers().isStatic();

        gatherLabels();

        // Gather the local variables of the parameters.
        LocalVariable[] paramVars = assembler.getParameters();
        for (int i=0; i<paramVars.length; i++) {
            LocalVariable paramVar = paramVars[i];
            int number = paramVar.getNumber();
            if (number >= mLocals.size()) {
                mLocals.setSize(number + 1);
            }
            mLocals.setElementAt(paramVar, number);
        }

        Location currentLoc = new Location() {
            public int getLocation() {
                return mAddress;
            }

            public int compareTo(Object obj) {
                if (this == obj) {
                    return 0;
                }
                Location other = (Location)obj;
                
                int loca = getLocation();
                int locb = other.getLocation();
                
                if (loca < locb) {
                    return -1;
                }
                else if (loca > locb) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        };

        int currentLine = -1;

        for (mAddress = 0; mAddress < mByteCodes.length; mAddress++) {
            int nextLine = mCode.getLineNumber(currentLoc);
            if (nextLine != currentLine) {
                if ((currentLine = nextLine) >= 0) {
                    mAssembler.mapLineNumber(currentLine);
                }
            }

            // Check if a label needs to be created and/or located.
            locateLabel();

            byte opcode = mByteCodes[mAddress];

            int index;
            Location loc;
            TypeDesc type;
            ConstantInfo ci;

            switch (opcode) {

            default:
                // TODO: raise an error.
                break;

                // Opcodes with no operands...

            case Opcode.NOP:
                assembler.nop();
                break;
            case Opcode.BREAKPOINT:
                assembler.breakpoint();
                break;

            case Opcode.ACONST_NULL:
                assembler.loadConstant(null);
                break;
            case Opcode.ICONST_M1:
                assembler.loadConstant(-1);
                break;
            case Opcode.ICONST_0:
                assembler.loadConstant(0);
                break;
            case Opcode.ICONST_1:
                assembler.loadConstant(1);
                break;
            case Opcode.ICONST_2:
                assembler.loadConstant(2);
                break;
            case Opcode.ICONST_3:
                assembler.loadConstant(3);
                break;
            case Opcode.ICONST_4:
                assembler.loadConstant(4);
                break;
            case Opcode.ICONST_5:
                assembler.loadConstant(5);
                break;
            case Opcode.LCONST_0:
                assembler.loadConstant(0L);
                break;
            case Opcode.LCONST_1:
                assembler.loadConstant(1L);
                break;
            case Opcode.FCONST_0:
                assembler.loadConstant(0f);
                break;
            case Opcode.FCONST_1:
                assembler.loadConstant(1f);
                break;
            case Opcode.FCONST_2:
                assembler.loadConstant(2f);
                break;
            case Opcode.DCONST_0:
                assembler.loadConstant(0d);
                break;
            case Opcode.DCONST_1:
                assembler.loadConstant(1d);
                break;

            case Opcode.POP:
                assembler.pop();
                break;
            case Opcode.POP2:
                assembler.pop2();
                break;
            case Opcode.DUP:
                assembler.dup();
                break;
            case Opcode.DUP_X1:
                assembler.dupX1();
                break;
            case Opcode.DUP_X2:
                assembler.dupX2();
                break;
            case Opcode.DUP2:
                assembler.dup2();
                break;
            case Opcode.DUP2_X1:
                assembler.dup2X2();
                break;
            case Opcode.DUP2_X2:
                assembler.dup2X2();
                break;
            case Opcode.SWAP:
                assembler.swap();
                break;

            case Opcode.IADD:  case Opcode.LADD: 
            case Opcode.FADD:  case Opcode.DADD:
            case Opcode.ISUB:  case Opcode.LSUB:
            case Opcode.FSUB:  case Opcode.DSUB:
            case Opcode.IMUL:  case Opcode.LMUL:
            case Opcode.FMUL:  case Opcode.DMUL:
            case Opcode.IDIV:  case Opcode.LDIV:
            case Opcode.FDIV:  case Opcode.DDIV:
            case Opcode.IREM:  case Opcode.LREM:
            case Opcode.FREM:  case Opcode.DREM:
            case Opcode.INEG:  case Opcode.LNEG:
            case Opcode.FNEG:  case Opcode.DNEG:
            case Opcode.ISHL:  case Opcode.LSHL:
            case Opcode.ISHR:  case Opcode.LSHR:
            case Opcode.IUSHR: case Opcode.LUSHR:
            case Opcode.IAND:  case Opcode.LAND:
            case Opcode.IOR:   case Opcode.LOR:
            case Opcode.IXOR:  case Opcode.LXOR:
            case Opcode.FCMPL: case Opcode.DCMPL:
            case Opcode.FCMPG: case Opcode.DCMPG:
            case Opcode.LCMP: 
                assembler.math(opcode);
                break;

            case Opcode.I2L:
                assembler.convert(TypeDesc.INT, TypeDesc.LONG);
                break;
            case Opcode.I2F:
                assembler.convert(TypeDesc.INT, TypeDesc.FLOAT);
                break;
            case Opcode.I2D:
                assembler.convert(TypeDesc.INT, TypeDesc.DOUBLE);
                break;
            case Opcode.L2I:
                assembler.convert(TypeDesc.LONG, TypeDesc.INT);
                break;
            case Opcode.L2F:
                assembler.convert(TypeDesc.LONG, TypeDesc.FLOAT);
                break;
            case Opcode.L2D:
                assembler.convert(TypeDesc.LONG, TypeDesc.DOUBLE);
                break;
            case Opcode.F2I:
                assembler.convert(TypeDesc.FLOAT, TypeDesc.INT);
                break;
            case Opcode.F2L:
                assembler.convert(TypeDesc.FLOAT, TypeDesc.LONG);
                break;
            case Opcode.F2D:
                assembler.convert(TypeDesc.FLOAT, TypeDesc.DOUBLE);
                break;
            case Opcode.D2I:
                assembler.convert(TypeDesc.DOUBLE, TypeDesc.INT);
                break;
            case Opcode.D2L:
                assembler.convert(TypeDesc.DOUBLE, TypeDesc.LONG);
                break;
            case Opcode.D2F:
                assembler.convert(TypeDesc.DOUBLE, TypeDesc.FLOAT);
                break;
            case Opcode.I2B:
                assembler.convert(TypeDesc.INT, TypeDesc.BYTE);
                break;
            case Opcode.I2C:
                assembler.convert(TypeDesc.INT, TypeDesc.CHAR);
                break;
            case Opcode.I2S:
                assembler.convert(TypeDesc.INT, TypeDesc.SHORT);
                break;

            case Opcode.IRETURN:
                assembler.returnValue(TypeDesc.INT);
                break;
            case Opcode.LRETURN:
                assembler.returnValue(TypeDesc.LONG);
                break;
            case Opcode.FRETURN:
                assembler.returnValue(TypeDesc.FLOAT);
                break;
            case Opcode.DRETURN:
                assembler.returnValue(TypeDesc.DOUBLE);
                break;
            case Opcode.ARETURN:
                assembler.returnValue(TypeDesc.OBJECT);
                break;
            case Opcode.RETURN:
                assembler.returnVoid();
                break;

            case Opcode.IALOAD:
                assembler.loadFromArray(TypeDesc.INT);
                break;
            case Opcode.LALOAD:
                assembler.loadFromArray(TypeDesc.LONG);
                break;
            case Opcode.FALOAD:
                assembler.loadFromArray(TypeDesc.FLOAT);
                break;
            case Opcode.DALOAD:
                assembler.loadFromArray(TypeDesc.DOUBLE);
                break;
            case Opcode.AALOAD:
                assembler.loadFromArray(TypeDesc.OBJECT);
                break;
            case Opcode.BALOAD:
                assembler.loadFromArray(TypeDesc.BYTE);
                break;
            case Opcode.CALOAD:
                assembler.loadFromArray(TypeDesc.CHAR);
                break;
            case Opcode.SALOAD:
                assembler.loadFromArray(TypeDesc.SHORT);
                break;

            case Opcode.IASTORE:
                assembler.storeToArray(TypeDesc.INT);
                break;
            case Opcode.LASTORE:
                assembler.storeToArray(TypeDesc.LONG);
                break;
            case Opcode.FASTORE:
                assembler.storeToArray(TypeDesc.FLOAT);
                break;
            case Opcode.DASTORE:
                assembler.storeToArray(TypeDesc.DOUBLE);
                break;
            case Opcode.AASTORE:
                assembler.storeToArray(TypeDesc.OBJECT);
                break;
            case Opcode.BASTORE:
                assembler.storeToArray(TypeDesc.BYTE);
                break;
            case Opcode.CASTORE:
                assembler.storeToArray(TypeDesc.CHAR);
                break;
            case Opcode.SASTORE:
                assembler.storeToArray(TypeDesc.SHORT);
                break;

            case Opcode.ARRAYLENGTH:
                assembler.arrayLength();
                break;
            case Opcode.ATHROW:
                assembler.throwObject();
                break;
            case Opcode.MONITORENTER:
                assembler.monitorEnter();
                break;
            case Opcode.MONITOREXIT:
                assembler.monitorExit();
                break;

                // End opcodes with no operands.

                // Opcodes that load a constant from the constant pool...
                
            case Opcode.LDC:
            case Opcode.LDC_W:
            case Opcode.LDC2_W:
                switch (opcode) {
                case Opcode.LDC:
                    index = readUnsignedByte();
                    break;
                case Opcode.LDC_W:
                case Opcode.LDC2_W:
                    index = readUnsignedShort();
                    break;
                default:
                    index = 0;
                    break;
                }

                ci = mCp.getConstant(index);

                if (ci instanceof ConstantStringInfo) {
                    assembler.loadConstant
                        (((ConstantStringInfo)ci).getValue());
                }
                else if (ci instanceof ConstantIntegerInfo) {
                    assembler.loadConstant
                        (((ConstantIntegerInfo)ci).getValue().intValue());
                }
                else if (ci instanceof ConstantLongInfo) {
                    assembler.loadConstant
                        (((ConstantLongInfo)ci).getValue().longValue());
                }
                else if (ci instanceof ConstantFloatInfo) {
                    assembler.loadConstant
                        (((ConstantFloatInfo)ci).getValue().floatValue());
                }
                else if (ci instanceof ConstantDoubleInfo) {
                    assembler.loadConstant
                        (((ConstantDoubleInfo)ci).getValue().doubleValue());
                }
                else {
                    // TODO: raise an error.
                }
                break;

            case Opcode.NEW:
                ci = mCp.getConstant(readUnsignedShort());

                if (ci instanceof ConstantClassInfo) {
                    assembler.newObject(((ConstantClassInfo)ci).getType());
                }
                else {
                    // TODO: raise an error.
                }
                break;
            case Opcode.ANEWARRAY:
                ci = mCp.getConstant(readUnsignedShort());
                if (ci instanceof ConstantClassInfo) {
                    type = ((ConstantClassInfo)ci).getType().toArrayType();
                    assembler.newObject(type);
                }
                else {
                    // TODO: raise an error.
                }
                break;
            case Opcode.MULTIANEWARRAY:
                ci = mCp.getConstant(readUnsignedShort());
                int dims = readUnsignedByte();
                if (ci instanceof ConstantClassInfo) {
                    type = ((ConstantClassInfo)ci).getType();
                    assembler.newObject(type, dims);
                }
                else {
                    // TODO: raise an error.
                }
                break;

            case Opcode.CHECKCAST:
                ci = mCp.getConstant(readUnsignedShort());

                if (ci instanceof ConstantClassInfo) {
                    assembler.checkCast(((ConstantClassInfo)ci).getType());
                }
                else {
                    // TODO: raise an error.
                }
                break;
            case Opcode.INSTANCEOF:
                ci = mCp.getConstant(readUnsignedShort());

                if (ci instanceof ConstantClassInfo) {
                    assembler.instanceOf(((ConstantClassInfo)ci).getType());
                }
                else {
                    // TODO: raise an error.
                }
                break;

            case Opcode.GETSTATIC:
            case Opcode.PUTSTATIC:
            case Opcode.GETFIELD:
            case Opcode.PUTFIELD:
                ci = mCp.getConstant(readUnsignedShort());
                if (!(ci instanceof ConstantFieldInfo)) {
                    // TODO: raise an error.
                    break;
                }
                ConstantFieldInfo field = (ConstantFieldInfo)ci;
                String className =
                    field.getParentClass().getType().getRootName();
                if (mEnclosingClassName.equals(className)) {
                    className = null;
                }
                String fieldName = field.getNameAndType().getName();
                Descriptor desc = field.getNameAndType().getType();
                if (!(desc instanceof TypeDesc)) {
                    // TODO: raise an error.
                    break;
                }
                else {
                    type = (TypeDesc)desc;
                }

                // Implementation note: Although it may seem convenient if the
                // CodeAssembler had methods that accepted ConstantFieldInfo
                // objects as parameters, it would cause problems because
                // ConstantPools are not portable between ClassFiles.
                
                switch (opcode) {
                case Opcode.GETSTATIC:
                    if (className == null) {
                        assembler.loadStaticField(fieldName, type);
                    }
                    else {
                        assembler.loadStaticField(className, fieldName, type);
                    }
                    break;
                case Opcode.PUTSTATIC:
                    if (className == null) {
                        assembler.storeStaticField(fieldName, type);
                    }
                    else {
                        assembler.storeStaticField(className, fieldName, type);
                    }
                    break;
                case Opcode.GETFIELD:
                    if (className == null) {
                        assembler.loadField(fieldName, type);
                    }
                    else {
                        assembler.loadField(className, fieldName, type);
                    }
                    break;
                case Opcode.PUTFIELD:
                    if (className == null) {
                        assembler.storeField(fieldName, type);
                    }
                    else {
                        assembler.storeField(className, fieldName, type);
                    }
                    break;
                }
                break;

            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKESTATIC:
            case Opcode.INVOKEINTERFACE:
                ci = mCp.getConstant(readUnsignedShort());

                ConstantNameAndTypeInfo nameAndType;

                if (opcode == Opcode.INVOKEINTERFACE) {
                    // Read and ignore nargs and padding byte.
                    readShort();
                    if (!(ci instanceof ConstantInterfaceMethodInfo)) {
                        // TODO: raise an error.
                        break;
                    }
                    ConstantInterfaceMethodInfo method = 
                        (ConstantInterfaceMethodInfo)ci;
                    className =
                        method.getParentClass().getType().getRootName();
                    nameAndType = method.getNameAndType();
                }
                else {
                    if (!(ci instanceof ConstantMethodInfo)) {
                        // TODO: raise an error.
                        break;
                    }
                    ConstantMethodInfo method = 
                        (ConstantMethodInfo)ci;
                    className =
                        method.getParentClass().getType().getRootName();
                    if (mEnclosingClassName.equals(className)) {
                        className = null;
                    }
                    nameAndType = method.getNameAndType();
                }

                String methodName = nameAndType.getName();
                desc = nameAndType.getType();
                if (!(desc instanceof MethodDesc)) {
                    // TODO: raise an error.
                    break;
                }
                TypeDesc ret = ((MethodDesc)desc).getReturnType();
                if (ret == TypeDesc.VOID) {
                    ret = null;
                }
                TypeDesc[] params = ((MethodDesc)desc).getParameterTypes();
                if (params.length == 0) {
                    params = null;
                }

                switch (opcode) {
                case Opcode.INVOKEVIRTUAL:
                    if (className == null) {
                        assembler.invokeVirtual(methodName, ret, params);
                    }
                    else {
                        assembler.invokeVirtual
                            (className, methodName, ret, params);
                    }
                    break;
                case Opcode.INVOKESPECIAL:
                    if ("<init>".equals(methodName)) {
                        if (className == null) {
                            assembler.invokeConstructor(params);
                        }
                        else {
                            if (className.equals(mSuperClassName)) {
                                assembler.invokeSuperConstructor(params);
                            }
                            else {
                                assembler.invokeConstructor(className, params);
                            }
                        }
                    }
                    else {
                        if (className == null) {
                            assembler.invokePrivate(methodName, ret, params);
                        }
                        else {
                            assembler.invokeSuper
                                (className, methodName, ret, params);
                        }
                    }
                    break;
                case Opcode.INVOKESTATIC:
                    if (className == null) {
                        assembler.invokeStatic(methodName, ret, params);
                    }
                    else {
                        assembler.invokeStatic
                            (className, methodName, ret, params);
                    }
                    break;
                case Opcode.INVOKEINTERFACE:
                    assembler.invokeInterface
                        (className, methodName, ret, params);
                    break;
                }
                break;

                // End opcodes that load a constant from the constant pool.

                // Opcodes that load or store local variables...

            case Opcode.ILOAD: case Opcode.ISTORE:
            case Opcode.LLOAD: case Opcode.LSTORE:
            case Opcode.FLOAD: case Opcode.FSTORE:
            case Opcode.DLOAD: case Opcode.DSTORE:
            case Opcode.ALOAD: case Opcode.ASTORE:
            case Opcode.ILOAD_0: case Opcode.ISTORE_0:
            case Opcode.ILOAD_1: case Opcode.ISTORE_1:
            case Opcode.ILOAD_2: case Opcode.ISTORE_2:
            case Opcode.ILOAD_3: case Opcode.ISTORE_3:
            case Opcode.LLOAD_0: case Opcode.LSTORE_0:
            case Opcode.LLOAD_1: case Opcode.LSTORE_1:
            case Opcode.LLOAD_2: case Opcode.LSTORE_2:
            case Opcode.LLOAD_3: case Opcode.LSTORE_3:
            case Opcode.FLOAD_0: case Opcode.FSTORE_0:
            case Opcode.FLOAD_1: case Opcode.FSTORE_1:
            case Opcode.FLOAD_2: case Opcode.FSTORE_2:
            case Opcode.FLOAD_3: case Opcode.FSTORE_3:
            case Opcode.DLOAD_0: case Opcode.DSTORE_0:
            case Opcode.DLOAD_1: case Opcode.DSTORE_1:
            case Opcode.DLOAD_2: case Opcode.DSTORE_2:
            case Opcode.DLOAD_3: case Opcode.DSTORE_3:
            case Opcode.ALOAD_0: case Opcode.ASTORE_0:
            case Opcode.ALOAD_1: case Opcode.ASTORE_1:
            case Opcode.ALOAD_2: case Opcode.ASTORE_2:
            case Opcode.ALOAD_3: case Opcode.ASTORE_3:
                switch (opcode) {
                case Opcode.ILOAD: case Opcode.ISTORE:
                    index = readUnsignedByte();
                    type = TypeDesc.INT;
                    break;
                case Opcode.LLOAD: case Opcode.LSTORE:
                    index = readUnsignedByte();
                    type = TypeDesc.LONG;
                    break;
                case Opcode.FLOAD: case Opcode.FSTORE:
                    index = readUnsignedByte();
                    type = TypeDesc.FLOAT;
                    break;
                case Opcode.DLOAD: case Opcode.DSTORE:
                    index = readUnsignedByte();
                    type = TypeDesc.DOUBLE;
                    break;
                case Opcode.ALOAD: case Opcode.ASTORE:
                    index = readUnsignedByte();
                    type = TypeDesc.OBJECT;
                    break;
                case Opcode.ILOAD_0: case Opcode.ISTORE_0:
                    index = 0;
                    type = TypeDesc.INT;
                    break;
                case Opcode.ILOAD_1: case Opcode.ISTORE_1:
                    index = 1;
                    type = TypeDesc.INT;
                    break;
                case Opcode.ILOAD_2: case Opcode.ISTORE_2:
                    index = 2;
                    type = TypeDesc.INT;
                    break;
                case Opcode.ILOAD_3: case Opcode.ISTORE_3:
                    index = 3;
                    type = TypeDesc.INT;
                    break;
                case Opcode.LLOAD_0: case Opcode.LSTORE_0:
                    index = 0;
                    type = TypeDesc.LONG;
                    break;
                case Opcode.LLOAD_1: case Opcode.LSTORE_1:
                    index = 1;
                    type = TypeDesc.LONG;
                    break;
                case Opcode.LLOAD_2: case Opcode.LSTORE_2:
                    index = 2;
                    type = TypeDesc.LONG;
                    break;
                case Opcode.LLOAD_3: case Opcode.LSTORE_3:
                    index = 3;
                    type = TypeDesc.LONG;
                    break;
                case Opcode.FLOAD_0: case Opcode.FSTORE_0:
                    index = 0;
                    type = TypeDesc.FLOAT;
                    break;
                case Opcode.FLOAD_1: case Opcode.FSTORE_1:
                    index = 1;
                    type = TypeDesc.FLOAT;
                    break;
                case Opcode.FLOAD_2: case Opcode.FSTORE_2:
                    index = 2;
                    type = TypeDesc.FLOAT;
                    break;
                case Opcode.FLOAD_3: case Opcode.FSTORE_3:
                    index = 3;
                    type = TypeDesc.FLOAT;
                    break;
                case Opcode.DLOAD_0: case Opcode.DSTORE_0:
                    index = 0;
                    type = TypeDesc.DOUBLE;
                    break;
                case Opcode.DLOAD_1: case Opcode.DSTORE_1:
                    index = 1;
                    type = TypeDesc.DOUBLE;
                    break;
                case Opcode.DLOAD_2: case Opcode.DSTORE_2:
                    index = 2;
                    type = TypeDesc.DOUBLE;
                    break;
                case Opcode.DLOAD_3: case Opcode.DSTORE_3:
                    index = 3;
                    type = TypeDesc.DOUBLE;
                    break;
                case Opcode.ALOAD_0: case Opcode.ASTORE_0:
                    index = 0;
                    type = TypeDesc.OBJECT;
                    break;
                case Opcode.ALOAD_1: case Opcode.ASTORE_1:
                    index = 1;
                    type = TypeDesc.OBJECT;
                    break;
                case Opcode.ALOAD_2: case Opcode.ASTORE_2:
                    index = 2;
                    type = TypeDesc.OBJECT;
                    break;
                case Opcode.ALOAD_3: case Opcode.ASTORE_3:
                    index = 3;
                    type = TypeDesc.OBJECT;
                    break;
                default:
                    index = 0;
                    type = null;
                    break;
                }

                switch (opcode) {
                case Opcode.ILOAD:
                case Opcode.LLOAD:
                case Opcode.FLOAD:
                case Opcode.DLOAD:
                case Opcode.ALOAD:
                case Opcode.ILOAD_0:
                case Opcode.ILOAD_1:
                case Opcode.ILOAD_2:
                case Opcode.ILOAD_3:
                case Opcode.LLOAD_0:
                case Opcode.LLOAD_1:
                case Opcode.LLOAD_2:
                case Opcode.LLOAD_3:
                case Opcode.FLOAD_0:
                case Opcode.FLOAD_1:
                case Opcode.FLOAD_2:
                case Opcode.FLOAD_3:
                case Opcode.DLOAD_0:
                case Opcode.DLOAD_1:
                case Opcode.DLOAD_2:
                case Opcode.DLOAD_3:
                case Opcode.ALOAD_0:
                case Opcode.ALOAD_1:
                case Opcode.ALOAD_2:
                case Opcode.ALOAD_3:
                    if (index == 0 && mHasThis) {
                        assembler.loadThis();
                    }
                    else {
                        assembler.loadLocal(getLocalVariable(index, type));
                    }
                    break;
                case Opcode.ISTORE:
                case Opcode.LSTORE:
                case Opcode.FSTORE:
                case Opcode.DSTORE:
                case Opcode.ASTORE:
                case Opcode.ISTORE_0:
                case Opcode.ISTORE_1:
                case Opcode.ISTORE_2:
                case Opcode.ISTORE_3:
                case Opcode.LSTORE_0:
                case Opcode.LSTORE_1:
                case Opcode.LSTORE_2:
                case Opcode.LSTORE_3:
                case Opcode.FSTORE_0:
                case Opcode.FSTORE_1:
                case Opcode.FSTORE_2:
                case Opcode.FSTORE_3:
                case Opcode.DSTORE_0:
                case Opcode.DSTORE_1:
                case Opcode.DSTORE_2:
                case Opcode.DSTORE_3:
                case Opcode.ASTORE_0:
                case Opcode.ASTORE_1:
                case Opcode.ASTORE_2:
                case Opcode.ASTORE_3:
                    if (index == 0 && mHasThis) {
                        // The "this" reference just got blown away.
                        mHasThis = false;
                    }
                    assembler.storeLocal(getLocalVariable(index, type));
                    break;
                }
                break;

            case Opcode.RET:
                LocalVariable local = getLocalVariable
                    (readUnsignedByte(), TypeDesc.OBJECT);
                assembler.ret(local);
                break;

            case Opcode.IINC:
                local = getLocalVariable(readUnsignedByte(), TypeDesc.INT);
                assembler.integerIncrement(local, readByte());
                break;

                // End opcodes that load or store local variables.

                // Opcodes that branch to another address.

            case Opcode.GOTO:
                loc = getLabel(mAddress + readShort());
                assembler.branch(loc);
                break;
            case Opcode.JSR:
                loc = getLabel(mAddress + readShort());
                assembler.jsr(loc);
                break;
            case Opcode.GOTO_W:
                loc = getLabel(mAddress + readInt());
                assembler.branch(loc);
                break;
            case Opcode.JSR_W:
                loc = getLabel(mAddress + readInt());
                assembler.jsr(loc);
                break;

            case Opcode.IFNULL:
                loc = getLabel(mAddress + readShort());
                assembler.ifNullBranch(loc, true);
                break;
            case Opcode.IFNONNULL:
                loc = getLabel(mAddress + readShort());
                assembler.ifNullBranch(loc, false);
                break;

            case Opcode.IF_ACMPEQ:
                loc = getLabel(mAddress + readShort());
                assembler.ifEqualBranch(loc, true);
                break;
            case Opcode.IF_ACMPNE:
                loc = getLabel(mAddress + readShort());
                assembler.ifEqualBranch(loc, false);
                break;

            case Opcode.IFEQ:
            case Opcode.IFNE:
            case Opcode.IFLT:
            case Opcode.IFGE:
            case Opcode.IFGT:
            case Opcode.IFLE:
                loc = getLabel(mAddress + readShort());
                String choice;
                switch (opcode) {
                case Opcode.IFEQ:
                    choice = "==";
                    break;
                case Opcode.IFNE:
                    choice = "!=";
                    break;
                case Opcode.IFLT:
                    choice = "<";
                    break;
                case Opcode.IFGE:
                    choice = ">=";
                    break;
                case Opcode.IFGT:
                    choice = ">";
                    break;
                case Opcode.IFLE:
                    choice = "<=";
                    break;
                default:
                    choice = null;
                    break;
                }
                assembler.ifZeroComparisonBranch(loc, choice);
                break;

            case Opcode.IF_ICMPEQ:
            case Opcode.IF_ICMPNE:
            case Opcode.IF_ICMPLT:
            case Opcode.IF_ICMPGE:
            case Opcode.IF_ICMPGT:
            case Opcode.IF_ICMPLE:
                loc = getLabel(mAddress + readShort());
                switch (opcode) {
                case Opcode.IF_ICMPEQ:
                    choice = "==";
                    break;
                case Opcode.IF_ICMPNE:
                    choice = "!=";
                    break;
                case Opcode.IF_ICMPLT:
                    choice = "<";
                    break;
                case Opcode.IF_ICMPGE:
                    choice = ">=";
                    break;
                case Opcode.IF_ICMPGT:
                    choice = ">";
                    break;
                case Opcode.IF_ICMPLE:
                    choice = "<=";
                    break;
                default:
                    choice = null;
                    break;
                }
                assembler.ifComparisonBranch(loc, choice);
                break;

                // End opcodes that branch to another address.

                // Miscellaneous opcodes...

            case Opcode.BIPUSH:
                assembler.loadConstant(readByte());
                break;
            case Opcode.SIPUSH:
                assembler.loadConstant(readShort());
                break;

            case Opcode.NEWARRAY:
                int atype = readByte();
                type = null;
                switch (atype) {
                case 4: // T_BOOLEAN
                    type = TypeDesc.BOOLEAN;
                    break;
                case 5: // T_CHAR
                    type = TypeDesc.CHAR;
                    break;
                case 6: // T_FLOAT
                    type = TypeDesc.FLOAT;
                    break;
                case 7: // T_DOUBLE
                    type = TypeDesc.DOUBLE;
                    break;
                case 8: // T_BYTE
                    type = TypeDesc.BYTE;
                    break;
                case 9: // T_SHORT
                    type = TypeDesc.SHORT;
                    break;
                case 10: // T_INT
                    type = TypeDesc.INT;
                    break;
                case 11: // T_LONG
                    type = TypeDesc.LONG;
                    break;
                }

                if (type == null) {
                    // TODO: raise an error.
                    break;
                }

                assembler.newObject(type.toArrayType());
                break;

            case Opcode.TABLESWITCH:
            case Opcode.LOOKUPSWITCH:
                int opcodeAddress = mAddress;
                // Read padding until address is 32 bit word aligned.
                while (((mAddress + 1) & 3) != 0) {
                    ++mAddress;
                }
                Location defaultLocation = getLabel(opcodeAddress + readInt());
                int[] cases;
                Location[] locations;
                
                if (opcode == Opcode.TABLESWITCH) {
                    int lowValue = readInt();
                    int highValue = readInt();
                    int caseCount = highValue - lowValue + 1;
                    try {
                        cases = new int[caseCount];
                    }
                    catch (NegativeArraySizeException e) {
                        // TODO: raise an error.
                        break;
                    }
                    locations = new Location[caseCount];
                    for (int i=0; i<caseCount; i++) {
                        cases[i] = lowValue + i;
                        locations[i] = getLabel(opcodeAddress + readInt());
                    }
                }
                else {
                    int caseCount = readInt();
                    try {
                        cases = new int[caseCount];
                    }
                    catch (NegativeArraySizeException e) {
                        // TODO: raise an error.
                        break;
                    }
                    locations = new Location[caseCount];
                    for (int i=0; i<caseCount; i++) {
                        cases[i] = readInt();
                        locations[i] = getLabel(opcodeAddress + readInt());
                    }
                }

                assembler.switchBranch(cases, locations, defaultLocation);
                break;

            case Opcode.WIDE:
                opcode = mByteCodes[++mAddress];
                switch (opcode) {

                default:
                    // TODO: raise an error.
                    break;

                case Opcode.ILOAD: case Opcode.ISTORE:
                case Opcode.LLOAD: case Opcode.LSTORE:
                case Opcode.FLOAD: case Opcode.FSTORE:
                case Opcode.DLOAD: case Opcode.DSTORE:
                case Opcode.ALOAD: case Opcode.ASTORE:

                    switch (opcode) {
                    case Opcode.ILOAD: case Opcode.ISTORE:
                        type = TypeDesc.INT;
                        break;
                    case Opcode.LLOAD: case Opcode.LSTORE:
                        type = TypeDesc.LONG;
                        break;
                    case Opcode.FLOAD: case Opcode.FSTORE:
                        type = TypeDesc.FLOAT;
                        break;
                    case Opcode.DLOAD: case Opcode.DSTORE:
                        type = TypeDesc.DOUBLE;
                        break;
                    case Opcode.ALOAD: case Opcode.ASTORE:
                        type = TypeDesc.OBJECT;
                        break;
                    default:
                        type = null;
                        break;
                    }
                    
                    index = readUnsignedShort();

                    switch (opcode) {
                    case Opcode.ILOAD:
                    case Opcode.LLOAD:
                    case Opcode.FLOAD:
                    case Opcode.DLOAD:
                    case Opcode.ALOAD:
                        if (index == 0 && mHasThis) {
                            assembler.loadThis();
                        }
                        else {
                            assembler.loadLocal(getLocalVariable(index, type));
                        }
                        break;
                    case Opcode.ISTORE:
                    case Opcode.LSTORE:
                    case Opcode.FSTORE:
                    case Opcode.DSTORE:
                    case Opcode.ASTORE:
                        if (index == 0 && mHasThis) {
                            // The "this" reference just got blown away.
                            mHasThis = false;
                        }
                        assembler.storeLocal(getLocalVariable(index, type));
                        break;
                    }
                    break;

                case Opcode.RET:
                    local = getLocalVariable
                        (readUnsignedShort(), TypeDesc.OBJECT);
                    assembler.ret(local);
                    break;
                    
                case Opcode.IINC:
                    local = getLocalVariable
                        (readUnsignedShort(), TypeDesc.INT);
                    assembler.integerIncrement(local, readShort());
                    break;
                }

                break;
            } // end huge switch
        } // end for loop
    }

    private void gatherLabels() {
        mLabels = new HashMap();
        mCatchLocations = new HashMap(mExceptionHandlers.length * 2 + 1);
        Integer labelKey;

        // Gather labels for any exception handlers.
        for (int i = mExceptionHandlers.length - 1; i >= 0; i--) {
            ExceptionHandler handler = mExceptionHandlers[i];
            labelKey = new Integer(handler.getStartLocation().getLocation());
            mLabels.put(labelKey, labelKey);
            labelKey = new Integer(handler.getEndLocation().getLocation());
            mLabels.put(labelKey, labelKey);
            labelKey = new Integer(handler.getCatchLocation().getLocation());
            List list = (List)mCatchLocations.get(labelKey);
            if (list == null) {
                list = new ArrayList(2);
                mCatchLocations.put(labelKey, list);
            }
            list.add(handler);
        }

        // Now gather labels that occur within byte code.
        for (mAddress = 0; mAddress < mByteCodes.length; mAddress++) {
            byte opcode = mByteCodes[mAddress];

            switch (opcode) {

            default:
                // TODO: raise an error.
                break;

                // Opcodes that use labels.

            case Opcode.GOTO:
            case Opcode.JSR:
            case Opcode.IFNULL:
            case Opcode.IFNONNULL:
            case Opcode.IF_ACMPEQ:
            case Opcode.IF_ACMPNE:
            case Opcode.IFEQ:
            case Opcode.IFNE:
            case Opcode.IFLT:
            case Opcode.IFGE:
            case Opcode.IFGT:
            case Opcode.IFLE:
            case Opcode.IF_ICMPEQ:
            case Opcode.IF_ICMPNE:
            case Opcode.IF_ICMPLT:
            case Opcode.IF_ICMPGE:
            case Opcode.IF_ICMPGT:
            case Opcode.IF_ICMPLE:
                labelKey = new Integer(mAddress + readShort());
                mLabels.put(labelKey, labelKey);
                break;

            case Opcode.GOTO_W:
            case Opcode.JSR_W:
                labelKey = new Integer(mAddress + readInt());
                mLabels.put(labelKey, labelKey);
                break;

            case Opcode.TABLESWITCH:
            case Opcode.LOOKUPSWITCH:
                int opcodeAddress = mAddress;
                // Read padding until address is 32 bit word aligned.
                while (((mAddress + 1) & 3) != 0) {
                    ++mAddress;
                }
                
                // Read the default location.
                labelKey = new Integer(opcodeAddress + readInt());
                mLabels.put(labelKey, labelKey);
                
                if (opcode == Opcode.TABLESWITCH) {
                    int lowValue = readInt();
                    int highValue = readInt();
                    int caseCount = highValue - lowValue + 1;

                    for (int i=0; i<caseCount; i++) {
                        // Read the branch location.
                        labelKey = new Integer(opcodeAddress + readInt());
                        mLabels.put(labelKey, labelKey);
                    }
                }
                else {
                    int caseCount = readInt();

                    for (int i=0; i<caseCount; i++) {
                        // Skip the case value.
                        mAddress += 4;
                        // Read the branch location.
                        labelKey = new Integer(opcodeAddress + readInt());
                        mLabels.put(labelKey, labelKey);
                    }
                }
                break;

                // All other operations are skipped. The amount to skip
                // depends on the operand size.

                // Opcodes with no operands...

            case Opcode.NOP:
            case Opcode.BREAKPOINT:
            case Opcode.ACONST_NULL:
            case Opcode.ICONST_M1:
            case Opcode.ICONST_0:
            case Opcode.ICONST_1:
            case Opcode.ICONST_2:
            case Opcode.ICONST_3:
            case Opcode.ICONST_4:
            case Opcode.ICONST_5:
            case Opcode.LCONST_0:
            case Opcode.LCONST_1:
            case Opcode.FCONST_0:
            case Opcode.FCONST_1:
            case Opcode.FCONST_2:
            case Opcode.DCONST_0:
            case Opcode.DCONST_1:
            case Opcode.POP:
            case Opcode.POP2:
            case Opcode.DUP:
            case Opcode.DUP_X1:
            case Opcode.DUP_X2:
            case Opcode.DUP2:
            case Opcode.DUP2_X1:
            case Opcode.DUP2_X2:
            case Opcode.SWAP:
            case Opcode.IADD:  case Opcode.LADD: 
            case Opcode.FADD:  case Opcode.DADD:
            case Opcode.ISUB:  case Opcode.LSUB:
            case Opcode.FSUB:  case Opcode.DSUB:
            case Opcode.IMUL:  case Opcode.LMUL:
            case Opcode.FMUL:  case Opcode.DMUL:
            case Opcode.IDIV:  case Opcode.LDIV:
            case Opcode.FDIV:  case Opcode.DDIV:
            case Opcode.IREM:  case Opcode.LREM:
            case Opcode.FREM:  case Opcode.DREM:
            case Opcode.INEG:  case Opcode.LNEG:
            case Opcode.FNEG:  case Opcode.DNEG:
            case Opcode.ISHL:  case Opcode.LSHL:
            case Opcode.ISHR:  case Opcode.LSHR:
            case Opcode.IUSHR: case Opcode.LUSHR:
            case Opcode.IAND:  case Opcode.LAND:
            case Opcode.IOR:   case Opcode.LOR:
            case Opcode.IXOR:  case Opcode.LXOR:
            case Opcode.FCMPL: case Opcode.DCMPL:
            case Opcode.FCMPG: case Opcode.DCMPG:
            case Opcode.LCMP: 
            case Opcode.I2L:
            case Opcode.I2F:
            case Opcode.I2D:
            case Opcode.L2I:
            case Opcode.L2F:
            case Opcode.L2D:
            case Opcode.F2I:
            case Opcode.F2L:
            case Opcode.F2D:
            case Opcode.D2I:
            case Opcode.D2L:
            case Opcode.D2F:
            case Opcode.I2B:
            case Opcode.I2C:
            case Opcode.I2S:
            case Opcode.IRETURN:
            case Opcode.LRETURN:
            case Opcode.FRETURN:
            case Opcode.DRETURN:
            case Opcode.ARETURN:
            case Opcode.RETURN:
            case Opcode.IALOAD:
            case Opcode.LALOAD:
            case Opcode.FALOAD:
            case Opcode.DALOAD:
            case Opcode.AALOAD:
            case Opcode.BALOAD:
            case Opcode.CALOAD:
            case Opcode.SALOAD:
            case Opcode.IASTORE:
            case Opcode.LASTORE:
            case Opcode.FASTORE:
            case Opcode.DASTORE:
            case Opcode.AASTORE:
            case Opcode.BASTORE:
            case Opcode.CASTORE:
            case Opcode.SASTORE:
            case Opcode.ARRAYLENGTH:
            case Opcode.ATHROW:
            case Opcode.MONITORENTER:
            case Opcode.MONITOREXIT:
            case Opcode.ILOAD_0: case Opcode.ISTORE_0:
            case Opcode.ILOAD_1: case Opcode.ISTORE_1:
            case Opcode.ILOAD_2: case Opcode.ISTORE_2:
            case Opcode.ILOAD_3: case Opcode.ISTORE_3:
            case Opcode.LLOAD_0: case Opcode.LSTORE_0:
            case Opcode.LLOAD_1: case Opcode.LSTORE_1:
            case Opcode.LLOAD_2: case Opcode.LSTORE_2:
            case Opcode.LLOAD_3: case Opcode.LSTORE_3:
            case Opcode.FLOAD_0: case Opcode.FSTORE_0:
            case Opcode.FLOAD_1: case Opcode.FSTORE_1:
            case Opcode.FLOAD_2: case Opcode.FSTORE_2:
            case Opcode.FLOAD_3: case Opcode.FSTORE_3:
            case Opcode.DLOAD_0: case Opcode.DSTORE_0:
            case Opcode.DLOAD_1: case Opcode.DSTORE_1:
            case Opcode.DLOAD_2: case Opcode.DSTORE_2:
            case Opcode.DLOAD_3: case Opcode.DSTORE_3:
            case Opcode.ALOAD_0: case Opcode.ASTORE_0:
            case Opcode.ALOAD_1: case Opcode.ASTORE_1:
            case Opcode.ALOAD_2: case Opcode.ASTORE_2:
            case Opcode.ALOAD_3: case Opcode.ASTORE_3:
                break;

                // Opcodes with one operand byte...

            case Opcode.LDC:
            case Opcode.ILOAD: case Opcode.ISTORE:
            case Opcode.LLOAD: case Opcode.LSTORE:
            case Opcode.FLOAD: case Opcode.FSTORE:
            case Opcode.DLOAD: case Opcode.DSTORE:
            case Opcode.ALOAD: case Opcode.ASTORE:
            case Opcode.RET:
            case Opcode.IINC:
            case Opcode.BIPUSH:
            case Opcode.NEWARRAY:
                mAddress += 1;
                break;

                // Opcodes with two operand bytes...

            case Opcode.LDC_W:
            case Opcode.LDC2_W:
            case Opcode.NEW:
            case Opcode.ANEWARRAY:
            case Opcode.CHECKCAST:
            case Opcode.INSTANCEOF:
            case Opcode.GETSTATIC:
            case Opcode.PUTSTATIC:
            case Opcode.GETFIELD:
            case Opcode.PUTFIELD:
            case Opcode.INVOKEVIRTUAL:
            case Opcode.INVOKESPECIAL:
            case Opcode.INVOKESTATIC:
            case Opcode.SIPUSH:
                mAddress += 2;
                break;

                // Opcodes with three operand bytes...

            case Opcode.MULTIANEWARRAY:
                mAddress += 3;
                break;

                // Opcodes with four operand bytes...

            case Opcode.INVOKEINTERFACE:
                mAddress += 4;
                break;

                // Wide opcode has a variable sized operand.

            case Opcode.WIDE:
                opcode = mByteCodes[++mAddress];
                mAddress += 2;
                if (opcode == Opcode.IINC) {
                    mAddress += 2;
                }
                break;
            } // end huge switch
        } // end for loop
    }

    private int readByte() {
        return mByteCodes[++mAddress];
    }

    private int readUnsignedByte() {
        return mByteCodes[++mAddress] & 0xff;
    }

    private int readShort() {
        return (mByteCodes[++mAddress] << 8) | (mByteCodes[++mAddress] & 0xff);
    }

    private int readUnsignedShort() {
        return 
            ((mByteCodes[++mAddress] & 0xff) << 8) | 
            ((mByteCodes[++mAddress] & 0xff) << 0);
    }

    private int readInt() {
        return
            (mByteCodes[++mAddress] << 24) | 
            ((mByteCodes[++mAddress] & 0xff) << 16) |
            ((mByteCodes[++mAddress] & 0xff) << 8) |
            ((mByteCodes[++mAddress] & 0xff) << 0);
    }

    private LocalVariable getLocalVariable(int index, TypeDesc type) {
        LocalVariable local;

        if (index >= mLocals.size()) {
            mLocals.setSize(index + 1);
            local = null;
        }
        else {
            local = (LocalVariable)mLocals.elementAt(index);
            if (local != null) {
                TypeDesc localType = local.getType();
                if (type != localType &&
                    (type.isPrimitive() || localType.isPrimitive())) {
                    // Existing type at this index isn't compatible.
                    local = null;
                }
            }
        }

        if (local == null) {
            local = mAssembler.createLocalVariable(null, type);
            mLocals.setElementAt(local, index);
        }

        return local;
    }

    private void locateLabel() {
        Integer labelKey = new Integer(mAddress);
        Object labelValue = mLabels.get(labelKey);
        if (labelValue != null) {
            if (labelValue instanceof Label) {
                ((Label)labelValue).setLocation();
            }
            else {
                labelValue = mAssembler.createLabel().setLocation();
                mLabels.put(labelKey, labelValue);
            }
        }

        List handlers = (List)mCatchLocations.get(labelKey);

        if (handlers != null) {
            for (int i=0; i<handlers.size(); i++) {
                ExceptionHandler handler = (ExceptionHandler)handlers.get(i);
                Label start =
                    getLabel(handler.getStartLocation().getLocation());
                Label end =
                    getLabel(handler.getEndLocation().getLocation());
                String catchClassName;
                if (handler.getCatchType() == null) {
                    catchClassName = null;
                }
                else {
                    catchClassName =
                        handler.getCatchType().getType().getRootName();
                }
                mAssembler.exceptionHandler(start, end, catchClassName);
            }
        }
    }

    private Label getLabel(int address) {
        Integer labelKey = new Integer(address);
        Object labelValue = mLabels.get(labelKey);
        // labelValue will never be null unless gatherLabels is broken.
        if (!(labelValue instanceof Label)) {
            labelValue = mAssembler.createLabel();
            mLabels.put(labelKey, labelValue);
        }
        return (Label)labelValue;
    }
}
