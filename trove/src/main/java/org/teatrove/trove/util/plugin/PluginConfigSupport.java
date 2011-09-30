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

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.ConfigSupport;
import org.teatrove.trove.util.PropertyMap;

import java.util.Collection;

/**
 * A support class the provides a default implementation for the PluginConfig interface.
 *
 * @author Scott Jappinen
 */
public class PluginConfigSupport extends ConfigSupport implements PluginConfig {
    
    private PluginContext mPluginContext;
    private String mName;
    
    public PluginConfigSupport(PropertyMap properties, Log log, PluginContext context, String name) {
        super(properties, new Log(name, log));
        getLog().applyProperties(properties.subMap("log"));
        mPluginContext = context;
        mName = name;
    }
    
    /**
     * Returns the name of this plugin.
     *
     * @return String the name of the plugin.
     */
    public String getName() {
        return mName;
    }
    
    /**
     * Returns a reference to the PluginContext.
     * 
     * @return PluginContext the PluginContext object.
     */	
    public PluginContext getPluginContext() {
        return mPluginContext;
    }

	/**
	 * Returns a Plugin by name.
	 *
	 * @param name the name of the Plugin.
	 * @return Plugin the Plugin object.
	 */
	public Plugin getPlugin(String name) {
		if (mPluginContext != null) {
			return mPluginContext.getPlugin(name);
		}
		return null;
	}

	/**
	 * Returns an array of all of the Plugins.
	 *
	 * @return the array of Plugins.
	 */
	public Plugin[] getPlugins() {
		if (mPluginContext != null) {
			Collection c = mPluginContext.getPlugins().values();
			if (c != null) {
				return (Plugin[])c.toArray(new Plugin[c.size()]);
			}
		}
		return new Plugin[0];
	}
}
