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
package org.teatrove.teaapps.contexts;

import java.util.Map;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;
import org.teatrove.trove.util.plugin.Plugin;

/**
 * @author Scott Jappinen
 */
public class PluginAccessorContext implements Context {

    private Map<String, Plugin> mPluginMap;
    private Plugin[] mPluginArray;
    
    public void init(ContextConfig config) {
        mPluginArray = config.getPluginArray();
        mPluginMap = config.getPlugins();
    }
    
    public Plugin getPlugin(String name) {
        return mPluginMap.get(name);
    }
    
    public Plugin[] getPlugins() {
        return mPluginArray;
    }
}
