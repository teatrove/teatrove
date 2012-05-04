package org.teatrove.tea.templates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.ErrorListener;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.MergedContextSource;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.util.FileCompilationProvider;
import org.teatrove.tea.util.StringCompilationProvider;
import org.teatrove.tea.util.TestCompiler;
import org.teatrove.trove.util.ClassInjector;

public abstract class AbstractTemplateTest {

    protected static boolean SHOW_OUTPUT = true; 
    protected static boolean ENABLE_CODEGEN = false;
    
    protected static final String DEST = "target/templates";
    protected static final String PKG = "org.teatrove.tea.templates";

    protected ClassInjector injector;
    protected Map<String, ContextSource> contexts;
    protected MergedContextSource context ;
    protected AtomicInteger counter = new AtomicInteger(0);

    public AbstractTemplateTest() {
        contexts = new HashMap<String, ContextSource>();
        contexts.put("DefaultApplication$", new ContextSource() {

            @Override
            public Class<?> getContextType() throws Exception {
                return TestCompiler.Context.class;
            }

            @Override
            public Object createContext(Object param) throws Exception {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) param;
                return new TestCompiler.Context(new PrintStream(baos));
            }
        });
    }

    public void addContext(final String name, final Object context) {
        contexts.put(name.concat("$"), new ContextSource() {

            @Override
            public Class<?> getContextType() throws Exception {
                return context.getClass();
            }

            @Override
            public Object createContext(Object param) throws Exception {
                return context;
            }
        });
    }

    public MergedContextSource getContext() {
        if (context == null) {
            synchronized (this) {
                if (context == null) {
                    try { context = createContext(); }
                    catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        return context;
    }

    protected MergedContextSource createContext() throws Exception {
        // setup merged context
        MergedContextSource source = new MergedContextSource();
        source.init
        (
            Thread.currentThread().getContextClassLoader(),
            contexts.values().toArray(new ContextSource[contexts.size()]),
            contexts.keySet().toArray(new String[contexts.size()]),
            false
        );

        return source;
    }

    protected ClassInjector getInjector() {
        if (injector == null) {
            synchronized (this) {
                if (injector == null) {
                    try { injector = createInjector(); }
                    catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        return injector;
    }

    protected ClassInjector createInjector() throws Exception {
        return new ClassInjector(getContext().getContextType().getClassLoader());
    }

    public void compileFiles(String... templates)
        throws Exception {

        for (String template : templates) {
            compileFile(template);
        }
    }

    public void compile(String template, String source)
        throws Exception {

        // create target directory
        File target = new File(DEST + '/' + PKG.replace('.', '/'));
        target.mkdirs();

        // clean template class
        new File(target, template.replace('.', '/').concat(".class")).delete();

        // create compiler
        File destDir = (ENABLE_CODEGEN ? new File(DEST) : null);
        Compiler compiler = new Compiler(getInjector(), PKG, destDir);
        StringCompilationProvider provider = new StringCompilationProvider();
        compiler.addCompilationProvider(provider);

        // setup context
        
        compiler.setRuntimeContext(getContext().getContextType());

        // setup error handler
        compiler.addErrorListener(new ErrorListener() {
            @Override
            public void compileError(ErrorEvent e) {
                System.err.println(e.getDetailedErrorMessage());
            }
        });

        // add sources for templates
        provider.setTemplateSource(template, source);

        // compile templates
        String[] results = compiler.compile(template);
        if (results == null || results.length < 1) {
            throw new IllegalStateException("unable to compile");
        }
    }

    public void compileFile(String template) throws Exception {
        // create target directory
        File target = new File(DEST + '/' + PKG.replace('.', '/'));
        target.mkdirs();

        // clean template class
        new File(target, template.replace('.', '/').concat(".class")).delete();

        // create compiler
        File destDir = (ENABLE_CODEGEN ? new File(DEST) : null);
        Compiler compiler = new Compiler(getInjector(), PKG, destDir);
        FileCompilationProvider provider = new FileCompilationProvider(
            new File("src/tests/templates")
        );
        compiler.addCompilationProvider(provider);

        // setup context
        compiler.setRuntimeContext(getContext().getContextType());

        // setup error handler
        compiler.addErrorListener(new ErrorListener() {
            @Override
            public void compileError(ErrorEvent e) {
                System.err.println(e.getDetailedErrorMessage());
            }
        });

        // compile templates
        String[] results = compiler.compile(template);
        if (results == null || results.length < 1) {
            throw new IllegalStateException("unable to compile");
        }
    }

    public String executeFile(String template, Object... params)
        throws Exception {

        // compile and execute
        compileFile(template);
        return _execute(template, params);
    }

    public String getTemplateName(int index) {
        return this.getClass().getSimpleName().toLowerCase() + index;
    }

    public String getTemplateSource(int index, String source) {
        return getTemplateSource(index, source, "");
    }

    public String getTemplateSource(int index,
                                    String source, String signature) {
        return "<% template " + getTemplateName(index) +
            "(" + signature + ") " + source;
    }

    public String executeSource(String source, Object... params)
        throws Exception {

        int index = counter.incrementAndGet(); 
        return execute(getTemplateName(index), 
                       getTemplateSource(index, source), params);
    }

    public String executeSource(String source, String signature,
                                Object... params)
        throws Exception {

        int index = counter.incrementAndGet(); 
        return execute(getTemplateName(index), 
                       getTemplateSource(index, source, signature), params);
    }

    public String execute(String template, String source, Object... params)
        throws Exception {

        // compile and execute
        compile(template, source);
        return _execute(template, params);
    }

    protected String _execute(String template, Object... params)
        throws Exception {

        // get class loader
        ClassLoader loader = getInjector();

        // load template class
        Class<?> clazz = loader.loadClass(PKG + '.' + template);
        if (clazz == null) {
            throw new IllegalStateException("unable to load class");
        }

        // lookup execute methd
        Method execute = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals("execute")) {
                execute = method;
                break;
            }
        }

        // verify execute method
        if (execute == null) {
            throw new IllegalStateException("unable to find execute method");
        }

        // create context
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
        Context ctx = (Context) getContext().createContext(output);
        
        // setup params
        Object[] args = new Object[params.length + 1];
        args[0] = ctx;
        for (int i = 0; i < params.length; i++) {
            args[i + 1] = params[i];
        }

        // execute template
        Object result = execute.invoke(null, args);
        if (!void.class.equals(execute.getReturnType())) {
            ctx.print(result);
        }

        // print code
        String outcome = output.toString();
        if (SHOW_OUTPUT) {
            System.out.println("template " + template + ": " + outcome);
        }

        // return code
        return outcome;
    }
}
