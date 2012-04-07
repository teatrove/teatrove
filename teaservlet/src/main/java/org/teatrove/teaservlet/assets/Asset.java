package org.teatrove.teaservlet.assets;

import java.io.InputStream;

public class Asset {

    private String path;
    private String mimeType;
    private InputStream input;
    
    public Asset(String path, String mimeType, InputStream input) {
        this.path = path;
        this.mimeType = mimeType;
        this.input = input;
    }
    
    public String getPath() { return this.path; }
    public String getMimeType() { return this.mimeType; }
    public InputStream getInputStream() { return this.input; }
    
    @Override
    public String toString() {
        return this.path + " [" + this.mimeType + "]";
    }
}
