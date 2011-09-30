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
 * A Socket wrapper that passes all calls to an internal Socket. This class is
 * designed for subclasses to override or hook into the behavior of a Socket
 * instance.
 * 
 * @author Brian S O'Neill
 * @see SocketFaceWrapper
 */
public class SocketWrapper implements SocketFace {
    protected final Socket mSocket;

    public SocketWrapper(Socket socket) {
        mSocket = socket;
    }

    public InetAddress getInetAddress() {
        return mSocket.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return mSocket.getLocalAddress();
    }

    public int getPort() {
        return mSocket.getPort();
    }

    public int getLocalPort() {
        return mSocket.getLocalPort();
    }

    public InputStream getInputStream() throws IOException {
        return mSocket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return mSocket.getOutputStream();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        mSocket.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return mSocket.getTcpNoDelay();
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        mSocket.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return mSocket.getSoLinger();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        mSocket.setSoTimeout(timeout);
    }

    public int getSoTimeout() throws SocketException {
        return mSocket.getSoTimeout();
    }

    public void setSendBufferSize(int size) throws SocketException {
        mSocket.setSendBufferSize(size);
    }

    public int getSendBufferSize() throws SocketException {
        return mSocket.getSendBufferSize();
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        mSocket.setReceiveBufferSize(size);
    }

    public int getReceiveBufferSize() throws SocketException {
        return mSocket.getReceiveBufferSize();
    }

    public void setKeepAlive(boolean on) throws SocketException {
        mSocket.setKeepAlive(on);
    }

    public boolean getKeepAlive() throws SocketException {
        return mSocket.getKeepAlive();
    }

    public void close() throws IOException {
        mSocket.close();
    }

    public void shutdownInput() throws IOException {
        mSocket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        mSocket.shutdownOutput();
    }

    public String toString() {
        return mSocket.toString();
    }
}
