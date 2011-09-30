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

import java.lang.reflect.Modifier;

/**
 * The Modifiers class is a wrapper around a Modifier bit mask. The
 * methods provided to manipulate the Modifier ensure that it is always
 * legal. i.e. setting it public automatically clears it from being
 * private or protected.
 * 
 * @author Brian S O'Neill, Nick Hagan
 */
public class Modifiers extends Modifier implements Cloneable {
    /**
     * When set public, the modifier is cleared from being private or
     * protected.
     */
    public static int setPublic(int modifier, boolean b) {
        if (b) {
            return (modifier | PUBLIC) & (~PROTECTED & ~PRIVATE);
        }
        else {
            return modifier & ~PUBLIC;
        }
    }
    
    /**
     * When set private, the modifier is cleared from being public or
     * protected.
     */
    public static int setPrivate(int modifier, boolean b) {
        if (b) {
            return (modifier | PRIVATE) & (~PUBLIC & ~PROTECTED);
        }
        else {
            return modifier & ~PRIVATE;
        }
    }

    /**
     * When set protected, the modifier is cleared from being public or
     * private.
     */
    public static int setProtected(int modifier, boolean b) {
        if (b) {
            return (modifier | PROTECTED) & (~PUBLIC & ~PRIVATE);
        }
        else {
            return modifier & ~PROTECTED;
        }
    }
    
    public static int setStatic(int modifier, boolean b) {
        if (b) {
            return modifier | STATIC;
        }
        else {
            return modifier & ~STATIC;
        }
    }

    /**
     * When set final, the modifier is cleared from being an interface or
     * abstract.
     */
    public static int setFinal(int modifier, boolean b) {
        if (b) {
            return (modifier | FINAL) & (~INTERFACE & ~ABSTRACT);
        }
        else {
            return modifier & ~FINAL;
        }
    }
    
    /**
     * When set synchronized, non-method settings are cleared.
     */
    public static int setSynchronized(int modifier, boolean b) {
        if (b) {
            return (modifier | SYNCHRONIZED) &
                (~VOLATILE & ~TRANSIENT & ~INTERFACE);
        }
        else {
            return modifier & ~SYNCHRONIZED;
        }
    }
    
    /**
     * When set volatile, non-field settings are cleared.
     */
    public static int setVolatile(int modifier, boolean b) {
        if (b) {
            return (modifier | VOLATILE) &
                (~SYNCHRONIZED & ~NATIVE & ~INTERFACE & ~ABSTRACT & ~STRICT);
        }
        else {
            return modifier & ~VOLATILE;
        }
    }
    
    /**
     * When set transient, non-field settings are cleared.
     */
    public static int setTransient(int modifier, boolean b) {
        if (b) {
            return (modifier | TRANSIENT) &
                (~SYNCHRONIZED & ~NATIVE & ~INTERFACE & ~ABSTRACT & ~STRICT);
        }
        else {
            return modifier & ~TRANSIENT;
        }
    }
    
    /**
     * When set native, non-native-method settings are cleared.
     */
    public static int setNative(int modifier, boolean b) {
        if (b) {
            return (modifier | NATIVE) & 
                (~VOLATILE & ~TRANSIENT & ~INTERFACE & ~ABSTRACT & ~STRICT);
        }
        else {
            return modifier & ~NATIVE;
        }
    }
    
    /**
     * When set as an interface, non-interface settings are cleared and the
     * modifier is set abstract.
     */
    public static int setInterface(int modifier, boolean b) {
        if (b) {
            return (modifier | (INTERFACE | ABSTRACT)) & 
                (~FINAL & ~SYNCHRONIZED & ~VOLATILE & ~TRANSIENT & ~NATIVE);
        }
        else {
            return modifier & ~INTERFACE;
        }
    }

    /**
     * When set abstract, the modifier is cleared from being final, volatile,
     * transient, native, synchronized, and strictfp. When cleared from being
     * abstract, the modifier is also cleared from being an interface.
     */
    public static int setAbstract(int modifier, boolean b) {
        if (b) {
            return (modifier | ABSTRACT) & 
                (~FINAL & ~VOLATILE & ~TRANSIENT & ~NATIVE &
                 ~SYNCHRONIZED & ~STRICT);
        }
        else {
            return modifier & ~ABSTRACT & ~INTERFACE;
        }
    }

    public static int setStrict(int modifier, boolean b) {
        if (b) {
            return modifier | STRICT;
        }
        else {
            return modifier & ~STRICT;
        }
    }
    
    public static int setBridge(int modifier, boolean b) {
        // NOTE: JDK 1.6 and less do not expose Modifier.BRIDGE as public
        // API...hower it shares its value with VOLATILE, so use that
        
        if (b) {
            return modifier | VOLATILE;
        }
        else {
            return modifier & ~VOLATILE;
        }
    }

    int mModifier;
    
    /** Construct with a modifier of 0. */
    public Modifiers() {
        mModifier = 0;
    }

    public Modifiers(int modifier) {
        mModifier = modifier;
    }
    
    public final int getModifier() {
        return mModifier;
    }
    
    public boolean isPublic() {
        return isPublic(mModifier);
    }

    public boolean isPrivate() {
        return isPrivate(mModifier);
    }

    public boolean isProtected() {
        return isProtected(mModifier);
    }
    
    public boolean isStatic() {
        return isStatic(mModifier);
    }

    public boolean isFinal() {
        return isFinal(mModifier);
    }

    public boolean isSynchronized() {
        return isSynchronized(mModifier);
    }

    public boolean isVolatile() {
        return isVolatile(mModifier);
    }

    public boolean isTransient() {
        return isTransient(mModifier);
    }
    
    public boolean isNative() {
        return isNative(mModifier);
    }
    
    public boolean isInterface() {
        return isInterface(mModifier);
    }
    
    public boolean isAbstract() {
        return isAbstract(mModifier);
    }

    public boolean isStrict() {
        return isStrict(mModifier);
    }

    public boolean isBridge() {
        // NOTE: JDK 1.6 and less do not expose isBridge yet, but BRIDGE shares
        // the same value as volatile so test for that
        return isVolatile(mModifier);
    }
    
    public void setPublic(boolean b) {
        mModifier = setPublic(mModifier, b);
    }
    
    public void setPrivate(boolean b) {
        mModifier = setPrivate(mModifier, b);
    }

    public void setProtected(boolean b) {
        mModifier = setProtected(mModifier, b);
    }

    public void setStatic(boolean b) {
        mModifier = setStatic(mModifier, b);
    }

    public void setFinal(boolean b) {
        mModifier = setFinal(mModifier, b);
    }

    public void setSynchronized(boolean b) {
        mModifier = setSynchronized(mModifier, b);
    }

    public void setVolatile(boolean b) {
        mModifier = setVolatile(mModifier, b);
    }

    public void setTransient(boolean b) {
        mModifier = setTransient(mModifier, b);
    }

    public void setNative(boolean b) {
        mModifier = setNative(mModifier, b);
    }

    public void setInterface(boolean b) {
        mModifier = setInterface(mModifier, b);
    }

    public void setAbstract(boolean b) {
        mModifier = setAbstract(mModifier, b);
    }

    public void setStrict(boolean b) {
        mModifier = setStrict(mModifier, b);
    }
    
    public void setBridge(boolean b) {
        mModifier = setBridge(mModifier, b);
    }

    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Returns the string value generated by the Modifier class.
     * @see java.lang.reflect.Modifier#toString()
     */
    public String toString() {
        return toString(mModifier);
    }
}
