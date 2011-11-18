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
 * A TreeWalker traverses a parse tree in its canonical order. By overriding
 * a visit method, individual nodes can be captured and processed based on
 * their type. Call super.visit inside the overriden visit method to ensure
 * that the node's children are properly traversed.
 *
 * @author Brian S O'Neill
 */
public abstract class TreeWalker implements NodeVisitor {
    public Object visit(Template node) {
        node.getName().accept(this);

        Variable[] params = node.getParams();
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                params[i].accept(this);
            }
        }

        Statement stmt = node.getStatement();
        if (stmt != null) {
            stmt.accept(this);
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
        node.getTypeName().accept(this);
        return null;
    }
        
    public Object visit(ExpressionList node) {
        Expression[] exprs = node.getExpressions();
        for (int i=0; i<exprs.length; i++) {
            exprs[i].accept(this);
        }
        
        return null;
    }
        
    public Object visit(Statement node) {
        return null;
    }
        
    public Object visit(ImportDirective node) {
        return null;
    }
        
    public Object visit(StatementList node) {
        Statement[] stmts = node.getStatements();
        if (stmts != null) {
            for (int i=0; i<stmts.length; i++) {
                stmts[i].accept(this);
            }
        }
        
        return null;
    }
        
    public Object visit(Block node) {
        Statement init = node.getInitializer();
        if (init != null) {
            init.accept(this);
        }

        visit((StatementList)node);

        Statement fin = node.getFinalizer();
        if (fin != null) {
            fin.accept(this);
        }

        return null;
    }
        
    public Object visit(AssignmentStatement node) {
        node.getLValue().accept(this);
        node.getRValue().accept(this);
        
        return null;
    }
        
    public Object visit(BreakStatement node) {
        return null;
    }

    public Object visit(ContinueStatement node) {
        return null;
    }

    public Object visit(ForeachStatement node) {
        node.getLoopVariable().accept(this);
        node.getRange().accept(this);
        Expression endRange = node.getEndRange();
        if (endRange != null) {
            endRange.accept(this);
        }

        Statement init = node.getInitializer();
        if (init != null) {
            init.accept(this);
        }

        Block body = node.getBody();
        if (body != null) {
            body.accept(this);
        }
        
        return null;
    }
        
    public Object visit(IfStatement node) {
        node.getCondition().accept(this);
            
        Block block = node.getThenPart();
        if (block != null) {
            block.accept(this);
        }
        
        block = node.getElsePart();
        if (block != null) {
            block.accept(this);
        }
        
        return null;
    }

    public Object visit(SubstitutionStatement node) {
        return null;
    }

    public Object visit(ExpressionStatement node) {
        node.getExpression().accept(this);
        return null;
    }

    public Object visit(ReturnStatement node) {
        Expression expr = node.getExpression();
        if (expr != null) {
            expr.accept(this);
        }
        return null;
    }

    public Object visit(ExceptionGuardStatement node) {
        Statement stmt = node.getGuarded();
        if (stmt != null) {
            stmt.accept(this);
        }
        stmt = node.getReplacement();
        if (stmt != null) {
            stmt.accept(this);
        }
        return null;
    }

    public Object visit(Expression node) {
        return null;
    }

    public Object visit(ParenExpression node) {
        node.getExpression().accept(this);
        return null;
    }

    public Object visit(NewArrayExpression node) {
        node.getExpressionList().accept(this);
        return null;
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
            expr.accept(this);
        }
        
        node.getParams().accept(this);
        
        Statement init = node.getInitializer();
        if (init != null) {
            init.accept(this);
        }

        Block subParam = node.getSubstitutionParam();
        if (subParam != null) {
            subParam.accept(this);
        }
        
        return null;
    }

    public Object visit(VariableRef node) {
        Variable v = node.getVariable();
        if (v != null) {
            v.accept(this);
        }

        return null;
    }

    public Object visit(Lookup node) {
        node.getExpression().accept(this);
        return null;
    }

    public Object visit(ArrayLookup node) {
        node.getExpression().accept(this);
        node.getLookupIndex().accept(this);
        return null;
    }

    public Object visit(NegateExpression node) {
        node.getExpression().accept(this);
        return null;
    }

    public Object visit(NotExpression node) {
        node.getExpression().accept(this);
        return null;
    }

    private Object visit(BinaryExpression node) {
        node.getLeftExpression().accept(this);
        node.getRightExpression().accept(this);
        return null;
    }

    public Object visit(ConcatenateExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(ArithmeticExpression node) {
        return visit((BinaryExpression)node);
    }

    public Object visit(RelationalExpression node) {
        if (node.getIsaTypeName() != null) {
            node.getLeftExpression().accept(this);
            node.getIsaTypeName().accept(this);
            return null;
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
        node.getCondition().accept(this);

        Expression block = node.getThenPart();
        if (block != null) {
            block.accept(this);
        }

        block = node.getElsePart();
        if (block != null) {
            block.accept(this);
        }

        return null;
    }

    public Object visit(CompareExpression node) {
        node.getLeftExpression().accept(this);
        node.getRightExpression().accept(this);
        
        return null;
    }
    
    public Object visit(NoOpExpression node) {
        return null;
    }
    
    public Object visit(TypeExpression node) {
        node.getTypeName().accept(this);
        return null;
    }
    
    public Object visit(SpreadExpression node) {
        node.getExpression().accept(this);
        node.getOperation().accept(this);
        
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
