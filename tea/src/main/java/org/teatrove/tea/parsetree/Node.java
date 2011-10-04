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

/**
 * The superclass of all parse tree nodes. Every Node contains source
 * information and can accept a NodeVisitor.
 * 
 * @author Brian S O'Neill
 * @see NodeVisitor
 */
public abstract class Node implements Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private static final String cPackage;
    private static final int cPackageLength;

    static {
        String className = Node.class.getName();
        int index = className.lastIndexOf('.');
        if (index >= 0) {
            cPackage = className.substring(0, index + 1);
        }
        else {
            cPackage = "";
        }

        cPackageLength = cPackage.length();
    }

    private SourceInfo mInfo;

    protected Node(SourceInfo info) {
        mInfo = info;
    }

    public final SourceInfo getSourceInfo() {
        return mInfo;
    }

    /**
     * Every subclass of Node must override this method with the following:
     * <code>return visitor.visit(this)</code>.
     *
     * @param visitor A visitor of this Node
     * @return Node The Node returned by the visitor
     * @see NodeVisitor
     */
    public abstract Object accept(NodeVisitor visitor);

    /**
     * Returns a clone of this Node and all its children. Immutable child
     * objects are not necessarily cloned
     */
    public Object clone() {
        try {
            return (Node)super.clone();
        }
        catch (CloneNotSupportedException e) {
            // Should never happen, since all Nodes are Cloneable.
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Returns a String that contains the type of this Node and source
     * information.
     */
    public String toString() {
        String name = getClass().getName();
        int index = name.indexOf(cPackage);
        if (index >= 0) {
            name = name.substring(cPackageLength);
        }

        String identityCode =
            Integer.toHexString(System.identityHashCode(this));

        if (mInfo == null) {
            return name + '@' + identityCode;
        }
        else {
            return
                name +
                '(' +
                mInfo.getLine() + ',' + ' ' +
                mInfo.getStartPosition() + ',' + ' ' +
                mInfo.getEndPosition() +
                ')' + '@' + identityCode;
        }
    }
}
