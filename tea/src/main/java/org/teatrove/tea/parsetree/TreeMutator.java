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

import java.util.Iterator;

import org.teatrove.tea.compiler.Type;

/**
 * TreeMutator is similar to {@link TreeWalker TreeWalker} in that it
 * traverses a parse tree in canonocal order, and only a few visit methods
 * should be overridden. The key difference is that visit methods must
 * return a node of the same type as the one passed in. By returning a node
 * which isn't the same as the one passed in, a node can be replaced.
 * 
 * @author Brian S O'Neill
 */
public abstract class TreeMutator implements NodeVisitor {
    public Object visit(Template node) {
        node.getName().accept(this);

        Variable[] params = node.getParams();
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                params[i] = (Variable)params[i].accept(this);
            }
        }

        Statement stmt = node.getStatement();
        if (stmt != null) {
            node.setStatement((Statement)stmt.accept(this));
        }
        
        return node;
    }
        
    public Object visit(Name node) {
        return node;
    }
        
    public Object visit(TypeName node) {
        return node;
    }

    public Object visit(Variable node) {
        node.getTypeName().accept(this);
        return node;
    }
        
    public Object visit(ExpressionList node) {
        Expression[] exprs = node.getExpressions();
        for (int i=0; i<exprs.length; i++) {
            exprs[i] = visitExpression(exprs[i]);
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
        Statement[] stmts = node.getStatements();
        if (stmts != null) {
            for (int i=0; i<stmts.length; i++) {
                stmts[i] = (Statement)stmts[i].accept(this);
            }
        }
        
        return node;
    }
        
    public Object visit(Block node) {
        Statement init = node.getInitializer();
        if (init != null) {
            node.setInitializer((Statement)init.accept(this));
        }

        visit((StatementList)node);

        Statement fin = node.getFinalizer();
        if (fin != null) {
            node.setFinalizer((Statement)fin.accept(this));
        }

        return node;
    }
        
    public Object visit(AssignmentStatement node) {
        node.getLValue().accept(this);
        node.setRValue(visitExpression(node.getRValue()));
        
        return node;
    }
        
    public Object visit(BreakStatement node) {
        return node;
    }

    public Object visit(ContinueStatement node) {
        return node;
    }

    public Object visit(ForeachStatement node) {
        node.getLoopVariable().accept(this);
        node.setRange(visitExpression(node.getRange()));
        Expression endRange = node.getEndRange();
        if (endRange != null) {
            node.setEndRange(visitExpression(endRange));
        }

        Statement init = node.getInitializer();
        if (init != null) {
            node.setInitializer((Statement)init.accept(this));
        }

        Block body = node.getBody();
        if (body != null) {
            node.setBody(visitBlock(body));
        }
        
        return node;
    }
        
    public Object visit(IfStatement node) {
        node.setCondition(visitExpression(node.getCondition()));

        Block block = node.getThenPart();
        if (block != null) {
            node.setThenPart(visitBlock(block));
        }
        
        block = node.getElsePart();
        if (block != null) {
            node.setElsePart(visitBlock(block));
        }
        
        return node;
    }

    public Object visit(SubstitutionStatement node) {
        return node;
    }

    public Object visit(ExpressionStatement node) {
        node.setExpression(visitExpression(node.getExpression()));
        return node;
    }

    public Object visit(ReturnStatement node) {
        Expression expr = node.getExpression();
        if (expr != null) {
            node.setExpression(visitExpression(node.getExpression()));
        }
        return node;
    }

    public Object visit(ExceptionGuardStatement node) {
        node.setGuarded((Statement)node.getGuarded().accept(this));
        Statement stmt = node.getReplacement();
        if (stmt != null) {
            node.setReplacement((Statement)stmt.accept(this));
        }
        return node;
    }

    public Object visit(Expression node) {
        return node;
    }

    public Object visit(ParenExpression node) {
        node.setExpression(visitExpression(node.getExpression()));
        return node;
    }

    public Object visit(NewArrayExpression node) {
        node.setExpressionList
            ((ExpressionList)node.getExpressionList().accept(this));
        return node;
    }

    public Object visit(FunctionCallExpression node) {
        return visit((CallExpression)node);
    }

    public Object visit(TemplateCallExpression node) {
        return visit((CallExpression)node);
    }

    private Object visit(CallExpression node) {
        Expression expr = node.getExpression();
        if (expr != null) {
            node.setExpression((Expression)expr.accept(this));
        }
        
        node.setParams((ExpressionList)node.getParams().accept(this));
        
        Statement init = node.getInitializer();
        if (init != null) {
            node.setInitializer((Statement)init.accept(this));
        }

        Block subParam = node.getSubstitutionParam();
        if (subParam != null) {
            node.setSubstitutionParam(visitBlock(subParam));
        }
        
        return node;
    }

    public Object visit(VariableRef node) {
        Variable v = node.getVariable();
        if (v != null) {
            node.setVariable((Variable)v.accept(this));
        }

        return node;
    }

    public Object visit(Lookup node) {
        node.setExpression(visitExpression(node.getExpression()));
        return node;
    }

    public Object visit(ArrayLookup node) {
        node.setExpression(visitExpression(node.getExpression()));
        node.setLookupIndex(visitExpression(node.getLookupIndex()));
        return node;
    }

    public Object visit(NegateExpression node) {
        node.setExpression(visitExpression(node.getExpression()));
        return node;
    }

    public Object visit(NotExpression node) {
        node.setExpression(visitExpression(node.getExpression()));
        return node;
    }

    private Object visit(BinaryExpression node) {
        node.setLeftExpression(visitExpression(node.getLeftExpression()));
        node.setRightExpression(visitExpression(node.getRightExpression()));
        return node;
    }

    public Object visit(ConcatenateExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(ArithmeticExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(RelationalExpression node) {
        if (node.getIsaTypeName() != null) {
            node.setLeftExpression(visitExpression(node.getLeftExpression()));
            node.getIsaTypeName().accept(this);
            return node;
        }
        else {
            return visit((BinaryExpression)node);
        }
    }

    public Object visit(AndExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(OrExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(TernaryExpression node) {
        node.setCondition(visitExpression(node.getCondition()));

        Expression block = node.getThenPart();
        if (block != null) {
            node.setThenPart(visitExpression(block));
        }

        block = node.getElsePart();
        if (block != null) {
            node.setElsePart(visitExpression(block));
        }

        return node;
    }
    
    public Object visit(CompareExpression node) {
        node.setLeftExpression((Expression) node.getLeftExpression().accept(this));
        node.setRightExpression((Expression) node.getRightExpression().accept(this));
        
        return node;
    }

    public Object visit(NoOpExpression node) {
        return node;
    }
    
    public Object visit(SpreadExpression node) {
        node.setExpression((Expression) node.getExpression().accept(this));
        node.setOperation((Expression) node.getOperation().accept(this));
        
        return node;
    }
    
    public Object visit(TypeExpression node) {
        node.setTypeName((TypeName) node.getTypeName().accept(this));
        return node;
    }
    
    public Object visit(NullLiteral node) {
        return node;
    }

    public Object visit(BooleanLiteral node) {
        return node;
    }

    public Object visit(StringLiteral node) {
        return node;
    }

    public Object visit(NumberLiteral node) {
        return node;
    }

    /**
     * All expressions pass through this method to ensure the expression's
     * type is preserved.
     */
    protected Expression visitExpression(Expression expr) {
        if (expr == null) {
            return null;
        }

        Expression newExpr = (Expression)expr.accept(this);
        if (expr != newExpr) {
            Type newType = newExpr.getType();

            if (newType == null || !newType.equals(expr.getType())) {
                Iterator<Expression.Conversion> it =
                    expr.getConversionChain().iterator();
                while (it.hasNext()) {
                    Expression.Conversion conv = it.next();
                    newExpr.convertTo
                        (conv.getToType(), conv.isCastPreferred());
                }
            }
        }
        return newExpr;
    }

    /**
     * Visit a Block to ensure that new Statement is a Block.
     */
    protected Block visitBlock(Block block) {
        if (block == null) {
            return null;
        }

        Statement stmt = (Statement)block.accept(this);

        if (stmt instanceof Block) {
            return (Block)stmt;
        }
        else if (stmt != null) {
            return new Block(stmt);
        }
        else {
            return new Block(block.getSourceInfo());
        }
    }

    // TODO: create a visitNode for all nodes to ensure scope is preserved?
}
