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

import org.teatrove.trove.util.plugin.Plugin;
import org.teatrove.trove.util.plugin.PluginConfig;
import org.teatrove.trove.util.plugin.PluginEvent;

/**
 * 
 * @author Jonathan Colwell
 */
public class EngineAccessPlugin implements Plugin, EngineAccess {

    TeaServletEngine mEngine;

    EngineAccessPlugin(TeaServletEngine engine) {
        mEngine = engine;
    }

    // Plugin methods

    public void init(PluginConfig conf) {
    }

    public void destroy() {

    }

    public String getName() {
        return mEngine.getName();
    }

    public void pluginAdded(PluginEvent pe) {

    }

    // EngineAccess methods

    public TeaServletEngine getTeaServletEngine() {
        return mEngine;
    }
}
