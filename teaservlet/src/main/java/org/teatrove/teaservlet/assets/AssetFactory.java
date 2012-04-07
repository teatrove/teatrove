package org.teatrove.teaservlet.assets;

import java.io.InputStream;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;

public interface AssetFactory {

    void init(Log log, PropertyMap properties) throws Exception;
    
    InputStream getAsset(String path);
}
