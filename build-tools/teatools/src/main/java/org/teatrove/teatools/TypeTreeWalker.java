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

package org.teatrove.teatools;

import org.teatrove.tea.compiler.Type;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.parsetree.*;

/**
 * TreeWalker for type discovery.
 *
 * @author Mark Masse
 */
public class TypeTreeWalker extends TreeWalker {
    
    /** The type of the expression to the left of the "dot" */
    private Class mType;
    
    /** The position (within the source text) of the type to look for */
    private int mPosition;
    
    /**
     * Creates a new TypeTreeWalker to look for the type at the
     * specified position.
     */
    public TypeTreeWalker(int position) {
        mPosition = position;    
    }
    
    /**
     * Returns the position (within the source text) of the type.
     */
    public int getPosition() {
        return mPosition;
    }
   
    /**
     * Returns the type (as a Class object) of the type.
     * Or, if the TypeTreeWalker did not find type, then this method will 
     * return null.
     */
    public Class getType() {
        return mType;
    }

    // Overridden method
    public Object visit(Expression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(ParenExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(NewArrayExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(FunctionCallExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(TemplateCallExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(VariableRef node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(Lookup node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(ArrayLookup node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(NegateExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(NotExpression node) {
        determineType(node);
        return super.visit(node);
    }


    // Overridden method
    public Object visit(ConcatenateExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(ArithmeticExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(RelationalExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(AndExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(OrExpression node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(NullLiteral node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(BooleanLiteral node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(StringLiteral node) {
        determineType(node);
        return super.visit(node);
    }

    // Overridden method
    public Object visit(NumberLiteral node) {
        determineType(node);
        return super.visit(node);
    }



    protected void determineType(Expression node) {
        
        SourceInfo info = node.getSourceInfo();
        
        if (isBoundedBy(info)) {
            
            Type type = node.getType();                
            if (type != null) {
                // Save the type class
                Class clazz = type.getObjectClass();
                if (clazz != null) {
                    mType = clazz;
                }
            }                            
        }
    }

    protected boolean isBoundedBy(SourceInfo info) {
        return (info != null && 
                info.getStartPosition() <= mPosition &&
                mPosition <= info.getEndPosition());        
    }


}
