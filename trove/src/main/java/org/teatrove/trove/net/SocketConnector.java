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
import org.teatrove.trove.util.*;

/**
 * Allows client socket connections to be established with a timeout.
 *
 * @author Brian S O'Neill
 */
public class SocketConnector {
    // Limit the number of threads that may simultaneously connect to a
    // specific destination.
    private static final int CONNECT_THREAD_MAX = 5;

    // Maps address:port pairs to ThreadPools for connecting.
    private static Map mConnectors =
        Collections.synchronizedMap(new SoftHashMap());

    /**
     * @param host Remote host to connect to
     * @param port Remote port to connect to
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(String host, int port, long timeout)
        throws SocketException
    {
        return connect((Object)host, port, null, 0, timeout);
    }

    /**
     * @param address Remote address to connect to
     * @param port Remote port to connect to
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(InetAddress address, int port, long timeout)
        throws SocketException
    {
        return connect((Object)address, port, null, 0, timeout);
    }

    /**
     * @param host Remote host to connect to
     * @param port Remote port to connect to
     * @param localAddress Local address to bind to
     * @param localPort Local port to bind to, 0 for any
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(String host, int port,
                                 InetAddress localAddress, int localPort,
                                 long timeout)
        throws SocketException
    {
        return connect((Object)host, port, localAddress, localPort, timeout);
    }

    /**
     * @param address Remote address to connect to
     * @param port Remote port to connect to
     * @param localAddress Local address to bind to
     * @param localPort Local port to bind to, 0 for any
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(InetAddress address, int port,
                                 InetAddress localAddress, int localPort,
                                 long timeout)
        throws SocketException
    {
        return connect((Object)address, port, localAddress, localPort,timeout);
    }

    /**
     * @param address either a string or InetAddress.
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time
     */
    private static Socket connect(Object address, int port,
                                  InetAddress localAddress, int localPort,
                                  long timeout)
        throws SocketException
    {
        Key key = new Key(address, port, localAddress, localPort);
        ThreadPool pool = getThreadPool(key);
        Connector connector = new Connector(key);
        Thread thread;

        long start;
        if (timeout > 0) {
            start = System.currentTimeMillis();
        }
        else {
            start = 0;
        }

        try {
            thread = pool.start(connector, timeout);
        }
        catch (InterruptedException e) {
            return null;
        }
        catch (IllegalThreadStateException e) {
            // Pool might have been destroyed.
            pool = getNewThreadPool(key, pool);
            try {
                thread = pool.start(connector, timeout);
            }
            catch (InterruptedException e2) {
                return null;
            }
        }

        if (timeout > 0) {
            timeout = timeout - (System.currentTimeMillis() - start);
            if (timeout < 0) {
                timeout = 0;
            }
        }

        try {
            Socket socket = connector.connect(timeout);
            if (socket != null) {
                return socket;
            }
        }
        catch (InterruptedException e) {
        }

        thread.interrupt();
        return null;
    }

    private static ThreadPool getNewThreadPool(Object key, ThreadPool old) {
        synchronized (mConnectors) {
            if (mConnectors.get(key) == old) {
                mConnectors.remove(key);
            }
            return getThreadPool(key);
        }
    }

    private static ThreadPool getThreadPool(Object key) {
        ThreadPool pool;
        synchronized (mConnectors) {
            pool = (ThreadPool)mConnectors.get(key);
            if (pool == null) {
                pool = new ThreadPool
                    ("SocketConnector[" + key + ']', CONNECT_THREAD_MAX);
                pool.setIdleTimeout(10000);
                mConnectors.put(key, pool);
            }
        }
        return pool;
    }

    private SocketConnector() {
    }

    private static class Key {
        final Object mAddress;
        final int mPort;
        final InetAddress mLocalAddress;
        final int mLocalPort;

        Key(Object address, int port,
            InetAddress localAddress, int localPort)
        {
            mAddress = address;
            mPort = port;
            mLocalAddress = localAddress;
            mLocalPort = localPort;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key key = (Key)obj;
                return
                    key.mAddress.equals(mAddress) && (key.mPort == mPort) &&
                    ((key.mLocalAddress == null) ? (mLocalAddress == null)
                     : key.mLocalAddress.equals(mLocalAddress)) &&
                    (key.mLocalPort == mLocalPort);
            }
            return false;
        }

        public int hashCode() {
            return mAddress.hashCode() + mPort;
        }

        public String toString() {
            if (mAddress instanceof InetAddress) {
                return ((InetAddress)mAddress).getHostAddress() + ':' + mPort;
            }
            else {
                return String.valueOf(mAddress) + ':' + mPort;
            }
        }
    }

    private static class Connector implements Runnable {
        private final Key mKey;
        private Object mSocketOrException;
        private boolean mDoneWaiting;

        public Connector(Key key) {
            mKey = key;
        }

        public synchronized Socket connect(long timeout)
            throws SocketException, InterruptedException
        {
            try {
                if (mSocketOrException == null) {
                    if (timeout < 0) {
                        wait();
                    }
                    else if (timeout > 0) {
                        wait(timeout);
                    }
                    else {
                        return null;
                    }
                }
            }
            finally {
                mDoneWaiting = true;
            }

            if (mSocketOrException instanceof Socket) {
                return (Socket)mSocketOrException;
            }
            else if (mSocketOrException instanceof InterruptedIOException) {
                throw new InterruptedException();
            }
            else if (mSocketOrException instanceof Exception) {
                throw new SocketException
                    ("Unable to connect to " + mKey + ", " +
                     ((Exception)mSocketOrException).getMessage());
            }

            return null;
        }

        public void run() {
            try {
                Socket socket;
                Object address = mKey.mAddress;
                InetAddress localAddress = mKey.mLocalAddress;
                if (address instanceof InetAddress) {
                    if (localAddress == null) {
                        socket = new Socket((InetAddress)address, mKey.mPort);
                    }
                    else {
                        socket = new Socket((InetAddress)address, mKey.mPort,
                                            localAddress, mKey.mLocalPort);
                    }
                }
                else {
                    if (localAddress == null) {
                        socket =
                            new Socket(String.valueOf(address), mKey.mPort);
                    }
                    else {
                        socket =
                            new Socket(String.valueOf(address), mKey.mPort,
                                       localAddress, mKey.mLocalPort);
                    }
                }

                synchronized (this) {
                    if (mDoneWaiting) {
                        try {
                            socket.close();
                        }
                        catch (IOException e) {
                        }
                    }
                    else {
                        mSocketOrException = socket;
                        notify();
                    }
                }
            }
            catch (Exception e) {
                synchronized (this) {
                    mSocketOrException = e;
                    notify();
                }
            }
        }
    }
}
