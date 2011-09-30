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
 * The PluginFactoryConfig is the class the PluginFactory will use
 * to configure itself.
 *
 * @author Scott Jappinen
 */
public interface PluginFactoryConfig extends Config {

    /**
     * Returns a reference to the PluginContext.
     * 
     * @returns PluginContext the PluginContext object.
     */
    public PluginContext getPluginContext();

}

