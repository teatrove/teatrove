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

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;

/**
 * This CompilationUnit is used in conjunction with the TextCompiler
 *
 * @author Mark Masse
 */
public class TextCompilationUnit extends CompilationUnit {
    
    /** The section of text to "compile" */
    private char[] mText;
    
    /**
     * Creates a new CompilationUnit with the specified name, compiler, 
     * and text.
     */
    public TextCompilationUnit(String name, Compiler compiler, char[] text) {
        
        super(name, null, compiler);
        
        mText = text;
    }

    /**
     * Returns the text
     */
    public char[] getText() {
        return mText;
    }

    /**
     * Returns the name of this TextCompilationUnit
     */
    public String getSourceFileName() {
        return getName();
    }
    
    /**
     * Returns a Reader for the text
     */
    @Override
    public Reader getReader() throws IOException {
        return new CharArrayReader(getText());                    
    }
    
    /**
     * Always returns null
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        // No code generation needed
        return null;
    }
    
    @Override
    public void resetOutputStream() {}
}
