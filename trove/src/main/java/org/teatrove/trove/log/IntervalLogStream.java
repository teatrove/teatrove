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

package org.teatrove.trove.log;

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;

/**
 * IntervalLogStream writes to an underlying OutputStream that is opened once
 * per a specific time interval. This class forms the basis of a dated file 
 * logging mechanism.
 *
 * @author Brian S O'Neill
 * @see DailyFileLogStream
 */
public abstract class IntervalLogStream extends OutputStream {
    private static int cCounter;

    private static synchronized String nextName() {
        return "IntervalLogStream Auto Rollover " + cCounter++;
    }

    private Factory mFactory;
    private OutputStream mOut;
    private boolean mIsClosed;

    private Calendar mIntervalStart;
    private Calendar mNextIntervalStart;

    private Thread mRolloverThread;

    public IntervalLogStream(Factory factory) {
        mFactory = factory;
    }

    /**
     * Starts up a thread that automatically rolls the underlying OutputStream
     * at the beginning of the interval, even if no output is written.
     */
    public synchronized void startAutoRollover() {
        // TODO: If java.util.Timer class is available, use it instead of
        // creating a thread each time.
        if (mRolloverThread == null) {
            mRolloverThread = new Thread(new AutoRollover(this), nextName());
            mRolloverThread.setDaemon(true);
            mRolloverThread.start();
        }
    }

    /**
     * If the auto-rollover thread was started, calling this method will 
     * stop it.
     */
    public synchronized void stopAutoRollover() {
        if (mRolloverThread != null) {
            mRolloverThread.interrupt();
            mRolloverThread = null;
        }
    }

    /**
     * Moves calendar to beginning of log interval.
     */
    protected abstract void moveToIntervalStart(Calendar cal);

    /**
     * Moves calendar to beginning of next log interval.
     */
    protected abstract void moveToNextIntervalStart(Calendar cal);

    public synchronized void write(int b) throws IOException {
        getOutputStream().write(b);
    }

    public synchronized void write(byte[] array) throws IOException {
        getOutputStream().write(array, 0, array.length);
    }

    public synchronized void write(byte[] array, int off, int len) 
        throws IOException {

        getOutputStream().write(array, off, len);
    }

    public synchronized void flush() throws IOException {
        getOutputStream().flush();
    }

    /**
     * Closes any underlying OutputStreams and stops the auto-rollover thread
     * if it is running.
     */
    public synchronized void close() throws IOException {
        mIsClosed = true;
        stopAutoRollover();

        if (mOut != null) {
            mOut.close();
        }
    }

    protected synchronized void finalize() throws IOException {
        close();
    }

    private synchronized OutputStream getOutputStream() throws IOException {
        if (mIsClosed) {
            throw new IOException("LogStream is closed");
        }

        Calendar cal = Calendar.getInstance();

        if (mOut == null || 
            cal.before(mIntervalStart) || !cal.before(mNextIntervalStart)) {

            if (mOut != null) {
                mOut.close();
            }

            mOut = new BufferedOutputStream
                (mFactory.openOutputStream(cal.getTime()));

            setIntervalEndpoints(cal);
        }

        return mOut;
    }

    private void setIntervalEndpoints(Calendar cal) {
        mIntervalStart = (Calendar)cal.clone();
        moveToIntervalStart(mIntervalStart);

        mNextIntervalStart = cal;
        moveToNextIntervalStart(mNextIntervalStart);
    }

    public static interface Factory {
        public OutputStream openOutputStream(Date date) throws IOException;
    }

    /**
     * Thread that just wakes up at the proper time so that log stream
     * rolls over even when there is no output.
     */
    private static class AutoRollover implements Runnable {
        // Refer to the log stream via a weak reference so that this thread
        // doesn't prevent it from being garbage collected.
        private WeakReference mLogStream;

        public AutoRollover(IntervalLogStream stream) {
            mLogStream = new WeakReference(stream);
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    IntervalLogStream stream = 
                        (IntervalLogStream)mLogStream.get();

                    if (stream == null || stream.mIsClosed) {
                        break;
                    }

                    try {
                        // Just requesting the stream forces a rollover.
                        stream.getOutputStream();
                    }
                    catch (IOException e) {
                    }

                    Calendar cal = Calendar.getInstance();
                    stream.moveToNextIntervalStart(cal);
                    
                    // Clear reference to stream so that it isn't strongly
                    // reachable from this thread.
                    stream = null;

                    long calTime = cal.getTime().getTime();
                    long timeLeft = calTime - System.currentTimeMillis();

                    while (timeLeft > 0) {
                        // Sleep until next start interval. ZZZ...
                        Thread.sleep(timeLeft);
                        timeLeft = calTime - System.currentTimeMillis();
                    }
                }
            }
            catch (InterruptedException e) {
                // Exit thread.
            }
        }
    }
}
