package org.teatrove.teaservlet.assets;

import java.io.InputStream;

import org.teatrove.trove.util.PropertyMap;

public interface AssetFactory {

    void init(PropertyMap properties) throws Exception;
    
    InputStream getAsset(String path);
}
