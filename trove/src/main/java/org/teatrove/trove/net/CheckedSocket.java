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
import java.util.*;

/**
 * A socket that tracks if any I/O exceptions have occured and ensures that
 * plain I/O exceptions are thrown as SocketExceptions. InterruptedIOExceptions
 * do not affect the exception count, and they are not converted to
 * SocketExceptions.
 * <p>
 * All socket exceptions thrown will actually be instances of
 * {@link CheckedSocketException}, which subclasses SocketException. The error
 * messages will contain additional information, and the original exception
 * and socket can be obtained from it.
 *
 * @author Brian S O'Neill
 */
public class CheckedSocket extends SocketFaceWrapper {
    /**
     * Returns a new CheckedSocket instance only if the given socket isn't
     * already one.
     */
    public static CheckedSocket check(SocketFace socket)
        throws SocketException
    {
        if (socket instanceof CheckedSocket) {
            return (CheckedSocket)socket;
        }
        else {
            return new CheckedSocket(socket);
        }
    }

    public static CheckedSocket check(Socket socket) throws SocketException {
        return new CheckedSocket(socket);
    }

    private int mExceptionCount;
    private InputStream mIn;
    private OutputStream mOut;

    private Set mExceptionListeners;

    protected CheckedSocket(SocketFace socket) throws SocketException {
        super(socket);
    }

    protected CheckedSocket(Socket socket) throws SocketException {
        super(socket);
    }

    /**
     * Returns the total number of exceptions encountered while using this
     * socket, excluding InterruptedIOExceptions. If this count is not zero,
     * then the socket is potentially in an unrecoverable state.
     */
    public int getExceptionCount() {
        return mExceptionCount;
    }

    /**
     * Internally, the collection of listeners is saved in a set so that
     * listener instances may be added multiple times without harm.
     */
    public synchronized void addExceptionListener(ExceptionListener listener) {
        if (mExceptionListeners == null) {
            mExceptionListeners = new HashSet();
        }
        mExceptionListeners.add(listener);
    }

    public synchronized void removeExceptionListener(ExceptionListener listener) {
        if (mExceptionListeners != null) {
            mExceptionListeners.remove(listener);
        }
    }
    
    public synchronized InputStream getInputStream() throws IOException {
        if (mIn != null) {
            return mIn;
        }
        
        try {
            return mIn = new Input(super.getInputStream());
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        if (mOut != null) {
            return mOut;
        }
        
        try {
            return mOut = new Output(super.getOutputStream());
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        try {
            super.setTcpNoDelay(on);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        try {
            return super.getTcpNoDelay();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        try {
            super.setSoLinger(on, linger);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSoLinger() throws SocketException {
        try {
            return super.getSoLinger();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSoTimeout(int timeout) throws SocketException {
        try {
            super.setSoTimeout(timeout);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSoTimeout() throws SocketException {
        try {
            return super.getSoTimeout();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSendBufferSize(int size) throws SocketException {
        try {
            super.setSendBufferSize(size);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSendBufferSize() throws SocketException {
        try {
            return super.getSendBufferSize();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        try {
            super.setReceiveBufferSize(size);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getReceiveBufferSize() throws SocketException {
        try {
            return super.getReceiveBufferSize();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void close() throws IOException {
        try {
            super.close();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void setKeepAlive(boolean on) throws SocketException {
        try {
            super.setKeepAlive(on);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public boolean getKeepAlive() throws SocketException {
        try {
            return super.getKeepAlive();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void shutdownInput() throws IOException {
        try {
            super.shutdownInput();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void shutdownOutput() throws IOException {
        try {
            super.shutdownOutput();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    /**
     * @param e should be instance of IOException or RuntimeException.
     */
    IOException handleIOException(Exception e) {
        if (e instanceof InterruptedIOException) {
            return CheckedInterruptedIOException.create
                ((InterruptedIOException)e, mSocket);
        }

        int count;
        synchronized (this) {
            count = ++mExceptionCount;
        }
        exceptionOccurred(e, count);

        if (e instanceof CheckedSocketException) {
            return (CheckedSocketException)e;
        }
        else if (e instanceof NullPointerException) {
            // Workaround for a bug in the Socket class that sometimes
            // causes a NullPointerException on a closed socket.
            return CheckedSocketException.create(e, mSocket, "Socket closed");
        }
        else {
            return CheckedSocketException.create(e, mSocket);
        }
    }

    /**
     * @param e Should be instance of SocketException or RuntimeException.
     */
    SocketException handleSocketException(Exception e) {
        int count;
        synchronized (this) {
            count = ++mExceptionCount;
        }
        exceptionOccurred(e, count);

        if (e instanceof CheckedSocketException) {
            return (CheckedSocketException)e;
        }
        else if (e instanceof NullPointerException) {
            // Workaround for a bug in the Socket class that sometimes
            // causes a NullPointerException on a closed socket.
            return CheckedSocketException.create(e, mSocket, "Socket closed");
        }
        else {
            return CheckedSocketException.create(e, mSocket);
        }
    }

    private synchronized void exceptionOccurred(Exception e, int count) {
        if (mExceptionListeners != null) {
            Iterator it = mExceptionListeners.iterator();
            while (it.hasNext()) {
                ((ExceptionListener)it.next())
                    .exceptionOccurred(this, e, count);
            }
        }
    }

    public static interface ExceptionListener {
        /**
         * @param count new exception count, which will be one the first time
         * this method is called.
         */
        public void exceptionOccurred(CheckedSocket s, Exception e, int count);
    }

    private class Input extends InputStream {
        private final InputStream mStream;

        public Input(InputStream in) {
            mStream = in;
        }

        public int read() throws IOException {
            try {
                return mStream.read();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int read(byte[] b) throws IOException {
            try {
                return mStream.read(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return mStream.read(b, off, len);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public long skip(long n) throws IOException {
            try {
                return mStream.skip(n);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int available() throws IOException {
            try {
                return mStream.available();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void close() throws IOException {
            try {
                mStream.close();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void mark(int readlimit) {
            mStream.mark(readlimit);
        }

        public void reset() throws IOException {
            try {
                mStream.reset();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }

        public boolean markSupported() {
            return mStream.markSupported();
        }
    }

    private class Output extends OutputStream {
        private final OutputStream mStream;

        public Output(OutputStream out) {
            mStream = out;
        }

        public void write(int b) throws IOException {
            try {
                mStream.write(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void write(byte[] b) throws IOException {
            try {
                mStream.write(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                mStream.write(b, off, len);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void flush() throws IOException {
            try {
                mStream.flush();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void close() throws IOException {
            try {
                mStream.close();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
    }
}
