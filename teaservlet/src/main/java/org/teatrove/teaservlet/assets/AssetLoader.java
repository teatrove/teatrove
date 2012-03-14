package org.teatrove.teaservlet.assets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.teatrove.trove.log.Syslog;

public class AssetLoader {

    private String name;
    private String basePath;
    private List<AssetFactory> factories;
    private Map<String, String> mimeTypes;
    
    private WeakHashMap<String, AssetFactory> assets = 
        new WeakHashMap<String, AssetFactory>();
    
    public AssetLoader() {
        this(null, "");
    }
    
    public AssetLoader(String name, String basePath) {
        this(name, basePath,
             new ArrayList<AssetFactory>(), new HashMap<String, String>());
    }
    
    public AssetLoader(String name, String basePath,
                       List<AssetFactory> factories, 
                       Map<String, String> mimeTypes) {
        
        this.name = name;
        this.basePath = basePath;
        this.factories = factories;
        this.mimeTypes = mimeTypes;
    }
    
    public void addAssetFactory(AssetFactory factory) {
        this.factories.add(factory);
    }
    
    public void addMimeType(String extension, String mimeType) {
        this.mimeTypes.put(extension, mimeType);
    }
    
    public Asset getAsset(String path) {
        // validate base path exists
        if (basePath != null && !basePath.isEmpty() && 
            !path.startsWith(basePath)) {
            return null;
        }
        
        // trim base path from path
        path = path.substring(basePath.length());
        
        // get extension
        int idx = path.lastIndexOf('.');
        if (idx < 0) { return null; }
        String extension = path.substring(idx + 1);
        
        // validate mime type
        String mimeType = mimeTypes.get(extension);
        if (mimeType == null) {
            Syslog.warn("unauthorized access to restricted mime type: " + 
                        this.name + ":" + path);
            
            return null;
        }
        
        // check if cached for better efficiency
        InputStream input = null;
        AssetFactory cached = this.assets.get(path);
        if (cached != null) {
            input = cached.getAsset(path);
        }
        
        // otherwise, find first valid factory
        else {
            for (AssetFactory factory : factories) {
                input = factory.getAsset(path);
                if (input != null) {
                    // cache for better efficiency
                    this.assets.put(path, factory);
                    
                    // break out of loop (factory found)
                    break; 
                }
            }
        }
        
        // verify input found
        if (input == null) {
            Syslog.debug("unable to find resource: ".concat(path));
            return null;
        }
        
        // return found resource
        return new Asset(path, mimeType, input);
    }
}
