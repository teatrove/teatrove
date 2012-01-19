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

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Token;
import java.lang.reflect.Method;

/**
 * An ArrayLookup can access indexed properties on objects. A Bean 
 * Introspector can be used to get the available indexed properties from
 * an object, however, the only ones supported by Tea are unnamed.
 * For this reason, the Introspector is not used. Any class with methods named
 * "get" that return something and have a single parameter (the type of which 
 * is not limited to ints) will support an array lookup.
 *
 * <p>Arrays, Collections and Strings are treated specially, and they all 
 * support array lookup on an int typed index.
 * 
 * @author Brian S O'Neill
 * @see org.teatrove.tea.util.BeanAnalyzer
 */
public class ArrayLookup extends Expression implements NullSafe {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private Token mToken;
    private Expression mLookupIndex;
    private Method mMethod;
    private boolean mNullSafe;
    
    public ArrayLookup(SourceInfo info, Expression expr, Token lookupToken,
                       Expression lookupIndex) {
        super(info);

        mExpr = expr;
        mToken = lookupToken;
        mLookupIndex = lookupIndex;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        ArrayLookup al = (ArrayLookup)super.clone();
        al.mExpr = (Expression)mExpr.clone();
        al.mLookupIndex = (Expression)mLookupIndex.clone();
        return al;
    }

    public boolean isExceptionPossible() {
        // ArrayIndexOutOfBoundsException is always a possibility.
        return true;
    }

    public Expression getExpression() {
        return mExpr;
    }

    public Token getLookupToken() {
        return mToken;
    }

    public Expression getLookupIndex() {
        return mLookupIndex;
    }

    /**
     * Returns the method to invoke in order to perform the lookup. This is
     * filled in by the type checker. If the expression type is an array, the
     * read method is null. A code generator must still be able to get
     * elements from an array.
     */
    public Method getReadMethod() {
        return mMethod;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }

    public void setLookupIndex(Expression lookupIndex) {
        mLookupIndex = lookupIndex;
    }

    public void setReadMethod(Method m) {
        mMethod = m;
    }
    
    public boolean isNullSafe() {
        return mNullSafe;
    }
    
    public void setNullSafe(boolean nullSafe) {
        mNullSafe = nullSafe;
    }
}
