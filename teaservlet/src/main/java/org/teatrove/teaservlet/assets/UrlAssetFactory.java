package org.teatrove.teaservlet.assets;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.teatrove.trove.log.Syslog;
import org.teatrove.trove.util.PropertyMap;

public class UrlAssetFactory extends AbstractAssetFactory {

    private URL baseUrl;
    
    public UrlAssetFactory() {
        super();
    }
    
    public UrlAssetFactory(String baseUrl) throws MalformedURLException {
        this(new URL(baseUrl));
    }
    
    public UrlAssetFactory(URL baseUrl) {
        String basePath = baseUrl.toExternalForm();
        if (!basePath.endsWith("/")) {
            try { baseUrl = new URL(basePath.concat("/")); }
            catch (MalformedURLException exception) {
                throw new IllegalStateException(exception);
            }
        }
        
        this.baseUrl = baseUrl;
    }
    
    @Override
    public void init(PropertyMap properties) throws Exception {
        // lookup base path, if provided
        String base = properties.getString("baseUrl");
        if (base != null) {
            if (!base.endsWith("/")) {
                base = base.concat("/");
            }

            this.baseUrl = new URL(base);
        }
        
        // validate base path
        if (this.baseUrl == null) {
            throw new IllegalStateException("missing base url");
        }
    }
    
    @Override
    public InputStream getAsset(String path) {
        // validate path
        path = validatePath(path);
        
        // append the path to create a new URL
        URL resourceUrl = null;
        try { resourceUrl = new URL(baseUrl.toExternalForm().concat(path)); }
        catch (MalformedURLException exception) {
            Syslog.debug(
                "invalid asset path url: " + baseUrl.toExternalForm() + path
            );
            Syslog.debug(exception);
            return null;
        }
        
        // ensure the resulting file is a child of the base
        String basePath = baseUrl.toExternalForm();
        String resourcePath = resourceUrl.toExternalForm();
        if (!resourcePath.startsWith(basePath)) {
            Syslog.error(
                "url paths must be relative to base url: " + 
                baseUrl.toExternalForm() + ":" + path
            );
            return null;
        }

        // open stream for associated url
        try { return resourceUrl.openStream(); }
        catch (IOException ioe) {
            Syslog.debug(
                "unable to open asset stream: " + resourceUrl.toExternalForm());
            Syslog.debug(ioe);
            return null;
        }
    }

}
