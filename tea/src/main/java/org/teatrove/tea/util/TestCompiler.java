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

package org.teatrove.tea.util;

import java.io.File;
import java.io.PrintStream;

import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.runtime.DefaultContext;
import org.teatrove.tea.runtime.TemplateLoader;

/**
 * A compiler implementation suitable for testing from a command console.
 * The runtime context is a PrintStream so that template output can go to
 * standard out.
 *
 * <p>Templates are read from files that must have the extension ".tea". The
 * code generated are Java class files which are written in the same directory
 * as the source files. Compilation error messages are sent to standard out.
 * 
 * @author Brian S O'Neill
 */
public class TestCompiler extends Compiler {
    public static void main(String[] args) throws Exception {
        File dir = new File(".");
        TestCompiler tc = new TestCompiler(dir, null, dir);
        tc.addCompileListener(new ConsoleReporter(System.out));
        tc.setForceCompile(true);
        String[] names = tc.compile(args[0]);

        System.out.println("Compiled " + names.length + " sources");
        for (int i=0; i<names.length; i++){
            System.out.println(names[i]);
        }

        int errorCount = tc.getErrorCount();
        if (errorCount > 0) {
            String msg = String.valueOf(errorCount) + " error";
            if (errorCount != 1) {
                msg += 's';
            }
            System.out.println(msg);
            return;
        }

        int warningCount = tc.getWarningCount();
        if (warningCount > 0) {
            String msg = String.valueOf(warningCount) + " warning";
            if (warningCount != 1) {
                msg += 's';
            }
            System.out.println(msg);
            return;
        }
        
        TemplateLoader loader = new TemplateLoader();
        TemplateLoader.Template template = loader.getTemplate(args[0]);

        int length = args.length - 1;
        Object[] params = new Object[length];
        for (int i=0; i<length; i++) {
            params[i] = args[i + 1];
        }

        System.out.println("Executing " + template);

        template.execute(new Context(System.out), params);
    }

    public TestCompiler(File rootSourceDir, 
                        String rootPackage,
                        File rootDestDir) {

        super(rootPackage, rootDestDir);
        addCompilationProvider(new FileCompilationProvider(rootSourceDir));
    }

    public static class Context extends DefaultContext {
        private PrintStream mOut;

        public Context(PrintStream out) {
            super();
            mOut = out;
        }

        public void print(Object obj) {
            mOut.print(toString(obj));
        }
    }
}
