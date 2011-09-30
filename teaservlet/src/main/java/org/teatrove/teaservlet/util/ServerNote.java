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

package org.teatrove.teaservlet.util;

import java.util.Date;

/**
 * ServerNote wraps a String and a Date for the purpose of leaving notes for 
 * other administrators on a server. 
 *
 * @author Jonathan Colwell
 */
public class ServerNote {

    private Date mTimestamp, mExpiration;
    private String mContents;

    /**
     * to prevent any manipulation of the timestamps, the constructor is the
     * only place that can set it using "new Date()".
     */
    public ServerNote(String contents, int lifespan) {
        mTimestamp = new Date();
        mExpiration = new Date(mTimestamp.getTime() + (lifespan * 1000));
        mContents = contents;
    }

    public String getContents() {
        return mContents;
    }

    public Date getTimestamp() {
        return mTimestamp;
    }

    public Date getExpiration() {
        return mExpiration;
    }
}
