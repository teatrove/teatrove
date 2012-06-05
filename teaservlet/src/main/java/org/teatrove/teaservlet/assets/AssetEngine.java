package org.teatrove.teaservlet.assets;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import javax.servlet.ServletContext;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;

public class AssetEngine {

    private Log log;
    private ServletContext context;
    
    private List<AssetLoader> loaders;
    
    private WeakHashMap<String, AssetLoader> assets = 
        new WeakHashMap<String, AssetLoader>();
    
    public AssetEngine(ServletContext context) {
        this.context = context;
    }
    
    @Override
    public String toString() {
        return "AssetEngine[" + this.loaders.toString() + "]";
    }
    
    @SuppressWarnings("unchecked")
    public void init(Log log, PropertyMap properties) 
        throws Exception {
    
        // save log
        this.log = log;
        
        // create loaders
        this.loaders = new ArrayList<AssetLoader>();
        
        // check for top-level mime types
        Map<String, String> mimeTypes = 
            loadMimeTypes(properties.subMap("mimeTypes"), null);
        
        // check for top-level default paths
        List<AssetFactory> factories = new ArrayList<AssetFactory>();
        String defaultPaths = properties.getString("path");
        if (defaultPaths != null) {
            createAssetFactories(defaultPaths, factories);
        }
        
        // process each given loader
        PropertyMap loaderProps = properties.subMap("loaders");
        Set<String> keys = loaderProps.subKeySet();
        for (String key : keys) {
            AssetLoader loader = 
                createAssetLoader(key, loaderProps.subMap(key), mimeTypes);
            if (loader != null) { this.loaders.add(loader); }
        }
        
        // add default loader
        if (!factories.isEmpty()) {
            log.debug("creating default asset loader");
            String basePath = properties.getString("basePath");
            this.loaders.add(new AssetLoader("Default", basePath,
                                             factories, mimeTypes));
        }
    }
    
    public Asset getAsset(String path) {
        
        // validate configuration
        if (this.loaders == null) {
            throw new IllegalStateException("asset engine not initialized");
        }
        else if (this.loaders.isEmpty()) {
            log.debug("no asset loaders configured!");
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("searching for asset '" + path + "' within: " + loaders);
        }
        
        // check if previously cached for better efficiency
        AssetLoader cached = this.assets.get(path);
        if (cached != null) {
            return cached.getAsset(path);
        }
        
        // lookup the asset in each loader (first one wins)
        Asset asset = null;
        for (AssetLoader loader : this.loaders) {
            asset = loader.getAsset(path);
            if (asset != null) {
                // cache loader if found for better efficiency
                this.assets.put(path, loader);
                
                // return matching asset
                return asset;
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("asset not found: ".concat(path));
        }

        // none found
        return null;
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, String> loadMimeTypes(PropertyMap props, 
        Map<String, String> defaultMimeTypes) {
        
        final String inheritKey = "inherit";
        
        // generate mime types
        Map<String, String> mimeTypes = new HashMap<String, String>();
        
        // check if inherit and add defaults
        if (defaultMimeTypes != null && props.getBoolean(inheritKey, true)) {
            mimeTypes.putAll(defaultMimeTypes);
        }
        
        // add each extension
        Set<String> extensions = props.subKeySet();
        for (String extension : extensions) {
            if (inheritKey.equals(extension)) { continue; }
            String mimeType = props.getString(extension).trim();
            if (!mimeType.isEmpty()) {
                mimeTypes.put(extension, mimeType);
            }
        }
        
        // return found types
        return mimeTypes;
    }
    
    protected AssetLoader createAssetLoader(String name,
                                            PropertyMap loaderProps,
                                            Map<String, String> defaultMimeTypes) 
        throws Exception {
        
        // create factories
        List<AssetFactory> factories = new ArrayList<AssetFactory>();
        
        // load mime-types
        Map<String, String> mimeTypes = 
            loadMimeTypes(loaderProps.subMap("mimeTypes"), defaultMimeTypes);
        
        // check if factory registered (precedence)
        String factoryClass = loaderProps.getString("factory.class");
        if (factoryClass != null) {
            PropertyMap config = loaderProps.subMap("factory.init");
            AssetFactory factory = createAssetFactory(factoryClass, config);
            factories.add(factory);
        }
        
        // process each supplied path
        String paths = loaderProps.getString("path");
        if (paths != null) {
            createAssetFactories(paths, factories);
        }
        
        // load base path
        String basePath = loaderProps.getString("basePath");
        
        // create and return asset loader
        if (!factories.isEmpty()) {
            log.debug("creating asset loader: ".concat(name));
            AssetLoader loader =
                new AssetLoader(name, basePath, factories, mimeTypes);
            loader.init(log);
            return loader;
        }
        
        // none found
        log.warn("no asset factories found for loader: ".concat(name));
        return null;
    }
    
    protected void createAssetFactories(String paths, 
                                        List<AssetFactory> factories)
        throws Exception {
        
        StringTokenizer tokens = new StringTokenizer(paths, ",");
        while (tokens.hasMoreTokens()) {
            String path = tokens.nextToken().trim();
            if (!path.isEmpty()) {
                AssetFactory factory = createAssetFactory(path);
                factories.add(factory);
            }
        }
    }
    
    protected AssetFactory createAssetFactory(String path) 
        throws Exception {
        
        AssetFactory factory = null;
        
        // check for classpath or file-based paths
        if (path.startsWith("classpath:")) {
            log.debug("creating classpath asset factory: ".concat(path));
            factory = new ClasspathAssetFactory(path.substring(10));
        }
        else if (path.startsWith("file:")) {
            log.debug("creating file asset factory: ".concat(path));
            factory = new FileAssetFactory(path.substring(5));
        }
        else if (path.startsWith("web:")) {
            log.debug("creating web asset factory: ".concat(path));
            factory = new ServletContextAssetFactory(context, path.substring(4));
        }
        
        // check for url-based path
        if (factory == null) {
            try {
                URL url = new URL(path);
                log.debug("creating url asset factory: ".concat(path));
                factory = new UrlAssetFactory(url);
            }
            catch (MalformedURLException exception) {
                log.debug("creating web asset factory: ".concat(path));
                factory = new ServletContextAssetFactory(context, path);
            }
        }

        // initialize and return factory
        factory.init(log, new PropertyMap());
        return factory;
    }
    
    protected AssetFactory createAssetFactory(String className, 
                                              PropertyMap config) 
        throws Exception {

        // instantiate factory
        AssetFactory factory = null;
        log.debug("creating asset factory for: ".concat(className));
        try { factory = (AssetFactory) Class.forName(className).newInstance(); }
        catch (Exception exception) {
            throw new IllegalStateException(
                "unable to create asset factory: " + className, exception);
        }

        // initialize factory
        factory.init(log, config);

        // return the factory instance
        return factory;
    }
}
