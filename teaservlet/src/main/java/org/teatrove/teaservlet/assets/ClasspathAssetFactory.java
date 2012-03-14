package org.teatrove.teaservlet.assets;

import java.io.InputStream;

import org.teatrove.trove.util.PropertyMap;

public class ClasspathAssetFactory extends AbstractAssetFactory {

    private String rootPackage;
    private ClassLoader classLoader;
    
    public ClasspathAssetFactory() {
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }
    
    public ClasspathAssetFactory(String rootPackage) {
        this(Thread.currentThread().getContextClassLoader(), rootPackage);
    }
    
    public ClasspathAssetFactory(ClassLoader classLoader, String rootPackage) {
        if (rootPackage != null && !rootPackage.endsWith("/")) {
            rootPackage = rootPackage.concat("/");
        }
        
        this.classLoader = classLoader;
        this.rootPackage = rootPackage;
    }
    
    @Override
    public void init(PropertyMap properties) throws Exception {
        // lookup root package, if provided
        String rootPkg = properties.getString("rootPackage");
        if (rootPkg != null) {
            this.rootPackage = rootPkg;
        }
        
        // validate root package
        if (this.rootPackage == null || this.rootPackage.isEmpty()) {
            throw new IllegalStateException("missing root package");
        }
    }
    
    @Override
    public InputStream getAsset(String path) {
        // validate path
        path = validatePath(path);
        
        // lookup resource
        String resource = rootPackage.concat(path);
        InputStream input = classLoader.getResourceAsStream(resource);
        if (input == null) {
            return null;
        }
        
        // return found resource
        return input;
    }

}
