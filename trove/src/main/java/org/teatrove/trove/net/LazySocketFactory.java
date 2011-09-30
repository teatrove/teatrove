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
 * A socket implementation that lazily establishs a connection. It only
 * connects when actually needed. Setting options and getting I/O streams will
 * not force a connection to be established. As soon as a read or write
 * operation is performed, a connection is established.
 * <p>
 * If the first write operation requires a connection to be established, then a
 * recycled connection is requested. The connection is tested by writing the
 * data to it. If this fails, a new connection is requested and the operation
 * is tried again.
 * 
 * @author Brian S O'Neill
 */
public class LazySocketFactory implements SocketFactory {
    private final SocketFactory mFactory;

    public LazySocketFactory(SocketFactory factory) {
        mFactory = factory;
    }

    public InetAddressAndPort getInetAddressAndPort() {
        return mFactory.getInetAddressAndPort();
    }

    public InetAddressAndPort getInetAddressAndPort(Object session) {
        return mFactory.getInetAddressAndPort(session);
    }
    
    public long getDefaultTimeout() {
        return mFactory.getDefaultTimeout();
    }

    public CheckedSocket createSocket()
        throws ConnectException, SocketException
    {
        return mFactory.createSocket();
    }

    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(session);
    }

    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(timeout);
    }

    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(session, timeout);
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket() throws ConnectException, SocketException {
        return CheckedSocket.check(new LazySocket(mFactory));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, session));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, timeout));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, session, timeout));
    }

    public void recycleSocket(CheckedSocket cs)
        throws SocketException, IllegalArgumentException
    {
        if (cs == null) {
            return;
        }

        // Bust through two layers of wrapping to get at actual socket.

        SocketFace s = cs.mSocket;
        if (s instanceof LazySocket) {
            cs = ((LazySocket)s).recycle();
            if (cs == null) {
                return;
            }
        }

        mFactory.recycleSocket(cs);
    }

    public void clear() {
        mFactory.clear();
    }

    public int getAvailableCount(){
        return mFactory.getAvailableCount();
    }
}
