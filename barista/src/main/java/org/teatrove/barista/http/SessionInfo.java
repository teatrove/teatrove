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

import java.util.*;
import javax.servlet.http.HttpSession;

/**
 * Provides session information to the Barista Admin pages.
 *
 * @author Brian S O'Neill
 */
public abstract class SessionInfo implements HttpSession {
    public Date getCreationDate() {
        return new Date(getCreationTime());
    }

    public Date getLastAccessedDate() {
        return new Date(getLastAccessedTime());
    }

    public String[] getAttributeNamesArray() {
        List names = new ArrayList();
        Enumeration enumeration = getAttributeNames();
        while (enumeration.hasMoreElements()) {
            names.add(enumeration.nextElement());
        }
        return (String[])names.toArray(new String[names.size()]);
    }
}
