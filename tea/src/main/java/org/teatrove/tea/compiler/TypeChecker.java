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

import java.util.Vector;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.Stack;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.beans.*;
import org.teatrove.tea.TypedElement;
import org.teatrove.tea.parsetree.*;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.Statement;
import org.teatrove.tea.util.BeanAnalyzer;

/**
 * A TypeChecker operates on a template's parse tree, created by a
 * {@link Parser}, filling in type information while it checks the validity of
 * the whole template. After a template has been type-checked and there are no
 * errors, the template is ready for code generation. Add an
 * {@link ErrorListener} to capture any semantic errors detected by the
 * TypeChecker.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 119 <!-- $-->, <!--$$JustDate:-->  8/16/04 <!-- $-->
 */
public class TypeChecker {
    private CompilationUnit mUnit;
    private String[] mImports;
    private boolean mHasImports = false;

    private Vector mListeners = new Vector(1);
    private int mErrorCount = 0;

    private ClassLoader mClassLoader;
    private boolean mExceptionGuardian;

    private MessageFormatter mFormatter;
    private int mForeachCount;

    public TypeChecker(CompilationUnit unit) {
        mUnit = unit;
        mImports = mUnit.getImportedPackages();
        mFormatter = MessageFormatter.lookup(this);     
    }

    public void addErrorListener(ErrorListener listener) {
        mListeners.addElement(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        mListeners.removeElement(listener);
    }

    private void dispatchParseError(ErrorEvent e) {
        mErrorCount++;

        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                ((ErrorListener)mListeners.elementAt(i)).compileError(e);
            }
        }
    }

    private void error(String str, Node culprit) {
        str = mFormatter.format(str);
        dispatchParseError(new ErrorEvent(this, str, 
                                          culprit.getSourceInfo(), mUnit));
    }

    private void error(String str, String arg, Node culprit) {
        str = mFormatter.format(str, arg);
        dispatchParseError(new ErrorEvent(this, str, 
                                          culprit.getSourceInfo(), mUnit));
    }

    private void error(String str, String arg1, String arg2, Node culprit) {
        str = mFormatter.format(str, arg1, arg2);
        dispatchParseError(new ErrorEvent(this, str, 
                                          culprit.getSourceInfo(), mUnit));
    }

    private void error(String str, String arg1, String arg2, String arg3,
                       Node culprit) {
        str = mFormatter.format(str, arg1, arg2, arg3);
        dispatchParseError(new ErrorEvent(this, str, 
                                          culprit.getSourceInfo(), mUnit));
    }

    private void error(String str, String arg, Token culprit) {
        str = mFormatter.format(str, arg);
        dispatchParseError(new ErrorEvent(this, str, 
                                          culprit.getSourceInfo(), mUnit));
    }

    private void error(String str, String arg, SourceInfo info) {
        str = mFormatter.format(str, arg);
        dispatchParseError(new ErrorEvent(this, str, info, mUnit));
    }

    private void error(String str, String arg1, String arg2, SourceInfo info) {
        str = mFormatter.format(str, arg1, arg2);
        dispatchParseError(new ErrorEvent(this, str, info, mUnit));
    }

    /**
     * Sets the ClassLoader to use to load classes with. If set to null,
     * then classes are loaded using Class.forName.
     */
    public void setClassLoader(ClassLoader loader) {
        mClassLoader = loader;
    }

    /**
     * Enabling Exception Guardian causes statements that might throw a
     * exception to be guarded. By default, this feature is off. If a guarded
     * statement throws a RuntimeException, it is caught and handed off to
     * {@link ThreadGroup.uncaughtException}. Execution then proceeds to the
     * next statement. If the guarded statement is an assignment, then the
     * variable is assigned null if an exception is caught.
     */
    public void setExceptionGuardianEnabled(boolean enabled) {
        mExceptionGuardian = enabled;
    }

    private Class loadClass(String name) throws ClassNotFoundException {
        while (true) {
            try {
                if (mClassLoader == null) {
                    return Class.forName(name);
                }
                else {
                    return mClassLoader.loadClass(name);
                }
            }
            catch (ClassNotFoundException e) {
                int index = name.lastIndexOf('.');
                if (index < 0) {
                    throw e;
                }

                // Search for inner class.
                name = name.substring(0, index) + '$' +
                    name.substring(index + 1);
            }
        }
    }

    public void typeCheck() {
        Template template = mUnit.getParseTree();
        Name name = template.getName();

        if (name != null) {
            if ( !(mUnit.getShortName().equals(name.getName())) ) {
                error("template.name", mUnit.getShortName(), name);
            }
        }

        // Convert some ExpressionStatements into ReturnStatements.
        template.accept(new ReturnConvertor());

        // Reduce the amount of concatenation operations.
        // It is important that this step be performed after using the
        // ReturnConvertor and before the main type checking. Otherwise, a
        // return may only capture a partial result or an expression may be
        // set to a type which is only appropriate for concatenation.
        //
        // Note: Decreasing the amount of concatenations can have a slight
        // impact on the language semantics depending on how the context is
        // defined. Generally, reducing the amount of concatenations improves
        // performance. If reducing the amount of concatenations hinders
        // performance, then the data buffering and/or output strategy in the
        // context may need to be improved.
        //
        template.accept(new ConcatenationReducer());

        // Increase the amount of concatenation operations.
        // It is important that this step be performed after using the
        // ReturnConvertor and before the main type checking. Otherwise, a
        // return may capture too large of a result or an expression may be
        // set to a type which is only appropriate for output.
        //
        // Note: Increasing the amount of concatenations can have a profound
        // impact on the language semantics depending on how the context is
        // defined. Generally, increasing the amount of concatenations hinders
        // performance. If increasing the amount of concatenations improves
        // performance, then the data buffering and/or output strategy in the
        // context may need to be improved.
        //
        //template.accept(new ConcatenationIncreaser());

        // Perform major type checking.
        new Visitor().check(template);

        if (mErrorCount == 0 && mExceptionGuardian) {
            // Equip the parse tree with ExceptionGuard statements.
            template.accept(new ExceptionGuardian());
        }
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    private class Visitor extends TreeWalker {
        private Type mReturnType;
        private Scope mScope = new Scope();

        private Stack mLoopVariables;

        public Visitor() {
            super();
            mLoopVariables = new Stack();
        }

        private Scope enterScope() {
            return mScope = new Scope(mScope);
        }

        private Scope exitScope() {
            return mScope = mScope.getParent();
        }

        private void defineVariable(VariableRef ref, Type type) {
            Variable v = ref.getVariable();
            boolean staticallyTyped = v != null ? v.isStaticallyTyped() : false;
            mScope.declareVariable
                (new Variable(ref.getSourceInfo(), ref.getName(), type, staticallyTyped));

            if (!mScope.bindToVariable(ref)) {
                // Shouldn't happen.
                error("variable.undefined", ref.getName(), ref);
            }
        }

        public void check(Node node) {
            node.accept(this);
        }

        private void checkAccess(Class clazz, Node node) {
            if ( !(Modifier.isPublic(clazz.getModifiers())) ) {
                error("access.check", clazz.getName(), node);
            }
        }

        public Object visit(Template node) {
            enterScope();

            if (node.getDirectives() != null) {
                for (Iterator i = node.getDirectives().iterator(); i.hasNext(); )
                    check((Directive) i.next());
            }
            
            Variable[] declared = node.getParams();
            if (declared != null) {
                for (int i=0; i<declared.length; i++) {
                    check(declared[i]);
                }
            }

            Statement stmt = node.getStatement();
            if (stmt != null) {
                check(stmt);
            }

            Type returnType = mReturnType;
            if (returnType == null) {
                returnType = Type.VOID_TYPE;
            }
            node.setReturnType(returnType);

            exitScope();
            return null;
        }
        
        public Object visit(Name node) {
            return null;
        }
        
        public Object visit(TypeName node) {
            // Check that type name is a valid java class.
            String name = node.getName();
            StringBuffer fb = new StringBuffer(name);
            // strip array descriptors from class names, we know its an array from the type node.
            if (name != null && name.charAt(0) == '[') {
                fb.deleteCharAt(name.lastIndexOf('[') + 1);
                int i = 0;
                while ((i = fb.indexOf("[")) != -1)
                    fb.deleteCharAt(i);
                if ((i = fb.indexOf(";")) != -1)
                    fb.deleteCharAt(i);
            }
            String checkName = fb.toString();
            String fixedName = checkName;
            String errorMsg = null;

            boolean resolved = false;
            String resolvedPackage = mImports[0];
            for (int i=-1; i<mImports.length; i++) {
                if (i >= 0) {
                    checkName = mImports[i] + '.' + fixedName;
                }

                try {
                    Class clazz = null;
                    if (checkName.equals("boolean"))
                        clazz = Boolean.TYPE;
                    else if (checkName.equals("int"))
                        clazz = Integer.TYPE;
                    else if (checkName.equals("double"))
                        clazz = Double.TYPE;
                    else if (checkName.equals("long"))
                        clazz = Long.TYPE;
                    else if (checkName.equals("short"))
                        clazz = Short.TYPE;
                    else
                        clazz = loadClass(checkName);
                    checkAccess(clazz, node);
                    
                    // Found class, check if should be array.
                    int dim = node.getDimensions();
                    if (dim > 0) {
                        clazz =
                            Array.newInstance(clazz, new int[dim]).getClass();
                    }

                    node.setType(new Type(clazz));
                    errorMsg = null;
                    if (! resolved) {
                        resolved = true;
                        if (! mHasImports)
                            return null;
                        if (i > 0)
                            resolvedPackage = mImports[i];
                        continue;
                    }
                    else {
                        if (i >= 0)
                            error("typename.package.conflict", name, resolvedPackage, mImports[i], node);
                        return null;
                    }
                    
                }
                catch (ClassNotFoundException e) {
                    if (errorMsg == null) {
                        errorMsg = mFormatter.format("typename.unknown", name);
                    }
                }
                catch (RuntimeException e) {
                    error(e.toString(), node);
                }
                catch (LinkageError e) {
                    error(e.toString(), node);
                    return null;
                }
            }

            // Fall through to here only if class wasn't found.
            if (!resolved && errorMsg != null) {
                error(errorMsg, node);
            }

            return null;
        }

        public Object visit(Variable node) {
            String name = node.getName();
            Variable v = mScope.getDeclaredVariable(name);
            if (v == null) {
                mScope.declareVariable(node);
            }
            else {
                error("variable.declared", name, node);
                error("variable.declared.here", name, v);
            }

            TypeName typeName = node.getTypeName();
            check(typeName);
            node.setType(typeName.getType());

            return null;
        }
        
        public Object visit(ExpressionList node) {
            Expression[] exprs = node.getExpressions();
            for (int i=0; i<exprs.length; i++) {
                check(exprs[i]);
            }
            return null;
        }

       
        public Object visit(Directive node) {
            if (node instanceof ImportDirective)
                return visit((ImportDirective) node);

            return null;
        }
        
        public Object visit(ImportDirective node) {
            String packageName = node.getName();
            mHasImports = true;
            LinkedHashSet importSet = new LinkedHashSet(Arrays.asList(mImports));
            if (! importSet.contains(packageName)) {
                importSet.add(packageName);
                mImports = (String[]) importSet.toArray(new String[importSet.size()]);
            }
            return null;
        }
        
        
        public Object visit(Statement node) {
            return null;
        }

        
        public Object visit(StatementList node) {
            Statement[] stmts = node.getStatements();
            for (int i=0; i<stmts.length; i++) {
                if(i>0 && stmts[i-1].isBreak() && 
                   stmts[i].getClass() != Statement.class) {
                    error("break.code.unreachable", stmts[i]);
                }
                check(stmts[i]);
            }
            return null;
        }
        
        public Object visit(Block node) {
            return visit((StatementList)node);
        }


        public Object visit(AssignmentStatement node) {
            VariableRef lvalue = node.getLValue();
            String lname = lvalue.getName();
            if(mLoopVariables.contains(lname)) {
                error("foreach.loopvar.nomodify", lname, node.getSourceInfo());
                return null;
            }

            Expression rvalue = node.getRValue();
            check(rvalue);

            Type type = rvalue.getType();

            // Start mod for declarative typing
            if (type != null) {
                Class rclass = type.getObjectClass();
                Variable dvar = mScope.getDeclaredVariable(lname);
                if (dvar == null) 
                    dvar = lvalue.getVariable();
                if (dvar != null && dvar.isStaticallyTyped()) {
                    check(dvar.getTypeName());
                    if (dvar.getType() == null) // Added this line
                        dvar.setType(dvar.getTypeName().getType());
                    if (dvar.getType() == null)
                        return null;   // Bad type
                    Class lclass = dvar.getType().getObjectClass();
                    if (lvalue.getVariable() == null)
                        lvalue.setVariable(dvar);
                    if (!type.isPrimitive())
                        type = dvar.getType();
                    if (rclass != null && lclass != null && rclass.isAssignableFrom(lclass) && rclass != lclass) {
                        type = dvar.getType();
                        if (rvalue instanceof NullLiteral && type.isPrimitive()) {
                            error("variable.primitive.uninitialized", type.getNaturalClass().getName(), node);
                            return null;
                        }
                        rvalue.convertTo(type.toNullable());
                    }
                    else if (rclass != lclass) {
                        error("assignmentstatement.cast.invalid", rclass.getName(), lclass.getName(), node);
                        return null;
                    }
                }
            }
           //End mod

            if (type != null) {
                if (mExceptionGuardian &&
                    rvalue.isExceptionPossible() &&
                    type.isNonNull()) {

                    // Since the expression may throw an exception and be
                    // guarded, an assignment of null needs to work.

                    rvalue.convertTo(type.toNullable());
                    type = rvalue.getType();
                }
                defineVariable(lvalue, type);
            }

            return null;
        }
        
        public Object visit(BreakStatement node) {
            // check to make sure that the break is contained in a loop
            if(mForeachCount <= 0) {
                error("break.not.inside.foreach", node);
            }

            return null;
        }

        public Object visit(ContinueStatement node) {
            // check to make sure that the break is contained in a loop
            if(mForeachCount <= 0) {
                error("continue.not.inside.foreach", node);
            }

            return null;
        }

        public Object visit(ForeachStatement node) {

            ++mForeachCount;

            VariableRef loopVar = node.getLoopVariable();
            String varName = loopVar.getName();
            if(mLoopVariables.contains(varName)) {
                error("foreach.loopvar.noreuse", varName, 
                      loopVar.getSourceInfo());
                return null;
            }
            mLoopVariables.push(varName);

            Expression range = node.getRange();
            Expression endRange = node.getEndRange();
            Block body = node.getBody();

            check(range);
            Type rangeType = range.getType();

            Type endRangeType;
            if (endRange == null) {
                endRangeType = null;
            }
            else {
                check(endRange);
                endRangeType = endRange.getType();
            }

            if (rangeType == null || 
                (endRange != null && endRangeType == null)) {

                // Some error must have been detected in the range, so just
                // check everything else possible and bail out.

                enterScope();
                check(body);
                exitScope();
                return null;
            }
            
            Class rangeClass = rangeType.getObjectClass();
            Type elementType;

            // If there is no end range, then range expression type must be 
            // an array or Collection.

            if (endRangeType == null) {
                if (loopVar.getVariable() != null && loopVar.getVariable().isStaticallyTyped()) {
                    check(loopVar.getVariable().getTypeName());
                    try {
                        if (rangeType.getIterationElementType() == null)
                            error("foreach.as.collection.required", rangeType.getSimpleName(), range);
                    }
                    catch (IntrospectionException e) {
                        error(e.toString(), node);
                        return null;
                    }
                    
                    elementType = loopVar.getVariable().getTypeName().getType();
                    if (rangeClass.isArray() && (!rangeClass.getComponentType().isAssignableFrom(
                            elementType.getNaturalClass()) 
                            && !elementType.getNaturalClass().isAssignableFrom(
                            rangeClass.getComponentType()))) {
                        try {
                            error("foreach.as.not.assignable", elementType.getSimpleName(), 
                                rangeType.getArrayElementType().getSimpleName(), range);
                        }
                        catch (IntrospectionException e) {
                            error(e.toString(), node);
                            return null;
                        }
                    }
                }
                else {
                    try {
                        elementType = rangeType.getIterationElementType();
                    }
                    catch (IntrospectionException e) {
                        error(e.toString(), node);
                        return null;
                    }
                }
                // end mod

                if (elementType == null) {
                    error("foreach.iteration.not.supported",
                          rangeType.getSimpleName(), range);
                }
                else {
                    checkAccess(elementType.getNaturalClass(), range);

                    if (node.isReverse() && 
                        !rangeType.isReverseIterationSupported()) {
                        error("foreach.reverse.not.supported",
                              rangeType.getSimpleName(), range);
                    }
                }
            }
            else {
                elementType = Type.INT_TYPE;

                if (!Number.class.isAssignableFrom(rangeClass)) {
                    error("foreach.range.start",
                          rangeType.getSimpleName(), range);
                    range.setType(elementType);
                }
                else {
                    if (Long.class.isAssignableFrom(rangeClass)) {
                        elementType = Type.LONG_TYPE;
                    }
                }

                Class endRangeClass = endRangeType.getObjectClass();
                if (!Number.class.isAssignableFrom(endRangeClass)) {
                    error("foreach.range.end",
                          endRangeType.getSimpleName(), endRange);
                    endRange.setType(elementType);
                }
                else {
                    if (Long.class.isAssignableFrom(endRangeClass)) {
                        elementType = Type.LONG_TYPE;
                    }
                }

                range.convertTo(elementType);
                endRange.convertTo(elementType);
            }

            // Enter a new scope because variable declarations local
            // to a foreach loop might never get a value assigned because
            // there is no guarantee that the body will ever execute.
            Scope bodyScope = enterScope();

            if (elementType != null) {
                defineVariable(loopVar, elementType);
            }
            
            check(body);
            exitScope();
            
            Variable[] vars = bodyScope.promote();

            if (vars.length > 0) {
                // Since the body can be executed multiple times, variables
                // must be of the correct type before entering the scope.
                node.setInitializer
                    (createConversions(mScope, vars, node.getSourceInfo()));

                // Apply any conversions at the end of the body as well in
                // order for the variables to be of the correct type when
                // accessed again at the start of the body.
                body.setFinalizer
                    (createConversions(bodyScope, vars, body.getSourceInfo()));

                // Declare all promoted variables outside body scope.
                mScope.declareVariables(vars);

                // Re-check body to account for variable declaration changes.
                if (mErrorCount == 0) {
                    bodyScope.delete();
                    bodyScope = enterScope();
                    
                    if (elementType != null) {
                        defineVariable(loopVar, elementType);
                    }
                    
                    check(body);
                    exitScope();
                }
            }

            mLoopVariables.pop();
            --mForeachCount;
            return null;
        }

        public Object visit(IfStatement node) {
            Expression condition = node.getCondition();
            Block thenPart = node.getThenPart();
            Block elsePart = node.getElsePart();
            Variable[] thenCasts = null;
            Variable[] elseCasts = null;

            int preCondErrorCount = mErrorCount;
            check(condition);

            Type type = condition.getType();
            if (type != null && type.getObjectClass() != Boolean.class) {
                error("if.condition", condition);
            }
            else {
                condition.convertTo(Type.BOOLEAN_TYPE);
                if (preCondErrorCount == mErrorCount) {
                    IsaDetector detector = new IsaDetector();
                    condition.accept(detector);
                    thenCasts = detector.getThenCasts();
                    elseCasts = detector.getElseCasts();
                }
            }

            Scope thenScope = null;
            Scope elseScope = null;

            thenScope = enterScope();
            if (thenPart != null) {
                Statement stmt = createCasts
                    (thenScope, thenCasts, thenPart.getSourceInfo());
                // Don't typecheck any inserted casts, they are already correct
                check(thenPart);
                thenPart.setInitializer(stmt);
            }
            exitScope();

            elseScope = enterScope();
            if (elsePart != null) {
                Statement stmt = createCasts
                    (elseScope, elseCasts, elsePart.getSourceInfo());
                // Don't typecheck any inserted casts, they are already correct
                check(elsePart);
                elsePart.setInitializer(stmt);
            }
            exitScope();

            // Merge then and else scopes
            Variable[] vars = thenScope.intersect(elseScope);

            if (vars.length == 0) {
                node.setMergedVariables(vars);
            }
            else {
                if (mExceptionGuardian && condition.isExceptionPossible()) {
                    // If condition could throw an exception, then the
                    // exception handler needs to ensure that these variables
                    // are assigned null. Therefore, the variables need to be
                    // able to accept null.
                    for (int i=0; i<vars.length; i++) {
                        Variable v = vars[i];
                        Type t = v.getType();
                        if (t.isNonNull()) {
                            v = (Variable)v.clone();
                            v.setType(t.toNullable());
                            vars[i] = v;
                        }
                    }
                }

                node.setMergedVariables(vars);

                // Add corrective code as a result of merging.
                if (thenPart == null) {
                    thenPart = new Block(node.getSourceInfo());
                    node.setThenPart(thenPart);
                }
                Statement fin = createConversions
                    (thenScope, vars, thenPart.getSourceInfo());
                thenPart.setFinalizer(fin);

                if (elsePart == null) {
                    elsePart = new Block(node.getSourceInfo());
                    node.setElsePart(elsePart);
                }
                fin = createConversions
                    (elseScope, vars, elsePart.getSourceInfo());
                elsePart.setFinalizer(fin);

                // Re-declare all new variables with new types for future
                // variable references.
                mScope.declareVariables(vars);
            }

            return null;
        }

        /**
         * Create variable casting assignment statements.
         *
         * @param scope scope of statement
         * @param newVars optional new variables to assign to
         * @param info source info to apply to created assignments
         * @return statement with assignments or null if none
         */
        private Statement createCasts(Scope scope,
                                      Variable[] newVars,
                                      SourceInfo info) {
            if (newVars == null) {
                return null;
            }

            int length = newVars.length;
            if (length == 0) {
                return null;
            }

            List newStatements = new ArrayList(length);

            for (int i=0; i<length; i++) {
                Variable newVar = newVars[i];
                String name = newVar.getName();
                Variable oldVar = scope.getDeclaredVariable(name);

                // It is important that the variable be declared private.
                newVar = scope.declareVariable(newVar, true);

                if (newVar == oldVar) {
                    continue;
                }

                VariableRef lvalue = new VariableRef(info, name);
                scope.bindToVariable(lvalue);
                lvalue.setVariable(newVar);
                
                VariableRef rvalue = new VariableRef(info, name);
                scope.bindToVariable(rvalue);
                rvalue.setVariable(oldVar);
                try {
                    rvalue.convertTo(newVar.getType(), true);
                }
                catch (IllegalArgumentException e) {
                    if (mErrorCount == 0) {
                        throw e;
                    }
                }
                
                newStatements.add
                    (new AssignmentStatement(info, lvalue, rvalue));
            }

            if (newStatements.size() == 0) {
                return null;
            }
            else {
                Statement[] stmts = new Statement[newStatements.size()];
                stmts = (Statement[])newStatements.toArray(stmts);
                return new StatementList(info, stmts);
            }
        }

        /**
         * Create variable conversion assignment statements.
         *
         * @param scope scope of statement
         * @param newVars optional new variables to assign to
         * @param info source info to apply to created assignments
         * @return statement with assignments or null if none
         */
        private Statement createConversions(Scope scope,
                                            Variable[] newVars,
                                            SourceInfo info) {
            if (newVars == null) {
                return null;
            }

            int length = newVars.length;
            if (length == 0) {
                return null;
            }

            List newStatements = new ArrayList(length);

            for (int i=0; i<length; i++) {
                Variable newVar = newVars[i];
                String name = newVar.getName();
                Variable oldVar = scope.getDeclaredVariable(name);

                newVar = scope.declareVariable(newVar);

                if (newVar == oldVar) {
                    continue;
                }

                VariableRef lvalue = new VariableRef(info, name);
                scope.bindToVariable(lvalue);
                lvalue.setVariable(newVar);
                
                VariableRef rvalue = new VariableRef(info, name);
                scope.bindToVariable(rvalue);
                rvalue.setVariable(oldVar);
                rvalue.convertTo(newVar.getType(), false);
                
                newStatements.add
                    (new AssignmentStatement(info, lvalue, rvalue));
            }

            if (newStatements.size() == 0) {
                return null;
            }
            else {
                Statement[] stmts = new Statement[newStatements.size()];
                stmts = (Statement[])newStatements.toArray(stmts);
                return new StatementList(info, stmts);
            }
        }

        public Object visit(SubstitutionStatement node) {
            // Check if substitution allowed in this template.
            if (!mUnit.getParseTree().hasSubstitutionParam()) {
                error("substitution.undeclared", node);
            }

            return null;
        }

        public Object visit(ExpressionStatement node) {
            Expression expr = node.getExpression();
            if (expr instanceof CallExpression) {
                ((CallExpression)expr).setVoidPermitted(true);
            }
            check(expr);

            Type type = expr.getType();
            Compiler c = mUnit.getCompiler();

            if (type != null && 
                type.getNaturalClass() != void.class &&
                c != null) {

                Method[] methods = c.getRuntimeContextMethods();
                String name = c.getRuntimeReceiver();
                
                int cnt = MethodMatcher.match(methods, name, new Type[]{type});
                    
                if (cnt < 1) {
                    error("expressionstatement.receiver",
                          expr.getType().getSimpleName(), node);
                }
                else {
                    Method receiver = methods[0];
                    node.setReceiverMethod(receiver);
                    expr.convertTo(new Type(receiver.getParameterTypes()[0]));
                }
            }

            return null;
        }
        
        public Object visit(ReturnStatement node) {
            Type type;
            Expression expr = node.getExpression();

            if (expr != null) {
                check(expr);
                type = expr.getType();
            }
            else {
                type = Type.VOID_TYPE;
            }

            if (mReturnType == null) {
                mReturnType = type;
            }
            else {
                Type newType = mReturnType.getCompatibleType(type);
                if (newType == null) {
                    error("returnstatement.type", type.getSimpleName(),
                          mReturnType.getSimpleName(), node);
                }
                mReturnType = newType;
            }

            return null;
        }        
        
        public Object visit(Expression node) {
            return null;
        }
        
        public Object visit(ParenExpression node) {
            Expression expr = node.getExpression();
            check(expr);
            return null;
        }
        
        public Object visit(NewArrayExpression node) {
            ExpressionList list = node.getExpressionList();
            check(list);

            Expression[] exprs = list.getExpressions();

            if (node.isAssociative()) {
                if (exprs.length % 2 != 0) {
                    error("newarrayexpression.associative", node);
                }

                Type elementType = newArrayElementType(exprs, 1, 2);
                // Since an element might not be found for a given key, the
                // element type is set to be nullable.
                elementType = elementType.toNonPrimitive().toNullable();

                Type arrayType = new Type(Map.class);
                try {
                    arrayType = arrayType.setArrayElementType(elementType);
                }
                catch (IntrospectionException e) {
                    error(e.toString(), node);
                    return null;
                }
                node.setType(arrayType);

                // Make sure all keys are converted to Objects.
                for (int i=0; i<exprs.length; i+=2) {
                    exprs[i].convertTo(Type.OBJECT_TYPE);
                }
            }
            else {
                Type elementType = newArrayElementType(exprs, 0, 1);
                Class elementClass = elementType.getNaturalClass();
                Class arrayClass = 
                    Array.newInstance(elementClass, 0).getClass();

                Type arrayType = new Type(arrayClass);
                if (elementType.isNonNull()) {
                    try {
                        arrayType = arrayType.setArrayElementType(elementType);
                    }
                    catch (IntrospectionException e) {
                    }
                }
                node.setType(arrayType);
            }

            return null;
        }
        
        private Type newArrayElementType(Expression[] exprs, 
                                         int start, int increment) {
            Type elementType = null;

            for (int i = start; i < exprs.length; i += increment) {
                Type type = exprs[i].getType();

                if (elementType == null) {
                    elementType = type;
                }
                else {
                    elementType = elementType.getCompatibleType(type);
                }
            }
            
            if (elementType == null) {
                elementType = Type.NULL_TYPE;
            }

            return elementType;
        }
        
        public Object visit(FunctionCallExpression node) {
            if (!initialCallExpressionCheck(node)) {
                return null;
            }

            Expression[] exprs = node.getParams().getExpressions();
            int length = exprs.length;
            Type[] actualTypes = new Type[length];
            for (int i=0; i<length; i++) {
                actualTypes[i] = exprs[i].getType();
            }

            Block subParam = node.getSubstitutionParam();
            Compiler compiler = mUnit.getCompiler();
            String name = node.getTarget().getName();

            // Look for Java function to call.
            name = name.replace('.', '$');

            if (subParam != null) {
                Type[] types = new Type[length + 1];
                System.arraycopy(actualTypes, 0, types, 0, length);
                types[length] = 
                    new Type(org.teatrove.tea.runtime.Substitution.class);
                actualTypes = types;
            }

            Method[] methods = compiler.getRuntimeContextMethods();
            int cnt = MethodMatcher.match(methods, name, actualTypes);
            if (cnt <= 0) {
                error("functioncallexpression.not.found", node);
                return null;
            }

            Method m = methods[0];
            node.setCalledMethod(m);
            
            if (m.getDeclaringClass() == Object.class) {
                error("functioncallexpression.not.found", node);
            }
            
            // Set param types.
            Class[] paramClasses = m.getParameterTypes();
            for (int i=0; i<length; i++) {
                exprs[i].convertTo(new Type(paramClasses[i]), false);
            }
            
            Class retClass = m.getReturnType();
            TypedElement te = m.getAnnotation(TypedElement.class);
            checkAccess(retClass, node);
            Type retType;
            if (retClass == char.class) {
                // Convert any returned char to a String.
                retType = Type.NON_NULL_STRING_TYPE;
            }
            else if (retClass == Character.class) {
                // Convert any returned Character to a String.
                retType = Type.STRING_TYPE;
            }
            else {
                retType = te != null ? new Type(retClass, te) : new Type(retClass);
            }
            node.setType(retType);
            
            if (retClass == void.class && !node.isVoidPermitted()) {
                error("functioncallexpression.function.void", node);
            }

            return null;

        }

        public Object visit(TemplateCallExpression node) {
            if (!initialCallExpressionCheck(node)) {
                return null;
            }

            Expression[] exprs = node.getParams().getExpressions();
            int length = exprs.length;
            Type[] actualTypes = new Type[length];
            for (int i=0; i<length; i++) {
                actualTypes[i] = exprs[i].getType();
            }

            Block subParam = node.getSubstitutionParam();
            Compiler compiler = mUnit.getCompiler();
            String name = node.getTarget().getName();

            // Look for a matching template to call.

            CompilationUnit unit = compiler.getCompilationUnit(name, mUnit);
            if (unit == null) {
                error("templatecallexpression.not.found", node);
                return null;
            }

            Template tree = unit.getParseTree();
            
            Variable[] formalParams = tree.getParams();
            if (formalParams != null) {
                if (formalParams.length != length) {
                    error("templatecallexpression.parameter.count",
                          String.valueOf(formalParams.length), 
                          String.valueOf(length), node);
                    return null;
                }
                
                if (subParam != null && !tree.hasSubstitutionParam()) {
                    error("templatecallexpression.substitution.no",
                          tree.getName().getName(), subParam);
                    return null;
                }
                else if (subParam == null && tree.hasSubstitutionParam()) {
                    error("templatecallexpression.substitution.yes",
                          tree.getName().getName(), node);
                }
                
                node.setCalledTemplate(unit);
                for (int i=0; i<length; i++) {
                    Type type = formalParams[i].getType();
                    
                    if (type == null) {
                        error("templatecallexpression.parameter.unknown",
                              node);
                        return null;
                    }
                    
                    int cost = type.convertableFrom(actualTypes[i]);
                    if (cost < 0) {
                        node.setCalledTemplate(null);
                        
                        String msg1 = actualTypes[i].getFullName();
                        String msg2 = type.getFullName();
                        
                        ClassLoader CL1 = actualTypes[i].getNaturalClass()
                            .getClassLoader();
                        
                        ClassLoader CL2 = type.getNaturalClass()
                            .getClassLoader();
                        
                        if (CL1 != CL2) {
                            msg1 += "(" + CL1 + ")";
                            msg2 += "(" + CL2 + ")";
                        }
                        
                        error("templatecallexpression.conversion",
                              msg1, msg2, exprs[i]);
                    }
                    else {
                        exprs[i].convertTo(type, false);
                    }
                }
            }
            
            Type retType = tree.getReturnType();
            if (retType == null) {
                retType = Type.VOID_TYPE;
            }
            node.setType(retType);
            
            if (Type.VOID_TYPE.equals(retType) && 
                !node.isVoidPermitted()) {
                
                error("templatecallexpression.template.void", node);
                return null;
            }
            
            return null;
        }

        /**
         * Returns false if initial checks failed.
         */
        private boolean initialCallExpressionCheck(CallExpression node) {
            ExpressionList params = node.getParams();
            check(params);

            Statement init = node.getInitializer();
            if (init != null) {
                check(init);
            }

            Block subParam = node.getSubstitutionParam();
            if (subParam != null) {
                // Enter a new scope because variable declarations local
                // to a substitution block might never get a value assigned 
                // because there is no guarantee it will ever execute.
                Scope subScope = enterScope();
                check(subParam);
                exitScope();

                Variable[] vars = subScope.promote();

                if (vars.length > 0) {
                    // Since the subParam can be executed multiple times,
                    // variables must be of the correct type before entering
                    // the scope.
                    node.setInitializer
                        (createConversions(mScope, vars,node.getSourceInfo()));

                    // Apply any conversions at the end of the subParam as well
                    // in order for the variables to be of the correct type
                    // when accessed again at the start of the body.
                    subParam.setFinalizer
                        (createConversions
                         (subScope, vars, subParam.getSourceInfo()));

                    // Promoted variables need to become fields
                    for (int i=0; i<vars.length; i++) {
                        vars[i].setField(true);
                    }

                    // Declare all promoted variables outside subParam scope.
                    mScope.declareVariables(vars);
                    
                    // Re-check subParam to account for variable declaration
                    // changes.
                    if (mErrorCount == 0) {
                        subScope.delete();
                        subScope = enterScope();
                        check(subParam);
                        exitScope();
                    }
                }

                // References inside a substitution block to variables
                // outside need to be fields so that they can be shared.
                VariableRef[] refs = subScope.getOutOfScopeVariableRefs();
                for (int i=0; i<refs.length; i++) {
                    refs[i].getVariable().setField(true);
                }
            }

            Expression[] exprs = params.getExpressions();
            int length = exprs.length;
            for (int i=0; i<length; i++) {
                if (exprs[i].getType() == null) {
                    // Type of a parameter is unknown, so bail out.
                    return false;
                }
            }

            Compiler compiler = mUnit.getCompiler();
            if (compiler == null) {
                return false;
            }

            return true;
        }
        
        public Object visit(VariableRef node) {
            if (!mScope.bindToVariable(node)) {
                error("variableref.undefined", node.getName(), node);
            }
            return null;
        }
        
        public Object visit(Lookup node) {
            Expression expr = node.getExpression();
            check(expr);

            // Now check if expression type class contains the lookup name.
            Type type = expr.getType();
            String lookupName = node.getLookupName().getName();

            if (type != null && lookupName != null) {
                Class clazz = type.getObjectClass();
                // Lookup can only work on objects.
                type = type.toNonPrimitive();
                expr.convertTo(type);

                if ("length".equals(lookupName)) {
                    if (clazz == String.class) {
                        node.setType(Type.INT_TYPE);
                        try {
                            node.setReadMethod
                                (String.class.getMethod("length", 
                                                        new Class[0]));
                        }
                        catch (NoSuchMethodException e) {
                            throw new LinkageError(e.toString());
                        }
                        return null;
                    }
                    else if (Collection.class.isAssignableFrom(clazz)) {
                        node.setType(Type.INT_TYPE);
                        try {
                            node.setReadMethod
                                (Collection.class.getMethod("size", 
                                                            new Class[0]));
                        }
                        catch (NoSuchMethodException e) {
                            throw new LinkageError(e.toString());
                        }
                        return null;
                    }
                    else if (clazz.isArray()) {
                        node.setType(Type.INT_TYPE);
                        return null;
                    }
                }

                Map properties;
                try {
                    properties = BeanAnalyzer.getAllProperties(clazz);
                }
                catch (IntrospectionException e) {
                    error(e.toString(), node);
                    return null;
                }

                PropertyDescriptor prop = 
                    (PropertyDescriptor)properties.get(lookupName);

                if (prop == null) {
                    error("lookup.undefined", lookupName, type.getSimpleName(),
                          node.getLookupName());
                    return null;
                }

                Class retClass = prop.getPropertyType();
                if (retClass == null) {
                    error("lookup.array.only", lookupName,
                          node.getLookupName());
                    return null;
                }

                Method rmethod = prop.getReadMethod();
                TypedElement te = null;
                if (rmethod != null) 
                    te = rmethod.getAnnotation(TypedElement.class);
                else 
                    error("lookup.unreadable", lookupName, node.getLookupName());
              
                checkAccess(retClass, node.getLookupName());

                node.setReadMethod(rmethod);

                node.setType(te != null ? new Type(retClass, te) : new Type(retClass));

                if (retClass == char.class) {
                    // Convert any returned char to a String.
                    node.convertTo(Type.NON_NULL_STRING_TYPE);
                }
                else if (retClass == Character.class) {
                    // Convert any returned Character to a String.
                    node.convertTo(Type.STRING_TYPE);
                }

            }

            return null;
        }
        
        public Object visit(ArrayLookup node) {
            Expression expr = node.getExpression();
            Expression lookupIndex = node.getLookupIndex();
            check(expr);
            check(lookupIndex);

            Type type = expr.getType();
            Type lookupType = lookupIndex.getType();

            if (type != null && lookupType != null) {
                // Array lookup can only work on objects.
                type = type.toNonPrimitive();
                expr.convertTo(type);

                // Now check if expression type class supports array lookup.
                // If so, check that lookup index is correct type
                
                Type elementType;
                try {
                    elementType = type.getArrayElementType();
                }
                catch (IntrospectionException e) {
                    error(e.toString(), node);
                    return null;
                }

                if (elementType == null) {
                    error("arraylookup.unsupported", type.getSimpleName(),
                          node.getLookupToken());
                    
                    return null;
                }

                checkAccess(elementType.getObjectClass(), node);

                // Look for the best array access method.

                Method[] methods;
                try {
                    methods = type.getArrayAccessMethods();
                }
                catch (IntrospectionException e) {
                    error(e.toString(), node);
                    return null;
                }

                boolean good = false;

                if (methods.length == 0) {
                    // Must be an actual Java array.
                    if (type.getObjectClass().isArray()) {
                        Class lookupClass = lookupType.getObjectClass();
                        if (Number.class.isAssignableFrom(lookupClass)) {
                            lookupIndex.convertTo(Type.INT_TYPE);
                            node.setType(elementType);
                            good = true;
                        }
                    }
                }
                else {
                    int count = MethodMatcher.match(methods, null,
                                                    new Type[] {lookupType});
                    if (count >= 1) {
                        Method m = methods[0];
                        lookupType = new Type(m.getParameterTypes()[0]);
                        lookupIndex.convertTo(lookupType);
                        node.setReadMethod(m);
                        node.setType(new Type(m.getReturnType()));
                        node.convertTo(elementType);
                        good = true;
                    }
                }

                if (good) {
                    if (elementType.getObjectClass() == Character.class) {
                        // Convert any returned character to a String.
                        node.convertTo(Type.STRING_TYPE);
                    }
                }
                else {
                    error("arraylookup.unsupported.for", type.getSimpleName(),
                          lookupType.getSimpleName(), lookupIndex);
                }
            }

            return null;
        }

        public Object visit(NegateExpression node) {
            Expression expr = node.getExpression();
            check(expr);

            Type type = expr.getType();
            if (type == null) {
                node.setType(Type.INT_TYPE);
            }
            else if (Number.class.isAssignableFrom(type.getObjectClass())) {
                type = type.toPrimitive();
                expr.convertTo(type);
                node.setType(type);
            }
            else {
                error("negateexpression.type", node);
            }

            return null;
        }
        
        public Object visit(NotExpression node) {
            Expression expr = node.getExpression();
            check(expr);

            Type type = expr.getType();
            if (type == null) {
                node.setType(Type.BOOLEAN_TYPE);
            }
            else if (type.getObjectClass() == Boolean.class) {
                type = type.toPrimitive();
                expr.convertTo(type);
                node.setType(type);
            }
            else {
                error("notexpression.type", node);
            }

            return null;
        }

        public Object visit(ConcatenateExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();

            check(left);
            check(right);
            
            left.convertTo(Type.NON_NULL_STRING_TYPE, false);
            right.convertTo(Type.NON_NULL_STRING_TYPE, false);
            node.setType(Type.NON_NULL_STRING_TYPE);

            return null;
        }

        public Object visit(ArithmeticExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();
            
            check(left);
            check(right);

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (binaryTypeCheck(node, Number.class)) {
                Type type = 
                    leftType.getCompatibleType(rightType).toPrimitive();

                left.convertTo(type);
                right.convertTo(type);
                node.setType(type);
            }

            return null;
        }

        private boolean binaryTypeCheck(BinaryExpression expr, Class clazz) {
            Expression left = expr.getLeftExpression();
            Expression right = expr.getRightExpression();

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (leftType == null || rightType == null) {
                return false;
            }

            if (!clazz.isAssignableFrom(leftType.getObjectClass())) {
                if (!clazz.isAssignableFrom(rightType.getObjectClass())) {
                    String name = new Type(clazz).getSimpleName();
                    error("binaryexpression.type.both",
                          expr.getOperator().getImage(), name, expr);
                }
                else {
                    String name = new Type(clazz).getSimpleName();
                    error("binaryexpression.type.left",
                          expr.getOperator().getImage(), name, left);
                }
            }
            else if (!clazz.isAssignableFrom(rightType.getObjectClass())) {
                String name = new Type(clazz).getSimpleName();
                error("binaryexpression.type.right",
                      expr.getOperator().getImage(), name, right);
            }
            else {
                return true;
            }

            return false;
        }
        
        public Object visit(RelationalExpression node) {
            node.setType(Type.BOOLEAN_TYPE);

            Token token = node.getOperator();
            int ID = token.getID();

            if (ID == Token.ISA) {
                return visitIsa(node);
            }

            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();
            
            check(left);
            check(right);

            Type leftType = left.getType();
            Type rightType = right.getType();

            if (leftType != null && rightType != null) {
                Class leftClass = leftType.getNaturalClass();
                Class rightClass = rightType.getNaturalClass();

                Type type;
                if (ID == Token.EQ || ID == Token.NE) {
                    if (String.class.isAssignableFrom(leftClass)) {
                        type = leftType.toNullable();
                    }
                    else if (String.class.isAssignableFrom(rightClass)) {
                        type = rightType.toNullable();
                    }
                    else {
                        type = leftType.getCompatibleType(rightType);
                    }
                }
                else {
                    type = leftType.getCompatibleType(rightType);
                }

                if (type == null) {
                    type = Type.NULL_TYPE;
                }

                Class clazz = type.getObjectClass();

                if (type.hasPrimitivePeer() &&
                    leftType.isNonNull() && rightType.isNonNull() &&
                    (leftType.isPrimitive() || leftType.hasPrimitivePeer()) &&
                    (rightType.isPrimitive() || rightType.hasPrimitivePeer()))
                {
                    leftType = rightType = type.toPrimitive();
                }
                else {
                    if (leftType.isNonNull()) {
                        leftType = type.toNonNull();
                        if (rightType.isNonNull()) {
                            rightType = leftType;
                        }
                        else {
                            rightType = type;
                        }
                    }
                    else {
                        leftType = type;
                        if (rightType.isNonNull()) {
                            rightType = type.toNonNull();
                        }
                        else {
                            rightType = type;
                        }
                    }
                }

                if (ID == Token.EQ || ID == Token.NE ||
                    Comparable.class.isAssignableFrom(clazz) ||
                    String.class.isAssignableFrom(clazz) ||
                    Number.class.isAssignableFrom(clazz)) {

                    // Don't prefer cast; possibly perform string conversion.
                    left.convertTo(leftType, false);
                    right.convertTo(rightType, false);
                }
                else {
                    error("relationalexpression.type.mismatch",
                          token.getImage(), left.getType().getSimpleName(),
                          right.getType().getSimpleName(), node);
                }
            }

            node.setType(Type.BOOLEAN_TYPE);

            return null;
        }

        private Object visitIsa(RelationalExpression node) {
            Token token = node.getOperator();

            Expression left = node.getLeftExpression();
            TypeName typeName = node.getIsaTypeName();

            check(left);
            check(typeName);

            if (!(left instanceof VariableRef)) {
                error("relationalexpression.isa.left", token.getImage(), left);
            }

            Type leftType = left.getType();
            Type rightType = typeName.getType();

            if (leftType != null && rightType != null) {
                // Ensure the left type is an object.
                leftType = leftType.toNonPrimitive();
                left.convertTo(leftType);

                Class leftClass = leftType.getObjectClass();
                Class rightClass = rightType.getObjectClass();

                if (rightClass.isAssignableFrom(leftClass)) {
                    // Widening case. i.e. (5 isa Number) is always true.
                }
                else if (leftClass.isAssignableFrom(rightClass)) {
                    // Narrowing case. i.e. (n isa Integer) might be true.

                    // For this case, a cast operation needs to be inserted
                    // in an if statement.
                }
                else {
                    SourceInfo info = new SourceDetailedInfo
                        (node.getSourceInfo(),
                         typeName.getSourceInfo().getDetailPosition());

                    error("relationalexpression.isa.impossible", 
                          leftType.getSimpleName(), rightType.getSimpleName(),
                          info);
                }
            }

            return null;
        }

        public Object visit(AndExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();

            check(left);
            check(right);

            Type type = Type.BOOLEAN_TYPE;

            if (binaryTypeCheck(node, Boolean.class)) {
                left.convertTo(type);
                right.convertTo(type);
            }

            node.setType(type);

            return null;
        }
        
        public Object visit(OrExpression node) {
            Expression left = node.getLeftExpression();
            Expression right = node.getRightExpression();

            check(left);
            check(right);

            Type type = Type.BOOLEAN_TYPE;

            if (binaryTypeCheck(node, Boolean.class)) {
                left.convertTo(type);
                right.convertTo(type);
            }

            node.setType(type);

            return null;
        }
        
        public Object visit(NullLiteral node) {
            return null;
        }
        
        public Object visit(BooleanLiteral node) {
            return null;
        }
        
        public Object visit(StringLiteral node) {
            return null;
        }
        
        public Object visit(NumberLiteral node) {
            return null;
        }
    }

    /**
     * Detects relational expressions that refine variable types for an if
     * statement's then and else scopes.
     */
    private static class IsaDetector extends TreeWalker {
        private static final char NOT_OP = 'n';
        private static final char AND_OP = 'a';
        private static final char OR_OP = 'o';

        private StringBuffer mOpStack;

        private Collection mThenCasts;
        private Collection mElseCasts;

        public Variable[] getThenCasts() {
            if (mThenCasts == null) {
                return null;
            }
            else {
                Variable[] vars = new Variable[mThenCasts.size()];
                return (Variable[])mThenCasts.toArray(vars);
            }
        }

        public Variable[] getElseCasts() {
            if (mElseCasts == null) {
                return null;
            }
            else {
                Variable[] vars = new Variable[mElseCasts.size()];
                return (Variable[])mElseCasts.toArray(vars);
            }
        }

        public Object visit(RelationalExpression node) {
            int operator = node.getOperator().getID();
            // "isa" and "!=" apply casts to "then" scope, "==" to "else".
            // The "isa" cast makes the type more specific, the "!=" cast makes
            // the type non-null, and the "==" cast makes the type non-null in
            // the "else" scope.
            boolean forThenPart = operator != Token.EQ;

            boolean typeKnown = true;

            if (mOpStack != null) {
                int length = mOpStack.length();
                for (int i=0; i<length; i++) {
                    switch (mOpStack.charAt(i)) {
                    case NOT_OP:
                        forThenPart = !forThenPart;
                        break;
                    case AND_OP:
                        if (!forThenPart) {
                            typeKnown = false;
                        }
                        break;
                    case OR_OP:
                        if (forThenPart) {
                            typeKnown = false;
                        }
                        break;
                    }
                }
            }

            if (!typeKnown) {
                return super.visit(node);
            }
            
            if (operator == Token.ISA) {
                TypeName typeName = node.getIsaTypeName();
                if (typeName != null) {
                    Expression left = node.getLeftExpression();
                    if (left instanceof VariableRef) {
                        Type type = typeName.getType();
                        if (type != null) {
                            addCast((VariableRef)left, type.toNonNull(), 
                                    forThenPart);
                        }
                    }
                }
            }
            else if (operator == Token.EQ || operator == Token.NE) {
                VariableRef ref;
                Expression test;
                
                Expression left = node.getLeftExpression();
                Expression right = node.getRightExpression();
                
                if (left instanceof VariableRef) {
                    ref = (VariableRef)left;
                    test = right;
                }
                else if (right instanceof VariableRef) {
                    ref = (VariableRef)right;
                    test = left;
                }
                else {
                    ref = null;
                    test = null;
                }

                if (test != null && 
                    test.isValueKnown() && test.getValue() == null) {

                    Type type = ref.getType();
                    if (type != null) {
                        // If the expression "var != null" is true, then
                        // the the type becomes non-null.
                        
                        if (!type.isNonNull()) {
                            addCast(ref, type.toNonNull(), forThenPart);
                        }
                    }
                }
            }

            return super.visit(node);
        }

        public Object visit(NotExpression node) {
            pushOp(NOT_OP);
            super.visit(node);
            popOp();
            return null;
        }

        public Object visit(AndExpression node) {
            pushOp(AND_OP);
            super.visit(node);
            popOp();
            return null;
        }

        public Object visit(OrExpression node) {
            pushOp(OR_OP);
            super.visit(node);
            popOp();
            return null;
        }

        private void pushOp(char op) {
            if (mOpStack == null) {
                mOpStack = new StringBuffer(4);
            }
            mOpStack.append(op);
        }

        private void popOp() {
            if (mOpStack != null) {
                int length = mOpStack.length();
                if (length > 0) {
                    mOpStack.setLength(length - 1);
                }
            }
        }

        private void addCast(VariableRef ref, Type type, boolean forThenPart) {
            Variable oldVar = ref.getVariable();
            if (oldVar != null) {
                Variable newVar = (Variable)oldVar.clone();
                newVar.setType(type);
                newVar.setField(false);

                if (forThenPart) {
                    if (mThenCasts == null) {
                        mThenCasts = new ArrayList(2);
                    }
                    mThenCasts.add(newVar);
                }
                else {
                    if (mElseCasts == null) {
                        mElseCasts = new ArrayList(2);
                    }
                    mElseCasts.add(newVar);
                }
            }
        }
    }

    /**
     * Ensures that the template ends in a ReturnStatement. If the final
     * Statement in the template is an ExpressionStatement, then it is 
     * converted to a ReturnStatement. Otherwise, a void ReturnStatement is
     * added to the end. 
     */
    private static class ReturnConvertor extends TreeMutator {
        private boolean mReturnAdded;
        public Object visit(Template node) {
            Statement stmt = node.getStatement();
            if (stmt != null) {
                stmt = (Statement)stmt.accept(this);
                if (!mReturnAdded) {
                    Statement[] stmts = new Statement[] {
                        stmt,
                        new ReturnStatement(stmt.getSourceInfo())
                    };
                    stmt = new StatementList(stmt.getSourceInfo(), stmts);
                }
                node.setStatement(stmt);
            }
            else {
                node.setStatement(new ReturnStatement(node.getSourceInfo()));
            }

            return node;
        }

        public Object visit(Statement node) {
            return node;
        }

        public Object visit(ImportDirective node) {
            return node;
        }

        public Object visit(StatementList node) {
            // Just traverse the last statement in the list.
            Statement[] statements = node.getStatements();
            for (int i = statements.length - 1; i >= 0; i--) {
                Statement stmt = statements[i];
                if (stmt != null) {
                    statements[i] = (Statement)stmt.accept(this);
                    break;
                }
            }
            return node;
        }

        public Object visit(Block node) {
            return visit((StatementList)node);
        }

        public Object visit(ExpressionStatement node) {
            mReturnAdded = true;
            Expression expr = node.getExpression();
            if (expr instanceof CallExpression) {
                ((CallExpression)expr).setVoidPermitted(true);
            }
            return new ReturnStatement(expr);
        }

        public Object visit(AssignmentStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(BreakStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(ContinueStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(ForeachStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(IfStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(SubstitutionStatement node) {
            // Skip traversing this node altogether.
            return node;
        }

        public Object visit(ReturnStatement node) {
            // Skip traversing this node altogether.
            return node;
        }
    }

    /**
     * Wraps all statements that can throw an exception with an
     * ExceptionGuardStatement.
     */
    private static class ExceptionGuardian extends TreeMutator {
        public Object visit(AssignmentStatement node) {
            if (!node.getRValue().isExceptionPossible()) {
                return node;
            }

            SourceInfo info = node.getSourceInfo();
            VariableRef lvalue = node.getLValue();

            // Replacement assigns null to lvalue.
            Statement replacement =
                new AssignmentStatement(info, lvalue, new NullLiteral(info));

            return new ExceptionGuardStatement(node, replacement);
        }
        
        public Object visit(ForeachStatement node) {
            Block body = node.getBody();
            if (body != null) {
                node.setBody(visitBlock(body));
            }

            boolean guard = false;
            test: {
                Expression range = node.getRange();
                if (range != null) {
                    if (range.isExceptionPossible()) {
                        guard = true;
                        break test;
                    }
                    Type type = range.getType();
                    if (type != null && type.isNullable()) {
                        guard = true;
                        break test;
                    }
                }
                
                range = node.getEndRange();
                if (range != null) {
                    if (range.isExceptionPossible()) {
                        guard = true;
                        break test;
                    }
                    Type type = range.getType();
                    if (type != null && type.isNullable()) {
                        guard = true;
                        break test;
                    }
                }
            }

            if (!guard) {
                return node;
            }

            return new ExceptionGuardStatement(node, node.getInitializer());
        }

        public Object visit(IfStatement node) {
            Block block = node.getThenPart();
            if (block != null) {
                node.setThenPart(visitBlock(block));
            }
            
            block = node.getElsePart();
            if (block != null) {
                node.setElsePart(visitBlock(block));
            }

            if (!node.getCondition().isExceptionPossible()) {
                return node;
            }

            Variable[] vars = node.getMergedVariables();
            int length;

            if (vars == null || (length = vars.length) == 0) {
                return new ExceptionGuardStatement(node, null);
            }

            // Create replacement assignments to ensure merged variables get
            // assigned null.
            Statement[] replacements = new Statement[length];
            SourceInfo info = node.getSourceInfo();

            for (int i=0; i<length; i++) {
                Variable v = vars[i];
                VariableRef lvalue = new VariableRef(v.getSourceInfo(),
                                                     v.getName());
                lvalue.setVariable(v);

                replacements[i] = new AssignmentStatement
                    (info, lvalue, new NullLiteral(info));
            }

            Statement replacement = new StatementList(info, replacements);
            return new ExceptionGuardStatement(node, replacement);
        }
        
        public Object visit(SubstitutionStatement node) {
            return new ExceptionGuardStatement(node, null);
        }
        
        public Object visit(ExpressionStatement node) {
            Expression expr = node.getExpression();
            if (expr != null) {
                if (expr instanceof CallExpression) {
                    expr = (CallExpression)expr.accept(this);
                    node.setExpression(expr);
                }
                if (expr.isExceptionPossible()) {
                    return new ExceptionGuardStatement(node, null);
                }
            }
            return node;
        }
        
        public Object visit(ReturnStatement node) {
            Expression expr = node.getExpression();
            if (expr instanceof CallExpression) {
                node.setExpression((CallExpression)expr.accept(this));
            }
            return node;
        }

        public Object visit(FunctionCallExpression node) {
            return visit((CallExpression)node);
        }
        
        public Object visit(TemplateCallExpression node) {
            return visit((CallExpression)node);
        }

        private Object visit(CallExpression node) {
            Block subParam = node.getSubstitutionParam();
            if (subParam != null) {
                node.setSubstitutionParam(visitBlock(subParam));
            }
            return node;
        }
    }

    /**
     * Reduces the amount of string concatenation operations performed by
     * breaking up ExpressionStatements that encapsulate ConcatenateExpressions
     * into several individual ExpressionStatements. Concatenations that are
     * enclosed in parenthesis are not broken up.
     */
    private static class ConcatenationReducer extends TreeMutator {
        public Object visit(ExpressionStatement node) {
            // Recurse into node.
            super.visit(node);

            Expression expr = node.getExpression();
            if (!(expr instanceof ConcatenateExpression)) {
                return node;
            }

            Collection statements = new ArrayList();
            breakup(statements, (ConcatenateExpression)expr);

            Statement[] stmts = new Statement[statements.size()];
            stmts = (Statement[])statements.toArray(stmts);

            return new StatementList(node.getSourceInfo(), stmts);
        }

        private void breakup(Collection statements,
                             ConcatenateExpression concat) {

            Expression left = concat.getLeftExpression();
            if (left instanceof ConcatenateExpression) {
                breakup(statements, ((ConcatenateExpression)left));
            }
            else {
                statements.add(new ExpressionStatement(left));
            }

            Expression right = concat.getRightExpression();
            if (right instanceof ConcatenateExpression) {
                breakup(statements, ((ConcatenateExpression)right));
            }
            else {
                statements.add(new ExpressionStatement(right));
            }
        }
    }

    /**
     * Increases the amount of string concatenation operations by finding
     * consecutive ExpressionStatements and merging them together into a
     * single ExpressionStatement that operates on the concatenated results.
     */ 
    private static class ConcatenationIncreaser extends TreeMutator {
        public Object visit(StatementList node) {
            // Recurse into node.
            super.visit(node);

            return new StatementList(node.getSourceInfo(), 
                                     visit(node.getStatements()));
        }

        public Object visit(Block node) {
            // Recurse into node.
            super.visit(node);

            return new Block(node.getSourceInfo(), 
                             visit(node.getStatements()));
        }

        private Statement[] visit(Statement[] stmts) {
            int length = stmts.length;

            Collection statements = new ArrayList();
            List expressionStatements = new ArrayList();

            for (int i=0; i<length; i++) {
                Statement stmt = stmts[i];
                if (stmt instanceof ExpressionStatement) {
                    Expression expr = 
                        ((ExpressionStatement)stmt).getExpression();
                    
                    if (!(expr instanceof CallExpression)) {
                        expressionStatements.add(stmt);
                        continue;
                    }
                }
                
                merge(statements, expressionStatements);
                expressionStatements.clear();
                statements.add(stmt);
            }

            merge(statements, expressionStatements);

            stmts = new Statement[statements.size()];
            return (Statement[])statements.toArray(stmts);
        }

        private void merge(Collection statements, List expressionStatements) {
            int size = expressionStatements.size();

            if (size == 0) {
            }
            else if (size == 1) {
                statements.add(expressionStatements.get(0));
            }
            else {
                List expressions = new ArrayList();
                
                for (int i=0; i<size; i++) {
                    ExpressionStatement stmt = 
                        (ExpressionStatement)expressionStatements.get(i);
                    gatherExpressions(expressions, stmt.getExpression());
                }

                size = expressions.size();
                Expression concat = (Expression)expressions.get(0);

                for (int i=1; i<size; i++) {
                    Expression right = (Expression)expressions.get(i);
                    SourceInfo rightInfo = right.getSourceInfo();

                    SourceInfo info = 
                        concat.getSourceInfo().setEndPosition(rightInfo);

                    Token token = new Token(rightInfo, Token.CONCAT);

                    concat = new ConcatenateExpression
                        (info, token, concat, right);
                }
                
                statements.add(new ExpressionStatement(concat));
            }
        }

        private void gatherExpressions(Collection expressions,
                                       Expression expr) {
            if (expr instanceof ConcatenateExpression) {
                ConcatenateExpression concat = (ConcatenateExpression)expr;
                gatherExpressions(expressions, concat.getLeftExpression());
                gatherExpressions(expressions, concat.getRightExpression());
            }
            else {
                expressions.add(expr);
            }
        }
    }
}
