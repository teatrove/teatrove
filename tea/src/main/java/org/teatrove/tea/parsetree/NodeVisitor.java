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

package org.teatrove.tea.parsetree;

/**
 * A NodeVisitor enables operations to be performed on a parse tree.
 * The Visitor design pattern is discussed in detail in <i>Design Patterns</i>
 * (ISBN 0-201-63361-2) by Gamma, Helm, Johnson and Vlissides.
 *
 * <p>The traditional operations performed on parse trees are type checking
 * and code generation. The Visitor allows those operations to all be
 * encapsulated into one place instead of having that functionality spread
 * out into every Node subclass. It also makes it easy to target multiple
 * languages by allowing any kind of code generation Visitor to be
 * designed.
 *
 * <p>When using a Visitor to traverse a parse tree, the code responsible
 * for moving the traversal into children nodes can either be placed in
 * the nodes or in the Visitor implementation. The NodeVisitor places that
 * responsibility onto NodeVisitor implementations because it is much
 * more flexible. As a result, all Nodes have the same simple implementation
 * for their "accept" method, but do not inherit it.
 *
 * <p>Every visit method in this interface returns an Object. The definition
 * of that returned Object is left up to the specific implementation of the
 * NodeVisitor. Most NodeVisitors can simply return null, but those that
 * are modifying a parse tree or are using one to create another can use the
 * returned Object to pass around newly created Nodes.
 *
 * @author Brian S O'Neill
 * @see Node#accept(NodeVisitor)
 */
public interface NodeVisitor {
    public Object visit(Template node);
    public Object visit(Name node);
    public Object visit(TypeName node);
    public Object visit(Variable node);
    public Object visit(ExpressionList node);

    public Object visit(Statement node);
    public Object visit(StatementList node);
    public Object visit(Block node);
    public Object visit(AssignmentStatement node);
    public Object visit(ForeachStatement node);
    public Object visit(IfStatement node);
    public Object visit(SubstitutionStatement node);
    public Object visit(ExpressionStatement node);
    public Object visit(ReturnStatement node);
    public Object visit(ExceptionGuardStatement node);
    public Object visit(BreakStatement node);
    public Object visit(ContinueStatement node);

    public Object visit(Expression node);
    public Object visit(ParenExpression node);
    public Object visit(NewArrayExpression node);
    public Object visit(FunctionCallExpression node);
    public Object visit(TemplateCallExpression node);
    public Object visit(VariableRef node);
    public Object visit(Lookup node);
    public Object visit(ArrayLookup node);
    public Object visit(NegateExpression node);
    public Object visit(NotExpression node);
    public Object visit(ConcatenateExpression node);
    public Object visit(ArithmeticExpression node);
    public Object visit(RelationalExpression node);
    public Object visit(AndExpression node);
    public Object visit(OrExpression node);
    public Object visit(TernaryExpression node);
    public Object visit(CompareExpression node);
    public Object visit(NoOpExpression node);
    public Object visit(SpreadExpression node);
    public Object visit(TypeExpression node);

    public Object visit(NullLiteral node);
    public Object visit(BooleanLiteral node);
    public Object visit(StringLiteral node);
    public Object visit(NumberLiteral node);
    public Object visit(ImportDirective node);
}
