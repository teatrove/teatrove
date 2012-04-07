package org.teatrove.teaservlet.assets;

import java.io.InputStream;

import javax.servlet.ServletContext;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;

public class ServletContextAssetFactory extends AbstractAssetFactory {

    private String basePath;
    private ServletContext context;
    
    public ServletContextAssetFactory(ServletContext context) {
        this.context = context;
    }
    
    public ServletContextAssetFactory(ServletContext context, String basePath) {
        if (basePath != null && !basePath.endsWith("/")) {
            basePath = basePath.concat("/");
        }
        
        this.context = context;
        this.basePath = basePath;
    }
    
    @Override
    public String toString() {
        return "web:".concat(this.basePath);
    }
    
    @Override
    public void init(Log log, PropertyMap properties) throws Exception {
        super.init(log, properties);
        
        // lookup base path, if provided
        String base = properties.getString("basePath");
        if (base != null) {
            if (!base.endsWith("/")) {
                base = base.concat("/");
            }
            
            this.basePath = base;
        }
        
        // validate base path
        if (this.basePath == null || this.basePath.isEmpty()) {
            throw new IllegalStateException("missing base path");
        }
    }
    
    @Override
    public InputStream getAsset(String path) {
        // validate path
        path = validatePath(path);
        
        // lookup resource
        String resource = basePath.concat(path);
        InputStream input = context.getResourceAsStream(resource);
        if (input == null) {
            return null;
        }
        
        // return found resource
        return input;
    }
}
