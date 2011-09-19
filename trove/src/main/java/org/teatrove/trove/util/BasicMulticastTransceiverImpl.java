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

package org.teatrove.trove.util;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.teatrove.trove.util.MessageTransceiver;
import org.teatrove.trove.util.MessageListener;

/**
 *  Simple multicast messaging implementation.  This should not be used in
 *  production, in favor of a more reliable, routable solution.
 *
 *  Author: Guy Molinari
 */
public class BasicMulticastTransceiverImpl implements MessageTransceiver {

    private ArrayList mListeners = new ArrayList();
    private InetAddress mGroup = null;
    private InetAddress mBindAddress = null;
    private int mPort = 0;
    private MulticastSocket mSocket = null;
    private PollThread mPollThread = null;
    private static final int SOCKET_POLL_INTERVAL = 100;   // 100ms

    BasicMulticastTransceiverImpl(InetAddress group, int port,
            InetAddress bindAddress) throws MessageException {

        mGroup = group;
        mBindAddress = bindAddress;
        mPort = port;
        mPollThread = new PollThread();
        Thread t = new Thread(mPollThread);
        t.start();

        try {
            mSocket = new MulticastSocket(new InetSocketAddress(
                mGroup, mPort));
            mSocket.setSoTimeout(SOCKET_POLL_INTERVAL);
        }
        catch (Exception e) {
            throw new MessageException("Cannot create socket on " +
                mGroup + ":" + mPort, e);
        }

        if (mBindAddress != null) {
            try {
                mSocket.setInterface(mBindAddress);
            }
            catch (Exception e) {
                throw new MessageException("Cannot bind socket on interface " +
                    mBindAddress, e);
            }
        }

        try {
            mSocket.joinGroup(mGroup);
        }
        catch (Exception e) {
            throw new MessageException("Cannot join group on " +
                mGroup + ":" + mPort, e);
        }

    }


    public void shutdown() {
        mPollThread.stop();
    }

    
    public void addMessageListener(MessageListener l) {
        mListeners.add(l);
    }

    
    public void removeMessageListener(MessageListener l) {
        mListeners.remove(l);
    }


    public void send(Serializable message) throws MessageException {

        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buf);
            out.writeObject(message);
            mSocket.send(new DatagramPacket(buf.toByteArray(), buf.size(), mGroup, mPort));
        }
        catch (Exception e) {
            throw new MessageException("Cannot send message on " +
                mGroup + ":" + mPort, e);
        }

    }

    
    private Serializable poll() {

        try {
            byte[] buf = new byte[4096];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            mSocket.receive(p);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
            return (Serializable) in.readObject();
        }
        catch (SocketTimeoutException e) {
            return null;
        }
        catch (Exception e) {
            throw new RuntimeException("Message receive failure on " +
                mGroup + ":" + mPort, e);
        }

    }


    private class PollThread implements Runnable {

        private boolean mRun = true;

        public void run() {
            while (mRun) {
                Serializable o = poll();
                if (o != null) {
                    for (Iterator i = mListeners.iterator(); i.hasNext(); ) {
                        MessageListener l = (MessageListener) i.next();
                        l.onMessage(o);
                    }
                }
                    
            }
        }


        public void stop() {
            mRun = false;
            try {
                Thread.sleep(SOCKET_POLL_INTERVAL + 1);
            }
            catch (InterruptedException ignore) {}
        }
    }

}
