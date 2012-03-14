package org.teatrove.teaservlet.assets;

import org.teatrove.trove.util.PropertyMap;

public abstract class AbstractAssetFactory implements AssetFactory {

    public AbstractAssetFactory() {
        super();
    }

    @Override
    public void init(PropertyMap properties) throws Exception {
        // nothing to do
    }
    
    protected String validatePath(String path) {
        
        // verify valid file
        if (path == null) {
            throw new NullPointerException("path");
        }
        
        path = path.trim();
        if (path.isEmpty()) {
            throw new IllegalArgumentException(
                "asset path may not be empty string");
        }
        
        // trim leading slashes
        int idx = -1;
        while (path.charAt(idx + 1) == '/') { idx++; }
        if (idx >= 0) { path = path.substring(idx + 1); }
        
        // return resulting path
        return path;
    }
}
