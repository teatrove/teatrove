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

import java.util.EventObject;

/**
 * The PluginEvent class encapsulates the information pertaining to a plugin event.
 *
 * @author Scott Jappinen
 */
public class PluginEvent extends EventObject {

    private Plugin mPlugin;
    
    public PluginEvent(Object src, Plugin plugin) {
        super(src);
        mPlugin = plugin;
    }
    
    /**
     * Returns the Plugin that this event pertains to.
     * 
     * @returns Plugin the Plugin object.
     */
    public Plugin getPlugin() {
        return mPlugin;
    }
    
    /**
     * Returns the name of the Plugin that this event pertains to.
     * 
     * @returns String the name of the Plugin.
     */
    public String getPluginName() {
        return mPlugin.getName();
    }
}

