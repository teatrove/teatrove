package org.teatrove.teaadmin.viewer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Parser;
import org.teatrove.tea.compiler.Scanner;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.teaservlet.TeaServlet;
import org.teatrove.teaservlet.TeaServletAdmin;
import org.teatrove.teaservlet.TeaServletAdmin.TemplateWrapper;
import org.teatrove.teaservlet.TeaServletEngine;
import org.teatrove.trove.io.SourceReader;
import org.teatrove.trove.log.Log;

public class TemplateViewerApplication implements Application
{
    private Log log;
    private int maxCache;
    private boolean initialized;
    private TeaServletAdmin admin;
    private ApplicationConfig config;
    private TeaServletEngine engine;
    private TemplateViewerContext context;

    private Map<String, TemplateView> cache =
        new HashMap<String, TemplateView>();
    private Map<String, TemplateSource> sourceMap =
        new HashMap<String, TemplateSource>();
    private SortedSet<TemplateSource> sourceList =
        new TreeSet<TemplateSource>();

    public TemplateViewerApplication()
    {
        super();
    }

    @Override
    public void init(ApplicationConfig conf)
        throws ServletException
    {
        this.config = conf;
        this.log = conf.getLog();
        this.maxCache = conf.getProperties().getInt("maxCache", 100);
    }

    @Override
    public void destroy()
    {
        // nothing to do
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getContextType()
    {
        return TemplateViewerContext.class;
    }

    @Override
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response)
    {
        if (!initialized) { initialize(); }
        return this.context;
    }

    protected void initialize()
    {
        try
        {
            // get the associated tea servlet
            ServletContext context = config.getServletContext();
            TeaServlet servlet =
                (TeaServlet) context.getAttribute(TeaServlet.class.getName());

            // error if no servlet found
            if (servlet == null)
            {
                throw new IllegalArgumentException("no tea servlet defined");
            }

            // find the TeaServlet.getEngine protected method
            Method method = null;
            Class<?> clazz = servlet.getClass();
            while (clazz != null && method == null) {
                try { method = clazz.getDeclaredMethod("getEngine"); }
                catch (Exception e) { clazz = clazz.getSuperclass(); }
            }

            // error if no method found
            if (method == null)
            {
                throw new IllegalArgumentException("no getEngine method found");
            }

            // get tea servlet engine
            method.setAccessible(true);
            this.engine = (TeaServletEngine) method.invoke(servlet);

            // create a new administration and paths
            this.admin = new TeaServletAdmin(engine);

            // create singleton context
            this.context = new TemplateViewerContextImpl();
            
            // mark initialized
            this.initialized = true;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("unable to initialize app", e);
        }
    }

    public static class TemplateSource implements Comparable<TemplateSource>
    {
        private long accessTime;
        private long accessCount;
        private String templateName;
        private String sourceCode;

        public TemplateSource(String templateName, String sourceCode)
        {
            this.templateName = templateName;
            this.sourceCode = sourceCode;
        }

        public String getTemplateName() { return this.templateName; }
        public String getSourceCode() { return this.sourceCode; }

        public void markAccessed()
        {
            this.accessCount++;
            this.accessTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) { return true; }
            else if (!(object instanceof TemplateSource)) { return false; }

            TemplateSource other = (TemplateSource) object;
            return this.templateName.equals(other.templateName);
        }

        @Override
        public int hashCode()
        {
            return this.templateName.hashCode();
        }

        @Override
        public int compareTo(TemplateSource other)
        {
            if (this.accessTime < other.accessTime) { return -1; }
            else if (this.accessTime > other.accessTime) { return 1; }
            else { return this.templateName.compareTo(other.templateName); }
        }
    }

    public static class Name {
        private String name;
        public Name(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public class TemplateViewerContextImpl implements TemplateViewerContext
    {
        public TemplateViewerContextImpl()
        {
            super();
        }

        public String[] findTemplates(String term)
        {
            Set<String> matches = new TreeSet<String>();
            TemplateWrapper[] templates = admin.getKnownTemplates();
            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                if (name.contains(term)) { matches.add(name); }
            }

            return matches.toArray(new String[matches.size()]);
        }

        public Parent[] getParents(String parent)
        {
            Set<Parent> parents = new TreeSet<Parent>();
            Map<String, Boolean> dirs = new HashMap<String, Boolean>();
            TemplateWrapper[] templates = admin.getKnownTemplates();

            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                if (parent.length() == 0)
                {
                    int idx = name.indexOf('.');
                    if (idx < 0) { parents.add(new Parent(parent, name, false)); }
                    else
                    {
                        String dir = name.substring(0, idx);
                        if (!dirs.containsKey(dir))
                        {
                            dirs.put(dir, Boolean.TRUE);
                            parents.add(new Parent(parent, dir, true));
                        }
                    }
                }
                else if (name.startsWith(parent))
                {
                    int idx = name.indexOf('.', parent.length() + 1);
                    if (idx < 0)
                    {
                        parents.add(new Parent(parent, name.substring(parent.length() + 1), false));
                    }
                    else
                    {
                        String dir = name.substring(parent.length() + 1, idx);
                        if (!dirs.containsKey(dir))
                        {
                            dirs.put(dir, Boolean.TRUE);
                            parents.add(new Parent(parent, dir, true));
                        }
                    }
                }
            }

            return parents.toArray(new Parent[parents.size()]);
        }

        public TemplateView[] getTemplateViews()
        {
            TemplateRepository repo = TemplateRepository.getInstance();

            Set<TemplateView> matches = new TreeSet<TemplateView>();
            TemplateWrapper[] templates = admin.getKnownTemplates();
            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                TemplateView view = cache.get(name);
                if (view == null)
                {
                    TemplateInfo info = repo.getTemplateInfo(name);
                    if (info != null) { view = new TemplateView(name, info); }
                }

                if (view != null) { matches.add(view); }
            }

            return matches.toArray(new TemplateView[matches.size()]);
        }

        public void resetTemplateViews()
        {
            // log result
            if (log.isDebugEnabled()) 
            {
                log.debug("resetting all template sources");
            }
            
            cache.clear();
            sourceList.clear();
            sourceMap.clear();
        }

        public boolean resetTemplateView(String name)
        {
            // log result
            if (log.isDebugEnabled()) 
            {
                log.debug("resetting template source: " + name);
            }
            
            name = name.replace("/", ".");
            TemplateView view = cache.remove(name);
            TemplateSource source = sourceMap.remove(name);
            if (source != null) { sourceList.remove(source); }
            return (view == null ? false : true);
        }

        public TemplateView getTemplateView(String parent, String name)
            throws Exception
        {
            // log result
            if (log.isDebugEnabled()) 
            {
                log.debug("looking for template source: " + parent + ":" + name);
            }
            
            // verify template
            if (name == null) { throw new IllegalArgumentException("name"); }

            // ensure format
            name = name.replace("/", ".");
            if (parent != null) { parent = parent.replace("/", "."); }
            
            // find actual path based on parent and template
            String path = null;
            TemplateInfo template = null;
            TemplateRepository repo = TemplateRepository.getInstance();
            if (parent != null)
            {
                path = parent + '.' + name;
                template = repo.getTemplateInfo(path);
            }

            if (template == null)
            {
                path = name;
                template = repo.getTemplateInfo(name);
            }

            // check cache
            TemplateView view = cache.get(path);
            if (view == null)
            {
                // build view and cache
                view = new TemplateView(path, template);
                cache.put(path, view);
            }

            // build source if necessary
            if (view.getSourceCode() == null)
            {
                try { this.parseTemplate(view); }
                catch (Exception e) {
                    log.error("source unavailable: " + path);
                    log.error(e);
                }
            }

            // get source code
            TemplateSource source = sourceMap.get(path);
            if (source == null)
            {
                source = new TemplateSource(path, view.getSourceCode());
                while (sourceMap.size() > maxCache)
                {
                    TemplateSource first = sourceList.first();
                    sourceList.remove(first);
                    sourceMap.remove(first.getTemplateName());

                    TemplateView temp = cache.get(first.getTemplateName());
                    if (temp != null) { temp.setSourceCode(null); }
                }

                sourceMap.put(path, source);
                sourceList.add(source);
            }

            // update source code
            source.markAccessed();
            view.setSourceCode(source.getSourceCode());

            // return associated view
            return view;
        }

        protected StringBuilder read(Reader input) 
            throws IOException {
            
            StringBuilder buffer = new StringBuilder(65535);
            
            // copy the input stream to a source buffer
            int ch = -1;
            while ((ch = input.read()) >= 0) {
                if (ch == '\r') { continue; }
                buffer.append((char) ch);
            }
            
            return buffer;
        }
        
        protected void parseTemplate(TemplateView view)
            throws Exception
        {
            // search for valid stream
            Reader input = this.findTemplate(view, view.getSimpleName());
            
            // build and return output
            try { this.processTemplate(view, input); }
            finally { input.close(); }

            // build call hierarchy
            TemplateRepository repo = TemplateRepository.getInstance();
            for (TemplateInfo info : repo.getCallers(view.getSimpleName()))
            {
                TemplateInfo callerTemplate =
                    repo.getTemplateInfo(info.getShortName());
                
                String tname = info.getShortName().replace("/", "."); 
                TemplateView callerView = 
                    new TemplateView(tname, callerTemplate );

                // search for valid stream
                Reader callerInput =
                    this.findTemplate(callerView, callerView.getSimpleName());
                
                // build and return output
                try { this.processTemplate(callerView, callerInput); }
                finally { input.close(); }

                for (TemplateView.Callee callee : callerView.getCallees())
                {
                    if (callee.getName().equals(view.getSimpleName()))
                    {
                        view.addCaller(
                            new TemplateView.Caller(tname, callee.getLine())
                        );
                    }
                }
            }
        }

        protected Reader findTemplate(TemplateView view, String name)
            throws Exception
        {
            name = name.replace("/", ".");
            TemplateCompilationResults results =
                engine.getTemplateSource().checkTemplates(null, true, name);
            CompilationUnit unit = results.getReloadedTemplate(name);
            if (unit == null) {
                throw new FileNotFoundException(name);
            }

            view.setLocation(unit.getSourcePath());
            return new BufferedReader(unit.getReader());
        }

        protected void processTemplate(TemplateView view, Reader input)
            throws Exception {
            
            // walk the source tree injecting tags onto keywords,
            // setting up newline boundaries, adding callee info, and cleaning
            // the source code
            StringBuilder buffer = read(input);
            StringReader reader = new StringReader(buffer.toString());
            Scanner scanner = new Scanner(new SourceReader(reader, "<%", "%>"));
            Parser parser = new Parser(scanner);
            SourceWalker walker = new SourceWalker(buffer);
            walker.visit(parser.parse());
            walker.finish(view);
        }
    }


    protected static final List<String> KEYWORDS = Arrays.asList
    (
        "if",
        "else",
        "foreach",
        "for",
        "while",
        "capture"
    );

    protected static enum State
    {
        DEFAULT,
        TAG_OPEN,
        TEMPLATE,
        COMMENT,
        COMMENT_START,
        SINGLE_COMMENT,
        MULTI_COMMENT,
        MULTI_COMMENT_END,
        TEMPLATE_END,
        COMMENT_END,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        STATEMENT,
        STATEMENT_END,
        STATEMENT_KEYWORD,
        CALL,
        PARAMS,
        DECLARATION,
        FUNCTION_PARAMS,
        CALL_PARAMS
    }
}
