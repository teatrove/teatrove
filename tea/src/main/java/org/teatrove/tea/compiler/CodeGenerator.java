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

package org.teatrove.tea.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import org.teatrove.tea.parsetree.Node;
import org.teatrove.tea.parsetree.Template;

/**
 *
 * @author Brian S O'Neill
 */
public abstract class CodeGenerator {
    protected Template mTree;
    protected Vector<CompileListener> mListeners = new Vector<CompileListener>(1);
    
    
    public CodeGenerator(Template tree) {
        mTree = tree;
    }

    public void addCompileListener(CompileListener listener) {
        mListeners.addElement(listener);
    }

    public void removeCompileListener(CompileListener listener) {
        mListeners.removeElement(listener);
    }
    
    public Template getParseTree() {
        return mTree;
    }

    public abstract void writeTo(OutputStream out) throws IOException;
    
    protected void dispatchCompileError(CompileEvent e) {
        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                mListeners.elementAt(i).compileError(e);
            }
        }
    }
    
    protected void dispatchCompileWarning(CompileEvent e) {
        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                mListeners.elementAt(i).compileWarning(e);
            }
        }
    }

}
