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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.ConnectException;

/**
 * Wrapper for using secure sockets.
 *
 * @author Jonathan Colwell
 */
public class SSLSocketFactory extends PlainSocketFactory {
    private final javax.net.ssl.SSLSocketFactory mSSLFactory;
       
    /**
     * @param addr Address to connect new sockets to.
     * @param port Port to connect new sockets to.
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public SSLSocketFactory(InetAddress addr, int port, long timeout) {
        super(addr, port, timeout);
        mSSLFactory = (javax.net.ssl.SSLSocketFactory)
            javax.net.ssl.SSLSocketFactory.getDefault();
    }

    /**
     * @param addr Address to connect new sockets to.
     * @param port Port to connect new sockets to.
     * @param localAddr Local address to bind new sockets to
     * @param localPort Local port to bind new sockets to, 0 for any
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public SSLSocketFactory(InetAddress addr, int port,
                            InetAddress localAddr, int localPort,
                            long timeout)
    {
        super(addr, port, localAddr, localPort, timeout);
        mSSLFactory = (javax.net.ssl.SSLSocketFactory)
            javax.net.ssl.SSLSocketFactory.getDefault();
    }
    
    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        Socket socket = SocketConnector.connect
            (mAddr, mPort, mLocalAddr, mLocalPort, timeout);
        if (socket == null) {
            throw new ConnectException("Connect timeout expired: " + timeout);
        }
        try {
            socket = mSSLFactory.createSocket
                (socket, mAddr.getHostAddress(), mPort, true);
        }
        catch (IOException e)  {
            throw new SocketException(e.toString());
        }
        return CheckedSocket.check(socket);
    }    
}
