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

import org.teatrove.tea.compiler.Scanner;

import org.teatrove.tea.compiler.CompilationUnit;

import org.teatrove.trove.io.SourceReader;

import java.io.*;

/**
 * This Tea compiler is used to perform limited compilation on a section
 * of text.
 *
 * @author Mark Masse
 */
public class TextCompiler extends TeaCompiler implements TeaToolsConstants {

    /**
     * A utility method that creates a Tea SourceReader for the specified text.
     * The text should contain Tea code.
     * <p>
     * This method can be used to create a SourceReader for a section of text.
     * 
     * @param text the Text to read
     * 
     * @return a SourceReader for the specified text
     */
    public static SourceReader createTextSourceReader(char[] text) {

        Reader r = new BufferedReader(new CharArrayReader(text));
        return new SourceReader(r, BEGIN_CODE_TAG, END_CODE_TAG);
    }
    
    public static SourceReader createTextSourceReader(String str) {

        Reader r = new BufferedReader(new StringReader(str));
        return new SourceReader(r, BEGIN_CODE_TAG, END_CODE_TAG);
    }

    /**
     * Creates a Tea scanner for the specified text.  The text
     * should contain Tea code.
     * <p>
     * This method can be used to create a Scanner for a section of text.
     * 
     * @param text the text to scan
     * 
     * @return a Scanner for the specified text
     */
    public static Scanner createTextScanner(char[] text) {
        return new Scanner(createTextSourceReader(text));        
    }    
    
    public static Scanner createTextScanner(String str) {
        return new Scanner(createTextSourceReader(str));        
    }    
    
    /** The name of the template being compiled */
    private String mTemplateName;
    
    /** The text to "compile" */
    private char[] mText;
                
    /**
     * Creates a new TextCompiler for the specified text
     *
     * @param rootSourceDir required root source directory for other Tea files.
     * This is needed to allow for the text "template" to call other templates.
     * @param templateName the name of the template associated with the text
     * @param text the text (Tea source) to compile
     */
    public TextCompiler(File rootSourceDir, String templateName, char[] text) {
        super(new File[] { rootSourceDir }, null);

        mTemplateName = templateName;
        mText = text;

        setForceCompile(false);
    }

    /**
     * Gets the template name associated with the text
     */
    public String getTemplateName() {
        return mTemplateName;
    }

    /**
     * Gets the template text
     */
    public char[] getText() {
        return mText;
    }

    // Overridden method
    public boolean sourceExists(String name) {

        if (name != null && name.equals(getTemplateName())) {
            return true;
        }

        // Look for a .tea file
        return super.sourceExists(name);
    }

    // Overridden method
    public CompilationUnit createCompilationUnit(String name) {

        if (name != null && name.equals(getTemplateName())) {
            return new TextCompilationUnit(name, this, getText());
        }
        
        // Create a compilation unit for a .tea file
        return super.createCompilationUnit(name);
    }
}
