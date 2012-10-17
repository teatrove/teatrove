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

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyParser;
import org.teatrove.trove.util.StatusEvent;
import org.teatrove.trove.util.StatusListener;
import org.teatrove.trove.util.XMLMapFactory;

/**
 * This class is responsible for creating plugins based from a
 * configuration block.
 *
 * @author Scott Jappinen, Travis Greer
 */
public class PluginFactory {

    private static final String cClassKey = "class";
    private static final String cInitKey = "init";
    private static final String cPluginsKey = "plugins";
    private static final String cInjectedFileKey = "propertyInjector";
	private static final String cExternalPropKey = "externalProperty";
	private static final String cExternalNameKey = "externalPluginName";


    public static final Plugin createPlugin(String name, PluginFactoryConfig config)
        throws PluginFactoryException
    {
        Plugin result;
        String className = config.getProperties().getString(cClassKey);
        PropertyMap props = config.getProperties().subMap(cInitKey);


        if (config.getProperties().containsKey(cInjectedFileKey)) {
        	try {
        		if (props == null) {
        			props = new PropertyMap();
        		}

        		Map injectedProps = new PropertyMap();
	        	PropertyParser parser = new PropertyParser(props);
				parser.parse(new FileReader((String)config.getProperties().get(cInjectedFileKey)));
				props.putAll(injectedProps);
        	} catch (IOException e) {
        		new PluginFactoryException(e);
			} catch (Exception e) {
        		new PluginFactoryException(e);
			}
        }

        //This block is used to retrieve plugins from an external file
		if (config.getProperties().containsKey(cExternalPropKey)) {
			try {
				if (props == null) {
					props = new PropertyMap();
				}

				PropertyMap injectedProps = new PropertyMap();
				String externalPlugin = config.getProperties().getString(cExternalNameKey);
				Document pluginsDoc = null;
				pluginsDoc = XMLMapFactory.createDocument(new FileReader((String)config.getProperties().get(cExternalPropKey)));
				
				if(pluginsDoc != null) {
					Element rootElement = pluginsDoc.getRootElement();

					if(rootElement != null) {
						Element child = null;
						//Plugin name in the services and external config may be different
						if(externalPlugin != null) {
							config.getLog().debug(name + " is loading the external plugin " + externalPlugin);
							child = rootElement.getChild(externalPlugin);
						}
						else {
							child = rootElement.getChild(name);
						}
						if(child != null) {
							injectedProps = XMLMapFactory.getPropertyMapFromElement(child);

							//if the class name wasn't specified, get it from the external config
							if(className == null) {
								className = child.getChildText(cClassKey);
							}
						}
					}
				}
				if(injectedProps != null) {
					if(externalPlugin != null) {
						props.putAll(injectedProps.subMap(externalPlugin).subMap(cInitKey));
					}
					else {
						props.putAll(injectedProps.subMap(name).subMap(cInitKey));
					}
				}
			} catch (Exception e) {
				throw new PluginFactoryException("Error loading external properties for " + name, e);
			}
		}//END external file load block
		
        try {
            Class clazz = Class.forName(className);
            result = (Plugin) clazz.newInstance();
            PluginConfig pluginConfig = new PluginConfigSupport
                (props, config.getLog(), config.getPluginContext(), name);
            result.init(pluginConfig);
        } catch (PluginException e) {
            throw new PluginFactoryException(e);
        } catch (ClassNotFoundException e) {
            throw new PluginFactoryException(e);
        } catch (InstantiationException e) {
            throw new PluginFactoryException(e);
        } catch (IllegalAccessException e) {
            throw new PluginFactoryException(e);
        }
        return result;
    }

    public static final Plugin[] createPlugins(PluginFactoryConfig config)
        throws PluginFactoryException 
    {
        return createPlugins(config, null);
    }
    
    public static final Plugin[] createPlugins(PluginFactoryConfig config,
                                               StatusListener listener)
        throws PluginFactoryException
    {
        PluginContext context = config.getPluginContext();
        
        Plugin[] result;
        Map plugins;
        PropertyMap properties = config.getProperties().subMap(cPluginsKey);
        if (properties.containsKey("properties.file")) {
            String defaultsFileName = (String)properties.remove("properties.file");
            loadDefaults(defaultsFileName, properties, config.getLog());
        }

        Set keySet = properties.subMapKeySet();
        int index = 0, count = keySet.size();
        if (listener != null) {
            listener.statusStarted(new StatusEvent(context, index, count, null));
        }
        
        plugins = new HashMap(keySet.size());
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            PropertyMap initProps = properties.subMap(name);
            PluginFactoryConfig conf = new PluginFactoryConfigSupport
                (initProps, config.getLog(), context);
            try {
	            plugins.put(name, createPlugin(name, conf));
			} catch (Exception e) {
				// catch everything so we can continue loading even if one plugin is broken
				config.getLog().error("Error loading plugin: " + name);
				config.getLog().error(e);
			}
            
            index++;
            if (listener != null) {
                listener.statusUpdate(new StatusEvent(context, index, count, name));
            }
        }
        Map registeredPlugins = config.getPluginContext().getPlugins();
        keySet = registeredPlugins.keySet();
        iterator = keySet.iterator();
        while(iterator.hasNext()) {
            String name = (String) iterator.next();
            if (!plugins.containsKey(name)) {
                plugins.put(name, registeredPlugins.get(name));
            }
        }
        result = new Plugin[plugins.size()];
        result = (Plugin[])plugins.values().toArray(result);
        
        if (listener != null) {
            listener.statusCompleted(new StatusEvent(context, index, count, null));
        }
        
        return result;
    }

    private static void loadDefaults(String filename, PropertyMap props, Log log) {
        log.info("loading Plugin defaults");
        try {
            Map defaults = new PropertyMap();
            FileReader fr = new FileReader(filename);
            PropertyParser pp = new PropertyParser(defaults);
            pp.parse(fr);
            log.info("loaded " + defaults.size() + " default keys");
            props.putDefaults(defaults);
        }
        catch (IOException ioe) {
            log.error(ioe);
        }
    }
}

