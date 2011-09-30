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

import java.net.InetAddress;

/**
 * Wrapper for using secure sockets.
 *
 * @author Jonathan Colwell
 */
public class MultiPooledSSLSocketFactory extends MultiPooledSocketFactory {
    public MultiPooledSSLSocketFactory(String[] hosts, 
                                       int[] ports, 
                                       long timeout) {
        super(hosts, ports, timeout);
    }

    public MultiPooledSSLSocketFactory(String[] hosts, 
                                       int[] ports, 
                                       long timeout,
                                       java.util.Random rnd) {
        super(hosts, ports, timeout, rnd);
    }

    public MultiPooledSSLSocketFactory(String[] hosts, 
                                       int[] ports, 
                                       InetAddress localAddr,
                                       int localPort,
                                       long timeout) {
        super(hosts, ports, localAddr, localPort, timeout);
    }

    public MultiPooledSSLSocketFactory(String[] hosts, 
                                       int[] ports, 
                                       InetAddress localAddr,
                                       int localPort,
                                       long timeout,
                                       java.util.Random rnd) {
        super(hosts, ports, localAddr, localPort, timeout, rnd);
    }

    /**
     * Create socket factories for newly resolved addresses. Default
     * implementation returns a LazySocketFactory wrapping a
     * PooledSocketFactory wrapping an SSLSocketFactory.
     */
    protected SocketFactory createSocketFactory(InetAddress address,
                                                int port,
                                                InetAddress localAddr,
                                                int localPort,
                                                long timeout)
    {
        SocketFactory factory;
        factory = new SSLSocketFactory
            (address, port, localAddr, localPort, timeout);
        factory = new PooledSocketFactory(factory);
        factory = new LazySocketFactory(factory);
        return factory;
    }
}

