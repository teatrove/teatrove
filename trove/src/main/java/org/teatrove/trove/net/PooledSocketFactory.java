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
 * Socket factory implementation that pools connections to one wrapped socket
 * factory. Sessions are ignored on all requests. Consider wrapping with a
 * {@link LazySocketFactory} for automatic checking against pooled connections
 * that may have been closed.
 *
 * @author Brian S O'Neill
 */
public class PooledSocketFactory implements SocketFactory {
    private final SocketFactory mFactory;
    private final long mTimeout;

    // Stack of Sockets.
    private final Stack mPool = new Stack();

    private CheckedSocket.ExceptionListener mListener;

    public PooledSocketFactory(SocketFactory factory) {
        this(factory, factory.getDefaultTimeout());
    }

    public PooledSocketFactory(SocketFactory factory, int maxPoolSize) {
		this(factory);
	}

    public PooledSocketFactory(SocketFactory factory, long timeout) {
        mFactory = factory;
        mTimeout = timeout;

        mListener = new CheckedSocket.ExceptionListener() {
            public void exceptionOccurred(CheckedSocket s,
                                          Exception e, int count) {
                // Only act on the first exception.
                if (count == 1) {
                    // Assume all the pooled connections are bad, so ditch 'em.
                    clear();
                }
            }
        };
    }

    public InetAddressAndPort getInetAddressAndPort() {
        return mFactory.getInetAddressAndPort();
    }

    public InetAddressAndPort getInetAddressAndPort(Object session) {
        return mFactory.getInetAddressAndPort();
    }

    public long getDefaultTimeout() {
        return mTimeout;
    }

    public CheckedSocket createSocket()
        throws ConnectException, SocketException
    {
        return createSocket(null, mTimeout);
    }

    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException
    {
        return createSocket(mTimeout);
    }

    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        return new PooledSocket(mFactory.createSocket(timeout));
    }

    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return createSocket(timeout);
    }

    public CheckedSocket getSocket()
        throws ConnectException, SocketException
    {
        return getSocket(mTimeout);
    }

    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException
    {
        return getSocket(mTimeout);
    }

    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException
    {
        synchronized (mPool) {
            if (mPool.size() > 0) {
                return new PooledSocket((SocketFace)mPool.pop());
            }
        }

        return new PooledSocket(mFactory.createSocket(timeout));
    }

    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return getSocket(timeout);
    }

    public void recycleSocket(CheckedSocket socket)
        throws SocketException, IllegalArgumentException
    {
        if (socket != null) {
            if (socket instanceof PooledSocket) {
                PooledSocket psock = (PooledSocket)socket;

                if (psock.getOwner() == this) {
                    psock.removeExceptionListener(mListener);
                    SocketFace s = psock.recycle();
                    if (s != null) {
                        mPool.push(s);
                    }
                    return;
                }
            }

            throw new IllegalArgumentException
                ("Socket did not originate from this pool");
        }
    }

    public void clear() {
        synchronized (mPool) {
            while (mPool.size() > 0) {
                try {
                    ((SocketFace)mPool.pop()).close();
                }
                catch (IOException e) {
                }
            }
        }
    }

    public int getAvailableCount() {
        return mPool.size();
    }

    /**
     * This class does two things. First, it supports virtual socket closure.
     * After a socket is put back into the pool, it can't be used again, but
     * the internal socket is still open.
     *
     * This class also tracks exceptions checks if this socket can be recycled.
     */
    private class PooledSocket extends CheckedSocket {
        private InputStream mIn;
        private OutputStream mOut;
        private boolean mClosed;

        public PooledSocket(SocketFace s) throws SocketException {
            super(s);
            addExceptionListener(mListener);
        }

        public synchronized InputStream getInputStream() throws IOException {
            if (mIn != null) {
                return mIn;
            }

            final InputStream mStream = super.getInputStream();

            mIn = new InputStream() {
                public int read() throws IOException {
                    check();
                    return mStream.read();
                }

                public int read(byte[] b) throws IOException {
                    check();
                    return mStream.read(b);
                }

                public int read(byte[] b, int off, int len) throws IOException{
                    check();
                    return mStream.read(b, off, len);
                }

                public long skip(long n) throws IOException {
                    check();
                    return mStream.skip(n);
                }

                public int available() throws IOException {
                    check();
                    return mStream.available();
                }

                public void close() throws IOException {
                    if (doClose()) {
                        mStream.close();
                    }
                }

                public void mark(int readlimit) {
                    mStream.mark(readlimit);
                }

                public void reset() throws IOException {
                    check();
                    mStream.reset();
                }

                public boolean markSupported() {
                    return mStream.markSupported();
                }
            };

            return mIn;
        }

        public synchronized OutputStream getOutputStream() throws IOException {
            if (mOut != null) {
                return mOut;
            }

            final OutputStream mStream = super.getOutputStream();

            mOut = new OutputStream() {
                public void write(int b) throws IOException {
                    check();
                    mStream.write(b);
                }

                public void write(byte[] b) throws IOException {
                    check();
                    mStream.write(b);
                }

                public void write(byte[] b, int off, int len) throws IOException {
                    check();
                    mStream.write(b, off, len);
                }

                public void flush() throws IOException {
                    check();
                    mStream.flush();
                }

                public void close() throws IOException {
                    if (doClose()){
                        mStream.close();
                    }
                }
            };

            return mOut;
        }

        public void setTcpNoDelay(boolean on) throws SocketException {
            check();
            super.setTcpNoDelay(on);
        }

        public boolean getTcpNoDelay() throws SocketException {
            check();
            return super.getTcpNoDelay();
        }

        public void setSoLinger(boolean on, int linger) throws SocketException {
            check();
            super.setSoLinger(on, linger);
        }

        public int getSoLinger() throws SocketException {
            check();
            return super.getSoLinger();
        }

        public void setSoTimeout(int timeout) throws SocketException {
            check();
            super.setSoTimeout(timeout);
        }

        public int getSoTimeout() throws SocketException {
            check();
            return super.getSoTimeout();
        }

        public void setSendBufferSize(int size) throws SocketException {
            check();
            super.setSendBufferSize(size);
        }

        public int getSendBufferSize() throws SocketException {
            check();
            return super.getSendBufferSize();
        }

        public void setReceiveBufferSize(int size) throws SocketException {
            check();
            super.setReceiveBufferSize(size);
        }

        public int getReceiveBufferSize() throws SocketException {
            check();
            return super.getReceiveBufferSize();
        }

        public void close() throws IOException {
            if (doClose()) {
                super.close();
            }
        }

        SocketFactory getOwner() {
            return PooledSocketFactory.this;
        }

        SocketFace recycle() throws SocketException {
            if (mClosed) {
                return null;
            }
            else if (getExceptionCount() != 0) {
                try {
                    close();
                }
                catch (IOException e) {
                    throw new SocketException(e.getMessage());
                }
                return null;
            }
            else {
                mClosed = true;
                return mSocket;
            }
        }

        boolean doClose() {
            return (mClosed) ? false : (mClosed = true);
        }

        void check() throws SocketException {
            if (mClosed) {
                throw new SocketException("Socket closed");
            }
        }
    }
}
