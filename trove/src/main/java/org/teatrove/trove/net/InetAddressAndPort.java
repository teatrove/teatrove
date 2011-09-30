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
 * 
 * @author Brian S O'Neill
 */
public class InetAddressAndPort {
    /**
     * A singleton instance representing an address of 0.0.0.0 and port of -1.
     */
    public static final InetAddressAndPort UNKNOWN;

    static {
        InetAddress addr;
        try {
            addr = InetAddress.getByName("0.0.0.0");
        }
        catch (UnknownHostException e) {
            // Not gonna happen.
            addr = null;
        }

        UNKNOWN = new InetAddressAndPort(addr, -1);
    }

    private final InetAddress mAddr;
    private final int mPort;

    public InetAddressAndPort(InetAddress addr, int port) {
        mAddr = addr;
        mPort = port;
    }

    public InetAddress getInetAddress() {
        return mAddr;
    }

    public int getPort() {
        return mPort;
    }

    public String toString() {
        return mAddr + ":" + mPort;
    }
}
