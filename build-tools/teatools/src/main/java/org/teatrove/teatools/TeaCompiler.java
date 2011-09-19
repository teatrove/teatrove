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

import org.teatrove.tea.compiler.*;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.util.*;
import org.teatrove.trove.io.SourceReader;

import java.io.*;
import java.util.*;

/**
 * Simple extension of the Tea FileCompiler that allows a context class to
 * be specified by name.  In addition the TeaCompiler acts as a factory for
 * producing various Tea compiler components.
 *
 * @author Mark Masse
 * @version
 * <!--$$Revision:--> 6 <!-- $-->, <!--$$JustDate:-->  2/06/01 <!-- $-->
 */
public class TeaCompiler extends FileCompiler {


    /**
     * Finds the name of the template using the specified tea scanner.
     *
     * @param scanner the Tea scanner.
     *
     * @return the name of the template
     */
    public static String findTemplateName(org.teatrove.tea.compiler.Scanner scanner) {
        // System.out.println("<-- findTemplateName -->");
        Token token;
        int id;
        String name = null;
        try {
            while ((id = (token = scanner.readToken()).getID()) != Token.EOF) {
                /*
                System.out.println("Token [Code: " +
                                   token.getCode() + "] [Image: " +
                                   token.getImage() + "] [Value: " +
                                   token.getStringValue() + "] [Id: " +
                                   token.getID() + "]");
                */
                if (token.getID() == Token.TEMPLATE) {
                    token = scanner.readToken();
                    if (token.getID() == Token.IDENT) {
                        name = token.getStringValue();
                    }
                    break;
                }
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return name;
    }

    /** The Runtime Context Class */
    protected Class mRuntimeContextClass;

    /** The ErrorListener for Scanner errors.  These
        errors are in the category of syntax errors. */
    protected ErrorListener mScannerErrorListener;

    /** The ErrorListener for Parser errors.  These
        errors are in the category of syntax errors. */
    protected ErrorListener mParserErrorListener;

    /** The ErrorListener for TypeChecker
        errors.  These errors are in the category of semantic errors.  A
        common cause of a semantic error is a type misuse. */
    protected ErrorListener mTypeCheckerErrorListener;


    /**
     * Creates a new TeaCompiler
     *
     * @param rootSourceDirs Required root source directories
     * @param parseTreeMap Optional map should be thread-safe
     */
    public TeaCompiler(File[] rootSourceDirs, Map parseTreeMap) {
        super(rootSourceDirs, null, null, null, null, parseTreeMap);

        setForceCompile(true);
        setCodeGenerationEnabled(false);
    }


    /**
     * Sets the class that defines a template's runtime context.
     *
     * @param className the name of the class that defines a
     * template's runtime context.
     */
    public void setRuntimeContextClass(String className)
    throws ClassNotFoundException {
        setRuntimeContextClass(loadClass(className));
    }

    /**
     * Sets the class that defines a template's runtime context.
     *
     * @param clazz the class that defines a template's runtime context.
     */
    public void setRuntimeContextClass(Class clazz) {
        mRuntimeContextClass = clazz;
    }

    /**
     * Gets the class that defines a template's runtime context.
     *
     * @return the class that defines a template's runtime context.
     */
    public Class getRuntimeContext() {
        return mRuntimeContextClass;
    }


    public void setScannerErrorListener(ErrorListener listener) {
        mScannerErrorListener = listener;
    }

    public ErrorListener getScannerErrorListener() {
        return mScannerErrorListener;
    }

    public void setParserErrorListener(ErrorListener listener) {
        mParserErrorListener = listener;
    }

    public ErrorListener getParserErrorListener() {
        return mParserErrorListener;
    }

    public void setTypeCheckerErrorListener(ErrorListener listener) {
        mTypeCheckerErrorListener = listener;
    }

    public ErrorListener getTypeCheckerErrorListener() {
        return mTypeCheckerErrorListener;
    }

    // Overridden to make public
    public CompilationUnit createCompilationUnit(String name) {
        return super.createCompilationUnit(name);
    }

    // Overridden to make public
    public SourceReader createSourceReader(CompilationUnit unit)
    throws IOException {
        return super.createSourceReader(unit);
    }

    // Overridden to make public
    public org.teatrove.tea.compiler.Scanner createScanner(SourceReader sr, CompilationUnit unit)
    throws IOException {
        org.teatrove.tea.compiler.Scanner scanner = super.createScanner(sr, unit);
        ErrorListener scannerErrorListener = getScannerErrorListener();
        if (scannerErrorListener != null) {
            scanner.addErrorListener(scannerErrorListener);
        }
        return scanner;
    }

    // Overridden to make public
    public Parser createParser(org.teatrove.tea.compiler.Scanner scanner, CompilationUnit unit)
    throws IOException {

        Parser parser = super.createParser(scanner, unit);
        ErrorListener parserErrorListener = getParserErrorListener();
        if (parserErrorListener != null) {
            parser.addErrorListener(parserErrorListener);
        }

        return parser;
    }

    // Overridden to make public
    public TypeChecker createTypeChecker(CompilationUnit unit) {

        TypeChecker typeChecker = super.createTypeChecker(unit);
        ErrorListener typeCheckerErrorListener = getTypeCheckerErrorListener();
        if (typeCheckerErrorListener != null) {
            typeChecker.addErrorListener(typeCheckerErrorListener);
        }

        return typeChecker;
    }


    /**
     * Perform Tea "parsing."  This method will compile the named template.
     *
     * @param templateName the name of the template to parse
     *
     * @return a Template object that represents the root node of the Tea
     * parse tree.
     */
    public Template parseTeaTemplate(String templateName) throws IOException {

        if (templateName == null) {
            return null;
        }

        preserveParseTree(templateName);
        compile(templateName);
        CompilationUnit unit = getCompilationUnit(templateName, null);
        if (unit == null) {
            return null;
        }

        return unit.getParseTree();
    }


}

