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
        if (rootPackage != null) {
            if (!rootPackage.endsWith("/")) {
                rootPackage = rootPackage.concat("/");
            }
            if (rootPackage.startsWith("/")) {
                rootPackage = rootPackage.substring(1);
            }
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
            
            // cleanup package
            if (!rootPackage.endsWith("/")) {
                rootPackage = rootPackage.concat("/");
            }
            if (rootPackage.startsWith("/")) {
                rootPackage = rootPackage.substring(1);
            }
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
        
        // allow path to contain either root package or an extension of it
        String resource = path;
        if (!resource.startsWith(rootPackage)) {
            resource = rootPackage.concat(path);
        }
        
        // lookup resource
        InputStream input = classLoader.getResourceAsStream(resource);
        if (input == null) {
            return null;
        }
        
        // return found resource
        return input;
    }

}
