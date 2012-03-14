package org.teatrove.teaservlet.assets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.teatrove.trove.util.PropertyMap;

public class FileAssetFactory extends AbstractAssetFactory {

    private File directory;
    
    public FileAssetFactory() {
        super();
    }
    
    public FileAssetFactory(String directory) {
        this(new File(directory));
    }
    
    public FileAssetFactory(File directory) {
        this.directory = directory;
    }
    
    @Override
    public void init(PropertyMap properties) throws Exception {
        // lookup directory, if provided
        String dir = properties.getString("directory");
        if (dir != null) {
            this.directory = new File(dir);
        }
        
        // validate root package
        if (this.directory == null) {
            throw new IllegalStateException("missing directory");
        }
        
        // validate directory
        if (!this.directory.exists() || !this.directory.isDirectory()) {
            throw new FileNotFoundException("invalid directory: " + directory);
        }
    }
    
    @Override
    public InputStream getAsset(String path) {
        // validate path
        path = validatePath(path);

        // load file relative to directory
        File file = new File(directory, path);
        if (!file.exists()) {
            // TODO: LOG.error("asset does not exist: " + path);
            return null;
        }
        
        // ensure file is a valid file
        if (!file.isFile()) {
            // TODO: LOG.error("assets must be files: " + path);
            return null;
        }
        
        try {
            // ensure the resulting file is a child of the directory
            String filePath = file.getCanonicalPath();
            String dirPath = directory.getCanonicalPath();
            if (!filePath.startsWith(dirPath.concat(File.separator))) {
                // TODO: LOG.error(
                //    "file paths must be relative to directory: " + path);
                return null;
            }
            
            // return input stream for file
            return new FileInputStream(file);
        }
        catch (IOException ioe) {
            // TODO: LOG.error("unable to retrieve asset: " + path, ioe);
            return null;
        }
    }
}
