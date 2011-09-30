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

package org.teatrove.trove.util.plugin;

import org.teatrove.trove.util.Config;

/**
 * A config interface that both PluginConfig and HandlerConfig extend.
 *
 * @author Reece Wilton
 */
public interface PluginBasicConfig extends Config {

	/**
	 * Returns the name of this plugin.
	 *
	 * @return String the name of the plugin.
	 */
	public String getName();

    /**
     * Returns a Plugin by name.
     *
     * @param name the name of the Plugin.
     * @return Plugin the Plugin object.
     */
    public Plugin getPlugin(String name);

    /**
     * Returns an array of all of the Plugins.
     *
     * @return the array of Plugins.
     */
    public Plugin[] getPlugins();
}
