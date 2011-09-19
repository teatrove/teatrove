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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import javax.servlet.http.Cookie;
import org.teatrove.trove.util.PropertyMap;
import org.joda.time.DateTime;

/**
 * Default session strategy that uses a cookie named "SWID" and has a
 * custom session id generator.
 *
 * @author Jonathan Colwell
 */
public class SWIDSessionStrategy extends TransientSessionStrategy {
    private static final int SWID_LENGTH = 8 + 12 + 8 + 8;

    private static byte[] cAddress;
    private int mMaxAge = (int)(20 * 365.25f * 24 * 60 * 60);

    static {
        try {
            cAddress = InetAddress.getLocalHost().getAddress();
        }
        catch (UnknownHostException e) {
            cAddress = new byte[0];
        }

    }

    /**
     * Thread-safe method that returns a new SWID.
     */
    public static synchronized String nextSWID() {
        StringBuffer id = new StringBuffer(SWID_LENGTH);
        DateTime now = new DateTime();

        for (int i=0; i<cAddress.length; i++) {
            id.append(format(Integer.toHexString(cAddress[i]), 2));
        }
        id.append('-');
        String uid = new UID().toString();
        id.append(uid.substring(0, 4) + "-");
        id.append(uid.substring(4, 7) + "0-");
        id.append(uid.substring(8, 12) + "-");
        id.append(uid.substring(12, 18) + uid.substring(20, 24));
        id.append(format(Integer.toHexString(now.dayOfMonth().get()), 2));

        return id.toString().toUpperCase();
    }

    private static String format(String str, int length) {
        int strLen = str.length();
        if (strLen == length) {
            return str;
        }
        else if (strLen < length) {
            StringBuffer buf = new StringBuffer(length);
            for (int i=length - strLen; i>0; i--) {
                buf.append('0');
            }
            return buf.append(str).toString();
        }
        else {
            return str.substring(strLen - length);
        }
    }

    /**
     * Creates a session strategy with a cookie named "SWID".
     */
    public SWIDSessionStrategy(int maxSessions) {
        this("SWID", maxSessions);
    }

    /**
     * @param cookieName the name of the cookie used to store the session id.
     */
    public SWIDSessionStrategy(String cookieName, int maxSessions) {
        super(cookieName, maxSessions);
    }


    /**
     * The init(PropertyMap properties) method must be called when using
     * this constructor.
     */
    public SWIDSessionStrategy() {
        this(500);
    }

    /**
     * @param properties a {@link PropertyMap} containing the keys
     * "cookie.name", "sessions", and "cookie.age" corresponding to a String
     * for the cookie name, an int for the maximum number of sessions, and an
     * int for the maximum cookie age.
     */
    public void init(PropertyMap properties) {
        super.init(properties);
        mMaxAge = properties.getInt("cookie.age", mMaxAge);
    }

    /**
     * If the given cookie value isn't an acceptable SWID, fills in a unique
     * value which combines the IP address of this machine, the current time
     * (in milliseconds), a counter value, and a random number. The cookie's
     * age is set to approximately 20 years, the path is '/', and the domain is
     * ".go.com".
     */
    protected boolean alterCookie(Cookie cookie) {
        String value = cookie.getValue();
        // fixed bug...do not check length
        //if (value != null && value.length() == SWID_LENGTH) {
        if (value != null) {
            return false;
        }
        cookie.setValue(nextSWID());
        cookie.setMaxAge(mMaxAge);
        cookie.setPath("/");
        cookie.setDomain(".go.com");
        return true;
    }
}
