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

package org.teatrove.teaservlet;

import java.util.Map;

import javax.servlet.ServletConfig;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.Config;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.plugin.Plugin;

/**
 * The ApplicationConfig is the object that an Application will use to
 * configure itself.
 *
 * @author Reece Wilton
 */
public interface ApplicationConfig extends ServletConfig, Config {
    /**
     * Returns initialization parameters in an easier to use map format.
     */
    public PropertyMap getProperties();

    /**
     * Returns the name of this application, which is the same as the log's
     * name.
     */
    public String getName();

    /**
     * Returns a log object that this application should use for reporting
     * information pertaining to the operation of the application.
     */
    public Log getLog();
    
    /**
     * Returns a plugin by name.
     */
    public Plugin getPlugin(String name);
    
    /**
     * Returns a mapping of all plugins by their names.
     */
    public Map<String, Plugin> getPlugins();    
}
