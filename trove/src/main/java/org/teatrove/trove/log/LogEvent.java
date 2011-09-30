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

import java.io.*;
import java.util.*;
import java.lang.ref.WeakReference;

/**
 * LogEvent captures information that should be logged. LogEvents are one
 * of four types: debug, info, warn or error. All LogEvents have a
 * timestamp for when the event occurred and a reference to the thread
 * that created it. Most have an embedded message, and some have an
 * embedded exception.
 * 
 * @author Brian S O'Neill
 */
public class LogEvent extends EventObject {
    /** Debug type of LogEvent */
    public static final int DEBUG_TYPE = 1;

    /** Info type of LogEvent */
    public static final int INFO_TYPE = 2;

    /** Warn type of LogEvent */
    public static final int WARN_TYPE = 3;

    /** Error type of LogEvent */
    public static final int ERROR_TYPE = 4;

    private int mType;
    private Date mTimestamp;
    private String mMessage;
    private Throwable mThrowable;
    private String mThreadName;
    // WeakReference to a Thread.
    private transient WeakReference mThread;
    
    public LogEvent(Log log, int type, 
                    String message, Throwable throwable,
                    Thread thread, Date timestamp) {
        super(log);
        
        if (type < DEBUG_TYPE || type > ERROR_TYPE) {
            throw new IllegalArgumentException
                ("Type out of range: " + type);
        }
        
        mType = type;
        
        if (message == null) {
            if (throwable != null) {
                mMessage = throwable.getMessage();
            }
        }
        else {
            mMessage = message;
        }
        
        mThrowable = throwable;
        
        if (thread == null) {
            mThread = new WeakReference(Thread.currentThread());
        }
        else {
            mThread = new WeakReference(thread);
        }
        
        if (timestamp == null) {
            mTimestamp = new Date();
        }
        else {
            mTimestamp = timestamp;
        }
    }
    
    public LogEvent(Log log, int type, 
                    String message, Thread thread, Date timestamp) {
        this(log, type, message, null, thread, timestamp);
    }
    
    public LogEvent(Log log, int type, 
                    Throwable throwable, Thread thread, Date timestamp) {
        this(log, type, null, throwable, thread, timestamp);
    }
    
    public LogEvent(Log log, int type, 
                    String message, Thread thread) {
        this(log, type, message, null, thread, null);
    }
    
    public LogEvent(Log log, int type, 
                    Throwable throwable, Thread thread) {
        this(log, type, null, throwable, thread, null);
    }
    
    public LogEvent(Log log, int type, 
                    String message, Throwable throwable) {
        this(log, type, message, throwable, null, null);
    }
    
    public LogEvent(Log log, int type, String message) {
        this(log, type, message, null, null, null);
    }
    
    public LogEvent(Log log, int type, Throwable throwable) {
        this(log, type, null, throwable, null, null);
    }
    
    public Log getLogSource() {
        return (Log)getSource();
    }
    
    /**
     * Returns the type of this LogEvent, which matches one of the defined
     * type constants.
     */
    public int getType() {
        return mType;
    }
    
    /**
     * Returns the date and time of this event.
     */
    public Date getTimestamp() {
        return mTimestamp;
    }
    
    /**
     * Message may be null.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Sets the message.
     * 
     * @param message  the value to set the message to
     */
    public void setMessage(String message) {
    	mMessage = message;
    }
    
    /**
     * Returns null if there is no exception logged.
     */
    public Throwable getException() {
        return mThrowable;
    }
    
    /**
     * Returns null if there is no exception logged.
     */
    public String getExceptionStackTrace() {
        Throwable t = getException();
        if (t == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Returns the name of the thread that created this event.
     */
    public String getThreadName() {
        if (mThreadName == null) {
            Thread t = getThread();
            if (t != null) {
                mThreadName = t.getName();
            }
        }
        return mThreadName;
    }
    
    /**
     * Returns the thread that created this event, which may be null if
     * this LogEvent was deserialized or the thread has been reclaimed.
     */
    public Thread getThread() {
        return (Thread)mThread.get();
    }
    
    public String toString() {
        String msg;
        if (getMessage() == null) {
            msg = "null";
        }
        else {
            msg = '"' + getMessage() + '"';
        }
        
        return 
            getClass().getName() + "[" + 
            getTimestamp() + ',' +
            getThreadName() + ',' + 
            msg +
            "] from " + getSource();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        getThreadName();
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) 
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
