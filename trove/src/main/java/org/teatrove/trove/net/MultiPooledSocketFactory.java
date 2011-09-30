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
import java.util.*;

/**
 * Pooled SocketFactory implementation that connects to multiple hosts that may
 * resolve to multiple InetAddresses. If running under Java 2, version 1.3,
 * changes in the address resolution are automatically detected using
 * {@link InetAddressResolver}.
 * <p>
 * Consider wrapping with a {@link LazySocketFactory} for automatic checking
 * against socket factories that may be dead.
 * 
 * @author Brian S O'Neill
 */
public class MultiPooledSocketFactory extends DistributedSocketFactory {
    final InetAddress mLocalAddr;
    final int mLocalPort;

    // Just references InetAddressResolvers to keep them from going away.
    private Object[] mResolvers;

    /**
     * @param hosts hosts to connect to; length matches ports
     * @param ports ports to connect to; length matches hosts
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public MultiPooledSocketFactory(String[] hosts,
                                    int[] ports,
                                    long timeout) {
        this(hosts, ports, null, 0, timeout, null);
    }


    /**
     * @param hosts hosts to connect to; length matches ports
     * @param ports ports to connect to; length matches hosts
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     * @param rnd Optional random number generator if random distribution is
     * desired.
     */
    public MultiPooledSocketFactory(String[] hosts,
                                    int[] ports,
                                    long timeout,
                                    Random rnd)
    {
        this(hosts, ports, null, 0, timeout, rnd);
    }

    /**
     * @param hosts hosts to connect to; length matches ports
     * @param ports ports to connect to; length matches hosts
     * @param localAddr Local address to bind new sockets to
     * @param localPort Local port to bind new sockets to, 0 for any
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public MultiPooledSocketFactory(String[] hosts,
                                    int[] ports,
                                    InetAddress localAddr,
                                    int localPort,
                                    long timeout) {
        this(hosts, ports, localAddr, localPort, timeout, null);
    }


    /**
     * @param hosts hosts to connect to; length matches ports
     * @param ports ports to connect to; length matches hosts
     * @param localAddr Local address to bind new sockets to
     * @param localPort Local port to bind new sockets to, 0 for any
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     * @param rnd Optional random number generator if random distribution is
     * desired.
     */
    public MultiPooledSocketFactory(String[] hosts,
                                    int[] ports,
                                    InetAddress localAddr,
                                    int localPort,
                                    long timeout,
                                    Random rnd)
    {
        super(timeout, rnd);

        mLocalAddr = localAddr;
        mLocalPort = localPort;

        try {
            mResolvers = new InetAddressResolver[hosts.length];
            for (int i=0; i<hosts.length; i++) {
                Listener listener = new Listener(ports[i], timeout);
                mResolvers[i] =
                    InetAddressResolver.listenFor(hosts[i], listener);
            }
        }
        catch (NoClassDefFoundError e) {
            // Timer class probably wasn't found, so InetAddressResolver
            // cannot be used.
            mResolvers = null;
            for (int i=0; i<hosts.length; i++) {
                Listener listener = new Listener(ports[i], timeout);
                try {
                    listener.resolved(InetAddress.getAllByName(hosts[i]));
                }
                catch (UnknownHostException e2) {
                    listener.unknown(e2);
                }
            }
        }
    }

    /**
     * Create socket factories for newly resolved addresses. Default
     * implementation returns a LazySocketFactory wrapping a
     * PooledSocketFactory wrapping a PlainSocketFactory.
     */
    protected SocketFactory createSocketFactory(InetAddress address,
                                                int port,
                                                InetAddress localAddr,
                                                int localPort,
                                                long timeout)
    {
        SocketFactory factory;
        factory = new PlainSocketFactory
            (address, port, localAddr, localPort, timeout);
        factory = new PooledSocketFactory(factory);
        factory = new LazySocketFactory(factory);
        return factory;
    }

    private class Listener implements InetAddressListener {
        private final int mPort;
        private final long mTimeout;

        // Maps InetAddress objects to SocketFactories.
        private Map mAddressedFactories;
    
        public Listener(int port, long timeout) {
            mPort = port;
            mTimeout = timeout;
            mAddressedFactories = new HashMap();
        }

        public void unknown(UnknownHostException e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);

            // Remove all the addressed factories.
            Iterator it = mAddressedFactories.keySet().iterator();
            while (it.hasNext()) {
                SocketFactory factory = 
                    (SocketFactory)mAddressedFactories.get(it.next());
                removeSocketFactory(factory);
            }
        }
        
        public void resolved(InetAddress[] addresses) {
            // Add newly discovered addresses.
            for (int i=0; i<addresses.length; i++) {
                InetAddress address = addresses[i];
                if (!mAddressedFactories.containsKey(address)) {
                    SocketFactory factory = createSocketFactory
                        (address, mPort, mLocalAddr, mLocalPort, mTimeout);
                    mAddressedFactories.put(address, factory);
                    addSocketFactory(factory);
                }
            }
            
            // Remove addresses no longer being routed to.
            Iterator it = mAddressedFactories.keySet().iterator();
            mainLoop:
            while (it.hasNext()) {
                InetAddress address = (InetAddress)it.next();
                for (int i=0; i<addresses.length; i++) {
                    if (addresses[i].equals(address)) {
                        continue mainLoop;
                    }
                }
                SocketFactory factory =
                    (SocketFactory)mAddressedFactories.get(address);
                removeSocketFactory(factory);
            }
        }
    }
}
