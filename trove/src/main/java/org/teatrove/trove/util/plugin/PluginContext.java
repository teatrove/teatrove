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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.resources.DefaultResourceFactory;
import org.teatrove.trove.util.resources.ResourceFactory;

/**
 * Plugin can reference other Plugins through the PluginContext. If a Plugin is
 * not immediately available the Plugin can register itself as a listener with 
 * the PluginContext so as to be notified when new Plugins are added to the 
 * PluginContext. When Plugins are fully initialized and want to make 
 * themselves available to other Plugins they should add themselves to the 
 * PluginContext.
 *
 * @author Scott Jappinen
 */
public class PluginContext implements ResourceFactory {
    
    private List<PluginListener> mPluginListeners;
    private Map<String, Plugin> mPluginMap;
    private ResourceFactory mResourceFactory;
    
    public PluginContext() {
        this(DefaultResourceFactory.getInstance());
    }
    
    public PluginContext(ResourceFactory resourceFactory) {
        mResourceFactory = resourceFactory;
        mPluginListeners = new ArrayList<PluginListener>();
        mPluginMap = new HashMap<String, Plugin>(7);
    }
    
    public ResourceFactory getResourceFactory() {
        return mResourceFactory;
    }
    
    public void setResourceFactory(ResourceFactory resourceFactory) {
        this.mResourceFactory = resourceFactory;
    }
   
    /**
     * Adds a PluginListener to the PluginContext. Plugins or anything
     * else that want to listen to PluginEvents should add themselves
     * to the PluginContext through this method.
     *
     * @param listener the PluginListener to be added.
     */
    public void addPluginListener(PluginListener listener) {
        if (!mPluginListeners.contains(listener)) {
            mPluginListeners.add(listener);
        }
    }
    
    /**
     * Adds a Plugin to the PluginContext. Plugins that want to make
     * themselves available to other Plugins should add themselves
     * to the PluginContext through this method. All PluginListeners
     * will be notified of the new addition.
     *
     * @param plugin the Plugin to be added.
     */
    public void addPlugin(Plugin plugin) {
        if (!mPluginMap.containsKey(plugin.getName())) {
            mPluginMap.put(plugin.getName(), plugin);
            PluginEvent event = new PluginEvent(this, plugin);
            firePluginAddedEvent(event);
        }
    }

    /**
     * Returns a Plugin by name.
     *
     * @param name the name of the Plugin.
     * @return Plugin the Plugin object.
     */
    public Plugin getPlugin(String name) {
        return mPluginMap.get(name);
    }
    
    /**
     * Returns a Map of all of the Plugins.
     *
     * @return Map the map of Plugins.
     */
    public Map<String, Plugin> getPlugins() {
        return new HashMap<String, Plugin>(mPluginMap);		
    }	
    
    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(String path) {
        
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResource(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResourceAsStream(path);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path) 
        throws IOException {
        
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResourceAsProperties(path);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, InputStream input) 
        throws IOException {
        
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResourceAsProperties(path, input);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, 
                                               PropertyMap substitutions)
        throws IOException {
        
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResourceAsProperties(path, substitutions);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyMap getResourceAsProperties(String path, InputStream input,
                                               PropertyMap substitutions)
        throws IOException {
        
        if (mResourceFactory == null) {
            return null;
        }
        
        return mResourceFactory.getResourceAsProperties(path, input,
                                                        substitutions);
    }
    
    /* Notifies all PluginListeners of a Plugin being added to this class.
     */
    protected void firePluginAddedEvent(PluginEvent event) {
        PluginListener[] listeners = new PluginListener[mPluginListeners.size()];
        listeners = mPluginListeners.toArray(listeners);
        for (int i=0; i < listeners.length; i++) {			
            listeners[i].pluginAdded(event);
        }
    }
}
