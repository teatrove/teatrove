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

/**
 *
 *
 * @author Scott Jappinen
 */
public class PluginAdapter implements Plugin {

    /**
     * Initializes resources used by the Plugin.
     *
     * @param config the plugins's configuration object
     */	
    public void init(PluginConfig config) throws PluginException {}
    
    /**
     * Return the name of the Plugin.
     *
     * @return String the name of the plugin.
     */
    public String getName() {
        return null;
    }
    
    /**
     * Called by the host container when the plugin is no longer needed.
     */	
    public void destroy() {}

    /** 
     * This method is invoked whenever a Plugin has added itself to the PluginContext.
     *
     * @param event a PluginEvent event object.
     */
    public void pluginAdded(PluginEvent event) {}
    
}

