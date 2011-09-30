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

package org.teatrove.trove.net;

import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * 
 * @author Brian S O'Neill
 */
public class CheckedInterruptedIOException extends InterruptedIOException {
    static CheckedInterruptedIOException create
        (InterruptedIOException cause, SocketFace source) {
        if (cause instanceof CheckedInterruptedIOException) {
            return (CheckedInterruptedIOException)cause;
        }
        else {
            return new CheckedInterruptedIOException(cause, source);
        }
    }

    private InterruptedIOException mCause;
    private String mMessagePrefix;
    private Reference mSource;

    private CheckedInterruptedIOException(InterruptedIOException cause,
                                          SocketFace source) {
        this(cause, source, cause.getMessage());
    }

    private CheckedInterruptedIOException(InterruptedIOException cause,
                                          SocketFace source, String message) {
        super(CheckedSocketException.createMessagePrefix(source) +
              ' ' + message);
        mCause = cause;
        mMessagePrefix = CheckedSocketException.createMessagePrefix(source);
        mSource = new WeakReference(source);
    }

    /**
     * @return instance of InterruptedIOException
     */
    public Throwable getCause() {
        return mCause;
    }

    /**
     * Returns null if source socket has been reclaimed by the garbage
     * collector.
     */
    public SocketFace getSource() {
        return (SocketFace)mSource.get();
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            ps.print(mMessagePrefix);
            ps.print(": ");
            mCause.printStackTrace(ps);
        }
    }

    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            pw.print(mMessagePrefix);
            pw.print(": ");
            mCause.printStackTrace(pw);
        }
    }
}
