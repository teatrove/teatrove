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

package org.teatrove.barista.udp;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ServerSocketFactory;
import org.teatrove.barista.util.ConfigWrapper;
import org.teatrove.barista.util.Config;
import org.teatrove.barista.util.DerivedConfig;
import org.teatrove.barista.http.Utils;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.LogEvent;
import org.teatrove.trove.util.NoThreadException;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.net.SocketFace;
import org.teatrove.trove.net.SocketWrapper;
import org.teatrove.trove.net.CheckedSocket;
import org.teatrove.trove.net.BufferedSocket;
import org.teatrove.trove.net.LineTooLongException;
import org.teatrove.trove.util.ThreadPool;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.Transaction;
import org.teatrove.trove.util.tq.UncaughtExceptionListener;
import org.teatrove.trove.util.tq.UncaughtExceptionEvent;

/**
 * UdpHandler implements the UDP protocol. The UdpHandler creates a
 * high priority, non-daemon thread that accepts DatagramPacket from socket 
 * connections on a
 * designated port. The packets are placed in a
 * {@link TransactionQueue} to be processed as soon as a thread is available.
 *
 * @author tammy wang
 */
public class UdpHandler {
    protected Log mLog;
   
    private Config mConfig;
    private String mScheme;
    
 
    private int mOpenSocketCounter;
    private Object mOpenSocketLock = new Object();
	
	private UdpHandlerStage[] mStages;

    private int mNewReadTimeout;
    private int mPersistentReadTimeout;

    private int mSocketReadBufferSize;
    private int mSocketWriteBufferSize;

    private TransactionQueue mNewQueue;
    private TransactionQueue mPersistentQueue;

    private SocketTracker[] mSocketTrackers;
    private DatagramSocket mSS;
    private Thread mAcceptThread;

    /**
     * UdpHandler immediatly {@link #init(Config) initializes} itself using
     * the passed in config.
     */ 
    public UdpHandler(Config config) throws Exception {
        init(config);
    }

    /**
     * UdpHandler must be {@link #init(Config) initialized} before it can be
     * used.
     */
    public UdpHandler() {
    }

    /**
     * Pass in null if this UdpHandler is being removed from service and
     * should clean up after itself.
     */
    public synchronized void init(Config config) throws Exception {
        if (config == null) {
            destroy();
            return;
        }

        mConfig = config;
        mLog = config.getLog();
		
		mStages = config.getStages();
        
        setNewSocketReadTimeout(config.getNewSocketReadTimeout());
        setPersistentSocketReadTimeout
            (config.getPersistentSocketReadTimeout());
        setSocketWriteBufferSize(config.getSocketWriteBufferSize());
        setSocketReadBufferSize(config.getSocketReadBufferSize());

        mNewQueue = config.getNewSocketQueue();
        mPersistentQueue = config.getPersistentSocketQueue();
       
               
        setServerSocket(config.getServerSocket());
		mSocketReadBufferSize = 1024;//default to 1K;

        
    }

    public UdpHandler.Config getConfig() {
        return mConfig;
    }

    
    /**
     * Returns -1 if this UdpHandler is currently disabled. To enable, call
     * setServerSocket.
     */
    public synchronized int getPort() {
        if (mSS == null) {
            return -1;
        }
        else {
            return mSS.getLocalPort();
        }
    }
	
	 public UdpHandlerStage[] getStages() {
        return (UdpHandlerStage[])mStages.clone();
    }

    /**
     * Set the ServerSocket to listen to for incoming connections. If set to
     * null, then this UdpHandler is disabled until a ServerSocket is set.
     * The old ServerSocket is returned so that any backlogged connections
     * can still be processed or closed.
     *
     * @return the old ServerSocket being used or null if was disabled.
     */
    public synchronized DatagramSocket setServerSocket(DatagramSocket ss)
        throws InterruptedException, NoThreadException
    {
        DatagramSocket old = mSS;

        if (ss != old) {
            // Stop existing thread.
            if (mAcceptThread != null) {
                mAcceptThread.interrupt();
            }
            
            if (ss != null) {
                mSS = ss;
                String name = "UDP " +
                    ss.getLocalAddress().getHostAddress() + ':' +
                    getPort() + " accept";
                mAcceptThread = new Thread
                    (mConfig.getThreadPool(), new AcceptLoop(), name);
                mAcceptThread.setPriority(Thread.MAX_PRIORITY - 1);
                mAcceptThread.start();
                
                // Notify for the getServerSocket() method.
                notify();
            }
        }

        return old;
    }


    public int getNewSocketReadTimeout() {
        return mNewReadTimeout;
    }

    public void setNewSocketReadTimeout(int timeout) {
        if (timeout < 0) {
            mNewReadTimeout = 0;
        }
        else if (timeout == 0) {
            mNewReadTimeout = 1;
        }
        else {
            mNewReadTimeout = timeout;
        }
    }

    public int getPersistentSocketReadTimeout() {
        return mPersistentReadTimeout;
    }

    public void setPersistentSocketReadTimeout(int timeout) {
        if (timeout < 0) {
            mPersistentReadTimeout = 0;
        }
        else if (timeout == 0) {
            mPersistentReadTimeout = 1;
        }
        else {
            mPersistentReadTimeout = timeout;
        }
    }

    /**
     * Returns 0 if uses socket's default.
     */
    public int getSocketWriteBufferSize() {
        return mSocketWriteBufferSize;
    }

    /**
     * Set to zero or a negative value to use socket's default.
     */
    public void setSocketWriteBufferSize(int size) {
        if (size <= 0) {
            mSocketWriteBufferSize = 0;
        }
        else {
            mSocketWriteBufferSize = size;
        }
    }

    /**
     * Returns 0 if uses socket's default.
     */
    public int getSocketReadBufferSize() {
        return mSocketReadBufferSize;
    }

    /**
     * Set to zero or a negative value to use socket's default.
     */
    public void setSocketReadBufferSize(int size) {
        if (size <= 0) {
            mSocketReadBufferSize = 0;
        }
        else {
            mSocketReadBufferSize = size;
        }
    }

    void adjustOpenSocketCount(int adjust) {
        synchronized (mOpenSocketLock) {
            mOpenSocketCounter += adjust;
            mOpenSocketLock.notify();
        }
    }

    private synchronized DatagramSocket getServerSocket()
        throws InterruptedException
    {
        // Wait for it...
        while (mSS == null) {
            wait();
        }
        return mSS;
    }

    private void destroy() {
        try {
            //setServerSocket(null).close();
        }
        catch (Exception e) {
            mLog.debug(e.toString());
        }
		
		 for (int i=0; i<mStages.length; i++) {
            try {
                mStages[i].init(null);
            }
            catch (Exception e) {
                mLog.debug(e);
            }
        }

        
    }

    public interface Config extends org.teatrove.barista.util.Config {
        /**
         * Returns a TransactionQueue for processing new socket connections.
         */
        public TransactionQueue getNewSocketQueue();

        /**
         * Returns a TransactionQueue for processing persistent socket
         * connections.
         */
        public TransactionQueue getPersistentSocketQueue();

        /**
         * Returns the ServerSocket to use for accepting connections.
         */
        public DatagramSocket getServerSocket();

        /**
         * Timeout, in milliseconds, for reads on new sockets.
         */
        public int getNewSocketReadTimeout();

        /**
         * Timeout, in milliseconds, for reads on persistent sockets.
         */
        public int getPersistentSocketReadTimeout();

        /**
         * Returns 0 if socket default should be used.
         */
        public int getSocketWriteBufferSize();

        /**
         * Returns 0 if socket default should be used.
         */
        public int getSocketReadBufferSize();
		
		 /**
         * Returns the handler stages that will be used to handle every HTTP
         * request that HttpHandler receives.
         */
        public UdpHandlerStage[] getStages();

    }

    public static class DefaultConfig extends ConfigWrapper implements Config {
        private TransactionQueue mNewQueue;
        private TransactionQueue mPerQueue;
        private DatagramSocket mSS;
		private UdpHandlerStage[] mStages;
             
        public DefaultConfig(org.teatrove.barista.util.Config config,
							 UdpHandlerStage[] stages) {
            super(config);
            PropertyMap props = getProperties();  
			mStages = prependStages
                (stages, new DerivedConfig(config, "stages"));         
         }

        /**
         * Use this constructor to reconfigure an active HttpHandler. Queues,
         * scheme and server socket settings will remain the same.
         */
        public DefaultConfig(Config originalConfig,
                             org.teatrove.barista.util.Config config,
							 UdpHandlerStage[] stages
                             ) {
            super(config);

            mNewQueue = originalConfig.getNewSocketQueue();
            mPerQueue = originalConfig.getPersistentSocketQueue();
            mSS = originalConfig.getServerSocket();
			mStages = prependStages
                (stages, new DerivedConfig(config, "stages"));
           

          }
		  
		/**
         * Prepends configurable stages to the array of required handler stages.
         */
        private UdpHandlerStage[] prependStages(UdpHandlerStage[] stages,
                                                 org.teatrove.barista.util.
                                                 Config stageConfig) {
            PropertyMap stageProps = stageConfig.getProperties();
            Set stageNames = stageProps.subMapKeySet();
            List allStages = new ArrayList();
            Iterator stageIt = stageNames.iterator();
            while (stageIt.hasNext()) {
                String stageName = (String)stageIt.next();
                getLog().info("Creating Stage: " + stageName);
                PropertyMap subStageProps = stageProps.subMap(stageName);
                String stageClassName = subStageProps.getString("class");
                //try {
                    if (stageClassName == null) {
                        //throw new StageCreationException("No stage classname specified", null);
                    }

                    UdpHandlerStage.Config subStageConfig =
                        new UdpHandlerStage.DefaultConfig
                        (new DerivedConfig(stageConfig, stageName,
                                           subStageProps.subMap("init"))
                         );

                    UdpHandlerStage stage =
                        createStage(stageClassName, subStageConfig);

                    if (stage != null) {
                        allStages.add(stage);
                    }
                //}
                //catch (StageCreationException sce) {
                    //getLog().warn(sce.getRootCause());
                //}
            }

            for (int i=0; i<stages.length; i++) {
                allStages.add(stages[i]);
            }

            getLog().debug("Returning " + allStages.size() + " stages.");
            return (UdpHandlerStage[])allStages.toArray(new UdpHandlerStage[0]);
        }

        private UdpHandlerStage createStage(String stageClassName,
                                                    UdpHandlerStage.Config stageConfig) 
                   
               {
                   UdpHandlerStage stageObj = null;
                   try {
                       if (stageClassName != null && stageClassName.length() > 0) {
                           Class stageClass = Class.forName(stageClassName);
                           if (stageClass != null) {
                               stageObj = (UdpHandlerStage)stageClass.newInstance();
                               if (stageObj != null) {
                                   getLog().info("Initializing " + stageObj);
                                   stageObj.init(stageConfig);
                               }
                           }
                       }
                   }
                   catch (Exception e) {
                       //throw new StageCreationException(e);
                   }
                   if (stageObj == null) {
                       //throw new StageCreationException("Stage could not be created",
                                                        //null);
                   }
                   return stageObj;
               }
		
        /**
         * Reads the sub-properties under "transactionQueue.new". The max queue
         * size defaults to 50, and the max thread count defaults to 200.
         */
        public TransactionQueue getNewSocketQueue() {
            if (mNewQueue == null) {
                mNewQueue = new TransactionQueue
                    (getThreadPool(), "new connections", 50, 200);
                mNewQueue.applyProperties
                    (getProperties().subMap("transactionQueue").subMap("new"));
                addExceptionListener(mNewQueue);
            }
            return mNewQueue;
        }

        /**
         * Reads the sub-properties under "transactionQueue.persistent". The
         * max queue size defaults to 10, and the max thread count defaults to
         * 200.
         */
        public TransactionQueue getPersistentSocketQueue() {
            if (mPerQueue == null) {
                mPerQueue = new TransactionQueue
                    (getThreadPool(), "persistent connections", 10, 200);
                mPerQueue.applyProperties
                    (getProperties().subMap("transactionQueue").subMap
                     ("persistent"));
                addExceptionListener(mPerQueue);
            }
            return mPerQueue;
        }

        public UdpHandlerStage[] getStages() {
            return mStages;
        }
       
        /**
         * Reads the properties "socket.port", "socket.backlog" and
         * "socket.bind". The port defaults to 80, the backlog to 200, and the
         * bind address to the system default. The bind property may specify
         * a comma or semi-colon separated list of bind addresses. The first
         * one found that works is chosen.
         */
        public DatagramSocket getServerSocket() {
            if (mSS != null) {
                return mSS;
            }

            PropertyMap properties = getProperties().subMap("socket");
            int port;
            port = properties.getInt("port", 88);
            //int backlog = properties.getInt("backlog", 200);
            Object[] addrs = getAllAddresses(properties.getString("bind"));
            ServerSocketFactory factory = null;
            String factoryClassname = properties.getString("factory");
            if (factoryClassname != null) {
                try {
                    Class factoryClass = Class.forName(factoryClassname);
                    if (factoryClass != null) {
                        Object factoryObj = factoryClass.newInstance();
                        if (factoryObj != null) {
                            factory = (ServerSocketFactory)factoryObj;
                        }
                    }
                }
                catch (ClassNotFoundException cnfe) {
                    getLog().warn(cnfe);
                }
                catch (InstantiationException ie) {
                    getLog().warn(ie);
                }
                catch (IllegalAccessException iae) {
                    getLog().warn(iae);
                }
                catch (ClassCastException cce) {
                    getLog().warn(cce);
                }
            }

            if (factory == null) {
                factory = ServerSocketFactory.getDefault();
            }

            mSS = openServerSocket(factory, port, addrs);

            if (mSS == null) {
                getLog().error("Unable to bind server socket on port " + port);
                for (int i=0; i<addrs.length; i++) {
                    if (addrs[i] instanceof Exception) {
                        getLog().error(addrs[i].toString());
                        // Just report first failure.
                        break;
                    }
                }
            }
            else {
                getLog().info
                    ("Server socket bound to: " + mSS.getLocalAddress() +
                     " on port " + port);
            }

            return mSS;
        }

        /**
         * Reads the property "socket.timeout.read.new", but defaults to 15000.
         */
        public int getNewSocketReadTimeout() {
            return getProperties().getInt("socket.timeout.read.new", 15000);
        }

        /**
         * Reads the property "socket.timeout.read.persistent", but defaults to
         * 15000.
         */
        public int getPersistentSocketReadTimeout() {
            return getProperties().getInt
                ("socket.timeout.read.persistent", 15000);
        }

        /**
         * Reads the property "socket.buffer.write", but defaults to 65536.
         */
        public int getSocketWriteBufferSize() {
            return getProperties().getInt("socket.buffer.write", 65536);
        }

        /**
         * Reads the property "socket.buffer.read", but defaults to 0, which in
         * turn uses the socket's default.
         */
        public int getSocketReadBufferSize() {
            return getProperties().getInt("socket.buffer.read", 0);
        }

       
        private void addExceptionListener(TransactionQueue queue) {
            UncaughtExceptionListener list = new UncaughtExceptionListener() {
                public void uncaughtException(UncaughtExceptionEvent event) {
                    Throwable e = event.getException();
                    int type;
                    if (e instanceof IOException) {
                        type = LogEvent.INFO_TYPE;
                    }
                    else {
                        type = LogEvent.ERROR_TYPE;
                    }
                    
                    Log log = getLog();

                    log.logException(new LogEvent(log, type, e,
                                                  event.getThread()));
                }
            };

            queue.addUncaughtExceptionListener(list);
        }

        /**
         * Returns an array of InetAddresses or UnknownHostExceptions. Array
         * has at least one element.
         */
        private Object[] getAllAddresses(String names) {
            List addresses = new ArrayList();

            if (names != null) {
                StringTokenizer st = new StringTokenizer(names, " ,;");
                while (st.hasMoreTokens()) {
                    String name = st.nextToken();
                    try {
                        InetAddress[] addrs = InetAddress.getAllByName(name);
                        for (int i=0; i<addrs.length; i++) {
                            addresses.add(addrs[i]);
                        }
                    }
                    catch (UnknownHostException e) {
                        addresses.add(e);
                    }
                }
            }

            if (addresses.isEmpty()) {
                try {
                    addresses.add(InetAddress.getLocalHost());
                }
                catch (UnknownHostException e) {
                    addresses.add(e);
                }
            }

            return addresses.toArray();
        }

        /**
         * Returns null if server socket couldn't be opened. Any exceptions
         * are stored in addrs array.
         */
        private DatagramSocket openServerSocket
            (final ServerSocketFactory factory,
             final int port, 
             final Object[] addrs)
        {
            final List binders = new ArrayList(addrs.length);

            for (int i=0; i<addrs.length; i++) {
                if (!(addrs[i] instanceof InetAddress)) {
                    continue;
                }

                final int index = i;

                Thread binder = new Thread("binder " + addrs[i]) {
                    public void run() {
                        try {
                            //addrs[index] = factory.createServerSocket
                                //(port,backlog,(InetAddress)addrs[index]);
							addrs[index] = new DatagramSocket(port,(InetAddress)addrs[index]);
                        }
                        catch (BindException e) {
                            addrs[index] = new BindException
                            ("Unable to bind to " + addrs[index]);
                        }
                        catch (IOException e) {
                            addrs[index] = e;
                        }
                    }
                };

                binders.add(binder);
                binder.start();
            }

            // Wait for all the binders to finish.
            Thread monitor = new Thread("Binder monitor") {
                public void run() {
                    for (int i=0; i<binders.size(); i++) {
                        Thread binder = (Thread)binders.get(i);
                        if (Thread.currentThread().isInterrupted()) {
                            binder.interrupt();
                        }
                        else {
                            try {
                                ((Thread)binders.get(i)).join();
                            }
                            catch (InterruptedException ie) {
                            }
                        }
                    }
                }
            };

            monitor.start();
            try {
                // Wait at most 15 seconds for all binders to respond.
                monitor.join(15000);
            }
            catch (InterruptedException ie) {
            }
            monitor.interrupt();

            DatagramSocket ss = null;

            for (int i=0; i<addrs.length; i++) {
                Object obj = addrs[i];
                if (obj instanceof DatagramSocket) {
                    if (ss == null) {
                        ss = (DatagramSocket)obj;
                    }
                    else {
                        
                            ((DatagramSocket)obj).close();
                        
                    }
                }
            }

            return ss;
        }
    }

    private class UdpTransaction
        implements Transaction, UdpServerConnectionImpl.Recycler
    {
        private DatagramSocket mSocket;
		private DatagramPacket mPacket;

        public UdpTransaction(DatagramSocket socket) {
            mSocket = socket;
        }
		
		
		public UdpTransaction(DatagramSocket socket, DatagramPacket packet) {
			mSocket = socket;
            mPacket = packet;
        }
        public void service() throws Exception {
            UdpServerConnection con;

            try {
                con = new UdpServerConnectionImpl
                    (mSocket, mPacket, this);
					
				con.setRequestPacket(mPacket);	

				//TODO: add servletpool for persisitentQueue later
                /*TransactionQueue ptq = mPersistentQueue;
                boolean doClose;
                synchronized (ptq) {
                    doClose = ptq.getThreadCount() + ptq.getQueueSize() >=
                        ptq.getMaximumThreads();
                }

                if (doClose) {
                    // Too many persistent connections, so ensure this one
                    // closes.
                    //con.getResponseHeaders().put("Connection", "Close");
                }*/
            }catch (Exception e) {
                cancel();
                return;
            }

            
		    //mLog.info("handle this request");
            UdpHandlerStage.Chain chain = new UdpHandlerStage.Chain() {
                private int mCursor;

                public boolean doNextStage(UdpServerConnection con) 
                throws Exception {
                    UdpHandlerStage[] stages = mStages;
                    if (mCursor < stages.length) {
                        return stages[mCursor++].handle(con, this);
                    }
                    else {
                        return false;
                    }
                }
            };

            try {
                if (!chain.doNextStage(con)) {
                    
                    con.getSocket().close();
                }
            }
            catch (Exception e) {
                mLog.error(e);
                //con.setResponseStatus(500, null);
                con.getSocket().close();
            }


           
     }

        public void cancel() {
            try {
                mSocket.close();
            }
            catch (Exception e) {
                mLog.error(e);
            }
        }

        public void recycleSocket(DatagramSocket socket) {
            mSocket = socket;

            try {
                socket.setSoTimeout(getPersistentSocketReadTimeout());
            }
            catch (SocketException e) {
                mLog.warn(e);
            }

            if (!mPersistentQueue.enqueue(this)) {
                cancel();
            }
        }
    }

    private class AcceptLoop implements Runnable {
        public void run() {
            DatagramSocket ss;
            try {
                ss = getServerSocket();
            }
            catch (InterruptedException e) {
                return;
            }

            SocketTrackingTask task;
            if (mSocketTrackers != null && mSocketTrackers.length > 0){
                task = new SocketTrackingTask();
                task.setPriority(Thread.NORM_PRIORITY);
                task.start();
            }
            else {
                task = null;
            }

            while (!Thread.interrupted()) {
                try {
                    //SocketFace socket = null;
                    try {
					     
                    	byte[] buf = new byte[mSocketReadBufferSize]; 
                    	DatagramPacket packet = new DatagramPacket(buf, buf.length); 
						ss.receive(packet); 
						Transaction tran = new UdpTransaction(ss,packet);
                        
                        if (!mNewQueue.enqueue(tran)) {
                            tran.cancel();
                        }
                    }
                    catch (InterruptedIOException e) {
                        mLog.debug(e.toString());
                        e = null;
                        close(ss);
                        continue;
                    }
                    catch (SocketException e) {
                        mLog.debug(e.toString());
                        e = null;
                        close(ss);
                        // Yield to be nice, just in case if in a tail spin.
                        Thread.yield();
                    }
                    catch (Throwable e) {
                        mLog.error(e);
                        e = null;
                        close(ss);
                        // Yield to be nice, just in case if in a tail spin.
                        Thread.yield();
                    }
                }
                catch (Throwable e) {
                    // Catch Throwable that may be thrown from error logging
                    // itself. I'm being really paranoid here. I never want
                    // this thread to die.
                    // Yield to be nice, just in case if in a tail spin.
                    Thread.yield();
                }
            }

            mLog.info("Exiting");

            if (task != null) {
                task.interrupt();
            }
        }

        private void close(DatagramSocket socket) {
            if (socket != null) {
                try {
                    socket.close();
                }
                catch (Exception e) {
                    mLog.debug(e.toString());
                }
            }
        }

        private void applyOptions(DatagramSocket socket) {
            // Try to disable broadcast.
            /*try {
                socket.setBroadcast(false);
            }
            catch (Exception e) {
                mLog.debug(e);
            }*/
            
            // Try to set the send buffer size.
            int bufSize = getSocketWriteBufferSize();
            if (bufSize > 0) {
                try {
                    socket.setSendBufferSize(bufSize);
                }
                catch (SocketException e) {
                    mLog.warn(e);
                }
            }

            // Try to set the receive buffer size.
            bufSize = getSocketReadBufferSize();
            if (bufSize > 0) {
                try {
                    socket.setReceiveBufferSize(bufSize);
                }
                catch (SocketException e) {
                    mLog.warn(e);
                }
            }

            // Try to set the read timeout.
            try {
                socket.setSoTimeout(getNewSocketReadTimeout());
            }
            catch (Exception e) {
                mLog.warn(e);
            }
        }
    }

    private class TrackedSocket extends CheckedSocket {
        private boolean mDecremented;
        private InputStream mIn;
        private OutputStream mOut;

        TrackedSocket(SocketFace s) throws IOException {
            super(s);
            adjustOpenSocketCount(1);
            mIn = new Input(super.getInputStream());
            mOut = new Output(super.getOutputStream());
        }
        
        public InputStream getInputStream() throws IOException {
            return mIn;
        }
        
        public OutputStream getOutputStream() throws IOException {
            return mOut;
        }

        public void close() throws IOException {
            super.close();
            finalize();
        }

        public void finalize() {
            if (!mDecremented) {
                adjustOpenSocketCount(-1);
                mDecremented = true;
            }
        }

        private class Input extends FilterInputStream {
            Input(InputStream in) {
                super(in);
            }
            
            public void close() throws IOException {
                in.close();
                TrackedSocket.this.finalize();
            }
        }
        
        private class Output extends FilterOutputStream {
            Output(OutputStream out) {
                super(out);
            }
            
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            public void close() throws IOException {
                out.close();
                TrackedSocket.this.finalize();
            }
        }
    }

    private class SocketTrackingTask extends Thread {
        SocketTrackingTask() {
            super("Socket Tracking Task");
        }
        
        public void run() {
            try {
                int oldCount = 0;
                while (true) {
                    int count;
                    synchronized(mOpenSocketLock) {
                        mOpenSocketLock.wait();
                        count = mOpenSocketCounter;
                    }
                    if (count != oldCount) {
                        for (int j=0; j<mSocketTrackers.length; j++) {
                            mSocketTrackers[j].updateSocketCount(count);
                        }
                        oldCount = count;
                    }
                }
            }
            catch (InterruptedException e) {
            }
        }
    }

 
}
