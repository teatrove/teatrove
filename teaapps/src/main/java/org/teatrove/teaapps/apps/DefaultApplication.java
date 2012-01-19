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
package org.teatrove.teaapps.apps;

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;

import org.teatrove.trove.util.plugin.Plugin;

import org.teatrove.teaapps.Context;
import org.teatrove.teaapps.ContextConfig;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Scott Jappinen
 */
public class DefaultApplication implements Application {
    
    private Object context;
	private Class contextClass;

    public void init(ApplicationConfig config) {
		String contextClassName = config.getProperties().getString("contextClass");
        try {
            contextClass = Class.forName(contextClassName);
            context = contextClass.newInstance();
            if (context instanceof Context) {
                Context castContext = (Context) context;
                Map plugins = config.getPlugins();
                ContextConfig contextConfig = new ContextConfig
                    (config.getProperties(), config.getLog(), 
                     config.getName(), getTypedPluginMap(plugins));
                castContext.init(contextConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private Map<String,Plugin> getTypedPluginMap(Map plugins) {
        Map<String,Plugin> result = new HashMap<String,Plugin>();
        Set keySet = plugins.keySet();
        for (Object key: keySet) {
            result.put((String) key, (Plugin) plugins.get(key));
        }
        return result;
    }
    
    public void destroy() {}
    
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {        
        return context;
    }

    public Class getContextType() {
        return contextClass;
    }
}
