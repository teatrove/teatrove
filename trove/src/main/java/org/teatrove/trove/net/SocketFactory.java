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
 * Allows client sockets to be created or recycled.
 *
 * @author Brian S O'Neill
 */
public interface SocketFactory {
    /**
     * Returns the InetAddress and port that this factory will most likely
     * connect to. If the address isn't precisely known, its value is 0.0.0.0.
     * If the port isn't known, its value is -1.
     */
    public InetAddressAndPort getInetAddressAndPort();

    /**
     * Returns the InetAddress and port that this factory will most likely
     * connect to. If the address isn't precisely known, its value is 0.0.0.0.
     * If the port isn't known, its value is -1.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     */
    public InetAddressAndPort getInetAddressAndPort(Object session);
    
    /**
     * Returns the default timeout for creating or getting sockets or -1 if
     * infinite.
     */
    public long getDefaultTimeout();

    /**
     * Must always return a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket()
        throws ConnectException, SocketException;

    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException;

    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be created before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException;
    
    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to become available before throwing an exception
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket() throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be returned before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be returned before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException;

    /**
     * Recycle a socket connection that was returned from the {@link getSocket}
     * or {@link createSocket} methods. Since SocketFactory has no knowledge of
     * any protocol being used on the socket, it is the responsibility of the
     * caller to ensure the socket is in a "clean" state. Depending on
     * implementation, the recycled socket may simply be closed.
     *
     * @param socket Socket which must have come from this factory. Passing in
     * null is ignored.
     */
    public void recycleSocket(CheckedSocket socket)
        throws SocketException, IllegalArgumentException;

    /**
     * Closes all recycled connections, but does not prevent new connections
     * from being created and recycled.
     */
    public void clear();

    /**
     * Returns the number of recycled sockets currently available.
     */    
    public int getAvailableCount();
}
