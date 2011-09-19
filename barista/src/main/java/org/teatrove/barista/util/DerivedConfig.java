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

package org.teatrove.barista.util;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.log.Log;

/**
 * @author Brian S O'Neill
 */
public class DerivedConfig extends DefaultConfig {
    /**
     * Derive a new Config using the configuration passed in. Any properties
     * in the "log" sub-map are applied to the new Log.
     *
     * @param config base configuration to derive from
     * @param name name of internal config; is applied to Log name and
     * is used to extract the correct properties.
     */
    public DerivedConfig(Config config, String name) {
        this(config, name, config.getProperties().subMap(name));
    }

    /**
     * Derived a new Config using the configuration passed in. Any properties
     * in the "log" sub-map are applied to the new Log.
     *
     * @param config base configuration to derive from
     * @param name name of internal config; is applied to Log name.
     * @param properties new properties to use.
     */
    public DerivedConfig(Config config, String name, PropertyMap properties) {
        super(properties,
              new Log(name, config.getLog()),
              config.getThreadPool());
        getLog().applyProperties(getProperties().subMap("log"));
    }
}
