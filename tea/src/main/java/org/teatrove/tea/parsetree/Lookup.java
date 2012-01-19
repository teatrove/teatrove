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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Token;
import org.teatrove.tea.compiler.Type;

/**
 * A Lookup can access properties on objects. A Bean Introspector is used to
 * get the available properties from an object. Arrays, Lists and Strings also
 * have a built-in property named "length". For arrays, the length field is
 * retrieved, for Lists, the size() method is called, and for Strings, the
 * length() method is called.
 *
 * @author Brian S O'Neill
 * @see java.beans.Introspector
 */
public class Lookup extends Expression implements NullSafe {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private Token mDot;
    private Name mLookupName;
    private Method mMethod;
    private Field mProperty;
    private boolean mNullSafe;
    
    public Lookup(SourceInfo info, Expression expr, Token dot,
                  Name lookupName) {
        super(info);

        mExpr = expr;
        mDot = dot;
        mLookupName = lookupName;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        Lookup lookup = (Lookup)super.clone();
        lookup.mExpr = (Expression)mExpr.clone();
        return lookup;
    }

    public boolean isExceptionPossible() {
        if (super.isExceptionPossible()) {
            return true;
        }

        if (mExpr != null) {
            if (mExpr.isExceptionPossible()) {
                return true;
            }
            
            if (!mNullSafe) {
                Type type = mExpr.getType();
                if (type != null && type.isNullable()) {
                    return true;
                }
            }
        }

        return mMethod != null;
    }

    public Expression getExpression() {
        return mExpr;
    }

    public Token getDot() {
        return mDot;
    }

    public Name getLookupName() {
        return mLookupName;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }

    /**
     * Returns the method to invoke in order to perform the lookup. This is
     * filled in by the type checker. If the lookup name is "length" and
     * the expression type is an array, the read method is null. A code
     * generator must still be able to get the length of the array.
     */
    public Method getReadMethod() {
        return mMethod;
    }

    public void setReadMethod(Method m) {
        mMethod = m;
    }

    /**
     * Returns the field to use in order to perform the lookup. This is filled
     * in by the type checker when a static constant is being referenced rather
     * than a variable lookup.
     */
    public Field getReadProperty() {
    	return mProperty;
    }
    
    public void setReadProperty(Field f) {
    	mProperty = f;
    }
    
    public boolean isNullSafe() {
        return mNullSafe;
    }
    
    public void setNullSafe(boolean nullSafe) {
        mNullSafe = nullSafe;
    }
}
