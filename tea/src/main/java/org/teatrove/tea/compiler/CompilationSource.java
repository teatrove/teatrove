package org.teatrove.tea.compiler;

import java.io.IOException;
import java.io.InputStream;

public interface CompilationSource {
    
    public String getSourcePath();
    
    public InputStream getSource() 
        throws IOException;

    public long getLastModified();
}
