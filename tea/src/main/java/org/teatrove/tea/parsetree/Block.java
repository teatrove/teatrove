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
 * A Block is a StatementList that most likely appeared in the source file
 * delimited by braces. A Block may contain initializer statements, inserted by
 * a type checker, to be executed at the beginning of the boock. A Block may
 * also contain a finalizer, inserted by a type checker, to be executed at the
 * end of the Block.
 *
 * @author Brian S O'Neill
 */
public class Block extends StatementList {
    private static final long serialVersionUID = 1L;

    private Statement mInitializer;
    private Statement mFinalizer;

    public Block(SourceInfo info, Statement[] statements) {
        super(info, statements);
    }

    public Block(SourceInfo info) {
        super(info, new Statement[0]);
    }

    public Block(Statement stmt) {
        super(stmt.getSourceInfo(), new Statement[] {stmt});
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        Block b = (Block)super.clone();
        if (mInitializer != null) {
            b.mInitializer = (Statement)mInitializer.clone();
        }
        if (mFinalizer != null) {
            b.mFinalizer = (Statement)mFinalizer.clone();
        }
        return b;
    }

    /**
     * Initializer is executed at the beginning of the block and may be defined
     * by a type checker in order to correctly manage variable types.
     */
    public Statement getInitializer() {
        return mInitializer;
    }

    /**
     * Finalizer is executed at the end of the block and may be defined by a
     * type checker in order to correctly manage variable types.
     */
    public Statement getFinalizer() {
        return mFinalizer;
    }

    public void setInitializer(Statement initializer) {
        mInitializer = initializer;
    }

    public void setFinalizer(Statement finalizer) {
        mFinalizer = finalizer;
    }
}
