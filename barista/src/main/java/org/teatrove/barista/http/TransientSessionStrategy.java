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

package org.teatrove.barista.http;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Enumeration;
import java.util.Vector;
import java.io.Serializable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.Cookie;
import org.teatrove.trove.util.Cache;
import org.teatrove.trove.util.PropertyMap;

/**
 * An abstract session strategy that stores sessions in memory and stores the
 * session identifier in a cookie. An implementation is required to provide
 * the name of the cookie and the mechanism for generating unique identifers.
 *
 * <p>A session is automatically deleted if available memory is low and the
 * session hasn't been used in awhile, or if a session has exceed its maximum
 * inactive interval. By default, newly created sessions have an unlimited
 * maximum inactive interval.
 *
 * @author Brian S O'Neill
 */
public abstract class TransientSessionStrategy implements HttpSessionStrategy {
    private SessionTimer mExpirationTimer;

    private String mCookieName;

    // Maps session identifiers to HttpSession objects.
    private Map mSessions;
    private int mMaxSessions;
    private Vector mSessionAttributeListeners;  
    private Vector mSessionListeners;

    private boolean mRedirectOnCreate;

    /**
     * @param cookieName the name of the cookie used to store the session id.
     * @param maxSessions the maximum amount of recently sessions that are
     * guaranteed to be available.
     */
    public TransientSessionStrategy(String cookieName, int maxSessions) {
        mCookieName = cookieName;
        mSessions = Collections.synchronizedMap(new Cache(maxSessions));
        mMaxSessions = maxSessions;
    }

    /**
     * The init(PropertyMap properties) method must be called when using
     * this constructor.
     */
    public TransientSessionStrategy() {
    }

    /**
     * @param properties a {@link PropertyMap} containing the keys
     * "cookie.name" and "sessions" corresponding to a String
     * for the cookie name and the maximum number of sessions. If
     * "redirect.on.create" is true, a redirect is sent back to the user
     * with the original URI. This allows proxies that perform sticky routing
     * based on the session cookie to direct the user to the correct server.
     */
    public void init(PropertyMap properties) {
        mCookieName = properties.getString("cookie.name", mCookieName);
        int maxSessions = properties.getInt("sessions", mMaxSessions);
        if (maxSessions != mMaxSessions) {
            mSessions = Collections.synchronizedMap(new Cache(maxSessions));
            mMaxSessions = maxSessions;
        }

        mRedirectOnCreate = properties.getBoolean("redirect.on.create", false);
        
        // Register Session Activation and Attribute Listeners.
        PropertyMap listenerMap = properties.subMap("listeners");
        Iterator listenToIt = listenerMap.subMapKeySet().iterator();
        while (listenToIt.hasNext()) {
            String listenerName = listenToIt.next().toString();
            try {
                String listenerClassName = 
                    listenerMap.getString(listenerName + ".class");
                if (listenerClassName != null && listenerClassName.length() > 0) {
                    Class listenerClass = Class.forName(listenerClassName);
                    Object listenerObj = listenerClass.newInstance();
                    boolean added = false;
                    if (listenerObj instanceof 
                        HttpSessionAttributeListener) {
                        if (mSessionAttributeListeners == null) {
                            mSessionAttributeListeners = new Vector();
                        }
                        mSessionAttributeListeners.add(listenerObj);
                        added = true;
                    }
                    if (listenerObj instanceof 
                        HttpSessionListener) {
                        if (mSessionListeners == null) {
                            mSessionListeners = new Vector();
                        }
                        mSessionListeners.add(listenerObj);
                        added = true;
                    }
                    if (!added) {
                        System.err.println
                            ("Listener is not an instance of " +
                             "HttpSessionListener or " +
                             "HttpSessionAttributeListener: " + listenerClass);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public SessionInfo[] getSessionInfos() {
        return (SessionInfo[])mSessions.values()
            .toArray(new SessionInfo[mSessions.size()]);
    }

    public HttpSessionStrategy.Support createSupport
        (final HttpServletRequest request,
         final HttpServletResponse response) {

        return new HttpSessionStrategy.Support() {
            private String mRequestedId;
            private boolean mRequestedValid;
            private boolean mRequestedFromCookie;

            private boolean mCreated;

            public HttpSession getSession(ServletContext context, 
                                          boolean create) {

                Session session = readRequestedSession();

                if (session == null && create ) {
                    // Prevent multiple creations in order to prevent multiple
                    // cookies from being set. The HttpServletResponse
                    // doesn't define a setCookie method.
                    mCreated = true;

                    mRequestedValid = false;
                    mRequestedFromCookie = false;

                    // Save the user's session id in a cookie.
                    Cookie cookie;
                    if (mRequestedId == null) {
                        cookie = new Cookie(mCookieName, "");
                    }
                    else {
                        cookie = new Cookie(mCookieName, mRequestedId);
                    }
                    if (alterCookie(cookie)) {
                        // Only send the cookie back if it was altered.
                        response.addCookie(cookie);
                        if (mRedirectOnCreate) {
                            // Always append a '?' or '&' to make the URI
                            // different and force the browser to redirect
                            // back.
                            String redirect =
                                request.getRequestURI().concat("?");
                            if (request.getQueryString() != null) {
                                redirect +=
                                    request.getQueryString().concat("&");
                            }
                            try {
                                response.sendRedirect(redirect);
                                // Barista will ensure that this is caught and
                                // not logged.
                                throw new AbortServlet();
                            }
                            catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    String id = cookie.getValue();
                    session = new Session(context, id);
                    if (mSessionListeners != null 
                        && mSessionListeners.size() > 0) {
                        Iterator it = mSessionListeners.iterator();
                        HttpSessionEvent event = new HttpSessionEvent(session);
                        while (it.hasNext()) {
                            ((HttpSessionListener)it.next())
                                .sessionCreated(event);
                        }
                    }

                    // remove and invalidate the old session 
                    // before adding the new one.
                    HttpSession old = (HttpSession)mSessions.get(id);
                    if (old != null) {
                        try {
                            old.invalidate();
                        }
                        catch (IllegalStateException e) {
                        }
                    }
                    mSessions.put(id, session);
                    mRequestedId = id;

                }

                return session;
            }
            

            public HttpSession getSession(ServletContext context) {
                return getSession(context, true);
            }

            public String getRequestedSessionId() {
                if (mRequestedId == null)
                    readRequestedSession();
                return mRequestedId;
            }

            public boolean isRequestedSessionIdValid() {
                readRequestedSession();
                return mRequestedValid;
            }

            public boolean isRequestedSessionIdFromCookie() {
                readRequestedSession();
                return mRequestedFromCookie;
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            private Session readRequestedSession() {
 
                Session session = null;
                if (mRequestedId == null) {
                    Cookie[] cookies = request.getCookies();
                    if (cookies != null) {
                        for (int i = 0; i < cookies.length; i++) {
                            Cookie cookie = cookies[i];
    
                            if (mCookieName.equals(cookie.getName())) {
                                mRequestedId = cookie.getValue();
                                break;
                            }
                        }
                    }
                }

                if (mRequestedId != null) {
                    session = (Session)mSessions.get(mRequestedId);
                    if (session != null && session.accessed()) {
                        mRequestedValid = true;
                    }
                    else {
                        mRequestedValid = false;
                        session = null;
                    }
                    mRequestedFromCookie = true;
                }
                return session;
            }
        };
    }

    /**
     * This method must check if the given cookie contains an acceptable unique
     * value (a GUID). If it isn't acceptable, a new unique value must be set
     * and true must be returned. If any changes at all are made to the cookie,
     * true must be returned.
     */
    protected abstract boolean alterCookie(Cookie cookie);

    synchronized SessionTimer getExpirationTimer() {
        if (mExpirationTimer == null) {
            mExpirationTimer = new SessionTimer();
        }
        return mExpirationTimer;
    }


    /**
     * A session that stores values in memory.
     *
     * @author Reece Wilton
     * @version
     * <!--$$Revision:--> 36 <!-- $-->, <!--$$JustDate:-->  9/16/04 <!-- $-->
     */
    private class Session extends SessionInfo implements Serializable {
        private final ServletContext mContext;
        private final String mId;
        private final long mCreationTime;
        private long mLastAccessedTime;
        private int mMaxInactive = -1;
        private boolean mNew;
        private Map mValues;

        public Session(ServletContext context, String id) {
            mContext = context;
            mId = id;
            mCreationTime = System.currentTimeMillis();
            mLastAccessedTime = mCreationTime;
            mNew = true;
            // Keep the initial size small because sessions will likely not
            // contain many top-level keys and there may be many sessions
            // in memory.
            mValues = new HashMap(5);
        }

        public synchronized long getCreationTime() {
            checkIfValid();
            return mCreationTime;
        }

        public String getId() {
            return mId;
        }

        public synchronized long getLastAccessedTime() {
            return mLastAccessedTime;
        }

        public int getMaxInactiveInterval() {
            return mMaxInactive;
        }

        public synchronized Object getAttribute(String name) {
            return getValue(name);
        }

        public synchronized Enumeration getAttributeNames() {
            checkIfValid();
            Set keys = mValues.keySet();
            return Collections.enumeration(keys);
        }

        public synchronized void setAttribute(String name,Object o) {
            putValue(name, o);
        }

        public synchronized void removeAttribute(String name) {
            removeValue(name);
        }

        public synchronized Object getValue(String name) {
            checkIfValid();
            return mValues.get(name);
        }

        public synchronized String[] getValueNames() {
            checkIfValid();
            Set keys = mValues.keySet();
            return (String[])keys.toArray(new String[keys.size()]);
        }

        public void invalidate() {
            Map values;
            
            synchronized (this) {
                checkIfValid();
                mSessions.remove(getId());
                values = mValues;
                mValues = null;
                getExpirationTimer().removeScheduledSession(this);
            }
                        
            Iterator it = values.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String name = (String)entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof HttpSessionBindingListener) {
                    ((HttpSessionBindingListener)value).valueUnbound
                        (new HttpSessionBindingEvent(this, name));
                }
            }
        
            if (mSessionListeners != null 
                && mSessionListeners.size() > 0) { 
                HttpSessionEvent event = new HttpSessionEvent(this);
                Iterator invalidateIt = mSessionListeners.iterator();
                while (invalidateIt.hasNext()) {
                    ((HttpSessionListener)invalidateIt.next())
                        .sessionDestroyed(event);
                }
            }
        }

        public synchronized boolean isNew() {
            checkIfValid();
            return mNew;
        }

        public void putValue(String name, Object value) {
            Object old;

            synchronized (this) {
                checkIfValid();
                old = mValues.put(name, value);
            }

            if (value instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener)value).valueBound
                    (new HttpSessionBindingEvent(this, name));
            }

            if (old instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener)old).valueUnbound
                    (new HttpSessionBindingEvent(this, name));
            }
            // Attribute Listener stuff from API 2.3
            if (mSessionAttributeListeners != null 
                && mSessionAttributeListeners.size() > 0) { 
                Iterator invalidateIt = mSessionAttributeListeners.iterator();
                while (invalidateIt.hasNext()) {
                    HttpSessionAttributeListener attrListener = 
                        (HttpSessionAttributeListener)invalidateIt.next();
                    if (old == null) {
                        attrListener.
                            attributeAdded(new HttpSessionBindingEvent(this,
                                                                       name,
                                                                       value));
                    }
                    else {
                        attrListener.
                            attributeReplaced(new HttpSessionBindingEvent(this,
                                                                          name,
                                                                          old));
                    }
                }
            }
        }

        public void removeValue(String name) {
            Object old;

            synchronized (this) {
                checkIfValid();
                old = mValues.remove(name);
            }

            if (old instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener)old).valueUnbound
                    (new HttpSessionBindingEvent(this, name));
            }
            // Attribute Listener stuff from API 2.3
            if (mSessionAttributeListeners != null 
                && mSessionAttributeListeners.size() > 0) { 
                HttpSessionBindingEvent attrEvent = 
                    new HttpSessionBindingEvent(this,name,old);
                Iterator invalidateIt = mSessionAttributeListeners.iterator();
                while (invalidateIt.hasNext()) {
                    ((HttpSessionAttributeListener)invalidateIt.next())
                        .attributeRemoved(attrEvent);
                }
            }
        }

        public synchronized void setMaxInactiveInterval(int interval) {
            mMaxInactive = interval;
            if (interval >= 0) {
                getExpirationTimer()
                    .scheduleSessionInvalidation(this, (long)interval * 1000);
            }
        }

        /**
         * @return false if session is inactive now because it wasn't accessed
         * in a long time.
         */
        synchronized boolean accessed() {
            mNew = false;
            mLastAccessedTime = System.currentTimeMillis();
            setMaxInactiveInterval(mMaxInactive);
            return (mValues != null);
        }

        public ServletContext getServletContext() {
            return mContext;
        }

        /**
         * @deprecated
         */
        public HttpSessionContext getSessionContext() {
            return null;
        }

        protected void finalize() {
            if (mValues != null) {
                invalidate();
            }
        }

        private void checkIfValid() {
            if (mValues == null) {
                throw new IllegalStateException("Session is invalid");
            }
        }
    }

    private class SessionTimer {

        private SortedSet mTree = new TreeSet();
        private Map mHash = new HashMap();

        private Thread mCleanerThread = new Thread("SessionCleaner") {
                public void run() {

                    SessionInvalidationWrapper firstWrapper;
                    long waitTime;
                    
                    while (true) {
                        try {
                            synchronized (SessionTimer.this) {            

                                if (mTree.isEmpty()) {
                                    try {
                                        SessionTimer.this.wait();
                                    }
                                    catch (InterruptedException ie) {
                                        ie.printStackTrace();
                                    }
                                    continue;
                                }                           
                                firstWrapper = 
                                    (SessionInvalidationWrapper)mTree.first();

                                waitTime = firstWrapper.mTime.getTime()
                                    - System.currentTimeMillis();
                            
                                if (waitTime > 0) {
                                    try {
                                        SessionTimer.this.wait(waitTime);
                                    }
                                    catch (InterruptedException ie) {
                                        ie.printStackTrace();
                                    }
                                    continue;
                                }

                                /* remove while invalidating session.
                              
                                   synchronize (mHash) {
                                   mHash.remove(firstWrapper.mSession);
                                   mTree.remove(firstWrapper);
                                   }
                                */
                            }

                            firstWrapper.mSession.invalidate();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            };

        SessionTimer() {
            mCleanerThread.setDaemon(true);
            mCleanerThread.start();
        }
 

         public synchronized boolean scheduleSessionInvalidation(TransientSessionStrategy
                                                .Session session,
                                                Date time) {

            SessionInvalidationWrapper wrapper =
                new SessionInvalidationWrapper(session, time);
           
            SessionInvalidationWrapper oldWrapper = 
                (SessionInvalidationWrapper)mHash.put(session, wrapper);
            
            if (oldWrapper != null) {
                mTree.remove(oldWrapper);
            }
            
            if (!mTree.add(wrapper)) {
                mHash.remove(session);
                return false;
            }
                    
            notify();
         
            return true;
         }

        public synchronized boolean scheduleSessionInvalidation(TransientSessionStrategy
                                                .Session session,
                                                long delay) {
            return scheduleSessionInvalidation(session,
                                               new Date(System.currentTimeMillis() 
                                                        + delay));
        }
    
        public synchronized boolean removeScheduledSession(TransientSessionStrategy
                                                        .Session session) {
            
            SessionInvalidationWrapper wrapperToRemove = 
                (SessionInvalidationWrapper)mHash.remove(session);
            if (wrapperToRemove != null) {
                return mTree.remove(wrapperToRemove);
            }
            return false;
        }

        
        private class SessionInvalidationWrapper implements Comparable {

            public TransientSessionStrategy.Session mSession;
            public Date mTime;

            SessionInvalidationWrapper(TransientSessionStrategy.Session session,
                                       Date time) {
                mSession = session;
                mTime = time;
            }

            public int compareTo(Object obj) {
                int comparisonVal = mTime
                    .compareTo(((SessionInvalidationWrapper)obj).mTime);
                if (comparisonVal == 0 && !equals(obj)) {
                    comparisonVal = mSession.getId()
                        .compareTo(((SessionInvalidationWrapper)obj)
                                   .mSession.getId());
                }
                return comparisonVal;
            }

            public boolean equals(Object obj) {
                return mSession.equals(((SessionInvalidationWrapper)obj)
                                       .mSession);
            }
        }
    }
}
