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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.CompileListener;
import org.teatrove.tea.compiler.Parser;
import org.teatrove.tea.compiler.Token;
import org.teatrove.tea.compiler.TypeChecker;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.util.FileCompilationProvider;
import org.teatrove.trove.io.SourceReader;
import org.teatrove.trove.util.ClassInjector;

/**
 * Simple extension of the Tea FileCompiler that allows a context class to
 * be specified by name.  In addition the TeaCompiler acts as a factory for
 * producing various Tea compiler components.
 *
 * @author Mark Masse
 */
public class TeaCompiler extends Compiler {


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
        String name = null;
        try {
            while (((token = scanner.readToken()).getID()) != Token.EOF) {
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
    protected Class<?> mRuntimeContextClass;

    /** The CompileListener for Scanner errors.  These
        errors are in the category of syntax errors. */
    protected CompileListener mScannerListener;

    /** The CompileListener for Parser errors.  These
        errors are in the category of syntax errors. */
    protected CompileListener mParserListener;

    /** The CompileListener for TypeChecker
        issues.  These issues are in the category of semantic errors or 
        warnings.  A common cause of a semantic error is a type misuse. */
    protected CompileListener mTypeCheckerListener;


    /**
     * Creates a new TeaCompiler
     *
     * @param rootSourceDirs Required root source directories
     * @param parseTreeMap Optional map should be thread-safe
     */
    public TeaCompiler(File[] rootSourceDirs, 
                       Map<String, Template> parseTreeMap) {
        super(ClassInjector.getInstance(), null, null, null, 0, parseTreeMap);

        setForceCompile(true);
        setCodeGenerationEnabled(false);
        for (File rootSourceDir : rootSourceDirs) {
            addCompilationProvider(new FileCompilationProvider(rootSourceDir));
        }
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
    public void setRuntimeContextClass(Class<?> clazz) {
        mRuntimeContextClass = clazz;
    }

    /**
     * Gets the class that defines a template's runtime context.
     *
     * @return the class that defines a template's runtime context.
     */
    public Class<?> getRuntimeContext() {
        return mRuntimeContextClass;
    }


    public void setScannerListener(CompileListener listener) {
        mScannerListener = listener;
    }

    public CompileListener getScannerListener() {
        return mScannerListener;
    }

    public void setParserListener(CompileListener listener) {
        mParserListener = listener;
    }

    public CompileListener getParserListener() {
        return mParserListener;
    }

    public void setTypeCheckerListener(CompileListener listener) {
        mTypeCheckerListener = listener;
    }

    public CompileListener getTypeCheckerListener() {
        return mTypeCheckerListener;
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
        CompileListener scannerListener = getScannerListener();
        if (scannerListener != null) {
            scanner.addCompileListener(scannerListener);
        }
        return scanner;
    }

    // Overridden to make public
    public Parser createParser(org.teatrove.tea.compiler.Scanner scanner, CompilationUnit unit)
    throws IOException {

        Parser parser = super.createParser(scanner, unit);
        CompileListener parserListener = getParserListener();
        if (parserListener != null) {
            parser.addCompileListener(parserListener);
        }

        return parser;
    }

    // Overridden to make public
    public TypeChecker createTypeChecker(CompilationUnit unit) {

        TypeChecker typeChecker = super.createTypeChecker(unit);
        CompileListener typeCheckerListener = getTypeCheckerListener();
        if (typeCheckerListener != null) {
            typeChecker.addCompileListener(typeCheckerListener);
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

