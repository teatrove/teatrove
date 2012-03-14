package org.teatrove.teaservlet.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletResponse;

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
    
    public void writeTo(OutputStream output) 
        throws IOException {
        
        // TODO: write input to output w/ mime type
    }
    
    public void writeTo(ServletResponse response) 
        throws IOException {
    
        // TODO: write input to output w/ mime type
    }
}
