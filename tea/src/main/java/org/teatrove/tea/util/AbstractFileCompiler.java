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



import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.parsetree.Template;

/**
 *
 * @author Brian S O'Neill
 */
public abstract class AbstractFileCompiler extends Compiler {
    protected AbstractFileCompiler() {
        super();
    }

    protected AbstractFileCompiler(Map<String, Template> parseTreeMap) {
        super(parseTreeMap);
    }

    /**
     * Recursively compiles all files in the source directory.
     *
     * @return The names of all the compiled sources
     */
    public String[] compileAll() throws IOException {
        return compile(getAllTemplateNames());
    }

    /**
     * Returns all sources (template names) available from the source
     * directory and in all sub-directories.
     */
    public abstract String[] getAllTemplateNames() throws IOException;


    /**
     * Overrides Compiler class implementation (TemplateRepository integration).
     */
    public String[] compile(String[] names) throws IOException {

        if (!TemplateRepository.isInitialized())
            return super.compile(names);
        String[] compNames = super.compile(names);
        ArrayList<String> compList =
            new ArrayList<String>(Arrays.asList(compNames));

        TemplateRepository rep = TemplateRepository.getInstance();
        String[] callers = rep.getCallersNeedingRecompile(compNames, this);
        if (callers.length > 0)
            compList.addAll(Arrays.asList(super.compile(callers)));
        String[] compiled = compList.toArray(new String[compList.size()]);

        // JoshY - There's a VM bug in JVM 1.4.2 that can cause the repository update to
        // throw a NullPointerException when it shouldn't.  There's a workaround in place
        // and we also put a catch here, to allow the TeaServlet init to finish just in case
        try {
            rep.update(compiled);
        } catch (Exception e) {
            System.err.println("Unable to update repository");
            e.printStackTrace(System.err);
        }
        return compiled;
    }

}
