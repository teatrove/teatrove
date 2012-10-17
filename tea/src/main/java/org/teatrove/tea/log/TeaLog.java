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

package org.teatrove.tea.log;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.log.LogListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The TeaLog class extends a regular log.  It simplifies stack traces for the
 * TeaServlet so it is easier for template authors to understand.  It strips off
 * irrelevant stack trace elements in the stack trace leaving only the template
 * and application elements.  These are the only elements that are relevant
 * unless there is a bug in the Tea hosting environment - which never happens! 
 * ;-)
 *
 * This TeaLog is designed to be used in hosting environments such as the
 * TeaServlet and StaticTea.  These hosts use templates as the controller so
 * any stack traces elements before the templates are irrelevant.  Authors of
 * systems that don't use templates as the controller will probably not want to
 * use the log as their stack traces will not return to them enough information.
 *
 * Some examples are listed below.
 *
 * The Trove Log class displays exceptions as:
 *    java.lang.NullPointerException
 *  	at org.teatrove.teaservlet.template.Test1NullPointer.execute(Test1NullPointer.tea:2)
 *  	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 *  	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
 *  	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
 *  	at java.lang.reflect.Method.invoke(Method.java:324)
 *  	at org.teatrove.tea.runtime.TemplateLoader$TemplateImpl.execute(TemplateLoader.java:270)
 *  	at org.teatrove.tea.engine.TemplateSourceImpl$TemplateImpl.execute(TemplateSourceImpl.java:838)
 *  	at org.teatrove.teaservlet.TeaServlet.processTemplate(TeaServlet.java:594)
 *  	at org.teatrove.teaservlet.TeaServlet.doGet(TeaServlet.java:259)
 *  	at org.teatrove.barista.servlet.TQTeaServlet.doGet(TQTeaServlet.java:117)
 *  	at javax.servlet.http.HttpServlet.service(HttpServlet.java:740)
 *  	at javax.servlet.http.HttpServlet.service(HttpServlet.java:853)
 *  	at org.teatrove.barista.http.HttpServletDispatcher$ServletFilterChainTransaction.doFilter(HttpServletDispatcher.java:1436)
 *  	at org.teatrove.barista.http.HttpServletDispatcher$ServletFilterChainTransaction.service(HttpServletDispatcher.java:1409)
 *  	at org.teatrove.trove.util.tq.TransactionQueue$Worker.run(TransactionQueue.java:671)
 *  	at org.teatrove.trove.util.ThreadPool$PooledThread.run(ThreadPool.java:676)
 *  
 * The TeaLog class displays exceptions as:
 *    java.lang.NullPointerException
 *  	at line 2 of template Test1NullPointer.tea
 *
 * @author Reece Wilton
 */
public class TeaLog extends Log {

    private static final long serialVersionUID = 1L;

    private final String TEA_EXCEPTION = ".tea:";

    /**
     * @param name
     * @param parent
     */
    public TeaLog(Log parent) {
        super(parent.getName(), parent);
    }

    public synchronized void info(Throwable t) {
        TeaStackTraceLine[] lines = getTeaStackTraceLines(t);
        if (lines == null) {
            super.info(t);
        }
        else {
            if (isEnabled() && isInfoEnabled()) {
                dispatchLogTeaStackTrace(new TeaLogEvent(this, LogEvent.INFO_TYPE,
                                                         lines));
            }
        }
    }

    public synchronized void error(Throwable t) {
        TeaStackTraceLine[] lines = getTeaStackTraceLines(t);
        if (lines == null) {
            super.error(t);
        }
        else {
            if (isEnabled() && isErrorEnabled()) {
                dispatchLogTeaStackTrace(new TeaLogEvent(this, LogEvent.ERROR_TYPE,
                                                         lines));
            }
        }
    }

    public synchronized void warn(Throwable t) {
        TeaStackTraceLine[] lines = getTeaStackTraceLines(t);
        if (lines == null) {
            super.warn(t);
        }
        else {
            if (isEnabled() && isWarnEnabled()) {
                dispatchLogTeaStackTrace(new TeaLogEvent(this, LogEvent.WARN_TYPE,
                                                         lines));
            }
        }
    }

    public synchronized void debug(Throwable t) {
        TeaStackTraceLine[] lines = getTeaStackTraceLines(t);
        if (lines == null) {
            super.debug(t);
        }
        else {
            if (isEnabled() && isDebugEnabled()) {
                dispatchLogTeaStackTrace(new TeaLogEvent(this, LogEvent.DEBUG_TYPE,
                                                         lines));
            }
        }
    }

    private void dispatchLogTeaStackTrace(TeaLogEvent e) {
        int size = mListeners.size();
        try {
            for (int i=0; i<size; i++) {
                LogListener listener = (LogListener) mListeners.get(i);
                if (listener instanceof TeaLogListener) {
                    ((TeaLogListener)listener).logTeaStackTrace(e);
                }
                else {
                    e.setMessage(printTeaStackTraceLines(e.getLines()));
                    listener.logMessage(e);
                }
            }
        }
        catch (IndexOutOfBoundsException ex) {
        }
    }

    /**
     * Prints the stack trace lines to a String.
     *
     * @param lines
     * @return  the output of the stack trace lines as a string
     */
    private String printTeaStackTraceLines(TeaStackTraceLine[] lines) {

        String result = "";
        for (int line = 0; line < lines.length; line++) {
            if (line > 0) {
                result += '\n';
            }
            result += lines[line].toString();
        }

        return result;
    }

    /**
     * Splits the stack trace into separate lines and extracts the template
     * name and line number.
     *
     * @param t
     * @return  the separated stack trace lines
     */
    private TeaStackTraceLine[] getTeaStackTraceLines(Throwable t) {

        // grab the existing stack trace
        StringWriter stackTraceGrabber = new StringWriter();
        t.printStackTrace(new PrintWriter(stackTraceGrabber));
        String stackTrace = stackTraceGrabber.toString();
        int extensionIndex = stackTrace.lastIndexOf(TEA_EXCEPTION);
        boolean isTeaException = extensionIndex != -1;
        if (isTeaException) {

            // trim off all lines after the last template exception
            int endIndex = stackTrace.indexOf('\n', extensionIndex);
            int endRIndex = stackTrace.indexOf('\r', extensionIndex);

            if(endRIndex>-1 && endRIndex<endIndex)
                endIndex=endRIndex;

            if (endIndex <= 0) {
                endIndex = stackTrace.length();
            }

            stackTrace = stackTrace.substring(0, endIndex);

            // parse each line
            List<TeaStackTraceLine> teaStackTraceLines =
                new ArrayList<TeaStackTraceLine>();
            StringTokenizer tokenizer = new StringTokenizer(stackTrace, "\n");
            while (tokenizer.hasMoreElements()) {
                String line = (String)tokenizer.nextElement();
                if (line.indexOf(TEA_EXCEPTION) != -1) {
                    /*
                    TODO: make sure this works for lines that don't have
                          line numbers.  Look in ESPN logs for examples.
                    at org.teatrove.teaservlet.template.schedule.substitute(schedule.tea:78)
                    at org.teatrove.teaservlet.template.shell.story.frame.substitute(shell/story/frame.tea)
                    */
                    String tempLine = line;

                    int bracket = tempLine.indexOf('(');
                    tempLine = tempLine.substring(bracket + 1);
                    bracket = tempLine.indexOf(')');
                    tempLine = tempLine.substring(0, bracket);
                    int colonIndex = tempLine.indexOf(':');
                    String templateName = null;
                    Integer lineNumber = null;
                    if (colonIndex >= 0) {
                        templateName = tempLine.substring(0, colonIndex);
                        try {
                            lineNumber = 
                                new Integer(tempLine.substring(colonIndex + 1));
                        }
                        catch (NumberFormatException nfe) { lineNumber = null; }
                    }
                    else {
                        templateName = tempLine;
                        lineNumber = null;
                    }
                    teaStackTraceLines.add(new TeaStackTraceLine(templateName,
                                                                 lineNumber,
                                                                 line));
                }
                else {
                    teaStackTraceLines.add(new TeaStackTraceLine(null, null, line));
                }
            }
            return (TeaStackTraceLine[]) teaStackTraceLines.toArray(
                              new TeaStackTraceLine[teaStackTraceLines.size()]);
        }
        else {
            return null;
        }
    }

    /**
     * The TeaStackTraceLine class contains the values of a stack trace line.
     * It contains the template name and line number.  It also includes the
     * original stack trace line from the JVM.
     *
     * @author Reece Wilton
     */
    public class TeaStackTraceLine {

        private String mTemplateName;
        private Integer mLineNumber;
        private String mLine;

        public TeaStackTraceLine(String templateName,
                                 Integer lineNumber,
                                 String line) {
            mTemplateName = templateName;
            mLineNumber = lineNumber;
            mLine = line;
        }

        /**
         * @return  the original stack trace line
         */
        public String getLine() {
            return mLine;
        }

        /**
         * @return  the Tea template line number.  May be null even if a
         * template name exists.
         */
        public Integer getLineNumber() {
            return mLineNumber;
        }

        /**
         * @return  the template name.  Will be null if the line isn't for a
         * Tea template.
         */
        public String getTemplateName() {
            return mTemplateName;
        }

        public String toString() {
            if (mLineNumber != null) {
                return "\tat " +
                       ((mLineNumber != null) ? "line " + mLineNumber + " of "
                                                : "unknown line of ")
                       + "template " + mTemplateName;
            }
            else {
                return mLine;
            }
        }
    }
}
