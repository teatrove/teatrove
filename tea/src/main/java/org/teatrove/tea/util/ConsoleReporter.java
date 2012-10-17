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
import java.io.PrintStream;
import java.io.BufferedReader;
import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.CompileEvent;
import org.teatrove.tea.compiler.CompileListener;
import org.teatrove.trove.io.LinePositionReader;

/**
 * ConsoleReporter takes CompileEvents and prints
 * detailed messages to a PrintStream. When no longer needed, close the
 * ConsoleReporter to ensure all open resources (except the PrintStream)
 * are closed. 
 *
 * @author Brian S O'Neill
 */
public class ConsoleReporter implements CompileListener {
    private PrintStream mOut;
    private LinePositionReader mPositionReader;
    private CompilationUnit mPositionReaderUnit;

    public ConsoleReporter(PrintStream out) {
        mOut = out;
    }

    /**
     * Closes all open resources.
     */
    public void close() throws IOException {
        if (mPositionReader != null) {
            mPositionReader.close();
        }

        mPositionReader = null;
        mPositionReaderUnit = null;
    }

    public void compileError(CompileEvent e) {
        compileIssue(e);
    }
    
    public void compileWarning(CompileEvent e) {
        compileIssue(e);
    }
    
    public void compileIssue(CompileEvent e) {
        mOut.println(e.getType().toString() + ": " + e.getDetailedMessage());

        SourceInfo info = e.getSourceInfo();
        CompilationUnit unit = e.getCompilationUnit();

        try {
            if (unit != null && info != null) {
                int line = info.getLine();
                int start = info.getStartPosition();
                int end = info.getEndPosition();

                if (mPositionReader == null ||
                    mPositionReaderUnit != unit ||
                    mPositionReader.getLineNumber() >= line) {

                    mPositionReaderUnit = unit;
                    mPositionReader = new LinePositionReader
                        (new BufferedReader(unit.getReader()));
                }

                mPositionReader.skipForwardToLine(line);
                int position = mPositionReader.getNextPosition();

                String lineStr = mPositionReader.readLine();
                lineStr = LinePositionReader.cleanWhitespace(lineStr);
                mOut.println(lineStr);

                int indentSize = start - position;
                String indent =
                    LinePositionReader.createSequence(' ', indentSize);

                int markerSize = end - start + 1;
                String marker =
                    LinePositionReader.createSequence('^', markerSize);

                mOut.print(indent);
                mOut.println(marker);
                mOut.println();
            }
        }
        catch (IOException ex) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, ex);
        }
    }

    protected void finalize() throws Throwable {
        close();
    }
}
