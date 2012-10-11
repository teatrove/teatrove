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


package org.teatrove.tea.compiler;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.teatrove.tea.parsetree.AndExpression;
import org.teatrove.tea.parsetree.ArithmeticExpression;
import org.teatrove.tea.parsetree.ArrayLookup;
import org.teatrove.tea.parsetree.AssignmentStatement;
import org.teatrove.tea.parsetree.Block;
import org.teatrove.tea.parsetree.BooleanLiteral;
import org.teatrove.tea.parsetree.BreakStatement;
import org.teatrove.tea.parsetree.CallExpression;
import org.teatrove.tea.parsetree.CompareExpression;
import org.teatrove.tea.parsetree.ConcatenateExpression;
import org.teatrove.tea.parsetree.ContinueStatement;
import org.teatrove.tea.parsetree.ExceptionGuardStatement;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.Expression.Conversion;
import org.teatrove.tea.parsetree.ExpressionList;
import org.teatrove.tea.parsetree.ExpressionStatement;
import org.teatrove.tea.parsetree.ForeachStatement;
import org.teatrove.tea.parsetree.FunctionCallExpression;
import org.teatrove.tea.parsetree.IfStatement;
import org.teatrove.tea.parsetree.ImportDirective;
import org.teatrove.tea.parsetree.Logical;
import org.teatrove.tea.parsetree.Lookup;
import org.teatrove.tea.parsetree.Name;
import org.teatrove.tea.parsetree.NegateExpression;
import org.teatrove.tea.parsetree.NewArrayExpression;
import org.teatrove.tea.parsetree.NoOpExpression;
import org.teatrove.tea.parsetree.Node;
import org.teatrove.tea.parsetree.NodeVisitor;
import org.teatrove.tea.parsetree.NotExpression;
import org.teatrove.tea.parsetree.NullLiteral;
import org.teatrove.tea.parsetree.NullSafe;
import org.teatrove.tea.parsetree.NumberLiteral;
import org.teatrove.tea.parsetree.OrExpression;
import org.teatrove.tea.parsetree.ParenExpression;
import org.teatrove.tea.parsetree.RelationalExpression;
import org.teatrove.tea.parsetree.ReturnStatement;
import org.teatrove.tea.parsetree.SpreadExpression;
import org.teatrove.tea.parsetree.Statement;
import org.teatrove.tea.parsetree.StatementList;
import org.teatrove.tea.parsetree.StringLiteral;
import org.teatrove.tea.parsetree.SubstitutionStatement;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.TemplateCallExpression;
import org.teatrove.tea.parsetree.TernaryExpression;
import org.teatrove.tea.parsetree.TreeWalker;
import org.teatrove.tea.parsetree.TypeExpression;
import org.teatrove.tea.parsetree.TypeName;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.tea.parsetree.VariableRef;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.runtime.Substitution;
import org.teatrove.tea.runtime.SubstitutionId;
import org.teatrove.tea.runtime.Truthful;
import org.teatrove.tea.runtime.WrapperTypeConversionUtil;
import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.classfile.CodeBuilder;
import org.teatrove.trove.classfile.Label;
import org.teatrove.trove.classfile.LocalVariable;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.classfile.Opcode;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.util.MergedClass;

/**
 * The JavaClassGenerator compiles a template into a single Java class file.
 * A template is compiled such that it has two static methods, execute and
 * getTemplateParameterNames. The signatures are:
 *
 * <pre>
 *     public static void execute(RuntimeContext, params ...) throws Exception;
 *
 *     public static String[] getTemplateParameterNames();
 * </pre>
 *
 * @author Brian S O'Neill
 */
public class JavaClassGenerator extends CodeGenerator {

    public static final String EXECUTE_METHOD_NAME = "execute";
    public static final String PARAMETER_METHOD_NAME =
        "getTemplateParameterNames";

    private static final String CONTEXT_PARAM_NAME = "context";
    private static final String SUB_PARAM_NAME = "sub";

    // Length estimate of unknown concatenation elements.
    private static final int LENGTH_ESTIMATE = 16;

    private static TypeDesc[] cObjectParam;
    private static TypeDesc[] cStringParam;
    private static TypeDesc[] cIntParam;
    private static TypeDesc cStringBuilderDesc;

    static {
        cObjectParam = new TypeDesc[] {TypeDesc.OBJECT};
        cStringParam = new TypeDesc[] {TypeDesc.STRING};
        cIntParam = new TypeDesc[] {TypeDesc.INT};
        cStringBuilderDesc = makeDesc(StringBuilder.class);
    }

    private static TypeDesc makeDesc(String name) {
        return TypeDesc.forClass(name);
    }

    private static TypeDesc makeDesc(Class<?> clazz) {
        return TypeDesc.forClass(clazz);
    }
    
    private static TypeDesc makeDesc(Class<?> clazz, 
                                     java.lang.reflect.Type genericType) {
        return TypeDesc.forClass(clazz, genericType);
    }

    private static TypeDesc makeDesc(Type type) {
        return makeDesc(type, true);
    }

    private static TypeDesc makeDesc(Type type, boolean natural) {
        if (natural) {
            return makeDesc(type.getNaturalClass(), type.getGenericClass());
        } else {
            return makeDesc(type.getObjectClass(), type.getGenericClass());
        }
    }

    private static TypeDesc makeDesc(Variable v) {
        if (v.getType() != null) {
            return makeDesc(v.getType(), true);
        } else if (v.getTypeName() != null) {
            return makeDesc(v.getTypeName().getName());
        } else {
            throw new IllegalStateException("missing type: " + v.getName());
        }
    }

    private CompilationUnit mUnit;

    private LocalVariable mGlobalTime;
    private LocalVariable mSubTime;
    
    private int mTemporary;

    /**
     * If a template has any substitution blocks that it passes in a
     * call expression, then the generated code is in a different format.
     *
     * Ordinarily, the generated class looks like this:
     *
     * public final class a.b.c.Template {
     *     public static void execute(Context, params ...)
     *     throws Exception {
     *         ...
     *     }
     *
     *     private Template() {
     *         super();
     *     }
     * }
     *
     * When mSubBlockCount is greater than zero, the generated class looks
     * like this:
     *
     * public final class a.b.c.Template implements Substitution {
     *     public static void execute(Context, params ...)
     *     throws Exception {
     *         new Template(Context, params ...).substitute();
     *     }
     *
     *     private int blockId;
     *     private Context context;
     *
     *     private Template(Context, params ...) {
     *         super();
     *         this.context = Context;
     *         this.params = params;
     *     }
     *
     *     public void substitute() throws Exception {
     *         if (this.context == null) {
     *             throw new UnsupportedOperationException();
     *         }
     *         substitute(this.context);
     *     }
     *
     *     public void substitute(Context) throws Exception {
     *         switch (this.blockId) {
     *         case 0:
     *             ...
     *         case 1:
     *             ...
     *         case n:
     *             ...
     *         }
     *     }
     *
     *     public Object getIdentifier() {
     *         return new SubstitutionId(this, blockId);
     *     }
     *
     *     public Substitution detach() {
     *         Substitution sub = (Substitution)clone();
     *         sub.context = null;
     *         return sub;
     *     }
     * }
     */
    private int mSubBlockCount;
    private boolean mGenerateSubFormat;
    private List<String> mCallerToSubNoList = new ArrayList<String>();

    // Maps Variable names to variable object fields that need to be defined.
    private Map<String, Variable> mFields = new HashMap<String, Variable>();

    // A list of Statements that need to be put into a static initializer.
    private List<Object> mInitializerStatements =
        new ArrayList<Object>();

    private MessageFormatter mFormatter;

    public JavaClassGenerator(CompilationUnit unit) {
        super(unit.getParseTree());
        mUnit = unit;
        mFormatter = MessageFormatter.lookup(this);
    }

    @SuppressWarnings("unused")
    private void error(String str, Node culprit) {
        str = mFormatter.format(str);
        error(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void error(String str, String arg, Node culprit) {
        str = mFormatter.format(str, arg);
        error(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void error(String str, String arg1, String arg2, Node culprit) {
        str = mFormatter.format(str, arg1, arg2);
        error(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void error(String str, String arg1, String arg2, String arg3,
                       Node culprit) {
        str = mFormatter.format(str, arg1, arg2, arg3);
        error(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void error(String str, String arg, Token culprit) {
        str = mFormatter.format(str, arg);
        error(str, culprit.getSourceInfo());
    }

    private void error(String str, SourceInfo info) {
        dispatchCompileError(new CompileEvent(this, CompileEvent.Type.ERROR,
                                              str, info, mUnit));
    }
    
    @SuppressWarnings("unused")
    private void error(String str, String arg, SourceInfo info) {
        str = mFormatter.format(str, arg);
        error(str, info);
    }

    @SuppressWarnings("unused")
    private void error(String str, String arg1, String arg2, SourceInfo info) {
        str = mFormatter.format(str, arg1, arg2);
        error(str, info);
    }

    private void warn(String str, Node culprit) {
        str = mFormatter.format(str);
        warn(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void warn(String str, String arg, Node culprit) {
        str = mFormatter.format(str, arg);
        warn(str, culprit.getSourceInfo());
    }

    private void warn(String str, String arg1, String arg2, Node culprit) {
        str = mFormatter.format(str, arg1, arg2);
        warn(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void warn(String str, String arg1, String arg2, String arg3,
                      Node culprit) {
        str = mFormatter.format(str, arg1, arg2, arg3);
        warn(str, culprit.getSourceInfo());
    }

    @SuppressWarnings("unused")
    private void warn(String str, String arg, Token culprit) {
        str = mFormatter.format(str, arg);
        warn(str, culprit.getSourceInfo());
    }

    private void warn(String str, SourceInfo info) {
        dispatchCompileWarning(new CompileEvent(this, CompileEvent.Type.WARNING,
                                                str, info, mUnit));
    }
    
    @SuppressWarnings("unused")
    private void warn(String str, String arg, SourceInfo info) {
        str = mFormatter.format(str, arg);
        warn(str, info);
    }

    @SuppressWarnings("unused")
    private void warn(String str, String arg1, String arg2, SourceInfo info) {
        str = mFormatter.format(str, arg1, arg2);
        warn(str, info);
    }
    
    public void writeTo(OutputStream out) throws IOException {
        String className = mUnit.getName();
        String targetPackage = mUnit.getTargetPackage();
        if (targetPackage != null) {
            className = targetPackage + '.' + className;
        }

        ClassFile classFile = new ClassFile(className);
        classFile.getModifiers().setFinal(true);

        String sourceFile = mUnit.getSourcePath();
        if (sourceFile != null) {
            classFile.setSourceFile(sourceFile);
        }

        final Template t = getParseTree();

        t.accept(new TreeWalker() {
            public Object visit(FunctionCallExpression node) {
                if (node.getSubstitutionParam() != null) {
                    mSubBlockCount++;
                    mGenerateSubFormat = true;
                }
                return super.visit(node);
            }

            public Object visit(TemplateCallExpression node) {
                if (node.getSubstitutionParam() != null) {
                    mSubBlockCount++;
                    mGenerateSubFormat = true;
                }
                return super.visit(node);
            }

            public Object visit(Variable node) {
                if (node.isField()) {
                    mGenerateSubFormat = true;
                }
                return super.visit(node);
            }
        });

        generateTemplate(t, className, classFile);

        classFile.writeTo(out);
        out.flush();
    }

    protected void generateTemplateParameters(Template t, ClassFile classFile) {

        // Build the static getTemplateParameterNames method.

        Variable[] params = t.getParams();
        int paramCount = params.length;

        Modifiers pubstat = new Modifiers();
        pubstat.setPublic(true);
        pubstat.setStatic(true);

        TypeDesc stringArrayDesc = TypeDesc.STRING.toArrayType();

        MethodInfo mi = classFile.addMethod
            (pubstat, PARAMETER_METHOD_NAME, stringArrayDesc);

        CodeBuilder builder = new CodeBuilder(mi);
        builder.loadConstant(paramCount);
        builder.newObject(stringArrayDesc);

        // Populate the array.
        for (int i=0; i<paramCount; i++) {
            builder.dup();
            builder.loadConstant(i);
            builder.loadConstant(params[i].getName());
            builder.storeToArray(TypeDesc.STRING);
        }

        builder.returnValue(TypeDesc.OBJECT);

        // Done building the static getTemplateParameterNames method.
    }

    protected void generateTemplate(Template t, String className,
                                    ClassFile classFile) {
        Class<?> subClass;
        Method subMethod, subContextMethod, subIdMethod, subDetachMethod;
        if (mGenerateSubFormat) {
            subClass = Substitution.class;
            classFile.addInterface(subClass);
            classFile.addInterface(Cloneable.class);
            classFile.addInterface(java.io.Serializable.class);
            try {
                subMethod = subClass.getMethod("substitute");
                subContextMethod = subClass.getMethod
                    ("substitute", new Class[]{Context.class});
                subIdMethod = subClass.getMethod("getIdentifier");
                subDetachMethod = subClass.getMethod("detach");
            }
            catch (NoSuchMethodException e) {
                throw new InternalError(e.toString());
            }
        }
        else {
            subClass = null;
            subMethod = null;
            subContextMethod = null;
            subIdMethod = null;
            subDetachMethod = null;
        }

        // Build the static getTemplateParameterNames method.

        Variable[] params = t.getParams();
        int paramCount = params.length;

        generateTemplateParameters(t, classFile);

        // Done building the static getTemplateParameterNames method.

        // Build the static execute method.

        Modifiers pubstat = new Modifiers();
        pubstat.setPublic(true);
        pubstat.setStatic(true);

        Variable[] allParams;
        if (t.hasSubstitutionParam()) {
            allParams = new Variable[paramCount + 2];
            Type type = new Type(Substitution.class);
            Variable subParam = new Variable(null, SUB_PARAM_NAME, type);
            allParams[paramCount + 1] = subParam;
        }
        else {
            allParams = new Variable[paramCount + 1];
        }

        Type type = new Type(mUnit.getCompiler().getRuntimeContext());
        allParams[0] = new Variable(null, CONTEXT_PARAM_NAME, type);
        allParams[0].setTransient(true);

        for (int i=0; i<paramCount; i++) {
            allParams[i + 1] = params[i];
        }

        TypeDesc[] paramTypes = new TypeDesc[allParams.length];

        for (int i=0; i<allParams.length; i++) {
            if (mGenerateSubFormat) {
                allParams[i].setField(true);
            }
            paramTypes[i] = makeDesc(allParams[i]);
        }

        Type returnType = t.getReturnType();
        TypeDesc returnTypeDesc;
        if (returnType == null) {
            returnTypeDesc = null;
        }
        else {
            returnTypeDesc = makeDesc(returnType);
        }

        MethodInfo mi = classFile.addMethod(pubstat, EXECUTE_METHOD_NAME,
                                            returnTypeDesc, paramTypes);

        mi.addException("java.lang.Exception");
        CodeBuilder builder = new CodeBuilder(mi);

        LocalVariable[] localVars = builder.getParameters();
        // Apply names to the passed in parameters, even though this step
        // is not required.
        for (int i=0; i<localVars.length; i++) {
            localVars[i].setName(allParams[i].getName());
        }

        // inject template body profiling bytecode - start marker
        TypeDesc methodObserverType = makeDesc(MergedClass.InvocationEventObserver.class);
        boolean profilingEnabled = isProfilingEnabled();
        if (profilingEnabled) {
            mGlobalTime = builder.createLocalVariable("startTime", makeDesc(long.class));
            builder.invokeStatic(mUnit.getRuntimeContext().getName(),
                "getInvocationObserver", methodObserverType);
            builder.invokeInterface(methodObserverType.getFullName(), "currentTime",
               TypeDesc.forClass(long.class));
            builder.storeLocal(mGlobalTime);
        }

        if (!mGenerateSubFormat) {
            Visitor gen = new Visitor(allParams, builder.getParameters());
            gen.allowInitializerStatements();
            gen.generateNormalFormat(builder, t);

        }
        else {
            boolean doReturnValue = !(Type.VOID_TYPE.equals(returnType));

            // new <this>(params ...).substitute();
            TypeDesc thisType = makeDesc(className);
            builder.newObject(thisType);
            builder.dup();
            for (int i=0; i<localVars.length; i++) {
                builder.loadLocal(localVars[i]);
            }
            builder.invokeConstructor(paramTypes);

            if (doReturnValue) {
                // We'll need the object again to load the field.
                builder.dup();
            }

            builder.invoke(subMethod);

            Visitor gen = new Visitor(allParams);
            gen.allowInitializerStatements();

            // Generate Substitution.substitute().
            mi = classFile.addMethod(subMethod);
            CodeBuilder subBuilder = new CodeBuilder(mi);
            gen.generateContext(subBuilder);
            Label gotContext = subBuilder.createLabel();
            subBuilder.ifNullBranch(gotContext, false);
            String unSupExName =
                UnsupportedOperationException.class.getName();
            subBuilder.newObject(makeDesc(unSupExName));
            subBuilder.dup();
            subBuilder.invokeConstructor(unSupExName);
            subBuilder.throwObject();
            gotContext.setLocation();
            subBuilder.loadThis();
            gen.generateContext(subBuilder);
            subBuilder.invoke(subContextMethod);
            subBuilder.returnVoid();

            // Generate Substitution.substitute(Context).
            mi = classFile.addMethod(subContextMethod);
            subBuilder = new CodeBuilder(mi);

            String[] subFieldNames = gen.generateSubFormat(subBuilder, t);

            // inject template body profiling bytecode - end and generate event
            if (profilingEnabled) {
                builder.invokeStatic(mUnit.getRuntimeContext().getName(),
                    "getInvocationObserver", methodObserverType);
                builder.loadConstant(mUnit.getName());
                builder.loadConstant(null);
                builder.invokeStatic(mUnit.getRuntimeContext().getName(),
                    "getInvocationObserver", methodObserverType);
                builder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                    makeDesc(long.class));
                builder.loadLocal(mGlobalTime);
                builder.math(Opcode.LSUB);
                builder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                    new TypeDesc[] { makeDesc(String.class), makeDesc(String.class),
                        makeDesc(long.class) });
            }

            // Finish execute method.
            if (doReturnValue) {
                // If template returns something, return it here by reading
                // from a special field.
                builder.loadField(subFieldNames[1], returnTypeDesc);
                builder.returnValue(makeDesc(returnType.getNaturalClass()));
            }
            else {
                builder.returnVoid();
            }

            // Generate getIdentifier method.
            mi = classFile.addMethod(subIdMethod);
            subBuilder = new CodeBuilder(mi);

            TypeDesc td = makeDesc(SubstitutionId.class);
            subBuilder.newObject(td);
            subBuilder.dup();
            subBuilder.loadThis();
            subBuilder.loadThis();
            subBuilder.loadField(subFieldNames[0], TypeDesc.INT);
            subBuilder.invokeConstructor
                (td.getRootName(),
                 new TypeDesc[]{TypeDesc.OBJECT, TypeDesc.INT});
            subBuilder.returnValue(td);

            // Generate detach method.
            mi = classFile.addMethod(subDetachMethod);
            subBuilder = new CodeBuilder(mi);
            subBuilder.loadThis();
            subBuilder.invokeVirtual("clone", TypeDesc.OBJECT);
            subBuilder.checkCast(thisType);
            LocalVariable sub = subBuilder.createLocalVariable(null, thisType);
            subBuilder.storeLocal(sub);
            subBuilder.loadLocal(sub);
            subBuilder.loadConstant(null);
            subBuilder.storeField(gen.mContextParam.getVariable().getName(),
                                  paramTypes[0]);
            if (t.hasSubstitutionParam()) {
                // Also detach internal substitution parameter.
                subBuilder.loadLocal(sub);
                subBuilder.loadLocal(sub);
                String subFieldName = gen.mSubParam.getVariable().getName();
                TypeDesc subType = makeDesc(subClass);
                subBuilder.loadField(subFieldName, subType);
                subBuilder.invoke(subDetachMethod);
                subBuilder.storeField(subFieldName, subType);
            }
            subBuilder.loadLocal(sub);
            subBuilder.returnValue(TypeDesc.OBJECT);
        }

        // Done building the static execute method.

        // Build the private constructor.

        Modifiers pvt = new Modifiers();
        pvt.setPrivate(true);

        if (mGenerateSubFormat) {
            mi = classFile.addConstructor(pvt, paramTypes);
        }
        else {
            mi = classFile.addConstructor(pvt);
        }

        builder = new CodeBuilder(mi);
        builder.loadThis();
        builder.invokeSuperConstructor();

        if (mGenerateSubFormat) {
            // Copy all the params to fields.

            localVars = builder.getParameters();

            for (int i=0; i<localVars.length; i++) {
                builder.loadThis();
                builder.loadLocal(localVars[i]);
                builder.storeField(allParams[i].getName(), paramTypes[i]);
            }
        }

        builder.returnVoid();

        // Build static initializer, if required.

        if (mInitializerStatements.size() > 0) {
            mi = classFile.addInitializer();
            builder = new CodeBuilder(mi);

            Visitor gen = new Visitor(new Variable[0]);

            for (int i=0; i<mInitializerStatements.size(); i++) {
                Statement stmt = (Statement)mInitializerStatements.get(i);
                gen.generateNormalFormat(builder, stmt);
            }

            builder.returnVoid();
        }

        // Done building static initializer, if required.

        // Create any necessary private fields, after all methods have been
        // defined.
        Iterator<Variable> it = mFields.values().iterator();
        Modifiers flags = new Modifiers();
        flags.setPrivate(true);
        while (it.hasNext()) {
            Variable v = it.next();
            flags.setStatic(v.isStatic());
            flags.setTransient(v.isTransient());
            TypeDesc td = makeDesc(v);
            classFile.addField(flags, v.getName(), td).markSynthetic();
        }
    }

    private class Visitor implements NodeVisitor {
        private CodeBuilder mBuilder;
        private int mLastLine = -1;

        // Maps Variable objects to classfile.LocalVariable objects or
        // to null if they are fields.
        private Map<Variable, LocalVariable> mVariableMap =
            new HashMap<Variable, LocalVariable>();

        private boolean mAllowInitializerStatements;

        private VariableRef mContextParam;
        private VariableRef mSubParam;

        private VariableRef mBlockId;
        private VariableRef mReturnValue;

        private LocalVariable mStartTime;

        // A stack of branch locations and block finalizer statements. The
        // branch location is pushed by the foreach statement, and the
        // finalizers are pushed by block statements. The break statement code
        // generator walks this stack until it finds a branch location. Along
        // the way, it generates any finalizer statements.
        private List<Object> mBreakInfoStack;

        // Is used when generating the substitution format to gather up
        // switch cases.
        private List<Node> mCaseNodes;

        // Exception guard handlers that need to be generated at the end.
        private List<GuardHandler> mExceptionGuardHandlers;

        /**
         * Construct a Vistor for generating code that can only read template
         * parameters from fields.
         */
        public Visitor(Variable[] params) {
            this(params, null);
        }

        /**
         * Construct a Vistor for generating code that assumes some (or all) of
         * the template parameters are available as local variables.
         */
        public Visitor(Variable[] params, LocalVariable[] localVars) {
            mBreakInfoStack = new ArrayList<Object>();
            // Declare parameter variables.
            for (int i=0; i<params.length; i++) {
                Variable param = params[i];

                if (param.isField()) {
                    declareVariable(param);
                }
                else {
                    declareVariable(param, localVars[i]);
                }
            }
        }

        /**
         * Calling this grants the ability to create or move certain
         * statements to a static initializer. The only time this is usually
         * not called is when constructing the static initializer itself.
         */
        public void allowInitializerStatements() {
            mAllowInitializerStatements = true;
        }

        /**
         * Invoked to generate static execute method for template with no
         * internal substitution blocks. Also use to generate static
         * initializer statements.
         */
        public void generateNormalFormat(CodeBuilder builder, Node node) {
            mBuilder = builder;
            generate(node);
            generateExceptionHandlers();
        }

        /**
         * Invoked to generate Substitution.substitute(Context) method.
         */
        public String[] generateSubFormat(CodeBuilder builder, Template node) {
            mBuilder = builder;

            // Acquire input parameter for use as context.
            final LocalVariable param = builder.getParameters()[0];
            Variable contextVar = mContextParam.getVariable();
            Variable newLocalContext =
                new Variable(null, contextVar.getName(), contextVar.getType());
            declareVariable(newLocalContext, param);

            // Cast local context parameter to ensure its the right type.
            mBuilder.loadLocal(param);
            mBuilder.checkCast(makeDesc(contextVar));
            mBuilder.storeLocal(param);

            // Store context in field in case a substitution is exported from
            // this template.
            storeToVariable(contextVar, new Runnable() {
                public void run() {
                    mBuilder.loadLocal(param);
                }
            });
            contextVar.setField(false);

            // Create ReturnValue field if template returns a value.
            Type returnType = node.getReturnType();
            if (!(Type.VOID_TYPE.equals(returnType))) {
                Variable returnValue =
                    new Variable(null, "returnValue", returnType);
                returnValue.setField(true);
                declareVariable(returnValue);

                mReturnValue = new VariableRef(null, returnValue.getName());
                mReturnValue.setVariable(returnValue);
            }

            // Create blockId field.
            Type blockIdType = Type.INT_TYPE;
            Variable blockId = new Variable(null, "blockId", blockIdType);
            blockId.setField(true);
            declareVariable(blockId);

            mBlockId = new VariableRef(null, blockId.getName());
            mBlockId.setVariable(blockId);

            // switch (blockId) {...
            mBuilder.loadThis();
            mBuilder.loadField(blockId.getName(), TypeDesc.INT);

            // Create case labels.
            int[] cases = new int[mSubBlockCount + 1];
            Label[] switchLabels = new Label[mSubBlockCount + 1];
            mCaseNodes = new ArrayList<Node>(mSubBlockCount + 1);

            for (int i=0; i <= mSubBlockCount; i++) {
                cases[i] = i;
                switchLabels[i] = mBuilder.createLabel();
            }

            Label defaultLabel = mBuilder.createLabel();

            mBuilder.switchBranch(cases, switchLabels, defaultLabel);

            int size = 0;
            int newSize;
            mCaseNodes.add(node);
            while ( (newSize = mCaseNodes.size()) > size) {
                for (; size < newSize; size++) {
                    if (size > 0) {
                        mBuilder.returnVoid();
                    }
                    switchLabels[size].setLocation();

                    // Inject pre-call profiling bytecode.
                    TypeDesc methodObserverType =
                        TypeDesc.forClass(MergedClass.InvocationEventObserver.class);

                    boolean profilingEnabled = isProfilingEnabled();

                    LocalVariable startTime = mBuilder.createLocalVariable("blockTime",
                        TypeDesc.forClass(long.class));
                    if (profilingEnabled && size > 0) {
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                           TypeDesc.forClass(long.class));
                        mBuilder.storeLocal(startTime);
                    }

                    generate(mCaseNodes.get(size));

                    // Inject post-call profiling bytecode.
                    if (profilingEnabled && size > 0) {
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.loadConstant(mUnit.getName());
                        mBuilder.loadConstant(mCallerToSubNoList.get(size - 1));
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                            TypeDesc.forClass(long.class));
                        mBuilder.loadLocal(startTime);
                        mBuilder.math(Opcode.LSUB);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                            new TypeDesc[] { TypeDesc.forClass(String.class), TypeDesc.forClass(String.class),
                                TypeDesc.forClass(long.class) });
                    }

                }
            }

            defaultLabel.setLocation();
            mBuilder.returnVoid();

            generateExceptionHandlers();

            return new String[] {
                mBlockId.getName(),
                (mReturnValue != null) ? mReturnValue.getName() : null
            };
        }

        /**
         * Just pushes the context object reference onto the stack.
         */
        public void generateContext(CodeBuilder builder) {
            mBuilder = builder;
            if (mContextParam == null) {
                throw new NullPointerException("Context parameter is null");
            }
            else {
                generate(mContextParam);
            }
        }

        private void generate(Node node) {
            try {
                setLineNumber(node.getSourceInfo());
                if (!(node instanceof Expression)) {
                    node.accept(this);
                    return;
                }

                // Expressions require special attention for type conversion.

                Expression expr = (Expression)node;
                List<Conversion> conversions = expr.getConversionChain();

                // Iterate conversion chain backwards at first.
                ListIterator<Conversion> it =
                    conversions.listIterator(conversions.size());

                while (it.hasPrevious()) {
                    typeConvertBegin(it.previous());
                }

                expr.accept(this);

                while (it.hasNext()) {
                    typeConvertEnd(it.next());
                }
            }
            catch (DetailException e) {
                throw e;
            }
            catch (RuntimeException e) {
                throw new DetailException(e, "(near line " + mLastLine + ')');
            }
        }

        private void generateExceptionHandlers() {
            if (mExceptionGuardHandlers == null) {
                return;
            }

            int size = mExceptionGuardHandlers.size();
            if (size == 0) {
                return;
            }

            Label dumpException = mBuilder.createLabel();

            for (int i=0; i<size; i++) {
                GuardHandler gh = mExceptionGuardHandlers.get(i);
                mBuilder.exceptionHandler(gh.tryStart, gh.tryEnd,
                                          "java.lang.RuntimeException");
                // Subroutine will pop exception object off stack.
                mBuilder.jsr(dumpException);

                if (gh.replacement != null) {
                    generate(gh.replacement);
                }

                mBuilder.branch(gh.tryEnd);
            }

            dumpException.setLocation();

            // Generate code to pass caught exception to
            // ThreadGroup.uncaughtException.

            TypeDesc throwableDesc = makeDesc(Throwable.class);
            TypeDesc threadDesc = makeDesc(Thread.class);
            TypeDesc threadGroupDesc = makeDesc(ThreadGroup.class);

            LocalVariable exception =
                mBuilder.createLocalVariable("e", throwableDesc);
            LocalVariable retAddr =
                mBuilder.createLocalVariable("addr", TypeDesc.OBJECT);
            LocalVariable thread =
                mBuilder.createLocalVariable("t", threadDesc);

            // Capture return address.
            mBuilder.storeLocal(retAddr);

            // Assume caller has placed exception on stack.
            mBuilder.storeLocal(exception);

            mBuilder.invokeStatic("java.lang.Thread", "currentThread",
                                  threadDesc);
            mBuilder.storeLocal(thread);
            mBuilder.loadLocal(thread);
            mBuilder.invokeVirtual("java.lang.Thread", "getThreadGroup",
                                   threadGroupDesc);
            mBuilder.loadLocal(thread);
            mBuilder.loadLocal(exception);
            mBuilder.invokeVirtual
                ("java.lang.ThreadGroup", "uncaughtException", null,
                 new TypeDesc[]{threadDesc, throwableDesc});

            mBuilder.ret(retAddr);
        }

        //
        // Begin implementation of Visitor interface.
        //

        public Object visit(Template node) {
            // Generate the template body.
            Statement stmt = node.getStatement();
            if (stmt != null) {
                generate(stmt);
            }

            return null;
        }

        public Object visit(Name node) {
            return null;
        }

        public Object visit(TypeName node) {
            return null;
        }

        public Object visit(Variable node) {
            declareVariable(node);
            return null;
        }

        public Object visit(ExpressionList node) {
            return null;
        }

        public Object visit(Statement node) {
            return null;
        }

        public Object visit(ImportDirective node) {
            return null;
        }

        public Object visit(StatementList node) {
            Statement[] list = node.getStatements();
            if (list == null) {
                return null;
            }

            for (int i=0; i<list.length; i++) {
                generate(list[i]);
            }

            return null;
        }

        public Object visit(Block node) {
            Statement init = node.getInitializer();
            if (init != null) {
                generate(init);
            }

            Statement fin = node.getFinalizer();

            // Push the finalizer so that any break statement(s) within the
            // statement list can execute it.
            if (fin != null) {
                mBreakInfoStack.add(fin);
            }

            visit((StatementList)node);

            if (fin != null) {
                generate(fin);
                mBreakInfoStack.remove(mBreakInfoStack.size() - 1);
            }

            return null;
        }

        public Object visit(AssignmentStatement node) {
            VariableRef lvalue = node.getLValue();
            Type ltype = lvalue.getType();
            Variable v = lvalue.getVariable();
            LocalVariable local = getLocalVariable(v);

            final Expression rvalue = node.getRValue();
            Type rtype = rvalue.getType();

            // Special optimization to convert a = a + n to a += n
            if (local != null &&
                ltype.getNaturalClass() == int.class &&
                rtype.getNaturalClass() == int.class &&
                rvalue instanceof ArithmeticExpression) {

                ArithmeticExpression arith = (ArithmeticExpression)rvalue;
                int ID = arith.getOperator().getID();
                if (ID == Token.PLUS || ID == Token.MINUS) {
                    Expression left = arith.getLeftExpression();
                    Expression right = arith.getRightExpression();
                    Integer amount = null;
                    if (left instanceof VariableRef &&
                        right.isValueKnown()) {
                        if ( ((VariableRef)left).getVariable() == v ) {
                            amount = (Integer)right.getValue();
                        }
                    }
                    else if (right instanceof VariableRef &&
                             left.isValueKnown() &&
                             ID != Token.MINUS) {
                        if ( ((VariableRef)right).getVariable() == v ) {
                            amount = (Integer)left.getValue();
                        }
                    }

                    if (amount != null) {
                        int i = amount.intValue();
                        if (ID == Token.PLUS) {
                            mBuilder.integerIncrement(local, i);
                        }
                        else {
                            mBuilder.integerIncrement(local, -i);
                        }
                        return null;
                    }
                }
            }

            storeToVariable(v, new Runnable() {
                public void run() {
                    generate(rvalue);
                }
            });

            return null;
        }

        public Object visit(ForeachStatement node) {
            // Push a label for this foreach loop. Break statement will
            // us this label to break out of loop.
            Label breakLoc = mBuilder.createLabel();
            mBreakInfoStack.add(new BreakAndContinueLabels(breakLoc));

            if (node.getEndRange() == null) {
                if (node.getRange().getType().getObjectClass().isArray()) {
                    generateForeachArray(node);
                }
                else {
                    generateForeachIterator(node);
                }
            }
            else {
                generateForeachRange(node);
            }

            mBreakInfoStack.remove(mBreakInfoStack.size() - 1);
            breakLoc.setLocation();

            return null;
        }

        public Object visit(BreakStatement node) {
            // Walk backwards, looking for finalizer statements. Stop when
            // a branch location is found.
            for (int i = mBreakInfoStack.size(); --i >= 0; ) {
                Object obj = mBreakInfoStack.get(i);
                if (obj instanceof BreakAndContinueLabels) {
                    mBuilder.branch(((BreakAndContinueLabels) obj).getBreakLabel());
                    break;
                }
                else if (obj instanceof Statement) {
                    generate((Statement)obj);
                }
            }

            return null;
        }

        public Object visit(ContinueStatement node) {
            // Walk backwards, looking for finalizer statements. Stop when
            // a branch location is found.
            for (int i = mBreakInfoStack.size(); --i >= 0; ) {
                Object obj = mBreakInfoStack.get(i);
                if (obj instanceof BreakAndContinueLabels) {
                    mBuilder.branch(((BreakAndContinueLabels) obj).getContinueLabel());
                    break;
                }
                else if (obj instanceof Statement) {
                    generate((Statement)obj);
                }
            }

            return null;
        }

        public Object visit(IfStatement node) {
            Statement thenPart = node.getThenPart();
            Statement elsePart = node.getElsePart();

            Label elseLabel = mBuilder.createLabel();
            Label endLabel = mBuilder.createLabel();

            if (thenPart == null) {
                generateBranch(node.getCondition(), endLabel, true);
            }
            else {
                generateBranch(node.getCondition(), elseLabel, false);
            }

            if (thenPart != null) {
                generate(thenPart);
                if (elsePart != null) {
                    mBuilder.branch(endLabel);
                }
            }

            elseLabel.setLocation();

            if (elsePart != null) {
                generate(elsePart);
            }

            endLabel.setLocation();

            return null;
        }

        public Object visit(SubstitutionStatement node) {

            // Inject pre-call profiling bytecode.
            TypeDesc methodObserverType =
                TypeDesc.forClass(MergedClass.InvocationEventObserver.class);

            boolean profilingEnabled = isProfilingEnabled();

            if (profilingEnabled) {
                if (mSubTime == null)
                    mSubTime = mBuilder.createLocalVariable("subTime",
                        TypeDesc.forClass(long.class));
                mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                    "getInvocationObserver", methodObserverType);
                mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                   TypeDesc.forClass(long.class));
                mBuilder.storeLocal(mSubTime);
            }


            Class<?> subClass = Substitution.class;
            Method subMethod;
            try {
                // Always pass context into substitution in order for this
                // template's own substitutions to be safely detached.
                subMethod = subClass.getMethod
                    ("substitute", new Class[]{Context.class});
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e.toString());
            }

            generate(mSubParam);
            generateContext();
            mBuilder.invoke(subMethod);


            // Inject post-call profiling bytecode.
            if (profilingEnabled) {
                mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                    "getInvocationObserver", methodObserverType);
                mBuilder.loadConstant(mUnit.getName());
                mBuilder.loadConstant("__substitution");
                mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                    "getInvocationObserver", methodObserverType);
                mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                    TypeDesc.forClass(long.class));
                mBuilder.loadLocal(mSubTime);
                mBuilder.math(Opcode.LSUB);
                mBuilder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                    new TypeDesc[] { TypeDesc.forClass(String.class), TypeDesc.forClass(String.class),
                        TypeDesc.forClass(long.class) });
            }

            return null;
        }

        public Object visit(ExpressionStatement node) {
            Method receiver = node.getReceiverMethod();

            if (receiver != null &&
                !Modifier.isStatic(receiver.getModifiers())) {
                generateContext();
            }

            generate(node.getExpression());

            if (receiver != null) {
                mBuilder.invoke(receiver);

                Class<?> retType = receiver.getReturnType();
                if (retType != null && retType != void.class) {
                    if (makeDesc(retType).isDoubleWord()) {
                        mBuilder.pop2();
                    }
                    else {
                        mBuilder.pop();
                    }
                }
            }

            return null;
        }

        public Object visit(ReturnStatement node) {
            Expression expr = node.getExpression();

            boolean profilingEnabled = isProfilingEnabled();

            // If generating sub-format...
            if (mCaseNodes != null) {
                if (expr != null) {
                    Type type = expr.getType();
                    if (!(Type.VOID_TYPE.equals(type))) {
                        // Generating sub-format and returning non-void, so
                        // store result in a field for later retrieval.
                        mBuilder.loadThis();
                        generate(node.getExpression());
                        TypeDesc td = makeDesc(mReturnValue.getType());
                        mBuilder.storeField(mReturnValue.getName(), td);
                    }
                    else {
                        generate(node.getExpression());
                    }
                }
            }
            else if (expr != null) {
                generate(node.getExpression());
                if (profilingEnabled)
                    generateGlobalProfilingEnd();
                mBuilder.returnValue(makeDesc(expr.getType()));
            }
            else {
                if (profilingEnabled)
                    generateGlobalProfilingEnd();
                mBuilder.returnVoid();
            }

            return null;
        }

        private void generateGlobalProfilingEnd() {

            // inject template body profiling bytecode - end and generate event
            TypeDesc methodObserverType = makeDesc(MergedClass.InvocationEventObserver.class);
            mBuilder.invokeStatic(mUnit.getRuntimeContext().getName(),
                "getInvocationObserver", methodObserverType);
            mBuilder.loadConstant(mUnit.getName());
            mBuilder.loadConstant(null);
            mBuilder.invokeStatic(mUnit.getRuntimeContext().getName(),
                "getInvocationObserver", methodObserverType);
            mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                makeDesc(long.class));
            mBuilder.loadLocal(mGlobalTime);
            mBuilder.math(Opcode.LSUB);
            mBuilder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                new TypeDesc[] { makeDesc(String.class), makeDesc(String.class),
                    makeDesc(long.class) });
        }

        public Object visit(ExceptionGuardStatement node) {
            Statement guarded = node.getGuarded();
            if (guarded == null) {
                return null;
            }
            Statement replacement = node.getReplacement();

            if (guarded.isReturn()) {
                if (replacement == null || !replacement.isReturn()) {
                    // No replacement return statement, so exception shouldn't
                    // be caught.
                    generate(guarded);
                    return null;
                }
            }

            Label tryStart = mBuilder.createLabel().setLocation();
            generate(guarded);
            Label tryEnd = mBuilder.createLabel().setLocation();

            if (mExceptionGuardHandlers == null) {
                mExceptionGuardHandlers = new ArrayList<GuardHandler>();
            }

            mExceptionGuardHandlers.add
                (new GuardHandler(tryStart, tryEnd, replacement));

            return null;
        }

        public Object visit(Expression node) {
            return null;
        }

        public Object visit(ParenExpression node) {
            generate(node.getExpression());
            return null;
        }

        public Object visit(NewArrayExpression node) {
            ExpressionList list = node.getExpressionList();
            Expression[] exprs = list.getExpressions();

            Type initialType = node.getInitialType();
            Class<?> initialClass = initialType.getObjectClass();

            // Check if this array is composed entirely of constants,
            // and if so, make the array a static field, and populate
            // it in a static initializer.
            if (mAllowInitializerStatements && node.isAllConstant()) {
                SourceInfo info = node.getSourceInfo();

                // Create a variable...
                Variable var = new Variable(info, "array", initialType);
                var.setStatic(true);
                generate(var);
                // Create an assignment statement...
                VariableRef ref = new VariableRef(info, "array");
                ref.setVariable(var);

                // Clone the NewArrayExpression so that the type can be
                // changed back to the underlying array type. This prevents
                // unecessary casting in the assignment statement.
                Expression clonedNode = (Expression)node.clone();
                clonedNode.setType(initialType);

                AssignmentStatement assn =
                    new AssignmentStatement(info, ref, clonedNode);

                // Move this statement into the static initializer
                mInitializerStatements.add(assn);

                // Substitute a field access for a NewArrayExpression
                TypeDesc td = makeDesc(initialClass);
                mBuilder.loadStaticField(var.getName(), td);
            }
            else if (node.isAssociative()) {
                // Constuct LinkedHashMap to support associative array.
                TypeDesc td = makeDesc(LinkedHashMap.class);
                mBuilder.newObject(td);
                mBuilder.dup();

                int capacity = exprs.length;
                if (capacity == 0) {
                    // Should probably use a JDK1.3 EMPTY_MAP, and not wrap
                    // with unmodifiableMap.
                }
                else if (capacity == 2) {
                    // Should probably use a JDK1.3 singletonMap, and not wrap
                    // with unmodifiableMap.
                    capacity = 1;
                }
                else {
                    // Initial capacity at least 2n + 1.
                    BigInteger cap = BigInteger.valueOf(capacity + 1);
                    // Ensure capacity is prime to reduce hash collisions.
                    while (!cap.isProbablePrime(100)) {
                        cap = cap.add(BigInteger.valueOf(2));
                    }
                    capacity = cap.intValue();
                }

                mBuilder.loadConstant(capacity);
                mBuilder.invokeConstructor
                    (LinkedHashMap.class.getName(), cIntParam);

                Method putMethod;
                try {
                    putMethod = LinkedHashMap.class.getMethod
                        ("put", new Class[] {Object.class, Object.class});
                }
                catch (NoSuchMethodException e) {
                    throw new RuntimeException(e.toString());
                }

                boolean doPop = (putMethod.getReturnType() != void.class);

                // Perform put operations to populate the Map.
                for (int i = 0; i < exprs.length; i += 2) {
                    mBuilder.dup(); // duplicate the Map
                    generate(exprs[i]); // generate the key
                    generate(exprs[i + 1]); // generate the value
                    mBuilder.invoke(putMethod);
                    if (doPop) {
                        mBuilder.pop();
                    }
                }

                // Prevent map modification by rogue Java functions.
                if (node.isAllConstant()) {
                    try {
                        Method unmodifiable = Collections.class.getMethod
                            ("unmodifiableMap", new Class[] {Map.class});
                        mBuilder.invoke(unmodifiable);
                    }
                    catch (NoSuchMethodException e) {
                    }
                }
            }
            else {
                Class<?> componentClass = initialClass.getComponentType();
                TypeDesc componentType = makeDesc(componentClass);

                mBuilder.loadConstant(exprs.length);
                mBuilder.newObject(componentType.toArrayType());

                // Populate the array.
                for (int i=0; i<exprs.length; i++) {
                    Expression expr = exprs[i];
                    if (expr.isValueKnown()) {
                        // Check if expression value is the same as the default
                        // for values in a newly initialized array.

                        Object value = expr.getValue();
                        Type type = expr.getType();
                        if (!type.isPrimitive()) {
                            if (value == null) {
                                continue;
                            }
                        }
                        else {
                            if (value instanceof Number) {
                                if (((Number)value).doubleValue() == 0) {
                                    continue;
                                }
                            }
                            else if (value instanceof Boolean) {
                                if (((Boolean)value).booleanValue() == false) {
                                    continue;
                                }
                            }
                        }
                    }

                    mBuilder.dup();
                    mBuilder.loadConstant(i);
                    generate(expr);
                    mBuilder.storeToArray(componentType);
                }
            }

            return null;
        }

        public Object visit(FunctionCallExpression node) {
            generateCallExpression(node);
            return null;
        }

        public Object visit(TemplateCallExpression node) {
            generateCallExpression(node);
            return null;
        }

        public Object visit(VariableRef node) {
            loadFromVariable(node.getVariable());
            return null;
        }

        public Object visit(final Lookup node) {
            // generate expression
            final Expression expr = node.getExpression();
            generate(expr);
            
            // generate line number
            setLineNumber(node.getSourceInfo());
            
            // handle class references
            if (expr instanceof TypeExpression &&
                "class".equals(node.getLookupName().getName())) {
                
                mBuilder.loadClass(makeDesc(expr.getType()));
                return null;
            }
            
            // generate static field lookup if provided
            Field field = node.getReadProperty();
            if (field != null) {
            	mBuilder.loadStaticField(field.getDeclaringClass().getName(), 
            			                 field.getName(), 
            			                 makeDesc(node.getType()));
            	
            	return null;
            }
            
            // generate null-safe check if necessary
            generateNullSafe(node, expr, new NullSafeCallback() {
                public Type execute() {
                    Method readMethod = node.getReadMethod();
                    String lookupName = node.getLookupName().getName();

                    if (expr.getType().getObjectClass().isArray() &&
                        lookupName.equals("length")) {

                        mBuilder.arrayLength();
                        return Type.INT_TYPE;
                    }
                    else {
                        if (Modifier.isStatic(readMethod.getModifiers())) {
                            // Discard the object to call method on.
                            mBuilder.pop();
                        }

                        mBuilder.invoke(readMethod);
                        return new Type(readMethod.getReturnType(),
                                        readMethod.getGenericReturnType());
                    }
                }
            });

            // nothing to update
            return null;
        }

        public Object visit(final ArrayLookup node) {
            // generate expression
            final Expression expr = node.getExpression();
            generate(expr);
            
            // generate null-safe check if necessary
            generateNullSafe(node, expr, new NullSafeCallback() {
                public Type execute() {
                    Method readMethod = node.getReadMethod();
                    Expression lookup = node.getLookupIndex();
    
                    Type type = expr.getType();
                    Class<?> lookupClass = type.getObjectClass();
                    boolean doArrayLookup = lookupClass.isArray();

                    if (!doArrayLookup &&
                        Modifier.isStatic(readMethod.getModifiers())) {
    
                        // Discard the object to call method on.
                        mBuilder.pop();
                    }
    
                    // generate lookup portion
                    generate(lookup);
                    setLineNumber(node.getLookupToken().getSourceInfo());

                    if (!doArrayLookup) {
                        mBuilder.invoke(readMethod);
                        // TODO: do we need to checkCast here for generics?
                        //mBuilder.checkCast(makeDesc(node.getType()));
                        return node.getType();
                    }
                    else {
                        try {
                            Type elementType = 
                                expr.getInitialType().getArrayElementType();
                            mBuilder.loadFromArray(makeDesc(elementType));
                            return elementType;
                        }
                        catch (IntrospectionException e) {
                            throw new RuntimeException(e.toString());
                        }
                    }
                }
            });

            // nothing to update
            return null;
        }

        public Object visit(NegateExpression node) {
            Expression expr = node.getExpression();
            Class<?> exprClass = expr.getType().getNaturalClass();

            generate(expr);

            byte opcode;
            if (exprClass == int.class) {
                opcode = Opcode.INEG;
            }
            else if (exprClass == float.class) {
                opcode = Opcode.FNEG;
            }
            else if (exprClass == long.class) {
                opcode = Opcode.LNEG;
            }
            else if (exprClass == double.class) {
                opcode = Opcode.DNEG;
            }
            else {
                opcode = Opcode.INEG;
            }

            mBuilder.math(opcode);

            return null;
        }

        public Object visit(ConcatenateExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();
            Type leftType = left.getType();
            Type rightType = right.getType();

            // If concatenation requires a chain of more than two append calls,
            // generate a StringBuffer append chain.
            if (left instanceof ConcatenateExpression ||
                // Note: These are just sanity checks since the type checker is
                // supposed to ensure that sub nodes are converted to non-null
                // strings.
                leftType.isNullable() ||
                leftType.getObjectClass() != String.class ||
                rightType.isNullable() ||
                rightType.getObjectClass() != String.class) {

                generateAppend(node, estimateBufferRequirement(node));
                mBuilder.invokeVirtual("java.lang.StringBuilder", "toString",
                                       TypeDesc.STRING);
            }
            else {
                // Concatenation is of the form 'a & b' or 'a & (b & c)' so
                // call String.concat as an optimization. It allocates less
                // objects, and it does no synchronization.
                generate(left);
                generate(right);
                mBuilder.invokeVirtual("java.lang.String", "concat",
                                       TypeDesc.STRING, cStringParam);
            }

            return null;
        }

        public Object visit(ArithmeticExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();

            Type ltype = left.getType();
            Type rtype = right.getType();
            if (ltype == null || rtype == null) {
                throw new RuntimeException(
                    "ArithmeticExpression types invalid: " + 
                    ltype + ", " + rtype
                );
            }

            setLineNumber(node.getOperator().getSourceInfo());

            int ID = node.getOperator().getID();

            // handle simple scenario of primitive to primitive calculation
            if (ltype.isPrimitive() && rtype.isPrimitive()) {
                if (!ltype.equals(rtype)) {
                    throw new RuntimeException(
                        "ArithmeticExpression types must match as primitives: " +
                        ltype + ", " + rtype
                    );
                }

                byte opcode = Opcode.NOP;
                Class<?> clazz = ltype.getNaturalClass();
                if (clazz == int.class) {
                    switch (ID) {
                    case Token.PLUS:
                        opcode = Opcode.IADD;
                        break;
                    case Token.MINUS:
                        opcode = Opcode.ISUB;
                        break;
                    case Token.MULT:
                        opcode = Opcode.IMUL;
                        break;
                    case Token.DIV:
                        opcode = Opcode.IDIV;
                        break;
                    case Token.MOD:
                        opcode = Opcode.IREM;
                        break;
                    }
                }
                else if (clazz == float.class) {
                    switch (ID) {
                    case Token.PLUS:
                        opcode = Opcode.FADD;
                        break;
                    case Token.MINUS:
                        opcode = Opcode.FSUB;
                        break;
                    case Token.MULT:
                        opcode = Opcode.FMUL;
                        break;
                    case Token.DIV:
                        opcode = Opcode.FDIV;
                        break;
                    case Token.MOD:
                        opcode = Opcode.FREM;
                        break;
                    }
                }
                else if (clazz == long.class) {
                    switch (ID) {
                    case Token.PLUS:
                        opcode = Opcode.LADD;
                        break;
                    case Token.MINUS:
                        opcode = Opcode.LSUB;
                        break;
                    case Token.MULT:
                        opcode = Opcode.LMUL;
                        break;
                    case Token.DIV:
                        opcode = Opcode.LDIV;
                        break;
                    case Token.MOD:
                        opcode = Opcode.LREM;
                        break;
                    }
                }
                else if (clazz == double.class) {
                    switch (ID) {
                    case Token.PLUS:
                        opcode = Opcode.DADD;
                        break;
                    case Token.MINUS:
                        opcode = Opcode.DSUB;
                        break;
                    case Token.MULT:
                        opcode = Opcode.DMUL;
                        break;
                    case Token.DIV:
                        opcode = Opcode.DDIV;
                        break;
                    case Token.MOD:
                        opcode = Opcode.DREM;
                        break;
                    }
                }
    
                generate(node.getLeftExpression());
                generate(node.getRightExpression());
                
                mBuilder.math(opcode);
            }
            
            // otherwise, we must be using some unknown numerical value, so
            // attempt a runtime calculation (Number, etc)
            else {
                int opcode = 0;
                switch (ID) {
                case Token.PLUS:
                    opcode = WrapperTypeConversionUtil.OP_ADD;
                    break;
                case Token.MINUS:
                    opcode = WrapperTypeConversionUtil.OP_SUB;
                    break;
                case Token.MULT:
                    opcode = WrapperTypeConversionUtil.OP_MULT;
                    break;
                case Token.DIV:
                    opcode = WrapperTypeConversionUtil.OP_DIV;
                    break;
                case Token.MOD:
                    opcode = WrapperTypeConversionUtil.OP_MOD;
                    break;
                }
                
                Class<?> lclass = Number.class, rclass = Number.class;
                if (ltype.isPrimitive()) {
                    lclass = ltype.getNaturalClass();
                }
                else if (rtype.isPrimitive()) {
                    rclass = rtype.getNaturalClass();
                }
                
                Method method = getMethod(
                    WrapperTypeConversionUtil.class, "math", int.class, 
                    lclass, rclass
                );
                
                mBuilder.loadConstant(opcode);
                generate(node.getLeftExpression());
                generate(node.getRightExpression());
                
                mBuilder.invoke(method);
            }

            return null;
        }

        public Object visit(RelationalExpression node) {
            generateLogical(node);
            return null;
        }

        public Object visit(NotExpression node) {
            generateLogical(node);
            return null;
        }

        public Object visit(AndExpression node) {
            generateLogical(node);
            return null;
        }

        public Object visit(OrExpression node) {
            generateLogical(node);
            return null;
        }

        public Object visit(TernaryExpression node) {
            Expression thenPart = node.getThenPart();
            Expression elsePart = node.getElsePart();
            Expression condition = node.getCondition();

            Label elseLabel = mBuilder.createLabel();
            Label endLabel = mBuilder.createLabel();

            // if elvis relationship (then/condition the same), generate shared
            // statement
            if (thenPart == condition) {
                // create temporary assignment and reference both
                condition = thenPart = createAssignment(condition).getLValue();
            }
            
            if (thenPart == null) {
                generateBranch(condition, endLabel, true);
            }
            else {
                generateBranch(condition, elseLabel, false);
            }

            if (thenPart != null) {
                generate(thenPart);
                if (elsePart != null) {
                    mBuilder.branch(endLabel);
                }
            }

            elseLabel.setLocation();

            if (elsePart != null) {
                generate(elsePart);
            }

            endLabel.setLocation();

            return null;
        }

        public Object visit(CompareExpression node) {

            // get expressions
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();
            
            // get associated types
            Type ltype = left.getType();
            Type rtype = right.getType();

            // create local variables
            generate(left);
            LocalVariable lvar = 
                mBuilder.createLocalVariable(null, makeDesc(ltype));
            mBuilder.storeLocal(lvar);
        
            generate(right);
            LocalVariable rvar =
                mBuilder.createLocalVariable(null, makeDesc(rtype));
            mBuilder.storeLocal(rvar);

            // check if primitives and handle directly
            if (ltype.isPrimitive() && rtype.isPrimitive()) {

                if (!ltype.equals(rtype)) {
                    throw new RuntimeException(
                        "CompareExpression has invalid types: " +
                        ltype + ", " + rtype
                    );
                }
                
                generateComparison(lvar, rvar, ltype);
            }
            
            // otherwise, compare as nulls and then handle comparison
            else {
                Class<?> lclass = ltype.getNaturalClass();
                Class<?> rclass = rtype.getNaturalClass();
                
                Label location1 = mBuilder.createLabel();
                Label endLocation = mBuilder.createLabel();
                
                if (ltype.isNonNull()) {
                    if (rtype.isNullable()) {
                        // left is non-null, but right is nullable, so do a
                        // null check on right and if null we return [1], else
                        // we jump to the beginning of the comparison block
                        // as both are non-null at that point
                        mBuilder.loadLocal(rvar);
                        mBuilder.ifNullBranch(location1, false);
                        mBuilder.loadConstant(1);
                        mBuilder.branch(endLocation);
                    }
                }
                else {
                    Label location2 = mBuilder.createLabel();
                    
                    // left is nullable, so do a null check on left.  If null
                    // we check right if necessary; otherwise if non-null, we
                    // check right for nullability or do the comparison
                    mBuilder.loadLocal(lvar);
                    mBuilder.ifNullBranch(location2, false);
                    
                    if (rtype.isNonNull()) {
                        // right is non-null and at this point we have found
                        // left to be null, so just return [-1]. We also must
                        // set the location so that the left check will jump
                        // to here to begin the general comparison block
                        mBuilder.loadConstant(-1);
                        mBuilder.branch(endLocation);
                        location2.setLocation();
                    }
                    else {
                        Label location3 = mBuilder.createLabel();
                        
                        // right is nullable so we must check if right is null
                        // or not.  If right is null, then left is already null,
                        // so we purely return [0]. Otherwise
                        mBuilder.loadLocal(rvar);
                        mBuilder.ifNullBranch(location3, false);
                        mBuilder.loadConstant(0);
                        mBuilder.branch(endLocation);
                        
                        // left is already null and per the above we have now
                        // found right to be non-null, so return [-1].
                        location3.setLocation();
                        mBuilder.loadConstant(-1);
                        mBuilder.branch(endLocation);
                        
                        // branch point from the initial nullable check on left
                        // so at this point left is not null so we must now
                        // check if right is null or not.  if not null, we jump
                        // to the comparison point...otherwise, left is not and
                        // right is, so we return [1]
                        location2.setLocation();
                        mBuilder.loadLocal(rvar);
                        mBuilder.ifNullBranch(location1, false);
                        mBuilder.loadConstant(1);
                        mBuilder.branch(endLocation);
                    }
                }
                
                // comparison branch point at which point both left and right
                // are non-null
                location1.setLocation();

                // if left is primitive and right has primitive peer
                if (ltype.isPrimitive() && rtype.hasPrimitivePeer()) {
                    Type type = ltype.getCompatibleType(rtype.toPrimitive());
                    TypeDesc desc = makeDesc(type);
                    
                    LocalVariable lvar2 = lvar; 
                    if (!type.equals(ltype)) {
                        lvar2 = mBuilder.createLocalVariable(null, desc);
                    
                        mBuilder.loadLocal(lvar);
                        typeConvertEnd(ltype, type, false);
                        mBuilder.storeLocal(lvar2);
                    }
                    
                    LocalVariable rvar2 = 
                        mBuilder.createLocalVariable(null, desc);
                    mBuilder.loadLocal(rvar);
                    typeConvertEnd(rtype, type, false);
                    mBuilder.storeLocal(rvar2);
                    
                    // primitive comparison
                    generateComparison(lvar2, rvar2, type);
                }
                
                // if right is primitive and left has primitive peer
                else if (ltype.hasPrimitivePeer() && rtype.isPrimitive()) {
                    Type type = rtype.getCompatibleType(ltype.toPrimitive());
                    TypeDesc desc = makeDesc(type);
                    
                    LocalVariable lvar2 = 
                        mBuilder.createLocalVariable(null, desc);
                    mBuilder.loadLocal(lvar);
                    typeConvertEnd(ltype, type, false);
                    mBuilder.storeLocal(lvar2);
                    
                    LocalVariable rvar2 = rvar; 
                    if (!type.equals(rtype)) {
                        rvar2 = mBuilder.createLocalVariable(null, desc);
                        
                        mBuilder.loadLocal(rvar);
                        typeConvertEnd(rtype, type, false);
                        mBuilder.storeLocal(rvar2);
                    }
                    
                    // primitive comparison
                    generateComparison(lvar2, rvar2, type);
                }
                
                // if left parent class of right and comparable
                else if (lclass.isAssignableFrom(rclass) &&
                         Comparable.class.isAssignableFrom(lclass)) {
                    
                    Method method =
                        getMethod(Comparable.class, "compareTo", Object.class);
                    
                    mBuilder.loadLocal(lvar);
                    mBuilder.loadLocal(rvar);
                    mBuilder.invoke(method);
                }
                
                // if right parent class of left and comparable
                else if (rclass.isAssignableFrom(lclass) &&
                         Comparable.class.isAssignableFrom(rclass)) {
                   
                   Method method =
                       getMethod(Comparable.class, "compareTo", Object.class);
                   
                   mBuilder.loadLocal(rvar);
                   mBuilder.loadLocal(lvar);
                   mBuilder.invoke(method);
                   mBuilder.math(Opcode.INEG);
               }
                
                // if Number parent class of either object types, then we must
                // perform runtime analysis of the numbers to compare to each
                // other
                else if ((ltype.isPrimitive() || Number.class.isAssignableFrom(lclass)) &&
                         (rtype.isPrimitive() || Number.class.isAssignableFrom(rclass))) {
                
                    if (!ltype.isPrimitive()) {
                        lclass = Number.class;
                    }
                    
                    if (!rtype.isPrimitive()) {
                        rclass = Number.class;
                    }
                    
                    mBuilder.loadLocal(lvar);
                    mBuilder.loadLocal(rvar);
                    
                    Method method = getMethod(
                        WrapperTypeConversionUtil.class, "compare", 
                        lclass, rclass
                    );
                    
                    mBuilder.invoke(method);
                }
                
                // otherwise, compare at runtime as strings
                else {
                    warn("compare.not.convertible",
                          ltype.getClassName(), rtype.getClassName(), node);
                    
                    mBuilder.loadLocal(lvar);
                    if (ltype.isPrimitive()) {
                        typeConvertEnd(ltype, ltype.toNonPrimitive(), false);
                    }
                    
                    mBuilder.invoke(getMethod(Object.class, "toString"));
                    
                    mBuilder.loadLocal(rvar);
                    if (rtype.isPrimitive()) {
                        typeConvertEnd(rtype, rtype.toNonPrimitive(), false);
                    }
                    
                    mBuilder.invoke(getMethod(Object.class, "toString"));
                    
                    Method method =
                        getMethod(Comparable.class, "compareTo", Object.class);
                    mBuilder.invoke(method);
                }

                endLocation.setLocation();
            }

            return null;
        }
        
        private void generateComparison(LocalVariable lvar, LocalVariable rvar, 
                                        Type type) {
            Label endLocation = mBuilder.createLabel();
            Label ltLocation = mBuilder.createLabel();
            Label gtLocation = mBuilder.createLabel();

            mBuilder.loadLocal(lvar);
            mBuilder.loadLocal(rvar);
            generateComparison(ltLocation, "!=", type);
            mBuilder.loadConstant(0);
            mBuilder.branch(endLocation);

            ltLocation.setLocation();
            mBuilder.loadLocal(lvar);
            mBuilder.loadLocal(rvar);
            generateComparison(gtLocation, "<", type);
            mBuilder.loadConstant(1);
            mBuilder.branch(endLocation);
            
            gtLocation.setLocation();
            mBuilder.loadConstant(-1);
            
            endLocation.setLocation();
        }
        
        private void generateComparison(Label label, String choice, 
                                        Type type) {
            generateComparison(label, choice, type.getNaturalClass());
        }
        
        private void generateComparison(Label label, String choice, 
                                        Class<?> clazz) {
            if (clazz == long.class) {
                mBuilder.math(Opcode.LCMP);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else if (clazz == float.class) {
                mBuilder.math(Opcode.FCMPG);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else if (clazz == double.class) {
                mBuilder.math(Opcode.DCMPG);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else {
                mBuilder.ifComparisonBranch(label, choice);                
            }
        }
        
        private void generateZeroComparison(Label label, String choice, 
                                            Type type) {
            generateZeroComparison(label, choice, type.getNaturalClass());
        }
        
        private void generateZeroComparison(Label label, String choice, 
                                            Class<?> clazz) {
            if (clazz == long.class) {
                mBuilder.loadConstant(0L);
                mBuilder.math(Opcode.LCMP);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else if (clazz == float.class) {
                mBuilder.loadConstant(0.0f);
                mBuilder.math(Opcode.FCMPG);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else if (clazz == double.class) {
                mBuilder.loadConstant(0.0);
                mBuilder.math(Opcode.DCMPG);
                mBuilder.ifZeroComparisonBranch(label, choice);
            }
            else {
                mBuilder.ifZeroComparisonBranch(label, choice);                
            }
        }
        
        public Object visit(NoOpExpression node) {
            return null;
        }
        
        public Object visit(TypeExpression node) {
        	// nothing to do....lookup and function call expressions
        	// will handle by performing static invocations
        	return null;
        }
        
        public Object visit(SpreadExpression node) {
            // generate expression
            Expression expr = node.getExpression();
            generate(expr);
            
            // store local variable
            LocalVariable collection = 
                mBuilder.createLocalVariable(null, makeDesc(expr.getType()));
            mBuilder.storeLocal(collection);
            
            // get associated type
            Class<?> exprClass = expr.getType().getNaturalClass();
            
            // get associated element type
            Type elementType = null;
            try { elementType = expr.getType().getIterationElementType(); }
            catch (IntrospectionException exception) {
                throw new IllegalStateException(exception);
            }
            
            Class<?> elementClass = elementType.getNaturalClass();
            
            // handle null values and return null
            mBuilder.loadLocal(collection);
            Label endLabel = mBuilder.createLabel();
            Label nullLabel = mBuilder.createLabel();
            mBuilder.ifNullBranch(nullLabel, true);

            // handle collections
            if (Collection.class.isAssignableFrom(exprClass)) {
                
                // create new collection
                TypeDesc arrayList = makeDesc(node.getType());
                mBuilder.newObject(arrayList);
                mBuilder.dup();
                
                // get length of collection
                mBuilder.loadLocal(collection);
                mBuilder.invoke(getMethod(Collection.class, "size"));

                // instantiate based on length of collection
                mBuilder.invokeConstructor(node.getType().getClassName(), 
                                           makeDesc(int.class));
                
                // store instance
                LocalVariable list =
                    mBuilder.createLocalVariable(null, arrayList);
                mBuilder.storeLocal(list);
                
                // generate collection iterator
                mBuilder.loadLocal(collection);
                LocalVariable iterator = 
                    mBuilder.createLocalVariable(null, makeDesc(Iterator.class));
                
                mBuilder.invoke(getMethod(Collection.class, "iterator"));
                mBuilder.storeLocal(iterator);
                
                // generate labels for checking foreach condition
                Label endLoopLabel = mBuilder.createLabel();
                Label checkLabel = mBuilder.createLabel();
                mBuilder.branch(checkLabel);

                // start foreach loop
                Label startLabel = mBuilder.createLabel().setLocation();
                Label continueLabel = mBuilder.createLabel();

                // load list for invoking add on
                mBuilder.loadLocal(list);
                
                // get the loop variable
                mBuilder.loadLocal(iterator);
                mBuilder.invokeInterface("java.util.Iterator", "next", 
                                         TypeDesc.OBJECT);

                // avoid null types
                mBuilder.dup();
                Label addLabel = mBuilder.createLabel();
                mBuilder.ifNullBranch(addLabel, true);

                // verify cast
                if (elementClass != Object.class) {
                    mBuilder.checkCast(makeDesc(elementClass));
                }

                // generate underlying expression
                generate(node.getOperation());

                // add to list
                addLabel.setLocation();
                mBuilder.invoke(getMethod(List.class, "add", Object.class));
                mBuilder.ifZeroComparisonBranch(nullLabel, "==");

                // set entry point for continue
                continueLabel.setLocation();
                checkLabel.setLocation();
                
                // determine if has next
                mBuilder.loadLocal(iterator);
                mBuilder.invokeInterface("java.util.Iterator", "hasNext", 
                                         TypeDesc.BOOLEAN);

                mBuilder.ifZeroComparisonBranch(startLabel, "!=");

                endLoopLabel.setLocation();
                
                // load list as the result
                mBuilder.loadLocal(list);
            }
            
            // handle arrays
            else if (exprClass.isArray()) {
                
                // create new array
                Type operationType = node.getOperation().getType();
                TypeDesc arrayType = makeDesc(node.getType());
                
                // calculate length of array
                LocalVariable length =
                    mBuilder.createLocalVariable(null, TypeDesc.INT);
                mBuilder.loadLocal(collection);
                mBuilder.arrayLength();
                mBuilder.storeLocal(length);
                
                // get length of array
                mBuilder.loadLocal(length);
                mBuilder.newObject(arrayType);

                // store instance
                LocalVariable array =
                    mBuilder.createLocalVariable(null, arrayType);
                mBuilder.storeLocal(array);
                
                // create end label
                Label endLoopLabel = mBuilder.createLabel();

                // setup start value
                LocalVariable indexLocal =
                    mBuilder.createLocalVariable(null, TypeDesc.INT);
                mBuilder.loadConstant(0);
                mBuilder.storeLocal(indexLocal);

                // perform initial loop check
                Label checkLabel = mBuilder.createLabel();
                mBuilder.branch(checkLabel);

                // start loop body
                Label startLabel = mBuilder.createLabel().setLocation();
                Label continueLabel = mBuilder.createLabel();

                // load array for adding
                mBuilder.loadLocal(array);
                mBuilder.loadLocal(indexLocal);
                
                // load array value
                mBuilder.loadLocal(collection);
                mBuilder.loadLocal(indexLocal);
                mBuilder.loadFromArray(TypeDesc.OBJECT);

                // avoid null types
                mBuilder.dup();
                Label addLabel = mBuilder.createLabel();
                Label null2Label = mBuilder.createLabel();
                mBuilder.ifNullBranch(null2Label, true);
                
                // generate underlying expression
                generate(node.getOperation());
                mBuilder.branch(addLabel);

                // handle null default values
                null2Label.setLocation();
                mBuilder.pop();
                if (operationType.isPrimitive()) {
                    Class<?> clazz = operationType.getNaturalClass();
                    if (int.class.equals(clazz)) {
                        mBuilder.loadConstant(0);
                    }
                    else if (boolean.class.equals(clazz)) {
                        mBuilder.loadConstant(false);
                    }
                    else if (double.class.equals(clazz)) {
                        mBuilder.loadConstant(0.0);
                    }
                    else if (float.class.equals(clazz)) {
                        mBuilder.loadConstant(0.0f);
                    }
                    else if (long.class.equals(clazz)) {
                        mBuilder.loadConstant(0L);
                    }
                    else { mBuilder.loadConstant(0); }
                }
                else { mBuilder.loadNull(); }
                
                // store to array
                addLabel.setLocation();
                mBuilder.storeToArray(makeDesc(operationType));

                // set entry point for continue
                continueLabel.setLocation();

                // build check label location and index adjustment
                mBuilder.integerIncrement(indexLocal, 1);
                checkLabel.setLocation();

                mBuilder.loadLocal(indexLocal);
                mBuilder.loadLocal(length);
                mBuilder.ifComparisonBranch(startLabel, "<");

                endLoopLabel.setLocation();
                
                // load array as the result
                mBuilder.loadLocal(array);
            }

            // skip past null check to end
            mBuilder.branch(endLabel);
            
            // null handle checking
            nullLabel.setLocation();
            mBuilder.loadNull();

            // finish operation
            endLabel.setLocation();

            return null;
        }
        
        public Object visit(NullLiteral node) {
            mBuilder.loadNull();
            return null;
        }

        public Object visit(BooleanLiteral node) {
            boolean value = ((Boolean)node.getValue()).booleanValue();
            Type type = node.getType();
            Class<?> clazz = type.getNaturalClass();
            if (clazz == boolean.class) {
                mBuilder.loadConstant(value);
            }
            else if (clazz.isAssignableFrom(Boolean.class)) {
                TypeDesc td = makeDesc(Boolean.class);

                if (value) {
                    mBuilder.loadStaticField("java.lang.Boolean", "TRUE", td);
                }
                else {
                    mBuilder.loadStaticField("java.lang.Boolean", "FALSE", td);
                }
            }
            else if (clazz.isAssignableFrom(String.class)) {
                mBuilder.loadConstant(String.valueOf(value));
            }
            else {
                typeError(Type.BOOLEAN_TYPE, type);
            }

            return null;
        }

        public Object visit(StringLiteral node) {
            mBuilder.loadConstant((String)node.getValue());
            return null;
        }

        public Object visit(NumberLiteral node) {
            Number value = (Number)node.getValue();
            Type toType = node.getType();
            Type fromType;

            if (Number.class.isAssignableFrom(toType.getObjectClass())) {
                fromType = toType.toPrimitive();
            }
            else {
                fromType = new Type(value.getClass()).toPrimitive();
            }

            Class<?> fromClass = fromType.getObjectClass();

            if (Integer.class.isAssignableFrom(fromClass)) {
                mBuilder.loadConstant(value.intValue());
            }
            else if (Float.class.isAssignableFrom(fromClass)) {
                mBuilder.loadConstant(value.floatValue());
            }
            else if (Long.class.isAssignableFrom(fromClass)) {
                mBuilder.loadConstant(value.longValue());
            }
            else if (Double.class.isAssignableFrom(fromClass)) {
                mBuilder.loadConstant(value.doubleValue());
            }
            else if (value instanceof Long) {
                mBuilder.loadConstant(value.longValue());
            }
            else if (value instanceof Float) {
                mBuilder.loadConstant(value.floatValue());
            }
            else if (value instanceof Double) {
                mBuilder.loadConstant(value.doubleValue());
            }
            else {
                mBuilder.loadConstant(value.intValue());
            }

            return null;
        }

        //
        // End implementation of Visitor interface.
        //

        private void generateContext() {
            if (mContextParam == null) {
                throw new NullPointerException("Context parameter is null");
            }
            else {
                generate(mContextParam);
            }
        }

        private void generateBranch(Expression expr, Label label,
                                    boolean whenTrue) {
            generateBranch(expr, label, whenTrue, false);
        }
        
        private void generateBranch(Expression expr, Label label,
                                    boolean whenTrue, boolean invert) {
            
            // get the simple state for handling whenTrue
            // if this is an inversion (ie: not operation), then we use the
            // opposite.  Note that this does not apply for compound statements
            // such as truthful expressions which requires the aggregate to be
            // the opposite rather than its individual parts
            boolean _whenTrue = (invert ? !whenTrue : whenTrue);
            
            if (expr instanceof Logical) {
                // What follows is something that the visitor design pattern
                // solves, but I would need to make a special visitor
                // interface that operates only on logicals that also accepts
                // labels and "whenTrue" flags.

                if (expr instanceof RelationalExpression) {
                    generateBranch((RelationalExpression)expr, label, _whenTrue);
                }
                else if (expr instanceof NotExpression) {
                    generateBranch((NotExpression)expr, label, _whenTrue);
                }
                else if (expr instanceof AndExpression) {
                    generateBranch((AndExpression)expr, label, _whenTrue);
                }
                else if (expr instanceof OrExpression) {
                    generateBranch((OrExpression)expr, label, _whenTrue);
                }
            }
            else {
                // Generate branch in the same special way for all
                // non-logical expressions.
                Type type = expr.getType();
                if (expr.isValueKnown()) {
                    Object value = expr.getValue();
                    if (value == null && !_whenTrue) {
                        mBuilder.branch(label);
                    }
                    else if (value instanceof Truthful) {
                        boolean isTrue = ((Truthful) value).isTrue();
                        if ((!isTrue && !_whenTrue) || (isTrue && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value instanceof Boolean) {
                        boolean isTrue = ((Boolean) value).booleanValue();
                        if ((!isTrue && !_whenTrue) || (isTrue && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value instanceof Number) {
                        boolean valid = 
                            WrapperTypeConversionUtil.isValid((Number) value);
                        if ((!valid && !_whenTrue) || (valid && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value instanceof String) {
                        int length = ((String) value).length();
                        if ((length == 0 && !_whenTrue) ||
                            (length != 0 && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value != null && value.getClass().isArray()) {
                        int length = Array.getLength(value);
                        if ((length == 0 && !_whenTrue) ||
                            (length != 0 && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value instanceof Collection) {
                        int length = ((Collection<?>) value).size();
                        if ((length == 0 && !_whenTrue) ||
                            (length != 0 && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value instanceof Map) {
                        int length = ((Map<?, ?>) value).size();
                        if ((length == 0 && !_whenTrue) ||
                            (length != 0 && _whenTrue)) {
                            mBuilder.branch(label);
                        }
                    }
                    else if (value != null && _whenTrue) {
                        mBuilder.branch(label);
                    }
                }
                else if (type == null) {
                    // unknown type, just perform straight comparison
                    generate(expr);
                    mBuilder.ifZeroComparisonBranch(label, _whenTrue ? "!=" : "==");
                }
                else if (type.isPrimitive()) {
                    generate(expr);
                    Class<?> clazz = type.getNaturalClass();
                    if (long.class.equals(clazz)) {
                           mBuilder.loadConstant(0L);
                           mBuilder.math(Opcode.LCMP);
                           mBuilder.ifZeroComparisonBranch(label, _whenTrue ? "!=" : "==");
                    }
                    else if (float.class.equals(clazz)) {
                        mBuilder.loadConstant(0.0f);
                        mBuilder.math(Opcode.FCMPG);
                        mBuilder.ifZeroComparisonBranch(label, _whenTrue ? "!=" : "==");
                    }
                    else if (double.class.equals(clazz)) {
                        mBuilder.loadConstant(0.0);
                        mBuilder.math(Opcode.DCMPG);
                        mBuilder.ifZeroComparisonBranch(label, _whenTrue ? "!=" : "==");
                    }
                    else {
                        // standard type, just perform straight comparison
                        mBuilder.ifZeroComparisonBranch(label, _whenTrue ? "!=" : "==");
                    }
                }
                else {
                    generate(expr);

                    // load the variable for variable refs to avoid duplicating
                    // local variables
                    Variable var = null;
                    if (expr instanceof VariableRef) {
                        var = ((VariableRef) expr).getVariable(); 
                    }
                    
                    // create a temp local variable for non-variable refs
                    // as long as we are nullable such that we must make two
                    // checks (null and truth)
                    LocalVariable local = null;
                    if (var == null && type.isNullable()) {
                        TypeDesc desc = makeDesc(type);
                        if (desc.isDoubleWord()) { mBuilder.dup2(); }
                        else { mBuilder.dup(); }
                        
                        local = mBuilder.createLocalVariable(null, desc);
                        mBuilder.storeLocal(local);
                    }

                    // create a branch to go to in order to evaluate the truth
                    // check after the null check...only valid if we need the
                    // null check
                    Label branch = null;
                    if (_whenTrue && type.isNullable()) {
                        branch = mBuilder.createLabel();
                    }
                    
                    // test for null (false value)
                    if (type.isNullable()) {
                        mBuilder.ifNullBranch(_whenTrue ? branch : label, true);

                        // load the data again to do further testing after the
                        // null check...if we have a local variable from 
                        // earlier, load it otherwise, if we have a reference 
                        // load that instead
                        if (local != null) { mBuilder.loadLocal(local); }
                        else if (var != null) { loadFromVariable(var); }
                    }
                    
                    // get the associated class for comparison
                    Class<?> clazz = type.getNaturalClass();

                    // handle truthful values
                    if (Truthful.class.isAssignableFrom(clazz)) {
                        mBuilder.invoke(getMethod(Truthful.class, "isTrue"));
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }

                    // handle boolean values
                    else if (Boolean.class.isAssignableFrom(clazz)) {
                        mBuilder.invoke(getMethod(Boolean.class, "booleanValue"));
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }

                    // handle numeric values
                    else if (Number.class.isAssignableFrom(clazz)) {
                        Type ctype = 
                            type.getCompatibleType(Type.INT_TYPE).toPrimitive();
                        
                        // handle types that are primitive and known
                        if (ctype.isPrimitive()) {
                            if (!type.equals(ctype)) {
                                typeConvertEnd(type, ctype, false);
                            }
                            
                            generateZeroComparison(label, 
                                _whenTrue ? "!=" : "==", ctype);
                        }
                        
                        // otherwise, test at runtime to evaluate
                        else {
                            Method method = getMethod
                            (
                                WrapperTypeConversionUtil.class, 
                                "isValid", Number.class
                            );
                            
                            mBuilder.invoke(method);
                            mBuilder.ifZeroComparisonBranch(label, 
                                _whenTrue ? "!=" : "==");
                        }
                    }

                    // handle string values
                    else if (String.class.isAssignableFrom(clazz)) {
                        mBuilder.invoke(getMethod(String.class, "length"));
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }

                    // handle array values
                    else if (clazz.isArray()) {
                        mBuilder.arrayLength();
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }

                    // handle collection values
                    else if (Collection.class.isAssignableFrom(clazz)) {
                        mBuilder.invoke(getMethod(Collection.class, "size"));
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }
                    
                    // handle map values
                    else if (Map.class.isAssignableFrom(clazz)) {
                        mBuilder.invoke(getMethod(Map.class, "size"));
                        mBuilder.ifZeroComparisonBranch(label, 
                            _whenTrue ? "!=" : "==");
                    }

                    // any other case, just pop last value (assume true)
                    // if expected true, then branch
                    else {
                        // warn("truthful.object.expression", expr);
                        
                        mBuilder.pop();
                        if (_whenTrue) { 
                            mBuilder.branch(label); 
                        }
                    }

                    // set the location to jump the null check to for when the
                    // request was to jump to the actual label if only true
                    // this branch is placed here as the next lines of code
                    // are the false aspect.  If the null check fails, it is
                    // not true and thus it jump here.
                    if (branch != null) {
                        branch.setLocation();
                    }
                }
            }
        }

        private Method getMethod(Class<?> clazz, String method, Class<?>... params) {
            try {
                return clazz.getMethod(method, params);
            } catch (Exception e) {
                throw new RuntimeException(
                    "unable to get method: " + clazz + "." + method, e);
            }
        }

        private void generateBranch(RelationalExpression expr,
                                    Label label, boolean whenTrue) {
            // RelationalExpressions produce different comparision instructions
            // based on the type of the expression.

            Token operator = expr.getOperator();
            Expression left = expr.getLeftExpression();

            if (operator.getID() == Token.ISA) {
                TypeName typeName = expr.getIsaTypeName();

                generate(left);
                setLineNumber(operator.getSourceInfo());
                mBuilder.instanceOf(makeDesc(typeName.getType(), false));
                mBuilder.ifZeroComparisonBranch(label, whenTrue ? "!=" : "==");

                return;
            }

            Expression right = expr.getRightExpression();
            String choice = getChoice(operator, whenTrue);

            Type leftType = left.getType();
            Class<?> leftClass = leftType.getNaturalClass();
            
            Type rightType = right.getType();
            Class<?> rightClass = rightType.getNaturalClass();

            Class<?> clazz = 
                leftType.getCompatibleType(rightType).getNaturalClass();

            if (clazz == null) {
                throw new RuntimeException("Relational type mismatch: " +
                                           leftType + ", " + rightType);
            }

            boolean isPrimitive = 
                leftType.isPrimitive() && rightType.isPrimitive();
            
            // handle object comparisons where at least one value is an object
            // rather than both being primitives
            
            if (!isPrimitive) {
                
                // if both are a number value including both being numbers or
                // one being primitive and the other a number, then we are
                // assuming we have an unknown or general purpose number we 
                // have to deal with at runtime so compare together using the
                // runtime types for type safety

                if (Number.class.equals(clazz)) {

                    if (!leftType.isPrimitive() && !rightType.isPrimitive()) {
                        // both are objects, so do pure equality checking
                        if (choice == "==" || choice == "!=") {
                            generateEquals(left, right, label, choice, operator);
                        }
                        
                        // convert both to Numbers and runtime compare for
                        // relational checking
                        else {
                            generate(left);
                            generate(right);
                            
                            Method method = getMethod(
                                WrapperTypeConversionUtil.class, "compare", 
                                Number.class, Number.class
                            );
                            
                            mBuilder.invoke(method);
                            mBuilder.ifZeroComparisonBranch(label, choice);
                        }
                    }
                    
                    // handle cases where one value is primitive and the other
                    // is some unknown numeric object and compare at runtime
                    else {
                        leftClass = Number.class;
                        if (leftType.isPrimitive()) {
                            leftClass = leftType.getNaturalClass();
                        }
                        
                        rightClass = Number.class;
                        if (rightType.isPrimitive()) {
                            rightClass = rightType.getNaturalClass();
                        }
                    
                        Method method = getMethod(
                            WrapperTypeConversionUtil.class, "compare", 
                            leftClass, rightClass
                        );

                        generate(left);
                        generate(right);
                        mBuilder.invoke(method);
                        mBuilder.ifZeroComparisonBranch(label, choice);
                    }
                }
                
                // non-numbers, so assume both are objects and do equality check
                else if (choice == "==" || choice == "!=") {
                    generateEquals(left, right, label, choice, operator);
                }
                
                // use comparable comparisons for relational checks
                else if (Comparable.class.isAssignableFrom(clazz) &&
                         (leftClass.isAssignableFrom(rightClass) ||
                          rightClass.isAssignableFrom(leftClass))) {

                    // check whether we need to compare left to right when
                    // left is the parent or right to left when right is the
                    // parent...in the latter case we also negate it
                    
                    boolean leftToRight = 
                        leftClass.isAssignableFrom(rightClass);
                    
                    if (leftToRight) {
                        generate(left);
                        generate(right);
                    }
                    else {
                        generate(right);
                        generate(left);
                    }
                    
                    setLineNumber(operator.getSourceInfo());

                    Method method = 
                        getMethod(Comparable.class, "compareTo", Object.class);
                    mBuilder.invoke(method);
                    
                    if (!leftToRight) {
                        mBuilder.math(Opcode.INEG);
                    }
                    
                    mBuilder.ifZeroComparisonBranch(label, choice);
                }
                else if (Number.class.isAssignableFrom(clazz)) {
                    // numbers must be converted down to primitives to be
                    // compared...this is potentially dangerous as we are
                    // converting types
                    warn("compare.as.double", expr);
                    
                    Method doubleMethod = 
                        getMethod(Number.class, "doubleValue");
                    
                    generate(left);
                    mBuilder.invoke(doubleMethod);
                    generate(right);
                    mBuilder.invoke(doubleMethod);
                    setLineNumber(operator.getSourceInfo());
                    
                    byte op;
                    int ID = operator.getID();
                    if (ID == Token.LT || ID == Token.LE ||
                        ID == Token.EQ) {
                        op = Opcode.DCMPG;
                    }
                    else {
                        op = Opcode.DCMPL;
                    }
                    
                    mBuilder.math(op);
                    mBuilder.ifZeroComparisonBranch(label, choice);
                }
                else {
                    throw new RuntimeException("Can't do " + choice +
                                               " for type " + leftType);
                }
            }
            
            // otherwise, handle purely primitive comparisons
            else {
                if (clazz == int.class) {
                    if (right.isValueKnown() && right.getValue() != null) {
                        int value = ((Integer)right.getValue()).intValue();
                        if (value == 0) {
                            generate(left);
                            mBuilder.ifZeroComparisonBranch(label, choice);
                            return;
                        }
                    }
                    else if (left.isValueKnown() && left.getValue() != null) {
                        int value = ((Integer)left.getValue()).intValue();
                        if (value == 0) {
                            generate(right);
                            choice = getChoice(operator, !whenTrue);
                            mBuilder.ifZeroComparisonBranch(label, choice);
                            return;
                        }
                    }

                    generate(left);
                    generate(right);
                    mBuilder.ifComparisonBranch(label, choice);
                }
                else if (clazz == boolean.class) {
                    // An optimizer should be able to detect and reduce this
                    // relational expression if the left or right side
                    // is constant.

                    generate(left);
                    generate(right);
                    mBuilder.ifComparisonBranch(label, choice);
                }
                else {
                    generate(left);
                    generate(right);

                    byte op;

                    if (clazz == long.class) {
                        op = Opcode.LCMP;
                    }
                    else if (clazz == float.class) {
                        int ID = operator.getID();
                        if (ID == Token.LT || ID == Token.LE ||
                            ID == Token.EQ) {
                            op = Opcode.FCMPG;
                        }
                        else {
                            op = Opcode.FCMPL;
                        }
                    }
                    else if (clazz == double.class) {
                        int ID = operator.getID();
                        if (ID == Token.LT || ID == Token.LE ||
                            ID == Token.EQ) {
                            op = Opcode.DCMPG;
                        }
                        else {
                            op = Opcode.DCMPL;
                        }
                    }
                    else {
                        throw new RuntimeException("Unsupported comparison " +
                                                   "type: " + leftType);
                    }

                    mBuilder.math(op);
                    mBuilder.ifZeroComparisonBranch(label, choice);
                }
            }
        }
        
        private void generateEquals(Expression left, Expression right,
                                    Label label, String choice, 
                                    Token operator) {
            
            if (right.isValueKnown() && right.getValue() == null) {
                generate(left);
                mBuilder.ifNullBranch(label, choice == "==");
            }
            else if (left.isValueKnown() && left.getValue() == null) {
                generate(right);
                mBuilder.ifNullBranch(label, choice == "==");
            }
            else {
                Method method =  
                    getMethod(Object.class ,"equals", Object.class);

                Type leftType = left.getType();
                Type rightType = right.getType();
                
                if (leftType.isNonNull() || right.isValueKnown()) {
                    if (leftType.isNonNull()) {
                        generate(left);
                        generate(right);
                    }
                    else {
                        // Reversing the order of generation is
                        // safe because the right expression is
                        // constant.
                        generate(right);
                        generate(left);
                    }
                    setLineNumber(operator.getSourceInfo());
                    mBuilder.invoke(method);
                    mBuilder.ifZeroComparisonBranch
                        (label, (choice == "==") ? "!=" : "==");
                }
                else {
                    // This is a little bit complicated. The results
                    // of the left expression are tested against null
                    // before the equals method is invoked on it.

                    generate(left);
                    mBuilder.dup();
                    Label leftNotNull = mBuilder.createLabel();
                    mBuilder.ifNullBranch(leftNotNull, false);
                    mBuilder.pop(); // discard left expression result

                    Label fallThrough = mBuilder.createLabel();

                    if (rightType.isNonNull()) {
                        if (choice == "==") {
                            mBuilder.branch(fallThrough);
                        }
                        else {
                            mBuilder.branch(label);
                        }
                    }
                    else {
                        generate(right);
                        mBuilder.ifNullBranch(label, choice == "==");
                        mBuilder.branch(fallThrough);
                    }

                    leftNotNull.setLocation();
                    generate(right);
                    setLineNumber(operator.getSourceInfo());
                    mBuilder.invoke(method);
                    mBuilder.ifZeroComparisonBranch
                        (label, (choice == "==") ? "!=" : "==");

                    fallThrough.setLocation();
                }
            }
        }

        // "not", "and" and "or" have short circuit semantics.

        private void generateBranch(NotExpression expr,
                                    Label label, boolean whenTrue) {
            // If someone is branching based on the result of this not
            // expression, we must invert the branch condition.
            generateBranch(expr.getExpression(), label, whenTrue, true);
        }

        private void generateBranch(AndExpression expr,
                                    Label label, boolean whenTrue) {
            if (whenTrue) {
                Label falseLabel = mBuilder.createLabel();
                generateBranch(expr.getLeftExpression(), falseLabel, false);
                generateBranch(expr.getRightExpression(), label, true);
                falseLabel.setLocation();
            }
            else {
                generateBranch(expr.getLeftExpression(), label, false);
                generateBranch(expr.getRightExpression(), label, false);
            }
        }

        private void generateBranch(OrExpression expr,
                                    Label label, boolean whenTrue) {
            if (whenTrue) {
                generateBranch(expr.getLeftExpression(), label, true);
                generateBranch(expr.getRightExpression(), label, true);
            }
            else {
                Label trueLabel = mBuilder.createLabel();
                generateBranch(expr.getLeftExpression(), trueLabel, true);
                generateBranch(expr.getRightExpression(), label, false);
                trueLabel.setLocation();
            }
        }

        private String getChoice(Token operator, boolean whenTrue) {
            if (whenTrue) {
                switch (operator.getID()) {
                case Token.EQ:
                    return "==";
                case Token.NE:
                    return "!=";
                case Token.LT:
                    return "<";
                case Token.GT:
                    return ">";
                case Token.LE:
                    return "<=";
                case Token.GE:
                    return ">=";
                }
            }
            else {
                switch (operator.getID()) {
                case Token.EQ:
                    return "!=";
                case Token.NE:
                    return "==";
                case Token.LT:
                    return ">=";
                case Token.GT:
                    return "<=";
                case Token.LE:
                    return ">";
                case Token.GE:
                    return "<";
                }
            }

            throw new RuntimeException("Unknown relational operator: " +
                                       operator.getImage());
        }

        /**
         * Is called by visit(ConcatenateExpression) and generates code
         * that performs string concatenation in a manner similar to the way a
         * Java program does it. The generated code tries to do a better job
         * of estimating the buffer size requirements.
         *
         * <p>The expression "a" & 4 & "c" gets translated (loosly) into
         * new StringBuffer("a").append(4).append("c").toString().
         * The expression x & y, where x could be null, gets translated into
         * new StringBuffer().append(x).append(y).toString().
         */
        private void generateAppend(Expression expr, int estimate) {
            if (!(expr instanceof ConcatenateExpression)) {
                generate(expr);
                return;
            }

            ConcatenateExpression concat = (ConcatenateExpression)expr;
            Expression left = concat.getLeftExpression();
            Expression right = concat.getRightExpression();

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (left instanceof ConcatenateExpression) {
                generateAppend(left, estimate);
            }
            else {
                // Construct the StringBuffer, but be smart with respect to
                // its initial capacity.
                mBuilder.newObject(cStringBuilderDesc);
                mBuilder.dup();

                if (left.isValueKnown()) {
                    // If the value of left is known, don't adjust estimate
                    // at runtime. Construct the StringBuffer immediately.
                    mBuilder.loadConstant(estimate);
                    mBuilder.invokeConstructor
                        ("java.lang.StringBuilder", cIntParam);

                    if (left instanceof StringLiteral) {
                        String val = (String)((StringLiteral)left).getValue();
                        if (val.length() == 1) {
                            mBuilder.loadConstant(val.charAt(0));
                            leftType = new Type(char.class);
                        }
                        else {
                            generate(left);
                        }
                    }
                    else {
                        generate(left);
                    }
                }
                else {
                    // Increase the size of the estimate at runtime, based on
                    // the length of the left result.

                    // Subtract the length estimate of the unknown left result.
                    if ((estimate -= LENGTH_ESTIMATE) < 0) {
                        estimate += LENGTH_ESTIMATE;
                    }

                    mBuilder.loadConstant(estimate);

                    LocalVariable leftResult =
                        mBuilder.createLocalVariable("left", TypeDesc.STRING);
                    generate(left);
                    mBuilder.storeLocal(leftResult);

                    Label ctor = mBuilder.createLabel();

                    if (leftType.isNullable()) {
                        // If left could be null, test for it. If it is null,
                        // don't adjust estimate.
                        mBuilder.loadLocal(leftResult);
                        mBuilder.ifNullBranch(ctor, true);
                    }

                    mBuilder.loadLocal(leftResult);
                    mBuilder.invokeVirtual("java.lang.String", "length",
                                           TypeDesc.INT);
                    mBuilder.math(Opcode.IADD);

                    ctor.setLocation();
                    mBuilder.invokeConstructor
                        ("java.lang.StringBuilder",cIntParam);

                    mBuilder.loadLocal(leftResult);
                }

                // Constructed StringBuffer and first append argument is on
                // the stack, so perform first append operation.
                append(leftType);
            }

            if (right instanceof StringLiteral) {
                String val = (String)((StringLiteral)right).getValue();
                if (val.length() == 1) {
                    mBuilder.loadConstant(val.charAt(0));
                    rightType = new Type(char.class);
                }
                else {
                    generate(right);
                }
            }
            else {
                generate(right);
            }

            setLineNumber(concat.getOperator().getSourceInfo());

            append(rightType);
        }

        private void append(Type type) {
            Class<?> clazz = type.getNaturalClass();

            TypeDesc[] param;
            if (type.isPrimitive()) {
                if (clazz == byte.class || clazz == short.class) {
                    clazz = int.class;
                }
                param = new TypeDesc[] {makeDesc(clazz)};
            }
            else if (clazz == String.class) {
                param = cStringParam;
            }
            else {
                param = cObjectParam;
            }

            mBuilder.invokeVirtual("java.lang.StringBuilder", "append",
                                   cStringBuilderDesc, param);
        }

        private int estimateBufferRequirement(ConcatenateExpression concat) {
            return estimateBufferRequirement(concat.getLeftExpression()) +
                estimateBufferRequirement(concat.getRightExpression());
        }

        private int estimateBufferRequirement(Expression expr) {
            Object value;

            if (expr.isValueKnown() &&
                (value = expr.getValue()) instanceof String) {

                return ((String)value).length();
            }
            else if (expr instanceof ConcatenateExpression) {
                return estimateBufferRequirement((ConcatenateExpression)expr);
            }
            else {
                return LENGTH_ESTIMATE;
            }
        }

        // Type conversion only applies to expressions.

        private void typeConvertBegin(Expression.Conversion conversion) {
            Type from = conversion.getFromType();
            if (from == null) {
                return;
            }
            Type to = conversion.getToType();
            typeConvertBegin(from, to, conversion.isCastPreferred());
        }

        private void typeConvertBegin(final Type from, final Type to,
                                      boolean castPreferred) {
            Class<?> fromNat = from.getNaturalClass();
            Class<?> toNat = to.getNaturalClass();

            if (fromNat.isArray() && toNat.isArray()) {
                // Nothing to do at beginning of conversion.
                return;
            }

            if (from.isPrimitive()) {
                if (to.isPrimitive()) {
                    // Nothing to do at beginning of conversion.
                    return;
                }
                else {
                    // Assume using object peer for primitive type.
                    Class<?> fromObj = from.getObjectClass();
                    Class<?> toObj = to.getObjectClass();

                    if (fromObj == toObj) {
                        if (fromObj != Boolean.class &&
                            fromObj != Integer.class)
                        {
                            mBuilder.newObject(makeDesc(from, false));
                            mBuilder.dup();
                        }
                        return;
                    }
                }
            }
            else {
                if (to.isPrimitive()) {
                    // Assume using primitive peer for object.
                    if (from.hasPrimitivePeer() ||
                        (Number.class.isAssignableFrom(to.getObjectClass()) &&
                         Number.class.isAssignableFrom(from.getObjectClass())))
                    {
                        // Nothing to do at beginning.
                        return;
                    }
                }
                else {
                    if (Number.class.isAssignableFrom(fromNat) &&
                        Number.class.isAssignableFrom(toNat)) {

                        // Nothing to do at beginning.
                        return;
                    }
                }
            }

            boolean convertStringToNonNull =
                from.isNullable() && to.isNonNull() &&
                String.class.isAssignableFrom(fromNat) &&
                String.class.isAssignableFrom(toNat);

            if (!convertStringToNonNull && toNat.isAssignableFrom(fromNat)) {
                // Do nothing at all for upcast.
                return;
            }

            boolean canCast = fromNat.isAssignableFrom(toNat);
            boolean canConvertToString = toNat.isAssignableFrom(String.class);

            if (canConvertToString && (!canCast || !castPreferred)) {
                // String conversion.
                if (String.class.isAssignableFrom(fromNat)) {
                    // Converting from a String to a non-null String.
                    // Do nothing at the beginning.
                }
                else if (to.isNullable() &&
                         (from.isNullable() ||
                          fromNat.isAssignableFrom(String.class))) {
                    // Converting to a nullable String from a nullable object
                    // or an object that might already be a string, so perform
                    // special logic. Do nothing at the beginning.
                }
                else {
                    Method converter = stringConversionMethod(from);
                    if (!Modifier.isStatic(converter.getModifiers())) {
                        // Push instance of Context onto stack.
                        generateContext();
                    }
                }
                return;
            }
            else if (canCast) {
                // Nothing to do at beginning for downcast.
                return;
            }

            typeError(from, to);
        }

        private void typeConvertEnd(Expression.Conversion conversion) {
            Type from = conversion.getFromType();
            if (from == null) {
                return;
            }
            Type to = conversion.getToType();
            typeConvertEnd(from, to, conversion.isCastPreferred());
        }

        private void typeConvertEnd(final Type from, final Type to,
                                    boolean castPreferred) {
            Class<?> fromNat = from.getNaturalClass();
            Class<?> toNat = to.getNaturalClass();

            if (fromNat.isArray() && toNat.isArray()) {
                if (fromNat != toNat) {
                    convertArray(from, to);
                }
                return;
            }

            if (from.isPrimitive()) {
                if (to.isPrimitive()) {
                    mBuilder.convert(makeDesc(from), makeDesc(to));
                    return;
                }
                else {
                    // Assume using object peer for primitive type.
                    Class<?> fromObj = from.getObjectClass();
                    Class<?> toObj = to.getObjectClass();

                    if (fromObj == toObj) {
                        if (fromObj == Boolean.class) {
                            TypeDesc td = makeDesc(Boolean.class);

                            Label falseLabel = mBuilder.createLabel();
                            Label endLabel = mBuilder.createLabel();
                            mBuilder.ifZeroComparisonBranch(falseLabel, "==");
                            mBuilder.loadStaticField("java.lang.Boolean",
                                                     "TRUE", td);
                            mBuilder.branch(endLabel);
                            falseLabel.setLocation();
                            mBuilder.loadStaticField("java.lang.Boolean",
                                                     "FALSE", td);
                            endLabel.setLocation();
                        }
                        else if (fromObj == Integer.class) {
                            mBuilder.invokeStatic("org.teatrove.trove.util.IntegerFactory",
                                                  "toInteger",
                                                  makeDesc(Integer.class),
                                                  cIntParam);
                        }
                        else {
                            TypeDesc[] param = new TypeDesc[1];
                            param[0] = makeDesc(from);
                            mBuilder.invokeConstructor(toObj.getName(), param);
                        }
                        return;
                    }
                }
            }
            else {
                if (to.isPrimitive()) {
                    // Assume using primitive peer for object.
                    if (Number.class.isAssignableFrom(from.getObjectClass()) &&
                        Number.class.isAssignableFrom(to.getObjectClass())) {

                        String methodName = null;
                        if (toNat == int.class) {
                            methodName = "intValue";
                        }
                        else if (toNat == float.class) {
                            methodName = "floatValue";
                        }
                        else if (toNat == long.class) {
                            methodName = "longValue";
                        }
                        else if (toNat == double.class) {
                            methodName = "doubleValue";
                        }
                        else if (toNat == byte.class) {
                            methodName = "byteValue";
                        }
                        else if (toNat == short.class) {
                            methodName = "shortValue";
                        }

                        if (methodName != null) {
                            mBuilder.invokeVirtual("java.lang.Number",
                                                   methodName,
                                                   makeDesc(toNat));
                            return;
                        }
                    }
                    else if (from.getObjectClass() == Boolean.class &&
                             toNat == boolean.class) {

                        mBuilder.invokeVirtual("java.lang.Boolean",
                                               "booleanValue",
                                               makeDesc(toNat));
                        return;
                    }
                    else if (from.getObjectClass() == Character.class &&
                             toNat == char.class) {

                        mBuilder.invokeVirtual("java.lang.Character",
                                               "charValue",
                                               makeDesc(toNat));
                        return;
                    }
                }
                else {
                    if (Number.class.isAssignableFrom(fromNat) &&
                        Number.class.isAssignableFrom(toNat)) {

                        if (fromNat == toNat) {
                            return;
                        }

                        mBuilder.loadStaticField(toNat.getName(), "TYPE", 
                                                 makeDesc(Class.class));

                        Method method = getMethod(
                            WrapperTypeConversionUtil.class, "convert", 
                            Number.class, Class.class
                        );

                        mBuilder.invoke(method);
                        mBuilder.checkCast(makeDesc(toNat));
                        return;

                    }
                }
            }

            boolean convertStringToNonNull =
                from.isNullable() && to.isNonNull() &&
                String.class.isAssignableFrom(fromNat) &&
                String.class.isAssignableFrom(toNat);

            if (!convertStringToNonNull && toNat.isAssignableFrom(fromNat)) {
                // Do nothing at all for upcast.
                return;
            }

            boolean canCast = fromNat.isAssignableFrom(toNat);
            boolean canConvertToString = toNat.isAssignableFrom(String.class);

            if (canConvertToString && (!canCast || !castPreferred)) {
                // String conversion.
                Method converter = stringConversionMethod(from);

                if (String.class.isAssignableFrom(fromNat)) {
                    // Converting from a String to a non-null String.
                    // Test against null before calling converter.

                    // TODO: if I can detect if a local variable is currently
                    // on the top of the stack, then I can use that instead of
                    // doing dup operations.

                    mBuilder.dup();
                    Label nonNullLabel = mBuilder.createLabel();
                    mBuilder.ifNullBranch(nonNullLabel, false);

                    if (!Modifier.isStatic(converter.getModifiers())) {
                        // Push instance of Context onto stack and
                        // swap it into the correct place.
                        generateContext();
                        mBuilder.swap();
                    }
                    mBuilder.invoke(converter);

                    nonNullLabel.setLocation();
                }
                else if (to.isNullable() &&
                         (from.isNullable() ||
                          fromNat.isAssignableFrom(String.class))) {
                    // Converting to a nullable String from a nullable object
                    // or an object that might already be a string, so perform
                    // special logic.

                    Label castLabel = mBuilder.createLabel();

                    // TODO: if I can detect if a local variable is currently
                    // on the top of the stack, then I can use that instead of
                    // doing dup operations.

                    if (from.isNullable()) {
                        mBuilder.dup();
                        mBuilder.ifNullBranch(castLabel, true);
                    }

                    if (fromNat.isAssignableFrom(String.class)) {
                        mBuilder.dup();
                        mBuilder.instanceOf(TypeDesc.STRING);
                        mBuilder.ifZeroComparisonBranch(castLabel, "!=");
                    }

                    if (!Modifier.isStatic(converter.getModifiers())) {
                        // Push instance of Context onto stack and
                        // swap it into the correct place.
                        generateContext();
                        mBuilder.swap();
                    }
                    mBuilder.invoke(converter);
                    Label continueLabel = mBuilder.createLabel();
                    mBuilder.branch(continueLabel);

                    castLabel.setLocation();
                    mBuilder.checkCast(TypeDesc.STRING);

                    continueLabel.setLocation();
                }
                else {
                    mBuilder.invoke(converter);
                }
                return;
            }
            else if (canCast) {
                if (from != Type.NULL_TYPE) {
                    mBuilder.checkCast(makeDesc(toNat));
                }
                return;
            }

            typeError(from, to);
        }

        private Method stringConversionMethod(Type from) {
            Compiler c = mUnit.getCompiler();

            Method[] methods = c.getStringConverterMethods();
            Type[] param = new Type[] {from};

            int cnt = MethodMatcher.match(methods, null, param);

            if (cnt >= 1) {
                return methods[0];
            }
            else {
                throw new RuntimeException("Couldn't convert " + from +
                                           " to String");
            }
        }

        private void typeError(Type from, Type to) {
            throw new RuntimeException("Can't convert " + from + " to " + to);
        }

        private void convertArray(Type from, Type to) {
            // Generate code to do a runtime conversion of an array.
            // This involves creating a new array and populating it with
            // converted elements from the original array.
            //
            // <from type>[] originalArray = stackVar;
            // int length = originalArray.length;
            // push (new <to type>[length]);
            // for (length--; length >= 0; length--) {
            //     dup; // dup the new array
            //     load local (length);
            //     store to array (convert (originalArray[length]));
            // }

            Type fromElement;
            Type toElement;

            try {
                fromElement = from.getArrayElementType();
                toElement = to.getArrayElementType();
            }
            catch (IntrospectionException e) {
                throw new RuntimeException(e.toString());
            }

            TypeDesc originalType = makeDesc(from);

            LocalVariable originalArray =
                mBuilder.createLocalVariable("originalArray", originalType);

            LocalVariable length =
                mBuilder.createLocalVariable("length", TypeDesc.INT);

            mBuilder.storeLocal(originalArray);

            Label endLabel;
            if (from.isNonNull()) {
                endLabel = null;
            }
            else {
                // If source array could be null, test it first.
                mBuilder.loadLocal(originalArray);
                Label startLabel = mBuilder.createLabel();
                mBuilder.ifNullBranch(startLabel, false);
                mBuilder.loadConstant(null);
                endLabel = mBuilder.createLabel();
                mBuilder.branch(endLabel);
                startLabel.setLocation();
            }

            mBuilder.loadLocal(originalArray);
            mBuilder.arrayLength();
            mBuilder.storeLocal(length);
            mBuilder.loadLocal(length);
            mBuilder.newObject(makeDesc(to));
            Label testLabel = mBuilder.createLabel();
            mBuilder.branch(testLabel);
            Label loopLabel = mBuilder.createLabel().setLocation();
            mBuilder.dup();
            mBuilder.loadLocal(length);
            typeConvertBegin(fromElement, toElement, false);
            mBuilder.loadLocal(originalArray);
            mBuilder.loadLocal(length);
            mBuilder.loadFromArray(makeDesc(fromElement));
            typeConvertEnd(fromElement, toElement, false);
            mBuilder.storeToArray(makeDesc(toElement));
            testLabel.setLocation();
            mBuilder.integerIncrement(length, -1);
            mBuilder.loadLocal(length);
            mBuilder.ifZeroComparisonBranch(loopLabel, ">=");

            if (endLabel != null) {
                endLabel.setLocation();
            }
        }

        private void saveContinueLabel(Label startLabel) {
            Object top = mBreakInfoStack.get(mBreakInfoStack.size() - 1);
            if (! (top instanceof BreakAndContinueLabels))
                throw new IllegalArgumentException("Internal compiler error: Break stack - unexpected object found.");
            ((BreakAndContinueLabels) top).setContinueLabel(startLabel);
        }

        private void generateForeachArray(ForeachStatement node) {
            Expression range = node.getRange();
            Statement init = node.getInitializer();
            Statement body = node.getBody();

            // Holds the loop index value.
            final LocalVariable indexLocal =
                mBuilder.createLocalVariable(null, TypeDesc.INT);

            TypeDesc rangeDesc = makeDesc(range.getType(), false);
            // Holds the array to extract from.
            final LocalVariable rangeLocal =
                mBuilder.createLocalVariable(null, rangeDesc);
            generate(range);
            mBuilder.storeLocal(rangeLocal);

            // Generate init right after the range is evaluated.
            if (init != null) {
                generate(init);
            }

            Label endLabel = mBuilder.createLabel();

            if (range.getType().isNullable()) {
                // If range is null, just skip past the loop, avoiding a
                // NullPointerException.
                mBuilder.loadLocal(rangeLocal);
                mBuilder.ifNullBranch(endLabel, true);
            }

            mBuilder.loadLocal(rangeLocal);

            // Put the end index value onto the stack.
            mBuilder.arrayLength();

            // Holds the value to compare against for when the index has
            // reached the end and looping should stop.
            LocalVariable endIndexLocal = null;

            if (!node.isReverse()) {
                endIndexLocal =
                    mBuilder.createLocalVariable(null, TypeDesc.INT);
                mBuilder.storeLocal(endIndexLocal);
                mBuilder.loadConstant(0);
                mBuilder.storeLocal(indexLocal);
            }
            else {
                // endIndexLocal is not needed because its value is zero.
                mBuilder.storeLocal(indexLocal);
            }

            Label checkLabel = mBuilder.createLabel();
            mBuilder.branch(checkLabel);

            // Loop body begins here.
            Label startLabel = mBuilder.createLabel().setLocation();
            Label continueLabel = mBuilder.createLabel();
            saveContinueLabel(continueLabel);


            // Feed the loop variable with a value.
            final VariableRef loopVarRef = node.getLoopVariable();
            final Class<?> loopVarClass = loopVarRef.getType().getNaturalClass();
            final Class<?> rangeComponentClass = range.getType().getObjectClass().getComponentType();

            storeToVariable(loopVarRef.getVariable(), new Runnable() {
                public void run() {
                    mBuilder.loadLocal(rangeLocal);
                    mBuilder.loadLocal(indexLocal);
                    mBuilder.loadFromArray(makeDesc(loopVarRef.getType()));
                    if (loopVarClass != rangeComponentClass)
                        mBuilder.checkCast(makeDesc(loopVarRef.getType()));
                }
            });

            if (body != null) {
                generate(body);
            }

            // set entry point for continue
            continueLabel.setLocation();

            if (!node.isReverse()) {
                // Build check label location and index adjustment
                mBuilder.integerIncrement(indexLocal, 1);
                checkLabel.setLocation();

                mBuilder.loadLocal(indexLocal);

                mBuilder.loadLocal(endIndexLocal);
                mBuilder.ifComparisonBranch(startLabel, "<");
            }
            else {
                // Build check label location and index adjustment
                checkLabel.setLocation();
                mBuilder.integerIncrement(indexLocal, -1);

                mBuilder.loadLocal(indexLocal);

                mBuilder.ifZeroComparisonBranch(startLabel, ">=");
            }

            endLabel.setLocation();
        }

        private void generateForeachIterator(final ForeachStatement node) {
            Expression range = node.getRange();
            Statement init = node.getInitializer();
            Statement body = node.getBody();

            // Allow Maps and Sets to be reverse iterable - Wrap it in an ArrayList
            // This is a stack optimization to avoid creating a local variable
            if (! "java.util.List".equals(range.getType().getNaturalClass().getName()) &&
                    node.isReverse()) {
                mBuilder.newObject(makeDesc(java.util.ArrayList.class));
                mBuilder.dup();
            }

            // Put the range onto the stack
            generate(range);

            // Convert the Map to a Set
            if ("java.util.Map".equals(range.getType().getNaturalClass().getName()))
                mBuilder.invokeInterface("java.util.Map", "keySet",
                    makeDesc(java.util.Set.class));


            // Allow Maps and Sets to be reverse iterable - invoke constructor on ArrayList.
            // The overall cost for this is only incurred for Sets/Maps
            if (! "java.util.List".equals(range.getType().getNaturalClass().getName()) &&
                    node.isReverse()) {
                mBuilder.invokeConstructor("java.util.ArrayList",
                    new TypeDesc[] { makeDesc(java.util.Collection.class) });
            }

            // Generate init right after the range is evaluated.
            if (init != null) {
                generate(init);
            }

            Label endLabel = mBuilder.createLabel();
            Label notNullLabel = mBuilder.createLabel();

            if (range.getType().isNullable()) {
                // If range is null, just skip past the loop, avoiding a
                // NullPointerException.
                mBuilder.dup();
                mBuilder.ifNullBranch(notNullLabel, false);
                mBuilder.pop();
                mBuilder.branch(endLabel);
            }

            notNullLabel.setLocation();

            // TODO: if array, this fails to iterate
            
            TypeDesc td;
            if (!node.isReverse()) {
                td = makeDesc(Iterator.class);
                mBuilder.invokeInterface
                    ("java.lang.Iterable", "iterator", td);
            }
            else {
                mBuilder.dup();
                mBuilder.invokeInterface
                    ("java.util.Collection", "size", TypeDesc.INT);
                td = makeDesc(ListIterator.class);
                mBuilder.invokeInterface
                    ("java.util.List", "listIterator", td, cIntParam);
            }

            final LocalVariable iteratorLocal =
                mBuilder.createLocalVariable(null, td);
            mBuilder.storeLocal(iteratorLocal);

            Label checkLabel = mBuilder.createLabel();
            mBuilder.branch(checkLabel);

            // Loop body begins here.
            Label startLabel = mBuilder.createLabel().setLocation();
            Label continueLabel = mBuilder.createLabel();
            saveContinueLabel(continueLabel);

            // Feed the loop variable with a value.
            final VariableRef loopVarRef = node.getLoopVariable();
            final Class<?> loopVarClass = loopVarRef.getType().getNaturalClass();

            storeToVariable(loopVarRef.getVariable(), new Runnable() {
                public void run() {
                    mBuilder.loadLocal(iteratorLocal);
                    if (!node.isReverse()) {
                        mBuilder.invokeInterface("java.util.Iterator", "next",
                                                 TypeDesc.OBJECT);
                    }
                    else {
                        mBuilder.invokeInterface("java.util.ListIterator",
                                                 "previous",
                                                 TypeDesc.OBJECT);
                    }
                    if (loopVarClass != Object.class) {
                        mBuilder.checkCast(makeDesc(loopVarRef.getType()));
                    }
                }
            });

            if (body != null) {
                generate(body);
            }

            // set entry point for continue
            continueLabel.setLocation();

            checkLabel.setLocation();
            mBuilder.loadLocal(iteratorLocal);

            td = TypeDesc.BOOLEAN;

            if (!node.isReverse()) {
                mBuilder.invokeInterface("java.util.Iterator",
                                         "hasNext", td);
            }
            else {
                mBuilder.invokeInterface("java.util.ListIterator",
                                         "hasPrevious", td);
            }

            mBuilder.ifZeroComparisonBranch(startLabel, "!=");

            endLabel.setLocation();
        }

        private void generateForeachRange(ForeachStatement node) {
            Expression range = node.getRange();
            Expression endRange = node.getEndRange();
            Statement init = node.getInitializer();
            Statement body = node.getBody();

            // Holds the value to compare against for when the index has
            // reached the end and looping should stop. Isn't used if the
            // end index has a known value.
            LocalVariable endIndexLocal = null;
            // Only valid if endIndexLocal is null.
            long endIndexValue = 0;

            // Initialize the index and end index local variables.

            final Expression indexExpr;
            Expression endIndexExpr;

            if (!node.isReverse()) {
                indexExpr = range;
                endIndexExpr = endRange;
            }
            else {
                indexExpr = endRange;
                endIndexExpr = range;
            }

            // Feed the loop variable with a value.
            VariableRef loopVarRef = node.getLoopVariable();
            Variable loopVar = loopVarRef.getVariable();
            storeToVariable(loopVar, new Runnable() {
                public void run() {
                    generate(indexExpr);
                }
            });

            boolean longRange = Type.LONG_TYPE.equals(loopVar.getType());

            if (endIndexExpr.isValueKnown()) {
                // End index is known, so don't use local variable to hold it.
                endIndexValue = ((Number)endIndexExpr.getValue()).longValue();
            }
            else {
                generate(endIndexExpr);
                if (longRange) {
                    endIndexLocal = mBuilder.createLocalVariable
                        (null, TypeDesc.LONG);
                }
                else {
                    endIndexLocal = mBuilder.createLocalVariable
                        (null, TypeDesc.INT);
                }
                mBuilder.storeLocal(endIndexLocal);
            }

            // Generate init right before the loop entry point.
            if (init != null) {
                generate(init);
            }

            Label checkLabel = mBuilder.createLabel();
            mBuilder.branch(checkLabel);

            // Loop body begins here.
            Label startLabel = mBuilder.createLabel().setLocation();
            Label continueLabel = mBuilder.createLabel();
            saveContinueLabel(continueLabel);

            if (body != null) {
                generate(body);
            }

            // entry point for continue
            continueLabel.setLocation();


            // Build index adjustment and determine choice comparison.
            String choice;
            if (!node.isReverse()) {
                incrementIntVariable(loopVar, 1);
                choice = "<=";
            }
            else {
                incrementIntVariable(loopVar, -1);
                choice = ">=";
            }

            // Build check.
            checkLabel.setLocation();
            loadFromVariable(loopVar);

            if (endIndexLocal != null) {
                mBuilder.loadLocal(endIndexLocal);
                if (longRange) {
                    mBuilder.math(Opcode.LCMP);
                    mBuilder.ifZeroComparisonBranch(startLabel, choice);
                }
                else {
                    mBuilder.ifComparisonBranch(startLabel, choice);
                }
            }
            else if (longRange) {
                mBuilder.loadConstant(endIndexValue);
                mBuilder.math(Opcode.LCMP);
                mBuilder.ifZeroComparisonBranch(startLabel, choice);
            }
            else if (endIndexValue != 0) {
                mBuilder.loadConstant((int)endIndexValue);
                mBuilder.ifComparisonBranch(startLabel, choice);
            }
            else {
                mBuilder.ifZeroComparisonBranch(startLabel, choice);
            }
        }

        private void generateCallExpression(final CallExpression node) {
            // generate expression if applicable
            final Expression expr = node.getExpression();
            if (expr != null) {
                generate(expr);
            }
            
            // build null-safe expression if applicable
            NullSafeCallback callback = new NullSafeCallback() {
                public Type execute() {
                    Statement init = node.getInitializer();
                    Statement subParam = node.getSubstitutionParam();
                    Expression[] exprs = node.getParams().getExpressions();

                    int blockNum;
                    if (subParam != null) {
                        blockNum = mCaseNodes.size() - 1;
                        mBuilder.loadThis();
                        mBuilder.loadConstant(blockNum + 1);
                        mBuilder.storeField(mBlockId.getName(), TypeDesc.INT);
                        mCaseNodes.add(subParam);
                        if (node instanceof FunctionCallExpression) {
                            Method call = 
                                ((FunctionCallExpression) node).getCalledMethod();
                            mCallerToSubNoList.add("__block_"+ call.getName());
                        }
                        if (node instanceof TemplateCallExpression) {
                            CompilationUnit unit = 
                                ((TemplateCallExpression) node).getCalledTemplate();
                            mCallerToSubNoList.add("__block__"+ unit.getName());
                        }
                    }
                    else {
                        blockNum = 0;
                    }
        
                    // Inject pre-call profiling bytecode.
                    TypeDesc methodObserverType =
                        TypeDesc.forClass(MergedClass.InvocationEventObserver.class);
                    LocalVariable retVal = null;
                    String calleeName = null;
                    Type returnType = null;
                    TypeDesc returnTypeDesc = null;
        
                    boolean profilingEnabled = isProfilingEnabled();
                    if (profilingEnabled) {
                        if (mStartTime == null)
                            mStartTime = mBuilder.createLocalVariable("startTime",
                                TypeDesc.forClass(long.class));
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                           TypeDesc.forClass(long.class));
                        mBuilder.storeLocal(mStartTime);
                    }
        
                    if (node instanceof FunctionCallExpression) {
                        FunctionCallExpression function = 
                            (FunctionCallExpression) node;
                        Method call = function.getCalledMethod();
        
                        if (expr == null && 
                            !Modifier.isStatic(call.getModifiers())) {
                            
                            // Push instance of Context onto stack.
                            generateContext();
                        }
        
                        // check if var-arg to inject params as array
                        Class<?>[] params = call.getParameterTypes();
                        for (int i = 0; i < exprs.length; i++) {
                            if (call.isVarArgs() && i == params.length - 1) {
                                Class<?> type = 
                                    exprs[i].getType().getNaturalClass();
                                if (!params[i].isAssignableFrom(type)) {
                                    // new array, generate exprs, store to array, load array
                                    mBuilder.loadConstant(exprs.length);
                                    
                                    TypeDesc desc = makeDesc(params[i]);
                                    mBuilder.newObject(desc, 1);
                                    for (int idx = 0, j = i; j < exprs.length; j++, idx++) {
                                        mBuilder.dup();
                                        mBuilder.loadConstant(idx);
                                        generate(exprs[j]);
                                        mBuilder.storeToArray(desc.getComponentType());
                                    }
                                    
                                    break;
                                }
                            }
                            
                            generate(exprs[i]);
                        }
                        
                        if (call.isVarArgs() && exprs.length < params.length) {
                            // generate empty array for var arg
                            mBuilder.loadConstant(0);
                            mBuilder.newObject(makeDesc(params[params.length - 1]), 1);
                        }
        
                        if (subParam != null) {
                            // Put this onto the stack as a substitution parameter.
                            mBuilder.loadThis();
                        }
        
                        // Generate init right before the call.
                        if (init != null) {
                            generate(init);
                        }
        
                        if (call.getReturnType() != null) {
                            returnType = new Type
                            (
                                call.getReturnType(), 
                                call.getGenericReturnType()
                            );
                            
                            returnTypeDesc = makeDesc(returnType);
                            retVal = mBuilder.createLocalVariable("retVal", returnTypeDesc);
                        }
        
                        mBuilder.invoke(call);
        
                        calleeName = call.getName();
                    }
                    else if (node instanceof TemplateCallExpression) {
                        CompilationUnit unit =
                            ((TemplateCallExpression) node).getCalledTemplate();
        
                        // Push instance of Context onto stack as first parameter.
                        if (expr == null) {
                            generateContext();
                        }
        
                        for (int i=0; i<exprs.length; i++) {
                            generate(exprs[i]);
                        }
        
                        // Generate init right before the call.
                        if (init != null) {
                            generate(init);
                        }
        
                        String className = unit.getTargetPackage();
                        if (className == null) {
                            className = unit.getName();
                        }
                        else {
                            className = className + '.' + unit.getName();
                        }
        
                        Template tree = unit.getParseTree();
        
                        Variable[] formals = tree.getParams();
                        int length = formals.length;
        
                        TypeDesc[] params;
                        if (subParam == null) {
                            params = new TypeDesc[length + 1];
                        }
                        else {
                            params = new TypeDesc[length + 2];
                            params[params.length - 1] = makeDesc(Substitution.class);
                            // Put this onto the stack as a substitution parameter.
                            mBuilder.loadThis();
                        }
        
                        params[0] = makeDesc(unit.getRuntimeContext());
        
                        for (int i=0; i<length; i++) {
                            params[i + 1] = makeDesc(formals[i]);
                        }
        
                        if (tree.getReturnType() != null) {
                            returnType = tree.getReturnType();
                            returnTypeDesc = makeDesc(tree.getReturnType());
                            retVal = mBuilder.createLocalVariable("retVal", returnTypeDesc);
                        }
        
                        calleeName = "_" + unit.getName();
        
                        mBuilder.invokeStatic(className, EXECUTE_METHOD_NAME, returnTypeDesc, params);
                    }

                    if (returnTypeDesc != null && ! TypeDesc.VOID.equals(returnTypeDesc))
                        mBuilder.storeLocal(retVal);
        
                    // Inject post-call profiling bytecode.
                    if (profilingEnabled) {
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.loadConstant(mUnit.getName());
                        mBuilder.loadConstant(calleeName);
                        mBuilder.invokeStatic(mContextParam.getVariable().getType().getObjectClass().getName(),
                            "getInvocationObserver", methodObserverType);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "currentTime",
                            TypeDesc.forClass(long.class));
                        mBuilder.loadLocal(mStartTime);
                        mBuilder.math(Opcode.LSUB);
                        mBuilder.invokeInterface(methodObserverType.getFullName(), "invokedEvent", null,
                            new TypeDesc[] { TypeDesc.forClass(String.class), TypeDesc.forClass(String.class),
                                TypeDesc.forClass(long.class) });
                    }
        
                    if (returnTypeDesc != null && ! TypeDesc.VOID.equals(returnTypeDesc)) {
                        mBuilder.loadLocal(retVal);
                    }

                    if (subParam != null) {
                        mBuilder.loadThis();
                        mBuilder.loadConstant(blockNum);
                        mBuilder.storeField(mBlockId.getName(), TypeDesc.INT);
                    }
                    
                    return returnType;
                }
            };
            
            // generate null-safe if provided
            if (expr != null) {
                generateNullSafe(node, expr, callback);
            }
            
            // otherwise, invoke callback by itself
            else {
                callback.execute();
            }
        }

        /*
         * The code generated by the logical expressions manipulate labels
         * and branches. To generate a value, they all branch to instructions
         * that push a boolean literal onto the stack.
         */
        private void generateLogical(Expression expr) {
            Label trueLabel = mBuilder.createLabel();
            Label endLabel = mBuilder.createLabel();

            generateBranch(expr, trueLabel, true);

            Type type = expr.getInitialType();
            Class<?> clazz = type.getNaturalClass();

            if (clazz == boolean.class) {
                mBuilder.loadConstant(false);
                mBuilder.branch(endLabel);
                trueLabel.setLocation();
                mBuilder.loadConstant(true);
                endLabel.setLocation();
            }
            else if (clazz.isAssignableFrom(Boolean.class)) {
                TypeDesc td = makeDesc(Boolean.class);

                mBuilder.loadStaticField("java.lang.Boolean", "FALSE", td);
                mBuilder.branch(endLabel);
                trueLabel.setLocation();
                mBuilder.loadStaticField("java.lang.Boolean", "TRUE", td);
                endLabel.setLocation();
            }
            else if (clazz.isAssignableFrom(String.class)) {
                mBuilder.loadConstant("false");
                mBuilder.branch(endLabel);
                trueLabel.setLocation();
                mBuilder.loadConstant("true");
                endLabel.setLocation();
            }
            else {
                typeError(Type.BOOLEAN_TYPE, type);
            }
        }


        private void generateNullSafe(Expression node, Expression expr,
                                      NullSafeCallback callback) {
            // TODO: optimize this by skipping all remaining checks if first
            // part of null-safe fails..currently, the way this is written it
            // performs an ifnull check for each section:
            // ie: a?.b?.c?.d will result in 4 ifnull checks even tho if the
            // first fails, all remaining will fail, so we could short circuit

            // define branch labels
            Label endLocation = null;
            Label elseLocation = null;
            
            // generate branch checks
            if (node instanceof NullSafe) {
                NullSafe nullSafe = (NullSafe) node;
                if (nullSafe.isNullSafe() && expr.getType().isNullable()) {
                    endLocation = mBuilder.createLabel();
                    elseLocation = mBuilder.createLabel();
                    
                    mBuilder.dup();
                    mBuilder.ifNullBranch(elseLocation, true);
                }
            }
            
            // generate callback
            Type rtype = callback.execute();

            // finish branch checks
            if (node instanceof NullSafe) {
                NullSafe nullSafe = (NullSafe) node;
                if (nullSafe.isNullSafe() && expr.getType().isNullable()) {
                    // if type is primitive, convert
                    //if (type != null && type.isPrimitive()) {
                    //    typeConvertEnd(type, type.toNonPrimitive(), true);
                    //}
    
                    mBuilder.branch(endLocation);
                    elseLocation.setLocation();
                    
                    Type ntype = rtype == null ? node.getType() : rtype;
                    if (ntype != null && ntype.isPrimitive()) {
                        mBuilder.pop();
                        
                        Class<?> primitive = ntype.getNaturalClass();
                        if (primitive == boolean.class) { 
                            mBuilder.loadConstant(false);
                        }
                        else if (primitive == long.class) { 
                            mBuilder.loadConstant(0L);
                        }
                        else if (primitive == float.class) {
                            mBuilder.loadConstant(0.0f);
                        }
                        else if (primitive == double.class) {
                            mBuilder.loadConstant(0.0d);
                        }
                        else {
                            mBuilder.loadConstant(0);
                        }
                    }
                    else {
                        mBuilder.checkCast(makeDesc(ntype));
                    }
                    
                    endLocation.setLocation();
                }
            }
        }
        
        private AssignmentStatement createAssignment(Expression expr) {
            return createAssignment("tmp" + mTemporary++, expr);
        }
        
        private AssignmentStatement createAssignment(String name, Expression expr) {
            // get info
            Type type = expr.getType();
            SourceInfo info = expr.getSourceInfo();
            
            // create variable
            Variable var = new Variable(info, name, type);
            generate(var);
            
            // create reference
            VariableRef ref = new VariableRef(info, name);
            ref.setVariable(var);
            
            // create assignment
            AssignmentStatement stmt = new AssignmentStatement(info, ref, expr);
            generate(stmt);
            
            // return assignment
            return stmt;
        }
        
        private void declareVariable(Variable node) {
            declareVariable(node, null);
        }

        private void declareVariable(Variable var, LocalVariable localVar) {
            String name = var.getName();

            if (name == CONTEXT_PARAM_NAME) {
                mContextParam = new VariableRef(null, name);
                mContextParam.setVariable(var);
            }
            else if (name == SUB_PARAM_NAME) {
                mSubParam = new VariableRef(null, name);
                mSubParam.setVariable(var);
            }

            if (var.isField()) {
                if (mFields.get(name) != var) {
                    // Ensure field names are unique
                    int i = 0;
                    do {
                        name = var.getName() + '$' + i++;
                    } while (mFields.get(name) != null);

                    mFields.put(name, var);
                    var.setName(name);
                }
                mVariableMap.put(var, null);
            }
            else {
                if (localVar == null) {
                    TypeDesc desc = makeDesc(var);
                    localVar = mBuilder.createLocalVariable
                        (var.getName(), desc);
                }
                mVariableMap.put(var, localVar);
            }
        }

        /**
         * Retrieves an already declared local variable. If variable is not
         * yet declared, it is declared. If variable is actually a field, it
         * is still declared, but null is returned.
         */
        private LocalVariable getLocalVariable(Variable var) {
            if (!mVariableMap.containsKey(var)) {
                declareVariable(var, null);
            }
            return mVariableMap.get(var);
        }

        private void loadFromVariable(Variable var) {
            if (var.isField()) {
                if (!mVariableMap.containsKey(var)) {
                    declareVariable(var, null);
                }

                TypeDesc td = makeDesc(var);

                if (var.isStatic()) {
                    mBuilder.loadStaticField(var.getName(), td);
                }
                else {
                    mBuilder.loadThis();
                    mBuilder.loadField(var.getName(), td);
                }
            }
            else {
                LocalVariable local = mVariableMap.get(var);
                if (local == null) {
                    throw new RuntimeException
                        ("Attempting to read from uninitialized local " +
                         "variable: " + var);
                }
                mBuilder.loadLocal(local);
            }
        }

        private void storeToVariable(Variable var, Runnable callback) {
            if (var.isField() && !var.isStatic()) {
                mBuilder.loadThis();
            }

            callback.run();

            if (var.isField()) {
                if (!mVariableMap.containsKey(var)) {
                    declareVariable(var, null);
                }

                TypeDesc td = makeDesc(var);
                if (var.isStatic()) {
                    mBuilder.storeStaticField(var.getName(), td);
                }
                else {
                    mBuilder.storeField(var.getName(), td);
                }
            }
            else {
                mBuilder.storeLocal(getLocalVariable(var));
            }
        }

        private void incrementIntVariable(final Variable var,
                                          final int amount) {
            if (amount == 0) {
                return;
            }

            final Class<?> clazz = var.getType().getNaturalClass();

            LocalVariable local = getLocalVariable(var);
            if (local != null && clazz == int.class) {
                mBuilder.integerIncrement(local, amount);
            }
            else {
                storeToVariable(var, new Runnable() {
                    public void run() {
                        loadFromVariable(var);

                        if (clazz == int.class) {
                            if (amount >= 0) {
                                mBuilder.loadConstant(amount);
                                mBuilder.math(Opcode.IADD);
                            }
                            else {
                                mBuilder.loadConstant(-amount);
                                mBuilder.math(Opcode.ISUB);
                            }
                        }
                        else if (clazz == long.class) {
                            if (amount >= 0) {
                                mBuilder.loadConstant((long)amount);
                                mBuilder.math(Opcode.LADD);
                            }
                            else {
                                mBuilder.loadConstant((long)-amount);
                                mBuilder.math(Opcode.LSUB);
                            }
                        }
                    }
                });
            }
        }

        private void setLineNumber(SourceInfo info) {
            if (info != null) {
                int line = info.getLine();
                if (line != mLastLine) {
                    mLastLine = line;
                    mBuilder.mapLineNumber(line);
                }
            }
        }
    }

    /*
     * Check context to see if profiling is enabled by checking
     * the observer mode in the context.  Generate inline profiling
     * calls to the observer if it is expecting events to be external
     * to the MergedClass.
     */
    private boolean isProfilingEnabled() {
        Class<?> mergedClass = mUnit.getRuntimeContext();
        boolean profilingEnabled = true;

        try {
            Method a = mergedClass.getDeclaredMethod("getObserverMode");
            int observerMode = ((Integer) a.invoke(null)).intValue();
            profilingEnabled = (observerMode & MergedClass.OBSERVER_ENABLED) != 0 &&
                (observerMode & MergedClass.OBSERVER_EXTERNAL) != 0;
        }
        catch (Exception ex) { profilingEnabled = false; }
        return profilingEnabled;
    }


    private static class GuardHandler {
        final Label tryStart;
        final Label tryEnd;
        final Statement replacement;

        public GuardHandler(Label tryStart, Label tryEnd,
                            Statement replacement) {
            this.tryStart = tryStart;
            this.tryEnd = tryEnd;
            this.replacement = replacement;
        }
    }

    private static class DetailException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private Exception mException;

        public DetailException(Exception e, String detail) {
            super(e.getMessage() + ' ' + detail);
            mException = e;
        }

        public String toString() {
            return mException.getClass().getName() + ": " + getMessage();
        }

        public void printStackTrace() {
            mException.printStackTrace();
        }

        public void printStackTrace(java.io.PrintStream ps) {
            mException.printStackTrace(ps);
        }

        public void printStackTrace(java.io.PrintWriter pw) {
            mException.printStackTrace(pw);
        }
    }

    private static class BreakAndContinueLabels {
        Label mBreakLabel = null;
        Label mContinueLabel = null;

        public BreakAndContinueLabels(Label breakLabel) {
            mBreakLabel = breakLabel;
        }

        public Label getBreakLabel() { return mBreakLabel; }
        public void setContinueLabel(Label continueLabel) { mContinueLabel = continueLabel; }
        public Label getContinueLabel() { return mContinueLabel; }
    }
    
    private static interface NullSafeCallback {
        Type execute();
    }
}
