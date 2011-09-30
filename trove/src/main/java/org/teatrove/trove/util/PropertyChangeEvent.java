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

import org.teatrove.trove.io.SourceInfo;

/**
 * 
 * @author Sean T. Treat
 */
public class PropertyChangeEvent extends java.util.EventObject {

    private String mKey;
    private String mValue;
    private SourceInfo mInfo;

    public PropertyChangeEvent(Object source) {
        this(source, null, null, null);
    }

    public PropertyChangeEvent(Object source, String key, String value, 
                               SourceInfo info) {
        super(source);
        mKey = key;
        mValue = key;
        mInfo = info;
    }

    public String getKey() {
        return mKey;
    }

    public String getValue() {
        return mValue;
    }

    public SourceInfo getSourceInfo() {
        return mInfo;
    }
}
