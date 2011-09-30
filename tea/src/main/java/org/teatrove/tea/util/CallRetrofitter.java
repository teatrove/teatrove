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

import java.io.*;
import java.util.*;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.*;
import org.teatrove.tea.runtime.*;

/**
 * Command-line tool that puts the 'call' keyword in front of template calls
 * in templates compatable with pre 3.x.x versions of Tea.
 *
 * @author Brian S O'Neill
 */
public class CallRetrofitter {
    /**
     * Entry point for a command-line tool that puts the 'call' keyword in
     * front of template calls in templates compatable with pre 3.x.x versions
     * of Tea.
     *
     * <pre>
     * Usage: java org.teatrove.tea.util.CallRetrofitter {options} 
     * &lt;template root directory&gt; {templates}
     *
     * where {options} includes:
     * -context &lt;class&gt;     Specify a runtime context class to compile against.
     * -encoding &lt;encoding&gt; Specify character encoding used by source files.
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            usage();
            return;
        }

        Class context = null;
        String encoding = null;
        File rootDir = null;
        Collection templates = new ArrayList(args.length);

        try {
            boolean parsingOptions = true;
            for (int i=0; i<args.length;) {
                String arg = args[i++];
                if (arg.startsWith("-") && parsingOptions) {
                    if (arg.equals("-context") && context == null) {
                        context = Class.forName(args[i++]);
                        continue;
                    }
                    else if (arg.equals("-encoding") && encoding == null) {
                        encoding = args[i++];
                        continue;
                    }
                }
                else {
                    if (parsingOptions) {
                        parsingOptions = false;
                        rootDir = new File(arg);
                        continue;
                    }

                    arg = arg.replace('/', '.');
                    arg = arg.replace(File.separatorChar, '.');
                    while (arg.startsWith(".")) {
                        arg = arg.substring(1);
                    }
                    while (arg.endsWith(".")) {
                        arg = arg.substring(0, arg.length() - 1);
                    }
                    templates.add(arg);
                    continue;
                }
                
                usage();
                return;
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            usage();
            return;
        }

        if (rootDir == null) {
            usage();
            return;
        }

        if (context == null) {
            context = org.teatrove.tea.runtime.UtilityContext.class;
        }

        int errorCount, changeCount;
        int totalChangeCount = 0;
        do {
            FileCompiler compiler =
                new FileCompiler(rootDir, null, null, null, encoding);
            
            compiler.setRuntimeContext(context);
            compiler.setForceCompile(true);
            compiler.setCodeGenerationEnabled(false);
            
            Retrofitter retrofitter = new Retrofitter(compiler, encoding);
            compiler.addErrorListener(retrofitter);
            
            String[] names;
            if (templates.size() == 0) {
                names = compiler.compileAll(true);
            }
            else {
                names =
                    (String[])templates.toArray(new String[templates.size()]);
                names = compiler.compile(names);
            }
            
            retrofitter.applyChanges();
            
            changeCount = retrofitter.getChangeCount();
            errorCount = compiler.getErrorCount() - changeCount;
            
            String msg = String.valueOf(errorCount) + " error";
            if (errorCount != 1) {
                msg += 's';
            }
            System.out.println(msg);
            
            totalChangeCount += changeCount;
            System.out.println("Total changes made: " + totalChangeCount);
        } while (changeCount > 0);
    }

    private static void usage() {
        String usageDetail =
            " -context <class>     Specify a runtime context class to compile against.\n" +
            " -encoding <encoding> Specify character encoding used by source files.\n";

        System.out.print("\nUsage: ");
        System.out.print("java ");
        System.out.print(CallRetrofitter.class.getName());
        System.out.println(" {options} <template root directory> {templates}");
        System.out.println();
        System.out.println("where {options} includes:");
        System.out.println(usageDetail);
    }

    private static class Retrofitter extends DefaultContext
        implements ErrorListener
    {

        private Compiler mCompiler;
        private String mEncoding;

        // Maps source files to lists of ErrorEvents.
        private Map mChanges;

        private int mChangeCount;
        
        private Retrofitter(Compiler c, String encoding) {
            mCompiler = c;
            mEncoding = encoding;
            mChanges = new TreeMap();
        }
        
        public void compileError(ErrorEvent e) {
            if ("Can't find function".equalsIgnoreCase(e.getErrorMessage())) {
                File sourceFile = ((FileCompiler.Unit)e.getCompilationUnit())
                    .getSourceFile();
                List errorEvents = (List)mChanges.get(sourceFile);
                if (errorEvents == null) {
                    errorEvents = new ArrayList();
                    mChanges.put(sourceFile, errorEvents);
                }
                errorEvents.add(e);
            }
        }
        
        public int getChangeCount() {
            return mChangeCount;
        }
        
        public void applyChanges() throws IOException {
            Iterator it = mChanges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                applyChanges((File)entry.getKey(), (List)entry.getValue());
            }
            mChanges = null;
        }

        private void applyChanges(File sourceFile, List errorEvents)
            throws IOException
        {
            RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");

            Map replacements = new HashMap();

            Iterator it = errorEvents.iterator();
            while (it.hasNext()) {
                ErrorEvent event = (ErrorEvent)it.next();
                SourceInfo info = event.getSourceInfo();
                CompilationUnit sourceUnit = event.getCompilationUnit();

                int startPos = info.getStartPosition();
                int endPos = info.getEndPosition();
                int len = (endPos - startPos) + 1;

                byte[] bytes = new byte[len];
                raf.seek(startPos);
                raf.readFully(bytes);

                String text;
                if (mEncoding == null) {
                    text = new String(bytes);
                }
                else {
                    text = new String(bytes, mEncoding);
                }

                int index = text.indexOf('(');
                if (index > 0) {
                    String templateName = text.substring(0, index).trim();
                    boolean templateExists = mCompiler.getCompilationUnit
                        (templateName, sourceUnit) != null;
                    if (templateExists) {
                        mChangeCount++;
                        replacements.put(text, "call " + text);
                    }
                }
            }

            raf.close();

            if (replacements.size() <= 0) {
                return;
            }

            print("Modifying: " + sourceFile);

            InputStream in = new FileInputStream(sourceFile);

            Reader reader;
            if (mEncoding == null) {
                reader = new InputStreamReader(in);
            }
            else {
                reader = new InputStreamReader(in, mEncoding);
            }

            reader = new BufferedReader(reader);

            StringBuffer sourceContents =
                new StringBuffer((int)sourceFile.length());

            int c;
            while ((c = reader.read()) != -1) {
                sourceContents.append((char)c);
            }
            reader.close();

            String newContents =
                replace(sourceContents.toString(), replacements);

            OutputStream out = new FileOutputStream(sourceFile);

            Writer writer;
            if (mEncoding == null) {
                writer = new OutputStreamWriter(out);
            }
            else {
                writer = new OutputStreamWriter(out, mEncoding);
            }

            writer = new BufferedWriter(writer);
            writer.write(newContents);
            writer.close();
        }

        public void print(Object obj) {
            System.out.println(toString(obj));
        }
    }
}
