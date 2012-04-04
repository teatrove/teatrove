package org.teatrove.teaservlet.assets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.teatrove.trove.log.Log;
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
    public String toString() {
        return "file:".concat(this.directory.getAbsolutePath());
    }
    
    @Override
    public void init(Log log, PropertyMap properties) throws Exception {
        super.init(log, properties);
        
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
            return null;
        }
        
        // ensure file is a valid file
        if (!file.isFile()) {
            log.error("assets must be files: " + file.getAbsolutePath());
            return null;
        }
        
        try {
            // ensure the resulting file is a child of the directory
            String filePath = file.getCanonicalPath();
            String dirPath = directory.getCanonicalPath();
            if (!filePath.startsWith(dirPath.concat(File.separator))) {
                log.error(
                    "file paths must be relative to directory: " + 
                    directory.getAbsolutePath() + ":" + path
                );
                return null;
            }
            
            // return input stream for file
            return new FileInputStream(file);
        }
        catch (IOException ioe) {
            log.error("unable to retrieve asset: ".concat(path));
            log.error(ioe);
            return null;
        }
    }
}
