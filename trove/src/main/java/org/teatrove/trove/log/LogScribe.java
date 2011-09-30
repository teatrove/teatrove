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

package org.teatrove.trove.log;

import java.io.*;
import java.text.*;
import java.util.TimeZone;
import org.teatrove.trove.util.FastDateFormat;

/**
 * LogScribe is a LogListener that writes log messages to a PrintWriter. Each
 * message is printed to a line that is prepended with other LogEvent
 * information. The default prepend format is as follows:
 *
 * <pre>
 * [event type code],[date & time],[thread name],[log source name]>[one space]
 * </pre>
 *
 * The event type codes are <tt>" D", " I", "*W" and "*E"</tt> for debug,
 * info, warn and error, respectively. The default date format looks like
 * this: <tt>"1999/06/08 18:08:34.067 PDT"</tt>. If there is no log source, or
 * it has no name, it is omitted from the prepend. Here is a sample line that
 * is written out:
 *
 * <pre>
 *  I,1999/06/08 18:08:34.67 PDT,main> Started Transaction Manager
 * </pre>
 *
 * @author Brian S O'Neill
 */
public class LogScribe implements LogListener {
    private PrintWriter mWriter;
    private DateFormat mSlowFormat;
    private FastDateFormat mFastFormat;

    private boolean mShowThread = true;
    private boolean mShowSourceName = true;

    public LogScribe(PrintWriter writer) {
        this(writer, (FastDateFormat)null);
    }

    public LogScribe(PrintWriter writer, DateFormat format) {
        mWriter = writer;
        mSlowFormat = format;
        if (format == null) {
            mFastFormat =
                FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS z");
        }
        else if (format instanceof SimpleDateFormat) {
            SimpleDateFormat simple = (SimpleDateFormat)format;
            String pattern = simple.toPattern();
            TimeZone timeZone = simple.getTimeZone();
            DateFormatSymbols symbols = simple.getDateFormatSymbols();
            mFastFormat =
                FastDateFormat.getInstance(pattern, timeZone, null, symbols);
        }
    }

    public LogScribe(PrintWriter writer, FastDateFormat format) {
        mWriter = writer;
        mSlowFormat = null;
        if (format == null) {
            format = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS z");
        }
        mFastFormat = format;
    }

    public void logMessage(LogEvent e) {
        String message = e.getMessage();
        if (message != null) {
            synchronized (mWriter) {
                mWriter.print(createPrepend(e));
                mWriter.println(message);
                mWriter.flush();
            }
        }
    }

    public void logException(LogEvent e) {
        Throwable t = e.getException();
        
        if (t == null) {
            logMessage(e);
        }
        else {
            synchronized (mWriter) {
                mWriter.print(createPrepend(e));
                t.printStackTrace(mWriter);
                mWriter.flush();
            }
        }
    }

    /**
     * The showing of the event thread name is on by default.
     */
    public boolean isShowThreadEnabled() {
        return mShowThread;
    }

    public void setShowThreadEnabled(boolean enabled) {
        mShowThread = enabled;
    }

    /**
     * The showing of the event source name is on by default.
     */
    public boolean isShowSourceEnabled() {
        return mShowSourceName;
    }

    public void setShowSourceEnabled(boolean enabled) {
        mShowSourceName = enabled;
    }

    /**
     * Creates the default line prepend for a message.
     */
    protected String createPrepend(LogEvent e) {
        StringBuffer pre = new StringBuffer(80);
                
        String code = "??";
        switch (e.getType()) {
        case LogEvent.DEBUG_TYPE:
            code = " D";
            break;
        case LogEvent.INFO_TYPE:
            code = " I";
            break;
        case LogEvent.WARN_TYPE:
            code = "*W";
            break;
        case LogEvent.ERROR_TYPE:
            code = "*E";
            break;
        }

        pre.append(code);
        pre.append(',');
        if (mFastFormat != null) {
            pre.append(mFastFormat.format(e.getTimestamp()));
        }
        else {
            synchronized (mSlowFormat) {
                pre.append(mSlowFormat.format(e.getTimestamp()));
            }
        }

        if (isShowThreadEnabled()) {
            pre.append(',');
            pre.append(e.getThreadName());
        }

        if (isShowSourceEnabled()) {
            Log source = e.getLogSource();
            if (source != null) {
                String sourceName = source.getName();
                if (sourceName != null) {
                    pre.append(',');
                    pre.append(sourceName);
                }
            }
        }

        pre.append('>');
        pre.append(' ');

        return pre.toString();
    }
}
