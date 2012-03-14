package org.teatrove.teaservlet.assets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetLoader {

    private List<AssetFactory> factories;
    private Map<String, String> mimeTypes;
    
    public AssetLoader() {
        this(new ArrayList<AssetFactory>(), new HashMap<String, String>());
    }
    
    public AssetLoader(List<AssetFactory> factories, 
                       Map<String, String> mimeTypes) {
        
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
        // get extension
        int idx = path.lastIndexOf('.');
        if (idx < 0) { return null; }
        String extension = path.substring(idx + 1);
        
        // validate mime type
        String mimeType = mimeTypes.get(extension);
        if (mimeType == null) {
            // TODO: LOG.info("unauthorized access to restricted mime type: " + path);
            return null;
        }
        
        // find first valid factory
        InputStream input = null;
        for (AssetFactory factory : factories) {
            input = factory.getAsset(path);
            if (input != null) { break; }
        }
        
        // verify input found
        if (input == null) {
            // TODO: LOG.debug("unable to find resource: " + path);
            return null;
        }
        
        // return found resource
        return new Asset(path, mimeType, input);
    }
}
