package org.teatrove.tea.engine;

import java.io.Serializable;
import java.util.List;

import org.teatrove.tea.compiler.CompilationUnit;

public class TemplateExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private CompilationUnit unit;
    private List<TemplateError> errors;
    private Template template;
    private String output;
    
    public TemplateExecutionResult(CompilationUnit unit, 
                                   List<TemplateError> errors) {
        this(unit, errors, null);
    }
    
    public TemplateExecutionResult(CompilationUnit unit, 
                                   List<TemplateError> errors, 
                                   Template template) {
        this.unit = unit;
        this.errors = errors;
        this.template = template;
    }
    
    public CompilationUnit getCompilationUnit() { return this.unit; }
    public Template getTemplate() { return this.template; }
    public List<TemplateError> getTemplateErrors() { return this.errors; }
    
    public boolean isSuccessful() {
        return (this.template != null &&
                (this.errors == null || this.errors.size() == 0));
    }
    
    public String getOutput() { return this.output; }
    public void setOutput(String output) { this.output = output; }
}
