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

import java.util.Random;
import java.util.Collections;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.Cookie;
import org.teatrove.trove.util.Cache;
import org.teatrove.trove.util.PropertyMap;

/**
 * Default session strategy that uses a cookie named "Barista" and has a
 * custom session id generator.
 *
 * @author Brian S O'Neill
 */
public class BaristaSessionStrategy extends TransientSessionStrategy {
    private static final int ID_LENGTH = 8 + 16 + 8 + 8;

    private static Random cRandom;
    private static byte[] cAddress;
    private static int cCounter;
    private int mMaxAge = -1;

    static {
        cRandom = new Random();

        try {
            cAddress = InetAddress.getLocalHost().getAddress();
            try {
                cAddress[0] ^= 0x4e;
                cAddress[1] ^= 0xa4;
                cAddress[2] ^= 0x14;
                cAddress[3] ^= 0x62;
                byte temp = cAddress[0];
                cAddress[0] = cAddress[3];
                cAddress[3] = temp;
            }
            catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        catch (UnknownHostException e) {
            cAddress = new byte[0];
        }

        cCounter = cRandom.nextInt();
    }

    private static synchronized String nextId() {
        StringBuffer id = new StringBuffer(ID_LENGTH);

        for (int i=0; i<cAddress.length; i++) {
            id.append(format(Integer.toHexString(cAddress[i]), 2));
        }

        id.append(format(Long.toHexString(System.currentTimeMillis()), 16));
        id.append(format(Integer.toHexString(cCounter++), 8));
        id.append(format(Integer.toHexString(cRandom.nextInt()), 8));

        return id.toString();
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
     * Creates a session strategy with a cookie named "Barista".
     */
    public BaristaSessionStrategy(int maxSessions) {
        this("Barista", maxSessions);
    }

    /**
     * @param cookieName the name of the cookie used to store the session id.
     */
    public BaristaSessionStrategy(String cookieName, int maxSessions) {
        super(cookieName, maxSessions);
    }

    /**
     * The init(PropertyMap properties) method must be called when using
     * this constructor.
     */
    public BaristaSessionStrategy() {
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
     * If the given cookie value isn't an acceptable id, fills in a unique
     * value that combines the IP address of this machine, the current time 
     * (in milliseconds), a counter value and a random number. The cookie's
     * maximum age is set to -1, making it a non-persistent, "session" cookie.
     */
    protected boolean alterCookie(Cookie cookie) {
        String value = cookie.getValue();
        if (value != null && value.length() == ID_LENGTH) {
            return false;
        }
        cookie.setValue(nextId());
        cookie.setMaxAge(mMaxAge);
        cookie.setPath("/");
        return true;
    }
}
