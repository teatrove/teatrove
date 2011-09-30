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

import java.util.*;
import javax.servlet.*;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.plugin.Plugin;

/**
 * An ApplicationConfig implementation that is used for deriving configuration
 * for internal Applications.
 *
 * @author Brian S O'Neill
 */
class InternalApplicationConfig implements ApplicationConfig {  
    private ApplicationConfig mBaseConfig;
    private PropertyMap mProperties;
    private Log mLog;
    private Map mPluginMap;

    /**
     * Derive a new ApplicationConfig using the configuration passed in. Any
     * properties in the "log" sub-map are applied to the new Log, and the
     * "init" sub-map defines the new properties. Properties not contained
     * in "applications" from the base configuration are added as defaults to
     * to the new properties.
     *
     * @param config base configuration to derive from
     * @param properties properties to use
     * @param name name of internal application; is applied to Log name and
     * is used to extract the correct properties.
     */
    public InternalApplicationConfig(ApplicationConfig config,
                                     PropertyMap properties,
                                     Map plugins,
                                     String name) {
        mBaseConfig = config;
        mLog = new Log(name, config.getLog());
        mLog.applyProperties(properties.subMap("log"));
        mProperties = new PropertyMap(properties.subMap("init"));

        PropertyMap baseProps = config.getProperties();
        String filtered = "applications" + baseProps.getSeparator();
        Iterator it = baseProps.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            if (!key.startsWith(filtered) && !mProperties.containsKey(key)) {
                mProperties.put(key, baseProps.get(key));
            }
        }

        mPluginMap = plugins;
    }

    /**
     * Returns initialization parameters in an easier to use map format.
     */
    public PropertyMap getProperties() {
        return mProperties;
    }

    /**
     * Returns the name of this application, which is the same as the log's
     * name.
     */
    public String getName() {
        return mLog.getName();
    }

    /**
     * Returns a log object that this application should use for reporting
     * information pertaining to the operation of the application.
     */
    public Log getLog() {
        return mLog;
    }
    
    public Plugin getPlugin(String name) {
        return (Plugin)mPluginMap.get(name);
    }
    
    public Map getPlugins() {       
        return mPluginMap;
    }

    public ServletContext getServletContext() {
        return mBaseConfig.getServletContext();
    }
    
    public String getInitParameter(String name) {
        return mProperties.getString(name);
    }
    
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(mProperties.keySet());
    }

    public String getServletName() {
        return mBaseConfig.getServletName();
    }
}
