package org.teatrove.trove.util.plugin;

public interface PluginFactoryListener {

    void pluginRegistered(String pluginName);
    
    void pluginAdded(String pluginName);
    // void pluginInitialized(String pluginName);
}
