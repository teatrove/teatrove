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
import java.util.*;

/**
 * LogEventParsingOutputStream parses the data written to it and converts it
 * to LogEvent objects. Add a LogListener to intercept LogEvents. Events are
 * parsed based on newline characters (LF, CRLF or CR) or a switch to a
 * different thread.
 * 
 * @author Brian S O'Neill
 */
public class LogEventParsingOutputStream extends OutputStream {
    private Vector mListeners;
    private Log mSource;
    private int mType;

    private byte[] mOneByte = new byte[1];

    private ByteArrayOutputStream mMessageBuffer;
    private String mEncoding;
    private Thread mMessageThread;

    private Date mTimestamp;

    // Set to true upon reading a CR so that if the next character is a LF,
    // it is consumed. This is how CRLF patterns are discovered.
    private boolean mTrackLF;

    /**
     * @param source Source object to create events with.
     * @param type Type of events to create.
     */
    public LogEventParsingOutputStream(Log source, int type) {
        mListeners = new Vector(2);
        mSource = source;
        mType = type;

        mMessageBuffer = new ByteArrayOutputStream();
    }

    /**
     * @param source Source object to create events with.
     * @param type Type of events to create.
     * @param encoding Character encoding of bytes.
     */
    public LogEventParsingOutputStream(Log source, int type,
                                       String encoding) {
        mListeners = new Vector(2);
        mSource = source;
        mType = type;

        mMessageBuffer = new ByteArrayOutputStream();
        mEncoding = encoding;
    }

    public void addLogListener(LogListener listener) {
        mListeners.addElement(listener);
    }

    public void removeLogListener(LogListener listener) {
        mListeners.removeElement(listener);
    }

    private synchronized void flushLogEvent() 
        throws UnsupportedEncodingException {

        if (mMessageThread == null) {
            return;
        }

        String message;
        if (mEncoding == null) {
            message = mMessageBuffer.toString();
        }
        else {
            message = mMessageBuffer.toString(mEncoding);
        }

        if (mMessageBuffer.size() > 10000) {
            mMessageBuffer = new ByteArrayOutputStream();
        }
        else {
            mMessageBuffer.reset();
        }

        LogEvent e;

        if (mTimestamp == null) {
            e = new LogEvent(mSource, mType, message, mMessageThread);
        }
        else {
            e = new LogEvent(mSource, mType, message, mMessageThread, 
                             mTimestamp);
            mTimestamp = null;
        }

        synchronized (mListeners) {
            Enumeration en = mListeners.elements();
            while (en.hasMoreElements()) {
                ((LogListener)en.nextElement()).logMessage(e);
            }
        }
    }

    public synchronized void write(int b) throws IOException {
        mOneByte[0] = (byte)b;
        write(mOneByte, 0, 1);
    }

    public synchronized void write(byte[] array, int off, int len) 
        throws IOException {

        if (!isEnabled()) {
            if (mMessageBuffer.size() > 0) {
                flushLogEvent();
            }
            return;
        }

        Thread current = Thread.currentThread();
        if (current != mMessageThread) {
            if (mMessageBuffer.size() > 0) {
                flushLogEvent();
            }
            mMessageThread = current;
        }

        int writtenLength = 0;

        int i = 0;
        for (i=0; i<len; i++) {
            byte b = array[i + off];
            if (b == (byte)'\r') {
                mTrackLF = true;
                writeToBuffer(array, writtenLength + off, i - writtenLength);
                // Add one more than i to skip the CR.
                writtenLength = i + 1;
                flushLogEvent();
            }
            else if (b == (byte)'\n') {
                if (mTrackLF) {
                    // Consume the LF of CRLF.
                    mTrackLF = false;
                    writtenLength++;
                }
                else {
                    writeToBuffer(array, writtenLength + off, 
                                  i - writtenLength);
                    // Add one more than i to skip the LF.
                    writtenLength = i + 1;
                    flushLogEvent();
                }
            }
            else {
                mTrackLF = false;
            }
        }

        writeToBuffer(array, writtenLength + off, i - writtenLength);
    }

    public synchronized void close() throws IOException {
        mMessageBuffer.close();
    }

    /**
     * Returning false discards written data, and events are not generated. 
     * Default implementation always returns true. 
     */
    public boolean isEnabled() {
        return true;
    }

    private void writeToBuffer(byte[] array, int off, int len) {
        if (mMessageBuffer.size() == 0) {
            mTimestamp = new Date();
        }
        mMessageBuffer.write(array, off, len);
    }
}
