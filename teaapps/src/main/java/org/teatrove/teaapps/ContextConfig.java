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
package org.teatrove.teaapps;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.ConfigSupport;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.plugin.Plugin;

import java.util.Collection;
import java.util.Map;

public class ContextConfig extends ConfigSupport {
    
    private String mName;
    private Map<String, Plugin> mPlugins;

	public ContextConfig(PropertyMap properties, Log log, String name, Map<String, Plugin> plugins) {
		super(properties, log);
		mName = name;
		mPlugins = plugins;
	}
    
    public String getName() {
        return mName;
    }
    
    public Plugin getPlugin(String name) {
        return mPlugins.get(name);
    }
    
    public Plugin[] getPluginArray() {
        Collection<Plugin> collection = mPlugins.values();        
        return collection.toArray(new Plugin[collection.size()]);
    }    
}

