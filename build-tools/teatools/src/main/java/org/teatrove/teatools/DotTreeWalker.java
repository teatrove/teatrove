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
import org.teatrove.tea.compiler.Token;
import org.teatrove.tea.parsetree.Node;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.Lookup;
import org.teatrove.tea.parsetree.TreeWalker;

/**
 * A TreeWalker that visits "Lookup" nodes looking for a specific 
 * "dot" (lookup expression).  
 * <p>
 * A lookup expression looks like this:
 * <pre>
 *     foo.bar
 *     |  |
 *     |  +---- The lookup ("dot")
 *     +------- The expression
 * </pre>
 * The DotTreeWalker can be used to determine the type of the expression to 
 * the left of the "dot."  For example:
 * <pre>
 *     DotTreeWalker dotTreeWalker = new DotTreeWalker(dotPosition);
 *     template.accept(dotTreeWalker);
 *     Class expressionType = dotTreeWalker.getType();                
 * </pre>
 * 
 * @author Mark Masse
 */
public class DotTreeWalker extends TreeWalker {
    
	/** The "dot" character */
    public final static char DOT_CHAR = '.';        


    /** The type of the expression to the left of the "dot" */
    private Class mType;
    
    /** The position (within the source text) of the "dot" to look for */
    private int mDotPosition;
    
    /**
     * Creates a new DotTreeWalker to look for the lookup expression at the
     * specified position.
     */
    public DotTreeWalker(int dotPosition) {
        mDotPosition = dotPosition;    
    }
    
    // Overridden method
    public Object visit(Lookup node) {
        
        Token dotToken = node.getDot();
        SourceInfo info = dotToken.getSourceInfo();
        
        if (info != null && info.getStartPosition() == mDotPosition) {
			
            // Found the matching position, so this node represents the
            // lookup expression that we are looking for.
            
            Expression expression = node.getExpression();
            
            Type type = expression.getType();                
            if (type != null) {
                // Save the type class
                mType = type.getObjectClass();                    
            }                
            
            return null;
        }
        
        return super.visit(node);
    }

    /**
     * Returns the position (within the source text) of the lookup 
	 * expression ("dot").
     */
    public int getDotPosition() {
        return mDotPosition;
    }

    
    /**
     * Returns the type (as a Class object) of the left side of the 
     * lookup expression ("dot").  Or, if the DotTreeWalker did not find
     * a matching lookup expression, then this method will return null.
     */
    public Class getType() {
        return mType;
    }
}
