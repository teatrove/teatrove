package org.teatrove.teaadmin.viewer;

import org.teatrove.tea.compiler.SourceInfo;

public class Rewrite {
    private String value;
    private SourceInfo sourceInfo;

    public Rewrite(String value, SourceInfo sourceInfo) {
        this.value = value;
        this.sourceInfo = sourceInfo;
    }

    public String getValue() { return this.value; }
    public SourceInfo getSourceInfo() { return this.sourceInfo; }
}
