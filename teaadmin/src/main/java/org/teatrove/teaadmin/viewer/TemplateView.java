package org.teatrove.teaadmin.viewer;

import java.util.ArrayList;
import java.util.List;

import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;

public class TemplateView implements Comparable<TemplateView>
{
    private String name;
    private String simpleName;
    private TemplateInfo template;
    private Parameter[] parameters;
    private String location;
    private String sourceCode;
    private String parent;
    private String[] parents;
    private List<Caller> callers = new ArrayList<Caller>(50);
    private List<Callee> callees = new ArrayList<Callee>(50);

    public TemplateView(String name, TemplateInfo template)
    {
        super();
        this.simpleName = name;
        this.template = template;

        this.initialize();
    }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSimpleName() { return simpleName; }
    public void setSimpleName(String simpleName) { this.simpleName = simpleName; }

    public TemplateInfo getTemplateInfo() { return template; }
    public void setTemplateInfo(TemplateInfo template) { this.template = template; }

    public Parameter[] getParameters() { return parameters; }
    public void setParameters(Parameter[] parameters) { this.parameters = parameters; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String[] getParents() { return parents; }
    public void setParents(String[] parents) { this.parents = parents; }
    public String getParent() { return this.parent; }

    public Caller[] getCallers() { return callers.toArray(new Caller[callers.size()]); }
    public void addCaller(Caller caller) { this.callers.add(caller); }

    public Callee[] getCallees() { return callees.toArray(new Callee[callees.size()]); }
    public void addCallee(Callee callee) { this.callees.add(callee); }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) { return true; }
        else if (!(object instanceof TemplateView)) { return false; }

        TemplateView other = (TemplateView) object;
        return this.getName().equals(other.getName());
    }

    @Override
    public int hashCode()
    {
        return this.getName().hashCode();
    }

    @Override
    public int compareTo(TemplateView other)
    {
        return this.getName().compareTo(other.getName());
    }

    private void initialize()
    {
        this.name = 
            (this.template == null ? this.simpleName.replace(".", "/") 
                                   : this.template.getShortName());
        
        this.parents = this.name.split("/");
        this.parent = (this.parents.length <= 1 ? "/" :
                       this.name.substring(0, this.name.lastIndexOf('/')));
    }

    public static class Parameter
    {
        private Class<?> type;
        private String name;

        public Parameter(Class<?> type, String name)
        {
            super();
            this.type = type;
            this.name = name;
        }

        public Class<?> getType() { return type; }
        public String getName() { return name; }
    }

    public static class Caller
    {
        private int line;
        private String name;

        public Caller(String name, int line)
        {
            super();
            this.name = name;
            this.line = line;
        }

        public int getLine() { return line; }
        public String getName() { return name; }
    }

    public static class Callee
    {
        private int line;
        private String name;
        private String statement;
        private boolean isTemplate;

        public Callee(int line, String name,
                      String statement, boolean isTemplate)
        {
            this(line, name, name, statement, isTemplate);
        }

        public Callee(int line, String name, String subname,
                      String statement, boolean isTemplate)
        {
            super();
            this.line = line;
            this.name = name;
            this.isTemplate = isTemplate;

            /*
            StringBuilder stmt =
                new StringBuilder(statement.length() + name.length() + 5);
            if (this.isTemplate) { stmt.append("call "); }
            stmt.append(subname).append(statement);
            this.statement = stmt.toString();
            */
            this.statement = statement;
        }

        public int getLine() { return line; }
        public String getName() { return name; }
        public String getStatement() { return this.statement; }
        public boolean getIsTemplate() { return this.isTemplate; }
    }
}
