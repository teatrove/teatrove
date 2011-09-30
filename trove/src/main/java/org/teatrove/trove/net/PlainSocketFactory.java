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

import java.net.*;

/**
 * Allows client socket connections to be established with a timeout. Calling
 * getSocket will always return a new socket, and recycle will always close the
 * socket. Sessions are ignored on all requests.
 *
 * @author Brian S O'Neill
 */
public class PlainSocketFactory implements SocketFactory {
    protected final InetAddress mAddr;
    protected final int mPort;
    protected final InetAddress mLocalAddr;
    protected final int mLocalPort;
    protected final long mTimeout;

    /**
     * @param addr Address to connect new sockets to.
     * @param port Port to connect new sockets to.
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public PlainSocketFactory(InetAddress addr, int port, long timeout) {
        mAddr = addr;
        mPort = port;
        mLocalAddr = null;
        mLocalPort = 0;
        mTimeout = timeout;
    }

    /**
     * @param addr Address to connect new sockets to.
     * @param port Port to connect new sockets to.
     * @param localAddr Local address to bind new sockets to
     * @param localPort Local port to bind new sockets to, 0 for any
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public PlainSocketFactory(InetAddress addr, int port,
                              InetAddress localAddr, int localPort,
                              long timeout)
    {
        mAddr = addr;
        mPort = port;
        mLocalAddr = localAddr;
        mLocalPort = localPort;
        mTimeout = timeout;
    }

    public InetAddressAndPort getInetAddressAndPort() {
        return new InetAddressAndPort(mAddr, mPort);
    }
    
    public InetAddressAndPort getInetAddressAndPort(Object session) {
        return getInetAddressAndPort();
    }
    
    public long getDefaultTimeout() {
        return mTimeout;
    }

    public CheckedSocket createSocket()
        throws ConnectException, SocketException
    {
        return createSocket(mTimeout);
    }

    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException
    {
        return createSocket(mTimeout);
    }

    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        Socket socket = SocketConnector.connect
            (mAddr, mPort, mLocalAddr, mLocalPort, timeout);
        if (socket == null) {
            throw new ConnectException("Connect timeout expired: " + timeout);
        }
        return CheckedSocket.check(socket);
    }
    
    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return createSocket(timeout);
    }

    public CheckedSocket getSocket() throws ConnectException, SocketException {
        return createSocket(mTimeout);
    }

    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException
    {
        return createSocket(mTimeout);
    }

    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException
    {
        return createSocket(timeout);
    }

    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return createSocket(timeout);
    }

    public void recycleSocket(CheckedSocket socket)
        throws SocketException, IllegalArgumentException
    {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (java.io.IOException e) {
                throw new SocketException(e.getMessage());
            }
        }
    }
    
    public void clear() {
    }
    
    public int getAvailableCount() {
        return 0;
    }
}
