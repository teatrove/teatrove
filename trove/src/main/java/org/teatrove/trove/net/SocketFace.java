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

/**
 * An interface to a Socket. Wouldn't it be nice if java.net.Socket was
 * already an interface?
 *
 * @author Brian S O'Neill
 */
public interface SocketFace {
    InetAddress getInetAddress();

    InetAddress getLocalAddress();

    int getPort();

    int getLocalPort();

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    void setTcpNoDelay(boolean on) throws SocketException;

    boolean getTcpNoDelay() throws SocketException;

    void setSoLinger(boolean on, int linger) throws SocketException;

    int getSoLinger() throws SocketException;

    void setSoTimeout(int timeout) throws SocketException;

    int getSoTimeout() throws SocketException;

    void setSendBufferSize(int size) throws SocketException;

    int getSendBufferSize() throws SocketException;

    void setReceiveBufferSize(int size) throws SocketException;

    int getReceiveBufferSize() throws SocketException;

    void setKeepAlive(boolean on) throws SocketException;

    boolean getKeepAlive() throws SocketException;

    void close() throws IOException;

    void shutdownInput() throws IOException;

    void shutdownOutput() throws IOException;
}
