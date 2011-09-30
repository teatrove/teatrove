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

import java.io.*;
import java.net.*;
import java.lang.ref.*;

/**
 * A SocketException that prints out additional detail in the message: address
 * and port of the socket.
 *
 * @author Brian S O'Neill
 */
public class CheckedSocketException extends SocketException {
    static CheckedSocketException create
        (Exception cause, SocketFace source) {
        if (cause instanceof CheckedSocketException) {
            return (CheckedSocketException)cause;
        }
        else {
            return new CheckedSocketException(cause, source);
        }
    }

    static CheckedSocketException create
        (Exception cause, SocketFace source, String message) {
        if (cause instanceof CheckedSocketException) {
            return (CheckedSocketException)cause;
        }
        else {
            return new CheckedSocketException(cause, source, message);
        }
    }

    static String createMessagePrefix(SocketFace source) {
        String localAddr;
        if (source.getLocalAddress() == null) {
            localAddr = "null";
        }
        else {
            localAddr = source.getLocalAddress().getHostAddress();
        }

        String remoteAddr;
        if (source.getInetAddress() == null) {
            remoteAddr = "null";
        }
        else {
            remoteAddr = source.getInetAddress().getHostAddress();
        }

        return 
            "[" + localAddr + ':' + source.getLocalPort() +
            ',' + remoteAddr + ':' + source.getPort() + ']';
    }

    private Exception mCause;
    private String mMessagePrefix;
    private Reference mSource;

    private CheckedSocketException(Exception cause, SocketFace source) {
        this(cause, source, cause.getMessage());
    }

    private CheckedSocketException(Exception cause, SocketFace source,
                                   String message) {
        super(createMessagePrefix(source) + ' ' + message);
        mCause = cause;
        mMessagePrefix = createMessagePrefix(source);
        mSource = new WeakReference(source);
    }

    /**
     * @return instance of Exception
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
