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

import java.lang.reflect.*;

/**
 * This class is used as an aid in generating code for a method.
 * It controls the max stack, local variable allocation, labels and bytecode.
 *
 * @author Brian S O'Neill, Nick Hagan
 */
public class CodeBuilder implements CodeBuffer, CodeAssembler {
    private String mName;
    private CodeAttr mCodeAttr;
    private ClassFile mClassFile;
    private ConstantPool mCp;

    private InstructionList mInstructions = new InstructionList();

    private LocalVariable mThisReference;
    private LocalVariable[] mParameters;

    private boolean mSaveLineNumberInfo;
    private boolean mSaveLocalVariableInfo;

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     */
    public CodeBuilder(MethodInfo info) {
        this(info, true, false);
    }

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     *
     * @param saveLineNumberInfo When set false, all calls to mapLineNumber
     * are ignored. By default, this value is true.
     *
     * @param saveLocalVariableInfo When set true, all local variable
     * usage information is saved in the ClassFile. By default, this value
     * is false.
     *
     * @see #mapLineNumber
     */
    public CodeBuilder(MethodInfo info, boolean saveLineNumberInfo,
                       boolean saveLocalVariableInfo) {
        mName = info.getName();
        mCodeAttr = info.getCodeAttr();
        mClassFile = info.getClassFile();
        mCp = mClassFile.getConstantPool();

        mCodeAttr.setCodeBuffer(this);

        mSaveLineNumberInfo = saveLineNumberInfo;
        mSaveLocalVariableInfo = saveLocalVariableInfo;

        // Create LocalVariable references for "this" reference and other
        // passed in parameters.

        LocalVariable localVar;
        int varNum = 0;

        if (!info.getModifiers().isStatic()) {
            localVar = mInstructions.createLocalParameter
                ("this", mClassFile.getType(), varNum++);
            mThisReference = localVar;

            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }

        TypeDesc[] paramTypes = info.getMethodDescriptor().getParameterTypes();
        String[] paramNames = info.getMethodDescriptor().getParameterNames();
        int paramSize = paramTypes.length;

        mParameters = new LocalVariable[paramSize];

        for (int i = 0; i<paramTypes.length; i++) {
            localVar = mInstructions.createLocalParameter
                (paramNames[i], paramTypes[i], varNum);
            varNum += (localVar.isDoubleWord() ? 2 : 1);
            mParameters[i] = localVar;

            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }
    }

    public int getMaxStackDepth() {
        return mInstructions.getMaxStackDepth();
    }

    public int getMaxLocals() {
        return mInstructions.getMaxLocals();
    }

    public byte[] getByteCodes() {
        try {
            return mInstructions.getByteCodes();
        }
        catch (Exception exception) {
            StringBuilder error = new StringBuilder();
            error.append("error generating code: ")
                 .append(mClassFile.getClassName()).append('.').append(mName)
                 .append('(');
            
            for (int i = 0; i < mParameters.length; i++) {
                if (i > 0) { error.append(", "); }
                error.append(mParameters[i].getType()).append(' ')
                     .append(mParameters[i].getName());
            }
            
            error.append(')');
            throw new RuntimeException(error.toString(), exception);
        }
    }

    public ExceptionHandler[] getExceptionHandlers() {
        return mInstructions.getExceptionHandlers();
    }

    private void addCode(int stackAdjust, byte opcode) {
        mInstructions.new CodeInstruction(stackAdjust, new byte[] {opcode});
    }

    private void addCode(int stackAdjust, byte opcode, byte operand) {
        mInstructions.new CodeInstruction
            (stackAdjust, new byte[] {opcode, operand});
    }

    private void addCode(int stackAdjust, byte opcode, short operand) {
        mInstructions.new CodeInstruction
            (stackAdjust,
             new byte[] {opcode, (byte)(operand >> 8), (byte)operand});
    }

// This method may be needed at some point in the future!
//    private void addCode(int stackAdjust, byte opcode, int operand) {
//        byte[] bytes = new byte[5];
//
//        bytes[0] = opcode;
//        bytes[1] = (byte)(operand >> 24);
//        bytes[2] = (byte)(operand >> 16);
//        bytes[3] = (byte)(operand >> 8);
//        bytes[4] = (byte)operand;
//
//        mInstructions.new CodeInstruction(stackAdjust, bytes);
//    }

    private void addCode(int stackAdjust, byte opcode, ConstantInfo info) {
        // The zeros get filled in later, when the ConstantInfo index
        // is resolved.
        mInstructions.new ConstantOperandInstruction
            (stackAdjust,
             new byte[] {opcode, (byte)0, (byte)0}, info);
    }

    public LocalVariable[] getParameters() {
        return (LocalVariable[])mParameters.clone();
    }

    public LocalVariable createLocalVariable(String name, TypeDesc type) {
        LocalVariable localVar = mInstructions.createLocalVariable(name, type);

        if (mSaveLocalVariableInfo) {
            mCodeAttr.localVariableUse(localVar);
        }

        return localVar;
    }

    public Label createLabel() {
        return mInstructions.new LabelInstruction();
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        Location catchLocation = createLabel().setLocation();

        ConstantClassInfo catchClass;
        if (catchClassName == null) {
            catchClass = null;
        }
        else {
            catchClass = ConstantClassInfo.make(mCp, catchClassName);
        }

        ExceptionHandler handler =
            new ExceptionHandler(startLocation, endLocation,
                                 catchLocation, catchClass);

        mInstructions.addExceptionHandler(handler);
    }

    public void mapLineNumber(int lineNumber) {
        if (mSaveLineNumberInfo) {
            mCodeAttr.mapLineNumber(createLabel().setLocation(), lineNumber);
        }
    }

    // load-constant-to-stack style instructions

    public void loadNull() {
        addCode(1, Opcode.ACONST_NULL);
    }
    
    public void loadConstant(String value) {
        if (value == null) {
            loadNull();
            return;
        }

        int strlen = value.length();

        if (strlen <= (65535 / 3)) {
            // Guaranteed to fit in a Java UTF encoded string.
            ConstantInfo info = ConstantStringInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
            return;
        }

        // Compute actual UTF length.

        int utflen = 0;

        for (int i=0; i<strlen; i++) {
            int c = value.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            }
            else if (c > 0x07FF) {
                utflen += 3;
            }
            else {
                utflen += 2;
            }
        }

        if (utflen <= 65535) {
            ConstantInfo info = ConstantStringInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
            return;
        }

        // Break string up into chunks and construct in a StringBuffer.

        TypeDesc stringBufferDesc = TypeDesc.forClass(StringBuffer.class);

        TypeDesc intDesc = TypeDesc.INT;
        TypeDesc stringDesc = TypeDesc.STRING;
        TypeDesc[] stringParam = new TypeDesc[] {stringDesc};

        newObject(stringBufferDesc);
        dup();
        loadConstant(strlen);
        invokeConstructor("java.lang.StringBuffer", new TypeDesc[] {intDesc});

        int beginIndex;
        int endIndex = 0;

        while (endIndex < strlen) {
            beginIndex = endIndex;

            // Make each chunk as large as possible.
            utflen = 0;
            for (; endIndex < strlen; endIndex++) {
                int c = value.charAt(endIndex);
                int size;
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    size = 1;
                }
                else if (c > 0x07FF) {
                    size = 3;
                }
                else {
                    size = 2;
                }

                if ((utflen + size) > 65535) {
                    break;
                }
                else {
                    utflen += size;
                }
            }

            String substr = value.substring(beginIndex, endIndex);

            ConstantInfo info = ConstantStringInfo.make(mCp, substr);
            mInstructions.new LoadConstantInstruction(1, info);

            invokeVirtual("java.lang.StringBuffer", "append",
                          stringBufferDesc, stringParam);
        }

        invokeVirtual("java.lang.StringBuffer", "toString",
                      stringDesc);
    }

    public void loadConstant(boolean value) {
        loadConstant(value?1:0);
    }

    public void loadConstant(int value) {
        if (-1 <= value && value <= 5) {
            byte op;

            switch(value) {
            case -1:
                op = Opcode.ICONST_M1;
                break;
            case 0:
                op = Opcode.ICONST_0;
                break;
            case 1:
                op = Opcode.ICONST_1;
                break;
            case 2:
                op = Opcode.ICONST_2;
                break;
            case 3:
                op = Opcode.ICONST_3;
                break;
            case 4:
                op = Opcode.ICONST_4;
                break;
            case 5:
                op = Opcode.ICONST_5;
                break;
            default:
                op = Opcode.NOP;
            }

            addCode(1, op);
        }
        else if (-128 <= value && value <= 127) {
            addCode(1, Opcode.BIPUSH, (byte)value);
        }
        else if (-32768 <= value && value <= 32767) {
            addCode(1, Opcode.SIPUSH, (short)value);
        }
        else {
            ConstantInfo info = ConstantIntegerInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
        }
    }

    public void loadConstant(long value) {
        if (value == 0) {
            addCode(2, Opcode.LCONST_0);
        }
        else if (value == 1) {
            addCode(2, Opcode.LCONST_1);
        }
        else {
            ConstantInfo info = ConstantLongInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(2, info, true);
        }
    }

    public void loadConstant(float value) {
        if (value == 0) {
            addCode(1, Opcode.FCONST_0);
        }
        else if (value == 1) {
            addCode(1, Opcode.FCONST_1);
        }
        else if (value == 2) {
            addCode(1, Opcode.FCONST_2);
        }
        else {
            ConstantInfo info = ConstantFloatInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
        }
    }

    public void loadConstant(double value) {
        if (value == 0) {
            addCode(2, Opcode.DCONST_0);
        }
        else if (value == 1) {
            addCode(2, Opcode.DCONST_1);
        }
        else {
            ConstantInfo info = ConstantDoubleInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(2, info, true);
        }
    }

    public void loadThisClass() {
        loadClass(mClassFile.getType());
    }
    
    public void loadClass(Class clazz) {
        loadClass(TypeDesc.forClass(clazz));
    }
    
    public void loadClass(TypeDesc clazz) {
        ConstantInfo info = mCp.addConstantClass(clazz.getFullName());
        mInstructions.new LoadConstantInstruction(1, info);
    }
    
    // load-local-to-stack style instructions

    public void loadLocal(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }
        int stackAdjust = local.getType().isDoubleWord() ? 2 : 1;
        mInstructions.new LoadLocalInstruction(stackAdjust, local);
    }

    public void loadThis() {
        if (mThisReference != null) {
            loadLocal(mThisReference);
        }
        else {
            throw new RuntimeException
                ("Attempt to load \"this\" reference in a static method: " +
                 mClassFile.getClassName() + "." + mName);
        }
    }

    // store-from-stack-to-local style instructions

    public void storeLocal(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }
        int stackAdjust = local.getType().isDoubleWord() ? -2 : -1;
        mInstructions.new StoreLocalInstruction(stackAdjust, local);
    }

    // load-to-stack-from-array style instructions

    public void loadFromArray(TypeDesc type) {
        byte op;
        int stackAdjust = -1;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
            op = Opcode.IALOAD;
            break;
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
            op = Opcode.BALOAD;
            break;
        case TypeDesc.SHORT_CODE:
            op = Opcode.SALOAD;
            break;
        case TypeDesc.CHAR_CODE:
            op = Opcode.CALOAD;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FALOAD;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = 0;
            op = Opcode.LALOAD;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = 0;
            op = Opcode.DALOAD;
            break;
        default:
            op = Opcode.AALOAD;
            break;
        }

        addCode(stackAdjust, op);
    }

    // store-to-array-from-stack style instructions

    public void storeToArray(TypeDesc type) {
        byte op;
        int stackAdjust = -3;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
            op = Opcode.IASTORE;
            break;
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
            op = Opcode.BASTORE;
            break;
        case TypeDesc.SHORT_CODE:
            op = Opcode.SASTORE;
            break;
        case TypeDesc.CHAR_CODE:
            op = Opcode.CASTORE;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FASTORE;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = -4;
            op = Opcode.LASTORE;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = -4;
            op = Opcode.DASTORE;
            break;
        default:
            op = Opcode.AASTORE;
            break;
        }

        addCode(stackAdjust, op);
    }

    // load-field-to-stack style instructions

    public void loadField(String fieldName,
                          TypeDesc type) {
        getfield(0, Opcode.GETFIELD, constantField(fieldName, type), type);
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDesc type) {

        getfield(0, Opcode.GETFIELD,
                 mCp.addConstantField(className, fieldName, type),
                 type);
    }

    public void loadStaticField(String fieldName,
                                TypeDesc type) {

        getfield(1, Opcode.GETSTATIC, constantField(fieldName, type), type);
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDesc type) {

        getfield(1, Opcode.GETSTATIC,
                 mCp.addConstantField(className, fieldName, type),
                 type);
    }

    private void getfield(int stackAdjust, byte opcode, ConstantInfo info,
                          TypeDesc type) {
        if (type.isDoubleWord()) {
            stackAdjust++;
        }
        addCode(stackAdjust, opcode, info);
    }

    private ConstantFieldInfo constantField(String fieldName,
                                            TypeDesc type) {
        return mCp.addConstantField
            (mClassFile.getClassName(), fieldName, type);
    }

    // store-to-field-from-stack style instructions

    public void storeField(String fieldName,
                           TypeDesc type) {

        putfield(-1, Opcode.PUTFIELD, constantField(fieldName, type), type);
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDesc type) {

        putfield(-1, Opcode.PUTFIELD,
                 mCp.addConstantField(className, fieldName, type),
                 type);
    }

    public void storeStaticField(String fieldName,
                                 TypeDesc type) {

        putfield(0, Opcode.PUTSTATIC, constantField(fieldName, type), type);
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDesc type) {

        putfield(0, Opcode.PUTSTATIC,
                 mCp.addConstantField(className, fieldName, type),
                 type);
    }

    private void putfield(int stackAdjust, byte opcode, ConstantInfo info,
                          TypeDesc type) {
        if (type.isDoubleWord()) {
            stackAdjust -= 2;
        }
        else {
            stackAdjust--;
        }
        addCode(stackAdjust, opcode, info);
    }

    // return style instructions

    public void returnVoid() {
        addCode(0, Opcode.RETURN);
    }

    public void returnValue(TypeDesc type) {
        int stackAdjust = -1;
        byte op;

        switch (type.getTypeCode()) {
        case TypeDesc.INT_CODE:
        case TypeDesc.BOOLEAN_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.CHAR_CODE:
            op = Opcode.IRETURN;
            break;
        case TypeDesc.FLOAT_CODE:
            op = Opcode.FRETURN;
            break;
        case TypeDesc.LONG_CODE:
            stackAdjust = -2;
            op = Opcode.LRETURN;
            break;
        case TypeDesc.DOUBLE_CODE:
            stackAdjust = -2;
            op = Opcode.DRETURN;
            break;
        case TypeDesc.VOID_CODE:
            stackAdjust = 0;
            op = Opcode.RETURN;
            break;
        default:
            op = Opcode.ARETURN;
            break;
        }

        addCode(stackAdjust, op);
    }

    // numerical conversion style instructions

    public void convert(TypeDesc fromType, TypeDesc toType) {
        if (toType == TypeDesc.OBJECT) {
            if (fromType.isPrimitive()) {
                toType = fromType.toObjectType();
            }
            else {
                return;
            }
        }

        if (fromType == toType) {
            return;
        }

        TypeDesc fromPrimitiveType = fromType.toPrimitiveType();
        if (fromPrimitiveType == null) {
            throw invalidConversion(fromType, toType);
        }
        int fromTypeCode = fromPrimitiveType.getTypeCode();

        if (!fromType.isPrimitive()) {
            unbox(fromType, fromPrimitiveType);
        }

        TypeDesc toPrimitiveType = toType.toPrimitiveType();
        if (toPrimitiveType == null) {
            throw invalidConversion(fromType, toType);
        }
        int toTypeCode = toPrimitiveType.getTypeCode();

        int stackAdjust = 0;
        byte op;

        switch (fromTypeCode) {
        case TypeDesc.INT_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.CHAR_CODE:
        case TypeDesc.BOOLEAN_CODE:
            switch (toTypeCode) {
            case TypeDesc.BYTE_CODE:
                op = (fromTypeCode == TypeDesc.BYTE_CODE) ?
                    Opcode.NOP : Opcode.I2B;
                break;
            case TypeDesc.SHORT_CODE:
                op = (fromTypeCode == TypeDesc.SHORT_CODE) ?
                    Opcode.NOP : Opcode.I2S;
                break;
            case TypeDesc.CHAR_CODE:
                op = (fromTypeCode == TypeDesc.CHAR_CODE) ?
                    Opcode.NOP : Opcode.I2C;
                break;
            case TypeDesc.FLOAT_CODE:
                op = Opcode.I2F;
                break;
            case TypeDesc.LONG_CODE:
                stackAdjust = 1;
                op = Opcode.I2L;
                break;
            case TypeDesc.DOUBLE_CODE:
                stackAdjust = 1;
                op = Opcode.I2D;
                break;
            case TypeDesc.INT_CODE:
                op = Opcode.NOP;
                break;
            case TypeDesc.BOOLEAN_CODE:
                toBoolean(!toType.isPrimitive());
                return;
            default:
                throw invalidConversion(fromType, toType);
            }
            break;

        case TypeDesc.LONG_CODE:
            switch (toTypeCode) {
            case TypeDesc.INT_CODE:
                stackAdjust = -1;
                op = Opcode.L2I;
                break;
            case TypeDesc.FLOAT_CODE:
                stackAdjust = -1;
                op = Opcode.L2F;
                break;
            case TypeDesc.DOUBLE_CODE:
                op = Opcode.L2D;
                break;
            case TypeDesc.BYTE_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.SHORT_CODE:
                addCode(-1, Opcode.L2I);
                convert(TypeDesc.INT, toPrimitiveType);
                // fall through
            case TypeDesc.LONG_CODE:
                op = Opcode.NOP;
                break;
            case TypeDesc.BOOLEAN_CODE:
                loadConstant(0L);
                math(Opcode.LCMP);
                toBoolean(!toType.isPrimitive());
                return;
            default:
                throw invalidConversion(fromType, toType);
            }
            break;

        case TypeDesc.FLOAT_CODE:
            switch (toTypeCode) {
            case TypeDesc.INT_CODE:
                op = Opcode.F2I;
                break;
            case TypeDesc.LONG_CODE:
                stackAdjust = 1;
                op = Opcode.F2L;
                break;
            case TypeDesc.DOUBLE_CODE:
                stackAdjust = 1;
                op = Opcode.F2D;
                break;
            case TypeDesc.BYTE_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.SHORT_CODE:
                addCode(0, Opcode.F2I);
                convert(TypeDesc.INT, toPrimitiveType);
                // fall through
            case TypeDesc.FLOAT_CODE:
                op = Opcode.NOP;
                break;
            case TypeDesc.BOOLEAN_CODE:
                loadConstant(0.0f);
                math(Opcode.FCMPG);
                toBoolean(!toType.isPrimitive());
                return;
            default:
                throw invalidConversion(fromType, toType);
            }
            break;

        case TypeDesc.DOUBLE_CODE:
            switch (toTypeCode) {
            case TypeDesc.INT_CODE:
                stackAdjust = -1;
                op = Opcode.D2I;
                break;
            case TypeDesc.FLOAT_CODE:
                stackAdjust = -1;
                op = Opcode.D2F;
                break;
            case TypeDesc.LONG_CODE:
                op = Opcode.D2L;
                break;
            case TypeDesc.BYTE_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.SHORT_CODE:
                addCode(-1, Opcode.D2I);
                convert(TypeDesc.INT, toPrimitiveType);
                // fall through
            case TypeDesc.DOUBLE_CODE:
                op = Opcode.NOP;
                break;
            case TypeDesc.BOOLEAN_CODE:
                loadConstant(0.0d);
                math(Opcode.DCMPG);
                toBoolean(!toType.isPrimitive());
                return;
            default:
                throw invalidConversion(fromType, toType);
            }
            break;

        default:
            throw invalidConversion(fromType, toType);
        }

        if (toType.isPrimitive()) {
            if (op != Opcode.NOP) {
                addCode(stackAdjust, op);
            }
        }
        else {
            if (op == Opcode.NOP) {
                prebox(toPrimitiveType, toType);
            }
            // Slight optimization here. Perform prebox on single word value,
            // depending on what conversion is being applied.
            else if (!fromPrimitiveType.isDoubleWord() &&
                     toPrimitiveType.isDoubleWord()) {
                prebox(fromPrimitiveType, toType);
                addCode(stackAdjust, op);
            }
            else {
                addCode(stackAdjust, op);
                prebox(toPrimitiveType, toType);
            }
            box(toPrimitiveType, toType);
        }
    }

    private void unbox(TypeDesc from, TypeDesc to) {
        String methodName;

        switch (to.getTypeCode()) {
        case TypeDesc.BOOLEAN_CODE:
            methodName = "booleanValue";
            break;
        case TypeDesc.CHAR_CODE:
            methodName = "charValue";
            break;
        case TypeDesc.FLOAT_CODE:
            methodName = "floatValue";
            break;
        case TypeDesc.DOUBLE_CODE:
            methodName = "doubleValue";
            break;
        case TypeDesc.BYTE_CODE:
            methodName = "byteValue";
            break;
        case TypeDesc.SHORT_CODE:
            methodName = "shortValue";
            break;
        case TypeDesc.INT_CODE:
            methodName = "intValue";
            break;
        case TypeDesc.LONG_CODE:
            methodName = "longValue";
            break;
        default:
            return;
        }

        invokeVirtual(from.getRootName(), methodName, to);
    }

    private void prebox(TypeDesc from, TypeDesc to) {
        // Wouldn't it be cool if I could walk backwards in the instruction
        // list and insert the new-dup pair before the value to box was even
        // put on the stack?

        switch (from.getTypeCode()) {
        default:
            break;
        case TypeDesc.BOOLEAN_CODE:
            if (to.toPrimitiveType().getTypeCode() == TypeDesc.BOOLEAN_CODE) {
                break;
            }
            // fall through
        case TypeDesc.CHAR_CODE:
        case TypeDesc.FLOAT_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.INT_CODE:
            newObject(to);
            dupX1();
            swap();
            break;
        case TypeDesc.DOUBLE_CODE:
        case TypeDesc.LONG_CODE:
            newObject(to);
            dupX2();
            dupX2();
            pop();
            break;
        }
    }

    private void box(TypeDesc from, TypeDesc to) {
        switch (from.getTypeCode()) {
        case TypeDesc.BOOLEAN_CODE:
            toBoolean(true);
            break;
        case TypeDesc.CHAR_CODE:
        case TypeDesc.FLOAT_CODE:
        case TypeDesc.BYTE_CODE:
        case TypeDesc.SHORT_CODE:
        case TypeDesc.INT_CODE:
        case TypeDesc.DOUBLE_CODE:
        case TypeDesc.LONG_CODE:
            invokeConstructor(to.getRootName(), new TypeDesc[]{from});
            break;
        }
    }

    // Converts an int on the stack to a boolean.
    private void toBoolean(boolean box) {
        Label nonZero = createLabel();
        Label done = createLabel();
        ifZeroComparisonBranch(nonZero, "!=");
        if (box) {
            TypeDesc newType = TypeDesc.forClass(Boolean.class);
            loadStaticField(newType.getRootName(), "FALSE", newType);
            branch(done);
            nonZero.setLocation();
            loadStaticField(newType.getRootName(), "TRUE", newType);
        }
        else {
            loadConstant(false);
            branch(done);
            nonZero.setLocation();
            loadConstant(true);
        }
        done.setLocation();
    }

    private IllegalArgumentException invalidConversion
        (TypeDesc from, TypeDesc to)
    {
        throw new IllegalArgumentException
            ("Invalid conversion: " + from.getFullName() + " to " +
             to.getFullName());
    }

    // invocation style instructions

    public void invoke(Method method) {
        TypeDesc ret = TypeDesc.forClass(method.getReturnType(),
                                         method.getGenericReturnType());

        Class[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
        }

        Class clazz = method.getDeclaringClass();

        if (Modifier.isStatic(method.getModifiers())) {
            invokeStatic(clazz.getName(),
                         method.getName(),
                         ret,
                         params);
        }
        else if (clazz.isInterface()) {
            invokeInterface(clazz.getName(),
                            method.getName(),
                            ret,
                            params);
        }
        else {
            invokeVirtual(clazz.getName(),
                          method.getName(),
                          ret,
                          params);
        }
    }

    public void invoke(Constructor constructor) {
        Class[] paramClasses = constructor.getParameterTypes();
        Type[] paramTypes = constructor.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
        }

        invokeConstructor(constructor.getDeclaringClass().toString(), params);
    }

    public void invokeVirtual(String methodName,
                              TypeDesc ret,
                              TypeDesc... params) {

        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKEVIRTUAL, info);
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDesc ret,
                              TypeDesc... params) {
        ConstantInfo info =
            mCp.addConstantMethod(className, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKEVIRTUAL, info);
    }

    public void invokeStatic(String methodName,
                             TypeDesc ret,
                             TypeDesc... params) {
        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 0;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESTATIC, info);
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDesc ret,
                             TypeDesc... params) {
        ConstantInfo info =
            mCp.addConstantMethod(className, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 0;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESTATIC, info);
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc... params) {

        ConstantInfo info =
            mCp.addConstantInterfaceMethod(className, methodName, ret, params);

        int paramCount = 1;
        if (params != null) {
            paramCount += argSize(params);
        }

        int stackAdjust = returnSize(ret) - paramCount;

        byte[] bytes = new byte[5];

        bytes[0] = Opcode.INVOKEINTERFACE;
        //bytes[1] = (byte)0;
        //bytes[2] = (byte)0;
        bytes[3] = (byte)paramCount;
        //bytes[4] = (byte)0;

        mInstructions.new ConstantOperandInstruction(stackAdjust, bytes, info);
    }

    public void invokePrivate(String methodName,
                              TypeDesc ret,
                              TypeDesc... params) {
        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDesc ret,
                            TypeDesc... params) {
        ConstantInfo info =
            mCp.addConstantMethod(superClassName, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    public void invokeSuper(Method method) {
        TypeDesc ret = TypeDesc.forClass(method.getReturnType(),
                                         method.getGenericReturnType());

        Class[] paramClasses = method.getParameterTypes();
        Type[] paramTypes = method.getGenericParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
        }

        invokeSuper(method.getDeclaringClass().getName(),
                    method.getName(),
                    ret,
                    params);
    }

    public void invokeConstructor(TypeDesc... params) {
        ConstantInfo info =
            mCp.addConstantConstructor(mClassFile.getClassName(), params);

        int stackAdjust = -1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }
    
    public void invokeConstructor(MethodInfo mi) {
        invokeConstructor(mi.getClassFile().getClassName(),
                          mi.getMethodDescriptor().getParameterTypes());
    }

    public void invokeConstructor(String className, TypeDesc... params) {
        ConstantInfo info = mCp.addConstantConstructor(className, params);

        int stackAdjust = -1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    public void invokeSuperConstructor(TypeDesc... params) {
        invokeConstructor(mClassFile.getSuperClassName(), params);
    }

    public void invokeSuper(Constructor constructor) {
        Class[] paramClasses = constructor.getParameterTypes();
        Type[] paramTypes = constructor.getGenericParameterTypes();

        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i], paramTypes[i]);
        }

        invokeSuperConstructor(params);
    }

    private int returnSize(TypeDesc ret) {
        if (ret == null || ret == TypeDesc.VOID) {
            return 0;
        }
        if (ret.isDoubleWord()) {
            return 2;
        }
        return 1;
    }

    private int argSize(TypeDesc... params) {
        int size = 0;
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                size += returnSize(params[i]);
            }
        }
        return size;
    }

    // creation style instructions

    public void newObject(TypeDesc type) {
        if (type.isArray()) {
            newObject(type, 1);
        }
        else {
            ConstantInfo info = mCp.addConstantClass(type);
            addCode(1, Opcode.NEW, info);
        }
    }

    public void newObject(TypeDesc type, int dimensions) {
        if (dimensions <= 0) {
            // If type refers to an array, then this code is bogus.
            ConstantInfo info = mCp.addConstantClass(type);
            addCode(1, Opcode.NEW, info);
            return;
        }

        TypeDesc componentType = type.getComponentType();

        if (dimensions == 1) {
            if (componentType.isPrimitive()) {
                addCode(0, Opcode.NEWARRAY, (byte)componentType.getTypeCode());
                return;
            }
            addCode(0, Opcode.ANEWARRAY, mCp.addConstantClass(componentType));
            return;
        }

        int stackAdjust = -(dimensions - 1);
        ConstantInfo info = mCp.addConstantClass(type);
        byte[] bytes = new byte[4];

        bytes[0] = Opcode.MULTIANEWARRAY;
        //bytes[1] = (byte)0;
        //bytes[2] = (byte)0;
        bytes[3] = (byte)dimensions;

        mInstructions.new ConstantOperandInstruction(stackAdjust, bytes, info);
    }

    // stack operation style instructions

    public void dup() {
        addCode(1, Opcode.DUP);
    }

    public void dupX1() {
        addCode(1, Opcode.DUP_X1);
    }

    public void dupX2() {
        addCode(1, Opcode.DUP_X2);
    }

    public void dup2() {
        addCode(2, Opcode.DUP2);
    }

    public void dup2X1() {
        addCode(2, Opcode.DUP2_X1);
    }

    public void dup2X2() {
        addCode(2, Opcode.DUP2_X2);
    }

    public void pop() {
        addCode(-1, Opcode.POP);
    }

    public void pop2() {
        addCode(-2, Opcode.POP2);
    }

    public void swap() {
        addCode(0, Opcode.SWAP);
    }

    public void swap2() {
        dup2X2();
        pop2();
    }

    // flow control instructions

    private void branch(int stackAdjust, Location location, byte opcode) {
        mInstructions.new BranchInstruction(stackAdjust, opcode, location);
    }

    public void branch(Location location) {
        branch(0, location, Opcode.GOTO);
    }

    public void ifNullBranch(Location location, boolean choice) {
        branch(-1, location, choice ? Opcode.IFNULL : Opcode.IFNONNULL);
    }

    public void ifEqualBranch(Location location, boolean choice) {
        branch(-2, location, choice ? Opcode.IF_ACMPEQ : Opcode.IF_ACMPNE);
    }

    public void ifZeroComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IFEQ;
        }
        else if (choice == "!=") {
            opcode = Opcode.IFNE;
        }
        else if (choice == "<") {
            opcode = Opcode.IFLT;
        }
        else if (choice == ">=") {
            opcode = Opcode.IFGE;
        }
        else if (choice == ">") {
            opcode = Opcode.IFGT;
        }
        else if (choice == "<=") {
            opcode = Opcode.IFLE;
        }
        else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }

        branch(-1, location, opcode);
    }

    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IF_ICMPEQ;
        }
        else if (choice == "!=") {
            opcode = Opcode.IF_ICMPNE;
        }
        else if (choice == "<") {
            opcode = Opcode.IF_ICMPLT;
        }
        else if (choice == ">=") {
            opcode = Opcode.IF_ICMPGE;
        }
        else if (choice == ">") {
            opcode = Opcode.IF_ICMPGT;
        }
        else if (choice == "<=") {
            opcode = Opcode.IF_ICMPLE;
        }
        else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }

        branch(-2, location, opcode);
    }

    public void switchBranch(int[] cases,
                             Location[] locations, Location defaultLocation) {

        mInstructions.new SwitchInstruction(cases, locations, defaultLocation);
    }

    public void jsr(Location location) {
        // Adjust the stack by one to make room for the return address.
        branch(1, location, Opcode.JSR);
    }

    public void ret(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        mInstructions.new RetInstruction(local);
    }

    // math instructions

    public void math(byte opcode) {
        int stackAdjust;

        switch(opcode) {
        case Opcode.INEG:
        case Opcode.LNEG:
        case Opcode.FNEG:
        case Opcode.DNEG:
            stackAdjust = 0;
            break;
        case Opcode.IADD:
        case Opcode.ISUB:
        case Opcode.IMUL:
        case Opcode.IDIV:
        case Opcode.IREM:
        case Opcode.IAND:
        case Opcode.IOR:
        case Opcode.IXOR:
        case Opcode.ISHL:
        case Opcode.ISHR:
        case Opcode.IUSHR:
        case Opcode.FADD:
        case Opcode.FSUB:
        case Opcode.FMUL:
        case Opcode.FDIV:
        case Opcode.FREM:
        case Opcode.FCMPG:
        case Opcode.FCMPL:
        case Opcode.LSHL:
        case Opcode.LSHR:
        case Opcode.LUSHR:
            stackAdjust = -1;
            break;
        case Opcode.LADD:
        case Opcode.LSUB:
        case Opcode.LMUL:
        case Opcode.LDIV:
        case Opcode.LREM:
        case Opcode.LAND:
        case Opcode.LOR:
        case Opcode.LXOR:
        case Opcode.DADD:
        case Opcode.DSUB:
        case Opcode.DMUL:
        case Opcode.DDIV:
        case Opcode.DREM:
            stackAdjust = -2;
            break;
        case Opcode.LCMP:
        case Opcode.DCMPG:
        case Opcode.DCMPL:
            stackAdjust = -3;
            break;
        default:
            throw new IllegalArgumentException
                ("Not a math opcode: " + Opcode.getMnemonic(opcode));
        }

        addCode(stackAdjust, opcode);
    }

    // miscellaneous instructions

    public void arrayLength() {
        addCode(0, Opcode.ARRAYLENGTH);
    }

    public void throwObject() {
        addCode(-1, Opcode.ATHROW);
    }

    public void checkCast(TypeDesc type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addCode(0, Opcode.CHECKCAST, info);
    }

    public void instanceOf(TypeDesc type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addCode(0, Opcode.INSTANCEOF, info);
    }

    public void integerIncrement(LocalVariable local, int amount) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        if (-32768 <= amount && amount <= 32767) {
            mInstructions.new ShortIncrementInstruction(local, (short)amount);
        }
        else {
            // Amount can't possibly fit in a 16-bit value, so use regular
            // instructions instead.

            loadLocal(local);
            loadConstant(amount);
            math(Opcode.IADD);
            storeLocal(local);
        }
    }

    public void monitorEnter() {
        addCode(-1, Opcode.MONITORENTER);
    }

    public void monitorExit() {
        addCode(-1, Opcode.MONITOREXIT);
    }

    public void nop() {
        addCode(0, Opcode.NOP);
    }

    public void breakpoint() {
        addCode(0, Opcode.BREAKPOINT);
    }
}
